package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception;

public class InvalidMessageFormatException extends RuntimeException {

    private final Long chatId;

    public InvalidMessageFormatException(String message, Long chatId) {
        super(message);
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }
}

