package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.MensagemProcessadaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemProcessadaJpaRepository extends JpaRepository<MensagemProcessadaEntity, Long> {
}
