package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ListarPedidosFiltro;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaginaDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoResumoDTO;

public interface ListarPedidosUseCase {
    PaginaDTO<PedidoResumoDTO> listar(ListarPedidosFiltro filtro, Long requisitanteId);
}
