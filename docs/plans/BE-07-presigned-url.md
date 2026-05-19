# BE-07 — Service de pre-signed URL pro S3

> Service que gera URL HTTPS temporária pra um objeto S3, válida por 10 minutos. Não expõe S3 público nem credenciais — o cliente recebe um link que funciona por tempo limitado. Pré-requisito pros endpoints de foto e comprovante da BE-08.

---

## Pré-requisitos

- `S3ImageUploadService` já existe e usa Spring Cloud AWS — verificar se já temos `S3Presigner` disponível ou se precisa adicionar dep

---

## Arquivos esperados

**Novos:**
- `application/port/out/StorageService.java` (interface) — métodos: `gerarUrlTemporariaParaLeitura(s3Key, ttl)`
- Renomear `S3ImageUploadService` pra implementar a interface, OU criar `S3StorageServiceImpl` que estende as funcionalidades atuais

**Modificados:**
- Verificar se o adapter atual de S3 implementa um port limpo ou se está acoplado. Se estiver acoplado, criar a port `StorageService` e fazer o adapter implementar.
- Possivelmente atualizar callers do upload pra usar a interface

**Tests:**
- `S3StorageServiceImplTest` — usar Mockito pra mockar `S3Presigner` e verificar que a URL gerada tem assinatura

---

## Código-chave

### `StorageService` port

```java
package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import java.time.Duration;

public interface StorageService {
    /**
     * Faz upload de uma imagem ao S3, particionando por data.
     * Retorna a URL pública (placeholder, não autenticada — uso interno).
     */
    String uploadImage(byte[] bytes);

    /**
     * Gera uma pre-signed URL pra leitura GET do objeto, válida pelo TTL informado.
     * Se s3Key inclui a base-url completa, extrair só a chave.
     */
    String gerarUrlTemporariaParaLeitura(String s3Key, Duration ttl);
}
```

### Implementação com AWS SDK v2

Se ainda não tiver `software.amazon.awssdk.services.s3:s3` direto (provavelmente já vem com `spring-cloud-aws-starter-s3`), confirmar:

```java
@Service
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String baseUrl;

    public S3StorageServiceImpl(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${financasbot.s3.base-url}") String baseUrl) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
    }

    @Override
    public String uploadImage(byte[] bytes) {
        // ... mantém lógica atual do S3ImageUploadService ...
    }

    @Override
    public String gerarUrlTemporariaParaLeitura(String s3Key, Duration ttl) {
        String chave = normalizarChave(s3Key);
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(chave)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(b -> b
                .signatureDuration(ttl)
                .getObjectRequest(get));
        return presigned.url().toString();
    }

    private String normalizarChave(String urlOuChave) {
        if (urlOuChave.startsWith(baseUrl)) {
            return urlOuChave.substring(baseUrl.length());
        }
        return urlOuChave;
    }
}
```

`S3Presigner` é um bean separado do `S3Client`. Adicionar config se não existir:

```java
@Configuration
public class S3Config {
    @Bean
    public S3Presigner s3Presigner(@Value("${aws.s3.region}") String region) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
```

(Verificar se o `spring-cloud-aws` já fornece `S3Presigner` como bean autoconfigured — em versões mais recentes do `spring-cloud-aws-starter-s3` ele vem grátis. Se vier, não precisa do `S3Config`.)

---

## Critério de aceitação

- [ ] `StorageService.gerarUrlTemporariaParaLeitura("pedidos/20260503/abc.jpg", Duration.ofMinutes(10))` retorna URL HTTPS com:
  - `X-Amz-Algorithm`
  - `X-Amz-Signature`
  - `X-Amz-Expires=600` (em segundos)
  - `X-Amz-Date`
- [ ] URL gerada funciona: dá `GET` na URL via curl e recebe o conteúdo do objeto (testado manualmente com objeto real em dev)
- [ ] Caller passando uma URL completa (`https://s3.../pedidos/...`) tem a base removida automaticamente
- [ ] Caller passando só a chave (`pedidos/...`) funciona igual
- [ ] Após o TTL expirar, GET na mesma URL retorna 403 do S3
- [ ] `S3ImageUploadService` legado continua funcionando OU foi substituído por `S3StorageServiceImpl`
- [ ] `./mvnw test` passa

---

## Fora de escopo

- Endpoints REST que usam essa URL (BE-08)
- Versionamento de objetos no S3 (não relevante)

---

## Status report

`docs/status/BE-07-presigned-url.md`. Output dos testes + URL gerada de exemplo + confirmação de teste manual (curl no link gerado retornou 200 com bytes). Próximo: BE-08.
