package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;

public interface IdempotenciaMensagemPort {

    /**
     * Tenta reservar (claim) o processamento de uma mensagem.
     * Idempotente: chamadas com a mesma (canal, idExterno) só conseguem claim uma vez.
     * @return true se este chamador fez o claim; false se já estava claimado.
     */
    boolean tentarClaim(Canal canal, String idExterno);
}
