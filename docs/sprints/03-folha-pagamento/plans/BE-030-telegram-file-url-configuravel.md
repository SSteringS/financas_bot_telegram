---
task: BE-030
titulo: "Tornar telegram.file.url configurável — pré-requisito WireMock E2E"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-04
branch_alvo: feature/be-030-telegram-file-url-configuravel
integration_branch: integration/03-folha-pagamento
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: [QA-011]
skills_dispatched: []
fluxos_qa: []
---

# BE-030 — Tornar `telegram.file.url` configurável (pré-requisito WireMock E2E)

---

## Intake

- **Origem:** ADR 0020 (`Proposed`, 2026-06-04) — decisão de usar WireMock standalone pra mockar o Telegram Bot API no E2E. A ADR identificou que `TelegramFileDownloaderService` tem a URL do `getFile` configurável mas a URL de download do arquivo está **hardcoded** em `"https://api.telegram.org/file/bot"`. Sem torná-la configurável, WireMock não consegue interceptar o download.
- **Por quê agora:** desbloqueia QA-011 Sub-área A (cenário foto+caption no E2E). Sem esta task, o caminho positivo mais importante do Telegram (foto com legenda cria pedido) fica sem cobertura E2E.
- **Esforço:** baixo (~30-45min). 1 arquivo Java modificado + 3 arquivos de properties. Zero lógica de negócio alterada — apenas parameterização de URL.
- **Riscos resumidos:** praticamente zero risco funcional — a URL com o default correto garante que produção não é afetada. Risco de o nome da propriedade não seguir o padrão relaxed binding do Spring (mitado com teste unitário).

---

## Contexto

`TelegramFileDownloaderService.java` realiza duas chamadas HTTP ao Telegram:

**Chamada 1 — `getFile`** (já configurável via `@Value`):
```java
@Value("${telegram.api.url}") String telegramApiUrl  // = https://api.telegram.org/bot
String getFileUrl = telegramApiUrl + botToken + "/getFile?file_id=" + fileId;
```

**Chamada 2 — download do arquivo** (hardcoded):
```java
String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
```

Propriedades existentes:
- `application.properties`: `telegram.api.url=https://api.telegram.org/bot`
- `application-dev.properties`: `telegram.api.url=https://api.telegram.org/bot`
- `application-dev.properties.example`: `telegram.api.url=https://api.telegram.org/bot`
- `application-prod.properties`: não tem `telegram.api.url` → herda do `application.properties` (default correto)

---

## Decisão / abordagem

Adicionar a propriedade `telegram.file.url` espelhando o padrão de `telegram.api.url`:

- **Default** (produção e dev normal): `https://api.telegram.org/file/bot`
- **Em E2E**: `http://localhost:8089/file/bot` (via env var `TELEGRAM_FILE_URL`, Spring Boot relaxed binding)

O `@Value` usa o default do Spring para garantir que produção não precise alterar `application-prod.properties`:

```java
@Value("${telegram.file.url:https://api.telegram.org/file/bot}") String telegramFileUrl
```

Construção da URL de download passa a ser:
```java
String downloadUrl = telegramFileUrl + botToken + "/" + filePath;
```

Nenhuma lógica de negócio, fluxo de parse ou tratamento de erro muda.

---

## Escopo / arquivos

### Modificar

**`TelegramFileDownloaderService.java`** (`adapters/out/telegram/service/`):
- Adicionar campo `private final String telegramFileUrl`.
- Adicionar parâmetro no construtor: `@Value("${telegram.file.url:https://api.telegram.org/file/bot}") String telegramFileUrl`.
- Substituir a linha hardcoded `String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;` por `String downloadUrl = telegramFileUrl + botToken + "/" + filePath;`.

**`application.properties`** (raiz dos resources):
- Adicionar: `telegram.file.url=https://api.telegram.org/file/bot`

**`application-dev.properties`**:
- Adicionar: `telegram.file.url=https://api.telegram.org/file/bot`

**`application-dev.properties.example`**:
- Adicionar: `telegram.file.url=https://api.telegram.org/file/bot`
- Adicionar comentário acima: `# Em E2E, sobrescrever com: TELEGRAM_FILE_URL=http://localhost:8089/file/bot`

### Não tocar

- `application-prod.properties` — não precisa de `telegram.file.url` explícito; o default do `@Value` garante o valor correto.
- Qualquer outra classe ou lógica de negócio.

---

## Testes

Atualizar o teste unitário existente `TelegramFileDownloaderServiceTest`:

