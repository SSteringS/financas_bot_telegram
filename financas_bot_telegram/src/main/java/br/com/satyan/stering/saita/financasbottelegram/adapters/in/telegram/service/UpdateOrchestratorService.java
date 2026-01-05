// Pacote: ...adapters.in.telegram.service (renomeado de ProcessPaymentMessageService)
package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy.UpdateProcessingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Service
public class UpdateOrchestratorService {

  private static final Logger logger = LoggerFactory.getLogger(UpdateOrchestratorService.class);
  private final List<UpdateProcessingStrategy> strategies;

  // Spring injeta todas as classes que implementam a interface
  public UpdateOrchestratorService(List<UpdateProcessingStrategy> strategies) {
    this.strategies = strategies;
  }

  public void process(Update update) {
    strategies.stream()
        .filter(strategy -> strategy.supports(update))
        .findFirst()
        .ifPresentOrElse(
            strategy -> {
              logger.info("Executando estratégia de adaptador: {}", strategy.getClass().getSimpleName());
              strategy.process(update);
            },
            () -> logger.warn("Nenhuma estratégia de adaptador encontrada para o update.")
        );
  }
}