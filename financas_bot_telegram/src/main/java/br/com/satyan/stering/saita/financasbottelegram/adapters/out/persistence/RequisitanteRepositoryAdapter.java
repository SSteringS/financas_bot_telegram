package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.RequisitanteMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RequisitanteRepositoryAdapter implements RequisitanteRepositoryPort {

    private final RequisitanteJpaRepository jpaRepository;
    private final RequisitanteMapper mapper;

    public RequisitanteRepositoryAdapter(RequisitanteJpaRepository jpaRepository, RequisitanteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Requisitante> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
