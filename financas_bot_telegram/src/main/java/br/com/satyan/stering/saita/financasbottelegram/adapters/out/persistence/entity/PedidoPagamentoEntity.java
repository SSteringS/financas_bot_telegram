package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.CategoriaPedido;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "pedidos_pagamento")
public class PedidoPagamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * FK para {@code requisitante.id}. Null para pedidos sistema (categoria=FOLHA),
     * obrigatório para pedidos do Telegram.
     */
    @Column(name = "requisitante_id")
    private Long requisitanteId;

    @Column(name = "telegram_user_id")
    private String telegramUserId;

    @Column(name = "telegram_message_id")
    private String telegramMessageId;

    @Column(name = "file_id_telegram")
    private String fileIdTelegram;

    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoPagamento tipo;

    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    // ── Campos V6: folha de pagamento (STI) ──────────────────────────────────

    /** Categoria do pedido no contexto da folha — null para pedidos normais do Telegram. */
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria")
    private CategoriaPedido categoria;

    /** FK para {@code funcionario.id} — obrigatório quando categoria não é null. */
    @Column(name = "funcionario_id")
    private Long funcionarioId;

    /**
     * Indica se o vale já foi considerado no fechamento do mês.
     * {@code false} para vales abertos; {@code true} após o {@code FecharMesUseCase}.
     */
    @Column(name = "fechado", nullable = false)
    private Boolean fechado = false;

    /** Breakdown legível do cálculo da folha (preenchido apenas para categoria=FOLHA). */
    @Column(name = "observacao", columnDefinition = "VARCHAR(1000)")
    private String observacao;

    /**
     * Mês de referência do fechamento (primeiro dia do mês — ex: 2026-05-01).
     * Obrigatório para categoria=FOLHA; null para VALE e pedidos normais.
     */
    @Column(name = "mes_referencia")
    private LocalDate mesReferencia;
}
