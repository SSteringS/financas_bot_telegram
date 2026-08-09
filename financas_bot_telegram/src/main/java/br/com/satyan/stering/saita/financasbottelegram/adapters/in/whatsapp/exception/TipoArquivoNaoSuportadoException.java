package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.exception;

public class TipoArquivoNaoSuportadoException extends RuntimeException {
    private final String waId;

    public TipoArquivoNaoSuportadoException(String message, String waId) {
        super(message);
        this.waId = waId;
    }

    public String getWaId() {
        return waId;
    }
}
