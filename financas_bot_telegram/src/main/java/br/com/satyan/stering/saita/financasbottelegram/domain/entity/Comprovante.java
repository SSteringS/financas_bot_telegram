package br.com.satyan.stering.saita.financasbottelegram.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "comprovantes")
public class Comprovante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoPagamento pedido;

    @Column(name = "file_id_telegram")
    private String fileIdTelegram;

    @Column(name = "tipo_pagamento")
    private String tipoPagamento;

    @CreationTimestamp
    @Column(name = "data_pagamento", updatable = false)
    private LocalDateTime dataPagamento;
}

