package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.metrics.MetricsConstants;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.IdempotenciaMensagemPort;
import br.com.satyan.stering.saita.financasbottelegram.application.strategy.MensagemProcessingStrategy;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MensagemEntranteServiceTest {

    @Mock private MensagemProcessingStrategy strategyA;
    @Mock private MensagemProcessingStrategy strategyB;
    @Mock private IdempotenciaMensagemPort idempotenciaPort;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    private MensagemEntranteService service(MensagemProcessingStrategy... strategies) {
        return new MensagemEntranteService(List.of(strategies), idempotenciaPort, meterRegistry);
    }

    private PaymentMessageDTO dto(Long chatId) {
        return PaymentMessageDTO.builder()
            .canal(Canal.TELEGRAM)
            .externalId("msg-" + chatId)
            .chatId(chatId)
            .caption("qualquer")
            .build();
    }

    @Test
    void deveDespacharParaAPrimeiraStrategyQueSuportar() {
        PaymentMessageDTO dto = dto(100L);
        when(idempotenciaPort.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        when(strategyA.supports(dto)).thenReturn(false);
        when(strategyB.supports(dto)).thenReturn(true);

        service(strategyA, strategyB).processar(dto);

        verify(strategyB).process(dto);
        verify(strategyA, never()).process(dto);
    }

    @Test
    void deveDespacharParaPrimeiraStrategyQuandoAmbasSuportam() {
        PaymentMessageDTO dto = dto(100L);
        when(idempotenciaPort.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        when(strategyA.supports(dto)).thenReturn(true);

        service(strategyA, strategyB).processar(dto);

        verify(strategyA).process(dto);
        verify(strategyB, never()).supports(dto);
    }

    @Test
    void deveLancarInvalidMessageFormatExceptionQuandoNenhumaStrategySuportar() {
        PaymentMessageDTO dto = dto(200L);
        when(idempotenciaPort.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        when(strategyA.supports(dto)).thenReturn(false);
        when(strategyB.supports(dto)).thenReturn(false);

        assertThatThrownBy(() -> service(strategyA, strategyB).processar(dto))
                .isInstanceOf(InvalidMessageFormatException.class)
                .satisfies(ex -> {
                    InvalidMessageFormatException ime = (InvalidMessageFormatException) ex;
                    org.assertj.core.api.Assertions.assertThat(ime.getChatId()).isEqualTo(200L);
                });
    }

    @Test
    void deveLancarInvalidMessageFormatExceptionComListaVazia() {
        PaymentMessageDTO dto = dto(300L);
        when(idempotenciaPort.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);

        assertThatThrownBy(() -> service().processar(dto))
                .isInstanceOf(InvalidMessageFormatException.class);
    }

    @Test
    void deveSaltarProcessamentoQuandoClaimFalhar() {
        PaymentMessageDTO dto = dto(400L);
        when(idempotenciaPort.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(false);

        service(strategyA, strategyB).processar(dto);

        verify(strategyA, never()).supports(dto);
        verify(strategyB, never()).supports(dto);
    }

    @Test
    void timerRegistrado_quandoStrategyExecuta() {
        PaymentMessageDTO dto = dto(100L);
        when(idempotenciaPort.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        when(strategyA.supports(dto)).thenReturn(true);

        service(strategyA).processar(dto);

        Timer timer = meterRegistry.find(MetricsConstants.MENSAGEM_ENTRANTE_TIMER)
            .tags(MetricsConstants.TAG_CANAL, Canal.TELEGRAM.name(),
                MetricsConstants.TAG_TIPO, MetricsConstants.TIPO_PEDIDO)
            .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    void timerNaoRegistrado_quandoClaimFalha() {
        PaymentMessageDTO dto = dto(400L);
        when(idempotenciaPort.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(false);

        service(strategyA).processar(dto);

        Timer timer = meterRegistry.find(MetricsConstants.MENSAGEM_ENTRANTE_TIMER).timer();
        assertThat(timer).isNull();
    }

    @Test
    void timerRegistrado_mesmoQuandoStrategyLancaExcecao() {
        PaymentMessageDTO dto = dto(100L);
        when(idempotenciaPort.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        when(strategyA.supports(dto)).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("erro simulado")).when(strategyA).process(dto);

        assertThatThrownBy(() -> service(strategyA).processar(dto));

        Timer timer = meterRegistry.find(MetricsConstants.MENSAGEM_ENTRANTE_TIMER).timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }
}
