package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppSendTextRequest(
    @JsonProperty("messaging_product") String messagingProduct,
    String to,
    String type,
    TextContent text
) {
    record TextContent(String body) {}

    public static WhatsAppSendTextRequest of(String waId, String body) {
        return new WhatsAppSendTextRequest("whatsapp", waId, "text", new TextContent(body));
    }
}
