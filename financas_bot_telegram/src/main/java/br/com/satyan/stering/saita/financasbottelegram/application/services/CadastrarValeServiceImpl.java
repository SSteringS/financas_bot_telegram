package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarValePortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.CategoriaPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Implementação do use case de cadastro de vale.
 *
 * <p>Valida que o funcionário existe e está ativo, e que o valor é positivo.
 * O vale nasce com {@code status=PENDENTE} e {@code fechado=false} — será descontado
 * e marcado como fechado pelo {@code FecharMesUseCase}.
 *
 * <p><strong>Decisão de status (confirmada):</strong> vale nasce {@code PENDENTE} porque
 * representa um compromisso a ser descontado no fechamento mensal. O status {@code PAGO}
 * seria prematura — o dinheiro foi dado em espécie mas o desconto no salário ainda não ocorreu.
 * PO confirmou: PENDENTE é o estado correto para vales (registrado no status report BE-026).
 */
@Service
public class CadastrarValeServiceImpl implements CadastrarValePortIn {

    private final FuncionarioRepositoryPortOut funcionarioRepository;
    private final PedidoPagamentoRepositoryPort pedidoRepository;

    public CadastrarValeServiceImpl(
            FuncionarioRepositoryPortOut funcionarioRepository,
            PedidoPagamentoRepositoryPort pedidoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public PedidoPagamento cadastrar(Long funcionarioId, PedidoPagamento vale) {
        // Validar que o funcionário existe e está ativo
        funcionarioRepository.findAtivoById(funcionarioId)
                .orElseThrow(() -> new FuncionarioNaoEncontradoException(
                        "Funcionário não encontrado ou inativo: id=" + funcionarioId));

        // Validar valor positivo
        if (vale.getValor() == null || vale.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do vale deve ser positivo");
        }

        // Validar descrição obrigatória
        if (vale.getDescricao() == null || vale.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição do vale é obrigatória");
        }

        // Montar pedido como VALE
        PedidoPagamento valeParaSalvar = PedidoPagamento.builder()
                .funcionarioId(funcionarioId)
                .categoria(CategoriaPedido.VALE)
                .valor(vale.getValor())
                .descricao(vale.getDescricao())
                .dataPedido(vale.getDataPedido() != null ? vale.getDataPedido() : LocalDate.now())
                .status(StatusPedido.PENDENTE)
                .fechado(false)
                .build();

        return pedidoRepository.save(valeParaSalvar);
    }
}
