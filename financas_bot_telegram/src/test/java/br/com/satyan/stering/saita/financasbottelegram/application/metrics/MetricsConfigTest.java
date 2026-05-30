package br.com.satyan.stering.saita.financasbottelegram.application.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsConfigTest {

    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        MeterFilter filtro = new MetricsConfig().metricasFiltro();
        registry.config().meterFilter(filtro);
    }

    @Test
    void mensagemEntranteTimer_naoFiltrado() {
        Timer timer = Timer.builder(MetricsConstants.MENSAGEM_ENTRANTE_TIMER)
            .tags("tipo", "PEDIDO", "canal", "TELEGRAM")
            .register(registry);

        assertThat(registry.find(MetricsConstants.MENSAGEM_ENTRANTE_TIMER).timer())
            .isNotNull()
            .isSameAs(timer);
    }

    @Test
    void notificacaoEnvioTimer_naoFiltrado() {
        Timer.builder(MetricsConstants.NOTIFICACAO_ENVIO_TIMER)
            .tags("canal", "TELEGRAM")
            .register(registry);

        assertThat(registry.find(MetricsConstants.NOTIFICACAO_ENVIO_TIMER).timer()).isNotNull();
    }

    @Test
    void notificacaoFalhaCounter_naoFiltrado() {
        registry.counter(MetricsConstants.NOTIFICACAO_FALHA_COUNTER, "canal", "TELEGRAM", "motivo", "EXCEPTION");

        assertThat(registry.find(MetricsConstants.NOTIFICACAO_FALHA_COUNTER).counter()).isNotNull();
    }

    @Test
    void processUptime_filtrado() {
        registry.gauge("process.uptime", 0);

        assertThat(registry.find("process.uptime").gauge()).isNull();
    }

    @Test
    void httpServerRequests_filtrado() {
        Timer.builder("http.server.requests").register(registry);

        assertThat(registry.find("http.server.requests").timer()).isNull();
    }

    @Test
    void jvmMemoryUsed_naoFiltrado() {
        registry.gauge("jvm.memory.used", 0);

        assertThat(registry.find("jvm.memory.used").gauge()).isNotNull();
    }

    @Test
    void timerSemPercentis_padrao() {
        Timer timer = Timer.builder(MetricsConstants.MENSAGEM_ENTRANTE_TIMER)
            .tags("tipo", "COMPROVANTE", "canal", "WHATSAPP")
            .register(registry);

        // Sem percentis configurados — Micrometer default não publica p50/p95 em registros simples
        assertThat(timer.takeSnapshot().percentileValues()).isEmpty();
    }
}
