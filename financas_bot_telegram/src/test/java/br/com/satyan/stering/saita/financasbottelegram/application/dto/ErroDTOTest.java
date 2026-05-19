package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ErroDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deveSerializarCodigoEMensagem() throws Exception {
        ErroDTO erro = new ErroDTO("PEDIDO_NAO_ENCONTRADO", "Pedido com id 999 não foi encontrado");
        String json = mapper.writeValueAsString(erro);
        assertThat(json).contains("\"codigo\":\"PEDIDO_NAO_ENCONTRADO\"");
        assertThat(json).contains("\"mensagem\":\"Pedido com id 999 não foi encontrado\"");
    }
}
