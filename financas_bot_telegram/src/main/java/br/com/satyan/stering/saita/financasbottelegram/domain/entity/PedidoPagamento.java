package br.com.satyan.stering.saita.financasbottelegram.domain.entity;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "pedidos_pagamento")
public class PedidoPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

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

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;
}
