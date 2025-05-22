package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.controller.TelegramWebhookController;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.ApiTelegramClientException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.TelegramPortIn;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class DownloadImageService implements TelegramPortIn {

  @Value("${telegram.api.url}")
  private String telegramApiUrl;

  @Value("${telegram.api.file.url}")
  private String telegramApiFileUrl;

  private final String TELEGRAM_TOKEN = System.getenv("TELEGRAM_TOKEN");
  private static final Logger logger = LoggerFactory.getLogger(DownloadImageService.class);
  private final WebClient webClient = WebClient.create();

  @Override
public byte[] getFile(String fileId) {
      String filePath = getFilePath(fileId);
      String downloadUrl = getDownloadUrl(filePath);

      try {
        return webClient.get()
            .uri(downloadUrl)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
      } catch (WebClientResponseException e) {
        logger.error("Erro HTTP baixar o arquivo do Telegram: " + e.getStatusCode(), e);
        throw new ApiTelegramClientException("Erro HTTP ao baixar o arquivo do Telegram: " + e.getStatusCode(), e);
      } catch (Exception e) {
        logger.error("Erro genérico ao arquivo do Telegram: " + e.getMessage(), e);
        throw new RuntimeException("Erro HTTP ao baixar o arquivo do Telegram: " + e.getMessage(), e);
      }
    }

  public String getFilePath(String fileId) {
    String getFileUrl = getTelegramApiUrlWithToken() + fileId;
    try {
      String response = webClient.get()
          .uri(getFileUrl)
          .retrieve()
          .bodyToMono(String.class)
          .block();
      return new JSONObject(response).getJSONObject("result").getString("file_path");
    } catch (WebClientResponseException e) {
      logger.error("Erro HTTP ao buscar file_path do Telegram: " + e.getStatusCode(), e);
      throw new ApiTelegramClientException("Erro HTTP ao buscar file_path do Telegram: " + e.getStatusCode(), e);
    } catch (Exception e) {
      logger.error("Erro genérico ao buscar file_path do Telegram: " + e.getMessage(), e);
      throw new RuntimeException("Erro ao buscar file_path do Telegram: " + e.getMessage(), e);
    }
  }

  private String getDownloadUrl(String filePath) {
    return String.format("%s%s/%s", telegramApiFileUrl, TELEGRAM_TOKEN, filePath);
  }

  private String getTelegramApiUrlWithToken() {
    return String.format("%s%s/getFile?file_id=", telegramApiUrl, TELEGRAM_TOKEN);
  }

}
