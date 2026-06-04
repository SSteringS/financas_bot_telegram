package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import br.com.satyan.stering.saita.financasbottelegram.domain.vo.FormaPagamento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "funcionario")
public class FuncionarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "salario_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false)
    private FormaPagamento formaPagamento;

    @Column(name = "chave_pix", length = 255)
    private String chavePix;

    @Column(name = "banco", length = 50)
    private String banco;

    @Column(name = "agencia", length = 20)
    private String agencia;

    @Column(name = "conta", length = 30)
    private String conta;

    /** CORRENTE ou POUPANCA — mapeado como String para evitar enum adicional no domínio JPA. */
    @Column(name = "tipo_conta")
    private String tipoConta;

    @Column(name = "conta_propria", nullable = false)
    private Boolean contaPropria = true;

    @Column(name = "obs_pagamento", length = 500)
    private String obsPagamento;

    @Column(name = "dia_pagamento_referencia")
    private Integer diaPagamentoReferencia;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false, nullable = false)
    private LocalDateTime criadoEm;

    /**
     * Atualizado automaticamente pelo banco ({@code ON UPDATE CURRENT_TIMESTAMP}).
     * Importante para rastreabilidade de alterações de salário.
     */
    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
