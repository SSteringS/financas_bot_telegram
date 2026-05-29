# EVO-07 — Aceitar comprovantes como Document (incluindo PDF e imagens do WhatsApp)

> Tarefa substancial: o bot hoje aceita só `message.photo`. Quando o pai compartilha imagem do WhatsApp pro Telegram, ela vem como `message.document` e o bot quebra com NullPointerException. Essa tarefa adiciona suporte a document (foto vinda do WhatsApp, PDF de boleto, etc).

---

## Contexto

Já aconteceu incidente real (registrado em `docs/aprendizado/argument-resolver-vs-requestparam.md` referência cruzada — na verdade na conversa do hotfix-document-vs-photo) onde uma mensagem antiga com PDF travou a fila do bot por dias até intervenção manual.

A BE-15 (handler genérico de exceções) resolveu o problema de travamento — agora qualquer exceção vira `200 OK` + mensagem amigável. Mas a feature em si (aceitar document) continua faltando.

Plano base original em `docs/plans/HOTFIX-document-vs-photo.md` cobre o caminho mínimo (aceitar qualquer document). Esta tarefa estende:

1. Validação de mimeType — aceitar `image/*` e `application/pdf`; rejeitar outros (vídeo, áudio, zip)
2. Salvar no S3 com extensão correta (`.pdf` vs `.jpg`)
3. Coluna `tipo_arquivo` em `comprovantes` pra o front saber como renderizar (imagem inline vs PDF iframe)
4. Service de upload mais genérico (não só "uploadImage")

---

## Pré-requisitos

- BE-15 (handler genérico) em develop ✓

---

## Arquivos esperados

**Migration nova:**
- `src/main/resources/db/migration/V3__add_tipo_arquivo_comprovantes.sql`

**Modificados:**
- `adapters/in/telegram/strategy/PaymentProofStrategy.java` — extractHighestQualityImageFileId aceita document
- `adapters/in/telegram/strategy/PaymentRequestStrategy.java` — idem
- `adapters/out/persistence/entity/ComprovanteEntity.java` — adicionar campo `tipoArquivo`
- `domain/model/Comprovante.java` — adicionar campo `tipoArquivo`
- `adapters/out/persistence/mapper/ComprovanteMapper.java` — mapear o novo campo
- `application/services/RegistrarComprovanteServiceImpl.java` (ou usecase) — receber e propagar o `tipoArquivo`
- `adapters/out/s3/service/S3ImageUploadService.java` — adicionar método `uploadFile(bytes, contentType, extension)` mais genérico
- Tests dos pontos tocados

**Novos:**
- `domain/enums/TipoArquivo.java` — enum `IMAGEM | PDF`
- `adapters/in/telegram/exception/TipoArquivoNaoSuportadoException.java`

---

## Migration

`V3__add_tipo_arquivo_comprovantes.sql`:

```sql
-- V3: adiciona tipo_arquivo em comprovantes
ALTER TABLE comprovantes
    ADD COLUMN tipo_arquivo ENUM('IMAGEM', 'PDF') NOT NULL DEFAULT 'IMAGEM' AFTER imagem_url;

-- Backfill: registros existentes assumimos que são imagem (era o único tipo aceito antes)
-- Como já é DEFAULT 'IMAGEM' no ALTER acima, o backfill é implícito.
```

---

## Código-chave

### Enum novo

```java
// domain/enums/TipoArquivo.java
package br.com.satyan.stering.saita.financasbottelegram.domain.enums;

public enum TipoArquivo {
    IMAGEM,
    PDF
}
```

### Extrair file_id com suporte a document

Em `PaymentProofStrategy.extractHighestQualityImageFileId`:

```java
private ExtraidoArquivo extrair(Message message, Long chatId) {
    // 1. Foto enviada nativa (sempre é imagem)
    if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
        String fileId = message.getPhoto().stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .map(PhotoSize::getFileId)
                .orElseThrow(() -> new PhotoProcessingException("Foto sem file_id válido.", chatId));
        return new ExtraidoArquivo(fileId, TipoArquivo.IMAGEM, "jpg", "image/jpeg");
    }

    // 2. Document (PDF, imagem do WhatsApp, etc)
    if (message.getDocument() != null) {
        Document doc = message.getDocument();
        String mime = doc.getMimeType();
        if (mime == null) mime = "";

        if (mime.startsWith("image/")) {
            String ext = extrairExtensaoDeMime(mime); // jpg, png, etc
            return new ExtraidoArquivo(doc.getFileId(), TipoArquivo.IMAGEM, ext, mime);
        }
        if (mime.equals("application/pdf")) {
            return new ExtraidoArquivo(doc.getFileId(), TipoArquivo.PDF, "pdf", mime);
        }
        // Caso especial: WhatsApp manda mimeType=application/octet-stream pra fotos compartilhadas.
        // Aceitar como IMAGEM e deixar S3 servir como JPG (mais comum).
        if (mime.equals("application/octet-stream") || mime.isBlank()) {
            return new ExtraidoArquivo(doc.getFileId(), TipoArquivo.IMAGEM, "jpg", "image/jpeg");
        }

        throw new TipoArquivoNaoSuportadoException(
            "Tipo de arquivo '" + mime + "' não suportado. Envie foto ou PDF.", chatId);
    }

    throw new PhotoProcessingException(
        "Nenhuma imagem ou anexo encontrado. Envie como foto ou anexo.", chatId);
}

private record ExtraidoArquivo(String fileId, TipoArquivo tipo, String extensao, String contentType) {}

private String extrairExtensaoDeMime(String mime) {
    return switch (mime) {
        case "image/jpeg", "image/jpg" -> "jpg";
        case "image/png" -> "png";
        case "image/webp" -> "webp";
        case "image/gif" -> "gif";
        default -> "jpg";
    };
}
```

