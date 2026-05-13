package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

public class AuthTokenInvalidoException extends RuntimeException {

    public AuthTokenInvalidoException(String message) {
        super(message);
    }
}
