package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.funcionario;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.FuncionarioRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.FuncionarioResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.AtualizarFuncionarioPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarFuncionarioPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.FormaPagamento;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gerenciamento de funcionários domésticos.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST   /api/v1/funcionarios} — Cadastrar funcionário</li>
 *   <li>{@code GET    /api/v1/funcionarios} — Listar ativos</li>
 *   <li>{@code GET    /api/v1/funcionarios/{id}} — Buscar por ID</li>
 *   <li>{@code PUT    /api/v1/funcionarios/{id}} — Atualizar dados</li>
 *   <li>{@code DELETE /api/v1/funcionarios/{id}} — Desativar (soft delete)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/funcionarios")
public class FuncionarioController {

    private final CadastrarFuncionarioPortIn cadastrarUseCase;
    private final AtualizarFuncionarioPortIn atualizarUseCase;
    private final FuncionarioRepositoryPortOut repository;

    public FuncionarioController(
            CadastrarFuncionarioPortIn cadastrarUseCase,
            AtualizarFuncionarioPortIn atualizarUseCase,
            FuncionarioRepositoryPortOut repository) {
        this.cadastrarUseCase = cadastrarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.repository = repository;
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo funcionário doméstico")
    public ResponseEntity<FuncionarioResponse> cadastrar(@Valid @RequestBody FuncionarioRequest request) {
        Funcionario funcionario = toFuncionario(request);
        Funcionario criado = cadastrarUseCase.cadastrar(funcionario);
        return ResponseEntity.status(HttpStatus.CREATED).body(FuncionarioResponse.from(criado));
    }

    @GetMapping
    @Operation(summary = "Lista todos os funcionários ativos")
    public List<FuncionarioResponse> listar() {
        return repository.findAllAtivos().stream()
                .map(FuncionarioResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca funcionário por ID")
    public FuncionarioResponse buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(FuncionarioResponse::from)
                .orElseThrow(() -> new FuncionarioNaoEncontradoException(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados do funcionário")
    public FuncionarioResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody FuncionarioRequest request) {
        Funcionario atualizado = atualizarUseCase.atualizar(id, toFuncionario(request));
        return FuncionarioResponse.from(atualizado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa funcionário (soft delete — ativo=false)")
    public void desativar(@PathVariable Long id) {
        // Verifica existência antes de tentar desativar
        repository.findById(id)
                .orElseThrow(() -> new FuncionarioNaoEncontradoException(id));
        repository.deleteById(id);
    }

    private Funcionario toFuncionario(FuncionarioRequest req) {
        return Funcionario.builder()
                .nome(req.getNome())
                .salarioBase(req.getSalarioBase())
                .formaPagamento(req.getFormaPagamento())
                .chavePix(req.getChavePix())
                .banco(req.getBanco())
                .agencia(req.getAgencia())
                .conta(req.getConta())
                .tipoConta(req.getTipoConta())
                .contaPropria(req.getContaPropria() != null ? req.getContaPropria() : true)
                .obsPagamento(req.getObsPagamento())
                .diaPagamentoReferencia(req.getDiaPagamentoReferencia())
                .build();
    }
}
