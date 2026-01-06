package br.com.satyan.stering.saita.financasbottelegram.application.exceptions;

public class PhotoProcessingException extends RuntimeException {

    private final Long chatId;

    public PhotoProcessingException(String message, Long chatId) {
        super(message);
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }
}

