package br.com.satyan.stering.saita.financasbottelegram.application.mapper;

import br.com.satyan.stering.saita.financasbottelegram.application.mapper.TelegramMediaGroupToPagamentoMapper;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Pagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.TelegramMediaGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TelegramMediaGroupToPagamentoMapperTest {

  @Test
  @DisplayName("Deve mapear todos os campos do TelegramMediaGroup para Pagamento corretamente")
  void deveMapearTodosOsCamposCorretamente() {
    TelegramMediaGroup mediaGroup = new TelegramMediaGroup();
    mediaGroup.setUrlPedido("urlPedido");
    mediaGroup.setUrlComprovante("urlComprovante");
    mediaGroup.setCategoria("categoria");
    mediaGroup.setOrigem("origem");

    Pagamento pagamento = TelegramMediaGroupToPagamentoMapper.mapToEntity(mediaGroup);

    assertEquals("urlPedido", pagamento.getUrlPedido());
    assertEquals("urlComprovante", pagamento.getUrlComprovante());
    assertEquals("categoria", pagamento.getCategoria());
    assertEquals("origem", pagamento.getOrigem());
    assertNotNull(pagamento.getDataHora());
  }
}