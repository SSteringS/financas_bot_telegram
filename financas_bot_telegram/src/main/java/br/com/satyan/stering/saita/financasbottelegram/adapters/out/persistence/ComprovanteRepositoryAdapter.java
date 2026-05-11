package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.ComprovanteMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.ComprovanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import org.springframework.stereotype.Component;

@Component
public class ComprovanteRepositoryAdapter implements ComprovanteRepositoryPort {

    private final ComprovanteJpaRepository jpaRepository;
    private final PedidoPagamentoJpaRepository pedidoJpaRepository;
    private final ComprovanteMapper mapper;

    public ComprovanteRepositoryAdapter(
            ComprovanteJpaRepository jpaRepository,
            PedidoPagamentoJpaRepository pedidoJpaRepository,
            ComprovanteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.pedidoJpaRepository = pedidoJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Comprovante save(Comprovante comprovante) {
        PedidoPagamentoEntity pedidoEntity = pedidoJpaRepository.findById(comprovante.getPedidoId())
                .orElseThrow(() -> new PedidoNaoEncontradoException(
                        "Pedido " + comprovante.getPedidoId() + " não encontrado ao salvar comprovante", null));
        ComprovanteEntity entity = mapper.toEntity(comprovante, pedidoEntity);
        ComprovanteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
