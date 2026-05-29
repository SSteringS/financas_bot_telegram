# HOTFIX — Aceitar imagens enviadas como Document (não só Photo)

> Bug independente do hotfix anterior (`HOTFIX-pedido-data-pedido-PATCH.md`). Manifesta em prod com `NullPointerException` quando o usuário envia uma imagem que veio do WhatsApp (que o Telegram transmite como `document`, não `photo`).

---

## Diagnóstico

### Erro observado em prod

```
java.lang.NullPointerException: Cannot invoke "java.util.List.stream()" because
the return value of "org.telegram.telegrambots.meta.api.objects.Message.getPhoto()"
is null
  at PaymentProofStrategy.extractHighestQualityImageFileId(PaymentProofStrategy.java:96)
  at PaymentProofStrategy.process(PaymentProofStrategy.java:57)
```

### Root cause

O Telegram permite imagens em duas estruturas distintas no `Update`:

| Estrutura | Quando aparece | Característica |
|---|---|---|
| `message.photo` (lista de `PhotoSize`) | Foto enviada nativa pelo cliente Telegram (compressão automática) | Várias resoluções pré-renderizadas |
| `message.document` (`Document`) | Arquivo enviado como "documento" — incluindo imagens compartilhadas do WhatsApp, screenshots salvos como arquivo, etc | Bytes brutos, qualquer mimeType |

O log mostra o payload do erro:

```
document=Document(fileId=..., fileName=DOC-20260511-WA0027 (1).,
                  mimeType=application/octet-stream, fileSize=62449)
photo=null
caption=#3 PIX
```

A imagem é um `Document` (vinda do WhatsApp, conforme o prefixo `DOC-...-WA0027`), com `photo=null`. O método `extractHighestQualityImageFileId` em **PaymentProofStrategy** e **PaymentRequestStrategy** assume que `getPhoto()` sempre retorna a lista de PhotoSizes — quando vem null, NPE.

### Bug sempre existiu

Não é regressão do hotfix anterior. É uma limitação de design da BE-original que só apareceu agora porque o data_pedido bloqueava o fluxo antes. Com o fluxo destravado, testes com formatos diferentes expuseram esta lacuna.

---

## Estratégia de fix

Tornar o `extractHighestQualityImageFileId` (nas duas strategies) tolerante a ambas estruturas:

1. Se `message.photo` existe e não está vazio → usar o `fileId` da maior foto (comportamento atual)
2. Se não, mas `message.document` existe → usar o `fileId` do document
3. Se nenhum dos dois existe → lançar exceção amigável

**Decisão consciente:** não vamos validar `mimeType` do document. A razão é que o WhatsApp manda `application/octet-stream` (genérico binário) — uma validação por mimeType rejeitaria casos legítimos. A confiança de que o usuário está mandando uma imagem (não um PDF, vídeo, etc) fica no contrato do bot: caption no formato certo + arquivo binário.

Se isso virar problema (usuário mandar PDF acidentalmente e o bot subir pro S3 do mesmo jeito), podemos endurecer mais tarde. Pra hotfix, mantém simples.

---

## Branch

```bash
git checkout develop
git pull
git checkout -b hotfix/aceitar-document
```

---

## Patch

### 1. `PaymentProofStrategy.extractHighestQualityImageFileId`

**Arquivo:** `financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/adapters/in/telegram/strategy/PaymentProofStrategy.java`

Substituir o método atual por:

```java
/**
 * Extrai o file_id da imagem da mensagem. Aceita tanto foto (message.photo)
 * quanto documento (message.document) — necessário porque imagens compartilhadas
 * do WhatsApp vêm como Document, não como Photo.
 */
private String extractHighestQualityImageFileId(Message message, Long chatId) {
    if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
        return message.getPhoto().stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .map(PhotoSize::getFileId)
                .orElseThrow(() -> new PhotoProcessingException(
                        "Não foi possível obter o file_id da foto.", chatId));
    }

    if (message.getDocument() != null) {
        return message.getDocument().getFileId();
    }

    throw new PhotoProcessingException(
            "Nenhuma imagem encontrada na mensagem. Envie como foto ou anexo.",
            chatId);
}
```

Adicionar o import de `Document` no topo do arquivo:

```java
import org.telegram.telegrambots.meta.api.objects.Document;
```

(Provavelmente não precisa do import explícito se for inferido pelo `getDocument()`, mas a IDE vai sugerir.)

### 2. `PaymentRequestStrategy.extractHighestQualityImageFileId`

**Arquivo:** `financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/adapters/in/telegram/strategy/PaymentRequestStrategy.java`

Mesma mudança, com adaptação da assinatura (não recebe `chatId` na versão atual; manter como está mas trocar a exceção lançada por algo razoável):

```java
/**
 * Extrai o file_id da imagem da mensagem. Aceita tanto foto quanto documento.
 */
private String extractHighestQualityImageFileId(Message message) {
    if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
        return message.getPhoto().stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .map(PhotoSize::getFileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Foto sem file_id válido."));
    }

    if (message.getDocument() != null) {
        return message.getDocument().getFileId();
    }

    throw new IllegalArgumentException(
            "Nenhuma imagem encontrada na mensagem. Envie como foto ou anexo.");
}
```

**Observação:** seria ideal que essa exceção também usasse `PhotoProcessingException` com `chatId` (pro handler global enviar mensagem amigável ao usuário). Mas isso muda a assinatura do método e os callers. Pra **hotfix**, manter `IllegalArgumentException` e tratar a mensagem ao usuário em iteração futura. O bug crítico (NPE em prod) é resolvido.

### 3. Atualizar testes

**`PaymentProofStrategyTest.java`** — adicionar 2 testes:

