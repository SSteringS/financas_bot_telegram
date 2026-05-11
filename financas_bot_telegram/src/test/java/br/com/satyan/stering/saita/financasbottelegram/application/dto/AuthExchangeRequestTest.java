package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthExchangeRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deveAceitarTokenValido() {
        AuthExchangeRequest req = new AuthExchangeRequest("ABCdef1234567890");
        Set<ConstraintViolation<AuthExchangeRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    void deveRejeitarTokenEmBranco() {
        AuthExchangeRequest req = new AuthExchangeRequest("");
        Set<ConstraintViolation<AuthExchangeRequest>> violations = validator.validate(req);
        assertThat(violations).hasSize(2); // @NotBlank + @Size(min=16)
    }

    @Test
    void deveRejeitarTokenCurto() {
        AuthExchangeRequest req = new AuthExchangeRequest("curto");
        Set<ConstraintViolation<AuthExchangeRequest>> violations = validator.validate(req);
        assertThat(violations).hasSize(1); // só @Size(min=16)
    }

    @Test
    void deveRejeitarTokenMuitoLongo() {
        AuthExchangeRequest req = new AuthExchangeRequest("x".repeat(129));
        Set<ConstraintViolation<AuthExchangeRequest>> violations = validator.validate(req);
        assertThat(violations).hasSize(1); // @Size(max=128)
    }
}
