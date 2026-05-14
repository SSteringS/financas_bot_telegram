package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PaymentProofStrategy implements UpdateProcessingStrategy {

  private static final Logger logger = LoggerFactory.getLogger(PaymentProofStrategy.class);
  private final RegistrarComprovanteUsecase registrarComprovanteUsecase;
  private final TelegramMessageSenderService telegramMessageSenderService;
  private final S3ImageUploadService s3ImageUploadService;
  private final TelegramFileDownloaderService telegramFileDownloaderService;

  // Expressão regular para capturar o tipo de pagamento e o ID do pedido.
  // Ex: "#123 pix"
  private static final Pattern COMPROVANTE_PATTERN = Pattern.compile("#(\\d+)\\s+(.+)");

  public PaymentProofStrategy(RegistrarComprovanteUsecase registrarComprovanteUsecase,
      TelegramMessageSenderService telegramMessageSenderService,
      S3ImageUploadService s3ImageUploadService,
      TelegramFileDownloaderService telegramFileDownloaderService) {
    this.registrarComprovanteUsecase = registrarComprovanteUsecase;
    this.telegramMessageSenderService = telegramMessageSenderService;
    this.s3ImageUploadService = s3ImageUploadService;
    this.telegramFileDownloaderService = telegramFileDownloaderService;
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

    String fileId = extractHighestQualityImageFileId(message, chatId);

    if (caption == null || caption.isBlank()) {
      throw new InvalidCaptionException(
          "A foto do comprovante precisa de uma legenda.\n\n" +
          "Use: `#<id_pedido> <tipo_pagamento>`\n\n" +
          "*Exemplos:*\n" +
          "• `#123 pix`\n" +
          "• `#456 boleto`\n" +
          "• `#789 ted`\n\n" +
          "O `<id_pedido>` é o número que apareceu quando você registrou o pedido.", chatId);
    }

    logger.info("Estratégia de Comprovante de Pagamento ativada. FileID: {}, Legenda: '{}'", fileId, caption);

    Matcher matcher = COMPROVANTE_PATTERN.matcher(caption.trim());
    if (!matcher.matches()) {
      throw new InvalidCaptionException(
          "Formato da legenda inválido.\n\n" +
          "Use: `#<id_pedido> <tipo_pagamento>`\n\n" +
          "*Exemplos:*\n" +
          "• `#123 pix`\n" +
          "• `#456 boleto`\n" +
          "• `#789 ted`\n\n" +
          "O `<id_pedido>` é o número que apareceu quando você registrou o pedido.", chatId);
    }

    Long pedidoId = Long.parseLong(matcher.group(1));
    String tipoPagamento = matcher.group(2).toUpperCase();

    // Download da imagem do Telegram
    byte[] imageBytes = telegramFileDownloaderService.downloadImageByFileId(fileId);

    // Upload para S3
    String s3ImageUrl = s3ImageUploadService.uploadImage(imageBytes);
    logger.info("Comprovante enviado para S3: {}", s3ImageUrl);

    // Salvar comprovante no banco

    Comprovante comprovanteSalvo = registrarComprovanteUsecase.execute(pedidoId, tipoPagamento, fileId, s3ImageUrl, chatId);
    logger.info("Comprovante para o pedido {} registrado com sucesso.", pedidoId);

    String successMessage = String.format(
        "✅ Comprovante registrado!\n\n*Pedido:* #%d\n*Tipo:* %s",
        comprovanteSalvo.getPedidoId(),
        comprovanteSalvo.getTipoPagamento()
    );
    telegramMessageSenderService.sendMessage(chatId, successMessage);
  }

  /**
   * Extrai o file_id da foto com maior qualidade (maior tamanho).
   */
  private String extractHighestQualityImageFileId(Message message, Long chatId) {
    return message.getPhoto().stream()
        .max(Comparator.comparing(PhotoSize::getFileSize))
        .map(PhotoSize::getFileId)
        .orElseThrow(() -> new PhotoProcessingException("Não foi possível obter o file_id da foto.", chatId));
  }
}