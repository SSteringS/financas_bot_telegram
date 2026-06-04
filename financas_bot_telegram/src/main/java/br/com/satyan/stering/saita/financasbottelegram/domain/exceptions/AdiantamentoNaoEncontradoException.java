package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

public class AdiantamentoNaoEncontradoException extends RuntimeException {

    public AdiantamentoNaoEncontradoException(Long id) {
        super("Adiantamento não encontrado: id=" + id);
    }
}
