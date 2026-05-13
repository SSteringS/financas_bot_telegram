package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Requisitante {
    private Long id;
    private String nome;
    private String telefone;
    private String email;
    private boolean ativo;
    private LocalDateTime criadoEm;
}
