package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AuthTokenEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.RequisitanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.AuthTokenMapper;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthTokenRepositoryAdapterTest {

    @Mock private AuthTokenJpaRepository jpaRepository;
    @Mock private RequisitanteJpaRepository requisitanteJpaRepository;
    @Mock private AuthTokenMapper mapper;
    @InjectMocks private AuthTokenRepositoryAdapter adapter;

    @Test
    void deveSalvarTokenBuscandoRequisitanteEntityParaFK() {
        AuthToken domain = AuthToken.builder()
                .tokenHash("hash64chars")
                .requisitanteId(1L)
                .criadoEm(LocalDateTime.now())
                .expiraEm(LocalDateTime.now().plusDays(7))
                .build();

        RequisitanteEntity reqEntity = new RequisitanteEntity();
        reqEntity.setId(1L);
        AuthTokenEntity entity = new AuthTokenEntity();
        AuthTokenEntity saved = new AuthTokenEntity();
        saved.setTokenHash("hash64chars");
        AuthToken resultado = AuthToken.builder().tokenHash("hash64chars").build();

        when(requisitanteJpaRepository.getReferenceById(1L)).thenReturn(reqEntity);
        when(mapper.toEntity(domain, reqEntity)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(resultado);

        AuthToken retornado = adapter.save(domain);

        assertThat(retornado.getTokenHash()).isEqualTo("hash64chars");
        verify(requisitanteJpaRepository).getReferenceById(1L);
        verify(mapper).toEntity(domain, reqEntity);
        verify(jpaRepository).save(entity);
    }

    @Test
    void deveRetornarTokenQuandoEncontradoPorHash() {
        AuthTokenEntity entity = new AuthTokenEntity();
        entity.setTokenHash("abc");
        AuthToken domain = AuthToken.builder().tokenHash("abc").build();

        when(jpaRepository.findByTokenHash("abc")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<AuthToken> result = adapter.findByTokenHash("abc");

        assertThat(result).isPresent();
        assertThat(result.get().getTokenHash()).isEqualTo("abc");
    }

    @Test
    void deveRetornarVazioQuandoHashNaoEncontrado() {
        when(jpaRepository.findByTokenHash("inexistente")).thenReturn(Optional.empty());

        Optional<AuthToken> result = adapter.findByTokenHash("inexistente");

        assertThat(result).isEmpty();
    }
}