```java
@Test
void deveAceitarComprovanteEnviadoComoDocument() {
    Update update = mockUpdateComDocument("#42 pix", 12345L, 99L, "file_doc_abc");
    when(telegramFileDownloaderService.downloadImageByFileId("file_doc_abc")).thenReturn(new byte[]{1,2,3});
    when(s3ImageUploadService.uploadImage(any())).thenReturn("https://s3.../doc.jpg");
    Comprovante comprovanteSalvo = Comprovante.builder().id(1L).pedidoId(42L).build();
    when(registrarComprovanteUsecase.execute(eq(42L), eq("PIX"), eq("file_doc_abc"), any(), eq(12345L)))
        .thenReturn(comprovanteSalvo);

    strategy.process(update);

    verify(telegramFileDownloaderService).downloadImageByFileId("file_doc_abc");
    verify(registrarComprovanteUsecase).execute(eq(42L), eq("PIX"), eq("file_doc_abc"), any(), eq(12345L));
}

@Test
void deveLancarExcecaoQuandoNemFotoNemDocumentPresentes() {
    Update update = mockUpdateSemFotoOuDocument("#42 pix", 12345L, 99L);

    assertThatThrownBy(() -> strategy.process(update))
        .isInstanceOf(PhotoProcessingException.class)
        .hasMessageContaining("Nenhuma imagem encontrada");
}
```

E helpers correspondentes:

```java
private Update mockUpdateComDocument(String legenda, Long chatId, Long userId, String fileId) {
    Update update = new Update();
    Message message = new Message();
    message.setCaption(legenda);
    message.setChatId(chatId);
    message.setFrom(new User(userId, "User", false));
    message.setMessageId(1);

    Document document = new Document();
    document.setFileId(fileId);
    document.setFileSize(50000L);
    message.setDocument(document);
    // photo deixado null

    update.setMessage(message);
    return update;
}

private Update mockUpdateSemFotoOuDocument(String legenda, Long chatId, Long userId) {
    Update update = new Update();
    Message message = new Message();
    message.setCaption(legenda);
    message.setChatId(chatId);
    message.setFrom(new User(userId, "User", false));
    message.setMessageId(1);
    // photo e document deixados null
    update.setMessage(message);
    return update;
}
```

**`PaymentRequestStrategyTest.java`** — adicionar testes equivalentes pro pedido (legenda `100.00 Almoço`, com document e sem nada).

---

## Validação

### 1. Build e testes

```bash
cd financas_bot_telegram
./mvnw clean test
```

Esperado: tudo verde, incluindo os novos testes de document e ausência de imagem.

### 2. Smoke test em dev

Subir app local. Via Telegram, mandar **três cenários**:

| Cenário | Como | Esperado |
|---|---|---|
| Comprovante como foto | Tirar foto direto no Telegram + legenda `#<id> pix` | ✅ persiste, status PAGO |
| Comprovante como document | Compartilhar imagem do WhatsApp pro Telegram + legenda `#<id> pix` | ✅ persiste, status PAGO |
| Mensagem só texto | Mandar legenda sem foto/anexo | ❌ erro amigável "Nenhuma imagem encontrada" |

### 3. Deploy

Após validado, merge `hotfix/aceitar-document` → develop → main. Pipeline faz deploy em prod.

Em prod, repetir o smoke test do cenário "document" (que era o que estava quebrando).

---

## Critério de aceitação

- [ ] `PaymentProofStrategy.extractHighestQualityImageFileId` aceita photo OU document
- [ ] `PaymentRequestStrategy.extractHighestQualityImageFileId` aceita photo OU document
- [ ] Exceção lançada quando nenhum dos dois existe é amigável (não NPE)
- [ ] Novos testes unitários cobrindo os 2 caminhos novos em cada strategy
- [ ] `./mvnw clean test` passa
- [ ] Smoke test em dev: foto, document, e ausência funcionam conforme esperado
- [ ] Deploy em prod: cenário do log original (document do WhatsApp) agora persiste sem NPE

---

## Fora de escopo

- **Validação de mimeType** do document (rejeitar PDF, vídeo etc). Por enquanto, qualquer document é aceito — se virar problema, endurecer depois.
- **Mudar a assinatura de `extractHighestQualityImageFileId` na PaymentRequestStrategy** pra receber `chatId` e usar exceção própria. Mudança maior, não bloqueia o hotfix.
- **Suporte a vídeo, áudio, ou outros tipos de mensagem** — fica fora do escopo do bot.
- **Refactor de exceções** pra unificar comportamento entre as duas strategies. Pode entrar em uma feature de "padronização de erros" futura.

---

## Reportar status

`docs/status/HOTFIX-document-vs-photo.md` seguindo `_TEMPLATE.md`. Cobrir:

- Resultado de `./mvnw test`
- Smoke test em dev: 3 cenários (foto, document, ausência) com confirmação do comportamento
- Smoke test em prod após deploy
- IDs dos pedidos/comprovantes criados nos testes

---

## Nota pra discussão futura (não bloqueia hotfix)

Esse bug aponta dois temas que valem revisitar depois:

1. **Cobertura insuficiente de input** nas strategies originais. O design assumiu fluxo feliz (foto + caption correta) e não testou variações do que o Telegram aceita. Quando criar testes de novas strategies, vale documentar a lista de "entradas possíveis" na própria classe e cobrir cada uma.

2. **Falta de teste E2E ou de integração com Update reais**. Os unit tests cobriram lógica de regex/parsing, mas não pegaram que o Update pode ter document em vez de photo. Quando estabilizar, vale considerar testes de integração com payloads reais do Telegram (versão mais light que Testcontainers — só carregar JSON de webhook).

Esses dois pontos podem virar ADRs / tarefas em iteração futura.
