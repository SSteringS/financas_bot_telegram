package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppValue(
    @JsonProperty("messaging_product") String messagingProduct,
    WhatsAppMetadata metadata,
    List<WhatsAppContact> contacts,
    List<WhatsAppMessage> messages,
    List<WhatsAppStatus> statuses
) {}
