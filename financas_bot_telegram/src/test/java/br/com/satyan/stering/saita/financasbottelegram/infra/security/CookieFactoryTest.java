package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class CookieFactoryTest {

    private final CookieFactory factory = new CookieFactory(180, false, "");

    @Test
    void deveCriarCookieHttpOnlyComJwt() {
        ResponseCookie cookie = factory.criar("meu.jwt.token");
        assertThat(cookie.getName()).isEqualTo("finbot_session");
        assertThat(cookie.getValue()).isEqualTo("meu.jwt.token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void deveCriarCookieComMaxAgeDe180Dias() {
        ResponseCookie cookie = factory.criar("jwt");
        long esperado = 180L * 24 * 60 * 60;
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(esperado);
    }

    @Test
    void deveCriarCookieExpiradoComMaxAgeZero() {
        ResponseCookie cookie = factory.criarExpirado();
        assertThat(cookie.getName()).isEqualTo("finbot_session");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }

    @Test
    void deveAdicionarDomainQuandoConfigurado() {
        CookieFactory comDomain = new CookieFactory(180, true, "finbot.satyan.com.br");
        ResponseCookie cookie = comDomain.criar("jwt");
        assertThat(cookie.getDomain()).isEqualTo("finbot.satyan.com.br");
        assertThat(cookie.isSecure()).isTrue();
    }
}
