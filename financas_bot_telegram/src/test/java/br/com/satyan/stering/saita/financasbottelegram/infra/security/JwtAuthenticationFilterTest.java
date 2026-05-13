package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private CookieFactory cookieFactory;
    @Mock private FilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, cookieFactory);
    }

    private MockHttpServletRequest reqComCookie(String valor) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("finbot_session", valor));
        return req;
    }

    @Test
    void deveProsseguirESalvarRequisitanteIdQuandoJwtValido() throws Exception {
        MockHttpServletRequest req = reqComCookie("jwt-valido");
        req.setRequestURI("/api/v1/pedidos");
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtService.validarERetornarRequisitanteId("jwt-valido")).thenReturn(1L);
        when(jwtService.precisaRenovar("jwt-valido")).thenReturn(false);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(req.getAttribute("requisitanteId")).isEqualTo(1L);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void deveRetornar401QuandoCookieAusente() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/pedidos");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("SESSAO_AUSENTE");
    }

    @Test
    void deveRetornar401QuandoJwtInvalido() throws Exception {
        MockHttpServletRequest req = reqComCookie("jwt-invalido");
        req.setRequestURI("/api/v1/pedidos");
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtService.validarERetornarRequisitanteId("jwt-invalido"))
                .thenThrow(new JwtException("assinatura inválida"));

        filter.doFilter(req, res, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("SESSAO_INVALIDA");
    }

    @Test
    void naoDeveAplicarFiltroEmExchange() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/auth/exchange");
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void naoDeveAplicarFiltroEmWebhook() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/webhook");

        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void naoDeveAplicarFiltroEmAdmin() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/api/v1/requisitantes/1/convite");

        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void deveAplicarFiltroEmApiV1Pedidos() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/pedidos");

        assertThat(filter.shouldNotFilter(req)).isFalse();
    }

    @Test
    void deveRenovarCookieQuandoJwtEstaVencendo() throws Exception {
        MockHttpServletRequest req = reqComCookie("jwt-velho");
        req.setRequestURI("/api/v1/auth/me");
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtService.validarERetornarRequisitanteId("jwt-velho")).thenReturn(1L);
        when(jwtService.precisaRenovar("jwt-velho")).thenReturn(true);
        when(jwtService.gerar(1L)).thenReturn("jwt-novo");
        org.springframework.http.ResponseCookie novoCookie =
                org.springframework.http.ResponseCookie.from("finbot_session", "jwt-novo")
                        .httpOnly(true).path("/").build();
        when(cookieFactory.criar("jwt-novo")).thenReturn(novoCookie);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(res.getHeader(HttpHeaders.SET_COOKIE)).contains("finbot_session");
    }
}
