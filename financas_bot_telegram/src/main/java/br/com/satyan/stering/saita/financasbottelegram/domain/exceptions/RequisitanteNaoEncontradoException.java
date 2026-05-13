package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

public class RequisitanteNaoEncontradoException extends RuntimeException {

    public RequisitanteNaoEncontradoException(Long id) {
        super("Requisitante não encontrado: id=" + id);
    }
}
