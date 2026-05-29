package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service;

public class WhatsAppApiException extends RuntimeException {

    private final Integer errorCode;
    private final String errorTitle;
    private final int httpStatus;
    private final String body;

    public WhatsAppApiException(int httpStatus, String body) {
        super("WhatsApp API error — httpStatus=" + httpStatus);
        this.httpStatus = httpStatus;
        this.body = body;
        this.errorCode = null;
        this.errorTitle = null;
    }

    public WhatsAppApiException(int httpStatus, Integer errorCode, String errorTitle, String body) {
        super("WhatsApp API error — code=" + errorCode + " title=" + errorTitle);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorTitle = errorTitle;
        this.body = body;
    }

    public Integer getErrorCode() { return errorCode; }
    public String getErrorTitle() { return errorTitle; }
    public int getHttpStatus() { return httpStatus; }
    public String getBody() { return body; }
}
