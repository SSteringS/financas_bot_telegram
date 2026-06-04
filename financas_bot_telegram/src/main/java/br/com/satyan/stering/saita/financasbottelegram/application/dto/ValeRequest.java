package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class ValeRequest {

    @NotBlank(message = "descricao é obrigatória")
    private String descricao;

    @NotNull(message = "valor é obrigatório")
    @DecimalMin(value = "0.01", message = "valor deve ser positivo")
    private BigDecimal valor;

    /**
     * Data do vale. Se não informada, usa a data de hoje.
     * Permite registrar vales retroativos.
     */
    private LocalDate dataPedido;
}
