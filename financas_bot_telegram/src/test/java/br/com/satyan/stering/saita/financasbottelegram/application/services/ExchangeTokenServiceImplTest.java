package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AuthTokenRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.HashService;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.AuthTokenInvalidoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeTokenServiceImplTest {

    @Mock private AuthTokenRepositoryPort tokenRepo;
    @Mock private RequisitanteRepositoryPort requisitanteRepo;
    @Mock private HashService hashService;

    private ExchangeTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExchangeTokenServiceImpl(tokenRepo, requisitanteRepo, hashService);
    }

    private AuthToken tokenValido() {
        return AuthToken.builder()
                .tokenHash("hash")
                .requisitanteId(1L)
                .criadoEm(LocalDateTime.now().minusHours(1))
                .expiraEm(LocalDateTime.now().plusDays(6))
                .usadoEm(null)
                .build();
    }

    @Test
    void deveLancarExcecaoQuandoTokenNaoEncontrado() {
        when(hashService.hash("invalido")).thenReturn("hash-invalido");
        when(tokenRepo.findByTokenHash("hash-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exchange("invalido"))
                .isInstanceOf(AuthTokenInvalidoException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void deveLancarExcecaoQuandoTokenExpirado() {
        AuthToken expirado = AuthToken.builder()
                .tokenHash("hash")
                .requisitanteId(1L)
                .criadoEm(LocalDateTime.now().minusDays(10))
                .expiraEm(LocalDateTime.now().minusDays(3))
                .build();

        when(hashService.hash("token")).thenReturn("hash");
        when(tokenRepo.findByTokenHash("hash")).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.exchange("token"))
                .isInstanceOf(AuthTokenInvalidoException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void deveLancarExcecaoQuandoTokenJaFoiUsado() {
        AuthToken usado = AuthToken.builder()
                .tokenHash("hash")
                .requisitanteId(1L)
                .criadoEm(LocalDateTime.now().minusHours(1))
                .expiraEm(LocalDateTime.now().plusDays(6))
                .usadoEm(LocalDateTime.now().minusMinutes(5))
                .build();

        when(hashService.hash("token")).thenReturn("hash");
        when(tokenRepo.findByTokenHash("hash")).thenReturn(Optional.of(usado));

        assertThatThrownBy(() -> service.exchange("token"))
                .isInstanceOf(AuthTokenInvalidoException.class)
                .hasMessageContaining("usado");
    }

    @Test
    void deveRetornarRequisitanteEMarcarTokenComoUsado() {
        AuthToken token = tokenValido();
        Requisitante requisitante = Requisitante.builder().id(1L).nome("Satyan").build();

        when(hashService.hash("token-plain")).thenReturn("hash");
        when(tokenRepo.findByTokenHash("hash")).thenReturn(Optional.of(token));
        when(tokenRepo.save(any())).thenReturn(token);
        when(requisitanteRepo.findById(1L)).thenReturn(Optional.of(requisitante));

        Requisitante resultado = service.exchange("token-plain");

        assertThat(resultado.getNome()).isEqualTo("Satyan");
        verify(tokenRepo).save(argThat(t -> t.getUsadoEm() != null));
    }
}
