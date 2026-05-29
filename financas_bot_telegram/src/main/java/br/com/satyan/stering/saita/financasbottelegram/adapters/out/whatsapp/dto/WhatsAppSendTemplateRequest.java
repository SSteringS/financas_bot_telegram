package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WhatsAppSendTemplateRequest(
    @JsonProperty("messaging_product") String messagingProduct,
    String to,
    String type,
    TemplateContent template
) {
    record TemplateContent(String name, LanguageCode language, List<Component> components) {}
    record LanguageCode(String code) {}
    record Component(String type, List<Parameter> parameters) {}
    record Parameter(String type, String text) {}

    public static WhatsAppSendTemplateRequest of(
        String waId,
        String templateName,
        String languageCode,
        List<TemplateParametro> parametros
    ) {
        List<Parameter> params = parametros.stream()
            .map(p -> new Parameter(p.tipo(), p.valor()))
            .toList();
        return new WhatsAppSendTemplateRequest(
            "whatsapp",
            waId,
            "template",
            new TemplateContent(templateName, new LanguageCode(languageCode), List.of(new Component("body", params)))
        );
    }
}
