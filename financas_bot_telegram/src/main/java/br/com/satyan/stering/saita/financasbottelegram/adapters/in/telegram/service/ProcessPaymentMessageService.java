package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.TelegramMediaGroup;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.util.TelegramMessageParser;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ProcessPaymentMessageUsecase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;


@Service
public class ProcessPaymentMessageService {

  private TelegramMessageParser parser;
  private ProcessPaymentMessageUsecase processPaymentMessageUsecase;
  private MediaGroupInMemoryService mediaGroupInMemoryService;

  private final String TELEGRAM_TOKEN = System.getenv("TELEGRAM_TOKEN");

  public ProcessPaymentMessageService(TelegramMessageParser parser,
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
    String message = parser.getMessage(json);
    var categoria = message != null ? TelegramMessageParser.extractCategoria(message) : null;
    var origem = message != null ? TelegramMessageParser.extractOrigem(message) : null;

    processMediaGroup(mediaGroupId, fileId, categoria, origem);
  }


  private void processMediaGroup(String mediaGroupId, String fileId, String categoria,
      String origem) {
    if (mediaGroupId == null) {
      return;
    }
    TelegramMediaGroup group = mediaGroupInMemoryService.addAndGetGroup(mediaGroupId, fileId,
        categoria, origem);
    if (group.getFileIdComprovante() != null && group.getFileIdPedido() != null) {
      processPaymentMessageUsecase.processPaymentMessage(group);
      mediaGroupInMemoryService.removeGroup(mediaGroupId);
    }
  }

}
