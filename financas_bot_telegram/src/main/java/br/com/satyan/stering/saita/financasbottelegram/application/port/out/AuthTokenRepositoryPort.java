package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import java.util.Optional;

public interface AuthTokenRepositoryPort {
    AuthToken save(AuthToken token);
    Optional<AuthToken> findByTokenHash(String tokenHash);
}
