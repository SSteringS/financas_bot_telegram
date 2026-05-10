package br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramFileDownloaderService {

  private static final Logger logger = LoggerFactory.getLogger(TelegramFileDownloaderService.class);

  private final RestClient restClient;
  private final String telegramApiUrl;
  private final String botToken;

  public TelegramFileDownloaderService(RestClient restClient,
      @Value("${telegram.api.url}") String telegramApiUrl,
      @Value("${telegram.bot-token}") String botToken) {
    this.restClient = restClient;
    this.telegramApiUrl = telegramApiUrl;
    this.botToken = botToken;
  }

  public byte[] downloadImageByFileId(String fileId) {
    try {
      logger.info("Iniciando download da imagem com file_id: {}", fileId);

      String getFileUrl = telegramApiUrl + botToken + "/getFile?file_id=" + fileId;
      String filePathResponse = restClient.get()
          .uri(getFileUrl)
          .retrieve()
          .body(String.class);

      String filePath = extractFilePathFromJson(filePathResponse);
      String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;

      byte[] imageBytes = restClient.get()
          .uri(downloadUrl)
          .retrieve()
          .body(byte[].class);

      logger.info("Imagem baixada com sucesso. Tamanho: {} bytes", imageBytes.length);
      return imageBytes;

    } catch (Exception e) {
      logger.error("Erro ao fazer download da imagem com file_id: {}", fileId, e);
      throw new TelegramFileDownloadException("Falha ao baixar imagem do Telegram: " + e.getMessage(), e);
    }
  }

  private String extractFilePathFromJson(String jsonResponse) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"file_path\":\"([^\"]+)\"");
    java.util.regex.Matcher matcher = pattern.matcher(jsonResponse);
    if (matcher.find()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException("Não foi possível extrair file_path da resposta do Telegram");
  }
}
