package br.com.satyan.stering.saita.financasbottelegram.adapter.in.telegram.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.ApiTelegramClientException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.DownloadImageService;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class DownloadImageServiceTest {

  private DownloadImageService service;
  private WebClient webClientMock;

  @BeforeEach
  void setUp() {
    service = new DownloadImageService();
    webClientMock = mock(WebClient.class, RETURNS_DEEP_STUBS);
    ReflectionTestUtils.setField(service, "webClient", webClientMock);
    ReflectionTestUtils.setField(service, "telegramApiUrl", "https://api.telegram.org/bot");
    ReflectionTestUtils.setField(service, "telegramApiFileUrl", "https://api.telegram.org/file/bot");
  }

  @Test
  @DisplayName("Deve retornar bytes ao baixar arquivo com sucesso")
  void deveRetornarBytesAoBaixarArquivo() {
    byte[] expectedBytes = new byte[]{1, 2, 3};
    when(webClientMock.get().uri(any(String.class)).retrieve().bodyToMono(byte[].class)).thenReturn(
        Mono.just(expectedBytes));

    ReflectionTestUtils.setField(service, "telegramApiUrl", "https://api.telegram.org/bot");
    ReflectionTestUtils.setField(service, "telegramApiFileUrl", "https://api.telegram.org/file/bot");
    // Mock getFilePath para retornar um caminho fixo
    DownloadImageService spyService = Mockito.spy(service);
    doReturn("file_path.jpg").when(spyService).getFilePath(any());
    byte[] result = spyService.getFile("fileId");
    assertArrayEquals(expectedBytes, result);

  }

  @Test
  @DisplayName("Deve lançar ApiTelegramClientException ao receber erro HTTP em getFile")
  void deveLancarExcecaoAoErroHttpGetFile() {
    when(webClientMock.get().uri(any(String.class)).retrieve().bodyToMono(byte[].class))
        .thenThrow(WebClientResponseException.create(404, "Not Found", null, null, null));

    DownloadImageService spyService = Mockito.spy(service);
    doReturn("file_path.jpg").when(spyService).getFilePath(any());
    assertThrows(ApiTelegramClientException.class, () -> spyService.getFile("fileId"));

  }

  @Test
  @DisplayName("Deve retornar file_path ao buscar getFilePath com sucesso")
  void deveRetornarFilePathComSucesso() throws JSONException {
    String response = new JSONObject().put("result", new JSONObject().put("file_path", "abc/def.jpg")).toString();
    when(webClientMock.get().uri(any(String.class)).retrieve().bodyToMono(String.class)).thenReturn(Mono.just(response));
    String filePath = service.getFilePath("fileId");
    assertEquals("abc/def.jpg", filePath);
  }

  @Test
  @DisplayName("Deve lançar ApiTelegramClientException ao receber erro HTTP em getFilePath")
  void deveLancarExcecaoAoErroHttpGetFilePath() {
    when(webClientMock.get().uri(any(String.class)).retrieve().bodyToMono(String.class))
        .thenThrow(WebClientResponseException.create(404, "Not Found", null, null, null));
    assertThrows(ApiTelegramClientException.class, () -> service.getFilePath("fileId"));

  }
}
