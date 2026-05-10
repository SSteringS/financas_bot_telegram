package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service;

import io.awspring.cloud.s3.S3Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Serviço responsável por fazer upload de imagens para o bucket S3.
 * Utiliza particionamento de pastas baseado em data: bucket/pedidos/YYYYMMDD/UUID.jpg
 */
@Service
public class S3ImageUploadService {

  private static final Logger logger = LoggerFactory.getLogger(S3ImageUploadService.class);
  private static final String FOLDER_PREFIX = "pedidos";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final String FILE_EXTENSION = ".jpg";

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

  /**
   * Faz upload da imagem para o S3 com particionamento por data.
   *
   * @param imageBytes Array de bytes da imagem
   * @return URL da imagem no S3
   */
  public String uploadImage(byte[] imageBytes) {
    try {
      String key = generateS3Key();
      logger.info("Iniciando upload da imagem para S3 com chave: {}", key);

      InputStream inputStream = new ByteArrayInputStream(imageBytes);
      s3Template.upload(bucketName, key, inputStream);

      String imageUrl = buildImageUrl(key);
      logger.info("Imagem enviada com sucesso para S3. URL: {}", imageUrl);
      return imageUrl;

    } catch (Exception e) {
      logger.error("Erro ao fazer upload da imagem para S3", e);
      throw new S3UploadException("Falha ao salvar imagem no S3: " + e.getMessage(), e);
    }
  }

  /**
   * Gera a chave S3 com particionamento por data: pedidos/YYYYMMDD/UUID.jpg
   */
  private String generateS3Key() {
    String today = LocalDate.now().format(DATE_FORMATTER);
    String fileName = UUID.randomUUID() + FILE_EXTENSION;
    return String.format("%s/%s/%s", FOLDER_PREFIX, today, fileName);
  }

  /**
   * Constrói a URL completa da imagem no S3.
   */
  private String buildImageUrl(String key) {
    return baseUrl + key;
  }
}

