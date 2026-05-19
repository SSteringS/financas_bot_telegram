package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;

public interface ResumoMesUseCase {

    ResumoMesDTO obter(Long requisitanteId);
}
