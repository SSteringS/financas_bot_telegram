package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.FuncionarioEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FuncionarioJpaRepository extends JpaRepository<FuncionarioEntity, Long> {

    Optional<FuncionarioEntity> findByIdAndAtivoTrue(Long id);

    List<FuncionarioEntity> findAllByAtivoTrue();
}
