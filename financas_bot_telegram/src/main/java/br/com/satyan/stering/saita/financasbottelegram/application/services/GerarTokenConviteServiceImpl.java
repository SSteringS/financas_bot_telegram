package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AuthTokenRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.HashService;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.GerarTokenConviteUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.RequisitanteNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.AuthToken;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GerarTokenConviteServiceImpl implements GerarTokenConviteUseCase {

    private static final Duration TTL = Duration.ofDays(7);
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final RequisitanteRepositoryPort requisitanteRepo;
    private final AuthTokenRepositoryPort tokenRepo;
    private final HashService hashService;
    private final String frontendBaseUrl;

    public GerarTokenConviteServiceImpl(
            RequisitanteRepositoryPort requisitanteRepo,
            AuthTokenRepositoryPort tokenRepo,
            HashService hashService,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.requisitanteRepo = requisitanteRepo;
        this.tokenRepo = tokenRepo;
        this.hashService = hashService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public String gerar(Long requisitanteId) {
        if (!requisitanteRepo.existsById(requisitanteId)) {
            throw new RequisitanteNaoEncontradoException(requisitanteId);
        }

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        random.nextBytes(tokenBytes);
        String tokenPlain = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = hashService.hash(tokenPlain);

        LocalDateTime agora = LocalDateTime.now();
        tokenRepo.save(AuthToken.builder()
                .tokenHash(tokenHash)
                .requisitanteId(requisitanteId)
                .criadoEm(agora)
                .expiraEm(agora.plus(TTL))
                .build());

        return frontendBaseUrl + "/entrar?t=" + tokenPlain;
    }
}
