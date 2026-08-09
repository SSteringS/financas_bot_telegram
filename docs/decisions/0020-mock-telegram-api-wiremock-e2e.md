---
adr: 0020
titulo: "Mock do Telegram Bot API em E2E — WireMock standalone com URL configurável"
data: 2026-06-04
status: Accepted
decisores: humano
relacionado: [0017, 0013]
supersedes: null
superseded_by: null
---

# ADR 0020 — Mock do Telegram Bot API em E2E — WireMock standalone com URL configurável

---

## Contexto

A suíte E2E usa o back real rodando em profile `dev`. Quando um webhook Telegram com `foto+caption` chega, o back executa dois passos dependentes de rede externa:

1. **`getFile`** — chama `https://api.telegram.org/bot{token}/getFile?file_id={id}` para obter o caminho do arquivo.
2. **Download do arquivo** — baixa `https://api.telegram.org/file/bot{token}/{file_path}`.

Em E2E, o `file_id` do payload é sintético (não existe no Telegram real). Mesmo com token válido, a chamada ao Telegram real falharia com erro 400/404. Com token inválido ou sem rede, o erro seria diferente — mas o teste quebraria igualmente por dependência externa não determinística.

Esse bloqueador foi registrado em `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §3 e deixou QA-011 (expansão E2E) em `estado: rascunho` aguardando esta ADR.

**Estado atual do código:**

`TelegramFileDownloaderService` tem um design parcialmente configurável:

```java
// getFile — URL configurável via @Value("${telegram.api.url}")
String getFileUrl = telegramApiUrl + botToken + "/getFile?file_id=" + fileId;

// download do arquivo — URL HARDCODED
String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
```

A propriedade `telegram.api.url = https://api.telegram.org/bot` já existe em `application.properties` e `application-dev.properties`. A URL de download está hardcoded.

Três opções foram avaliadas:

| # | Opção | Resumo |
|---|---|---|
| A | **WireMock standalone** | Servidor HTTP mock na porta 8089; back com URL configurável aponta pra ele em E2E |
| B | Flag `app.telegram.skip-media-download=true` | Back pula o download em modo E2E; caminho crítico não é exercitado |
| C | Stub `@Profile("e2e")` | Bean substituto ativado por profile Spring; exige wiring novo |

**Decisão do humano (2026-06-04): Opção A — WireMock.**

---

## Decisão

### 1. Mecanismo de mock: WireMock standalone

Adotar **WireMock standalone** como servidor HTTP mock no ambiente E2E. WireMock sobe como processo separado na porta `8089`, antes do back. O back é iniciado normalmente, apontando `telegram.api.url` e `telegram.file.url` para `localhost:8089`.

### 2. Tornar o URL de download também configurável no back

Adicionar a propriedade `telegram.file.url` ao back (mínimo de mudança no código de produção):

**`TelegramFileDownloaderService.java`** — injetar `telegram.file.url`:

```java
public TelegramFileDownloaderService(RestClient restClient,
    @Value("${telegram.api.url}") String telegramApiUrl,
    @Value("${telegram.file.url:https://api.telegram.org/file/bot}") String telegramFileUrl,
    @Value("${telegram.bot-token}") String botToken) {
  this.telegramApiUrl = telegramApiUrl;
  this.telegramFileUrl = telegramFileUrl;
  this.botToken = botToken;
}

// substituir a linha hardcoded:
String downloadUrl = telegramFileUrl + botToken + "/" + filePath;
```

**Propriedades a adicionar/atualizar:**

- `application.properties`: `telegram.file.url=https://api.telegram.org/file/bot`
- `application-dev.properties`: `telegram.file.url=https://api.telegram.org/file/bot`
- `application-dev.properties.example`: `telegram.file.url=https://api.telegram.org/file/bot`

Em produção (`application-prod.properties`), não é necessário adicionar — o default do `@Value` garante fallback correto.

### 3. Configuração E2E — variáveis de ambiente

No `frontend/.env.e2e.example`, adicionar:

```
# Mock do Telegram Bot API (WireMock na porta 8089)
TELEGRAM_API_URL=http://localhost:8089/bot
TELEGRAM_FILE_URL=http://localhost:8089/file/bot
```

Spring Boot mapeia `TELEGRAM_API_URL` → `telegram.api.url` e `TELEGRAM_FILE_URL` → `telegram.file.url` via relaxed binding. O script `subir-stack.ts` passa essas variáveis como env do processo Spring ao iniciar o back em E2E.

### 4. Stubs WireMock obrigatórios

Criar `frontend/e2e/wiremock/mappings/` com os stubs JSON:

**`telegram-getfile.json`** — responde ao `getFile`:

```json
{
  "request": { "method": "GET", "urlPathPattern": "/bot.*/getFile.*" },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "body": "{\"ok\":true,\"result\":{\"file_id\":\"test_file_id\",\"file_unique_id\":\"test_unique\",\"file_size\":1000,\"file_path\":\"photos/test_photo.jpg\"}}"
  }
}
```

**`telegram-getfile-error.json`** — stub de erro para cenário de falha (opcional, mas recomendado para testar `TelegramFileDownloadException`):

```json
{
  "request": { "method": "GET", "urlPathPattern": "/bot.*/getFile.*", "queryParameters": { "file_id": { "equalTo": "error_file_id" } } },
  "response": { "status": 400, "body": "{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: wrong file_id\"}" }
}
```

**`telegram-download.json`** — responde ao download do arquivo:

```json
{
  "request": { "method": "GET", "urlPathPattern": "/file/bot.*/photos/test_photo.jpg" },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "image/jpeg" },
    "bodyFileName": "sample-photo.jpg"
  }
}
```

