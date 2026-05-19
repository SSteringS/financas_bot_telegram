package br.com.satyan.stering.saita.financasbottelegram.domain.enums;

public enum TipoUploadS3 {
    PEDIDO("pedidos"),
    COMPROVANTE("comprovantes");

    private final String folder;

    TipoUploadS3(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}
