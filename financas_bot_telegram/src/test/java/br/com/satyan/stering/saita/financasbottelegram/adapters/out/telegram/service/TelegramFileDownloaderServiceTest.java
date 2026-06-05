package br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/**
 * Testes unitários de {@link TelegramFileDownloaderService}.
 *
 * <p>Foco: verificar que {@code telegram.api.file.url} é usada na URL de download
 * (não hardcoded), permitindo que WireMock intercepte a chamada em testes E2E.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class TelegramFileDownloaderServiceTest {

    private static final String API_URL  = "https://api.telegram.org/bot";
    private static final String FILE_URL = "https://api.telegram.org/file/bot";
    private static final String BOT_TOKEN = "test-token";

    private static final String GET_FILE_RESPONSE =
            "{\"ok\":true,\"result\":{\"file_id\":\"abc\",\"file_path\":\"photos/file_0.jpg\"}}";

    @Mock RestClient restClient;
    @Mock RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock RestClient.RequestHeadersSpec    requestHeadersSpec;
    @Mock RestClient.ResponseSpec          responseSpec;

    @BeforeEach
    void setUp() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(GET_FILE_RESPONSE);
        when(responseSpec.body(byte[].class)).thenReturn(new byte[]{1, 2, 3});
    }

    @Test
    void deveUsarFileUrlPadraoNaUrlDeDownload() {
        TelegramFileDownloaderService service = new TelegramFileDownloaderService(
                restClient, API_URL, BOT_TOKEN, FILE_URL);

        service.downloadImageByFileId("abc");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec, org.mockito.Mockito.times(2)).uri(urlCaptor.capture());

        String downloadUrl = urlCaptor.getAllValues().get(1);
        assertThat(downloadUrl)
                .startsWith(FILE_URL)
                .contains(BOT_TOKEN)
                .endsWith("photos/file_0.jpg");
    }

    @Test
    void deveUsarFileUrlCustomizadaParaWireMock() {
        // Simula o que WireMock E2E vai injetar via env var TELEGRAM_API_FILE_URL
        String wireMockFileUrl = "http://localhost:8089/file/bot";

        TelegramFileDownloaderService service = new TelegramFileDownloaderService(
                restClient, API_URL, BOT_TOKEN, wireMockFileUrl);

        service.downloadImageByFileId("abc");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec, org.mockito.Mockito.times(2)).uri(urlCaptor.capture());

        String downloadUrl = urlCaptor.getAllValues().get(1);
        assertThat(downloadUrl)
                .startsWith(wireMockFileUrl)
                .doesNotContain("api.telegram.org");
    }
}
