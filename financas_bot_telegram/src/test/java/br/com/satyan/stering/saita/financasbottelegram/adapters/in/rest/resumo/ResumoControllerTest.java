package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.resumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoStatusDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ResumoMesUseCase;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumoControllerTest {

    @Mock private ResumoMesUseCase useCase;

    private ResumoController controller;

    @BeforeEach
    void setUp() {
        controller = new ResumoController(useCase);
    }

    @Test
    void deveRetornarDTODoUseCase() {
        ResumoMesDTO esperado = new ResumoMesDTO("2026-05",
                new ResumoStatusDTO(3, new BigDecimal("900.00")),
                new ResumoStatusDTO(2, new BigDecimal("500.00")));
        when(useCase.obter(42L)).thenReturn(esperado);

        ResumoMesDTO resultado = controller.obter(42L);

        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    void devePassarRequisitanteIdCorretamente() {
        when(useCase.obter(7L)).thenReturn(new ResumoMesDTO("2026-05",
                new ResumoStatusDTO(0, BigDecimal.ZERO),
                new ResumoStatusDTO(0, BigDecimal.ZERO)));

        controller.obter(7L);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(useCase).obter(captor.capture());
        assertThat(captor.getValue()).isEqualTo(7L);
    }
}
