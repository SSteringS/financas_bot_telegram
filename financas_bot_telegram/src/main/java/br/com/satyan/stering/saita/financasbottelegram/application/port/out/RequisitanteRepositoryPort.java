package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import java.util.Optional;

public interface RequisitanteRepositoryPort {
    Optional<Requisitante> findById(Long id);
    boolean existsById(Long id);
}
