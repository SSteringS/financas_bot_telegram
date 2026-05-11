package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.PedidoPagamentoMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PedidoPagamentoRepositoryAdapter implements PedidoPagamentoRepositoryPort {

    private final PedidoPagamentoJpaRepository jpaRepository;
    private final PedidoPagamentoMapper mapper;

    public PedidoPagamentoRepositoryAdapter(
            PedidoPagamentoJpaRepository jpaRepository,
            PedidoPagamentoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PedidoPagamento save(PedidoPagamento pedido) {
        PedidoPagamentoEntity entity = mapper.toEntity(pedido);
        PedidoPagamentoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PedidoPagamento> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
