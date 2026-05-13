package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

public interface ObterUrlImagemPedidoUseCase {

    String obter(Long pedidoId, Long requisitanteId);
}
