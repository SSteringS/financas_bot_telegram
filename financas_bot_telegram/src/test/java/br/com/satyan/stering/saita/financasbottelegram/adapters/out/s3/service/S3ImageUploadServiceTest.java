package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.s3.S3Template;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3ImageUploadServiceTest {

    @Mock private S3Template s3Template;

    private S3ImageUploadService service;

    private static final String BUCKET = "test-bucket";
    private static final String BASE_URL = "https://s3.example.com/";

    @BeforeEach
    void setUp() {
        service = new S3ImageUploadService(s3Template, BUCKET, BASE_URL);
    }

    @Test
    void uploadFile_deveRetornarUrlComExtensaoJpg() {
        String url = service.uploadFile(new byte[]{1, 2, 3}, "jpg");

        assertThat(url).startsWith(BASE_URL + "pedidos/");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    void uploadFile_deveRetornarUrlComExtensaoPdf() {
        String url = service.uploadFile(new byte[]{1, 2, 3}, "pdf");

        assertThat(url).startsWith(BASE_URL + "pedidos/");
        assertThat(url).endsWith(".pdf");
    }

    @Test
    void uploadFile_deveChamarS3TemplateComBucketCorreto() {
        service.uploadFile(new byte[]{}, "jpg");

        verify(s3Template).upload(eq(BUCKET), any(), any(InputStream.class));
    }

    @Test
    void uploadImage_deveDelegarParaUploadFileComExtensaoJpg() {
        String url = service.uploadImage(new byte[]{1});

        assertThat(url).endsWith(".jpg");
        verify(s3Template).upload(eq(BUCKET), any(), any(InputStream.class));
    }

    @Test
    void uploadFile_deveLancarS3UploadExceptionQuandoS3Falha() {
        doThrow(new RuntimeException("S3 indisponível")).when(s3Template).upload(any(), any(), any());

        assertThatThrownBy(() -> service.uploadFile(new byte[]{}, "jpg"))
                .isInstanceOf(S3UploadException.class)
                .hasMessageContaining("Falha ao salvar arquivo no S3");
    }

    @Test
    void uploadFile_deveGerarChavesUnicasParaUploadsDistintos() {
        String url1 = service.uploadFile(new byte[]{}, "png");
        String url2 = service.uploadFile(new byte[]{}, "png");

        assertThat(url1).isNotEqualTo(url2);
    }
}
