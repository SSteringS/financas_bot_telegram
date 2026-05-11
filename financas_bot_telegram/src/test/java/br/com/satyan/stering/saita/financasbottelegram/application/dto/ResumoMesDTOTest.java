package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ResumoMesDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deveSerializarResumoComNested() throws Exception {
        ResumoMesDTO dto = new ResumoMesDTO(
                "2026-05",
                new ResumoStatusDTO(3, new BigDecimal("450.00")),
                new ResumoStatusDTO(7, new BigDecimal("1230.50"))
        );

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"mesAtual\":\"2026-05\"");
        assertThat(json).contains("\"pendentes\"");
        assertThat(json).contains("\"quantidade\":3");
        assertThat(json).contains("\"quantidade\":7");
        assertThat(json).contains("\"total\":450.00");
        assertThat(json).contains("\"total\":1230.50");
    }
}
