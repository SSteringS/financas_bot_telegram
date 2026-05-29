package br.com.satyan.stering.saita.financasbottelegram.application.strategy;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.CanalNotificadorPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoUploadS3;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// NOTA ARQUITETURAL: importa de adapters/in/telegram/exception/ e adapters/out/s3/service/ —
// violações hexagonais conhecidas (ver PaymentRequestStrategy para detalhes).
@Component
public class PaymentProofStrategy implements MensagemProcessingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProofStrategy.class);
    private static final Pattern COMPROVANTE_PATTERN = Pattern.compile("#(\\d+)\\s+(.+)");

    private final RegistrarComprovanteUsecase registrarComprovanteUsecase;
    private final CanalNotificadorPort canalNotificadorPort;
    private final S3ImageUploadService s3ImageUploadService;

    public PaymentProofStrategy(RegistrarComprovanteUsecase registrarComprovanteUsecase,
        CanalNotificadorPort canalNotificadorPort,
        S3ImageUploadService s3ImageUploadService) {
        this.registrarComprovanteUsecase = registrarComprovanteUsecase;
        this.canalNotificadorPort = canalNotificadorPort;
        this.s3ImageUploadService = s3ImageUploadService;
    }

    @Override
    public boolean supports(PaymentMessageDTO dto) {
        String caption = dto.getCaption();
        if (caption == null) return false;
        return COMPROVANTE_PATTERN.matcher(caption.trim()).matches();
    }

    @Override
    public void process(PaymentMessageDTO dto) {
        Long chatId = dto.getChatId();
        String caption = dto.getCaption();

        if (dto.getFileBytes() == null) {
            throw new PhotoProcessingException(
                "Nenhuma imagem ou anexo encontrado. Envie como foto ou anexo.", chatId);
        }

        if (caption == null || caption.isBlank()) {
            throw new InvalidCaptionException(
                "A foto do comprovante precisa de uma legenda.\n\n" +
                "Use: `#<id_pedido> <tipo_pagamento>`\n\n" +
                "*Exemplos:*\n" +
                "• `#123 pix`\n" +
                "• `#456 boleto`\n" +
                "• `#789 ted`\n\n" +
                "O `<id_pedido>` é o número que apareceu quando você registrou o pedido.", chatId);
        }

        logger.info("Estratégia de Comprovante de Pagamento ativada. MediaId: {}, Legenda: '{}'",
            dto.getMediaId(), caption);

        Matcher matcher = COMPROVANTE_PATTERN.matcher(caption.trim());
        if (!matcher.matches()) {
            throw new InvalidCaptionException(
                "Formato da legenda inválido.\n\n" +
                "Use: `#<id_pedido> <tipo_pagamento>`\n\n" +
                "*Exemplos:*\n" +
                "• `#123 pix`\n" +
                "• `#456 boleto`\n" +
                "• `#789 ted`\n\n" +
                "O `<id_pedido>` é o número que apareceu quando você registrou o pedido.", chatId);
        }

        Long pedidoId = Long.parseLong(matcher.group(1));
        String tipoPagamento = matcher.group(2).toUpperCase();

        String s3Url = s3ImageUploadService.uploadFile(
            dto.getFileBytes(), dto.getFileExtension(), TipoUploadS3.COMPROVANTE);
        logger.info("Comprovante enviado para S3: {}", s3Url);

        Comprovante comprovanteSalvo = registrarComprovanteUsecase.execute(
            pedidoId, tipoPagamento, dto.getMediaId(), s3Url, dto.getTipoArquivo(), chatId);
        logger.info("Comprovante para o pedido {} registrado com sucesso.", pedidoId);

        String successMessage = String.format(
            "✅ Comprovante registrado!\n\n*Pedido:* #%d\n*Tipo:* %s",
            comprovanteSalvo.getPedidoId(), comprovanteSalvo.getTipoPagamento());
        canalNotificadorPort.enviar(chatId, successMessage);
    }
}
