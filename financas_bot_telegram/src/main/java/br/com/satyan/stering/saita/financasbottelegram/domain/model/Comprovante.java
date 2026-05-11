package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comprovante {
    private Long id;
    private Long pedidoId;
    private String fileIdTelegram;
    private String imagemUrl;
    private String tipoPagamento;
    private LocalDateTime dataPagamento;
}
