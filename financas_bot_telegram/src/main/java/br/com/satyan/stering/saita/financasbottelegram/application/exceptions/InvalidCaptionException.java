package br.com.satyan.stering.saita.financasbottelegram.application.exceptions;

public class InvalidCaptionException extends RuntimeException {

    private final Long chatId;

    public InvalidCaptionException(String message, Long chatId) {
        super(message);
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }
}

