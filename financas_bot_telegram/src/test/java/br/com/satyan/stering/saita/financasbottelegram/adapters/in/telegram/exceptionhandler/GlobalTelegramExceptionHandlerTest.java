package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exceptionhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidUpdateException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.BusinessRuleException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.DatabaseException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.UnauthorizedUserException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.WebRequest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@ExtendWith(MockitoExtension.class)
class GlobalTelegramExceptionHandlerTest {

    @Mock private TelegramMessageSenderService telegramMessageSenderService;
    @Mock private WebRequest webRequest;
    @InjectMocks private GlobalTelegramExceptionHandler handler;

    @Test
    void deveRetornarOkEEnviarMensagemParaUnauthorizedUserException() {
        UnauthorizedUserException ex = new UnauthorizedUserException("Não autorizado", 100L);

        ResponseEntity<Void> response = handler.handleUnauthorizedUser(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService).sendMessage(eq(100L), any());
    }

    @Test
    void deveRetornarOkSemEnviarMensagemParaInvalidUpdateException() {
        InvalidUpdateException ex = new InvalidUpdateException("Update inválido");

        ResponseEntity<Void> response = handler.handleInvalidUpdate(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deveRetornarOkEEnviarMensagemParaInvalidMessageFormatException() {
        InvalidMessageFormatException ex = new InvalidMessageFormatException("Formato inválido", 200L);

        ResponseEntity<Void> response = handler.handleInvalidMessageFormat(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService).sendMessage(eq(200L), any());
    }

    @Test
    void deveRetornarOkEEnviarMensagemParaPedidoNaoEncontradoException() {
        PedidoNaoEncontradoException ex = new PedidoNaoEncontradoException("Pedido não encontrado", 300L);

        ResponseEntity<Void> response = handler.handlePedidoNaoEncontrado(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService).sendMessage(eq(300L), any());
    }

    @Test
    void deveRetornarOkEEnviarMensagemParaPhotoProcessingException() {
        PhotoProcessingException ex = new PhotoProcessingException("Erro na foto", 400L);

        ResponseEntity<Void> response = handler.handlePhotoProcessing(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService).sendMessage(eq(400L), any());
    }

    @Test
    void deveRetornarOkEEnviarMensagemParaInvalidCaptionException() {
        InvalidCaptionException ex = new InvalidCaptionException("Legenda inválida", 500L);

        ResponseEntity<Void> response = handler.handleInvalidCaption(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService).sendMessage(eq(500L), any());
    }

    @Test
    void deveRetornarOkEEnviarMensagemParaTipoArquivoNaoSuportadoException() {
        TipoArquivoNaoSuportadoException ex = new TipoArquivoNaoSuportadoException("video/mp4 não suportado", 550L);

        ResponseEntity<Void> response = handler.handleTipoArquivoNaoSuportado(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService).sendMessage(eq(550L), any());
    }

    @Test
    void deveRetornarOkEEnviarMensagemParaBusinessRuleException() {
        BusinessRuleException ex = new BusinessRuleException("Regra violada", 600L);

        ResponseEntity<Void> response = handler.handleBusinessRuleException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService).sendMessage(eq(600L), any());
    }

    @Test
    void deveRetornar500EEnviarMensagemParaDatabaseException() {
        DatabaseException ex = new DatabaseException("Erro de banco", 700L, new RuntimeException("causa"));

        ResponseEntity<Void> response = handler.handleDatabaseException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(telegramMessageSenderService).sendMessage(eq(700L), any());
    }

    @Test
    void handleAnyOther_deveRetornar200EEnviarMensagemComChatIdDoUpdate() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Update update = updateComChatId(42L);
        request.setAttribute("__update", update);

        ResponseEntity<Void> response = handler.handleAnyOther(new RuntimeException("bug"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService).sendMessage(eq(42L), any());
    }

    @Test
    void handleAnyOther_deveRetornar200MesmoQuandoSendMessageFalha() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("__update", updateComChatId(42L));
        doThrow(new RuntimeException("send falhou")).when(telegramMessageSenderService).sendMessage(any(), any());

        ResponseEntity<Void> response = handler.handleAnyOther(new RuntimeException("bug"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void handleAnyOther_deveRetornar200SemEnviarMensagemQuandoUpdateSemMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Update update = new Update();
        request.setAttribute("__update", update);

        ResponseEntity<Void> response = handler.handleAnyOther(new NullPointerException("npe"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService, never()).sendMessage(any(), any());
    }

    @Test
    void handleAnyOther_deveRetornar200SemEnviarMensagemQuandoSemAttributeNoRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Void> response = handler.handleAnyOther(new IllegalStateException("erro"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(telegramMessageSenderService, never()).sendMessage(any(), any());
    }

    private Update updateComChatId(Long chatId) {
        Chat chat = new Chat();
        chat.setId(chatId);
        Message message = new Message();
        message.setChat(chat);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
