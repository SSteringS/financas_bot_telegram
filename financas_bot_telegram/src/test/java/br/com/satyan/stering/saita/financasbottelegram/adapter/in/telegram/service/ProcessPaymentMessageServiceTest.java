package br.com.satyan.stering.saita.financasbottelegram.adapter.in.telegram.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.MediaGroupInMemoryService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.ProcessPaymentMessageService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.util.TelegramMessageParser;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ProcessPaymentMessageUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.TelegramMediaGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProcessPaymentMessageServiceTest {

  private TelegramMessageParser parser;
  private ProcessPaymentMessageUsecase usecase;
  private MediaGroupInMemoryService memoryService;
  private ProcessPaymentMessageService service;

  @BeforeEach
  void setUp() {
    usecase = mock(ProcessPaymentMessageUsecase.class);
    memoryService = mock(MediaGroupInMemoryService.class);
    parser = mock(TelegramMessageParser.class);
    service = new ProcessPaymentMessageService(parser, usecase, memoryService);
  }

  @Test
  @DisplayName("Deve processar mensagem com photo e chamar handlePhotoMessage")
  void deveProcessarMensagemComPhoto() throws JSONException {
    String payload = "{\"photo\":[{\"file_id\":\"file1\"}],\"media_group_id\":\"group1\",\"message\":\"cat origem\"}";
    JSONArray photoArray = new JSONArray().put(new JSONObject().put("file_id", "file1"));

    when(parser.getPhotoArray(any(JSONObject.class))).thenReturn(photoArray);
    when(parser.getFileId(any(JSONObject.class))).thenReturn("file1");
    when(parser.getMediaGroupId(any(JSONObject.class))).thenReturn("group1");
    when(parser.getMessage(any(JSONObject.class))).thenReturn("cat origem");

    when(memoryService.addAndGetGroup("group1", "file1", null, null))
        .thenReturn(new TelegramMediaGroup());

    service.processPaymentMessage(payload);

    verify(parser).getMessage(any(JSONObject.class));
    verify(parser).getFileId(any(JSONObject.class));

  }

  @Test
  @DisplayName("Deve processar grupo e chamar usecase quando grupo estiver completo")
  void deveProcessarGrupoCompleto() throws JSONException {
    String payload = "{\"photo\":[{\"file_id\":\"file1\"}],\"media_group_id\":\"group1\",  \"caption\": \"Categoria: fazenda\\nOrigem: pedido whats pai\"}";

    when(parser.getPhotoArray(any(JSONObject.class))).thenReturn(
        new JSONArray().put(new JSONObject().put("file_id", "file1")));
    when(parser.getFileId(any(JSONObject.class))).thenReturn("filePedido");
    when(parser.getMediaGroupId(any(JSONObject.class))).thenReturn("group1");
    when(parser.getMessage(any(JSONObject.class))).thenReturn("cat origem");

    TelegramMediaGroup group = new TelegramMediaGroup();
    group.setFileIdPedido("filePedido");
    group.setFileIdComprovante("fileComprovante");
    when(memoryService.addAndGetGroup("group1", "filePedido", "cat", "origem")).thenReturn(group);

    service.processPaymentMessage(payload);

    verify(usecase).processPaymentMessage(group);
    verify(memoryService).removeGroup("group1");
  }

  @Test
  @DisplayName("Não deve processar grupo se mediaGroupId for null")
  void naoDeveProcessarGrupoSeMediaGroupIdNull() throws JSONException {
    String payload = "{\"photo\":[{\"file_id\":\"file1\"}]}";
    JSONObject json = new JSONObject(payload);
    JSONArray photoArray = new JSONArray().put(new JSONObject().put("file_id", "file1"));

    when(parser.getPhotoArray(any(JSONObject.class))).thenReturn(photoArray);
    when(parser.getFileId(any(JSONObject.class))).thenReturn("filePedido");
    when(parser.getMediaGroupId(any(JSONObject.class))).thenReturn(null);

    service.processPaymentMessage(payload);

    verify(usecase, never()).processPaymentMessage(any());
    verify(memoryService, never()).removeGroup(any());
  }

  @Test
  @DisplayName("Não deve processar se não houver photo")
  void naoDeveProcessarSeNaoHouverPhoto() throws JSONException {
    String payload = "{\"text\":\"mensagem sem foto\"}";
    JSONObject json = new JSONObject(payload);

    when(parser.getPhotoArray(any(JSONObject.class))).thenReturn(null);

    service.processPaymentMessage(payload);

    verify(parser).getPhotoArray(any(JSONObject.class));
    verifyNoInteractions(memoryService);
    verifyNoInteractions(usecase);
  }
}