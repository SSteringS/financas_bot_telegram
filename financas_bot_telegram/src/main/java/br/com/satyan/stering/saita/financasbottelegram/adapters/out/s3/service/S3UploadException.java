package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service;

/**
 * Exception lançada quando há falha ao fazer upload de imagem no S3.
 */
public class S3UploadException extends RuntimeException {

  public S3UploadException(String message) {
    super(message);
  }

  public S3UploadException(String message, Throwable cause) {
    super(message, cause);
  }
}

