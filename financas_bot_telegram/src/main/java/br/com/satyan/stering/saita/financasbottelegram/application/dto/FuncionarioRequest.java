package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.FormaPagamento;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para criação e atualização de funcionário.
 *
 * <p>Validação condicional PIX/TED: a validação de campos obrigatórios por forma de pagamento
 * é feita no use case, não via Bean Validation, para evitar complexidade de validators de classe
 * e dar mensagens de erro mais claras.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuncionarioRequest {

    @NotBlank(message = "nome é obrigatório")
    @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
    private String nome;

    @NotNull(message = "salarioBase é obrigatório")
    @DecimalMin(value = "0.01", message = "salarioBase deve ser positivo")
    private BigDecimal salarioBase;

    @NotNull(message = "formaPagamento é obrigatório")
    private FormaPagamento formaPagamento;

    /** Obrigatório quando formaPagamento = PIX. */
    private String chavePix;

    /** Obrigatório quando formaPagamento = TED. */
    private String banco;

    /** Obrigatório quando formaPagamento = TED. */
    private String agencia;

    /** Obrigatório quando formaPagamento = TED. */
    private String conta;

    /** Obrigatório quando formaPagamento = TED. Valores aceitos: CORRENTE, POUPANCA. */
    private String tipoConta;

    private Boolean contaPropria;

    @Size(max = 500, message = "obsPagamento deve ter no máximo 500 caracteres")
    private String obsPagamento;

    @Min(value = 1, message = "diaPagamentoReferencia deve ser entre 1 e 31")
    @Max(value = 31, message = "diaPagamentoReferencia deve ser entre 1 e 31")
    private Integer diaPagamentoReferencia;
}
