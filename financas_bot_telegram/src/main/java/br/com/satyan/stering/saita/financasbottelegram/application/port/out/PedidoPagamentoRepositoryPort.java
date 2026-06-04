package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PedidoPagamentoRepositoryPort {

    PedidoPagamento save(PedidoPagamento pedidoPagamento);

    Optional<PedidoPagamento> findById(Long id);

    /**
     * Retorna vales abertos ({@code categoria=VALE}, {@code fechado=false}) do funcionário
     * cujo {@code data_pedido} esteja dentro do intervalo [inicio, fim] (inclusive).
     */
    List<PedidoPagamento> findValesAbertos(Long funcionarioId, LocalDate inicio, LocalDate fim);

    /**
     * Verifica se já existe um Pedido FOLHA para o funcionário no mês de referência.
     * Usado pelo {@code FecharMesUseCase} para garantir idempotência (passo 1).
     */
    boolean existsFolha(Long funcionarioId, LocalDate mesReferencia);

    /**
     * Marca todos os pedidos da lista como {@code fechado=true}.
     * Chamado pelo {@code FecharMesUseCase} após criar o Pedido FOLHA (passo 9).
     */
    void markAllClosed(List<Long> pedidoIds);

    /**
     * Lista todos os Pedidos FOLHA do funcionário, ordenados por {@code mes_referencia DESC}.
     * Usado pelo endpoint {@code GET /api/funcionarios/{id}/fechamentos}.
     */
    List<PedidoPagamento> findFolhasByFuncionario(Long funcionarioId);

    /**
     * Lista todos os vales (abertos e fechados) do funcionário no período [inicio, fim].
     * Usado pelo endpoint {@code GET /api/funcionarios/{id}/vales?mes=YYYY-MM}.
     */
    List<PedidoPagamento> findValesByFuncionarioAndPeriodo(Long funcionarioId, LocalDate inicio, LocalDate fim);
}
