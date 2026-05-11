package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import java.math.BigDecimal;
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
    private String telegramUserId;
    private String telegramMessageId;
    private String fileIdTelegram;
    private String imagemUrl;
    private BigDecimal valor;
    private String descricao;
    private StatusPedido status;
    private LocalDateTime dataCriacao;
}
