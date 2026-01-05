package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Comprovante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprovanteRepository extends JpaRepository<Comprovante, Long> {
}

