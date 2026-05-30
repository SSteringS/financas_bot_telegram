package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppDocument;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppImage;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppMessage;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppText;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service.WhatsAppMediaDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsAppMessageMapperTest {

    @Mock private WhatsAppMediaDownloaderService downloader;
    @InjectMocks private WhatsAppMessageMapper mapper;

    private static final String WA_ID = "5511999998888";
    private static final String WAMID = "wamid.ABCDEF123456";
    private static final byte[] FAKE_BYTES = new byte[]{1, 2, 3};

    @Test
    void textoPuro_retornaDtoSemMedia() {
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "1234567890", "text",
            new WhatsAppText("150.50 Almoço"), null, null);

        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(msg);

        assertThat(dto.getCanal()).isEqualTo(Canal.WHATSAPP);
        assertThat(dto.getExternalId()).isEqualTo(WAMID);
        assertThat(dto.getChatId()).isEqualTo(Long.parseLong(WA_ID));
        assertThat(dto.getFromId()).isEqualTo(WA_ID);
        assertThat(dto.getCaption()).isEqualTo("150.50 Almoço");
        assertThat(dto.getFileBytes()).isNull();
        assertThat(dto.getTipoArquivo()).isNull();
        assertThat(dto.getMediaId()).isNull();
    }

    @Test
    void imagemComCaption_retornaDtoComBytesEImagem() {
        WhatsAppImage img = new WhatsAppImage("media-id-img", "image/jpeg", "abc123", "200.00 Jantar");
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "1234567890", "image",
            null, img, null);
        when(downloader.baixar("media-id-img")).thenReturn(FAKE_BYTES);

        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(msg);

        assertThat(dto.getCanal()).isEqualTo(Canal.WHATSAPP);
        assertThat(dto.getExternalId()).isEqualTo(WAMID);
        assertThat(dto.getCaption()).isEqualTo("200.00 Jantar");
        assertThat(dto.getFileBytes()).isEqualTo(FAKE_BYTES);
        assertThat(dto.getTipoArquivo()).isEqualTo(TipoArquivo.IMAGEM);
        assertThat(dto.getFileExtension()).isEqualTo("jpg");
        assertThat(dto.getMediaId()).isEqualTo("media-id-img");
        verify(downloader).baixar("media-id-img");
    }

    @Test
    void imagemPng_extensaoCorreta() {
        WhatsAppImage img = new WhatsAppImage("media-id", "image/png", null, null);
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "ts", "image", null, img, null);
        when(downloader.baixar("media-id")).thenReturn(FAKE_BYTES);

        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(msg);

        assertThat(dto.getFileExtension()).isEqualTo("png");
        assertThat(dto.getTipoArquivo()).isEqualTo(TipoArquivo.IMAGEM);
    }

    @Test
    void documentoPdf_retornaTipoArquivoPdf() {
        WhatsAppDocument doc = new WhatsAppDocument("media-doc", "application/pdf", null, "comprovante.pdf", "#123 PIX");
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "ts", "document", null, null, doc);
        when(downloader.baixar("media-doc")).thenReturn(FAKE_BYTES);

        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(msg);

        assertThat(dto.getTipoArquivo()).isEqualTo(TipoArquivo.PDF);
        assertThat(dto.getFileExtension()).isEqualTo("pdf");
        assertThat(dto.getCaption()).isEqualTo("#123 PIX");
        assertThat(dto.getFileBytes()).isEqualTo(FAKE_BYTES);
    }

    @Test
    void documentoImagemJpeg_retornaTipoArquivoImagem() {
        WhatsAppDocument doc = new WhatsAppDocument("media-doc", "image/jpeg", null, "foto.jpg", "100.00 Teste");
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "ts", "document", null, null, doc);
        when(downloader.baixar("media-doc")).thenReturn(FAKE_BYTES);

        PaymentMessageDTO dto = mapper.toPaymentMessageDTO(msg);

        assertThat(dto.getTipoArquivo()).isEqualTo(TipoArquivo.IMAGEM);
        assertThat(dto.getFileExtension()).isEqualTo("jpg");
    }

    @Test
    void documentoTipoNaoSuportado_lancaException() {
        WhatsAppDocument doc = new WhatsAppDocument("media-doc", "application/zip", null, "arquivo.zip", null);
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "ts", "document", null, null, doc);

        assertThatThrownBy(() -> mapper.toPaymentMessageDTO(msg))
            .isInstanceOf(TipoArquivoNaoSuportadoException.class)
            .hasMessageContaining("application/zip");
    }

    @Test
    void tipoAudio_lancaException() {
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "ts", "audio", null, null, null);

        assertThatThrownBy(() -> mapper.toPaymentMessageDTO(msg))
            .isInstanceOf(TipoArquivoNaoSuportadoException.class)
            .hasMessageContaining("audio");
    }

    @Test
    void tipoVideo_lancaException() {
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "ts", "video", null, null, null);

        assertThatThrownBy(() -> mapper.toPaymentMessageDTO(msg))
            .isInstanceOf(TipoArquivoNaoSuportadoException.class)
            .hasMessageContaining("video");
    }

    @Test
    void tipoLocation_lancaException() {
        WhatsAppMessage msg = new WhatsAppMessage(WA_ID, WAMID, "ts", "location", null, null, null);

        assertThatThrownBy(() -> mapper.toPaymentMessageDTO(msg))
            .isInstanceOf(TipoArquivoNaoSuportadoException.class);
    }
}
