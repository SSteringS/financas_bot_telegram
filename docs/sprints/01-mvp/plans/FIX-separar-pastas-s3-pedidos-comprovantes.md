# FIX — Separar pastas no S3: pedidos e comprovantes

## Contexto

Hoje o `S3ImageUploadService` salva **todos** os arquivos no mesmo prefixo S3, hardcoded como `pedidos/`:

```java
// S3ImageUploadService.java
private static final String FOLDER_PREFIX = "pedidos";

private String generateS3Key() {
  String today = LocalDate.now().format(DATE_FORMATTER);
  String fileName = UUID.randomUUID() + FILE_EXTENSION;
  return String.format("%s/%s/%s", FOLDER_PREFIX, today, fileName);
}
```

Os dois callers chamam o mesmo método `uploadImage(byte[])`:

- `PaymentRequestStrategy.process()` — upload da **foto do pedido**
- `PaymentProofStrategy.process()` — upload do **comprovante**

Resultado em S3: `bucket/pedidos/20260513/<uuid>.jpg` para os dois casos, sem distinção. Identificado pelo humano ao validar manualmente os arquivos no bucket após `feature/backend-polish-evo07`.

**Problema:** ao listar arquivos no bucket, não dá pra distinguir o que é foto do pedido original e o que é comprovante de pagamento. Em volume baixo é só inconveniente; à medida que o volume cresce, vira fonte real de confusão (auditoria, debug, eventual ferramenta admin que liste arquivos).

## Objetivo

Particionar o S3 por **propósito do upload**:

- `bucket/pedidos/YYYYMMDD/<uuid>.<ext>` — foto enviada junto com o pedido novo
- `bucket/comprovantes/YYYYMMDD/<uuid>.<ext>` — comprovante de pagamento de pedido existente

Particionamento por data e UUID se mantêm.

## Branch

Criar a partir de `develop` (após `feature/backend-polish-evo07` mergear, pra layer em cima do EVO-07):

```
feature/fix-separar-pastas-s3
```

> **Nota:** se polish-evo07 ainda não tiver sido mergeada quando essa task for executada, alinhar com o Claude de planejamento — pode valer adicionar este fix dentro da própria polish-evo07 antes do merge final, ou esperar.

## Implementação

### 1. `S3ImageUploadService` — receber tipo de upload

Substituir o `FOLDER_PREFIX` estático por parâmetro. Recomendação: enum no domínio.

**Novo enum** (em `domain/enums/` ou `adapters/out/s3/`):

```java
public enum TipoUploadS3 {
  PEDIDO("pedidos"),
  COMPROVANTE("comprovantes");

  private final String folder;

  TipoUploadS3(String folder) { this.folder = folder; }
  public String getFolder() { return folder; }
}
```

**Assinatura nova:**

```java
public String uploadImage(byte[] imageBytes, TipoUploadS3 tipoUpload) { ... }
```

(Se EVO-07 já estiver mergeado e a assinatura for `uploadFile(byte[], TipoArquivo)`, a nova assinatura vira `uploadFile(byte[], TipoArquivo, TipoUploadS3)`. Não confundir os dois enums — eles representam coisas diferentes: `TipoArquivo` é IMAGEM/PDF, `TipoUploadS3` é PEDIDO/COMPROVANTE.)

`generateS3Key` passa a usar o folder do enum:

```java
private String generateS3Key(TipoUploadS3 tipoUpload) {
  String today = LocalDate.now().format(DATE_FORMATTER);
  String fileName = UUID.randomUUID() + FILE_EXTENSION;
  return String.format("%s/%s/%s", tipoUpload.getFolder(), today, fileName);
}
```

### 2. Callers — passar o tipo correto

- `PaymentRequestStrategy.process()`:
  ```java
  String s3ImageUrl = s3ImageUploadService.uploadImage(imageBytes, TipoUploadS3.PEDIDO);
  ```
