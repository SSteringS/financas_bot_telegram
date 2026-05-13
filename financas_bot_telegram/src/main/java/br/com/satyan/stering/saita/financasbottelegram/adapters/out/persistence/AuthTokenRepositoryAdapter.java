package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AuthTokenEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.RequisitanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.AuthTokenMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AuthTokenRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenRepositoryAdapter implements AuthTokenRepositoryPort {

    private final AuthTokenJpaRepository jpaRepository;
    private final RequisitanteJpaRepository requisitanteJpaRepository;
    private final AuthTokenMapper mapper;

    public AuthTokenRepositoryAdapter(
            AuthTokenJpaRepository jpaRepository,
            RequisitanteJpaRepository requisitanteJpaRepository,
            AuthTokenMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.requisitanteJpaRepository = requisitanteJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AuthToken save(AuthToken token) {
        RequisitanteEntity requisitanteEntity = requisitanteJpaRepository
                .getReferenceById(token.getRequisitanteId());
        AuthTokenEntity entity = mapper.toEntity(token, requisitanteEntity);
        AuthTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AuthToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }
}
