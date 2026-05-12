package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.AuthExchangeRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.AuthMeResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ExchangeTokenUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.CookieFactory;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private ExchangeTokenUseCase exchangeUseCase;
    @Mock private JwtService jwtService;
    @Mock private CookieFactory cookieFactory;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(exchangeUseCase, jwtService, cookieFactory);
    }

    @Test
    void deveRetornar200ComCookieEBodyQuandoTokenValido() {
        String tokenPlain = "token-valido-de-teste-aqui12345";
        Requisitante req = Requisitante.builder().id(1L).nome("Satyan").build();
        ResponseCookie cookie = ResponseCookie.from("finbot_session", "jwt.token.aqui")
                .httpOnly(true).path("/").build();

        when(exchangeUseCase.exchange(tokenPlain)).thenReturn(req);
        when(jwtService.gerar(1L)).thenReturn("jwt.token.aqui");
        when(cookieFactory.criar("jwt.token.aqui")).thenReturn(cookie);

        ResponseEntity<AuthMeResponse> response = controller.exchange(new AuthExchangeRequest(tokenPlain));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requisitante().id()).isEqualTo(1L);
        assertThat(response.getBody().requisitante().nome()).isEqualTo("Satyan");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("finbot_session");
    }
}
