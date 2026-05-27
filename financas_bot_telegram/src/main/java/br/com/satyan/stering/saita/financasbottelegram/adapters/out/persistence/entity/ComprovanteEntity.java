package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "comprovantes")
public class ComprovanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoPagamentoEntity pedido;

    @Column(name = "file_id_telegram")
    private String fileIdTelegram;

    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_arquivo", nullable = false)
    private TipoArquivo tipoArquivo;

    @Column(name = "tipo_pagamento")
    private String tipoPagamento;

    @CreationTimestamp
    @Column(name = "data_pagamento", updatable = false)
    private LocalDateTime dataPagamento;
}
