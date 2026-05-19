package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Agregado de pedidos por status (quantidade + total)")
public record ResumoStatusDTO(
        @Schema(example = "3") int quantidade,
        @Schema(example = "7230.00") BigDecimal total
) {}
