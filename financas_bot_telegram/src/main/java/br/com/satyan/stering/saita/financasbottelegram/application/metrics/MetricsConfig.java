package br.com.satyan.stering.saita.financasbottelegram.application.metrics;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    // Whitelist explícita pra CloudWatch — evita publicar ~80 built-ins do Actuator ($24/mês).
    // JVM essencial (jvm.memory.used, jvm.threads.live) + métricas custom da app.
    @Bean
    public MeterFilter metricasFiltro() {
        return MeterFilter.denyUnless(id -> {
            String name = id.getName();
            return name.startsWith("mensagem_entrante")
                || name.startsWith("notificacao")
                || name.equals("jvm.memory.used")
                || name.equals("jvm.threads.live");
        });
    }
}
