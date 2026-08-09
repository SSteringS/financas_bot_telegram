package br.com.satyan.stering.saita.financasbottelegram.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plano de desconto parcelado concedido a um funcionário — entidade de domínio (POJO puro).
 *
 * <p>Um adiantamento é automaticamente quitado quando {@code parcelasPagas == numParcelas}.
 * Pode ser cancelado manualmente (soft: {@code ativo=false}) antes da quitação.
 *
 * <p>Invariante matemática (verificada também no banco via CHECK constraint):
 * {@code |valorTotal - valorParcela * numParcelas| <= 0.01}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Adiantamento {

    private Long id;

    private Long funcionarioId;

    private String descricao;

    /** Valor total do adiantamento. */
    private BigDecimal valorTotal;

    /** Valor descontado por mês no fechamento. */
    private BigDecimal valorParcela;

    /** Número total de parcelas planejadas. */
    private Integer numParcelas;

    /** Parcelas já descontadas em fechamentos anteriores. Incrementado pelo FecharMesUseCase. */
    private Integer parcelasPagas;

    /** Data de início do plano (primeiro desconto previsto). */
    private LocalDate dataInicio;

    /**
     * {@code true} enquanto ainda há parcelas a pagar ou o adiantamento não foi cancelado.
     * Setado para {@code false} quando {@code parcelasPagas == numParcelas} (quitado)
     * ou quando cancelado manualmente.
     */
    private Boolean ativo;

    private LocalDateTime criadoEm;
}
