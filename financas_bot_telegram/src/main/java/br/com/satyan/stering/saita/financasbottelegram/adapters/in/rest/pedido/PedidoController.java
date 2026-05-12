package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.pedido;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ListarPedidosFiltro;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaginaDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoDetalheDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoResumoDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.BuscarPedidoUseCase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ListarPedidosUseCase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ObterUrlComprovanteUseCase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ObterUrlImagemPedidoUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.RequisitanteId;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final ListarPedidosUseCase listarUseCase;
    private final BuscarPedidoUseCase buscarUseCase;
    private final ObterUrlImagemPedidoUseCase obterUrlImagemPedidoUseCase;
    private final ObterUrlComprovanteUseCase obterUrlComprovanteUseCase;

    public PedidoController(ListarPedidosUseCase listarUseCase,
                             BuscarPedidoUseCase buscarUseCase,
                             ObterUrlImagemPedidoUseCase obterUrlImagemPedidoUseCase,
                             ObterUrlComprovanteUseCase obterUrlComprovanteUseCase) {
        this.listarUseCase = listarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.obterUrlImagemPedidoUseCase = obterUrlImagemPedidoUseCase;
        this.obterUrlComprovanteUseCase = obterUrlComprovanteUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista pedidos do requisitante autenticado, com filtros e paginação")
    public PaginaDTO<PedidoResumoDTO> listar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(name = "tipo", required = false) List<TipoPagamento> tipos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequisitanteId Long requisitanteId) {

        return listarUseCase.listar(
                new ListarPedidosFiltro(status, tipos, de, ate, busca, page, tamanho),
                requisitanteId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um pedido específico")
    public PedidoDetalheDTO buscar(
            @PathVariable Long id,
            @RequisitanteId Long requisitanteId) {
        return buscarUseCase.buscar(id, requisitanteId);
    }

    @GetMapping("/{id}/foto-pedido")
    @Operation(summary = "Redireciona pra pre-signed URL da foto original do pedido")
    public ResponseEntity<Void> fotoPedido(
            @PathVariable Long id,
            @RequisitanteId Long requisitanteId) {
        String url = obterUrlImagemPedidoUseCase.obter(id, requisitanteId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=600")
                .build();
    }

    @GetMapping("/{id}/comprovante")
    @Operation(summary = "Redireciona pra pre-signed URL do comprovante do pedido")
    public ResponseEntity<Void> comprovante(
            @PathVariable Long id,
            @RequisitanteId Long requisitanteId) {
        String url = obterUrlComprovanteUseCase.obter(id, requisitanteId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=600")
                .build();
    }
}
