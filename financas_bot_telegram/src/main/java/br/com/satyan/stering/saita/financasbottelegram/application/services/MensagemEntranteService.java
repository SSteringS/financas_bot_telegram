package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.metrics.MetricsConstants;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.MensagemEntrantePortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.IdempotenciaMensagemPort;
import br.com.satyan.stering.saita.financasbottelegram.application.strategy.MensagemProcessingStrategy;
import br.com.satyan.stering.saita.financasbottelegram.application.strategy.PaymentProofStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// NOTA ARQUITETURAL: InvalidMessageFormatException está em adapters/in/telegram/exception/.
// Importá-la aqui é uma violação hexagonal conhecida — será resolvida quando essa exceção
// for movida para application/exceptions/ (tarefa futura, sem urgência para BE-17).
@Service
public class MensagemEntranteService implements MensagemEntrantePortIn {

    public static final String ERROR_MESSAGE =
        "😕 Formato de mensagem inválido. Não entendi o que você quis dizer.\n\n" +
            "Para registrar um *novo pedido*, envie a foto ou documento com a legenda no formato:\n" +
            "`VALOR DESCRIÇÃO`\n" +
            "*Exemplo:* `150.50 Almoço com cliente`\n\n" +
            "Para adicionar um *comprovante* a um pedido existente, use:\n" +
            "`#ID_DO_PEDIDO TIPO_PAGAMENTO`\n" +
            "*Exemplo:* `#123 PIX`";

    private static final Logger logger = LoggerFactory.getLogger(MensagemEntranteService.class);
    private final List<MensagemProcessingStrategy> strategies;
    private final IdempotenciaMensagemPort idempotenciaPort;
    private final MeterRegistry meterRegistry;

    public MensagemEntranteService(
        List<MensagemProcessingStrategy> strategies,
        IdempotenciaMensagemPort idempotenciaPort,
        MeterRegistry meterRegistry
    ) {
        this.strategies = strategies;
        this.idempotenciaPort = idempotenciaPort;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    @Override
    public void processar(PaymentMessageDTO dto) {
        if (!idempotenciaPort.tentarClaim(dto.getCanal(), dto.getExternalId())) {
            logger.info("Mensagem {} (canal={}) já processada — descartando (idempotência).",
                dto.getExternalId(), dto.getCanal());
            return;
        }

        MensagemProcessingStrategy strategy = strategies.stream()
            .filter(s -> s.supports(dto))
            .findFirst()
            .orElseThrow(() -> {
                logger.warn("Nenhuma estratégia encontrada para a mensagem. Lançando InvalidMessageFormatException.");
                return new InvalidMessageFormatException(ERROR_MESSAGE, dto.getChatId());
            });

        String tipoMetrica = strategy instanceof PaymentProofStrategy
            ? MetricsConstants.TIPO_COMPROVANTE
            : MetricsConstants.TIPO_PEDIDO;
        String canalMetrica = dto.getCanal() != null ? dto.getCanal().name() : "DESCONHECIDO";

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            logger.info("Executando estratégia: {}", strategy.getClass().getSimpleName());
            strategy.process(dto);
        } finally {
            sample.stop(Timer.builder(MetricsConstants.MENSAGEM_ENTRANTE_TIMER)
                .tags(MetricsConstants.TAG_TIPO, tipoMetrica, MetricsConstants.TAG_CANAL, canalMetrica)
                .register(meterRegistry));
        }
    }
}
