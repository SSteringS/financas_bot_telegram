package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AuthTokenEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.RequisitanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenMapper {

    public AuthToken toDomain(AuthTokenEntity entity) {
        if (entity == null) return null;
        return AuthToken.builder()
                .tokenHash(entity.getTokenHash())
                .requisitanteId(entity.getRequisitante() != null ? entity.getRequisitante().getId() : null)
                .criadoEm(entity.getCriadoEm())
                .expiraEm(entity.getExpiraEm())
                .usadoEm(entity.getUsadoEm())
                .build();
    }

    public AuthTokenEntity toEntity(AuthToken domain, RequisitanteEntity requisitanteEntity) {
        if (domain == null) return null;
        AuthTokenEntity entity = new AuthTokenEntity();
        entity.setTokenHash(domain.getTokenHash());
        entity.setRequisitante(requisitanteEntity);
        entity.setCriadoEm(domain.getCriadoEm());
        entity.setExpiraEm(domain.getExpiraEm());
        entity.setUsadoEm(domain.getUsadoEm());
        return entity;
    }
}
