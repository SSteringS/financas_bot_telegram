package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy.UpdateProcessingStrategy;
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

    private Update updateComChatId(Long chatId) {
        Chat chat = new Chat();
        chat.setId(chatId);
        Message message = new Message();
        message.setChat(chat);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    @Test
    void deveExecutarPrimeiraStrategyQueSuporta() {
        Update update = updateComChatId(100L);
        when(strategyA.supports(update)).thenReturn(true);

        UpdateOrchestratorService service = new UpdateOrchestratorService(List.of(strategyA, strategyB));
        service.process(update);

        verify(strategyA).process(update);
        verify(strategyB, never()).process(update);
    }

    @Test
    void deveExecutarSegundaStrategyQuandoPrimeiraFalhar() {
        Update update = updateComChatId(100L);
        when(strategyA.supports(update)).thenReturn(false);
        when(strategyB.supports(update)).thenReturn(true);

        UpdateOrchestratorService service = new UpdateOrchestratorService(List.of(strategyA, strategyB));
        service.process(update);

        verify(strategyA, never()).process(update);
        verify(strategyB).process(update);
    }

    @Test
    void deveLancarInvalidMessageFormatExceptionQuandoNenhumaStrategySuporta() {
        Update update = updateComChatId(200L);
        when(strategyA.supports(update)).thenReturn(false);
        when(strategyB.supports(update)).thenReturn(false);

        UpdateOrchestratorService service = new UpdateOrchestratorService(List.of(strategyA, strategyB));

        assertThatThrownBy(() -> service.process(update))
                .isInstanceOf(InvalidMessageFormatException.class)
                .satisfies(ex -> {
                    InvalidMessageFormatException imfe = (InvalidMessageFormatException) ex;
                    org.assertj.core.api.Assertions.assertThat(imfe.getChatId()).isEqualTo(200L);
                });
    }

    @Test
    void deveLancarExcecaoComListaDeStrategiesVazia() {
        Update update = updateComChatId(300L);

        UpdateOrchestratorService service = new UpdateOrchestratorService(List.of());

        assertThatThrownBy(() -> service.process(update))
                .isInstanceOf(InvalidMessageFormatException.class);
    }
}
