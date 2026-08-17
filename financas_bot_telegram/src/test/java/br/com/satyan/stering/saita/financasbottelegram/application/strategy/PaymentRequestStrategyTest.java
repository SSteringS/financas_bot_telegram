package br.com.satyan.stering.saita.financasbottelegram.application.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.CanalNotificadorPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoUploadS3;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRequestStrategyTest {

    @Mock private SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase;
    @Mock private CanalNotificadorPort canalNotificadorPort;
    @Mock private S3ImageUploadService s3ImageUploadService;

    @InjectMocks private PaymentRequestStrategy strategy;

    // --- supports() ---

    @Test
    void deveSuportarLegendaComValorEDescricao() {
        assertThat(strategy.supports(dtoComCaption("150.00 Almoço no restaurante"))).isTrue();
    }

    @Test
    void deveSuportarValorComVirgula() {
        assertThat(strategy.supports(dtoComCaption("150,50 Jantar"))).isTrue();
    }

    @Test
    void naoDeveSuportarLegendaDeComprovante() {
        assertThat(strategy.supports(dtoComCaption("#123 PIX"))).isFalse();
    }

    @Test
    void naoDeveSuportarLegendaSemValor() {
        assertThat(strategy.supports(dtoComCaption("Apenas descrição"))).isFalse();
    }

    @Test
    void naoDeveSuportarCaptionNulo() {
        assertThat(strategy.supports(dtoComCaption(null))).isFalse();
    }

    // --- process() ---

    @Test
    void deveProcessarPedidoComTodosOsDados() {
        PaymentMessageDTO dto = dtoCompleto("150.50 Almoço com cliente", 12345L, "99", "msg_10", "file_abc");
        when(s3ImageUploadService.uploadFile(any(), any(), any())).thenReturn("https://s3.example.com/foto.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(1L).valor(new BigDecimal("150.50"))
            .descricao("Almoço com cliente").tipo(TipoPagamento.OUTRO).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), eq(12345L))).thenReturn(salvo);

        strategy.process(dto);

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        verify(salvarPedidoPagamentoUsecase).execute(captor.capture(), eq(12345L));
        PedidoPagamento capturado = captor.getValue();
        assertThat(capturado.getValor()).isEqualByComparingTo("150.50");
        assertThat(capturado.getDescricao()).isEqualTo("Almoço com cliente");
        assertThat(capturado.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(capturado.getTelegramUserId()).isEqualTo("99");
        assertThat(capturado.getFileIdTelegram()).isEqualTo("file_abc");
        assertThat(capturado.getImagemUrl()).isEqualTo("https://s3.example.com/foto.jpg");
        assertThat(capturado.getRequisitanteId()).isEqualTo(1L);
        assertThat(capturado.getDataPedido()).isEqualTo(LocalDate.now());
    }

    @Test
    void deveConverterValorComVirgulaParaBigDecimal() {
        PaymentMessageDTO dto = dtoCompleto("99,90 Café", 12345L, "99", "msg_11", "file_001");
        when(s3ImageUploadService.uploadFile(any(), any(), any())).thenReturn("https://s3.example.com/foto.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(2L).valor(new BigDecimal("99.90"))
            .descricao("Café").tipo(TipoPagamento.OUTRO).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(dto);

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        verify(salvarPedidoPagamentoUsecase).execute(captor.capture(), any());
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("99.90");
    }

    @Test
    void deveMostrarTipoNaMensagemDeSucesso() {
        PaymentMessageDTO dto = dtoCompleto("200 pix Maria", 12345L, "99", "msg_12", "file_pix");
        when(s3ImageUploadService.uploadFile(any(), any(), any())).thenReturn("https://s3.example.com/foto.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(3L).valor(new BigDecimal("200"))
            .descricao("pix Maria").tipo(TipoPagamento.PIX).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(dto);

        verify(canalNotificadorPort).enviar(eq(12345L), contains("PIX"));
    }

    @Test
    void deveMostrarDicaQuandoTipoForOUTRO() {
        PaymentMessageDTO dto = dtoCompleto("50 Almoço", 12345L, "99", "msg_13", "file_outro");
        when(s3ImageUploadService.uploadFile(any(), any(), any())).thenReturn("https://s3.example.com/foto.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(4L).valor(new BigDecimal("50"))
            .descricao("Almoço").tipo(TipoPagamento.OUTRO).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(dto);

        verify(canalNotificadorPort).enviar(eq(12345L), contains("Tipo não detectado"));
    }

    @Test
    void devePassarExtensaoPdfParaS3() {
        PaymentMessageDTO dto = dtoComExtensao("500 boleto Energia", 12345L, "file_pdf", "pdf", TipoArquivo.PDF);
        when(s3ImageUploadService.uploadFile(any(), eq("pdf"), any())).thenReturn("https://s3.example.com/file.pdf");
        PedidoPagamento salvo = PedidoPagamento.builder().id(5L).valor(new BigDecimal("500"))
            .descricao("boleto Energia").tipo(TipoPagamento.BOLETO).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(dto);

        verify(s3ImageUploadService).uploadFile(any(), eq("pdf"), eq(TipoUploadS3.PEDIDO));
    }

    @Test
    void deveUsarTipoUploadPedidoNoS3() {
        PaymentMessageDTO dto = dtoCompleto("100 pix Teste", 12345L, "99", "msg_14", "file_chk");
        when(s3ImageUploadService.uploadFile(any(), any(), any())).thenReturn("https://s3.example.com/pedidos/abc.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(9L).valor(new BigDecimal("100"))
            .descricao("pix Teste").tipo(TipoPagamento.PIX).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(dto);

        verify(s3ImageUploadService).uploadFile(any(), any(), eq(TipoUploadS3.PEDIDO));
    }

    @Test
    void deveLancarPhotoProcessingExceptionQuandoSemFileBytes() {
        PaymentMessageDTO dto = PaymentMessageDTO.builder()
            .caption("100 pix Teste")
            .chatId(12345L)
            .fromId("99")
            .build();

        assertThatThrownBy(() -> strategy.process(dto))
                .isInstanceOf(PhotoProcessingException.class);
    }

    // Contrato do metodo publico process(): chamado com legenda fora do formato
    // `<valor> <descricao>`, a classe rejeita a entrada em vez de persistir lixo.
    // NAO e a reproducao de um cenario de usuario: supports() e parsePedido aplicam o
    // mesmo PEDIDO_PATTERN sobre o mesmo caption.trim(), e pelo caminho do dispatcher
    // process() so roda depois de supports() devolver true — logo este throw e
    // inalcancavel em producao hoje. O teste existe para que a redundancia defensiva
    // deixe de ser removivel sem quebrar nada.
    @Test
    void deveLancarInvalidMessageFormatExceptionQuandoLegendaNaoTemValor() {
        PaymentMessageDTO dto = dtoCompleto("Apenas descricao sem valor", 12345L, "99", "msg_15", "file_bad");

        assertThatThrownBy(() -> strategy.process(dto))
            .isInstanceOf(InvalidMessageFormatException.class)
            .hasMessageContaining("<valor> <descrição>")
            .extracting(e -> ((InvalidMessageFormatException) e).getChatId())
            .isEqualTo(12345L);

        verifyNoInteractions(salvarPedidoPagamentoUsecase, canalNotificadorPort);
    }

    // --- helpers ---

    private PaymentMessageDTO dtoComCaption(String caption) {
        return PaymentMessageDTO.builder().caption(caption).build();
    }

    private PaymentMessageDTO dtoCompleto(String caption, Long chatId, String fromId,
            String externalId, String mediaId) {
        return PaymentMessageDTO.builder()
            .caption(caption)
            .chatId(chatId)
            .fromId(fromId)
            .externalId(externalId)
            .mediaId(mediaId)
            .fileBytes(new byte[]{1, 2, 3})
            .fileExtension("jpg")
            .tipoArquivo(TipoArquivo.IMAGEM)
            .build();
    }

    private PaymentMessageDTO dtoComExtensao(String caption, Long chatId, String mediaId,
            String ext, TipoArquivo tipo) {
        return PaymentMessageDTO.builder()
            .caption(caption)
            .chatId(chatId)
            .fromId("99")
            .externalId("msg_ext")
            .mediaId(mediaId)
            .fileBytes(new byte[]{})
            .fileExtension(ext)
            .tipoArquivo(tipo)
            .build();
    }
}
