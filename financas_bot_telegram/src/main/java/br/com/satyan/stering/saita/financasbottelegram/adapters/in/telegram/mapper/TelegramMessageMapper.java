package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.TipoArquivoNaoSuportadoException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import java.util.Comparator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramMessageMapper {

    private final TelegramFileDownloaderService telegramFileDownloaderService;

    public TelegramMessageMapper(TelegramFileDownloaderService telegramFileDownloaderService) {
        this.telegramFileDownloaderService = telegramFileDownloaderService;
    }

    /**
     * Converte um Update do Telegram em PaymentMessageDTO canal-agnóstico.
     * Faz o download do arquivo (foto/documento) neste ponto — a strategy recebe os bytes prontos.
     */
    public PaymentMessageDTO toPaymentMessageDTO(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            return mapPhoto(message, chatId);
        }

        if (message.getDocument() != null) {
            return mapDocument(message, chatId);
        }

        // Mensagem texto-puro (sem anexo)
        return PaymentMessageDTO.builder()
            .externalId(message.getMessageId().toString())
            .chatId(chatId)
            .fromId(message.getFrom().getId().toString())
            .caption(message.getText())
            .build();
    }

    private PaymentMessageDTO mapPhoto(Message message, Long chatId) {
        PhotoSize photo = message.getPhoto().stream()
            .max(Comparator.comparing(PhotoSize::getFileSize))
            .orElseThrow(() -> new PhotoProcessingException("Foto sem file_id válido.", chatId));

        String fileId = photo.getFileId();
        byte[] bytes = telegramFileDownloaderService.downloadImageByFileId(fileId);

        return PaymentMessageDTO.builder()
            .externalId(message.getMessageId().toString())
            .chatId(chatId)
            .fromId(message.getFrom().getId().toString())
            .caption(message.getCaption())
            .fileBytes(bytes)
            .fileExtension("jpg")
            .tipoArquivo(TipoArquivo.IMAGEM)
            .mediaId(fileId)
            .build();
    }

    private PaymentMessageDTO mapDocument(Message message, Long chatId) {
        Document doc = message.getDocument();
        String mime = doc.getMimeType() != null ? doc.getMimeType() : "";
        String fileId = doc.getFileId();

        TipoArquivo tipoArquivo;
        String extensao;

        if (mime.equals("application/pdf")) {
            tipoArquivo = TipoArquivo.PDF;
            extensao = "pdf";
        } else if (mime.startsWith("image/")) {
            tipoArquivo = TipoArquivo.IMAGEM;
            extensao = extensaoDeMime(mime);
        } else if (mime.equals("application/octet-stream") || mime.isBlank()) {
            tipoArquivo = TipoArquivo.IMAGEM;
            extensao = "jpg";
        } else {
            throw new TipoArquivoNaoSuportadoException(
                "Tipo de arquivo '" + mime + "' não suportado. Envie foto, imagem ou PDF.", chatId);
        }

        byte[] bytes = telegramFileDownloaderService.downloadImageByFileId(fileId);

        return PaymentMessageDTO.builder()
            .externalId(message.getMessageId().toString())
            .chatId(chatId)
            .fromId(message.getFrom().getId().toString())
            .caption(message.getCaption())
            .fileBytes(bytes)
            .fileExtension(extensao)
            .tipoArquivo(tipoArquivo)
            .mediaId(fileId)
            .build();
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
