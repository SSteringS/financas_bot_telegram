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

class PedidoResumoDTOTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void deveSerializarParaJsonComCamposCamelCase() throws Exception {
        PedidoResumoDTO dto = new PedidoResumoDTO(
                142L,
                new BigDecimal("287.50"),
                "Boleto energia",
                TipoPagamento.BOLETO,
                StatusPedido.PAGO,
                LocalDate.of(2026, 5, 3),
                LocalDate.of(2026, 5, 4),
                true
        );

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":142");
        assertThat(json).contains("\"valor\":287.50");
        assertThat(json).contains("\"dataPedido\":\"2026-05-03\"");
        assertThat(json).contains("\"dataPagamento\":\"2026-05-04\"");
        assertThat(json).contains("\"tipo\":\"BOLETO\"");
        assertThat(json).contains("\"status\":\"PAGO\"");
        assertThat(json).contains("\"temComprovante\":true");
    }

    @Test
    void deveSerializarComDataPagamentoNulaQuandoPendente() throws Exception {
        PedidoResumoDTO dto = new PedidoResumoDTO(
                1L, new BigDecimal("100.00"), "X", TipoPagamento.PIX,
                StatusPedido.PENDENTE, LocalDate.now(), null, false
        );
        String json = mapper.writeValueAsString(dto);
        assertThat(json).contains("\"dataPagamento\":null");
    }
}
