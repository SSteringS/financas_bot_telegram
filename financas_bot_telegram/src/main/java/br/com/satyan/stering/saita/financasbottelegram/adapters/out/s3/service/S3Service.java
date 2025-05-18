package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.S3PortOut;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service implements S3PortOut {

    private final S3Client s3Client;
    private static final String BUCKET_NAME = "bot-financas-pagamentos-satyan";

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadPhoto(String fileName, byte[] content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(fileName)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

}
