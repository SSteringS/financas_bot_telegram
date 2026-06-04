package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.folha;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ValeRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ValeResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarValePortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para funcionalidades da folha de pagamento doméstica.
 *
 * <p>Endpoints de vales (BE-026), adiantamentos (BE-027) e fechamento (BE-028)
 * são consolidados neste controller.
 *
 * <p>Vales:
 * <ul>
 *   <li>{@code POST /api/funcionarios/{id}/vales} — Cadastrar vale</li>
 *   <li>{@code GET  /api/funcionarios/{id}/vales?mes=YYYY-MM} — Listar vales do mês</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/funcionarios")
public class FolhaController {

    private final CadastrarValePortIn cadastrarValeUseCase;
    private final PedidoPagamentoRepositoryPort pedidoRepository;

    public FolhaController(
            CadastrarValePortIn cadastrarValeUseCase,
            PedidoPagamentoRepositoryPort pedidoRepository) {
        this.cadastrarValeUseCase = cadastrarValeUseCase;
        this.pedidoRepository = pedidoRepository;
    }

    // ── Vales ─────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/vales")
    @Operation(summary = "Cadastra um vale para o funcionário")
    public ResponseEntity<ValeResponse> cadastrarVale(
            @PathVariable Long id,
            @Valid @RequestBody ValeRequest request) {

        PedidoPagamento vale = PedidoPagamento.builder()
                .descricao(request.getDescricao())
                .valor(request.getValor())
                .dataPedido(request.getDataPedido())
                .build();

        PedidoPagamento criado = cadastrarValeUseCase.cadastrar(id, vale);
        return ResponseEntity.status(HttpStatus.CREATED).body(ValeResponse.from(criado));
    }

    @GetMapping("/{id}/vales")
    @Operation(summary = "Lista vales do funcionário no mês informado (default: mês corrente)")
    public List<ValeResponse> listarVales(
            @PathVariable Long id,
            @RequestParam(required = false) String mes) {

        YearMonth yearMonth = parseMes(mes);
        LocalDate inicio = yearMonth.atDay(1);
        LocalDate fim = yearMonth.atEndOfMonth();

        return pedidoRepository.findValesByFuncionarioAndPeriodo(id, inicio, fim).stream()
                .map(ValeResponse::from)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private YearMonth parseMes(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de mês inválido: '" + mes + "'. Use YYYY-MM.");
        }
    }
}
