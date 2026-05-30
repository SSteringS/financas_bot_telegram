package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppDocument(
    String id,
    @JsonProperty("mime_type") String mimeType,
    String sha256,
    String filename,
    String caption
) {}
