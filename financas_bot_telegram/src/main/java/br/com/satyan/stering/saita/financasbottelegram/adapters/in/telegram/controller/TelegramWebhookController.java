package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.controller;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.UpdateOrchestratorService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class TelegramWebhookController {

  private static final Logger logger = LoggerFactory.getLogger(TelegramWebhookController.class);

  private final UpdateOrchestratorService updateOrchestratorService;
  private final List<String> allowedUserIds;

  public TelegramWebhookController(UpdateOrchestratorService updateOrchestratorService,
      @Value("${telegram.allowed-user-ids}") List<String> allowedUserIds) {
    this.updateOrchestratorService = updateOrchestratorService;
    this.allowedUserIds = allowedUserIds;
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> receberMensagem(@RequestBody Update update) {
    logger.info("Recebendo mensagem do Telegram: {}", update);

    if (!update.hasMessage() || update.getMessage().getFrom() == null) {
      logger.warn("Update recebido sem 'message' ou 'from'. Ignorando.");
      return ResponseEntity.ok().build();
    }

    String userId = update.getMessage().getFrom().getId().toString();

    if (!allowedUserIds.contains(userId)) {
      logger.warn("Usuário com ID {} não autorizado. Ignorando mensagem.", userId);
      return ResponseEntity.ok().build(); //
    }

    updateOrchestratorService.process(update);
    logger.info("Mensagem do usuário {} processada com sucesso.", userId);

    return ResponseEntity.ok().build();
  }



}


