package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Define o contrato para estratégias que processam diferentes tipos de Updates do Telegram.
 * Esta estratégia pertence à camada do Adaptador.
 */
public interface UpdateProcessingStrategy {

  /**
   * Verifica se esta estratégia é aplicável ao Update recebido.
   * @param update O objeto Update do Telegram.
   * @return true se a estratégia pode lidar com o update, false caso contrário.
   */
  boolean supports(Update update);

  /**
   * Processa o Update.
   * @param update O objeto Update do Telegram.
   */
  void process(Update update);
}