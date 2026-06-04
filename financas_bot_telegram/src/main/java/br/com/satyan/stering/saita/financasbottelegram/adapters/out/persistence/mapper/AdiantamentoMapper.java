package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AdiantamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import org.springframework.stereotype.Component;

/**
 * Mapper entre {@link AdiantamentoEntity} (JPA) e {@link Adiantamento} (domínio).
 *
 * <p>Garante que a camada de domínio permaneça livre de anotações JPA.
 */
@Component
public class AdiantamentoMapper {

    public Adiantamento toDomain(AdiantamentoEntity entity) {
        if (entity == null) return null;
        return Adiantamento.builder()
                .id(entity.getId())
                .funcionarioId(entity.getFuncionarioId())
                .descricao(entity.getDescricao())
                .valorTotal(entity.getValorTotal())
                .valorParcela(entity.getValorParcela())
                .numParcelas(entity.getNumParcelas())
                .parcelasPagas(entity.getParcelasPagas())
                .dataInicio(entity.getDataInicio())
                .ativo(entity.getAtivo())
                .criadoEm(entity.getCriadoEm())
                .build();
    }

    public AdiantamentoEntity toEntity(Adiantamento domain) {
        if (domain == null) return null;
        AdiantamentoEntity entity = new AdiantamentoEntity();
        entity.setId(domain.getId());
        entity.setFuncionarioId(domain.getFuncionarioId());
        entity.setDescricao(domain.getDescricao());
        entity.setValorTotal(domain.getValorTotal());
        entity.setValorParcela(domain.getValorParcela());
        entity.setNumParcelas(domain.getNumParcelas());
        entity.setParcelasPagas(domain.getParcelasPagas() != null ? domain.getParcelasPagas() : 0);
        entity.setDataInicio(domain.getDataInicio());
        entity.setAtivo(domain.getAtivo() != null ? domain.getAtivo() : true);
        // criadoEm gerenciado pelo banco/Hibernate
        return entity;
    }
}
