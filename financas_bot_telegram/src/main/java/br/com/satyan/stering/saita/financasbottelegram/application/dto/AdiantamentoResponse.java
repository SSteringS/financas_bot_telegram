package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
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
public class AdiantamentoResponse {

    private Long id;
    private Long funcionarioId;
    private String descricao;
    private BigDecimal valorTotal;
    private BigDecimal valorParcela;
    private Integer numParcelas;
    private Integer parcelasPagas;
    private Integer parcelasRestantes;
    private LocalDate dataInicio;
    private Boolean ativo;
    private LocalDateTime criadoEm;

    public static AdiantamentoResponse from(Adiantamento a) {
        int restantes = (a.getNumParcelas() != null && a.getParcelasPagas() != null)
                ? a.getNumParcelas() - a.getParcelasPagas()
                : 0;
        return AdiantamentoResponse.builder()
                .id(a.getId())
                .funcionarioId(a.getFuncionarioId())
                .descricao(a.getDescricao())
                .valorTotal(a.getValorTotal())
                .valorParcela(a.getValorParcela())
                .numParcelas(a.getNumParcelas())
                .parcelasPagas(a.getParcelasPagas())
                .parcelasRestantes(restantes)
                .dataInicio(a.getDataInicio())
                .ativo(a.getAtivo())
                .criadoEm(a.getCriadoEm())
                .build();
    }
}
