package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo do mês atual: contagem e total de pendentes e pagos")
public record ResumoMesDTO(
        @Schema(description = "Mês de referência no formato YYYY-MM", example = "2026-05") String mesAtual,
        ResumoStatusDTO pendentes,
        ResumoStatusDTO pagos
) {}
