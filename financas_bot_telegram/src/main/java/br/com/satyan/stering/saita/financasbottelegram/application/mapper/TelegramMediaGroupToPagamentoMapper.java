package br.com.satyan.stering.saita.financasbottelegram.application.mapper;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.TelegramMediaGroup;
import java.time.LocalDateTime;

public class TelegramMediaGroupToPagamentoMapper {

  public static Pagamento mapToEntity(TelegramMediaGroup mediaGroup) {
    Pagamento pagamento = new Pagamento();
    pagamento.setUrlPedido(mediaGroup.getUrlPedido());
    pagamento.setUrlComprovante(mediaGroup.getUrlComprovante());
    pagamento.setCategoria(mediaGroup.getCategoria());
    pagamento.setOrigem(mediaGroup.getOrigem());
    pagamento.setDataHora(LocalDateTime.now());
    return pagamento;
  }

}
