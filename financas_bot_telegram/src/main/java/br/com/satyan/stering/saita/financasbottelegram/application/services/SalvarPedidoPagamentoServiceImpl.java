package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import org.springframework.stereotype.Service;

@Service
public class SalvarPedidoPagamentoServiceImpl implements SalvarPedidoPagamentoUsecase {

    private final PedidoPagamentoRepositoryPort pedidoPagamentoRepository;

    public SalvarPedidoPagamentoServiceImpl(PedidoPagamentoRepositoryPort pedidoPagamentoRepository) {
        this.pedidoPagamentoRepository = pedidoPagamentoRepository;
    }

    @Override
    public PedidoPagamento execute(PedidoPagamento pedidoPagamento) {
        return pedidoPagamentoRepository.save(pedidoPagamento);
    }
}

