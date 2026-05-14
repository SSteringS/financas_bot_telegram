package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginaDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deveSerializarPaginaComItems() throws Exception {
        PaginaDTO<String> pagina = new PaginaDTO<>(List.of("a", "b"), 50L, 0, 20, 3);

        String json = mapper.writeValueAsString(pagina);

        assertThat(json).contains("\"items\":[\"a\",\"b\"]");
        assertThat(json).contains("\"total\":50");
        assertThat(json).contains("\"pagina\":0");
        assertThat(json).contains("\"tamanho\":20");
        assertThat(json).contains("\"totalPaginas\":3");
    }

    @Test
    void deveSerializarPaginaVazia() throws Exception {
        PaginaDTO<Object> pagina = new PaginaDTO<>(List.of(), 0L, 0, 20, 0);
        String json = mapper.writeValueAsString(pagina);
        assertThat(json).contains("\"items\":[]");
        assertThat(json).contains("\"total\":0");
    }
}
