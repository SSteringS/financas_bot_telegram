package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
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
import org.telegram.telegrambots.meta.api.objects.User;

@ExtendWith(MockitoExtension.class)
class TelegramMessageMapperTest {

    @Mock private TelegramFileDownloaderService telegramFileDownloaderService;

    @InjectMocks private TelegramMessageMapper mapper;

    // --- foto ---

    @Test
    void deveMapeiarFotoParaDTO() {
        byte[] bytes = {1, 2, 3};
        when(telegramFileDownloaderService.downloadImageByFileId("file_photo")).thenReturn(bytes);

        Update update = updateComFoto("#123 pix", 100L, 99L, "file_photo");
        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(update);

        assertThat(dto.getChatId()).isEqualTo(100L);
        assertThat(dto.getFromId()).isEqualTo("99");
        assertThat(dto.getCaption()).isEqualTo("#123 pix");
        assertThat(dto.getMediaId()).isEqualTo("file_photo");
        assertThat(dto.getFileBytes()).isEqualTo(bytes);
        assertThat(dto.getFileExtension()).isEqualTo("jpg");
        assertThat(dto.getTipoArquivo()).isEqualTo(TipoArquivo.IMAGEM);
    }

    // --- documento PDF ---

    @Test
    void deveMapeiarDocumentoPdfParaDTO() {
        byte[] bytes = {4, 5, 6};
        when(telegramFileDownloaderService.downloadImageByFileId("doc_pdf")).thenReturn(bytes);

        Update update = updateComDocument("500 boleto Energia", 100L, 99L, "doc_pdf", "application/pdf");
        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(update);

        assertThat(dto.getTipoArquivo()).isEqualTo(TipoArquivo.PDF);
        assertThat(dto.getFileExtension()).isEqualTo("pdf");
        assertThat(dto.getFileBytes()).isEqualTo(bytes);
        assertThat(dto.getMediaId()).isEqualTo("doc_pdf");
    }

    // --- documento imagem ---

    @Test
    void deveMapeiarDocumentoImagemJpegParaDTO() {
        when(telegramFileDownloaderService.downloadImageByFileId("doc_jpg")).thenReturn(new byte[]{});

        Update update = updateComDocument("50 pix", 100L, 99L, "doc_jpg", "image/jpeg");
        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(update);

        assertThat(dto.getTipoArquivo()).isEqualTo(TipoArquivo.IMAGEM);
        assertThat(dto.getFileExtension()).isEqualTo("jpg");
    }

    @Test
    void deveMapeiarDocumentoImagemPngParaDTO() {
        when(telegramFileDownloaderService.downloadImageByFileId("doc_png")).thenReturn(new byte[]{});

        Update update = updateComDocument("50 pix", 100L, 99L, "doc_png", "image/png");
        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(update);

        assertThat(dto.getFileExtension()).isEqualTo("png");
    }

    @Test
    void deveMapeiarOctetStreamComoImagem() {
        when(telegramFileDownloaderService.downloadImageByFileId("doc_ws")).thenReturn(new byte[]{});

        Update update = updateComDocument("77 ted", 100L, 99L, "doc_ws", "application/octet-stream");
        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(update);

        assertThat(dto.getTipoArquivo()).isEqualTo(TipoArquivo.IMAGEM);
        assertThat(dto.getFileExtension()).isEqualTo("jpg");
    }

    // --- documento MIME não suportado ---

    @Test
    void deveLancarTipoArquivoNaoSuportadoParaVideo() {
        Update update = updateComDocument("200 pix Teste", 100L, 99L, "doc_vid", "video/mp4");

        assertThatThrownBy(() -> mapper.toPaymentMessageDTO(update))
                .isInstanceOf(TipoArquivoNaoSuportadoException.class)
                .hasMessageContaining("video/mp4");
    }

    // --- mensagem texto puro (sem anexo) ---

    @Test
    void deveMapeiarMensagemTextoParaDTO() {
        Update update = updateTexto("olá", 100L, 99L);
        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(update);

        assertThat(dto.getCaption()).isEqualTo("olá");
        assertThat(dto.getFileBytes()).isNull();
        assertThat(dto.getMediaId()).isNull();
        assertThat(dto.getTipoArquivo()).isNull();
    }

    // --- helpers ---

    private Update updateComFoto(String caption, Long chatId, Long userId, String fileId) {
        Message message = new Message();
        message.setCaption(caption);
        message.setMessageId(1);

        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);

        User user = new User();
        user.setId(userId);
        message.setFrom(user);

        PhotoSize photo = new PhotoSize();
        photo.setFileId(fileId);
        photo.setFileSize(2000);
        message.setPhoto(List.of(photo));

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update updateComDocument(String caption, Long chatId, Long userId,
            String fileId, String mimeType) {
        Message message = new Message();
        message.setCaption(caption);
        message.setMessageId(1);

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

    private Update updateTexto(String text, Long chatId, Long userId) {
        Message message = new Message();
        message.setText(text);
        message.setMessageId(1);

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
