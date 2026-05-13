package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.RequisitanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import org.springframework.stereotype.Component;

@Component
public class RequisitanteMapper {

    public Requisitante toDomain(RequisitanteEntity entity) {
        if (entity == null) return null;
        return Requisitante.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .telefone(entity.getTelefone())
                .email(entity.getEmail())
                .ativo(entity.isAtivo())
                .criadoEm(entity.getCriadoEm())
                .build();
    }

    public RequisitanteEntity toEntity(Requisitante domain) {
        if (domain == null) return null;
        RequisitanteEntity entity = new RequisitanteEntity();
        entity.setId(domain.getId());
        entity.setNome(domain.getNome());
        entity.setTelefone(domain.getTelefone());
        entity.setEmail(domain.getEmail());
        entity.setAtivo(domain.isAtivo());
        entity.setCriadoEm(domain.getCriadoEm());
        return entity;
    }
}
