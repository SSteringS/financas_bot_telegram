package br.com.satyan.stering.saita.financasbottelegram.application.event;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.NotificacaoDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.NotificadorPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.event.ComprovanteRegistradoEvent;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
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

    public NotificacaoComprovanteListener(
        List<NotificadorPortOut> notificadorList,
        RequisitanteRepositoryPort requisitanteRepository,
        @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.notificadores = notificadorList.stream()
            .collect(Collectors.toMap(NotificadorPortOut::getCanal, Function.identity()));
        this.requisitanteRepository = requisitanteRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Async("notificacaoExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComprovanteRegistrado(ComprovanteRegistradoEvent evento) {
        try {
            Requisitante requisitante = requisitanteRepository.findById(evento.requisitanteId())
                .orElseThrow(() -> new IllegalStateException(
                    "Requisitante não encontrado: id=" + evento.requisitanteId()));

            Canal canal = requisitante.getCanalPreferido();
            NotificadorPortOut notificador = notificadores.get(canal);

            if (notificador == null) {
                logger.warn("Sem notificador para o canal {}; notificação ignorada. comprovanteId={}",
                    canal, evento.comprovanteId());
                return;
            }

            String linkSite = frontendBaseUrl + "/pedidos/" + evento.pedidoId();
            NotificacaoDTO dto = new NotificacaoDTO(evento.chatId(), evento.pedidoId(), linkSite);

            notificador.notificar(dto);
            logger.info("Notificação enviada via {} para comprovanteId={} pedidoId={}",
                canal, evento.comprovanteId(), evento.pedidoId());

        } catch (Exception e) {
            logger.error("Falha ao enviar notificação para comprovanteId={} — notificação descartada (best-effort).",
                evento.comprovanteId(), e);
            // TODO BE-22: incrementar counter de falha do listener (metric)
        }
    }
}
