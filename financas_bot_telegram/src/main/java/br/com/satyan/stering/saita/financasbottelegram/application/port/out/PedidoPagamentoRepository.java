package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoPagamentoRepository extends JpaRepository<PedidoPagamento, Long> {
}

