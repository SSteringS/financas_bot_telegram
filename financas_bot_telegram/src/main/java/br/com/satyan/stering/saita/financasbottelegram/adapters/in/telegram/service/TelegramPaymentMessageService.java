package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.util.TelegramMessageParser;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ProcessPaymentMessageUsecase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;


@Service
public class TelegramPaymentMessageService {

    private TelegramMessageParser parser;
    private ProcessPaymentMessageUsecase processPaymentMessageUsecase;
    private MediaGroupInMemoryService mediaGroupInMemoryService;

    private final String TELEGRAM_TOKEN =System.getenv("TELEGRAM_TOKEN");

    public TelegramPaymentMessageService(TelegramMessageParser parser,
                                         ProcessPaymentMessageUsecase processPaymentMessageUsecase,
                                         MediaGroupInMemoryService mediaGroupInMemoryService) {
        this.parser = parser;
        this.processPaymentMessageUsecase = processPaymentMessageUsecase;
        this.mediaGroupInMemoryService = mediaGroupInMemoryService;
    }
    public void processPaymentMessage(String message) {
        JSONObject json = new JSONObject(message);
        JSONArray photoArray = parser.getPhotoArray(json);
        if (photoArray != null) {
            handlePhotoMessage(json);
        }
    }


    private void handlePhotoMessage(JSONObject json) {
        String fileId = parser.getFileId(json);
        String mediaGroupId = parser.getMediaGroupId(json);
        processMediaGroup(mediaGroupId, fileId);
    }


    private void processMediaGroup(String mediaGroupId, String fileId) {
        if (mediaGroupId == null) return;
        List<String> group = mediaGroupInMemoryService.addAndGetGroup(mediaGroupId, fileId);
        if (group.size() == 2) {
            processPaymentMessageUsecase.processPaymentMessage(group);
            mediaGroupInMemoryService.removeGroup(mediaGroupId);
     }
    }

}
