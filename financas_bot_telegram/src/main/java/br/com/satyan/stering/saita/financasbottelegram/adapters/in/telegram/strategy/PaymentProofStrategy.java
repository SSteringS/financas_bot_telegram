package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoUploadS3;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Document;
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

    ExtraidoArquivo extraido = extrair(message, chatId);

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

    logger.info("Estratégia de Comprovante de Pagamento ativada. FileID: {}, Legenda: '{}'", extraido.fileId(), caption);

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

    byte[] bytes = telegramFileDownloaderService.downloadImageByFileId(extraido.fileId());

    String s3Url = s3ImageUploadService.uploadFile(bytes, extraido.extensao(), TipoUploadS3.COMPROVANTE);
    logger.info("Comprovante enviado para S3: {}", s3Url);

    Comprovante comprovanteSalvo = registrarComprovanteUsecase.execute(
        pedidoId, tipoPagamento, extraido.fileId(), s3Url, extraido.tipo(), chatId);
    logger.info("Comprovante para o pedido {} registrado com sucesso.", pedidoId);

    String successMessage = String.format(
        "✅ Comprovante registrado!\n\n*Pedido:* #%d\n*Tipo:* %s",
        comprovanteSalvo.getPedidoId(),
        comprovanteSalvo.getTipoPagamento()
    );
    telegramMessageSenderService.sendMessage(chatId, successMessage);
  }

  private ExtraidoArquivo extrair(Message message, Long chatId) {
    if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
      String fileId = message.getPhoto().stream()
          .max(Comparator.comparing(PhotoSize::getFileSize))
          .map(PhotoSize::getFileId)
          .orElseThrow(() -> new PhotoProcessingException("Foto sem file_id válido.", chatId));
      return new ExtraidoArquivo(fileId, TipoArquivo.IMAGEM, "jpg");
    }

    if (message.getDocument() != null) {
      Document doc = message.getDocument();
      String mime = doc.getMimeType() != null ? doc.getMimeType() : "";

      if (mime.startsWith("image/")) {
        return new ExtraidoArquivo(doc.getFileId(), TipoArquivo.IMAGEM, extensaoDeMime(mime));
      }
      if (mime.equals("application/pdf")) {
        return new ExtraidoArquivo(doc.getFileId(), TipoArquivo.PDF, "pdf");
      }
      if (mime.equals("application/octet-stream") || mime.isBlank()) {
        return new ExtraidoArquivo(doc.getFileId(), TipoArquivo.IMAGEM, "jpg");
      }

      throw new TipoArquivoNaoSuportadoException(
          "Tipo de arquivo '" + mime + "' não suportado. Envie foto, imagem ou PDF.", chatId);
    }

    throw new PhotoProcessingException(
        "Nenhuma imagem ou anexo encontrado. Envie como foto ou anexo.", chatId);
  }

  private record ExtraidoArquivo(String fileId, TipoArquivo tipo, String extensao) {}

  private String extensaoDeMime(String mime) {
    return switch (mime) {
      case "image/jpeg", "image/jpg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      case "image/gif" -> "gif";
      default -> "jpg";
    };
  }
}
