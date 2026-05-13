package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta dos endpoints de autenticação (/auth/exchange e /auth/me)")
public record AuthMeResponse(
        RequisitanteDTO requisitante
) {}
