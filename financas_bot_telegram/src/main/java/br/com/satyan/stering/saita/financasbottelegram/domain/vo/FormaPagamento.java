package br.com.satyan.stering.saita.financasbottelegram.domain.vo;

/**
 * Forma de pagamento do salário do funcionário.
 *
 * <p>Dimensão ortogonal a {@link CategoriaPedido}: enquanto {@code CategoriaPedido} descreve o
 * <em>propósito</em> do pedido (vale de desconto vs. folha de pagamento), {@code FormaPagamento}
 * descreve o <em>canal financeiro</em> utilizado para transferir dinheiro ao funcionário.
 * Os dois enums são independentes e não se sobrepõem.
 */
public enum FormaPagamento {

    /** Pagamento via chave PIX — requer {@code chave_pix} preenchida no cadastro. */
    PIX,

    /** Pagamento via TED — requer {@code banco}, {@code agencia}, {@code conta} e {@code tipo_conta}. */
    TED
}
