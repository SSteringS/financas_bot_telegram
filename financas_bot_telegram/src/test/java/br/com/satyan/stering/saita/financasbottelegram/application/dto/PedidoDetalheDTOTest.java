package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PedidoDetalheDTOTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void deveSerializarTodosCampos() throws Exception {
        PedidoDetalheDTO dto = new PedidoDetalheDTO(
                10L,
                new BigDecimal("500.00"),
                "Conta de água",
                TipoPagamento.TED,
                StatusPedido.PENDENTE,
                LocalDate.of(2026, 5, 1),
                null,
                false
        );

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":10");
        assertThat(json).contains("\"valor\":500.00");
        assertThat(json).contains("\"descricao\":\"Conta de água\"");
        assertThat(json).contains("\"tipo\":\"TED\"");
        assertThat(json).contains("\"status\":\"PENDENTE\"");
        assertThat(json).contains("\"dataPedido\":\"2026-05-01\"");
        assertThat(json).contains("\"dataPagamento\":null");
        assertThat(json).contains("\"temComprovante\":false");
    }
}
