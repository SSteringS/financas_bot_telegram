package br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class TelegramMessageSenderService {

  private static final Logger logger = LoggerFactory.getLogger(TelegramMessageSenderService.class);

  private final String botToken;
  private final WebClient webClient;
  private final String telegramApiUrl;

  public TelegramMessageSenderService(@Value("${telegram.bot-token}") String botToken, WebClient webClient,
                                     @Value("${telegram.api.url}") String telegramApiUrl) {
    this.webClient = webClient;
    this.botToken = botToken;
    this.telegramApiUrl = telegramApiUrl;
  }

  public void sendMessage(Long chatId, String text) {
    String fullUrl = telegramApiUrl + botToken + "/sendMessage";

  webClient.get()
      .uri(fullUrl, uriBuilder -> uriBuilder
          .queryParam("chat_id", chatId)
          .queryParam("text", text)
          .build())
      .retrieve()
      .bodyToMono(String.class)
      .doOnSuccess(response -> logger.info("Resposta do sendMessage da API do Telegram: {}", response))
      .doOnError(error -> logger.error("Erro ao enviar mensagem para o chat ID {}: {}", chatId, error.getMessage()))
      .subscribe();
  }

}
