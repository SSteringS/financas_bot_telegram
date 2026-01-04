package br.com.satyan.stering.saita.financasbottelegram.application.usecases;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.TelegramPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PagamentoRepository;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.S3PortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Pagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.TelegramMediaGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProcessPaymentMessageUsecaseTest {

  private TelegramPortIn telegramPortIn;
  private S3PortOut s3PortOut;
  private PagamentoRepository pagamentoRepository;
  private ProcessPaymentMessageUsecase usecase;

  @BeforeEach
  void setUp() {
    telegramPortIn = mock(TelegramPortIn.class);
    s3PortOut = mock(S3PortOut.class);
    pagamentoRepository = mock(PagamentoRepository.class);
    usecase = new ProcessPaymentMessageUsecase(telegramPortIn, s3PortOut, pagamentoRepository);
    ReflectionTestUtils.setField(usecase, "s3BaseUrl", "https://s3.amazonaws.com/bucket/");
  }

  @Test
  @DisplayName("Deve processar mensagem de pagamento e salvar corretamente")
  void deveProcessarMensagemDePagamento() {
    TelegramMediaGroup mediaGroup = new TelegramMediaGroup();
    mediaGroup.setFileIdPedido("pedido123");
    mediaGroup.setFileIdComprovante("comp456");

    when(telegramPortIn.getFile("pedido123")).thenReturn(new byte[]{1, 2, 3});
    when(telegramPortIn.getFile("comp456")).thenReturn(new byte[]{4, 5, 6});
    when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(new Pagamento());

    usecase.processPaymentMessage(mediaGroup);

    verify(telegramPortIn).getFile("pedido123");
    verify(telegramPortIn).getFile("comp456");
    verify(s3PortOut, times(2)).uploadPhoto(anyString(), any());
    verify(pagamentoRepository).save(any(Pagamento.class));
    assertNotNull(mediaGroup.getUrlPedido());
    assertNotNull(mediaGroup.getUrlComprovante());
  }
}