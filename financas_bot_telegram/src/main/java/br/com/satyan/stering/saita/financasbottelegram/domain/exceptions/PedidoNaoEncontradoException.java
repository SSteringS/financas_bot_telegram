package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

public class PedidoNaoEncontradoException extends RuntimeException {

    private final Long chatId;

    public PedidoNaoEncontradoException(String message, Long chatId) {
        super(message);
        this.chatId = chatId;
    }

    public PedidoNaoEncontradoException(Long pedidoId) {
        super("Pedido não encontrado: id=" + pedidoId);
        this.chatId = null;
    }

    public Long getChatId() {
        return chatId;
    }
}

