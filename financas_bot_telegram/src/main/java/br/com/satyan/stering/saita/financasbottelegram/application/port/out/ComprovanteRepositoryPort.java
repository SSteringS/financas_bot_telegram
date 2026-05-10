package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Comprovante;

public interface ComprovanteRepositoryPort {
    Comprovante save(Comprovante comprovante);
}

