package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy.UpdateProcessingStrategy;
import br.com.satyan.stering.saita.financasbottelegram.application.services.MensagemProcessadaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@ExtendWith(MockitoExtension.class)
class UpdateOrchestratorServiceTest {

    @Mock private UpdateProcessingStrategy strategyA;
    @Mock private UpdateProcessingStrategy strategyB;
    @Mock private MensagemProcessadaService mensagemProcessadaService;

    private Update updateComChatId(Long chatId) {
        return updateComChatIdEUpdateId(chatId, 42);
    }

    private Update updateComChatIdEUpdateId(Long chatId, Integer updateId) {
        Chat chat = new Chat();
        chat.setId(chatId);
        Message message = new Message();
        message.setChat(chat);
        Update update = new Update();
        update.setMessage(message);
        update.setUpdateId(updateId);
        return update;
    }

    @Test
    void deveExecutarPrimeiraStrategyQueSuporta() {
        Update update = updateComChatId(100L);
        when(mensagemProcessadaService.tentarClaim(any(), any())).thenReturn(true);
        when(strategyA.supports(update)).thenReturn(true);

        new UpdateOrchestratorService(List.of(strategyA, strategyB), mensagemProcessadaService)
            .process(update);

        verify(strategyA).process(update);
        verify(strategyB, never()).process(update);
    }

    @Test
    void deveExecutarSegundaStrategyQuandoPrimeiraFalhar() {
        Update update = updateComChatId(100L);
        when(mensagemProcessadaService.tentarClaim(any(), any())).thenReturn(true);
        when(strategyA.supports(update)).thenReturn(false);
        when(strategyB.supports(update)).thenReturn(true);

        new UpdateOrchestratorService(List.of(strategyA, strategyB), mensagemProcessadaService)
            .process(update);

        verify(strategyA, never()).process(update);
        verify(strategyB).process(update);
    }

    @Test
    void deveLancarInvalidMessageFormatExceptionQuandoNenhumaStrategySuporta() {
        Update update = updateComChatId(200L);
        when(mensagemProcessadaService.tentarClaim(any(), any())).thenReturn(true);
        when(strategyA.supports(update)).thenReturn(false);
        when(strategyB.supports(update)).thenReturn(false);

        assertThatThrownBy(() ->
            new UpdateOrchestratorService(List.of(strategyA, strategyB), mensagemProcessadaService)
                .process(update))
            .isInstanceOf(InvalidMessageFormatException.class)
            .satisfies(ex -> {
                InvalidMessageFormatException imfe = (InvalidMessageFormatException) ex;
                org.assertj.core.api.Assertions.assertThat(imfe.getChatId()).isEqualTo(200L);
            });
    }

    @Test
    void deveLancarExcecaoComListaDeStrategiesVazia() {
        Update update = updateComChatId(300L);
        when(mensagemProcessadaService.tentarClaim(any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
            new UpdateOrchestratorService(List.of(), mensagemProcessadaService).process(update))
            .isInstanceOf(InvalidMessageFormatException.class);
    }

    @Test
    void deveSaltarProcessamentoQuandoClaimFalhar() {
        Update update = updateComChatIdEUpdateId(100L, 99);
        when(mensagemProcessadaService.tentarClaim(any(), any())).thenReturn(false);

        new UpdateOrchestratorService(List.of(strategyA, strategyB), mensagemProcessadaService)
            .process(update);

        verify(strategyA, never()).supports(update);
        verify(strategyA, never()).process(update);
        verify(strategyB, never()).supports(update);
        verify(strategyB, never()).process(update);
    }
}
