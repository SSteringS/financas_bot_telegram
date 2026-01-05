package br.com.satyan.stering.saita.financasbottelegram.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagamentoRepositoryJpa extends JpaRepository<Pagamento, Long> {

}
