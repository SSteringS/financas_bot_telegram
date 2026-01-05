package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.controller;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.ProcessPaymentMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TelegramWebhookController {

  private static final Logger logger = LoggerFactory.getLogger(TelegramWebhookController.class);

  private final ProcessPaymentMessageService processPaymentMessageService;

  public TelegramWebhookController(ProcessPaymentMessageService processPaymentMessageService) {
    this.processPaymentMessageService = processPaymentMessageService;
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> receberMensagem(@RequestBody String payload) {

    logger.info("Recebendo mensagem do Telegram: {}", payload);
    processPaymentMessageService.processPaymentMessage(payload);
    logger.info("Mensagem processada com sucesso.");

    return ResponseEntity.ok().build();
  }



}


