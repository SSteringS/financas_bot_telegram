package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthToken {
    private String tokenHash;
    private Long requisitanteId;
    private LocalDateTime criadoEm;
    private LocalDateTime expiraEm;
    private LocalDateTime usadoEm;

    public boolean estaExpirado(LocalDateTime agora) {
        return agora.isAfter(expiraEm);
    }

    public boolean foiUsado() {
        return usadoEm != null;
    }
}
