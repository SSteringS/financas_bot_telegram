package br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramMessageSenderService {

  private static final Logger logger = LoggerFactory.getLogger(TelegramMessageSenderService.class);

  private final RestClient restClient;
  private final String botToken;
  private final String telegramApiUrl;

  public TelegramMessageSenderService(RestClient restClient,
      @Value("${telegram.bot-token}") String botToken,
      @Value("${telegram.api.url}") String telegramApiUrl) {
    this.restClient = restClient;
    this.botToken = botToken;
    this.telegramApiUrl = telegramApiUrl;
  }

  public void sendMessage(Long chatId, String text) {
    String fullUrl = telegramApiUrl + botToken + "/sendMessage";
    try {
      String response = restClient.get()
          .uri(fullUrl + "?chat_id={chatId}&text={text}", chatId, text)
          .retrieve()
          .body(String.class);
      logger.info("Resposta do sendMessage: {}", response);
    } catch (Exception e) {
      logger.error("Erro ao enviar mensagem para o chat ID {}: {}", chatId, e.getMessage());
    }
  }
}
