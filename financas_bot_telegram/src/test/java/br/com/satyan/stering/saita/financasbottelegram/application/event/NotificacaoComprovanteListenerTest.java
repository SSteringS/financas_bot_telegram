package br.com.satyan.stering.saita.financasbottelegram.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.metrics.MetricsConstants;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.NotificadorPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.event.ComprovanteRegistradoEvent;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacaoComprovanteListenerTest {

    @Mock private NotificadorPortOut notificadorTelegram;
    @Mock private RequisitanteRepositoryPort requisitanteRepository;

    private SimpleMeterRegistry meterRegistry;
    private NotificacaoComprovanteListener listener;

    private static final String FRONTEND_URL = "http://localhost:5173";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        when(notificadorTelegram.getCanal()).thenReturn(Canal.TELEGRAM);
        listener = new NotificacaoComprovanteListener(
            List.of(notificadorTelegram), requisitanteRepository, FRONTEND_URL, meterRegistry);
    }

    @Test
    void sucesso_timerRegistrado_semFalhaCounter() {
        Requisitante req = Requisitante.builder().id(1L).canalPreferido(Canal.TELEGRAM).build();
        when(requisitanteRepository.findById(1L)).thenReturn(Optional.of(req));

        listener.onComprovanteRegistrado(new ComprovanteRegistradoEvent(10L, 20L, 1L, "123"));

        Timer timer = meterRegistry.find(MetricsConstants.NOTIFICACAO_ENVIO_TIMER)
            .tags(MetricsConstants.TAG_CANAL, Canal.TELEGRAM.name())
            .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);

        Counter falha = meterRegistry.find(MetricsConstants.NOTIFICACAO_FALHA_COUNTER).counter();
        assertThat(falha).isNull();
    }

    @Test
    void semNotificador_contadorSemNotificadorIncrementado() {
        Requisitante req = Requisitante.builder().id(1L).canalPreferido(Canal.WHATSAPP).build();
        when(requisitanteRepository.findById(1L)).thenReturn(Optional.of(req));

        listener.onComprovanteRegistrado(new ComprovanteRegistradoEvent(10L, 20L, 1L, "5511"));

        Counter counter = meterRegistry.find(MetricsConstants.NOTIFICACAO_FALHA_COUNTER)
            .tags(MetricsConstants.TAG_CANAL, Canal.WHATSAPP.name(),
                MetricsConstants.TAG_MOTIVO, MetricsConstants.MOTIVO_SEM_NOTIFICADOR)
            .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
        verify(notificadorTelegram, never()).notificar(any());
    }

    @Test
    void excecaoNoNotificar_contadorExceptionIncrementado() {
        Requisitante req = Requisitante.builder().id(1L).canalPreferido(Canal.TELEGRAM).build();
        when(requisitanteRepository.findById(1L)).thenReturn(Optional.of(req));
        org.mockito.Mockito.doThrow(new RuntimeException("Graph API offline"))
            .when(notificadorTelegram).notificar(any());

        listener.onComprovanteRegistrado(new ComprovanteRegistradoEvent(10L, 20L, 1L, "123"));

        Counter counter = meterRegistry.find(MetricsConstants.NOTIFICACAO_FALHA_COUNTER)
            .tags(MetricsConstants.TAG_CANAL, Canal.TELEGRAM.name(),
                MetricsConstants.TAG_MOTIVO, MetricsConstants.MOTIVO_EXCEPTION)
            .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void excecaoNoNotificar_timerAindaRegistrado() {
        Requisitante req = Requisitante.builder().id(1L).canalPreferido(Canal.TELEGRAM).build();
        when(requisitanteRepository.findById(1L)).thenReturn(Optional.of(req));
        org.mockito.Mockito.doThrow(new RuntimeException("erro"))
            .when(notificadorTelegram).notificar(any());

        listener.onComprovanteRegistrado(new ComprovanteRegistradoEvent(10L, 20L, 1L, "123"));

        Timer timer = meterRegistry.find(MetricsConstants.NOTIFICACAO_ENVIO_TIMER)
            .tags(MetricsConstants.TAG_CANAL, Canal.TELEGRAM.name())
            .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    void requisitanteNaoEncontrado_contadorExceptionIncrementado() {
        when(requisitanteRepository.findById(99L)).thenReturn(Optional.empty());

        listener.onComprovanteRegistrado(new ComprovanteRegistradoEvent(10L, 20L, 99L, "123"));

        Counter counter = meterRegistry.find(MetricsConstants.NOTIFICACAO_FALHA_COUNTER)
            .tags(MetricsConstants.TAG_MOTIVO, MetricsConstants.MOTIVO_EXCEPTION)
            .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
