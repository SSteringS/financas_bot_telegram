package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de um mês: contagem e total por status (todos, pendentes, pagos)")
public record ResumoMesDTO(
        @Schema(description = "Mês de referência no formato YYYY-MM", example = "2026-05") String mes,
        ResumoStatusDTO todos,
        ResumoStatusDTO pendentes,
        ResumoStatusDTO pagos
) {}
