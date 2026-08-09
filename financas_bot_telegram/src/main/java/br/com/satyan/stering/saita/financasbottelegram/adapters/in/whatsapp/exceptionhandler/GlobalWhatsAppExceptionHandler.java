package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exceptionhandler;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exception.InvalidWhatsAppPayloadException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exception.WhatsAppMediaDownloadException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service.WhatsAppMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.BusinessRuleException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.DatabaseException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.UnauthorizedUserException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// InvalidMessageFormatException importada de adapters.in.telegram — violação hexagonal conhecida.
// Essa exceção deve ser movida para application/exceptions/ em task futura (FIX separada).
@RestControllerAdvice(basePackages = "br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp")
public class GlobalWhatsAppExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalWhatsAppExceptionHandler.class);

    private static final String MSG_RETRY = "⚠️ Não consegui registrar sua mensagem agora. Pode reenviar daqui a pouco, por favor?";

    private final WhatsAppMessageSenderService senderService;

    public GlobalWhatsAppExceptionHandler(WhatsAppMessageSenderService senderService) {
        this.senderService = senderService;
    }

    @ExceptionHandler(UnauthorizedUserException.class)
    public ResponseEntity<Void> handleUnauthorized(UnauthorizedUserException ex, HttpServletRequest request) {
        logger.warn("WhatsApp: usuário não autorizado — chatId={}", ex.getChatId());
        String waId = extrairWaId(request);
        if (waId != null) {
            enviaSemFalhar(waId, "🚫 Você não tem permissão para usar este bot.");
        }
        return ResponseEntity.ok().build();
    }

    // ADR 0003 — sempre 200; nunca 4xx/5xx pra webhook WhatsApp
    @ExceptionHandler(InvalidMessageFormatException.class)
    public ResponseEntity<Void> handleInvalidFormat(InvalidMessageFormatException ex, HttpServletRequest request) {
        logger.warn("WhatsApp: formato de mensagem inválido — chatId={}", ex.getChatId());
        String waId = extrairWaId(request);
        if (waId != null) {
            enviaSemFalhar(waId, "🚫 " + ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<Void> handlePedidoNaoEncontrado(PedidoNaoEncontradoException ex, HttpServletRequest request) {
        logger.warn("WhatsApp: pedido não encontrado — {}", ex.getMessage());
        String waId = extrairWaId(request);
        if (waId != null) {
            enviaSemFalhar(waId, "⚠️ " + ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(TipoArquivoNaoSuportadoException.class)
    public ResponseEntity<Void> handleTipoArquivo(TipoArquivoNaoSuportadoException ex, HttpServletRequest request) {
        logger.warn("WhatsApp: tipo de arquivo não suportado — waId={} msg={}", ex.getWaId(), ex.getMessage());
        String waId = extrairWaId(request);
        if (waId != null) {
            enviaSemFalhar(waId, "❌ " + ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Void> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        logger.warn("WhatsApp: regra de negócio violada — {}", ex.getMessage());
        String waId = extrairWaId(request);
        if (waId != null) {
            enviaSemFalhar(waId, "⚠️ " + ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    // ADR 0003 — DatabaseException retorna 200 (ao contrário do Telegram que retorna 500)
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<Void> handleDatabase(DatabaseException ex, HttpServletRequest request) {
        logger.error("WhatsApp: erro de banco de dados — {}", ex.getMessage(), ex.getCause());
        String waId = extrairWaId(request);
        if (waId != null) {
            enviaSemFalhar(waId, MSG_RETRY);
        }
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(WhatsAppMediaDownloadException.class)
    public ResponseEntity<Void> handleMediaDownload(WhatsAppMediaDownloadException ex, HttpServletRequest request) {
        logger.error("WhatsApp: falha no download de mídia — waId={} msg={}", ex.getWaId(), ex.getMessage());
        String waId = extrairWaId(request);
        if (waId != null) {
            enviaSemFalhar(waId, MSG_RETRY);
        }
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(InvalidWhatsAppPayloadException.class)
    public ResponseEntity<Void> handleInvalidPayload(InvalidWhatsAppPayloadException ex, HttpServletRequest request) {
        logger.warn("WhatsApp: payload inválido — {}", ex.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleAny(Exception ex, HttpServletRequest request) {
        logger.error("WhatsApp: exceção não mapeada no processamento do webhook", ex);
        String waId = extrairWaId(request);
        if (waId != null) {
            enviaSemFalhar(waId, MSG_RETRY);
        }
        return ResponseEntity.ok().build();
    }

    private void enviaSemFalhar(String waId, String mensagem) {
        try {
            senderService.enviarTexto(waId, mensagem);
        } catch (Exception e) {
            logger.error("Falha ao enviar mensagem de feedback ao usuário waId={} — segunda falha, descartando", waId, e);
        }
    }

    private String extrairWaId(HttpServletRequest request) {
        Object attr = request.getAttribute("__whatsapp_wa_id");
        return attr instanceof String s ? s : null;
    }
}
