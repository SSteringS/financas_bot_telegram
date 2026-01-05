package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;

public interface SalvarPedidoPagamentoUsecase {
    PedidoPagamento execute(PedidoPagamento pedidoPagamento);
}

