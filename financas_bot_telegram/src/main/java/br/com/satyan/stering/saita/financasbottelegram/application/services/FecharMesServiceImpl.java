package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.FecharMesPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FechamentoDuplicadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.CategoriaPedido;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação do use case de fechamento mensal da folha de pagamento.
 *
 * <p>Executa o algoritmo de 11 passos da spec EVO-09 §4 dentro de uma única transação
 * {@code @Transactional}: qualquer falha reverte todos os passos (vales, adiantamentos, pedido).
 *
 * <p>Idempotência em duas camadas:
 * <ol>
 *   <li>Passo 1 — check no banco antes de criar qualquer coisa.</li>
 *   <li>UNIQUE INDEX {@code uq_folha_por_mes} — guarda contra race condition de duplo-clique;
 *       {@code DataIntegrityViolationException} é traduzida para {@code FechamentoDuplicadoException}.</li>
 * </ol>
 */
@Service
public class FecharMesServiceImpl implements FecharMesPortIn {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final FuncionarioRepositoryPortOut funcionarioRepository;
    private final PedidoPagamentoRepositoryPort pedidoRepository;
    private final AdiantamentoRepositoryPortOut adiantamentoRepository;

    public FecharMesServiceImpl(
            FuncionarioRepositoryPortOut funcionarioRepository,
            PedidoPagamentoRepositoryPort pedidoRepository,
            AdiantamentoRepositoryPortOut adiantamentoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.adiantamentoRepository = adiantamentoRepository;
    }

    @Override
    @Transactional
    public PedidoPagamento fechar(Long funcionarioId, YearMonth mes, BigDecimal ajuste) {
        BigDecimal ajusteEfetivo = ajuste != null ? ajuste : BigDecimal.ZERO;

        // ─── (1) IDEMPOTÊNCIA ────────────────────────────────────────────────
        LocalDate primeiroDoMes = mes.atDay(1);
        if (pedidoRepository.existsFolha(funcionarioId, primeiroDoMes)) {
            throw new FechamentoDuplicadoException(funcionarioId, mes);
        }

        // ─── (2) Funcionário ativo ───────────────────────────────────────────
        Funcionario funcionario = funcionarioRepository.findAtivoById(funcionarioId)
                .orElseThrow(() -> new FuncionarioNaoEncontradoException(
                        "Funcionário não encontrado ou inativo: id=" + funcionarioId));

        // ─── (3) Período ─────────────────────────────────────────────────────
        LocalDate ultimoDoMes = mes.atEndOfMonth();

        // ─── (4) Vales abertos do período ────────────────────────────────────
        List<PedidoPagamento> vales =
                pedidoRepository.findValesAbertos(funcionarioId, primeiroDoMes, ultimoDoMes);

        // ─── (5) Adiantamentos ativos com parcelas a pagar neste mês ─────────
        List<Adiantamento> adiantamentosAtivos = adiantamentoRepository
                .findAtivosParaFechamento(funcionarioId)
                .stream()
                .filter(a -> a.getDataInicio() != null
                        && !a.getDataInicio().isAfter(primeiroDoMes))
                .filter(a -> a.getParcelasPagas() != null
                        && a.getNumParcelas() != null
                        && a.getParcelasPagas() < a.getNumParcelas())
                .collect(Collectors.toList());

        // ─── (6) Cálculo do valor líquido ────────────────────────────────────
        BigDecimal totalVales = vales.stream()
                .map(PedidoPagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalParcelas = adiantamentosAtivos.stream()
                .map(Adiantamento::getValorParcela)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorFinal = funcionario.getSalarioBase()
                .subtract(totalVales)
                .subtract(totalParcelas)
                .add(ajusteEfetivo);

        // ─── (7) Texto de observação ─────────────────────────────────────────
        String observacao = gerarTextoFechamento(
                funcionario.getSalarioBase(),
                totalVales, vales.size(),
                totalParcelas, adiantamentosAtivos.size(),
                ajusteEfetivo, valorFinal);

        // ─── (8) Criar Pedido FOLHA ──────────────────────────────────────────
        PedidoPagamento pedidoParaSalvar = PedidoPagamento.builder()
                .categoria(CategoriaPedido.FOLHA)
                .funcionarioId(funcionarioId)
                .valor(valorFinal)
                .status(StatusPedido.PENDENTE)
                .observacao(observacao)
                .mesReferencia(primeiroDoMes)
                .dataPedido(primeiroDoMes)
                .fechado(false)
                .build();

        PedidoPagamento pedidoFolha;
        try {
            pedidoFolha = pedidoRepository.save(pedidoParaSalvar);
        } catch (DataIntegrityViolationException e) {
            // Race condition: duplo-clique passou pelo check mas bateu no UNIQUE INDEX
            throw new FechamentoDuplicadoException(funcionarioId, mes);
        }

        // ─── (9) Marcar vales como fechados ──────────────────────────────────
        if (!vales.isEmpty()) {
            List<Long> idsVales = vales.stream()
                    .map(PedidoPagamento::getId)
                    .collect(Collectors.toList());
            pedidoRepository.markAllClosed(idsVales);
        }

        // ─── (10) Atualizar adiantamentos ────────────────────────────────────
        for (Adiantamento adiantamento : adiantamentosAtivos) {
            adiantamento.setParcelasPagas(adiantamento.getParcelasPagas() + 1);
            if (adiantamento.getParcelasPagas() >= adiantamento.getNumParcelas()) {
                adiantamento.setAtivo(false);
            }
            adiantamentoRepository.save(adiantamento);
        }

        // ─── (11) Retornar Pedido FOLHA criado ───────────────────────────────
        return pedidoFolha;
    }

    /**
     * Gera o texto de observação da folha com breakdown legível.
     *
     * <p>Formato (spec EVO-09 §4 passo 7):
     * <pre>
     * Salario base: R$ 2.000,00
     * Vales: R$ 150,00 (3 vales)
     * Adiantamentos: R$ 80,00 (2 parcelas)
     * Ajuste: R$ 0,00
     * Liquido: R$ 1.770,00
     * </pre>
     */
    static String gerarTextoFechamento(
            BigDecimal salarioBase,
            BigDecimal totalVales, int qtdVales,
            BigDecimal totalParcelas, int qtdParcelas,
            BigDecimal ajuste,
            BigDecimal valorFinal) {

        return "Salario base: " + brl(salarioBase) + "\n"
                + "Vales: " + brl(totalVales) + " (" + qtdVales + " " + pluralVale(qtdVales) + ")\n"
                + "Adiantamentos: " + brl(totalParcelas) + " (" + qtdParcelas + " " + pluralParcela(qtdParcelas) + ")\n"
                + "Ajuste: " + brl(ajuste) + "\n"
                + "Liquido: " + brl(valorFinal);
    }

    private static String brl(BigDecimal value) {
        NumberFormat nf = NumberFormat.getNumberInstance(PT_BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "R$ " + nf.format(value);
    }

    private static String pluralVale(int qtd) {
        return qtd == 1 ? "vale" : "vales";
    }

    private static String pluralParcela(int qtd) {
        return qtd == 1 ? "parcela" : "parcelas";
    }
}
