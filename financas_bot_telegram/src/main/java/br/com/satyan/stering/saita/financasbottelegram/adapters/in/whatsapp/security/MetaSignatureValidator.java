package br.com.satyan.stering.saita.financasbottelegram.adapters.in.whatsapp.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MetaSignatureValidator {

    private static final Logger logger = LoggerFactory.getLogger(MetaSignatureValidator.class);
    private static final String SHA256_PREFIX = "sha256=";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] appSecretBytes;

    public MetaSignatureValidator(@Value("${whatsapp.app-secret}") String appSecret) {
        this.appSecretBytes = appSecret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(byte[] rawBody, String signatureHeader) {
        if (rawBody == null || signatureHeader == null || !signatureHeader.startsWith(SHA256_PREFIX)) {
            logger.warn("Assinatura ausente, sem prefixo 'sha256=', ou corpo nulo");
            return false;
        }
        String expectedHex = signatureHeader.substring(SHA256_PREFIX.length());
        try {
            SecretKeySpec keySpec = new SecretKeySpec(appSecretBytes, HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] computed = mac.doFinal(rawBody);
            String computedHex = bytesToHex(computed);
            return MessageDigest.isEqual(
                computedHex.getBytes(StandardCharsets.UTF_8),
                expectedHex.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Falha ao calcular HMAC-SHA256: {}", e.getMessage());
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
