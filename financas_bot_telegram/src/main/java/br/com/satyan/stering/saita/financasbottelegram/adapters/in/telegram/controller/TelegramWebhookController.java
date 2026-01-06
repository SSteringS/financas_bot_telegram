package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.controller;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.UpdateOrchestratorService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidUpdateException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.UnauthorizedUserException;
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

    validateRequest(update);
    authorizeUser(update);

    updateOrchestratorService.process(update);
    logger.info("Mensagem do usuário {} processada com sucesso.", update.getMessage().getFrom().getId());

    return ResponseEntity.ok().build();
  }

  private void authorizeUser(Update update) {
    String userId = update.getMessage().getFrom().getId().toString();
    if (!allowedUserIds.contains(userId)) {
      throw new UnauthorizedUserException("Usuário não autorizado.", update.getMessage().getChatId());
    }
  }

  private void validateRequest(Update update) {
    if (!update.hasMessage() || update.getMessage().getFrom() == null) {
      throw new InvalidUpdateException("Update recebido sem 'message' ou 'from'.");
    }
  }



}

