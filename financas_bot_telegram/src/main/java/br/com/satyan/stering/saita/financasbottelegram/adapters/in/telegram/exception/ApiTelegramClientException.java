package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception;

public class ApiTelegramClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ApiTelegramClientException(String message) {
        super(message);
    }

    public ApiTelegramClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiTelegramClientException(Throwable cause) {
        super(cause);
    }

}
