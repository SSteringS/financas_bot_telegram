package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppDocument;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppImage;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto.WhatsAppMessage;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exception.WhatsAppMediaDownloadException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service.WhatsAppApiException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service.WhatsAppMediaDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppMessageMapper {

    private final WhatsAppMediaDownloaderService downloader;

    public WhatsAppMessageMapper(WhatsAppMediaDownloaderService downloader) {
        this.downloader = downloader;
    }

    public PaymentMessageDTO toPaymentMessageDTO(WhatsAppMessage message) {
        String waId = message.from();
        return switch (message.type()) {
            case "text" -> mapText(message, waId);
            case "image" -> mapImage(message, waId);
            case "document" -> mapDocument(message, waId);
            default -> throw new TipoArquivoNaoSuportadoException(
                "Tipo de mensagem '" + message.type() + "' não suportado. Envie foto, documento ou texto.", waId);
        };
    }

    private PaymentMessageDTO mapText(WhatsAppMessage message, String waId) {
        return PaymentMessageDTO.builder()
            .canal(Canal.WHATSAPP)
            .externalId(message.id())
            .chatId(parseChatId(waId))
            .fromId(waId)
            .caption(message.text() != null ? message.text().body() : null)
            .build();
    }

    private PaymentMessageDTO mapImage(WhatsAppMessage message, String waId) {
        WhatsAppImage img = message.image();
        byte[] bytes = baixarMidia(img.id(), waId);
        return PaymentMessageDTO.builder()
            .canal(Canal.WHATSAPP)
            .externalId(message.id())
            .chatId(parseChatId(waId))
            .fromId(waId)
            .caption(img.caption())
            .fileBytes(bytes)
            .fileExtension(extensaoDeMime(img.mimeType()))
            .tipoArquivo(TipoArquivo.IMAGEM)
            .mediaId(img.id())
            .build();
    }

    private PaymentMessageDTO mapDocument(WhatsAppMessage message, String waId) {
        WhatsAppDocument doc = message.document();
        String mime = doc.mimeType() != null ? doc.mimeType() : "";

        TipoArquivo tipoArquivo;
        String extensao;

        if (mime.equals("application/pdf")) {
            tipoArquivo = TipoArquivo.PDF;
            extensao = "pdf";
        } else if (mime.startsWith("image/")) {
            tipoArquivo = TipoArquivo.IMAGEM;
            extensao = extensaoDeMime(mime);
        } else {
            throw new TipoArquivoNaoSuportadoException(
                "Tipo de arquivo '" + mime + "' não suportado. Envie foto, imagem ou PDF.", waId);
        }

        byte[] bytes = baixarMidia(doc.id(), waId);
        return PaymentMessageDTO.builder()
            .canal(Canal.WHATSAPP)
            .externalId(message.id())
            .chatId(parseChatId(waId))
            .fromId(waId)
            .caption(doc.caption())
            .fileBytes(bytes)
            .fileExtension(extensao)
            .tipoArquivo(tipoArquivo)
            .mediaId(doc.id())
            .build();
    }

    private byte[] baixarMidia(String mediaId, String waId) {
        try {
            return downloader.baixar(mediaId);
        } catch (WhatsAppApiException e) {
            throw new WhatsAppMediaDownloadException(
                "Falha ao baixar mídia mediaId=" + mediaId, waId, e);
        }
    }

    // wa_id é número E.164 sem '+' (ex: 5511999998888) — cabe em Long
    private Long parseChatId(String waId) {
        try {
            return Long.parseLong(waId);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String extensaoDeMime(String mime) {
        return switch (mime) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };
    }
}
