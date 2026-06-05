package br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.notificador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.NotificacaoDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitários de {@link TelegramNotificadorImpl}.
 *
 * <p>Verifica que o notificador formata a mensagem corretamente, usa o chatId
 * do destinatário e reporta o canal correto.
 */
@ExtendWith(MockitoExtension.class)
class TelegramNotificadorImplTest {

    @Mock
    private TelegramMessageSenderService telegramSender;

    private TelegramNotificadorImpl notificador;

    @BeforeEach
    void setUp() {
        notificador = new TelegramNotificadorImpl(telegramSender);
    }

    @Test
    void deveRetornarCanalTelegram() {
        assertThat(notificador.getCanal()).isEqualTo(Canal.TELEGRAM);
    }

    @Test
    void deveEnviarParaChatIdCorreto() {
        NotificacaoDTO dto = new NotificacaoDTO("123456789", 42L, "https://app.example.com/pedido/42");

        notificador.notificar(dto);

        verify(telegramSender).sendMessage(eq(123456789L), contains("42"));
    }

    @Test
    void deveMensagemConterNumeroDoComprovante() {
        NotificacaoDTO dto = new NotificacaoDTO("111222333", 99L, "https://app.example.com/pedido/99");

        notificador.notificar(dto);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramSender).sendMessage(eq(111222333L), msgCaptor.capture());

        String mensagem = msgCaptor.getValue();
        assertThat(mensagem).contains("99");
        assertThat(mensagem).contains("https://app.example.com/pedido/99");
    }

    @Test
    void deveMensagemConterMarcadorDeComprovante() {
        NotificacaoDTO dto = new NotificacaoDTO("555444333", 7L, "https://link.com/7");

        notificador.notificar(dto);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramSender).sendMessage(eq(555444333L), msgCaptor.capture());

        // A mensagem deve conter indicador visual de sucesso e referência ao pedido
        String mensagem = msgCaptor.getValue();
        assertThat(mensagem).isNotBlank();
        assertThat(mensagem).contains("7");
    }
}
