package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppProfile(String name) {}
