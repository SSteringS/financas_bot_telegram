package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.TelegramPortIn;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

@Service
public class DownloadImageService implements TelegramPortIn {

  @Value("${telegram.api.url}")
  private String telegramApiUrl;

  @Value("${telegram.api.file.url}")
  private String telegramApiFileUrl;

  private final String TELEGRAM_TOKEN = System.getenv("TELEGRAM_TOKEN");

  @Override
  public byte[] getFile(String fileId) {

    String filePath = getFilePath(fileId);

    String downloadUrl = getDownloadUrl(filePath);

    RestTemplate restTemplate = new RestTemplate();

    return restTemplate.getForObject(downloadUrl, byte[].class);
  }

  public String getFilePath(String fileId) {
    String getFileUrl = getTelegramApiUrlWithToken() + fileId;
    RestTemplate restTemplate = new RestTemplate();
    String response = restTemplate.getForObject(getFileUrl, String.class);
    return new JSONObject(response).getJSONObject("result").getString("file_path");
  }

  private String getDownloadUrl(String filePath) {
    return String.format("%s%s/%s", telegramApiFileUrl, TELEGRAM_TOKEN, filePath);
  }

  private String getTelegramApiUrlWithToken() {
    return String.format("%s%s/getFile?file_id=", telegramApiUrl, TELEGRAM_TOKEN);
  }

}
