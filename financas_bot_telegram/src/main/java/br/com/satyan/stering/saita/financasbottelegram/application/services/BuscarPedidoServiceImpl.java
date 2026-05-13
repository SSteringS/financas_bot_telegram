package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.ComprovanteJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoDetalheDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.BuscarPedidoUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoAutorizadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import org.springframework.stereotype.Service;

@Service
public class BuscarPedidoServiceImpl implements BuscarPedidoUseCase {

    private final PedidoPagamentoJpaRepository jpaRepository;
    private final ComprovanteJpaRepository comprovanteRepo;

    public BuscarPedidoServiceImpl(PedidoPagamentoJpaRepository jpaRepository,
                                    ComprovanteJpaRepository comprovanteRepo) {
        this.jpaRepository = jpaRepository;
        this.comprovanteRepo = comprovanteRepo;
    }

    @Override
    public PedidoDetalheDTO buscar(Long pedidoId, Long requisitanteId) {
        PedidoPagamentoEntity entity = jpaRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        if (!entity.getRequisitanteId().equals(requisitanteId)) {
            throw new PedidoNaoAutorizadoException(pedidoId, requisitanteId);
        }

        boolean temComprovante = comprovanteRepo.existsByPedidoId(pedidoId);

        return new PedidoDetalheDTO(
                entity.getId(), entity.getValor(), entity.getDescricao(),
                entity.getTipo(), entity.getStatus(),
                entity.getDataPedido(), entity.getDataPagamento(),
                temComprovante);
    }
}
