package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.TelegramPortIn;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DownloadImageService implements TelegramPortIn {

    private final String TELEGRAM_TOKEN = System.getenv("TELEGRAM_TOKEN");
    private final String TELEGRAM_API_URL = "https://api.telegram.org/bot" + TELEGRAM_TOKEN + "/getFile?file_id=";

    @Override
    public byte[] getFile(String fileId) {
        String url = TELEGRAM_API_URL + fileId;

        String getFileUrl = "https://api.telegram.org/bot" + TELEGRAM_TOKEN + "/getFile?file_id=" + fileId;
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(getFileUrl, String.class);
        String filePath = new JSONObject(response).getJSONObject("result").getString("file_path");

        String downloadUrl = "https://api.telegram.org/file/bot" + TELEGRAM_TOKEN + "/" + filePath;

        byte[] imageBytes = restTemplate.getForObject(downloadUrl, byte[].class);

        return imageBytes;
    }
}
