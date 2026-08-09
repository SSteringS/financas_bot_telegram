package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AdiantamentoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdiantamentoJpaRepository extends JpaRepository<AdiantamentoEntity, Long> {

    List<AdiantamentoEntity> findByFuncionarioIdAndAtivoTrue(Long funcionarioId);
}
