package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.AuthMeResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ExchangeTokenUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.CookieFactory;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerMeTest {

    @Mock private ExchangeTokenUseCase exchangeUseCase;
    @Mock private JwtService jwtService;
    @Mock private CookieFactory cookieFactory;
    @Mock private RequisitanteRepositoryPort requisitanteRepo;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(exchangeUseCase, jwtService, cookieFactory, requisitanteRepo);
    }

    @Test
    void deveRetornarRequisitanteAutenticado() {
        when(requisitanteRepo.findById(1L))
                .thenReturn(Optional.of(Requisitante.builder().id(1L).nome("Satyan").build()));

        ResponseEntity<AuthMeResponse> response = controller.me(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requisitante().id()).isEqualTo(1L);
        assertThat(response.getBody().requisitante().nome()).isEqualTo("Satyan");
    }

    @Test
    void deveLancarExcecaoSeRequisitanteDoJwtNaoExisteNoBanco() {
        when(requisitanteRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.me(99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("99");
    }
}
