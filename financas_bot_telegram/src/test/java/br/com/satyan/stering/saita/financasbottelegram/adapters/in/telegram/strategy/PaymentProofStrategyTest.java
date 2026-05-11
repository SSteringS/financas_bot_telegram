package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
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

    // --- process() ---

    @Test
    void deveProcessarComprovanteComSucesso() {
        Update update = updateComFoto("#123 PIX", 12345L, "file_xyz");
        when(telegramFileDownloaderService.downloadImageByFileId("file_xyz")).thenReturn(new byte[]{1, 2, 3});
        when(s3ImageUploadService.uploadImage(any())).thenReturn("https://s3.example.com/comprovante.jpg");
        Comprovante salvo = Comprovante.builder().id(1L).pedidoId(123L).build();
        when(registrarComprovanteUsecase.execute(eq(123L), eq("PIX"), eq("file_xyz"), any(), eq(12345L))).thenReturn(salvo);

        strategy.process(update);

        verify(registrarComprovanteUsecase).execute(eq(123L), eq("PIX"), eq("file_xyz"), any(), eq(12345L));
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
        when(s3ImageUploadService.uploadImage(any())).thenReturn("https://s3.example.com/img.jpg");
        Comprovante salvo = Comprovante.builder().id(2L).pedidoId(10L).build();
        when(registrarComprovanteUsecase.execute(eq(10L), eq("TED"), any(), any(), eq(12345L))).thenReturn(salvo);

        strategy.process(update);

        verify(registrarComprovanteUsecase).execute(eq(10L), eq("TED"), any(), any(), eq(12345L));
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
}
