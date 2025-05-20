package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.controller;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.ProcessPaymentMessageService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TelegramWebhookController {

  private static final Logger logger = LoggerFactory.getLogger(TelegramWebhookController.class);

  private final String TELEGRAM_TOKEN = System.getenv("TELEGRAM_TOKEN");
  private ProcessPaymentMessageService processPaymentMessageService;

  public TelegramWebhookController(ProcessPaymentMessageService processPaymentMessageService) {
    this.processPaymentMessageService = processPaymentMessageService;
  }

  @PostMapping("/webhook")
  public String receberMensagem(@RequestBody String payload) {

    logger.info("Recebendo mensagem do Telegram: {}", payload);
    processPaymentMessageService.processPaymentMessage(payload);

    JSONObject json = new JSONObject(payload);
    long chatId = json.getJSONObject("message").getJSONObject("chat").getLong("id");
    String resposta = "Olá, pagoto cara de cu!";
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
