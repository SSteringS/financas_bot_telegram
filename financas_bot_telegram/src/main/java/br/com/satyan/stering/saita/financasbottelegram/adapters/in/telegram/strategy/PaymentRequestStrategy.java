package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
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

    // Obter o file_id da imagem de maior qualidade
    String fileId = extractHighestQualityImageFileId(message);

    // Download da imagem do Telegram
    byte[] imageBytes = telegramFileDownloaderService.downloadImageByFileId(fileId);

    // Upload para S3
    String s3ImageUrl = s3ImageUploadService.uploadImage(imageBytes);
    logger.info("Imagem do pedido enviada para S3: {}", s3ImageUrl);

    // Parsear dados do pedido
    PedidoPagamento pedido = parsePedido(message);
    pedido.setFileIdTelegram(fileId);
    pedido.setImagemUrl(s3ImageUrl);

    // Salvar pedido no banco
    PedidoPagamento pedidoSalvo = salvarPedidoPagamentoUsecase.execute(pedido, chatId);
    logger.info("Pedido de pagamento {} do usuário {} salvo com sucesso.", pedidoSalvo.getId(), pedido.getTelegramUserId());

    String successMessage = String.format(
        "✅ Pedido de pagamento registrado com sucesso!\n\n*ID do Pedido:* `%d`\n*Valor:* R$ %.2f\n*Descrição:* %s",
        pedidoSalvo.getId(),
        pedidoSalvo.getValor(),
        pedidoSalvo.getDescricao()
    );
    telegramMessageSenderService.sendMessage(chatId, successMessage);
  }

  /**
   * Extrai o file_id da foto com maior qualidade (maior tamanho).
   */
  private String extractHighestQualityImageFileId(Message message) {
    return message.getPhoto().stream()
        .max(Comparator.comparing(PhotoSize::getFileSize))
        .map(PhotoSize::getFileId)
        .orElseThrow(() -> new IllegalArgumentException("Nenhuma foto encontrada na mensagem"));
  }

  /**
   * Converte a mensagem do Telegram em um objeto PedidoPagamento usando regex.
   */
  private PedidoPagamento parsePedido(Message message) {
    String text = message.getCaption().trim();
    Matcher matcher = PEDIDO_PATTERN.matcher(text);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Formato inválido. Use: pedido <valor> <descrição>");
    }

    String valorStr = matcher.group(1).replace(',', '.');
    BigDecimal valor = new BigDecimal(valorStr);

    String descricao = matcher.group(3);

    PedidoPagamento pedido = new PedidoPagamento();
    pedido.setValor(valor);
    pedido.setDescricao(descricao);
    pedido.setTelegramUserId(message.getFrom().getId().toString());
    pedido.setTelegramMessageId(message.getMessageId().toString());
    pedido.setStatus(StatusPedido.PENDENTE);

    return pedido;
  }
}