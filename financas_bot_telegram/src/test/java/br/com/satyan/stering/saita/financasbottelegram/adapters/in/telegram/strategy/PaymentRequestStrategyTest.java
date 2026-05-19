package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@ExtendWith(MockitoExtension.class)
class PaymentRequestStrategyTest {

    @Mock private SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase;
    @Mock private TelegramMessageSenderService telegramMessageSenderService;
    @Mock private S3ImageUploadService s3ImageUploadService;
    @Mock private TelegramFileDownloaderService telegramFileDownloaderService;

    @InjectMocks private PaymentRequestStrategy strategy;

    // --- supports() ---

    @Test
    void deveSuportarLegendaComValorEDescricao() {
        assertThat(strategy.supports(updateComLegenda("150.00 Almoço no restaurante"))).isTrue();
    }

    @Test
    void deveSuportarValorComVirgula() {
        assertThat(strategy.supports(updateComLegenda("150,50 Jantar"))).isTrue();
    }

    @Test
    void naoDeveSuportarLegendaDeComprovante() {
        assertThat(strategy.supports(updateComLegenda("#123 PIX"))).isFalse();
    }

    @Test
    void naoDeveSuportarLegendaSemValor() {
        assertThat(strategy.supports(updateComLegenda("Apenas descrição"))).isFalse();
    }

    // --- process() ---

    @Test
    void deveProcessarPedidoComTodosOsDados() {
        Update update = updateCompleto("150.50 Almoço com cliente", 12345L, 99L, 10, "file_abc");
        when(telegramFileDownloaderService.downloadImageByFileId("file_abc")).thenReturn(new byte[]{1, 2, 3});
        when(s3ImageUploadService.uploadFile(any(), any())).thenReturn("https://s3.example.com/foto.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(1L).valor(new BigDecimal("150.50")).descricao("Almoço com cliente").tipo(TipoPagamento.OUTRO).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), eq(12345L))).thenReturn(salvo);

        strategy.process(update);

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
        Update update = updateCompleto("99,90 Café", 12345L, 99L, 10, "file_001");
        when(telegramFileDownloaderService.downloadImageByFileId(any())).thenReturn(new byte[]{});
        when(s3ImageUploadService.uploadFile(any(), any())).thenReturn("https://s3.example.com/foto.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(2L).valor(new BigDecimal("99.90")).descricao("Café").tipo(TipoPagamento.OUTRO).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(update);

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        verify(salvarPedidoPagamentoUsecase).execute(captor.capture(), any());
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("99.90");
    }

    @Test
    void deveMostrarTipoNaMensagemDeSucesso() {
        Update update = updateCompleto("200 pix Maria", 12345L, 99L, 10, "file_pix");
        when(telegramFileDownloaderService.downloadImageByFileId(any())).thenReturn(new byte[]{});
        when(s3ImageUploadService.uploadFile(any(), any())).thenReturn("https://s3.example.com/foto.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(3L).valor(new BigDecimal("200")).descricao("pix Maria").tipo(TipoPagamento.PIX).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(update);

        verify(telegramMessageSenderService).sendMessage(eq(12345L), contains("PIX"));
    }

    @Test
    void deveMostrarDicaQuandoTipoForOUTRO() {
        Update update = updateCompleto("50 Almoço", 12345L, 99L, 10, "file_outro");
        when(telegramFileDownloaderService.downloadImageByFileId(any())).thenReturn(new byte[]{});
        when(s3ImageUploadService.uploadFile(any(), any())).thenReturn("https://s3.example.com/foto.jpg");
        PedidoPagamento salvo = PedidoPagamento.builder().id(4L).valor(new BigDecimal("50")).descricao("Almoço").tipo(TipoPagamento.OUTRO).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(update);

        verify(telegramMessageSenderService).sendMessage(eq(12345L), contains("Tipo não detectado"));
    }

    @Test
    void deveAceitarDocumentPdfComoPedido() {
        Update update = updateComDocument("500 boleto Energia", 12345L, 99L, 10, "doc_pdf", "application/pdf");
        when(telegramFileDownloaderService.downloadImageByFileId("doc_pdf")).thenReturn(new byte[]{});
        when(s3ImageUploadService.uploadFile(any(), eq("pdf"))).thenReturn("https://s3.example.com/file.pdf");
        PedidoPagamento salvo = PedidoPagamento.builder().id(5L).valor(new BigDecimal("500")).descricao("boleto Energia").tipo(TipoPagamento.BOLETO).build();
        when(salvarPedidoPagamentoUsecase.execute(any(), any())).thenReturn(salvo);

        strategy.process(update);

        verify(s3ImageUploadService).uploadFile(any(), eq("pdf"));
    }

    @Test
    void deveLancarTipoArquivoNaoSuportadoParaVideoEmPedido() {
        Update update = updateComDocument("200 pix Teste", 12345L, 99L, 10, "doc_vid", "video/mp4");

        assertThatThrownBy(() -> strategy.process(update))
                .isInstanceOf(TipoArquivoNaoSuportadoException.class)
                .hasMessageContaining("video/mp4");
    }

    @Test
    void deveLancarPhotoProcessingExceptionQuandoSemFotoEDocument() {
        Update update = updateSemAnexo("100 pix Teste", 12345L, 99L, 10);

        assertThatThrownBy(() -> strategy.process(update))
                .isInstanceOf(PhotoProcessingException.class);
    }

    // --- helpers ---

    private Update updateComLegenda(String legenda) {
        Message message = new Message();
        message.setCaption(legenda);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update updateCompleto(String legenda, Long chatId, Long userId, Integer messageId, String fileId) {
        Message message = new Message();
        message.setCaption(legenda);
        message.setMessageId(messageId);

        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);

        User user = new User();
        user.setId(userId);
        message.setFrom(user);

        PhotoSize photo = new PhotoSize();
        photo.setFileId(fileId);
        photo.setFileSize(1000);
        message.setPhoto(List.of(photo));

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update updateComDocument(String legenda, Long chatId, Long userId, Integer messageId, String fileId, String mimeType) {
        Message message = new Message();
        message.setCaption(legenda);
        message.setMessageId(messageId);

        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);

        User user = new User();
        user.setId(userId);
        message.setFrom(user);

        Document doc = new Document();
        doc.setFileId(fileId);
        doc.setMimeType(mimeType);
        message.setDocument(doc);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update updateSemAnexo(String legenda, Long chatId, Long userId, Integer messageId) {
        Message message = new Message();
        message.setCaption(legenda);
        message.setMessageId(messageId);

        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);

        User user = new User();
        user.setId(userId);
        message.setFrom(user);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
