package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.CanalMensagem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "mensagem_processada",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_msg_canal_idexterno",
        columnNames = {"canal", "id_externo"}
    )
)
public class MensagemProcessadaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalMensagem canal;

    @Column(name = "id_externo", nullable = false)
    private String idExterno;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;

    public MensagemProcessadaEntity(CanalMensagem canal, String idExterno) {
        this.canal = canal;
        this.idExterno = idExterno;
        this.processadoEm = LocalDateTime.now();
    }
}
