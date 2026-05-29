package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidUpdateException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.mapper.TelegramMessageMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.UnauthorizedUserException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.MensagemEntrantePortIn;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@ExtendWith(MockitoExtension.class)
class TelegramWebhookControllerTest {

    @Mock private TelegramMessageMapper telegramMessageMapper;
    @Mock private MensagemEntrantePortIn mensagemEntrantePortIn;

    private TelegramWebhookController controller(String... allowedIds) {
        return new TelegramWebhookController(telegramMessageMapper, mensagemEntrantePortIn, List.of(allowedIds));
    }

    private Update updateValido(Long chatId, Long userId) {
        User user = new User();
        user.setId(userId);

        Chat chat = new Chat();
        chat.setId(chatId);

        Message message = new Message();
        message.setFrom(user);
        message.setChat(chat);
        message.setMessageId(1);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    @Test
    void deveRetornar200ParaUsuarioAutorizado() {
        Update update = updateValido(100L, 123L);
        PaymentMessageDTO dto = PaymentMessageDTO.builder().chatId(100L).build();
        when(telegramMessageMapper.toPaymentMessageDTO(update)).thenReturn(dto);

        TelegramWebhookController ctrl = controller("123");
        ResponseEntity<Void> response = ctrl.receberMensagem(update, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageMapper).toPaymentMessageDTO(update);
        verify(mensagemEntrantePortIn).processar(dto);
    }

    @Test
    void deveLancarUnauthorizedExceptionParaUsuarioNaoAutorizado() {
        Update update = updateValido(100L, 999L);
        TelegramWebhookController ctrl = controller("123", "456");

        assertThatThrownBy(() -> ctrl.receberMensagem(update, new MockHttpServletRequest()))
                .isInstanceOf(UnauthorizedUserException.class)
                .satisfies(ex -> assertThat(((UnauthorizedUserException) ex).getChatId()).isEqualTo(100L));
    }

    @Test
    void deveLancarInvalidUpdateExceptionParaUpdateSemMessage() {
        Update update = new Update();
        TelegramWebhookController ctrl = controller("123");

        assertThatThrownBy(() -> ctrl.receberMensagem(update, new MockHttpServletRequest()))
                .isInstanceOf(InvalidUpdateException.class);
    }

    @Test
    void deveLancarInvalidUpdateExceptionParaMessageSemFrom() {
        Message message = new Message();
        // from = null
        Update update = new Update();
        update.setMessage(message);
        TelegramWebhookController ctrl = controller("123");

        assertThatThrownBy(() -> ctrl.receberMensagem(update, new MockHttpServletRequest()))
                .isInstanceOf(InvalidUpdateException.class);
    }
}
