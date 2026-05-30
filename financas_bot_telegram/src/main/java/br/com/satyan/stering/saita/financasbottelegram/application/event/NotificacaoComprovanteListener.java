package br.com.satyan.stering.saita.financasbottelegram.application.event;

import br.com.satyan.stering.saita.financasbottelegram.application.metrics.MetricsConstants;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.NotificacaoDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.NotificadorPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.event.ComprovanteRegistradoEvent;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacaoComprovanteListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificacaoComprovanteListener.class);

    private final Map<Canal, NotificadorPortOut> notificadores;
    private final RequisitanteRepositoryPort requisitanteRepository;
    private final String frontendBaseUrl;
    private final MeterRegistry meterRegistry;

    public NotificacaoComprovanteListener(
        List<NotificadorPortOut> notificadorList,
        RequisitanteRepositoryPort requisitanteRepository,
        @Value("${app.frontend.base-url}") String frontendBaseUrl,
        MeterRegistry meterRegistry
    ) {
        this.notificadores = notificadorList.stream()
            .collect(Collectors.toMap(NotificadorPortOut::getCanal, Function.identity()));
        this.requisitanteRepository = requisitanteRepository;
        this.frontendBaseUrl = frontendBaseUrl;
        this.meterRegistry = meterRegistry;
    }

    @Async("notificacaoExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComprovanteRegistrado(ComprovanteRegistradoEvent evento) {
        Canal canal = null;
        try {
            Requisitante requisitante = requisitanteRepository.findById(evento.requisitanteId())
                .orElseThrow(() -> new IllegalStateException(
                    "Requisitante não encontrado: id=" + evento.requisitanteId()));

            canal = requisitante.getCanalPreferido();
            NotificadorPortOut notificador = notificadores.get(canal);

            if (notificador == null) {
                logger.warn("Sem notificador para o canal {}; notificação ignorada. comprovanteId={}",
                    canal, evento.comprovanteId());
                meterRegistry.counter(MetricsConstants.NOTIFICACAO_FALHA_COUNTER,
                    MetricsConstants.TAG_CANAL, canal.name(),
                    MetricsConstants.TAG_MOTIVO, MetricsConstants.MOTIVO_SEM_NOTIFICADOR)
                    .increment();
                return;
            }

            String linkSite = frontendBaseUrl + "/pedidos/" + evento.pedidoId();
            NotificacaoDTO dto = new NotificacaoDTO(evento.chatId(), evento.pedidoId(), linkSite);

            // Timer mede só a execução do notificar — não inclui a fila do executor @Async
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                notificador.notificar(dto);
            } finally {
                sample.stop(Timer.builder(MetricsConstants.NOTIFICACAO_ENVIO_TIMER)
                    .tags(MetricsConstants.TAG_CANAL, canal.name())
                    .register(meterRegistry));
            }

            logger.info("Notificação enviada via {} para comprovanteId={} pedidoId={}",
                canal, evento.comprovanteId(), evento.pedidoId());

        } catch (Exception e) {
            logger.error("Falha ao enviar notificação para comprovanteId={} — notificação descartada (best-effort).",
                evento.comprovanteId(), e);
            meterRegistry.counter(MetricsConstants.NOTIFICACAO_FALHA_COUNTER,
                MetricsConstants.TAG_CANAL, canal != null ? canal.name() : "DESCONHECIDO",
                MetricsConstants.TAG_MOTIVO, MetricsConstants.MOTIVO_EXCEPTION)
                .increment();
        }
    }
}
