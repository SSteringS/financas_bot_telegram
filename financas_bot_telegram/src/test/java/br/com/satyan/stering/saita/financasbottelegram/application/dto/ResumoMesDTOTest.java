package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ResumoMesDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deveSerializarResumoComNested() throws Exception {
        ResumoStatusDTO todos = new ResumoStatusDTO(10, new BigDecimal("1680.50"));
        ResumoStatusDTO pendentes = new ResumoStatusDTO(3, new BigDecimal("450.00"));
        ResumoStatusDTO pagos = new ResumoStatusDTO(7, new BigDecimal("1230.50"));
        ResumoMesDTO dto = new ResumoMesDTO("2026-05", todos, pendentes, pagos);

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"mes\":\"2026-05\"");
        assertThat(json).contains("\"todos\"");
        assertThat(json).contains("\"pendentes\"");
        assertThat(json).contains("\"pagos\"");
        assertThat(json).contains("\"quantidade\":10");
        assertThat(json).contains("\"quantidade\":3");
        assertThat(json).contains("\"quantidade\":7");
        assertThat(json).contains("\"total\":1680.50");
    }

    @Test
    void naoDeveConterCampoMesAtual() throws Exception {
        ResumoMesDTO dto = new ResumoMesDTO("2026-05",
                new ResumoStatusDTO(0, BigDecimal.ZERO),
                new ResumoStatusDTO(0, BigDecimal.ZERO),
                new ResumoStatusDTO(0, BigDecimal.ZERO));

        String json = mapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("mesAtual");
    }
}
