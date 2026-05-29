// Pacote: ...adapters.in.telegram.service (renomeado de ProcessPaymentMessageService)
package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy.UpdateProcessingStrategy;
import br.com.satyan.stering.saita.financasbottelegram.application.services.MensagemProcessadaService;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class UpdateOrchestratorService {

  public static final String ERROR_MESSAGE =
      "😕 Formato de mensagem inválido. Não entendi o que você quis dizer.\n\n" +
          "Para registrar um *novo pedido*, envie a foto com a legenda no formato:\n" +
          "`VALOR DESCRIÇÃO`\n" +
          "*Exemplo:* `150.50 Almoço com cliente`\n\n" +
          "Para adicionar um *comprovante* a um pedido existente, use:\n" +
          "`#ID_DO_PEDIDO TIPO_PAGAMENTO`\n" +
          "*Exemplo:* `#123 PIX`";

  private static final Logger logger = LoggerFactory.getLogger(UpdateOrchestratorService.class);
  private final List<UpdateProcessingStrategy> strategies;
  private final MensagemProcessadaService mensagemProcessadaService;

  public UpdateOrchestratorService(List<UpdateProcessingStrategy> strategies,
      MensagemProcessadaService mensagemProcessadaService) {
    this.strategies = strategies;
    this.mensagemProcessadaService = mensagemProcessadaService;
  }

  @Transactional
  public void process(Update update) {
    String idExterno = update.getUpdateId() != null ? update.getUpdateId().toString() : "";
    if (!mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, idExterno)) {
      logger.info("Update {} já processado anteriormente — descartando (idempotência).", idExterno);
      return;
    }

    strategies.stream()
        .filter(strategy -> strategy.supports(update))
        .findFirst()
        .ifPresentOrElse(
            strategy -> {
              logger.info("Executando estratégia de adaptador: {}", strategy.getClass().getSimpleName());
              strategy.process(update);
            },
            () -> {
              logger.warn("Nenhuma estratégia encontrada para a mensagem com foto. Lançando InvalidMessageFormatException.");
              Long chatId = update.getMessage().getChatId();
              throw new InvalidMessageFormatException(ERROR_MESSAGE, chatId);
            }
        );
  }
}
