package br.com.satyan.stering.saita.financasbottelegram.adapter.in.telegram.controllers;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.controller.TelegramWebhookController;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.ProcessPaymentMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TelegramWebhookControllerTest {

  private ProcessPaymentMessageService processPaymentMessageService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    processPaymentMessageService = mock(ProcessPaymentMessageService.class);
    TelegramWebhookController controller = new TelegramWebhookController(
        processPaymentMessageService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  @DisplayName("Deve receber mensagem do Telegram e processar corretamente")
  void deveReceberMensagemEProcessar() throws Exception {
    String payload = "{ \"message\": { \"chat\": { \"id\": 12345 } } }";

    mockMvc.perform(post("/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));

    verify(processPaymentMessageService, times(1)).processPaymentMessage(payload);
  }
}