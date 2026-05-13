package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AuthTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenJpaRepository extends JpaRepository<AuthTokenEntity, String> {

    Optional<AuthTokenEntity> findByTokenHash(String tokenHash);
}
