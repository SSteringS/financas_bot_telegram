package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.controller;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppEntry;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppMessage;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppStatus;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppWebhookPayload;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.mapper.WhatsAppMessageMapper;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.security.MetaSignatureValidator;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.UnauthorizedUserException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.MensagemEntrantePortIn;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhatsAppWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final MetaSignatureValidator signatureValidator;
    private final WhatsAppMessageMapper messageMapper;
    private final MensagemEntrantePortIn mensagemEntrantePortIn;
    private final ObjectMapper objectMapper;
    private static final String SENTINELA_NAO_CONFIGURADO = "NAO_CONFIGURADO";

    private final String verifyToken;
    private final boolean verifyTokenConfigurado;
    private final List<String> allowedWaIds;

    public WhatsAppWebhookController(
        MetaSignatureValidator signatureValidator,
        WhatsAppMessageMapper messageMapper,
        MensagemEntrantePortIn mensagemEntrantePortIn,
        ObjectMapper objectMapper,
        @Value("${whatsapp.verify-token:NAO_CONFIGURADO}") String verifyToken,
        @Value("${whatsapp.allowed-wa-ids:}") List<String> allowedWaIds
    ) {
        this.signatureValidator = signatureValidator;
        this.messageMapper = messageMapper;
        this.mensagemEntrantePortIn = mensagemEntrantePortIn;
        this.objectMapper = objectMapper;
        this.verifyToken = verifyToken;
        this.verifyTokenConfigurado = !SENTINELA_NAO_CONFIGURADO.equals(verifyToken);
        if (!verifyTokenConfigurado) {
            logger.warn(
                "whatsapp.verify-token nao configurado — handshake do webhook sempre retornara 403"
            );
        }
        // Descarta entradas em branco: uma property vazia chega aqui como lista de um elemento vazio.
        this.allowedWaIds = allowedWaIds.stream().filter(s -> !s.isBlank()).toList();
    }

    @GetMapping("/webhook/whatsapp")
    public ResponseEntity<String> handshake(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.verify_token") String token,
        @RequestParam("hub.challenge") String challenge
    ) {
        // A sentinela e um literal versionado, logo nao e segredo: sem esta guarda, qualquer um
        // passaria no handshake e teria o parametro 'hub.challenge' refletido na resposta.
        if (!verifyTokenConfigurado) {
            logger.warn("WhatsApp webhook handshake recusado — whatsapp.verify-token nao configurado");
            return ResponseEntity.status(403).build();
        }
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            logger.info("WhatsApp webhook handshake bem-sucedido.");
            return ResponseEntity.ok(challenge);
        }
        logger.warn("WhatsApp webhook handshake falhou — mode={} token mismatch={}", mode, !verifyToken.equals(token));
        return ResponseEntity.status(403).build();
    }

    @PostMapping("/webhook/whatsapp")
    public ResponseEntity<Void> receberWebhook(
        @RequestBody byte[] rawBody,
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
        HttpServletRequest request
    ) {
        if (!signatureValidator.isValid(rawBody, signatureHeader)) {
            logger.warn("Assinatura X-Hub-Signature-256 inválida ou ausente — descartando payload silenciosamente.");
            return ResponseEntity.ok().build();
        }

        WhatsAppWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, WhatsAppWebhookPayload.class);
        } catch (Exception e) {
            logger.warn("Falha ao desserializar payload WhatsApp: {}", e.getMessage());
            return ResponseEntity.ok().build();
        }

        for (WhatsAppEntry entry : nullSafe(payload.entry())) {
            var changes = entry.changes();
            for (var change : nullSafe(changes)) {
                var value = change.value();
                if (value == null) continue;

                for (WhatsAppStatus status : nullSafe(value.statuses())) {
                    logger.info("WhatsApp status update: id={} status={} recipient={}",
                        status.id(), status.status(), status.recipientId());
                }

                for (WhatsAppMessage message : nullSafe(value.messages())) {
                    String waId = message.from();
                    request.setAttribute("__whatsapp_wa_id", waId);
                    logger.info("WhatsApp mensagem recebida: wamid={} waId={} type={}", message.id(), waId, message.type());

                    autorizarUsuario(waId);

                    PaymentMessageDTO dto = messageMapper.toPaymentMessageDTO(message);
                    mensagemEntrantePortIn.processar(dto);
                    logger.info("Mensagem WhatsApp processada com sucesso: wamid={} waId={}", message.id(), waId);
                }
            }
        }

        return ResponseEntity.ok().build();
    }

    private void autorizarUsuario(String waId) {
        if (!allowedWaIds.contains(waId)) {
            throw new UnauthorizedUserException("Usuário não autorizado.", parseChatId(waId));
        }
    }

    private Long parseChatId(String waId) {
        try {
            return Long.parseLong(waId);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
