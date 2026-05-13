package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256HashServiceTest {

    private final Sha256HashService service = new Sha256HashService();

    @Test
    void deveRetornarHashHexDe64Chars() {
        String hash = service.hash("teste");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void deveSerDeterministico() {
        String hash1 = service.hash("entrada-fixa");
        String hash2 = service.hash("entrada-fixa");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void deveProuzirHashesDiferentesParaEntradasDiferentes() {
        String h1 = service.hash("abc");
        String h2 = service.hash("xyz");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void deveRetornarHashEsperadoParaStringVazia() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String hash = service.hash("");
        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
