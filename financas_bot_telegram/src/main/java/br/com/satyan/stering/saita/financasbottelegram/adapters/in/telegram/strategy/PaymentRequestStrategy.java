package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.service.LegendaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PaymentRequestStrategy implements UpdateProcessingStrategy {

  private static final Logger logger = LoggerFactory.getLogger(PaymentRequestStrategy.class);
  private final SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase;
  private final TelegramMessageSenderService telegramMessageSenderService;
  private final S3ImageUploadService s3ImageUploadService;
  private final TelegramFileDownloaderService telegramFileDownloaderService;

  private static final Pattern PEDIDO_PATTERN = Pattern.compile("^(\\d+([.,]\\d{1,2})?)\\s+(.+)$");

  public PaymentRequestStrategy(SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase,
      TelegramMessageSenderService telegramMessageSenderService,
      S3ImageUploadService s3ImageUploadService,
      TelegramFileDownloaderService telegramFileDownloaderService) {
    this.salvarPedidoPagamentoUsecase = salvarPedidoPagamentoUsecase;
    this.telegramMessageSenderService = telegramMessageSenderService;
    this.s3ImageUploadService = s3ImageUploadService;
    this.telegramFileDownloaderService = telegramFileDownloaderService;
  }

  @Override
  public boolean supports(Update update) {
    String text = update.getMessage().getCaption().trim();
    return PEDIDO_PATTERN.matcher(text).matches();
  }

  @Override
  public void process(Update update) {
    Message message = update.getMessage();
    Long chatId = message.getChatId();
    logger.info("Estratégia de Pedido de Pagamento ativada para o chat ID: {}", chatId);

    ExtraidoArquivo extraido = extrair(message, chatId);

    byte[] bytes = telegramFileDownloaderService.downloadImageByFileId(extraido.fileId());

    String s3Url = s3ImageUploadService.uploadFile(bytes, extraido.extensao());
    logger.info("Imagem do pedido enviada para S3: {}", s3Url);

    PedidoPagamento pedido = parsePedido(message);
    pedido.setFileIdTelegram(extraido.fileId());
    pedido.setImagemUrl(s3Url);

    PedidoPagamento pedidoSalvo = salvarPedidoPagamentoUsecase.execute(pedido, chatId);
    logger.info("Pedido de pagamento {} do usuário {} salvo com sucesso.", pedidoSalvo.getId(), pedido.getTelegramUserId());

    String dica = pedidoSalvo.getTipo() == TipoPagamento.OUTRO
        ? "\n\n_Tipo não detectado. Inclua 'boleto', 'pix', 'ted' ou 'agendamento' na descrição para auto-categorizar._"
        : "";
    String successMessage = String.format(
        "✅ Pedido registrado!\n\n*ID:* `%d`\n*Valor:* R$ %.2f\n*Descrição:* %s\n*Tipo:* %s%s",
        pedidoSalvo.getId(),
        pedidoSalvo.getValor(),
        pedidoSalvo.getDescricao(),
        pedidoSalvo.getTipo(),
        dica
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

  private PedidoPagamento parsePedido(Message message) {
    String text = message.getCaption().trim();
    Matcher matcher = PEDIDO_PATTERN.matcher(text);

    if (!matcher.matches()) {
      throw new InvalidMessageFormatException(
          "Use: `<valor> <descrição>`\n\n" +
          "*Exemplos:*\n" +
          "• `100 boleto Energia`\n" +
          "• `200 pix Maria`\n" +
          "• `1500 ted Construtora`\n" +
          "• `300 agendamento Luz`\n" +
          "• `50 Almoço` (sem tipo vira OUTRO)\n\n" +
          "O tipo é detectado automaticamente pela palavra-chave na descrição.",
          message.getChatId());
    }

    String valorStr = matcher.group(1).replace(',', '.');
    BigDecimal valor = new BigDecimal(valorStr);
    String descricao = matcher.group(3);

    return PedidoPagamento.builder()
        .valor(valor)
        .descricao(descricao)
        .telegramUserId(message.getFrom().getId().toString())
        .telegramMessageId(message.getMessageId().toString())
        .status(StatusPedido.PENDENTE)
        .requisitanteId(1L)
        .dataPedido(LocalDate.now())
        .tipo(LegendaParser.parseTipo(text))
        .build();
  }
}
