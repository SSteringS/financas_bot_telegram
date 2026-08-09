package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetaSignatureValidatorTest {

    private static final String APP_SECRET = "test-app-secret";
    private MetaSignatureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MetaSignatureValidator(APP_SECRET);
    }

    @Test
    void assinaturaCorreta_retornaTrue() throws Exception {
        byte[] body = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + calcularHmac(APP_SECRET, body);

        assertThat(validator.isValid(body, signature)).isTrue();
    }

    @Test
    void assinaturaModificada_retornaFalse() throws Exception {
        byte[] body = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String signatureCorreta = "sha256=" + calcularHmac(APP_SECRET, body);
        String signatureModificada = signatureCorreta.substring(0, signatureCorreta.length() - 2) + "00";

        assertThat(validator.isValid(body, signatureModificada)).isFalse();
    }

    @Test
    void secretDiferente_retornaFalse() throws Exception {
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + calcularHmac("outro-secret", body);

        assertThat(validator.isValid(body, signature)).isFalse();
    }

    @Test
    void semPrefixoSha256_retornaFalse() throws Exception {
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        String semPrefixo = calcularHmac(APP_SECRET, body);

        assertThat(validator.isValid(body, semPrefixo)).isFalse();
    }

    @Test
    void headerNulo_retornaFalse() {
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        assertThat(validator.isValid(body, null)).isFalse();
    }

    @Test
    void bodyVazio_assinaturaCorreta_retornaTrue() throws Exception {
        byte[] body = new byte[0];
        String signature = "sha256=" + calcularHmac(APP_SECRET, body);

        assertThat(validator.isValid(body, signature)).isTrue();
    }

    @Test
    void bodyNulo_retornaFalse() {
        assertThat(validator.isValid(null, "sha256=qualquer")).isFalse();
    }

    private String calcularHmac(String secret, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] raw = mac.doFinal(data);
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
