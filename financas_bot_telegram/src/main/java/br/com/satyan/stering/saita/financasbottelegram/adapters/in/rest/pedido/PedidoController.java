package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.pedido;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ListarPedidosFiltro;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaginaDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoResumoDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ListarPedidosUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.RequisitanteId;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final ListarPedidosUseCase listarUseCase;

    public PedidoController(ListarPedidosUseCase listarUseCase) {
        this.listarUseCase = listarUseCase;
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
}
