package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public class PedidoPagamentoRepositoryAdapter implements PedidoPagamentoRepositoryPort {

    private final PedidoPagamentoJpaRepository jpaRepository;

    public PedidoPagamentoRepositoryAdapter(PedidoPagamentoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PedidoPagamento save(PedidoPagamento pedidoPagamento) {
        return jpaRepository.save(pedidoPagamento);
    }

    @Override
    public java.util.Optional<PedidoPagamento> findById(Long id) {
        return jpaRepository.findById(id);
    }
}

