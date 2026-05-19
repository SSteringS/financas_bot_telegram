package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.resumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoStatusDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ResumoMesUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.MesFormatoInvalidoException;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumoControllerTest {

    @Mock private ResumoMesUseCase useCase;

    private ResumoController controller;

    private static final ResumoStatusDTO ZEROS = new ResumoStatusDTO(0, BigDecimal.ZERO);

    @BeforeEach
    void setUp() {
        controller = new ResumoController(useCase);
    }

    @Test
    void deveRetornarDTODoUseCase() {
        ResumoMesDTO esperado = new ResumoMesDTO("2026-05", ZEROS, ZEROS, ZEROS);
        when(useCase.obter(eq(42L), isNull(), isNull())).thenReturn(esperado);

        ResumoMesDTO resultado = controller.obter(42L, null, null);

        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    void devePassarYearMonthParsadoCorretamente() {
        when(useCase.obter(eq(1L), eq(YearMonth.of(2026, 3)), isNull()))
                .thenReturn(new ResumoMesDTO("2026-03", ZEROS, ZEROS, ZEROS));

        controller.obter(1L, "2026-03", null);

        verify(useCase).obter(eq(1L), eq(YearMonth.of(2026, 3)), isNull());
    }

    @Test
    void devePassarBuscaCorretamente() {
        when(useCase.obter(eq(1L), isNull(), eq("pix"))).thenReturn(new ResumoMesDTO("2026-05", ZEROS, ZEROS, ZEROS));

        controller.obter(1L, null, "pix");

        verify(useCase).obter(eq(1L), isNull(), eq("pix"));
    }

    @Test
    void deveLancarMesFormatoInvalidoExceptionParaMesInvalido() {
        assertThatThrownBy(() -> controller.obter(1L, "2026-13", null))
                .isInstanceOf(MesFormatoInvalidoException.class)
                .hasMessageContaining("2026-13");
    }

    @Test
    void deveLancarMesFormatoInvalidoExceptionParaFormatoErrado() {
        assertThatThrownBy(() -> controller.obter(1L, "05-2026", null))
                .isInstanceOf(MesFormatoInvalidoException.class);
    }

    @Test
    void devePassarMesNuloQuandoMesParametroNuloOuBranco() {
        when(useCase.obter(eq(7L), isNull(), isNull())).thenReturn(new ResumoMesDTO("2026-05", ZEROS, ZEROS, ZEROS));

        controller.obter(7L, null, null);
        controller.obter(7L, "", null);
        controller.obter(7L, "   ", null);

        verify(useCase, org.mockito.Mockito.times(3)).obter(eq(7L), isNull(), isNull());
    }
}
