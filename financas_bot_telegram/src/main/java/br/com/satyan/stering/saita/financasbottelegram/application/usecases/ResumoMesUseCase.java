package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import java.time.YearMonth;

public interface ResumoMesUseCase {

    ResumoMesDTO obter(Long requisitanteId, YearMonth mes, String busca);
}
