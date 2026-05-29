package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.MensagemEntrantePortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.strategy.MensagemProcessingStrategy;
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
            "Para registrar um *novo pedido*, envie a foto com a legenda no formato:\n" +
            "`VALOR DESCRIÇÃO`\n" +
            "*Exemplo:* `150.50 Almoço com cliente`\n\n" +
            "Para adicionar um *comprovante* a um pedido existente, use:\n" +
            "`#ID_DO_PEDIDO TIPO_PAGAMENTO`\n" +
            "*Exemplo:* `#123 PIX`";

    private static final Logger logger = LoggerFactory.getLogger(MensagemEntranteService.class);
    private final List<MensagemProcessingStrategy> strategies;
    private final MensagemProcessadaService mensagemProcessadaService;

    public MensagemEntranteService(List<MensagemProcessingStrategy> strategies,
        MensagemProcessadaService mensagemProcessadaService) {
        this.strategies = strategies;
        this.mensagemProcessadaService = mensagemProcessadaService;
    }

    @Transactional
    @Override
    public void processar(PaymentMessageDTO dto) {
        if (!mensagemProcessadaService.tentarClaim(dto.getCanal(), dto.getExternalId())) {
            logger.info("Mensagem {} (canal={}) já processada — descartando (idempotência).",
                dto.getExternalId(), dto.getCanal());
            return;
        }

        strategies.stream()
            .filter(strategy -> strategy.supports(dto))
            .findFirst()
            .ifPresentOrElse(
                strategy -> {
                    logger.info("Executando estratégia: {}", strategy.getClass().getSimpleName());
                    strategy.process(dto);
                },
                () -> {
                    logger.warn("Nenhuma estratégia encontrada para a mensagem. Lançando InvalidMessageFormatException.");
                    throw new InvalidMessageFormatException(ERROR_MESSAGE, dto.getChatId());
                }
            );
    }
}
