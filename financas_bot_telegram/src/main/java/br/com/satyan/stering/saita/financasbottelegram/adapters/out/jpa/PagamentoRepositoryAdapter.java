package br.com.satyan.stering.saita.financasbottelegram.adapters.out.jpa;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.jpa.exceptions.DataBaseException;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Pagamento;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PagamentoRepository;
import br.com.satyan.stering.saita.financasbottelegram.infrastructure.persistence.PagamentoRepositoryJpa;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class PagamentoRepositoryAdapter implements PagamentoRepository {

  private final Logger logger = LoggerFactory.getLogger(PagamentoRepositoryAdapter.class);
  private final PagamentoRepositoryJpa pagamentoRepositoryJpa;

  public PagamentoRepositoryAdapter(PagamentoRepositoryJpa pagamentoRepositoryJpa) {
    this.pagamentoRepositoryJpa = pagamentoRepositoryJpa;
  }

  @Override
public Pagamento save(Pagamento pagamento) {
    try {
      return pagamentoRepositoryJpa.save(pagamento);
    } catch (DataAccessException ex) {
      logger.error("Erro ao salvar pagamento no banco de dados: {}", ex.getMessage());
      throw new DataBaseException("Erro ao salvar pagamento no banco de dados.", ex);
    }
  }

  @Override
  public Optional<Pagamento> findById(Long id) {
    return pagamentoRepositoryJpa.findById(id);
  }

}
