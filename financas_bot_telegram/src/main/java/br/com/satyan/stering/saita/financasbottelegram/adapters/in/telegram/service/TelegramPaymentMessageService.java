package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.util.TelegramMessageParser;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ProcessPaymentMessageUsecase;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class TelegramPaymentMessageService {

    private TelegramMessageParser parser;
    private ProcessPaymentMessageUsecase processPaymentMessageUsecase;
    private final String TELEGRAM_TOKEN =System.getenv("TELEGRAM_TOKEN");

    public TelegramPaymentMessageService(TelegramMessageParser parser, ProcessPaymentMessageUsecase processPaymentMessageUsecase) {
        this.parser = parser;
        this.processPaymentMessageUsecase = processPaymentMessageUsecase;
    }
    public void processPaymentMessage(String message) {
        JSONObject json = new JSONObject(message);
        if (json.getJSONObject("message").has("photo")) {
            String fileId = parser.getFileId(json);
            processPaymentMessageUsecase.processPaymentMessage(fileId);
        }

    }
}
