package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto.TemplateParametro;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto.WhatsAppSendResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(WhatsAppMessageSenderService.class)
@TestPropertySource(properties = {
    "whatsapp.graph-api-base-url=https://graph.facebook.com",
    "whatsapp.graph-api-version=v20.0",
    "whatsapp.phone-number-id=TEST_PHONE_ID",
    "whatsapp.access-token=TEST_TOKEN"
})
class WhatsAppMessageSenderServiceTest {

    @Autowired
    private WhatsAppMessageSenderService service;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    void enviarTexto_fazPostParaEndpointCorretoComAuthorization() {
        mockServer.expect(requestTo("https://graph.facebook.com/v20.0/TEST_PHONE_ID/messages"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer TEST_TOKEN"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{\"type\":\"text\",\"to\":\"5511999999999\"}"))
            .andRespond(withSuccess(
                "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.ABC\"}]}",
                MediaType.APPLICATION_JSON));

        WhatsAppSendResponse response = service.enviarTexto("5511999999999", "Olá!");

        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).id()).isEqualTo("wamid.ABC");
        mockServer.verify();
    }

    @Test
    void enviarTemplate_fazPostComTypeTemplate() {
        mockServer.expect(requestTo("https://graph.facebook.com/v20.0/TEST_PHONE_ID/messages"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("{\"type\":\"template\",\"to\":\"5511999999999\"}"))
            .andRespond(withSuccess(
                "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.DEF\"}]}",
                MediaType.APPLICATION_JSON));

        service.enviarTemplate(
            "5511999999999",
            "hello_world",
            "pt_BR",
            List.of(new TemplateParametro("text", "João")));

        mockServer.verify();
    }

    @Test
    void enviarTexto_erroHttp400ComCodigoMeta_lancaExcecaoComCodigo() {
        mockServer.expect(requestTo("https://graph.facebook.com/v20.0/TEST_PHONE_ID/messages"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":130497,\"message\":\"Re-engagement message\",\"type\":\"OAuthException\"}}"));

        assertThatThrownBy(() -> service.enviarTexto("5511999999999", "msg"))
            .isInstanceOf(WhatsAppApiException.class)
            .satisfies(ex -> {
                WhatsAppApiException waEx = (WhatsAppApiException) ex;
                assertThat(waEx.getErrorCode()).isEqualTo(130497);
                assertThat(waEx.getHttpStatus()).isEqualTo(400);
            });
    }

    @Test
    void enviarTexto_erroHttp401_lancaExcecaoComCodigoOAuth() {
        mockServer.expect(requestTo("https://graph.facebook.com/v20.0/TEST_PHONE_ID/messages"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":190,\"message\":\"Invalid OAuth access token.\",\"type\":\"OAuthException\"}}"));

        assertThatThrownBy(() -> service.enviarTexto("5511999999999", "msg"))
            .isInstanceOf(WhatsAppApiException.class)
            .satisfies(ex -> {
                WhatsAppApiException waEx = (WhatsAppApiException) ex;
                assertThat(waEx.getHttpStatus()).isEqualTo(401);
                assertThat(waEx.getErrorCode()).isEqualTo(190);
            });
    }

    @Test
    void enviarTexto_erroHttp500_lancaExcecaoSemCodigoMeta() {
        mockServer.expect(requestTo("https://graph.facebook.com/v20.0/TEST_PHONE_ID/messages"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error"));

        assertThatThrownBy(() -> service.enviarTexto("5511999999999", "msg"))
            .isInstanceOf(WhatsAppApiException.class)
            .satisfies(ex -> {
                WhatsAppApiException waEx = (WhatsAppApiException) ex;
                assertThat(waEx.getHttpStatus()).isEqualTo(500);
                assertThat(waEx.getErrorCode()).isNull();
            });
    }
}
