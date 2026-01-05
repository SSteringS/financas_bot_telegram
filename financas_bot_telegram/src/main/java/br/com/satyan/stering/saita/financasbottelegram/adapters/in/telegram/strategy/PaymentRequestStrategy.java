package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
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
    logger.info("Estratégia de Pedido de Pagamento ativada. Mensagem: '{}'", message.getText());

    try {
      PedidoPagamento pedido = parsePedido(message);
      PedidoPagamento pedidoSalvo = salvarPedidoPagamentoUsecase.execute(pedido);
      logger.info("Pedido de pagamento {} do usuário {} salvo com sucesso.", pedidoSalvo.getId(), pedido.getTelegramUserId());

      String successMessage = String.format(
          "✅ Pedido de pagamento registrado com sucesso!\n\n*ID do Pedido:* `%d`\n*Valor:* R$ %.2f\n*Descrição:* %s",
          pedidoSalvo.getId(),
          pedidoSalvo.getValor(),
          pedidoSalvo.getDescricao()
      );
      telegramMessageSenderService.sendMessage(chatId, successMessage);

    } catch (IllegalArgumentException e) {
      logger.error("Formato de mensagem inválido para pedido de pagamento: '{}'. Erro: {}", message.getText(), e.getMessage());
      String errorMessage = String.format("❌ Formato de mensagem inválido.\nUse: `pedido <valor> <descrição>`\n\n*Exemplo:* `pedido 150,50 para material de escritório`");
      telegramMessageSenderService.sendMessage(chatId, errorMessage);
    } catch (Exception e) {
      logger.error("Erro ao processar pedido de pagamento a partir do texto: '{}'", message.getText(), e);
      telegramMessageSenderService.sendMessage(chatId, "❌ Ocorreu um erro inesperado ao salvar seu pedido. Tente novamente.");
    }
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