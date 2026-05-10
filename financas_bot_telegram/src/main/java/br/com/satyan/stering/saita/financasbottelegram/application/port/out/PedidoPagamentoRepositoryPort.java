package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;

import java.util.Optional;

public interface PedidoPagamentoRepositoryPort {
    PedidoPagamento save(PedidoPagamento pedidoPagamento);
    Optional<PedidoPagamento> findById(Long id);
}

