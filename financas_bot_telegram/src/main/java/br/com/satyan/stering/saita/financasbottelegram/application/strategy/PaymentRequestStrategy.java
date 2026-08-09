package br.com.satyan.stering.saita.financasbottelegram.application.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.CanalNotificadorPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoUploadS3;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.service.LegendaParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// NOTA ARQUITETURAL: importa de adapters/in/telegram/exception/ e adapters/out/s3/service/ —
// violações hexagonais conhecidas. Serão resolvidas em tarefas futuras:
// • exceções → mover para application/exceptions/
// • S3ImageUploadService → extrair porta com uploadFile(bytes, ext, tipo) quando BE-22 instrumentar
@Component
public class PaymentRequestStrategy implements MensagemProcessingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRequestStrategy.class);
    private static final Pattern PEDIDO_PATTERN = Pattern.compile("^(\\d+([.,]\\d{1,2})?)\\s+(.+)$");

    private final SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase;
    private final CanalNotificadorPort canalNotificadorPort;
    private final S3ImageUploadService s3ImageUploadService;

    public PaymentRequestStrategy(SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase,
        CanalNotificadorPort canalNotificadorPort,
        S3ImageUploadService s3ImageUploadService) {
        this.salvarPedidoPagamentoUsecase = salvarPedidoPagamentoUsecase;
        this.canalNotificadorPort = canalNotificadorPort;
        this.s3ImageUploadService = s3ImageUploadService;
    }

    @Override
    public boolean supports(PaymentMessageDTO dto) {
        String caption = dto.getCaption();
        if (caption == null) return false;
        return PEDIDO_PATTERN.matcher(caption.trim()).matches();
    }

    @Override
    public void process(PaymentMessageDTO dto) {
        Long chatId = dto.getChatId();
        logger.info("Estratégia de Pedido de Pagamento ativada para o chat ID: {}", chatId);

        if (dto.getFileBytes() == null) {
            throw new PhotoProcessingException(
                "Nenhuma imagem ou anexo encontrado. Envie como foto ou anexo.", chatId);
        }

        String s3Url = s3ImageUploadService.uploadFile(
            dto.getFileBytes(), dto.getFileExtension(), TipoUploadS3.PEDIDO);
        logger.info("Imagem do pedido enviada para S3: {}", s3Url);

        PedidoPagamento pedido = parsePedido(dto);
        pedido.setFileIdTelegram(dto.getMediaId());
        pedido.setImagemUrl(s3Url);

        PedidoPagamento pedidoSalvo = salvarPedidoPagamentoUsecase.execute(pedido, chatId);
        logger.info("Pedido de pagamento {} do usuário {} salvo com sucesso.",
            pedidoSalvo.getId(), pedido.getTelegramUserId());

        String dica = pedidoSalvo.getTipo() == TipoPagamento.OUTRO
            ? "\n\n_Tipo não detectado. Inclua 'boleto', 'pix', 'ted' ou 'agendamento' na descrição para auto-categorizar._"
            : "";
        String successMessage = String.format(
            "✅ Pedido registrado!\n\n*ID:* `%d`\n*Valor:* R$ %.2f\n*Descrição:* %s\n*Tipo:* %s%s",
            pedidoSalvo.getId(), pedidoSalvo.getValor(),
            pedidoSalvo.getDescricao(), pedidoSalvo.getTipo(), dica);
        canalNotificadorPort.enviar(chatId, successMessage);
    }

    private PedidoPagamento parsePedido(PaymentMessageDTO dto) {
        String caption = dto.getCaption().trim();
        Matcher matcher = PEDIDO_PATTERN.matcher(caption);

        if (!matcher.matches()) {
            throw new InvalidMessageFormatException(
                "Use: `<valor> <descrição>`\n\n" +
                "*Exemplos:*\n" +
                "• `100 boleto Energia`\n" +
                "• `200 pix Maria`\n" +
                "• `1500 ted Construtora`\n" +
                "• `300 agendamento Luz`\n" +
                "• `50 Almoço` (sem tipo vira OUTRO)\n\n" +
                "O tipo é detectado automaticamente pela palavra-chave na descrição.",
                dto.getChatId());
        }

        String valorStr = matcher.group(1).replace(',', '.');
        BigDecimal valor = new BigDecimal(valorStr);
        String descricao = matcher.group(3);

        return PedidoPagamento.builder()
            .valor(valor)
            .descricao(descricao)
            .telegramUserId(dto.getFromId())
            .telegramMessageId(dto.getExternalId())
            .status(StatusPedido.PENDENTE)
            .requisitanteId(1L)
            .dataPedido(LocalDate.now())
            .tipo(LegendaParser.parseTipo(caption))
            .build();
    }
}
