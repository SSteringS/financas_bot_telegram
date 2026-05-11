package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Token de uso único recebido via link mágico")
public record AuthExchangeRequest(
        @NotBlank
        @Size(min = 16, max = 128)
        @Schema(description = "Token gerado pelo admin, recebido como query param ?t=", example = "ABCdef123...")
        String token
) {}
