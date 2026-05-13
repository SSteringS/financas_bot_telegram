package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieFactory {

    private final int maxAgeSec;
    private final boolean secure;
    private final String domain;

    public CookieFactory(
            @Value("${app.jwt.ttl-dias}") int ttlDias,
            @Value("${app.cookie.secure:false}") boolean secure,
            @Value("${app.cookie.domain:}") String domain) {
        this.maxAgeSec = ttlDias * 24 * 60 * 60;
        this.secure = secure;
        this.domain = domain;
    }

    public ResponseCookie criar(String jwt) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from("finbot_session", jwt)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSec);
        if (!domain.isBlank()) b.domain(domain);
        return b.build();
    }

    public ResponseCookie criarExpirado() {
        return ResponseCookie.from("finbot_session", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .build();
    }
}
