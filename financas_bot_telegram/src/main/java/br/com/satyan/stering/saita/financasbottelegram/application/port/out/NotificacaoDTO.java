package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

public record NotificacaoDTO(
    String destinatarioCanalId,
    Long pedidoId,
    String linkSite
) {}
