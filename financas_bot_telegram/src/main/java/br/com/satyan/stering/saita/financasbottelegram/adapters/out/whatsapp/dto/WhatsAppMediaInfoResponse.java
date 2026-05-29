package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppMediaInfoResponse(
    String url,
    @JsonProperty("mime_type") String mimeType,
    @JsonProperty("file_size") Long fileSize,
    String id
) {}
