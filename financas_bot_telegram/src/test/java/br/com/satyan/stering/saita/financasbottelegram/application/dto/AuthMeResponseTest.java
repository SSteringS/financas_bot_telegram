package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AuthMeResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deveSerializarRequisitanteAninhado() throws Exception {
        AuthMeResponse response = new AuthMeResponse(new RequisitanteDTO(1L, "Satyan Saita"));

        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("\"requisitante\"");
        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"nome\":\"Satyan Saita\"");
    }
}
