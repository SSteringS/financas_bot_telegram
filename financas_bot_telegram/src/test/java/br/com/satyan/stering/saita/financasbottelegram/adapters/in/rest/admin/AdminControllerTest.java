package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.GerarConviteResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.GerarTokenConviteUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.RequisitanteNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private GerarTokenConviteUseCase useCase;

    private AdminController controller;
    private static final String VALID_KEY = "chave-admin-valida";

    @BeforeEach
    void setUp() {
        controller = new AdminController(useCase, VALID_KEY);
    }

    @Test
    void deveRetornar200ComUrlQuandoChaveCorreta() {
        when(useCase.gerar(1L)).thenReturn("http://localhost:5173/entrar?t=abc123");

        ResponseEntity<GerarConviteResponse> response = controller.gerarConvite(1L, VALID_KEY);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().url()).isEqualTo("http://localhost:5173/entrar?t=abc123");
    }

    @Test
    void deveRetornar401SemHeader() {
        ResponseEntity<GerarConviteResponse> response = controller.gerarConvite(1L, null);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verify(useCase, never()).gerar(1L);
    }

    @Test
    void deveRetornar401ComChaveErrada() {
        ResponseEntity<GerarConviteResponse> response = controller.gerarConvite(1L, "chave-errada");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verify(useCase, never()).gerar(1L);
    }

    @Test
    void deveRetornar404QuandoRequisitanteNaoExiste() {
        when(useCase.gerar(99L)).thenThrow(new RequisitanteNaoEncontradoException(99L));

        ResponseEntity<GerarConviteResponse> response = controller.gerarConvite(99L, VALID_KEY);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
