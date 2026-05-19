package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.StorageService;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class S3StorageServiceImpl implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceImpl.class);
    private static final String FOLDER_PREFIX = "pedidos";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String FILE_EXTENSION = ".jpg";

    private final S3Template s3Template;
    private final String bucketName;
    private final String baseUrl;

    public S3StorageServiceImpl(
            S3Template s3Template,
            @Value("${aws.s3.bucket-name:bot-financas-pagamentos-satyan}") String bucketName,
            @Value("${financasbot.s3.base-url:https://s3.amazonaws.com/bot-financas-pagamentos-satyan/}") String baseUrl) {
        this.s3Template = s3Template;
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
    }

    @Override
    public String uploadImage(byte[] imageBytes) {
        try {
            String key = generateS3Key();
            logger.info("Iniciando upload da imagem para S3 com chave: {}", key);
            s3Template.upload(bucketName, key, new ByteArrayInputStream(imageBytes));
            String imageUrl = baseUrl + key;
            logger.info("Imagem enviada com sucesso para S3. URL: {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            logger.error("Erro ao fazer upload da imagem para S3", e);
            throw new S3UploadException("Falha ao salvar imagem no S3: " + e.getMessage(), e);
        }
    }

    @Override
    public String gerarUrlTemporariaParaLeitura(String s3Key, Duration ttl) {
        String chave = normalizarChave(s3Key);
        URL url = s3Template.createSignedGetURL(bucketName, chave, ttl);
        return url.toString();
    }

    private String generateS3Key() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return String.format("%s/%s/%s", FOLDER_PREFIX, today, UUID.randomUUID() + FILE_EXTENSION);
    }

    private String normalizarChave(String urlOuChave) {
        if (urlOuChave.startsWith(baseUrl)) {
            return urlOuChave.substring(baseUrl.length());
        }
        return urlOuChave;
    }
}
