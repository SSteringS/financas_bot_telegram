package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import java.time.Duration;

public interface StorageService {

    String uploadImage(byte[] bytes);

    String gerarUrlTemporariaParaLeitura(String s3Key, Duration ttl);
}
