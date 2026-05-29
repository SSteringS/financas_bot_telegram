package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto.TemplateParametro;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto.WhatsAppSendResponse;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto.WhatsAppSendTemplateRequest;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto.WhatsAppSendTextRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class WhatsAppMessageSenderService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppMessageSenderService.class);

    private final RestClient restClient;
    private final String accessToken;
    private final String phoneNumberId;
    private final ObjectMapper objectMapper;

    public WhatsAppMessageSenderService(
        RestClient.Builder restClientBuilder,
        @Value("${whatsapp.graph-api-base-url}") String graphApiBaseUrl,
        @Value("${whatsapp.graph-api-version}") String graphApiVersion,
        @Value("${whatsapp.phone-number-id}") String phoneNumberId,
        @Value("${whatsapp.access-token}") String accessToken,
        ObjectMapper objectMapper
    ) {
        this.restClient = restClientBuilder
            .baseUrl(graphApiBaseUrl + "/" + graphApiVersion)
            .build();
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.objectMapper = objectMapper;
    }

    public WhatsAppSendResponse enviarTexto(String waId, String body) {
        logger.info("Enviando mensagem de texto WhatsApp para waId={}", waId);
        return post(WhatsAppSendTextRequest.of(waId, body));
    }

    public WhatsAppSendResponse enviarTemplate(
        String waId,
        String templateName,
        String languageCode,
        List<TemplateParametro> parametros
    ) {
        logger.info("Enviando mensagem de template WhatsApp para waId={} template={}", waId, templateName);
        return post(WhatsAppSendTemplateRequest.of(waId, templateName, languageCode, parametros));
    }

    private WhatsAppSendResponse post(Object requestBody) {
        try {
            return restClient.post()
                .uri("/{phoneNumberId}/messages", phoneNumberId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(WhatsAppSendResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToApiException(e);
        }
    }

    private WhatsAppApiException mapToApiException(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String rawBody = e.getResponseBodyAsString();
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                Integer code = error.path("code").isMissingNode() ? null : error.path("code").intValue();
                String title = error.path("message").asText(null);
                return new WhatsAppApiException(status, code, title, rawBody);
            }
        } catch (Exception parseEx) {
            logger.debug("Não foi possível parsear erro da Graph API: {}", parseEx.getMessage());
        }
        return new WhatsAppApiException(status, rawBody);
    }
}
