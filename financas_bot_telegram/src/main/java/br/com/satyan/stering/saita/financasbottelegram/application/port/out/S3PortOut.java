package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

public interface S3PortOut {

    void uploadPhoto(String fileName, byte[] content);
}
