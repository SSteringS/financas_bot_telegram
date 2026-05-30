package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.controller;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidUpdateException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.mapper.TelegramMessageMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.UnauthorizedUserException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.MensagemEntrantePortIn;
import jakarta.servlet.http.HttpServletRequest;
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

  private final TelegramMessageMapper telegramMessageMapper;
  private final MensagemEntrantePortIn mensagemEntrantePortIn;
  private final List<String> allowedUserIds;

  public TelegramWebhookController(TelegramMessageMapper telegramMessageMapper,
      MensagemEntrantePortIn mensagemEntrantePortIn,
      @Value("${telegram.allowed-user-ids}") List<String> allowedUserIds) {
    this.telegramMessageMapper = telegramMessageMapper;
    this.mensagemEntrantePortIn = mensagemEntrantePortIn;
    this.allowedUserIds = allowedUserIds;
  }

  @PostMapping("/webhook/telegram")
  public ResponseEntity<Void> receberMensagem(@RequestBody Update update, HttpServletRequest request) {
    logger.info("Recebendo mensagem do Telegram: {}", update);
    request.setAttribute("__update", update);

    validateRequest(update);
    authorizeUser(update);

    PaymentMessageDTO dto = telegramMessageMapper.toPaymentMessageDTO(update);
    mensagemEntrantePortIn.processar(dto);
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
