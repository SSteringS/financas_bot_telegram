package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl-dias}") int ttlDias) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret precisa ter >= 32 chars");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofDays(ttlDias);
    }

    public String gerar(Long requisitanteId) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(requisitanteId))
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Long validarERetornarRequisitanteId(String jwt) {
        Claims claims = parseClaims(jwt);
        return Long.valueOf(claims.getSubject());
    }

    public boolean precisaRenovar(String jwt) {
        Claims c = parseClaims(jwt);
        Instant iat = c.getIssuedAt().toInstant();
        Instant exp = c.getExpiration().toInstant();
        Instant meio = iat.plus(Duration.between(iat, exp).dividedBy(2));
        return Instant.now().isAfter(meio);
    }

    private Claims parseClaims(String jwt) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}
