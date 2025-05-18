package br.com.satyan.stering.saita.financasbottelegram.adapters.in.controller;

import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TelegramWebhookController {

    private final String TELEGRAM_TOKEN = "7632697875:AAEN6LRjPK1fLdMUhlV_cdNWxs0Gw4QyWao";

    @PostMapping("/webhook")
    public String receberMensagem(@RequestBody String payload) {
        JSONObject json = new JSONObject(payload);
        long chatId = json.getJSONObject("message").getJSONObject("chat").getLong("id");

        String resposta = "Olá, natsu cara de cu!";
        enviarMensagemTelegram(chatId, resposta);

        return "ok";
    }

    private void enviarMensagemTelegram(long chatId, String texto) {
        String url = "https://api.telegram.org/bot" + TELEGRAM_TOKEN + "/sendMessage";
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", texto);

        restTemplate.postForObject(url, body, String.class);
    }
}
