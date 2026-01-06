package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PaymentRequestStrategy implements UpdateProcessingStrategy {

  private static final Logger logger = LoggerFactory.getLogger(PaymentRequestStrategy.class);
  private final SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase;
  private final TelegramMessageSenderService telegramMessageSenderService;

  private static final Pattern PEDIDO_PATTERN = Pattern.compile("^(\\d+([.,]\\d{1,2})?)\\s+(.+)$");

  public PaymentRequestStrategy(SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase,
      TelegramMessageSenderService telegramMessageSenderService) {
    this.salvarPedidoPagamentoUsecase = salvarPedidoPagamentoUsecase;
    this.telegramMessageSenderService = telegramMessageSenderService;
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

    PedidoPagamento pedido = parsePedido(message);
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