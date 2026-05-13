package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-at-least-32-chars-ok!";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 180);
    }

    @Test
    void deveGerarJwtComSubjectCorreto() {
        String jwt = jwtService.gerar(1L);
        Long id = jwtService.validarERetornarRequisitanteId(jwt);
        assertThat(id).isEqualTo(1L);
    }

    @Test
    void deveGerarJwtComExpiracaoCorreta() {
        Instant antes = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String jwt = jwtService.gerar(42L);
        // valida sem lancar excecao (token ainda valido)
        Long id = jwtService.validarERetornarRequisitanteId(jwt);
        assertThat(id).isEqualTo(42L);
    }

    @Test
    void deveLancarExcecaoParaJwtComSecretDiferente() {
        String jwt = jwtService.gerar(1L);
        JwtService outroService = new JwtService("outro-secret-diferente-32-chars!!", 180);

        assertThatThrownBy(() -> outroService.validarERetornarRequisitanteId(jwt))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveLancarExcecaoParaJwtMalformado() {
        assertThatThrownBy(() -> jwtService.validarERetornarRequisitanteId("nao-e-um-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveLancarExcecaoSeSecretMenorQue32Chars() {
        assertThatThrownBy(() -> new JwtService("curto", 180))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void deveRetornarFalsePrecisaRenovarParaTokenNovo() {
        String jwt = jwtService.gerar(1L);
        assertThat(jwtService.precisaRenovar(jwt)).isFalse();
    }
}
