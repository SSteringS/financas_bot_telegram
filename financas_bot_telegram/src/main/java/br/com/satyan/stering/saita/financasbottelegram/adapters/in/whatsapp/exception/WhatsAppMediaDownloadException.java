package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exception;

public class WhatsAppMediaDownloadException extends RuntimeException {
    private final String waId;

    public WhatsAppMediaDownloadException(String message, String waId, Throwable cause) {
        super(message, cause);
        this.waId = waId;
    }

    public String getWaId() {
        return waId;
    }
}
