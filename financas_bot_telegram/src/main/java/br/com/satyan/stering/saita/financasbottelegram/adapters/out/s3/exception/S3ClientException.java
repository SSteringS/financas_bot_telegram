package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.exception;

public class S3ClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public S3ClientException(String message) {
        super(message);
    }

    public S3ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public S3ClientException(Throwable cause) {
        super(cause);
    }

}
