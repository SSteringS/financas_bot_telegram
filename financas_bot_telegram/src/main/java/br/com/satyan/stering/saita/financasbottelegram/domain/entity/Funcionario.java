package br.com.satyan.stering.saita.financasbottelegram.domain.entity;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.FormaPagamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Funcionário doméstico — entidade de domínio (POJO puro, sem anotações JPA).
 *
 * <p>Espelha a tabela {@code funcionario} criada na migração V6. Campos de pagamento são
 * condicionais à {@link FormaPagamento}: PIX requer {@code chavePix}; TED requer
 * {@code banco}, {@code agencia}, {@code conta} e {@code tipoConta}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Funcionario {

    private Long id;

    private String nome;

    private BigDecimal salarioBase;

    /** Canal financeiro para pagamento do salário — PIX ou TED. */
    private FormaPagamento formaPagamento;

    /** Obrigatório quando {@code formaPagamento == PIX}. */
    private String chavePix;

    /** Obrigatório quando {@code formaPagamento == TED}. */
    private String banco;

    /** Obrigatório quando {@code formaPagamento == TED}. */
    private String agencia;

    /** Obrigatório quando {@code formaPagamento == TED}. */
    private String conta;

    /** Obrigatório quando {@code formaPagamento == TED}. Valores: CORRENTE ou POUPANCA. */
    private String tipoConta;

    /** Indica se a conta bancária é em nome do funcionário. */
    private Boolean contaPropria;

    /** Observações sobre forma de pagamento (campo livre). */
    private String obsPagamento;

    /** Dia de referência para geração da folha (1–31, pode ser null). */
    private Integer diaPagamentoReferencia;

    /** {@code false} quando o funcionário é desativado (soft delete). */
    private Boolean ativo;

    private LocalDateTime criadoEm;

    /** Atualizado automaticamente no banco; relevante para rastreabilidade de alterações de salário. */
    private LocalDateTime atualizadoEm;
}
