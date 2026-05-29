# BE-18 — Adapter de saída WhatsApp (sender + media downloader)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** `docs/architecture/adapter-whatsapp-cloud-api.md` §5. É o "como" técnico do envio e download de mídia da Graph API.
> - **Prioridade:** alta — usado por BE-19 (feedback ao usuário §4.5), BE-21b (notificação template) e indiretamente pelo BE-19a + listener da BE-21a (futuro).
> - **Esforço:** médio (dois services + config de secrets + tratamento de erros HTTP + testes com mocks).
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/` + `application*.properties` → **Claude do back**.
> - **Branch:** `feature/be-18-adapter-saida-whatsapp`, a partir de `develop`.
> - **Dependências:** **BE-17 em `develop`** ✅. **NÃO depende de PREP-WA estar pronto** — os testes são contra mocks (RestTemplate/WebClient); integração real só na BE-20.
> - **Riscos:**
>   1. **Escolher HTTP client diferente do padrão do projeto** — confirmar antes de começar (provavelmente RestTemplate, mesmo do Telegram). Mitigação: ler o `TelegramMessageSenderService` existente como espelho.
>   2. **Secrets não wired** (mapeamento `whatsapp.access-token` → AWS Secrets Manager) — Spring Cloud AWS resolve hifens, mas é fácil de esquecer um placeholder em `application.properties`.
>   3. **Tratamento de erro HTTP genérico** — Graph API pode retornar 401 (token expirado), 429 (rate limit), 4xx de negócio (template não aprovado, número inválido). Confundir tudo num exception genérico esconde a causa real.

---

## Contexto

A spec §5 detalha **dois services novos** em `adapters/out/whatsapp/service/`:

1. **`WhatsAppMessageSenderService`** — `POST https://graph.facebook.com/{version}/{phone_number_id}/messages` com `Authorization: Bearer <token>`. Dois modos:
   - **Texto livre** (resposta na janela de 24h, "service message"): `{ messaging_product, to, type: "text", text: { body } }`.
   - **Template** (fora da janela, EVO-02): `{ messaging_product, to, type: "template", template: { name, language, components } }`.
2. **`WhatsAppMediaDownloaderService`** — dois passos: `GET /{media_id}` (com Bearer) → JSON com `url` temporária; `GET <url>` (com Bearer) → bytes. A URL exige header de auth e expira rápido — baixar na hora.

Substituem (em padrão) `TelegramMessageSenderService` e `TelegramFileDownloaderService`. Mesma responsabilidade, contrato diferente.

⚠️ **Sobre a restrição BR (erro 130497):** descoberta em `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md` — Business não-verificada não consegue enviar pra BR. **Isso não bloqueia esta task**: testes são com mocks; envio real só rola pós-Business Verification (PREP-WA Fase 4). Documentar no status report como gotcha pro implementador entender.

## Decisão / abordagem

- **HTTP client:** RestTemplate (assumindo é o padrão do Telegram). Confirmar olhando o código existente do Telegram sender; se for WebClient, usar o mesmo. **Manter consistência com o estado-atual.**
- **Versão da Graph API:** começar em `v20.0` (atual estável da Meta na época deste plano). Property configurável: `whatsapp.graph-api-version`.
- **Tratamento de erro HTTP:**
  - 2xx → success.
  - 4xx **específicos importantes** (`401` token, `400` com `error.code` da Meta como 131026/130497/etc.) → `WhatsAppApiException` carregando `code` + `title` + `details` da Meta. Permite o caller decidir.
  - Outros 4xx/5xx → `WhatsAppApiException` genérico com status + body.
- **DTOs da request/response:** records simples em `adapters/out/whatsapp/dto/` (ou alinhar com `adapters/in/whatsapp/dto/` se já estiver decidido — provavelmente separar por camada in/out).

## Escopo / arquivos

**Criar:**

- `adapters/out/whatsapp/service/WhatsAppMessageSenderService.java` — com métodos:
  - `void enviarTexto(String waId, String body)` — monta JSON de service message (texto livre).
  - `void enviarTemplate(String waId, String templateName, String languageCode, List<TemplateParametro> parametros)` — monta JSON de template message. **`TemplateParametro`** pode ser um record simples com tipo (texto, número) e valor.
