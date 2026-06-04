package br.com.satyan.stering.saita.financasbottelegram.application.port.in;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;

/**
 * Port de entrada para cadastrar um vale para um funcionário.
 *
 * <p>Um vale é um pedido com {@code categoria=VALE}, {@code funcionario_id} preenchido,
 * {@code fechado=false} e {@code status=PENDENTE} (aguarda desconto no fechamento do mês).
 */
public interface CadastrarValePortIn {

    PedidoPagamento cadastrar(Long funcionarioId, PedidoPagamento vale);
}
