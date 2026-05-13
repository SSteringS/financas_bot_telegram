package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.AuthExchangeRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.AuthMeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void fluxoCompleto_geraConvite_exchange_meRetornaRequisitante() {
        String urlConvite = gerarConviteUseCase.gerar(1L);
        String token = urlConvite.substring(urlConvite.indexOf("?t=") + 3);

        ResponseEntity<String> exchangeResp = restTemplate.postForEntity(
                "/api/v1/auth/exchange", new AuthExchangeRequest(token), String.class);

        assertThat(exchangeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String cookie = exchangeResp.getHeaders().getFirst("Set-Cookie");
        assertThat(cookie).contains("finbot_session=").contains("HttpOnly");

        String cookieValue = cookie.split(";")[0];
        ResponseEntity<AuthMeResponse> meResp = getAutenticado("/api/v1/auth/me", cookieValue, AuthMeResponse.class);

        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResp.getBody().requisitante().id()).isEqualTo(1L);
    }

    @Test
    void exchange_tokenUsadoDuasVezes_retorna401NaSegundaVez() {
        String urlConvite = gerarConviteUseCase.gerar(1L);
        String token = urlConvite.substring(urlConvite.indexOf("?t=") + 3);

        restTemplate.postForEntity("/api/v1/auth/exchange", new AuthExchangeRequest(token), String.class);

        ResponseEntity<String> segundaVez = restTemplate.postForEntity(
                "/api/v1/auth/exchange", new AuthExchangeRequest(token), String.class);

        assertThat(segundaVez.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_semCookie_retorna401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/v1/auth/me", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
