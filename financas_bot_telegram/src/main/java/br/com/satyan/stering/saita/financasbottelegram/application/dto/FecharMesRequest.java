package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body para {@code POST /api/funcionarios/{id}/fechamentos}.
 *
 * <p>Exemplo:
 * <pre>{ "mes": "2026-05", "ajuste": 0.00 }</pre>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FecharMesRequest {

    /** Mês de referência no formato YYYY-MM (ex: "2026-05"). */
    @NotBlank(message = "mes é obrigatório (formato: YYYY-MM)")
    private String mes;

    /**
     * Ajuste manual sobre o valor líquido.
     * Positivo = bônus; negativo = desconto extra. Padrão: zero.
     */
    @DecimalMin(value = "-999999.99", message = "ajuste fora do intervalo permitido")
    private BigDecimal ajuste = BigDecimal.ZERO;
}
