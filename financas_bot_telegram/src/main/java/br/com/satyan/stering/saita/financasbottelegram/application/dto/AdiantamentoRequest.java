package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdiantamentoRequest {

    @NotBlank(message = "descricao é obrigatória")
    private String descricao;

    @NotNull(message = "valorTotal é obrigatório")
    @DecimalMin(value = "0.01", message = "valorTotal deve ser positivo")
    private BigDecimal valorTotal;

    @NotNull(message = "valorParcela é obrigatório")
    @DecimalMin(value = "0.01", message = "valorParcela deve ser positivo")
    private BigDecimal valorParcela;

    @NotNull(message = "numParcelas é obrigatório")
    @Min(value = 1, message = "numParcelas deve ser pelo menos 1")
    private Integer numParcelas;

    @NotNull(message = "dataInicio é obrigatória")
    private LocalDate dataInicio;
}
