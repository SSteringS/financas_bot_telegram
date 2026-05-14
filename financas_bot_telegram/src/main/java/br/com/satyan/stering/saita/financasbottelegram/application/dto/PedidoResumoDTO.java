package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Resumo de um pedido pra listagem")
public record PedidoResumoDTO(
        @Schema(description = "ID único do pedido", example = "142") Long id,
        @Schema(description = "Valor em reais", example = "287.50") BigDecimal valor,
        @Schema(description = "Descrição livre do pedido", example = "Boleto energia") String descricao,
        @Schema(description = "Tipo de pagamento") TipoPagamento tipo,
        @Schema(description = "Status atual do pedido") StatusPedido status,
        @Schema(description = "Data em que o pedido foi solicitado", example = "2026-05-03") LocalDate dataPedido,
        @Schema(description = "Data em que o pagamento foi efetuado (null se PENDENTE)", example = "2026-05-04", nullable = true) LocalDate dataPagamento,
        @Schema(description = "Existe um comprovante anexado a este pedido?", example = "true") boolean temComprovante
) {}
