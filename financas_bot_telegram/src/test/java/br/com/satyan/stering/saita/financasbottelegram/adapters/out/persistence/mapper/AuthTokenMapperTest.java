package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AuthTokenEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.RequisitanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AuthTokenMapperTest {

    private final AuthTokenMapper mapper = new AuthTokenMapper();

    @Test
    void deveMappearEntityParaDomain() {
        RequisitanteEntity req = new RequisitanteEntity();
        req.setId(1L);

        AuthTokenEntity entity = new AuthTokenEntity();
        entity.setTokenHash("abc123");
        entity.setRequisitante(req);
        entity.setCriadoEm(LocalDateTime.of(2026, 5, 1, 10, 0));
        entity.setExpiraEm(LocalDateTime.of(2026, 5, 8, 10, 0));
        entity.setUsadoEm(null);

        AuthToken domain = mapper.toDomain(entity);

        assertThat(domain.getTokenHash()).isEqualTo("abc123");
        assertThat(domain.getRequisitanteId()).isEqualTo(1L);
        assertThat(domain.getCriadoEm()).isEqualTo(LocalDateTime.of(2026, 5, 1, 10, 0));
        assertThat(domain.getExpiraEm()).isEqualTo(LocalDateTime.of(2026, 5, 8, 10, 0));
        assertThat(domain.getUsadoEm()).isNull();
    }

    @Test
    void deveMappearDomainParaEntityComRequisitante() {
        AuthToken domain = AuthToken.builder()
                .tokenHash("hash64")
                .requisitanteId(5L)
                .criadoEm(LocalDateTime.of(2026, 5, 1, 10, 0))
                .expiraEm(LocalDateTime.of(2026, 5, 8, 10, 0))
                .build();

        RequisitanteEntity req = new RequisitanteEntity();
        req.setId(5L);

        AuthTokenEntity entity = mapper.toEntity(domain, req);

        assertThat(entity.getTokenHash()).isEqualTo("hash64");
        assertThat(entity.getRequisitante()).isSameAs(req);
        assertThat(entity.getUsadoEm()).isNull();
    }

    @Test
    void deveTratarRequisitanteNuloSemNpe() {
        AuthTokenEntity entity = new AuthTokenEntity();
        entity.setTokenHash("xyz");
        entity.setRequisitante(null);

        AuthToken domain = mapper.toDomain(entity);

        assertThat(domain.getRequisitanteId()).isNull();
    }

    @Test
    void deveRetornarNullParaEntityNula() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void deveRetornarNullParaDomainNulo() {
        assertThat(mapper.toEntity(null, new RequisitanteEntity())).isNull();
    }
}
