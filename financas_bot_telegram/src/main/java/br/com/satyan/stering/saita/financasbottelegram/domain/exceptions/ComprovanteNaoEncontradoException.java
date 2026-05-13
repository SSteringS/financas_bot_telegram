package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

public class ComprovanteNaoEncontradoException extends RuntimeException {

    public ComprovanteNaoEncontradoException(Long pedidoId) {
        super("Comprovante não encontrado para pedido id=" + pedidoId);
    }
}
