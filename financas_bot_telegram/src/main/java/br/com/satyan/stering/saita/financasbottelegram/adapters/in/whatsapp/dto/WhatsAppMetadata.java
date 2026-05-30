package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppMetadata(
    @JsonProperty("display_phone_number") String displayPhoneNumber,
    @JsonProperty("phone_number_id") String phoneNumberId
) {}
