package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import java.time.LocalDate;
import java.util.List;

public record ListarPedidosFiltro(
        StatusPedido status,
        List<TipoPagamento> tipos,
        LocalDate de,
        LocalDate ate,
        String busca,
        int page,
        int tamanho
) {
    public static int TAMANHO_DEFAULT = 20;
    public static int TAMANHO_MAX = 50;
}
