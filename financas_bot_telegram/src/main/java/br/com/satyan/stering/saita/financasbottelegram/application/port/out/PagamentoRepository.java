package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Pagamento;
import java.util.Optional;

public interface PagamentoRepository {
  Pagamento save(Pagamento pagamento);
  Optional<Pagamento> findById(Long id);
}
