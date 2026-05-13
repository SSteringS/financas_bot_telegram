package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "finbot_session";
    private static final String ATTR_NAME = "requisitanteId";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CookieFactory cookieFactory;

    public JwtAuthenticationFilter(JwtService jwtService, CookieFactory cookieFactory) {
        this.jwtService = jwtService;
        this.cookieFactory = cookieFactory;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/")
                || path.equals("/api/v1/auth/exchange");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String jwt = extrairCookie(req);
        if (jwt == null) {
            responder401(res, "SESSAO_AUSENTE", "Cookie de sessão ausente");
            return;
        }

        try {
            Long requisitanteId = jwtService.validarERetornarRequisitanteId(jwt);
            req.setAttribute(ATTR_NAME, requisitanteId);

            if (jwtService.precisaRenovar(jwt)) {
                String novoJwt = jwtService.gerar(requisitanteId);
                res.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.criar(novoJwt).toString());
            }
        } catch (JwtException e) {
            log.debug("JWT inválido: {}", e.getMessage());
            responder401(res, "SESSAO_INVALIDA", "Cookie de sessão inválido ou expirado");
            return;
        }

        chain.doFilter(req, res);
    }

    private String extrairCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (COOKIE_NAME.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void responder401(HttpServletResponse res, String codigo, String msg) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(String.format("{\"codigo\":\"%s\",\"mensagem\":\"%s\"}", codigo, msg));
    }
}
