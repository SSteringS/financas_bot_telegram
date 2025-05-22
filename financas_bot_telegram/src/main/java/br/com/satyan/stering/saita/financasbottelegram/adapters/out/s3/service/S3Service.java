package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.exception.S3ClientException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.S3PortOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service implements S3PortOut {

    private final S3Client s3Client;
    private final static Logger logger = LoggerFactory.getLogger(S3Service.class);
    private static final String BUCKET_NAME = "bot-financas-pagamentos-satyan";

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }


    public void uploadPhoto(String fileName, byte[] content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(fileName)
            .build();

        try {
            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(content));
        } catch (SdkClientException e) {
            logger.error("Erro de SDK ao enviar arquivo para o S3: " + e.getMessage(), e);
            throw new S3ClientException("Erro de SDK ao enviar arquivo para o S3: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Erro genérico ao enviar arquivo para o S3: " + e.getMessage(), e);
            throw new S3ClientException("Erro genérico ao enviar arquivo para o S3:" + e.getMessage(), e);
        }
    }

}
