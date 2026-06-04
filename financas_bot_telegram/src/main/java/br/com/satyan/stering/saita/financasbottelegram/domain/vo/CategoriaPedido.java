package br.com.satyan.stering.saita.financasbottelegram.domain.vo;

/**
 * Categoria de um pedido relacionado à folha de pagamento doméstica.
 *
 * <p><strong>Dimensão ortogonal a {@link FormaPagamento}:</strong> {@code FormaPagamento} descreve
 * o canal financeiro (PIX/TED), enquanto {@code CategoriaPedido} descreve o <em>propósito</em> do
 * registro na tabela {@code pedidos_pagamento} via Single-Table Inheritance (STI).
 *
 * <p>Também é ortogonal a {@code TipoPagamento} (que classifica pedidos do fluxo Telegram —
 * ex.: {@code PIX}, {@code TED}). Os dois enums coexistem sem conflito: um pedido do tipo
 * {@code FOLHA} não precisa ter {@code TipoPagamento} preenchido.
 *
 * <p>Pedidos SEM categoria (NULL) são pedidos normais do fluxo Telegram.
 */
public enum CategoriaPedido {

    /**
     * Vale concedido ao funcionário — desconto parcelado no fechamento do mês.
     * Armazenado como {@code Pedido} com {@code fechado=false} até o fechamento.
     */
    VALE,

    /**
     * Registro de fechamento mensal da folha de pagamento.
     * Contém o breakdown do cálculo em {@code observacao} e referência ao mês em
     * {@code mes_referencia}. Unicidade garantida por {@code uq_folha_por_mes}.
     */
    FOLHA
}
