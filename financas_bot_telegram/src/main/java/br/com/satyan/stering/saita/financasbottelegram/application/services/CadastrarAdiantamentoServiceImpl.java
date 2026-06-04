package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarAdiantamentoPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Implementação do use case de cadastro de adiantamento.
 *
 * <p>Valida:
 * <ul>
 *   <li>Funcionário existe e está ativo</li>
 *   <li>Consistência matemática: |valorTotal - valorParcela × numParcelas| ≤ 0.01</li>
 * </ul>
 *
 * <p>A validação matemática é feita ANTES do banco para dar mensagem clara ao usuário,
 * evitando que a {@code DataIntegrityViolationException} do CHECK constraint vaze para a API.
 */
@Service
public class CadastrarAdiantamentoServiceImpl implements CadastrarAdiantamentoPortIn {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01");

    private final FuncionarioRepositoryPortOut funcionarioRepository;
    private final AdiantamentoRepositoryPortOut adiantamentoRepository;

    public CadastrarAdiantamentoServiceImpl(
            FuncionarioRepositoryPortOut funcionarioRepository,
            AdiantamentoRepositoryPortOut adiantamentoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.adiantamentoRepository = adiantamentoRepository;
    }

    @Override
    public Adiantamento cadastrar(Long funcionarioId, Adiantamento adiantamento) {
        // Validar funcionário ativo
        funcionarioRepository.findAtivoById(funcionarioId)
                .orElseThrow(() -> new FuncionarioNaoEncontradoException(
                        "Funcionário não encontrado ou inativo: id=" + funcionarioId));

        // Validar consistência matemática do plano
        validarConsistenciaMatemática(adiantamento);

        Adiantamento paraSalvar = Adiantamento.builder()
                .funcionarioId(funcionarioId)
                .descricao(adiantamento.getDescricao())
                .valorTotal(adiantamento.getValorTotal())
                .valorParcela(adiantamento.getValorParcela())
                .numParcelas(adiantamento.getNumParcelas())
                .parcelasPagas(0)
                .dataInicio(adiantamento.getDataInicio())
                .ativo(true)
                .build();

        return adiantamentoRepository.save(paraSalvar);
    }

    /**
     * Valida que |valorTotal - valorParcela × numParcelas| ≤ 0.01.
     *
     * <p>Essa invariante é espelhada no banco como {@code chk_adiant_consistencia}.
     * Validar aqui (application layer) garante mensagem de erro clara antes de chegar no banco.
     */
    static void validarConsistenciaMatemática(Adiantamento a) {
        BigDecimal totalCalculado = a.getValorParcela().multiply(new BigDecimal(a.getNumParcelas()));
        BigDecimal diferenca = a.getValorTotal().subtract(totalCalculado).abs();
        if (diferenca.compareTo(TOLERANCIA) > 0) {
            throw new IllegalArgumentException(
                    "Plano inconsistente: |valorTotal (" + a.getValorTotal()
                            + ") - valorParcela (" + a.getValorParcela()
                            + ") × numParcelas (" + a.getNumParcelas()
                            + ")| = " + diferenca + " > 0.01");
        }
    }
}
