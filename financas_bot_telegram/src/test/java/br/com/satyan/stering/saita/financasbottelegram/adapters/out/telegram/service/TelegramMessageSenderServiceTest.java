package br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/**
 * Testes unitários de {@link TelegramMessageSenderService}.
 *
 * <p>Verifica que a URL correta é construída ao enviar mensagens e que o
 * método {@code enviar} do port {@code CanalNotificadorPort} delega corretamente.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class TelegramMessageSenderServiceTest {

    private static final String BOT_TOKEN   = "test-bot-token";
    private static final String API_URL     = "https://api.telegram.org/bot";
    private static final String SEND_MSG_RESPONSE = "{\"ok\":true,\"result\":{\"message_id\":1}}";

    @Mock RestClient restClient;
    @Mock RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock RestClient.RequestHeadersSpec    requestHeadersSpec;
    @Mock RestClient.ResponseSpec          responseSpec;

    private TelegramMessageSenderService service;

    @BeforeEach
    void setUp() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        // uri(String template, Object... uriVars) — chatId é Long, text é String
        when(requestHeadersUriSpec.uri(anyString(), any(), any()))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(SEND_MSG_RESPONSE);

        service = new TelegramMessageSenderService(restClient, BOT_TOKEN, API_URL);
    }

    @Test
    void deveConstruirUrlCorretaParaSendMessage() {
        service.sendMessage(123456L, "Olá!");

        // URL capturada deve conter token e sendMessage
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture(), any(), any());

        String urlTemplate = urlCaptor.getValue();
        assertThat(urlTemplate)
                .contains(BOT_TOKEN)
                .contains("/sendMessage")
                .contains("{chatId}")
                .contains("{text}");
    }

    @Test
    void deveUsarApiUrlConfiguravel() {
        String customUrl = "http://wiremock:8089/bot";
        TelegramMessageSenderService customService =
                new TelegramMessageSenderService(restClient, BOT_TOKEN, customUrl);

        customService.sendMessage(999L, "Teste WireMock");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture(), any(), any());

        assertThat(urlCaptor.getValue())
                .startsWith(customUrl)
                .doesNotContain("api.telegram.org");
    }

    @Test
    void deveImplementarPortCanalNotificador() {
        // enviar() delega para sendMessage() — verifica via port
        service.enviar(77777L, "Mensagem via port");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture(), any(), any());

        assertThat(urlCaptor.getValue()).contains("/sendMessage");
    }

    @Test
    void deveContinuarSemLancarExcecaoQuandoApiRetornaErro() {
        // TelegramMessageSenderService captura exceções e apenas loga — não propaga
        when(responseSpec.body(String.class))
                .thenThrow(new RuntimeException("Timeout"));

        // Não deve lançar exceção — falha silenciosa (canal best-effort)
        service.sendMessage(123L, "msg que vai falhar");
    }
}
