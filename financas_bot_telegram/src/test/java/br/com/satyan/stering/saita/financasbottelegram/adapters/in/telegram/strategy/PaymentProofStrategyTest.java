package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

@ExtendWith(MockitoExtension.class)
class PaymentProofStrategyTest {

    @Mock private RegistrarComprovanteUsecase registrarComprovanteUsecase;
    @Mock private TelegramMessageSenderService telegramMessageSenderService;
    @Mock private S3ImageUploadService s3ImageUploadService;
    @Mock private TelegramFileDownloaderService telegramFileDownloaderService;

    @InjectMocks private PaymentProofStrategy strategy;

    // --- supports() ---

    @Test
    void deveSuportarLegendaDeComprovante() {
        assertThat(strategy.supports(updateComLegenda("#123 pix"))).isTrue();
    }

    @Test
    void deveSuportarComTipoPagamentoComposto() {
        assertThat(strategy.supports(updateComLegenda("#42 transferencia bancaria"))).isTrue();
    }

    @Test
    void naoDeveSuportarLegendaDePedido() {
        assertThat(strategy.supports(updateComLegenda("150.00 Almoço"))).isFalse();
    }

    @Test
    void naoDeveSuportarLegendaSemHashId() {
        assertThat(strategy.supports(updateComLegenda("123 pix"))).isFalse();
    }

    // --- process() com photo ---

    @Test
    void deveProcessarComprovanteComSucesso() {
        Update update = updateComFoto("#123 PIX", 12345L, "file_xyz");
        when(telegramFileDownloaderService.downloadImageByFileId("file_xyz")).thenReturn(new byte[]{1, 2, 3});
        when(s3ImageUploadService.uploadFile(any(), any())).thenReturn("https://s3.example.com/comprovante.jpg");
        Comprovante salvo = Comprovante.builder().id(1L).pedidoId(123L).tipoPagamento("PIX").build();
        when(registrarComprovanteUsecase.execute(eq(123L), eq("PIX"), eq("file_xyz"), any(), eq(TipoArquivo.IMAGEM), eq(12345L))).thenReturn(salvo);

        strategy.process(update);

        verify(registrarComprovanteUsecase).execute(eq(123L), eq("PIX"), eq("file_xyz"), any(), eq(TipoArquivo.IMAGEM), eq(12345L));
        verify(telegramMessageSenderService).sendMessage(eq(12345L), any());
    }

    @Test
    void deveLancarInvalidCaptionExceptionQuandoFormatoInvalido() {
        Update update = updateComFoto("formato-invalido", 12345L, "file_xyz");

        assertThatThrownBy(() -> strategy.process(update))
                .isInstanceOf(InvalidCaptionException.class);
    }

    @Test
    void deveConverterTipoPagamentoParaMaiusculo() {
        Update update = updateComFoto("#10 ted", 12345L, "file_abc");
        when(telegramFileDownloaderService.downloadImageByFileId(any())).thenReturn(new byte[]{});
        when(s3ImageUploadService.uploadFile(any(), any())).thenReturn("https://s3.example.com/img.jpg");
        Comprovante salvo = Comprovante.builder().id(2L).pedidoId(10L).tipoPagamento("TED").build();
        when(registrarComprovanteUsecase.execute(eq(10L), eq("TED"), any(), any(), eq(TipoArquivo.IMAGEM), eq(12345L))).thenReturn(salvo);

        strategy.process(update);

        verify(registrarComprovanteUsecase).execute(eq(10L), eq("TED"), any(), any(), eq(TipoArquivo.IMAGEM), eq(12345L));
    }

    // --- process() com document ---

