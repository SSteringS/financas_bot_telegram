package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.CategoriaPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValeResponse {

    private Long id;
    private Long funcionarioId;
    private CategoriaPedido categoria;
    private String descricao;
    private BigDecimal valor;
    private StatusPedido status;
    private Boolean fechado;
    private LocalDate dataPedido;
    private LocalDateTime dataCriacao;

    public static ValeResponse from(PedidoPagamento p) {
        return ValeResponse.builder()
                .id(p.getId())
                .funcionarioId(p.getFuncionarioId())
                .categoria(p.getCategoria())
                .descricao(p.getDescricao())
                .valor(p.getValor())
                .status(p.getStatus())
                .fechado(p.getFechado())
                .dataPedido(p.getDataPedido())
                .dataCriacao(p.getDataCriacao())
                .build();
    }
}
