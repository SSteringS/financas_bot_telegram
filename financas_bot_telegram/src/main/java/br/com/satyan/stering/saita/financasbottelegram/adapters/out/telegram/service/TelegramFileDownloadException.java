package br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service;

/**
 * Exception lançada quando há falha ao fazer download de arquivo do Telegram.
 */
public class TelegramFileDownloadException extends RuntimeException {

  public TelegramFileDownloadException(String message) {
    super(message);
  }

  public TelegramFileDownloadException(String message, Throwable cause) {
    super(message, cause);
  }
}