Arquivo `frontend/e2e/wiremock/__files/sample-photo.jpg` — imagem JPEG mínima (~1-2KB) commitada no repo. Gerada com ImageMagick ou similar: `convert -size 100x100 xc:gray sample-photo.jpg`.

### 5. Integração com os scripts de orquestração E2E

`frontend/e2e/scripts/subir-stack.ts` — estender pra:

1. Verificar se WireMock está disponível (JAR local ou via npm `wiremock-standalone`).
2. Iniciar WireMock: `npx wiremock --port 8089 --root-dir ./e2e/wiremock` (ou `java -jar wiremock.jar ...`).
3. Aguardar saúde: `GET http://localhost:8089/__admin/health` retorna 200 (timeout 10s).
4. Registrar PID do WireMock na lista de processos gerenciados.
5. Iniciar back com as env vars `TELEGRAM_API_URL` e `TELEGRAM_FILE_URL` definidas.

`frontend/e2e/scripts/derrubar-stack.ts` — matar WireMock junto com back e front.

### 6. Task de implementação no back

As mudanças em `TelegramFileDownloaderService.java` e nas properties são escopo de uma **task BE separada** (não do QA-011). QA-011 só pode rodar o cenário foto+caption depois que essa task BE for mergeada. O plano de QA-011 registra essa dependência sequencial.

---

## Razões

- **Cobre o caminho completo.** O download de mídia é parte crítica do fluxo foto+caption. WireMock executa todo o código de `TelegramFileDownloaderService` — incluindo parsing de `file_path` e chamada HTTP de download. Se essa lógica regredir, o E2E captura. Opção B pula esse código.
- **Não polui lógica de negócio.** A única mudança no código de produto é tornar uma URL hardcoded em propriedade configurável — boa prática de qualquer forma (facilita staging e smoke pós-deploy). Sem `if (flag)`, sem profile spring novo.
- **Opção A já tem um análogo no projeto.** `WhatsAppMediaDownloaderService` e `WhatsAppMessageSenderService` têm testes que mocam chamadas HTTP externas (via `MockMvc` / RestClient mock). WireMock aplica o mesmo princípio em nível de E2E.
- **WireMock é padrão de mercado** para mock de dependências HTTP externas em testes de integração/E2E. Documentação ampla, stubs versionados como JSON, suporte a cenários de erro.
- **Opção C (stub @Profile)** foi descartada: requer profile `e2e` novo no Spring + wiring adicional. Toda divergência de profile é vetor de "funciona em teste, quebra em prod". Sem ganho sobre a Opção A.
- **Opção B (flag skip-media)** foi descartada: introduz branch de produção cujo único propósito é pular código em teste. O código que mais importa testar (`TelegramFileDownloaderService`) fica fora da cobertura.

---

## Consequências

**Positivas:**

- QA-011 Sub-área A (cenário foto+caption) é desbloqueada após task BE de URL configurável mergear.
- `TelegramFileDownloaderService` passa a ser exercitado ponta a ponta no E2E, incluindo parsing de resposta e download HTTP.
- `telegram.file.url` vira configurável — facilita staging / smoke pós-deploy no futuro.
- Stubs WireMock versionados em `frontend/e2e/wiremock/` servem de documentação viva do contrato esperado com o Telegram.

**Negativas / custos:**

- Dependência nova: WireMock standalone (JAR ~30MB ou via npm). CI precisará provisionar.
- Porta 8089 precisa estar livre no ambiente E2E (dev local e CI).
- `subir-stack.ts` fica mais complexo: mais um processo a gerenciar (start, healthcheck, shutdown).
- Task BE adicional necessária antes de QA-011 começar: tornar `telegram.file.url` configurável.
- `application-dev.properties` expõe o token real — já é o caso hoje; não piora, mas vale observar que `telegram.file.url` ficará ao lado do token.

**Métricas para avaliar adoção:**

- Baseline: 0 testes E2E de cenário foto+caption.
- Alvo: ≥ 1 teste foto+caption verde 3x consecutivas em ambiente limpo.
- Critério de sucesso: QA-011 entregue com `npm run e2e:full` verde e `TELEGRAM_API_URL` não definida em produção (fallback `https://api.telegram.org` ativo, sem regressão).

---

## Alternativas consideradas

- **Opção B — flag `app.telegram.skip-media-download=true`:** descartada — pula o código mais crítico. Branch de produção sem uso real.
- **Opção C — stub `@Profile("e2e")`:** descartada — profile Spring novo, wiring adicional, divergência prod/teste. Sem ganho sobre Opção A.
- **WireMock embutido (`@WireMockTest` no back):** descartada — E2E usa o back real (não é `@SpringBootTest`). Não há contexto Spring de test para injetar.
- **Mock via MSW (front):** descartada — MSW intercepta requests do browser, não do back. O download de mídia é chamado pelo back direto.
- **Manter sem cobertura:** descartado — foto+caption é o caminho positivo mais importante do produto; deixar sem cobertura é regressão silenciosa esperando acontecer.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §3 — análise original das 3 opções.
- `docs/sprints/03-folha-pagamento/plans/QA-011-expansao-e2e-cenarios-positivos.md` — task que consome esta ADR.
- ADR 0017 — prefixo QA-NNN (contexto das tasks de qualidade).
- ADR 0013 — estratégia multi-canal adapters (contexto do `TelegramFileDownloaderService` como adapter de saída).
- `financas_bot_telegram/src/main/java/.../adapters/out/telegram/service/TelegramFileDownloaderService.java` — arquivo afetado pela mudança de URL configurável.
- [WireMock standalone docs](https://wiremock.org/docs/standalone/java-jar/) — referência de uso do JAR.
- [wiremock npm package](https://www.npmjs.com/package/wiremock) — alternativa via npm pra integrar ao toolchain Node/TypeScript.
