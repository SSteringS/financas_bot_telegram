package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Comprovante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PaymentProofStrategy implements UpdateProcessingStrategy {

  private static final Logger logger = LoggerFactory.getLogger(PaymentProofStrategy.class);
  private final RegistrarComprovanteUsecase registrarComprovanteUsecase;
  private final TelegramMessageSenderService telegramMessageSenderService;

  // Expressão regular para capturar o tipo de pagamento e o ID do pedido.
  // Ex: "pix 123"
  private static final Pattern COMPROVANTE_PATTERN = Pattern.compile("#(\\d+)\\s+(.+)");

  public PaymentProofStrategy(RegistrarComprovanteUsecase registrarComprovanteUsecase, TelegramMessageSenderService telegramMessageSenderService) {
    this.registrarComprovanteUsecase = registrarComprovanteUsecase;
    this.telegramMessageSenderService = telegramMessageSenderService;
  }

  @Override
  public boolean supports(Update update) {

    String caption = update.getMessage().getCaption().trim();
    return COMPROVANTE_PATTERN.matcher(caption).matches();
  }

  @Override
  public void process(Update update) {
    Message message = update.getMessage();
    Long chatId = message.getChatId();
    String caption = message.getCaption();

    String fileId = message.getPhoto().stream()
        .max(Comparator.comparing(PhotoSize::getFileSize))
        .map(PhotoSize::getFileId)
        .orElseThrow(() -> new PhotoProcessingException("Não foi possível obter o file_id da foto.", chatId));

    if (caption == null || caption.isBlank()) {
      throw new InvalidCaptionException("A foto do comprovante precisa de uma legenda.\nUse: `<tipo_pagamento> <id_pedido>`\n\n*Exemplo:* `pix 123`", chatId);
    }

    logger.info("Estratégia de Comprovante de Pagamento ativada. FileID: {}, Legenda: '{}'", fileId, caption);

    Matcher matcher = COMPROVANTE_PATTERN.matcher(caption.trim());
    if (!matcher.matches()) {
      throw new InvalidCaptionException("Formato da legenda inválido.\nUse: `<tipo_pagamento> <id_pedido>`\n\n*Exemplo:* `pix 123`", chatId);
    }

    String tipoPagamento = matcher.group(2).toUpperCase();
    Long pedidoId = Long.parseLong(matcher.group(1));

    Comprovante comprovanteSalvo = registrarComprovanteUsecase.execute(pedidoId, tipoPagamento, fileId, chatId);
    logger.info("Comprovante para o pedido {} registrado com sucesso.", pedidoId);

    String successMessage = String.format(
        "✅ Comprovante de pagamento para o *Pedido ID %d* foi registrado com sucesso!",
        comprovanteSalvo.getPedido().getId()
    );
    telegramMessageSenderService.sendMessage(chatId, successMessage);
  }
}