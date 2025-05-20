package br.com.satyan.stering.saita.financasbottelegram.domain.repository;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagamentoRepositoryJpa extends JpaRepository<Pagamento, Long> {

}
