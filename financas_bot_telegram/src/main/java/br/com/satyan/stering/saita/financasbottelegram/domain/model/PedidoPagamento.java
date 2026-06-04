package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.CategoriaPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoPagamento {
    private Long id;
    private Long requisitanteId;
    private String telegramUserId;
    private String telegramMessageId;
    private String fileIdTelegram;
    private String imagemUrl;
    private BigDecimal valor;
    private String descricao;
    private StatusPedido status;
    private TipoPagamento tipo;
    private LocalDate dataPedido;
    private LocalDate dataPagamento;
    private LocalDateTime dataCriacao;

    // ── Campos V6: folha de pagamento (STI) ──────────────────────────────────

    /** Categoria do pedido — null para pedidos normais do Telegram. */
    private CategoriaPedido categoria;

    /** ID do funcionário doméstico — obrigatório quando categoria não é null. */
    private Long funcionarioId;

    /** Vale marcado como fechado após o FecharMesUseCase. */
    private Boolean fechado;

    /** Breakdown do cálculo da folha (preenchido para categoria=FOLHA). */
    private String observacao;

    /** Primeiro dia do mês de referência — obrigatório para categoria=FOLHA. */
    private LocalDate mesReferencia;
}
