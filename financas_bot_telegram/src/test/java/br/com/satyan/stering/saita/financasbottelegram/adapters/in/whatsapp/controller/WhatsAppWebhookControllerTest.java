package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.mapper.WhatsAppMessageMapper;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.security.MetaSignatureValidator;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.MensagemEntrantePortIn;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    @Mock private MetaSignatureValidator signatureValidator;
    @Mock private WhatsAppMessageMapper messageMapper;
    @Mock private MensagemEntrantePortIn mensagemEntrantePortIn;

    private static final String VERIFY_TOKEN = "meu-token-de-teste";
    private static final String WA_ID_AUTORIZADO = "5511999998888";

    private WhatsAppWebhookController controller(String... allowedWaIds) {
        return new WhatsAppWebhookController(
            signatureValidator, messageMapper, mensagemEntrantePortIn,
            new ObjectMapper(), VERIFY_TOKEN, List.of(allowedWaIds));
    }

    // ===== GET handshake =====

    @Test
    void handshake_tokenCorreto_retorna200ComChallenge() {
        ResponseEntity<String> response = controller(WA_ID_AUTORIZADO)
            .handshake("subscribe", VERIFY_TOKEN, "abc123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("abc123");
    }

    @Test
    void handshake_tokenErrado_retorna403() {
        ResponseEntity<String> response = controller(WA_ID_AUTORIZADO)
            .handshake("subscribe", "token-errado", "abc123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handshake_verifyTokenSentinela_naoRefleteChallenge_retorna403() {
        // A sentinela e um literal versionado: se fosse aceita como token, qualquer um passaria
        // no handshake e teria o 'hub.challenge' refletido de volta na resposta.
        WhatsAppWebhookController naoConfigurado = new WhatsAppWebhookController(
            signatureValidator, messageMapper, mensagemEntrantePortIn,
            new ObjectMapper(), "NAO_CONFIGURADO", List.of(WA_ID_AUTORIZADO));

        ResponseEntity<String> response =
            naoConfigurado.handshake("subscribe", "NAO_CONFIGURADO", "<script>alert(1)</script>");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void handshake_modeErrado_retorna403() {
        ResponseEntity<String> response = controller(WA_ID_AUTORIZADO)
            .handshake("unsubscribe", VERIFY_TOKEN, "abc123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===== POST webhook =====

    @Test
    void post_assinaturaInvalida_retorna200SemChamarPort() {
        when(signatureValidator.isValid(any(), any())).thenReturn(false);

        ResponseEntity<Void> response = controller(WA_ID_AUTORIZADO)
            .receberWebhook("{}".getBytes(), "sha256=invalida", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mensagemEntrantePortIn, never()).processar(any());
    }

    @Test
    void post_payloadSoComStatuses_retorna200SemChamarPort() throws Exception {
        String payload = """
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "id": "WABAID",
                "changes": [{
                  "field": "messages",
                  "value": {
                    "messaging_product": "whatsapp",
                    "statuses": [{"id":"wamid.ID","status":"delivered","timestamp":"1234","recipient_id":"5511"}]
                  }
                }]
              }]
            }
            """;
        byte[] body = payload.getBytes();
        when(signatureValidator.isValid(eq(body), any())).thenReturn(true);

        ResponseEntity<Void> response = controller(WA_ID_AUTORIZADO)
            .receberWebhook(body, "sha256=ok", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mensagemEntrantePortIn, never()).processar(any());
    }

    @Test
    void post_mensagemTextValida_retorna200EChamaPort() throws Exception {
        String payload = buildTextPayload(WA_ID_AUTORIZADO, "wamid.TEXTO123", "150.50 Almoço");
        byte[] body = payload.getBytes();
        when(signatureValidator.isValid(eq(body), any())).thenReturn(true);
        PaymentMessageDTO dto = PaymentMessageDTO.builder().canal(Canal.WHATSAPP).externalId("wamid.TEXTO123").build();
        when(messageMapper.toPaymentMessageDTO(any())).thenReturn(dto);

        ResponseEntity<Void> response = controller(WA_ID_AUTORIZADO)
            .receberWebhook(body, "sha256=ok", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mensagemEntrantePortIn).processar(dto);
    }

    @Test
    void post_waIdNaoAutorizado_lancaUnauthorizedException() throws Exception {
        String payload = buildTextPayload("9999999999", "wamid.NAOA", "texto");
        byte[] body = payload.getBytes();
        when(signatureValidator.isValid(eq(body), any())).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(
            br.com.satyan.stering.saita.financasbottelegram.application.exceptions.UnauthorizedUserException.class,
            () -> controller(WA_ID_AUTORIZADO).receberWebhook(body, "sha256=ok", new MockHttpServletRequest())
        );

        verify(mensagemEntrantePortIn, never()).processar(any());
    }

    @Test
    void post_payloadJsonInvalido_retorna200SemChamarPort() {
        byte[] body = "not-valid-json".getBytes();
        when(signatureValidator.isValid(eq(body), any())).thenReturn(true);

        ResponseEntity<Void> response = controller(WA_ID_AUTORIZADO)
            .receberWebhook(body, "sha256=ok", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mensagemEntrantePortIn, never()).processar(any());
    }

    @Test
    void post_requestAtributeTerWaId_setadoAntesDoMapper() throws Exception {
        String payload = buildTextPayload(WA_ID_AUTORIZADO, "wamid.ATTR", "texto");
        byte[] body = payload.getBytes();
        when(signatureValidator.isValid(eq(body), any())).thenReturn(true);
        when(messageMapper.toPaymentMessageDTO(any())).thenReturn(
            PaymentMessageDTO.builder().canal(Canal.WHATSAPP).externalId("wamid.ATTR").build());

        MockHttpServletRequest request = new MockHttpServletRequest();
        controller(WA_ID_AUTORIZADO).receberWebhook(body, "sha256=ok", request);

        assertThat(request.getAttribute("__whatsapp_wa_id")).isEqualTo(WA_ID_AUTORIZADO);
    }

    private String buildTextPayload(String waId, String wamid, String text) {
        return """
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "id": "WABAID",
                "changes": [{
                  "field": "messages",
                  "value": {
                    "messaging_product": "whatsapp",
                    "messages": [{
                      "from": "%s",
                      "id": "%s",
                      "timestamp": "1234567890",
                      "type": "text",
                      "text": {"body": "%s"}
                    }]
                  }
                }]
              }]
            }
            """.formatted(waId, wamid, text);
    }
}
