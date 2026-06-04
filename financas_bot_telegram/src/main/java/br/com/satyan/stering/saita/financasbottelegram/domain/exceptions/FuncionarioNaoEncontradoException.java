package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

public class FuncionarioNaoEncontradoException extends RuntimeException {

    public FuncionarioNaoEncontradoException(Long id) {
        super("Funcionário não encontrado: id=" + id);
    }

    public FuncionarioNaoEncontradoException(String message) {
        super(message);
    }
}
