package br.com.satyan.stering.saita.financasbottelegram.application.exceptions;

public class BusinessRuleException extends RuntimeException {

    private final Long chatId;

    public BusinessRuleException(String message, Long chatId) {
        super(message);
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }
}

