package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComprovanteJpaRepository extends JpaRepository<ComprovanteEntity, Long> {
}
