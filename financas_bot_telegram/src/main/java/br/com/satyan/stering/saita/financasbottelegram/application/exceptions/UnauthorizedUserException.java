package br.com.satyan.stering.saita.financasbottelegram.application.exceptions;

public class UnauthorizedUserException extends RuntimeException {

    private final Long chatId;

    public UnauthorizedUserException(String message, Long chatId) {
        super(message);
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }
}

