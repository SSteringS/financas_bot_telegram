package br.com.satyan.stering.saita.financasbottelegram.domain.repository;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Pagamento;
import br.com.satyan.stering.saita.financasbottelegram.infrastructure.persistence.PagamentoRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PagamentoRepositoryAdapter implements PagamentoRepository {

  private final PagamentoRepositoryJpa pagamentoRepositoryJpa;

  public PagamentoRepositoryAdapter(PagamentoRepositoryJpa pagamentoRepositoryJpa) {
    this.pagamentoRepositoryJpa = pagamentoRepositoryJpa;
  }

  @Override
  public Pagamento save(Pagamento pagamento) {
    return pagamentoRepositoryJpa.save(pagamento);
  }

  @Override
  public Optional<Pagamento> findById(Long id) {
    return pagamentoRepositoryJpa.findById(id);
  }

}