    @Test
    void deveAceitarDocumentComMimeTypeImageJpeg() {
        Update update = updateComDocument("#50 pix", 12345L, "doc_jpg", "image/jpeg");
        when(telegramFileDownloaderService.downloadImageByFileId("doc_jpg")).thenReturn(new byte[]{});
        when(s3ImageUploadService.uploadFile(any(), eq("jpg"))).thenReturn("https://s3.example.com/file.jpg");
        Comprovante salvo = Comprovante.builder().id(3L).pedidoId(50L).tipoPagamento("PIX").build();
        when(registrarComprovanteUsecase.execute(eq(50L), eq("PIX"), eq("doc_jpg"), any(), eq(TipoArquivo.IMAGEM), eq(12345L))).thenReturn(salvo);

        strategy.process(update);

        verify(registrarComprovanteUsecase).execute(eq(50L), eq("PIX"), eq("doc_jpg"), any(), eq(TipoArquivo.IMAGEM), eq(12345L));
    }

    @Test
    void deveAceitarDocumentComMimeTypePdf() {
        Update update = updateComDocument("#99 boleto", 12345L, "doc_pdf", "application/pdf");
        when(telegramFileDownloaderService.downloadImageByFileId("doc_pdf")).thenReturn(new byte[]{});
        when(s3ImageUploadService.uploadFile(any(), eq("pdf"))).thenReturn("https://s3.example.com/file.pdf");
        Comprovante salvo = Comprovante.builder().id(4L).pedidoId(99L).tipoPagamento("BOLETO").build();
        when(registrarComprovanteUsecase.execute(eq(99L), eq("BOLETO"), eq("doc_pdf"), any(), eq(TipoArquivo.PDF), eq(12345L))).thenReturn(salvo);

        strategy.process(update);

        verify(registrarComprovanteUsecase).execute(eq(99L), eq("BOLETO"), eq("doc_pdf"), any(), eq(TipoArquivo.PDF), eq(12345L));
    }

    @Test
    void deveAceitarDocumentOctetStreamComoImagem() {
        Update update = updateComDocument("#77 ted", 12345L, "doc_ws", "application/octet-stream");
        when(telegramFileDownloaderService.downloadImageByFileId("doc_ws")).thenReturn(new byte[]{});
        when(s3ImageUploadService.uploadFile(any(), eq("jpg"))).thenReturn("https://s3.example.com/file.jpg");
        Comprovante salvo = Comprovante.builder().id(5L).pedidoId(77L).tipoPagamento("TED").build();
        when(registrarComprovanteUsecase.execute(eq(77L), eq("TED"), eq("doc_ws"), any(), eq(TipoArquivo.IMAGEM), eq(12345L))).thenReturn(salvo);

        strategy.process(update);

        verify(registrarComprovanteUsecase).execute(eq(77L), eq("TED"), eq("doc_ws"), any(), eq(TipoArquivo.IMAGEM), eq(12345L));
    }

    @Test
    void deveLancarTipoArquivoNaoSuportadoParaVideo() {
        Update update = updateComDocument("#10 pix", 12345L, "doc_vid", "video/mp4");

        assertThatThrownBy(() -> strategy.process(update))
                .isInstanceOf(TipoArquivoNaoSuportadoException.class)
                .hasMessageContaining("video/mp4");
    }

    @Test
    void deveLancarPhotoProcessingExceptionQuandoSemFotoEDocument() {
        Update update = updateSemAnexo("#10 pix", 12345L);

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

    private Update updateComFoto(String legenda, Long chatId, String fileId) {
        Message message = new Message();
        message.setCaption(legenda);

        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);

        PhotoSize photo = new PhotoSize();
        photo.setFileId(fileId);
        photo.setFileSize(2000);
        message.setPhoto(List.of(photo));

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update updateComDocument(String legenda, Long chatId, String fileId, String mimeType) {
        Message message = new Message();
        message.setCaption(legenda);

        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);

        Document doc = new Document();
        doc.setFileId(fileId);
        doc.setMimeType(mimeType);
        message.setDocument(doc);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update updateSemAnexo(String legenda, Long chatId) {
        Message message = new Message();
        message.setCaption(legenda);

        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
