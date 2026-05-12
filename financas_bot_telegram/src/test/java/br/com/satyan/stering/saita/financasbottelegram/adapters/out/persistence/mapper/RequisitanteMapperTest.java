package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.RequisitanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RequisitanteMapperTest {

    private final RequisitanteMapper mapper = new RequisitanteMapper();

    @Test
    void deveMappearEntityParaDomain() {
        RequisitanteEntity entity = new RequisitanteEntity();
        entity.setId(1L);
        entity.setNome("Satyan Saita");
        entity.setTelefone("11999999999");
        entity.setEmail("satyan@email.com");
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.of(2026, 1, 1, 0, 0));

        Requisitante domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getNome()).isEqualTo("Satyan Saita");
        assertThat(domain.getTelefone()).isEqualTo("11999999999");
        assertThat(domain.getEmail()).isEqualTo("satyan@email.com");
        assertThat(domain.isAtivo()).isTrue();
        assertThat(domain.getCriadoEm()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void deveMappearDomainParaEntity() {
        Requisitante domain = Requisitante.builder()
                .id(2L)
                .nome("Outro")
                .telefone("21000000000")
                .email("outro@email.com")
                .ativo(false)
                .build();

        RequisitanteEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getNome()).isEqualTo("Outro");
        assertThat(entity.isAtivo()).isFalse();
    }

    @Test
    void deveRetornarNullParaEntityNula() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void deveRetornarNullParaDomainNulo() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
