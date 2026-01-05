package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.ComprovanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Comprovante;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import org.springframework.stereotype.Service;

@Service
public class RegistrarComprovanteServiceImpl implements RegistrarComprovanteUsecase {

    private final ComprovanteRepositoryPort comprovanteRepository;
    private final PedidoPagamentoRepositoryPort pedidoPagamentoRepository;

    public RegistrarComprovanteServiceImpl(ComprovanteRepositoryPort comprovanteRepository, PedidoPagamentoRepositoryPort pedidoPagamentoRepository) {
        this.comprovanteRepository = comprovanteRepository;
        this.pedidoPagamentoRepository = pedidoPagamentoRepository;
    }

    @Override
    public Comprovante execute(Long pedidoId, String tipoPagamento, String fileIdTelegram) {
        PedidoPagamento pedido = pedidoPagamentoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado")); // Lançar exceção mais específica

        pedido.setStatus(StatusPedido.PAGO);
        pedidoPagamentoRepository.save(pedido);

        Comprovante comprovante = new Comprovante();
        comprovante.setPedido(pedido);
        comprovante.setTipoPagamento(tipoPagamento);
        comprovante.setFileIdTelegram(fileIdTelegram);

        return comprovanteRepository.save(comprovante);
    }
}

