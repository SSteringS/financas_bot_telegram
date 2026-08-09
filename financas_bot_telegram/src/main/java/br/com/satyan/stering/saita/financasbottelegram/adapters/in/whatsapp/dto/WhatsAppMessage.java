package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppMessage(
    String from,
    String id,
    String timestamp,
    String type,
    WhatsAppText text,
    WhatsAppImage image,
    WhatsAppDocument document
) {}
