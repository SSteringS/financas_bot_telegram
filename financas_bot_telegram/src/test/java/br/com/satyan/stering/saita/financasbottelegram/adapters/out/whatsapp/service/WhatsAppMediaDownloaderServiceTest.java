package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(WhatsAppMediaDownloaderService.class)
@TestPropertySource(properties = {
    "whatsapp.graph-api-base-url=https://graph.facebook.com",
    "whatsapp.graph-api-version=v20.0",
    "whatsapp.phone-number-id=TEST_PHONE_ID",
    "whatsapp.access-token=TEST_TOKEN"
})
class WhatsAppMediaDownloaderServiceTest {

    @Autowired
    private WhatsAppMediaDownloaderService service;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    void baixar_fazDoisGetsComAuthorization() {
        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};

        // 1º GET: busca info da mídia (URL temporária)
        mockServer.expect(requestTo("https://graph.facebook.com/v20.0/12345"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer TEST_TOKEN"))
            .andRespond(withSuccess(
                "{\"url\":\"https://lookaside.fbsbx.com/test-media\",\"mime_type\":\"image/jpeg\",\"file_size\":5,\"id\":\"12345\"}",
                MediaType.APPLICATION_JSON));

        // 2º GET: baixa os bytes usando a URL temporária
        mockServer.expect(requestTo("https://lookaside.fbsbx.com/test-media"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer TEST_TOKEN"))
            .andRespond(withSuccess(new ByteArrayResource(imageBytes), MediaType.IMAGE_JPEG));

        byte[] resultado = service.baixar("12345");

        assertThat(resultado).isEqualTo(imageBytes);
        mockServer.verify();
    }

    @Test
    void baixar_erroNoPrimeiroGet_lancaWhatsAppApiException() {
        mockServer.expect(requestTo("https://graph.facebook.com/v20.0/INVALID_ID"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":100,\"message\":\"Media ID not found\",\"type\":\"GraphMethodException\"}}"));

        assertThatThrownBy(() -> service.baixar("INVALID_ID"))
            .isInstanceOf(WhatsAppApiException.class)
            .satisfies(ex -> {
                WhatsAppApiException waEx = (WhatsAppApiException) ex;
                assertThat(waEx.getHttpStatus()).isEqualTo(404);
                assertThat(waEx.getErrorCode()).isEqualTo(100);
            });
    }

    @Test
    void baixar_erroNoSegundoGet_lancaWhatsAppApiException() {
        // 1º GET: sucesso — retorna URL
        mockServer.expect(requestTo("https://graph.facebook.com/v20.0/99999"))
            .andRespond(withSuccess(
                "{\"url\":\"https://lookaside.fbsbx.com/expired-media\",\"id\":\"99999\"}",
                MediaType.APPLICATION_JSON));

        // 2º GET: falha (URL expirada)
        mockServer.expect(requestTo("https://lookaside.fbsbx.com/expired-media"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN)
                .body("Forbidden"));

        assertThatThrownBy(() -> service.baixar("99999"))
            .isInstanceOf(WhatsAppApiException.class)
            .satisfies(ex -> {
                assertThat(((WhatsAppApiException) ex).getHttpStatus()).isEqualTo(403);
            });
    }
}
