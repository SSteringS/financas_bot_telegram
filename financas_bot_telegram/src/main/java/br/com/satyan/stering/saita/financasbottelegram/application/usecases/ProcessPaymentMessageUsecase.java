package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.application.mapper.TelegramMediaGroupToPagamentoMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.TelegramPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PagamentoRepository;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.S3PortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.TelegramMediaGroup;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.jpa.PagamentoRepositoryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProcessPaymentMessageUsecase {

  @Value("${financasbot.s3.base-url}")
  private String s3BaseUrl;

  private TelegramPortIn telegramPortIn;
  private S3PortOut s3PortOut;
  private PagamentoRepository pagamentoRepository;

  public ProcessPaymentMessageUsecase(TelegramPortIn telegramPortIn, S3PortOut s3PortOut,
      PagamentoRepositoryAdapter pagamentoRepositoryAdapter) {
    this.telegramPortIn = telegramPortIn;
    this.s3PortOut = s3PortOut;
    this.pagamentoRepository = pagamentoRepository;
  }

  public void processPaymentMessage(TelegramMediaGroup telegramMediaGroup) {

    byte[] pedidoImg = telegramPortIn.getFile(telegramMediaGroup.getFileIdPedido());
    byte[] comprovanteImg = telegramPortIn.getFile(telegramMediaGroup.getFileIdComprovante());

    String dataParticao = java.time.LocalDate.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    String baseUrl = s3BaseUrl + dataParticao + "/";

    String urlPedido =
        baseUrl + "pagamentos_pedido_" + telegramMediaGroup.getFileIdPedido() + ".jpg";
    String urlComprovante =
        baseUrl + "pagamentos_comprovante_" + telegramMediaGroup.getFileIdComprovante() + ".jpg";

    s3PortOut.uploadPhoto(
        dataParticao + "/pagamentos_pedido_" + telegramMediaGroup.getFileIdPedido() + ".jpg",
        pedidoImg);
    s3PortOut.uploadPhoto(
        dataParticao + "/pagamentos_comprovante_" + telegramMediaGroup.getFileIdComprovante()
            + ".jpg", comprovanteImg);

    telegramMediaGroup.setUrlPedido(urlPedido);
    telegramMediaGroup.setUrlComprovante(urlComprovante);

    pagamentoRepository.save(TelegramMediaGroupToPagamentoMapper.mapToEntity(telegramMediaGroup));

  }
}