- Verificar que o construtor injeta `telegramFileUrl` corretamente.
- Verificar que a URL de download usa o `telegramFileUrl` injetado (não a string hardcoded).
- Se ainda não houver teste de construtor / cenário de URL customizada, adicionar 1 caso que injeta `http://localhost:8089/file/bot` e verifica que a chamada HTTP usa essa base.

**`testes_novos` esperado:** 1-2 (ajuste no teste existente + 1 novo caso de URL customizada).
**`testes_total` esperado:** sem variação significativa no total do back.

---

## Critérios de aceitação

- [ ] `TelegramFileDownloaderService` injeta `telegram.file.url` via `@Value` com default `https://api.telegram.org/file/bot`.
- [ ] URL de download do arquivo usa `telegramFileUrl` (não hardcoded).
- [ ] `application.properties`, `application-dev.properties`, `application-dev.properties.example` têm `telegram.file.url` adicionado.
- [ ] `./mvnw test` verde. `TelegramFileDownloaderServiceTest` cobre o novo parâmetro.
- [ ] `./mvnw package -DskipTests` verde (build limpo).
- [ ] **Verificação de não-regressão:** aplicação sobe localmente com `application-dev.properties` e processa um webhook Telegram de texto puro sem erro (o fluxo foto+caption não precisa funcionar end-to-end — WireMock não está rodando; basta a URL não causar erro de startup).
- [ ] Zero mudança em lógica de negócio — diff deve ser só: 1 campo, 1 parâmetro de construtor, 1 linha de uso, 3-4 linhas de properties.
- [ ] Branch `feature/be-030-telegram-file-url-configuravel` saindo de `integration/03-folha-pagamento`.
- [ ] Status report `docs/sprints/03-folha-pagamento/status/BE-030-telegram-file-url-configuravel.md` com frontmatter válido.

---

## Fora de escopo

- Tornar `telegram.api.url` ainda mais configurável (já funciona).
- Configurar o WireMock em si ou os stubs JSON — é escopo do QA-011.
- Alterar `application-prod.properties` — não é necessário (default garante fallback correto).
- Testes de integração ou E2E — escopo do QA-011.
- Qualquer outra classe do adapter Telegram (`TelegramMessageSenderService`, `TelegramNotificadorImpl`) — fora; se precisar configurabilidade lá também, abre BE-NNN separado.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Default do `@Value` não funcionar em produção por ausência da propriedade | Muito Baixa | Alto | Default no próprio `@Value(":https://api.telegram.org/file/bot")` é comportamento padrão do Spring. Verificar se `application-prod.properties` herda corretamente na validação local. |
| Testes existentes hardcodam a URL de download e quebram | Baixa | Baixo | Revisar `TelegramFileDownloaderServiceTest` antes de commitar — se houver assert de URL, atualizar para usar o novo campo. |
| Drift entre `telegram.api.url` (inclui `/bot`) e `telegram.file.url` (inclui `/file/bot`) causar confusão | Baixa | Baixo | Comentário em `application.properties` explica o padrão. Planner registra em `PENDENCIAS-TECNICAS.md` se quiser refatorar pra base URL única no futuro. |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-009, QA-010 (territórios disjuntos — QA-009 é testes Java mas não toca `TelegramFileDownloaderService.java`; QA-010 é front).
- **Depende sequencialmente de:** nada.
- **Bloqueia:** QA-011 Sub-área A (foto+caption). Sub-áreas B, C e D do QA-011 podem começar antes.
- **Atenção pro Reviewer:** verificar que (a) zero lógica de negócio alterada; (b) `@Value` com default correto; (c) default em `application.properties` e `application-dev.properties` consistentes com o atual hardcoded.
- **Após merge:** planner notifica o QA-011 implementador que Sub-área A está desbloqueada.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, revisão do Reviewer (ADR 0005).

`fluxos_qa: []` — mudança de parameterização sem comportamento novo a validar via QA externo.

---

## Referências

- **ADR 0020** `docs/decisions/0020-mock-telegram-api-wiremock-e2e.md` — decisão que originou esta task.
- `financas_bot_telegram/src/main/java/.../adapters/out/telegram/service/TelegramFileDownloaderService.java` — arquivo principal.
- `financas_bot_telegram/src/main/resources/application.properties` + `application-dev.properties` + `application-dev.properties.example` — properties a atualizar.
- `financas_bot_telegram/src/test/java/.../adapters/out/telegram/service/TelegramFileDownloaderServiceTest.java` — teste a atualizar.
- **QA-011** `docs/sprints/03-folha-pagamento/plans/QA-011-expansao-e2e-cenarios-positivos.md` — task dependente (consumidora do WireMock).
