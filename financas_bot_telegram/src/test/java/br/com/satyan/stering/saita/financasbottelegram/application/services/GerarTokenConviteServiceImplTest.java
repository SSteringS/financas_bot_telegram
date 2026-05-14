package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AuthTokenRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.HashService;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.RequisitanteNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GerarTokenConviteServiceImplTest {

    @Mock private RequisitanteRepositoryPort requisitanteRepo;
    @Mock private AuthTokenRepositoryPort tokenRepo;
    @Mock private HashService hashService;

    private GerarTokenConviteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GerarTokenConviteServiceImpl(
                requisitanteRepo, tokenRepo, hashService, "http://localhost:5173");
    }

    @Test
    void deveLancarExcecaoQuandoRequisitanteNaoExiste() {
        when(requisitanteRepo.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.gerar(999L))
                .isInstanceOf(RequisitanteNaoEncontradoException.class)
                .hasMessageContaining("999");
    }

    @Test
    void deveGerarUrlComBaseUrlEToken() {
        when(requisitanteRepo.existsById(1L)).thenReturn(true);
        when(hashService.hash(any())).thenReturn("fakehash64");
        when(tokenRepo.save(any())).thenReturn(AuthToken.builder().tokenHash("fakehash64").build());

        String url = service.gerar(1L);

        assertThat(url).startsWith("http://localhost:5173/entrar?t=");
    }

    @Test
    void deveSalvarTokenComHashEExpiracao7Dias() {
        when(requisitanteRepo.existsById(1L)).thenReturn(true);
        when(hashService.hash(any())).thenReturn("hashresult");
        when(tokenRepo.save(any())).thenReturn(AuthToken.builder().tokenHash("hashresult").build());

        service.gerar(1L);

        verify(tokenRepo).save(argThat(token ->
                token.getTokenHash().equals("hashresult") &&
                token.getRequisitanteId().equals(1L) &&
                token.getUsadoEm() == null &&
                token.getExpiraEm().isAfter(token.getCriadoEm().plusDays(6))
        ));
    }

    @Test
    void deveUsarHashDoTokenPlain() {
        when(requisitanteRepo.existsById(1L)).thenReturn(true);
        when(hashService.hash(any())).thenReturn("hashdetoken");
        when(tokenRepo.save(any())).thenReturn(AuthToken.builder().tokenHash("hashdetoken").build());

        service.gerar(1L);

        verify(hashService).hash(any());
    }

    @Test
    void deveGerarTokensDiferentesACadaChamada() {
        when(requisitanteRepo.existsById(1L)).thenReturn(true);
        when(hashService.hash(any())).thenReturn("hash1", "hash2");
        when(tokenRepo.save(any())).thenReturn(AuthToken.builder().build());

        String url1 = service.gerar(1L);
        String url2 = service.gerar(1L);

        // Os tokens plain são extraídos da URL após "?t="
        String token1 = url1.substring(url1.indexOf("?t=") + 3);
        String token2 = url2.substring(url2.indexOf("?t=") + 3);
        assertThat(token1).isNotEqualTo(token2);
    }
}
