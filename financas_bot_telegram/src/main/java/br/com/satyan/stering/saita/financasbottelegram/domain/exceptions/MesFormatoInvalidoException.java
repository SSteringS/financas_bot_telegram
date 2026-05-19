package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

public class MesFormatoInvalidoException extends RuntimeException {

    public MesFormatoInvalidoException(String mes) {
        super("Formato de mês inválido: '" + mes + "'. Use YYYY-MM (ex: 2026-05).");
    }
}
