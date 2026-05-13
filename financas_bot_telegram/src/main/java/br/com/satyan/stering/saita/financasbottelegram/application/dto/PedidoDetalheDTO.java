package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Detalhes completos de um pedido")
public record PedidoDetalheDTO(
        @Schema(example = "142") Long id,
        @Schema(example = "287.50") BigDecimal valor,
        @Schema(example = "Boleto energia") String descricao,
        TipoPagamento tipo,
        StatusPedido status,
        @Schema(example = "2026-05-03") LocalDate dataPedido,
        @Schema(example = "2026-05-04", nullable = true) LocalDate dataPagamento,
        @Schema(description = "Existe comprovante?") boolean temComprovante
) {}
