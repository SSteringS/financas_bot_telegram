package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.folha;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.AdiantamentoRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.AdiantamentoResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.FecharMesRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoFolhaResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ValeRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ValeResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarAdiantamentoPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CancelarAdiantamentoPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarValePortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.FecharMesPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
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
 *   <li>{@code POST /api/v1/funcionarios/{id}/vales} — Cadastrar vale</li>
 *   <li>{@code GET  /api/v1/funcionarios/{id}/vales?mes=YYYY-MM} — Listar vales do mês</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/funcionarios")
public class FolhaController {

    private final CadastrarValePortIn cadastrarValeUseCase;
    private final PedidoPagamentoRepositoryPort pedidoRepository;
    private final CadastrarAdiantamentoPortIn cadastrarAdiantamentoUseCase;
    private final CancelarAdiantamentoPortIn cancelarAdiantamentoUseCase;
    private final AdiantamentoRepositoryPortOut adiantamentoRepository;
    private final FecharMesPortIn fecharMesUseCase;

    public FolhaController(
            CadastrarValePortIn cadastrarValeUseCase,
            PedidoPagamentoRepositoryPort pedidoRepository,
            CadastrarAdiantamentoPortIn cadastrarAdiantamentoUseCase,
            CancelarAdiantamentoPortIn cancelarAdiantamentoUseCase,
            AdiantamentoRepositoryPortOut adiantamentoRepository,
            FecharMesPortIn fecharMesUseCase) {
        this.cadastrarValeUseCase = cadastrarValeUseCase;
        this.pedidoRepository = pedidoRepository;
        this.cadastrarAdiantamentoUseCase = cadastrarAdiantamentoUseCase;
        this.cancelarAdiantamentoUseCase = cancelarAdiantamentoUseCase;
        this.adiantamentoRepository = adiantamentoRepository;
        this.fecharMesUseCase = fecharMesUseCase;
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

    // ── Adiantamentos ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/adiantamentos")
    @Operation(summary = "Cadastra um adiantamento parcelado para o funcionário")
    public ResponseEntity<AdiantamentoResponse> cadastrarAdiantamento(
            @PathVariable Long id,
            @Valid @RequestBody AdiantamentoRequest request) {

        Adiantamento adiantamento = Adiantamento.builder()
                .descricao(request.getDescricao())
                .valorTotal(request.getValorTotal())
                .valorParcela(request.getValorParcela())
                .numParcelas(request.getNumParcelas())
                .dataInicio(request.getDataInicio())
                .build();

        Adiantamento criado = cadastrarAdiantamentoUseCase.cadastrar(id, adiantamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdiantamentoResponse.from(criado));
    }

    @GetMapping("/{id}/adiantamentos")
    @Operation(summary = "Lista adiantamentos ativos do funcionário")
    public List<AdiantamentoResponse> listarAdiantamentos(@PathVariable Long id) {
        return adiantamentoRepository.findAtivosParaFuncionario(id).stream()
                .map(AdiantamentoResponse::from)
                .toList();
    }

    @DeleteMapping("/adiantamentos/{adiantamentoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela (soft delete: ativo=false) um adiantamento")
    public void cancelarAdiantamento(@PathVariable Long adiantamentoId) {
        cancelarAdiantamentoUseCase.cancelar(adiantamentoId);
    }

    // ── Fechamentos ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/fechamentos")
    @Operation(summary = "Fecha o mês para o funcionário — gera Pedido FOLHA com breakdown")
    public ResponseEntity<PedidoFolhaResponse> fecharMes(
            @PathVariable Long id,
            @Valid @RequestBody FecharMesRequest request) {

        YearMonth mes = parseMesYearMonth(request.getMes());
        PedidoPagamento pedidoFolha = fecharMesUseCase.fechar(id, mes, request.getAjuste());
        return ResponseEntity.ok(PedidoFolhaResponse.from(pedidoFolha));
    }

    @GetMapping("/{id}/fechamentos")
    @Operation(summary = "Lista fechamentos anteriores do funcionário (Pedidos FOLHA) por mes_referencia DESC")
    public List<PedidoFolhaResponse> listarFechamentos(@PathVariable Long id) {
        return pedidoRepository.findFolhasByFuncionario(id).stream()
                .map(PedidoFolhaResponse::from)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private YearMonth parseMesYearMonth(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de mês inválido: '" + mes + "'. Use YYYY-MM.");
        }
    }

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
