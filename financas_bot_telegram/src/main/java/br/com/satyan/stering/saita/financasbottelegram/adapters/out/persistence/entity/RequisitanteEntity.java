package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "requisitante")
public class RequisitanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "email")
    private String email;

    @Column(name = "ativo")
    private boolean ativo;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
}
