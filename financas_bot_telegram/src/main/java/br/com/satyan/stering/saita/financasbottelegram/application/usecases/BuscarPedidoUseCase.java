package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoDetalheDTO;

public interface BuscarPedidoUseCase {

    PedidoDetalheDTO buscar(Long pedidoId, Long requisitanteId);
}
