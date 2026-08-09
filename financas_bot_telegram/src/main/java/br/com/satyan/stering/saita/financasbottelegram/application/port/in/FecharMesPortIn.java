package br.com.satyan.stering.saita.financasbottelegram.application.port.in;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Port de entrada para o caso de uso de fechamento mensal da folha.
 *
 * <p>Implementado por {@code FecharMesServiceImpl} na camada de aplicação.
 */
public interface FecharMesPortIn {

    /**
     * Fecha o mês {@code mes} para o funcionário {@code funcionarioId}.
     *
     * <p>Executa o algoritmo de 11 passos da spec EVO-09 §4 dentro de uma única transação:
     * verifica idempotência, calcula salário líquido, gera Pedido FOLHA, marca vales como
     * fechados e incrementa parcelas de adiantamentos.
     *
     * @param funcionarioId ID do funcionário
     * @param mes           Mês de referência (ex: 2026-05)
     * @param ajuste        Ajuste manual (positivo = bônus, negativo = desconto extra); pode ser zero
     * @return Pedido FOLHA criado com observação de breakdown
     * @throws br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FechamentoDuplicadoException se o mês já foi fechado
     * @throws br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException se o funcionário não existir ou estiver inativo
     */
    PedidoPagamento fechar(Long funcionarioId, YearMonth mes, BigDecimal ajuste);
}