Aplicar mudança equivalente em `PaymentRequestStrategy` (pra a parte de pedido aceitar document também). Embora o caso de uso de PDF como pedido seja menos comum, vale consistência.

### S3 storage genérico

Em `S3ImageUploadService`, adicionar método novo (mantendo o antigo pra compat retroativa):

```java
public String uploadFile(byte[] bytes, String contentType, String extension) {
    String chave = construirChaveS3(extension); // pedidos/YYYYMMDD/<uuid>.<extension>
    PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(chave)
            .contentType(contentType)
            .build();
    s3Client.putObject(request, RequestBody.fromBytes(bytes));
    return baseUrl + chave;
}

private String construirChaveS3(String extension) {
    String data = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    return "pedidos/" + data + "/" + UUID.randomUUID() + "." + extension;
}
```

(O `uploadImage` existente pode delegar pra `uploadFile(bytes, "image/jpeg", "jpg")` mantendo retrocompat.)

### Persistir tipo_arquivo

`ComprovanteEntity`:

```java
@Enumerated(EnumType.STRING)
@Column(name = "tipo_arquivo", nullable = false)
private TipoArquivo tipoArquivo;
```

`Comprovante` domain POJO:

```java
private TipoArquivo tipoArquivo;
```

`ComprovanteMapper` — mapear nas duas direções, default `IMAGEM` se domain não setou (defensivo).

`RegistrarComprovanteServiceImpl` — receber `tipoArquivo` no parâmetro do `execute()` (ou via dto) e propagar pra entity.

`PaymentProofStrategy.process()` — após o `extrair()`, passar o `tipoArquivo` adiante.

### Tests

- `PaymentProofStrategyTest` — adicionar casos:
  - Document com mimeType `image/jpeg` → tipoArquivo=IMAGEM, extensao=jpg
  - Document com mimeType `application/pdf` → tipoArquivo=PDF, extensao=pdf
  - Document com mimeType `application/octet-stream` (WhatsApp share) → tipoArquivo=IMAGEM (fallback)
  - Document com mimeType `video/mp4` → `TipoArquivoNaoSuportadoException`
  - Mensagem sem foto nem document → `PhotoProcessingException`
- `S3ImageUploadServiceTest` — testar `uploadFile` com diferentes content types
- `ComprovanteMapperTest` — round-trip preserva `tipoArquivo`

---

## Critério de aceitação

- [ ] Migration V3 criada e aplica sem erro em dev (recriar banco e subir app)
- [ ] Enum `TipoArquivo` existe
- [ ] `Comprovante` domain + `ComprovanteEntity` + Mapper têm campo `tipoArquivo`
- [ ] `PaymentProofStrategy` aceita: photo, document image/*, document application/pdf, document application/octet-stream. Rejeita: document de mime não suportado (vídeo, áudio, zip)
- [ ] `PaymentRequestStrategy` aceita os mesmos formatos (mesma lógica)
- [ ] S3 service tem método `uploadFile` mais genérico
- [ ] Upload pra S3 usa extensão correta (`.pdf` quando PDF, `.jpg` quando imagem)
- [ ] Coluna `tipo_arquivo` persistida no banco com valor correto
- [ ] `./mvnw test` passa, todos os novos casos cobertos
- [ ] Smoke test manual em dev:
  - Mandar foto direto pelo Telegram → comprovante.tipo_arquivo=IMAGEM
  - Compartilhar imagem do WhatsApp pro Telegram → comprovante.tipo_arquivo=IMAGEM (vem como document mas é aceito)
  - Compartilhar PDF pro Telegram → comprovante.tipo_arquivo=PDF
  - Compartilhar vídeo pro Telegram → mensagem amigável "Tipo de arquivo não suportado"
- [ ] Adicionar entrada em `docs/aprendizado/` explicando o problema/decisão de aceitar octet-stream do WhatsApp como IMAGEM (vale pro humano lembrar do contexto)

---

## Fora de escopo

- Validar conteúdo real do arquivo (verificar que PDF é PDF de verdade, não vídeo renomeado) — overkill por agora
- Conversão de PDF pra imagem pra preview — fica pra futuro
- Frontend renderizar PDF inline via iframe (a FE-09 já tem isso, vai funcionar nativamente)
- Limitar tamanho máximo do arquivo (Telegram já limita a 20MB pra bots)

---

## Status report

`docs/status/EVO-07-aceitar-document-pdf.md`. Cobrir:
- Lista detalhada das mudanças
- Output de `./mvnw test` (especial atenção: testes de strategy devem ter ~4 novos cenários cada)
- Smoke test manual no Telegram: cenários acima testados, IDs dos comprovantes criados
- SQL: `SELECT id, pedido_id, tipo_arquivo, imagem_url FROM comprovantes ORDER BY id DESC LIMIT 5;` mostrando que `tipo_arquivo` está sendo preenchido
- Marcar `docs/PENDENCIAS-TECNICAS.md` removendo o item correspondente da EVO-07 (mover pra resolvidos)

Atualizar `docs/plans/FASE-3-VISUALIZACAO.md` marcando EVO-07 como ✅ concluída na seção "Fase 3d — Evolução pós-MVP".
