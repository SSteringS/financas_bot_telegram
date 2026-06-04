package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response para endpoints de fechamento mensal da folha.
 *
 * <p>Representa um Pedido FOLHA com os campos relevantes para o front-end:
 * valor líquido calculado, observação de breakdown e mês de referência.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoFolhaResponse {

    private Long id;
    private Long funcionarioId;
    private BigDecimal valor;
    private StatusPedido status;
    private String observacao;
    private LocalDate mesReferencia;
    private LocalDateTime dataCriacao;

    public static PedidoFolhaResponse from(PedidoPagamento p) {
        return PedidoFolhaResponse.builder()
                .id(p.getId())
                .funcionarioId(p.getFuncionarioId())
                .valor(p.getValor())
                .status(p.getStatus())
                .observacao(p.getObservacao())
                .mesReferencia(p.getMesReferencia())
                .dataCriacao(p.getDataCriacao())
                .build();
    }
}
