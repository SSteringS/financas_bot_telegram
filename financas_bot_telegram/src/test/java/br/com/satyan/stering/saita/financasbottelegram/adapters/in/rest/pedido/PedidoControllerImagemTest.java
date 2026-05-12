package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.usecases.BuscarPedidoUseCase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ListarPedidosUseCase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ObterUrlComprovanteUseCase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ObterUrlImagemPedidoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PedidoControllerImagemTest {

    @Mock private ListarPedidosUseCase listarUseCase;
    @Mock private BuscarPedidoUseCase buscarUseCase;
    @Mock private ObterUrlImagemPedidoUseCase obterUrlImagemPedidoUseCase;
    @Mock private ObterUrlComprovanteUseCase obterUrlComprovanteUseCase;

    private PedidoController controller;

    @BeforeEach
    void setUp() {
        controller = new PedidoController(listarUseCase, buscarUseCase, obterUrlImagemPedidoUseCase, obterUrlComprovanteUseCase);
    }

    @Test
    void fotoPedido_deveRetornar302ComLocationHeader() {
        String presigned = "https://s3.amazonaws.com/bucket/pedidos/abc.jpg?X-Amz-Signature=sig";
        when(obterUrlImagemPedidoUseCase.obter(1L, 42L)).thenReturn(presigned);

        ResponseEntity<Void> response = controller.fotoPedido(1L, 42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(presigned);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("private, max-age=600");
    }

    @Test
    void comprovante_deveRetornar302ComLocationHeader() {
        String presigned = "https://s3.amazonaws.com/bucket/pedidos/comprovante.jpg?X-Amz-Signature=sig";
        when(obterUrlComprovanteUseCase.obter(1L, 42L)).thenReturn(presigned);

        ResponseEntity<Void> response = controller.comprovante(1L, 42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(presigned);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("private, max-age=600");
    }
}
