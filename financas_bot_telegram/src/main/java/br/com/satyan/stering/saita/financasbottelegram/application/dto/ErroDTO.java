package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estrutura padrão de erro retornada por endpoints REST")
public record ErroDTO(
        @Schema(description = "Código identificador do erro", example = "PEDIDO_NAO_ENCONTRADO") String codigo,
        @Schema(description = "Mensagem amigável", example = "Pedido com id 999 não foi encontrado") String mensagem
) {}
