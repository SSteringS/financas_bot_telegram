package br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.s3.S3Template;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceImplTest {

    @Mock private S3Template s3Template;

    private S3StorageServiceImpl service;
    private static final String BUCKET = "meu-bucket";
    private static final String BASE_URL = "https://s3.amazonaws.com/meu-bucket/";

    @BeforeEach
    void setUp() {
        service = new S3StorageServiceImpl(s3Template, BUCKET, BASE_URL);
    }

    @Test
    void gerarUrlTemporaria_deveRetornarUrlPresigned() throws MalformedURLException {
        URL expected = new URL("https://s3.amazonaws.com/meu-bucket/pedidos/20260501/abc.jpg?X-Amz-Signature=sig");
        when(s3Template.createSignedGetURL(eq(BUCKET), eq("pedidos/20260501/abc.jpg"), any(Duration.class)))
                .thenReturn(expected);

        String resultado = service.gerarUrlTemporariaParaLeitura("pedidos/20260501/abc.jpg", Duration.ofMinutes(10));

        assertThat(resultado).contains("X-Amz-Signature");
    }

    @Test
    void gerarUrlTemporaria_deveNormalizarUrlCompletaParaChave() throws MalformedURLException {
        URL expected = new URL("https://s3.amazonaws.com/meu-bucket/pedidos/20260501/abc.jpg?X-Amz-Signature=sig");
        when(s3Template.createSignedGetURL(eq(BUCKET), eq("pedidos/20260501/abc.jpg"), any(Duration.class)))
                .thenReturn(expected);

        service.gerarUrlTemporariaParaLeitura(BASE_URL + "pedidos/20260501/abc.jpg", Duration.ofMinutes(10));

        verify(s3Template).createSignedGetURL(eq(BUCKET), eq("pedidos/20260501/abc.jpg"), any(Duration.class));
    }

    @Test
    void gerarUrlTemporaria_devePassarTtlCorreto() throws MalformedURLException {
        URL expected = new URL("https://s3.amazonaws.com/meu-bucket/key.jpg?X-Amz-Expires=600");
        Duration ttl = Duration.ofMinutes(10);
        when(s3Template.createSignedGetURL(any(), any(), eq(ttl))).thenReturn(expected);

        service.gerarUrlTemporariaParaLeitura("key.jpg", ttl);

        verify(s3Template).createSignedGetURL(any(), any(), eq(ttl));
    }
}
