package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "auth_token")
public class AuthTokenEntity {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisitante_id", nullable = false)
    private RequisitanteEntity requisitante;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;
}
