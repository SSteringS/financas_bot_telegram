package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.RequisitanteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequisitanteJpaRepository extends JpaRepository<RequisitanteEntity, Long> {
}
