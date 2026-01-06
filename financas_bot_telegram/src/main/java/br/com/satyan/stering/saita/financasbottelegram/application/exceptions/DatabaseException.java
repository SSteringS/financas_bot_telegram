package br.com.satyan.stering.saita.financasbottelegram.application.exceptions;

public class DatabaseException extends RuntimeException {

    private final Long chatId;

    public DatabaseException(String message, Long chatId, Throwable cause) {
        super(message, cause);
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }
}

