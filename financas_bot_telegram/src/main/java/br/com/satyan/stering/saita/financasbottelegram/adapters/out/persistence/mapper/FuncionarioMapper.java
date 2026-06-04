package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.FuncionarioEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import org.springframework.stereotype.Component;

/**
 * Mapper entre {@link FuncionarioEntity} (JPA) e {@link Funcionario} (domínio).
 *
 * <p>Mantém isolamento da camada de persistência: o domínio não conhece {@link FuncionarioEntity}.
 * O campo {@code atualizadoEm} é incluído no mapeamento para rastreabilidade de salário.
 */
@Component
public class FuncionarioMapper {

    public Funcionario toDomain(FuncionarioEntity entity) {
        if (entity == null) return null;
        return Funcionario.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .salarioBase(entity.getSalarioBase())
                .formaPagamento(entity.getFormaPagamento())
                .chavePix(entity.getChavePix())
                .banco(entity.getBanco())
                .agencia(entity.getAgencia())
                .conta(entity.getConta())
                .tipoConta(entity.getTipoConta())
                .contaPropria(entity.getContaPropria())
                .obsPagamento(entity.getObsPagamento())
                .diaPagamentoReferencia(entity.getDiaPagamentoReferencia())
                .ativo(entity.getAtivo())
                .criadoEm(entity.getCriadoEm())
                .atualizadoEm(entity.getAtualizadoEm())
                .build();
    }

    public FuncionarioEntity toEntity(Funcionario domain) {
        if (domain == null) return null;
        FuncionarioEntity entity = new FuncionarioEntity();
        entity.setId(domain.getId());
        entity.setNome(domain.getNome());
        entity.setSalarioBase(domain.getSalarioBase());
        entity.setFormaPagamento(domain.getFormaPagamento());
        entity.setChavePix(domain.getChavePix());
        entity.setBanco(domain.getBanco());
        entity.setAgencia(domain.getAgencia());
        entity.setConta(domain.getConta());
        entity.setTipoConta(domain.getTipoConta());
        entity.setContaPropria(domain.getContaPropria() != null ? domain.getContaPropria() : true);
        entity.setObsPagamento(domain.getObsPagamento());
        entity.setDiaPagamentoReferencia(domain.getDiaPagamentoReferencia());
        entity.setAtivo(domain.getAtivo() != null ? domain.getAtivo() : true);
        // criadoEm e atualizadoEm são gerenciados pelo banco/Hibernate
        return entity;
    }
}
