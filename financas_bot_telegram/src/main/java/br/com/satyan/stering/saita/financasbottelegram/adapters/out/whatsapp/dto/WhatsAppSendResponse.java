package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppSendResponse(
    @JsonProperty("messaging_product") String messagingProduct,
    List<MessageId> messages
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageId(String id) {}
}
