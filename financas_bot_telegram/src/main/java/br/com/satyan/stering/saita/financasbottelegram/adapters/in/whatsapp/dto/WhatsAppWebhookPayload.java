package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayload(String object, List<WhatsAppEntry> entry) {}
