package br.com.satyan.stering.saita.financasbottelegram.application.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.CanalNotificadorPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoUploadS3;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentProofStrategyTest {

    @Mock private RegistrarComprovanteUsecase registrarComprovanteUsecase;
    @Mock private CanalNotificadorPort canalNotificadorPort;
    @Mock private S3ImageUploadService s3ImageUploadService;

    @InjectMocks private PaymentProofStrategy strategy;

    // --- supports() ---

    @Test
    void deveSuportarLegendaDeComprovante() {
        assertThat(strategy.supports(dtoComCaption("#123 pix"))).isTrue();
    }

    @Test
    void deveSuportarComTipoPagamentoComposto() {
        assertThat(strategy.supports(dtoComCaption("#42 transferencia bancaria"))).isTrue();
    }

    @Test
    void naoDeveSuportarLegendaDePedido() {
        assertThat(strategy.supports(dtoComCaption("150.00 Almoço"))).isFalse();
    }

    @Test
    void naoDeveSuportarLegendaSemHashId() {
        assertThat(strategy.supports(dtoComCaption("123 pix"))).isFalse();
    }

    @Test
    void naoDeveSuportarCaptionNulo() {
        assertThat(strategy.supports(dtoComCaption(null))).isFalse();
    }

    // --- process() ---

    @Test
    void deveProcessarComprovanteComSucesso() {
        PaymentMessageDTO dto = dtoCompleto("#123 PIX", 12345L, "file_xyz", TipoArquivo.IMAGEM, "jpg");
        when(s3ImageUploadService.uploadFile(any(), any(), any())).thenReturn("https://s3.example.com/comprovante.jpg");
        Comprovante salvo = Comprovante.builder().id(1L).pedidoId(123L).tipoPagamento("PIX").build();
        when(registrarComprovanteUsecase.execute(eq(123L), eq("PIX"), eq("file_xyz"), any(), eq(TipoArquivo.IMAGEM), eq(12345L)))
            .thenReturn(salvo);

        strategy.process(dto);

        verify(registrarComprovanteUsecase).execute(eq(123L), eq("PIX"), eq("file_xyz"), any(), eq(TipoArquivo.IMAGEM), eq(12345L));
        verify(canalNotificadorPort).enviar(eq(12345L), any());
    }

    @Test
    void deveLancarInvalidCaptionExceptionQuandoFormatoInvalido() {
        PaymentMessageDTO dto = dtoCompleto("formato-invalido", 12345L, "file_xyz", TipoArquivo.IMAGEM, "jpg");

        assertThatThrownBy(() -> strategy.process(dto))
                .isInstanceOf(InvalidCaptionException.class);
    }

    @Test
    void deveLancarPhotoProcessingExceptionQuandoSemFileBytes() {
        PaymentMessageDTO dto = PaymentMessageDTO.builder()
            .caption("#10 pix")
            .chatId(12345L)
            .build();

        assertThatThrownBy(() -> strategy.process(dto))
                .isInstanceOf(PhotoProcessingException.class);
    }

    @Test
    void deveLancarInvalidCaptionExceptionQuandoCaptionBranco() {
        PaymentMessageDTO dto = PaymentMessageDTO.builder()
            .chatId(12345L)
            .caption("   ")
            .fileBytes(new byte[]{1, 2, 3})
            .build();

        assertThatThrownBy(() -> strategy.process(dto))
                .isInstanceOf(InvalidCaptionException.class);
    }

    @Test
    void deveConverterTipoPagamentoParaMaiusculo() {
        PaymentMessageDTO dto = dtoCompleto("#10 ted", 12345L, "file_abc", TipoArquivo.IMAGEM, "jpg");
        when(s3ImageUploadService.uploadFile(any(), any(), any())).thenReturn("https://s3.example.com/img.jpg");
        Comprovante salvo = Comprovante.builder().id(2L).pedidoId(10L).tipoPagamento("TED").build();
        when(registrarComprovanteUsecase.execute(eq(10L), eq("TED"), any(), any(), eq(TipoArquivo.IMAGEM), eq(12345L)))
            .thenReturn(salvo);

        strategy.process(dto);

        verify(registrarComprovanteUsecase).execute(eq(10L), eq("TED"), any(), any(), eq(TipoArquivo.IMAGEM), eq(12345L));
    }

    @Test
    void devePassarTipoArquivoPdfParaUsecase() {
        PaymentMessageDTO dto = dtoCompleto("#99 boleto", 12345L, "doc_pdf", TipoArquivo.PDF, "pdf");
        when(s3ImageUploadService.uploadFile(any(), eq("pdf"), any())).thenReturn("https://s3.example.com/file.pdf");
        Comprovante salvo = Comprovante.builder().id(4L).pedidoId(99L).tipoPagamento("BOLETO").build();
        when(registrarComprovanteUsecase.execute(eq(99L), eq("BOLETO"), eq("doc_pdf"), any(), eq(TipoArquivo.PDF), eq(12345L)))
            .thenReturn(salvo);

        strategy.process(dto);

        verify(registrarComprovanteUsecase).execute(eq(99L), eq("BOLETO"), eq("doc_pdf"), any(), eq(TipoArquivo.PDF), eq(12345L));
    }

    @Test
    void deveUsarTipoUploadComprovanteNoS3() {
        PaymentMessageDTO dto = dtoCompleto("#20 pix", 12345L, "file_chk", TipoArquivo.IMAGEM, "jpg");
        when(s3ImageUploadService.uploadFile(any(), any(), any())).thenReturn("https://s3.example.com/comprovantes/abc.jpg");
        Comprovante salvo = Comprovante.builder().id(9L).pedidoId(20L).tipoPagamento("PIX").build();
        when(registrarComprovanteUsecase.execute(any(), any(), any(), any(), any(), any())).thenReturn(salvo);

        strategy.process(dto);

        verify(s3ImageUploadService).uploadFile(any(), any(), eq(TipoUploadS3.COMPROVANTE));
    }

    // --- helpers ---

    private PaymentMessageDTO dtoComCaption(String caption) {
        return PaymentMessageDTO.builder().caption(caption).build();
    }

    private PaymentMessageDTO dtoCompleto(String caption, Long chatId, String mediaId,
            TipoArquivo tipo, String ext) {
        return PaymentMessageDTO.builder()
            .caption(caption)
            .chatId(chatId)
            .mediaId(mediaId)
            .fileBytes(new byte[]{1, 2, 3})
            .tipoArquivo(tipo)
            .fileExtension(ext)
            .build();
    }
}
