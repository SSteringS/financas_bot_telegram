package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

public class PedidoNaoAutorizadoException extends RuntimeException {

    public PedidoNaoAutorizadoException(Long pedidoId, Long requisitanteId) {
        super("Pedido " + pedidoId + " não pertence ao requisitante " + requisitanteId);
    }
}
