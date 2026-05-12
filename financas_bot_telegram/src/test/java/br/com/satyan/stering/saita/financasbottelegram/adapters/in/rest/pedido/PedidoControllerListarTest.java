package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ListarPedidosFiltro;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaginaDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoResumoDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ListarPedidosUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoControllerListarTest {

    @Mock private ListarPedidosUseCase listarUseCase;

    private PedidoController controller;

    @BeforeEach
    void setUp() {
        controller = new PedidoController(listarUseCase);
    }

    @Test
    void devePassarFiltrosCorretos() {
        PaginaDTO<PedidoResumoDTO> pagina = new PaginaDTO<>(List.of(), 0, 0, 20, 0);
        when(listarUseCase.listar(any(), any())).thenReturn(pagina);

        controller.listar(StatusPedido.PAGO, null, null, null, "energia", 0, 20, 1L);

        ArgumentCaptor<ListarPedidosFiltro> captor = ArgumentCaptor.forClass(ListarPedidosFiltro.class);
        verify(listarUseCase).listar(captor.capture(), any());

        ListarPedidosFiltro filtro = captor.getValue();
        assertThat(filtro.status()).isEqualTo(StatusPedido.PAGO);
        assertThat(filtro.busca()).isEqualTo("energia");
        assertThat(filtro.tamanho()).isEqualTo(20);
    }

    @Test
    void devePassarRequisitanteIdCorretamente() {
        PaginaDTO<PedidoResumoDTO> pagina = new PaginaDTO<>(List.of(), 0, 0, 20, 0);
        when(listarUseCase.listar(any(), any())).thenReturn(pagina);

        controller.listar(null, null, null, null, null, 0, 20, 42L);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(listarUseCase).listar(any(), captor.capture());
        assertThat(captor.getValue()).isEqualTo(42L);
    }

    @Test
    void deveRetornarPaginaDoUseCase() {
        PaginaDTO<PedidoResumoDTO> pagina = new PaginaDTO<>(List.of(), 5L, 1, 10, 1);
        when(listarUseCase.listar(any(), any())).thenReturn(pagina);

        PaginaDTO<PedidoResumoDTO> resultado = controller.listar(null, null, null, null, null, 1, 10, 1L);

        assertThat(resultado.total()).isEqualTo(5L);
        assertThat(resultado.pagina()).isEqualTo(1);
    }
}