- `adapters/out/whatsapp/service/WhatsAppMediaDownloaderService.java` — com método:
  - `byte[] baixar(String mediaId)` (ou `InputStream`, alinhar com o uso esperado em BE-19) — executa os 2 GETs.
- `adapters/out/whatsapp/service/WhatsAppApiException.java extends RuntimeException` — com `errorCode` (int da Meta, nullable), `errorTitle`, `httpStatus`, `body` (raw, pra debug). Construtores convenientes pra cada caso.
- `adapters/out/whatsapp/dto/` — DTOs request/response da Graph API (request de send, response de `/{media_id}` etc.). Records simples.

**Modificar:**

- `application.properties` — adicionar placeholders:
  ```
  whatsapp.graph-api-base-url=https://graph.facebook.com
  whatsapp.graph-api-version=v20.0
  whatsapp.phone-number-id=CHANGE_ME
  whatsapp.access-token=CHANGE_ME
  # app-secret e verify-token entram em outras tasks (BE-19); aqui só o que o sender/downloader usam
  ```
- `application-prod.properties` — referenciar Secrets Manager (`${whatsapp_phone_number_id}`, `${whatsapp_access_token}`).
- `application-dev.properties.example` — placeholders, sem valor real.

**Não tocar:**
- Nada de `application/` (porta + listener vão na BE-21a; uso real do sender na BE-19/BE-21).
- Nada de `frontend/`, infra, ou adapter Telegram.

## Critérios de aceitação

- [ ] `WhatsAppMessageSenderService.enviarTexto` monta o JSON conforme §5.1 (text mode) e faz POST pro endpoint correto — validado em teste com mock de RestTemplate/WebClient.
- [ ] `WhatsAppMessageSenderService.enviarTemplate` monta o JSON com `type: "template"` e parâmetros corretos.
- [ ] `WhatsAppMediaDownloaderService.baixar` faz 2 GETs (URL lookup → bytes) com `Authorization: Bearer` em ambos — validado em teste.
- [ ] Erros HTTP da Graph API mapeados pra `WhatsAppApiException`:
  - Teste: response 400 com `{ "error": { "code": 130497, ... } }` → exception carrega `code=130497`.
  - Teste: response 401 → exception com flag/info de token problemático.
  - Teste: response 500 → exception com http status + body.
- [ ] Properties wired corretamente (validar com `@Value` ou `@ConfigurationProperties`).
- [ ] `mvn test` verde (incluindo todos os testes anteriores).
- [ ] `mvn package -DskipTests` ok.
- [ ] **Nada integrado com listener/usecase nesta task** — só os services + DTOs + exceção (verificar com `git diff --stat` que não há mudança em `application/event/`, `application/usecases/`).
- [ ] Branch saiu de `develop`; território só `financas_bot_telegram/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/BE-18.md` com frontmatter válido. Anotar:
  - HTTP client escolhido (RestTemplate ou WebClient) + por quê.
  - Confirmação de que a restrição BR (130497) NÃO bloqueia esta task (gotcha pro próximo dev).

## Coordenação

- **BE-19 (entrada WhatsApp)** vai usar `WhatsAppMessageSenderService.enviarTexto` pro feedback ao usuário (§4.5).
- **BE-21b (notificação WhatsApp)** vai usar `enviarTemplate` quando o template for aprovado.
- **BE-19a** já dá idempotência; aqui só envio.
- **Telegram sender existente NÃO MUDA** — coexistência (ADR 0013).
- **Integração real com a Meta só na BE-20** — esta task entrega o adapter pronto contra mocks.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint na, testes verdes, branch, território), status report válido, e **revisão do Reviewer** — toca configuração de secrets e HTTP client externo. Abrir PR pra `develop`; não mergear sozinho.

## Referências

- `docs/architecture/adapter-whatsapp-cloud-api.md` §5 (sender + downloader).
- `docs/architecture/estado-atual.md` §3 (ver o padrão do Telegram sender existente — espelhar).
- ADR 0012 (provider escolhido = Cloud API oficial).
- `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md` (gotcha da restrição BR — não bloqueia esta task).
- `docs/runbooks/RUNBOOK-prep-wa-meta-cloud-api.md` Fase 7 (secrets esperados).
</content>
