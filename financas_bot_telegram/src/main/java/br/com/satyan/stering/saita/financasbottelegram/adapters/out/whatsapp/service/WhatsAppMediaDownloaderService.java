package br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.dto.WhatsAppMediaInfoResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class WhatsAppMediaDownloaderService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppMediaDownloaderService.class);

    private final RestClient restClient;
    private final String accessToken;
    private final ObjectMapper objectMapper;

    public WhatsAppMediaDownloaderService(
        RestClient.Builder restClientBuilder,
        @Value("${whatsapp.graph-api-base-url}") String graphApiBaseUrl,
        @Value("${whatsapp.graph-api-version}") String graphApiVersion,
        @Value("${whatsapp.access-token}") String accessToken,
        ObjectMapper objectMapper
    ) {
        this.restClient = restClientBuilder
            .baseUrl(graphApiBaseUrl + "/" + graphApiVersion)
            .build();
        this.accessToken = accessToken;
        this.objectMapper = objectMapper;
    }

    public byte[] baixar(String mediaId) {
        logger.info("Baixando mídia WhatsApp mediaId={}", mediaId);

        WhatsAppMediaInfoResponse mediaInfo = buscarInfoMidia(mediaId);
        logger.debug("URL temporária de download obtida para mediaId={}", mediaId);

        return baixarBytes(mediaInfo.url());
    }

    private WhatsAppMediaInfoResponse buscarInfoMidia(String mediaId) {
        try {
            return restClient.get()
                .uri("/{mediaId}", mediaId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(WhatsAppMediaInfoResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToApiException(e);
        }
    }

    private byte[] baixarBytes(String mediaUrl) {
        try {
            // mediaUrl é absoluta (https://lookaside.fbsbx.com/...) — RestClient usa como-está
            return restClient.get()
                .uri(mediaUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(byte[].class);
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
