package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.FormaPagamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuncionarioResponse {

    private Long id;
    private String nome;
    private BigDecimal salarioBase;
    private FormaPagamento formaPagamento;
    private String chavePix;
    private String banco;
    private String agencia;
    private String conta;
    private String tipoConta;
    private Boolean contaPropria;
    private String obsPagamento;
    private Integer diaPagamentoReferencia;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static FuncionarioResponse from(Funcionario f) {
        return FuncionarioResponse.builder()
                .id(f.getId())
                .nome(f.getNome())
                .salarioBase(f.getSalarioBase())
                .formaPagamento(f.getFormaPagamento())
                .chavePix(f.getChavePix())
                .banco(f.getBanco())
                .agencia(f.getAgencia())
                .conta(f.getConta())
                .tipoConta(f.getTipoConta())
                .contaPropria(f.getContaPropria())
                .obsPagamento(f.getObsPagamento())
                .diaPagamentoReferencia(f.getDiaPagamentoReferencia())
                .ativo(f.getAtivo())
                .criadoEm(f.getCriadoEm())
                .atualizadoEm(f.getAtualizadoEm())
                .build();
    }
}
