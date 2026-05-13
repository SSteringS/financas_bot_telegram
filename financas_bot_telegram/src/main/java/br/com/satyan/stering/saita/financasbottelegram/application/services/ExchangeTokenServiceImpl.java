package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AuthTokenRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.HashService;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ExchangeTokenUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.AuthTokenInvalidoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeTokenServiceImpl implements ExchangeTokenUseCase {

    private final AuthTokenRepositoryPort tokenRepo;
    private final RequisitanteRepositoryPort requisitanteRepo;
    private final HashService hashService;

    public ExchangeTokenServiceImpl(
            AuthTokenRepositoryPort tokenRepo,
            RequisitanteRepositoryPort requisitanteRepo,
            HashService hashService) {
        this.tokenRepo = tokenRepo;
        this.requisitanteRepo = requisitanteRepo;
        this.hashService = hashService;
    }

    @Override
    @Transactional
    public Requisitante exchange(String tokenPlain) {
        String tokenHash = hashService.hash(tokenPlain);
        AuthToken token = tokenRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthTokenInvalidoException("token não encontrado"));

        LocalDateTime agora = LocalDateTime.now();
        if (token.estaExpirado(agora)) {
            throw new AuthTokenInvalidoException("token expirado");
        }
        if (token.foiUsado()) {
            throw new AuthTokenInvalidoException("token já foi usado");
        }

        token.setUsadoEm(agora);
        tokenRepo.save(token);

        return requisitanteRepo.findById(token.getRequisitanteId())
                .orElseThrow(() -> new IllegalStateException(
                        "Requisitante referenciado pelo token não existe"));
    }
}
