package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoDetalheDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.BuscarPedidoUseCase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ListarPedidosUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoAutorizadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoControllerDetalheTest {

    @Mock private ListarPedidosUseCase listarUseCase;
    @Mock private BuscarPedidoUseCase buscarUseCase;
    @Mock private br.com.satyan.stering.saita.financasbottelegram.application.usecases.ObterUrlImagemPedidoUseCase obterUrlImagemPedidoUseCase;
    @Mock private br.com.satyan.stering.saita.financasbottelegram.application.usecases.ObterUrlComprovanteUseCase obterUrlComprovanteUseCase;

    private PedidoController controller;

    @BeforeEach
    void setUp() {
        controller = new PedidoController(listarUseCase, buscarUseCase, obterUrlImagemPedidoUseCase, obterUrlComprovanteUseCase);
    }

    private PedidoDetalheDTO detalhe(Long id) {
        return new PedidoDetalheDTO(id, new BigDecimal("200.00"), "Boleto luz",
                null, StatusPedido.PENDENTE, LocalDate.of(2026, 5, 1), null, false);
    }

    @Test
    void deveRetornarDetalheDoPedido() {
        when(buscarUseCase.buscar(1L, 42L)).thenReturn(detalhe(1L));

        PedidoDetalheDTO resultado = controller.buscar(1L, 42L);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.valor()).isEqualByComparingTo("200.00");
    }

    @Test
    void devePropagarPedidoNaoEncontrado() {
        when(buscarUseCase.buscar(99L, 42L)).thenThrow(new PedidoNaoEncontradoException(99L));

        assertThatThrownBy(() -> controller.buscar(99L, 42L))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }

    @Test
    void devePropagarPedidoNaoAutorizado() {
        when(buscarUseCase.buscar(1L, 42L)).thenThrow(new PedidoNaoAutorizadoException(1L, 42L));

        assertThatThrownBy(() -> controller.buscar(1L, 42L))
                .isInstanceOf(PedidoNaoAutorizadoException.class);
    }
}
