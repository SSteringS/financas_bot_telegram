package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoUploadS3;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class S3ImageUploadService {

    private static final Logger logger = LoggerFactory.getLogger(S3ImageUploadService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final S3Template s3Template;
    private final String bucketName;
    private final String baseUrl;

    public S3ImageUploadService(S3Template s3Template,
            @Value("${aws.s3.bucket-name:bot-financas-pagamentos-satyan}") String bucketName,
            @Value("${financasbot.s3.base-url:https://s3.amazonaws.com/bot-financas-pagamentos-satyan/}") String baseUrl) {
        this.s3Template = s3Template;
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
    }

    public String uploadImage(byte[] imageBytes) {
        return uploadFile(imageBytes, "jpg", TipoUploadS3.PEDIDO);
    }

    public String uploadFile(byte[] bytes, String extension, TipoUploadS3 tipoUpload) {
        try {
            String key = construirChaveS3(extension, tipoUpload);
            logger.info("Iniciando upload para S3 com chave: {}", key);
            s3Template.upload(bucketName, key, new ByteArrayInputStream(bytes));
            String url = buildImageUrl(key);
            logger.info("Arquivo enviado para S3. URL: {}", url);
            return url;
        } catch (Exception e) {
            logger.error("Erro ao fazer upload do arquivo para S3", e);
            throw new S3UploadException("Falha ao salvar arquivo no S3: " + e.getMessage(), e);
        }
    }

    private String construirChaveS3(String extension, TipoUploadS3 tipoUpload) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return String.format("%s/%s/%s.%s", tipoUpload.getFolder(), today, UUID.randomUUID(), extension);
    }

    private String buildImageUrl(String key) {
        return baseUrl + key;
    }
}