- `PaymentProofStrategy.process()`:
  ```java
  String s3ImageUrl = s3ImageUploadService.uploadImage(imageBytes, TipoUploadS3.COMPROVANTE);
  ```

### 3. Não migrar arquivos antigos

Arquivos já em S3 sob `pedidos/` (que misturam pedidos e comprovantes) **ficam onde estão**. Os URLs guardados no banco continuam apontando pra eles e seguem funcionando.

A separação vale só pra uploads novos a partir do deploy.

**Justificativa:**
- Migração envolveria copiar arquivos no S3 (custo, risco) + atualizar URLs no banco (transação ampla)
- O ganho é só de "limpeza visual", o sistema funciona normalmente sem migrar
- Se um dia precisar (auditoria, ferramenta admin), dá pra escrever um script de migração isolado

Documentar isso em comentário no `TipoUploadS3` ou no commit message: "novos uploads particionam por tipo; arquivos antigos sob `pedidos/` continuam acessíveis pelas URLs originais."

## Testes

### `S3ImageUploadServiceTest`

Atualizar os testes existentes pra passar o enum, e adicionar dois cenários novos:

- `upload de PEDIDO gera chave com prefixo "pedidos/"`
- `upload de COMPROVANTE gera chave com prefixo "comprovantes/"`
- (Manter) `chave inclui data corrente no formato YYYYMMDD`
- (Manter) `UUID único por chamada`

### `PaymentRequestStrategyTest` e `PaymentProofStrategyTest`

Verificar (via mock do `S3ImageUploadService`) que cada strategy chama com o enum correto:

```java
verify(s3ImageUploadService).uploadImage(any(), eq(TipoUploadS3.PEDIDO));
```

### Integração (opcional)

Se já houver teste de integração que exercita o fluxo completo end-to-end, conferir que ele continua passando. Não é obrigatório criar novo só pra esse fix.

## Critérios de aceite

- [ ] Existe `TipoUploadS3` (ou nome equivalente) com pelo menos `PEDIDO` e `COMPROVANTE`
- [ ] `S3ImageUploadService` aceita o tipo e gera chave com folder apropriado
- [ ] `PaymentRequestStrategy` chama com `PEDIDO`
- [ ] `PaymentProofStrategy` chama com `COMPROVANTE`
- [ ] Hardcode `FOLDER_PREFIX = "pedidos"` removido do service
- [ ] Todos os testes existentes (unit + integration) passam
- [ ] Novos testes do enum + roteamento de folder passam
- [ ] `mvn clean verify` limpo
- [ ] Validação manual pós-deploy: enviar 1 foto de pedido + 1 comprovante; conferir no bucket S3 que aparecem em `pedidos/YYYYMMDD/` e `comprovantes/YYYYMMDD/` respectivamente

## Pontos de atenção

- **Não confundir com `TipoArquivo`** (EVO-07): aquele enum representa o formato do arquivo (IMAGEM vs PDF); o `TipoUploadS3` representa o propósito do upload (pedido vs comprovante). Os dois coexistem.
- **Ordem das tasks:** este FIX assume que `feature/backend-polish-evo07` (com EVO-07) já foi mergeado. Se ainda não, alinhar com o planejamento.
- **Sem mudança de contrato externo:** as URLs guardadas no banco seguem sendo absolutas e seguem funcionando, antes e depois. O front não precisa saber dessa mudança.

## Referências

- Observação original: `docs/avaliacoes/backend-polish-evo07.md` (validação manual S3)
- Código afetado:
  - `financas_bot_telegram/src/main/java/.../adapters/out/s3/service/S3ImageUploadService.java`
  - `financas_bot_telegram/src/main/java/.../adapters/in/telegram/strategy/PaymentRequestStrategy.java`
  - `financas_bot_telegram/src/main/java/.../adapters/in/telegram/strategy/PaymentProofStrategy.java`
