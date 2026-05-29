package br.com.satyan.stering.saita.financasbottelegram.domain.model;

// Enum canal-agnóstico para idempotência (BE-19a) e roteamento de notificações (BE-21a).
// BE-21a pode renomear/unificar com seu enum Canal — coordenar no PR.
public enum CanalMensagem {
    TELEGRAM,
    WHATSAPP
}
