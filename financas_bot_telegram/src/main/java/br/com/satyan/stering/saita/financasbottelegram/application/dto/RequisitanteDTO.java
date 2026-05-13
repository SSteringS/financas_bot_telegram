package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados do requisitante autenticado")
public record RequisitanteDTO(
        @Schema(example = "1") Long id,
        @Schema(example = "Pedro Marques") String nome
) {}
