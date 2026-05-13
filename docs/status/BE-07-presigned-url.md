# BE-07 — Service de pre-signed URL pro S3

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- `StorageService` (port interface) criado em `application/port/out/` com:
  - `uploadImage(byte[])` — upload de imagem ao S3
  - `gerarUrlTemporariaParaLeitura(String s3Key, Duration ttl)` — pre-signed URL GET
- `S3StorageServiceImpl` criado em `adapters/out/s3/service/`:
  - Implementa `StorageService`
  - Upload via `S3Template.upload()`
  - Pre-signed URL via `S3Template.createSignedGetURL()` (awspring 3.2 — S3Template já abstrai o `S3Presigner`)
  - `normalizarChave()`: se `s3Key` começa com `baseUrl`, extrai só a chave; caso contrário, usa como está
- `S3ImageUploadService` legado mantido intacto (callers: Telegram strategies)
- 3 testes unitários mockando `S3Template`
- 144 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 144, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

- Não foi criado `S3Config` com `S3Presigner` — awspring 3.2.0 expõe `createSignedGetURL()` via `S3Template` diretamente, sem necessidade de bean extra.

---

## Próximos passos

- BE-08: endpoints de foto e comprovante usando `StorageService`

---

## Arquivos criados/modificados

**Novos (produção):**
- `application/port/out/StorageService.java`
- `adapters/out/s3/service/S3StorageServiceImpl.java`

**Novos (testes):**
- `S3StorageServiceImplTest`
