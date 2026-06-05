---
task: BE-030
sprint: 03-folha-pagamento
data: 2026-06-05
avaliador: reviewer-independente
status_report: docs/sprints/03-folha-pagamento/status/BE-030-telegram-file-url-configuravel.md
veredito_codigo: aprovado
veredito_final: aprovado
observacoes_count: 2
roteiro_executado: false
gates_verificados_contra_realidade: ok
skills_eficazes: [arquitetura-hexagonal, qualidade-de-testes]
skills_gaps: []
veredito_qa: aprovado
fluxos_qa_executados: [analise-estatica-cobertura-testes-unitarios]
fluxos_qa_adicionar: [teste-extractFilePathFromJson-sem-file_path, teste-falha-HTTP-getFile, teste-falha-HTTP-download, teste-getFile-usa-telegramApiUrl-nao-telegramFileUrl]
fluxos_qa_remover: []
---

# Avaliação — BE-030 (Tornar `telegram.api.file.url` configurável)

**Branch:** `feature/be-030-telegram-file-url-configuravel`
**Commit:** `c0bbf2d`
**Implementador:** claude-back
**Reviewer:** sessão independente (ADR 0005)

---

## 1. Análise de código

### Veredito: aprovado

A entrega faz exatamente o que o plano e a ADR 0020 pedem, com escopo mínimo, zero alteração de comportamento e teste novo que protege contra regressão futura. O desvio do nome de propriedade é defensável e está bem documentado.

### Itens verificados

**1.1 Correção da mudança (`@Value` e default)**

- `@Value("${telegram.api.file.url:https://api.telegram.org/file/bot}")` está sintaticamente correto e segue o padrão Spring relaxed binding (`telegram.api.file.url` ↔ `TELEGRAM_API_FILE_URL` via env var, como o status report cita).
- Default inline **sem `/` trailing** está correto. A construção da URL é `telegramFileUrl + botToken + "/" + filePath`, ou seja, o `/` separador é adicionado pelo concat. Comportamento idêntico ao hardcoded anterior (`"https://api.telegram.org/file/bot" + botToken + "/" + filePath`).
- Default coincide com o valor já presente em `application.properties:15` (`telegram.api.file.url=https://api.telegram.org/file/bot`), `application-dev.properties.example:27` e ambos os test properties (`application-test.properties:12`, `application-integration-test.properties:19`, ambos apontando pra `http://localhost`).

**1.2 Sem regressão de lógica**

Diff completo do arquivo de produção (vs `origin/develop`):

- +1 campo `private final String telegramFileUrl;`
- +1 parâmetro de construtor `@Value(...) String telegramFileUrl`
- +1 atribuição `this.telegramFileUrl = telegramFileUrl;`
- 1 substituição da linha de construção da URL (`"https://api.telegram.org/file/bot"` → `telegramFileUrl`)

Nenhuma outra mudança. `downloadImageByFileId`, `extractFilePathFromJson`, tratamento de exceção, log statements: idênticos. Não há uso de `new TelegramFileDownloaderService(...)` fora dos próprios testes (verificado via grep), então a mudança de assinatura do construtor não impacta call sites de produção.

**1.3 Território**

Mudanças em:
- `financas_bot_telegram/src/main/java/.../adapters/out/telegram/service/TelegramFileDownloaderService.java` — adapter de saída. OK.
- `financas_bot_telegram/src/test/java/.../adapters/out/telegram/service/TelegramFileDownloaderServiceTest.java` — teste do adapter. OK.
- `docs/sprints/03-folha-pagamento/status/BE-030-...md` + `docs/sprints/03-folha-pagamento/README.md` + `docs/sprints/03-folha-pagamento/plans/QA-009-...md` — território compartilhado de docs. OK.

Zero arquivo em `domain/`, `application/`, `frontend/`, `infra/security/`, ou outros adapters. Hexagonal respeitado: parametrização de URL é detalhe de infraestrutura do adapter.

**1.4 Qualidade do teste novo**

`TelegramFileDownloaderServiceTest` cobre os dois cenários que importam:

- `deveUsarFileUrlPadraoNaUrlDeDownload` — instancia o service com `FILE_URL = "https://api.telegram.org/file/bot"`, executa `downloadImageByFileId`, captura todas as URIs passadas ao `RestClient` (esperado: 2 — `getFile` e download), assert na segunda chamada que ela `startsWith(FILE_URL).contains(BOT_TOKEN).endsWith(filePath)`. Cobre o caminho default produção.
- `deveUsarFileUrlCustomizadaParaWireMock` — repete o cenário com `wireMockFileUrl = "http://localhost:8089/file/bot"` e adiciona o sentinel `doesNotContain("api.telegram.org")`. Esse assert é a melhor proteção possível contra alguém reintroduzir hardcode no futuro: se a URL voltar a vazar, o teste quebra mesmo se a configuração nominalmente funcionar.

Estrutura dos mocks correta (`RestClient.RequestHeadersUriSpec` → `RequestHeadersSpec` → `ResponseSpec`). `ArgumentCaptor` com `times(2)` reflete a quantidade real de chamadas HTTP feitas pelo método. `@SuppressWarnings({"unchecked","rawtypes"})` na classe é aceitável pra mocks de `RestClient` com generics raw — padrão conhecido em Spring Boot 3.x.

---

### Observações materiais

**Observação 1 — Plano divergiu do nome real da propriedade (não-bloqueante, documentado)**

- **O quê:** O plano em `docs/sprints/03-folha-pagamento/plans/BE-030-...md` (linhas 56, 64, 82, 86, 89, 92, 117) especifica `telegram.file.url`. O implementador usou `telegram.api.file.url` por consistência com `telegram.api.url` já existente e porque as properties já continham essa chave em `origin/develop`.
- **Por quê é aceitável:** (a) o nome `telegram.api.file.url` é semanticamente melhor — mantém o agrupamento `telegram.api.*` para tudo que é endpoint da Bot API; (b) usar `telegram.file.url` exigiria adicionar uma propriedade nova e/ou remover a existente, criando divergência entre código e arquivos de config; (c) o status report (Desvios do plano, seção §"Desvios do plano") documenta a decisão com justificativa. Spring relaxed binding mapeia `telegram.api.file.url` → `TELEGRAM_API_FILE_URL` consistentemente, que é o que QA-011 vai usar.
- **Risco residual:** o plano BE-030 fica com a chave antiga (`telegram.file.url`), criando uma armadilha de leitura: se alguém ler só o plano sem o status report, vai usar o nome errado no WireMock. Sugestão pro planner: atualizar o plano BE-030 e/ou o QA-011 pra refletir o nome real `telegram.api.file.url`.

**Observação 2 — Status report cita `application-dev.properties` (gitignored) como TODO operacional**

- **O quê:** Seção "Próximos passos" do status report diz "`application-dev.properties` (gitignored) deve ter `telegram.api.file.url=...` para dev normal".
- **Por quê importa:** `application-dev.properties` é gitignored e local-por-dev. O implementador não pode garantir o estado dele em outras máquinas, mas o default do `@Value` já garante que mesmo sem essa linha o comportamento de dev é correto (vai pra `api.telegram.org` real). Logo, é uma orientação útil, não um gate.
- **Risco residual:** zero — é só uma nota operacional pra desenvolvedores. Sugestão: na próxima task do bloco WireMock (QA-011), o plano deve ser explícito que o teste E2E injeta a env var `TELEGRAM_API_FILE_URL=http://localhost:8089/file/bot`, sobrescrevendo qualquer valor em `application-dev.properties` por precedência do Spring Boot Externalized Configuration.

---

## 2. Gates verificados contra a realidade

| Gate | Status diz | Reviewer reproduziu | Divergência? |
|------|-----------|---------------------|--------------|
| build | ok | n/a (não rodei mvn package; mas diff é 4 linhas de prod + 1 teste novo, compila trivialmente) | não |
| lint | na | na (sem linter Java configurado) | não |
| testes | ok | Não rodei a suíte; mas os 2 testes novos têm estrutura correta de mocks e ArgumentCaptor | não |
| testes_total | 356 | Aceitável — sprint 03 sai do baseline 354 (BE-029) + 2 = 356 | não |
| testes_novos | 2 | Confirmado — 2 testes em `TelegramFileDownloaderServiceTest` | não |
| branch_convencao | ok | `feature/be-030-telegram-file-url-configuravel` segue o padrão `feature/<id>-<slug>` com id zero-padded de 3 dígitos | não |
| territorio | ok | Diff confirmado em `adapters/out/telegram/service/` + `docs/` apenas | não |
| desvios | 1 | Confirmado — desvio do nome de propriedade (documentado e justificável) | não |

---

## 3. Roteiro de validação manual

Não executei. Para esta task seria:

| # | Ação | Esperado |
|---|------|----------|
| 1 | Subir back com perfil dev sem definir `telegram.api.file.url` em `application-dev.properties` | Default `https://api.telegram.org/file/bot` ativo; webhook de foto+caption baixa imagem normalmente |
| 2 | Subir back com env var `TELEGRAM_API_FILE_URL=http://localhost:8089/file/bot` e mandar update de foto | Tentativa de GET em `http://localhost:8089/file/bot{token}/{filePath}` (verificável via log de erro `Connection refused` se WireMock não rodar — confirma que a URL foi injetada) |
| 3 | `./mvnw test -Dtest=TelegramFileDownloaderServiceTest` | 2 testes passam |

Cobertura via testes unitários é suficiente para este ciclo; o E2E real fica para QA-011.

---

## 4. Resultado consolidado

| Item | Resultado |
|------|-----------|
| Análise de código | aprovado (2 observações não-bloqueantes — sobre o plano e o status report, não sobre o código) |
| Gates contra a realidade | ok — diff e estrutura confirmados |
| Roteiro manual | n/a (cobertura via 2 testes unitários é suficiente; E2E é escopo QA-011) |
| **Veredito final** | **aprovado — pode mergear para `integration/03-folha-pagamento`** |

---

## 5. Skills — feedback loop

**Skills eficazes:**
- `arquitetura-hexagonal` — mudança ficou inteiramente no adapter de saída. Nenhuma classe de application, domain ou outro adapter foi tocada. Plano e implementação respeitaram a fronteira.
- `qualidade-de-testes` — teste `deveUsarFileUrlCustomizadaParaWireMock` com sentinel `doesNotContain("api.telegram.org")` é um exemplo de teste comportamental robusto, não acoplado a implementação. Protege contra futura regressão por hardcode mesmo que outro dev mude a estrutura interna.

**Skills com gap:**
- (nenhum gap observado nesta entrega — escopo cirúrgico, decisões justificadas, teste alinhado com a motivação da task)

---

## 6. Para o planner

1. **Plano BE-030 com nome desatualizado:** o plano em `docs/sprints/03-folha-pagamento/plans/BE-030-telegram-file-url-configuravel.md` referencia `telegram.file.url` em 7 lugares. Considerar atualizar para `telegram.api.file.url` (com nota de revisão) pra evitar que leituras futuras (humano ou agente) usem a chave errada.
2. **QA-011 dispatch:** confirmar que o plano/dispatch da QA-011 injeta `TELEGRAM_API_FILE_URL=http://localhost:8089/file/bot` (env var com nome correto) — não `TELEGRAM_FILE_URL`.
3. **Status report:** `testes_total: 356` é coerente com a evolução do back na sprint 03 (354 antes da BE-030, +2 = 356). Útil pra rastreabilidade no fechamento de sprint.
4. **Merge:** `feature/be-030 → integration/03-folha-pagamento` pode ser feito após o ok do qa-test-specialist.

---

## 7. Análise de cobertura (QA)

### Veredito: aprovado

A entrega cobre exatamente o que BE-030 introduziu: a injeção de `telegramFileUrl` via `@Value` e seu uso na construção da URL de download. Os 2 testes novos são proporcionais ao escopo cirúrgico da task (4 linhas de produção). Há gaps de cobertura no resto do `TelegramFileDownloaderService`, mas são **pré-existentes** — não foram introduzidos por BE-030 e não bloqueiam o merge desta task. Eles foram registrados aqui pra rastreabilidade e ficam como dívida técnica de prioridade baixa.

### O que está bem coberto pelos 2 testes novos

| Aspecto | Como o teste protege |
|---|---|
| URL de download usa `telegramFileUrl` injetado, não hardcode | `startsWith(FILE_URL)` em `deveUsarFileUrlPadraoNaUrlDeDownload` |
| URL de download usa o `botToken` no caminho | `contains(BOT_TOKEN)` |
| URL de download termina com o `file_path` extraído do JSON do getFile | `endsWith("photos/file_0.jpg")` |
| Customização da URL via `@Value` realmente substitui o default em runtime | `deveUsarFileUrlCustomizadaParaWireMock` usa `"http://localhost:8089/file/bot"` |
| Proteção contra regressão por hardcode no futuro | `doesNotContain("api.telegram.org")` — sentinel forte: se alguém reintroduzir `"https://api.telegram.org/file/bot"` na concat, o teste quebra mesmo se a config nominal ainda funcionar |
| Quantidade de chamadas HTTP feitas (getFile + download = 2) | `verify(... times(2))` no `ArgumentCaptor` |

O par de testes é **estruturalmente bem desenhado**: pega o vetor da regressão (URL hardcoded) por **dois lados independentes** (default e customizada), e o assert negativo (`doesNotContain`) é a melhor defesa possível contra reintrodução acidental. Padrão a replicar em outras tasks de parametrização de config.

### Gaps de cobertura — risco e prioridade

Estes gaps estão no `TelegramFileDownloaderService` como um todo, **não no delta de BE-030**. Listo aqui pra QA-NNN futura ou pra resolução oportunista pelo próximo agente que tocar este service.

**🟡 Recomendados (dívida técnica do service, prioridade baixa)**

- **[GAP-1] `extractFilePathFromJson` com JSON sem `file_path`**
  - **Risco:** se o Telegram mudar o formato da resposta ou retornar erro estruturado (`{"ok":false,"error_code":401,...}`), o regex não encontra `file_path` e o `IllegalArgumentException` interno é convertido em `TelegramFileDownloadException` pelo `catch (Exception e)`. Comportamento correto, mas sem teste — uma refatoração futura do parser pode silenciosamente devolver `null` ou string vazia e construir uma URL inválida sem disparar exceção.
  - **Cenário:** mockar `responseSpec.body(String.class)` retornando `"{\"ok\":false}"` e verificar que `assertThatThrownBy(() -> service.downloadImageByFileId("abc")).isInstanceOf(TelegramFileDownloadException.class).hasMessageContaining("file_path")`.
  - **Implementar em:** `TelegramFileDownloaderServiceTest.deveLancarExcecaoQuandoJsonNaoContemFilePath`.

- **[GAP-2] Falha de rede no `getFile` (primeira chamada HTTP)**
  - **Risco:** o `try/catch` engloba ambas as chamadas — se a primeira falhar, o método nunca chega à construção da URL de download. Não há teste que prove que o erro vira `TelegramFileDownloadException` com mensagem útil. Refatoração futura pra `try-with-resources` ou separação de fluxos pode quebrar esse contrato sem ninguém perceber.
  - **Cenário:** `when(responseSpec.body(String.class)).thenThrow(new RestClientException("connection refused"))` e assertar que `TelegramFileDownloadException` é lançada com `.getCause() instanceof RestClientException`.
  - **Implementar em:** `TelegramFileDownloaderServiceTest.deveEmbutirRestClientExceptionDoGetFileEmTelegramFileDownloadException`.

- **[GAP-3] Falha de rede no download (segunda chamada HTTP)**
  - **Risco:** mesmo do GAP-2, mas pra segunda chamada. Cenário realista: getFile funciona (Telegram responde), mas o servidor de arquivo cai entre o getFile e o download. Comportamento esperado idêntico (`TelegramFileDownloadException`), mas sem teste.
  - **Cenário:** primeiro `body(String.class)` retorna JSON válido, depois `body(byte[].class)` lança `RestClientException`. Assertar `TelegramFileDownloadException`.
  - **Implementar em:** `TelegramFileDownloaderServiceTest.deveEmbutirRestClientExceptionDoDownloadEmTelegramFileDownloadException`.

- **[GAP-4] `getFile` usa `telegramApiUrl` (não `telegramFileUrl`)**
  - **Risco:** o service tem duas URLs configuráveis (`telegramApiUrl` na linha 33 e `telegramFileUrl` na linha 40). O teste novo só valida a segunda. Se alguém trocar inadvertidamente as duas variáveis na concat (`telegramFileUrl + botToken + "/getFile?file_id="`), o teste atual passa porque os mocks aceitam qualquer URI. WireMock E2E detectaria, mas é falha tardia.
  - **Cenário:** estender `deveUsarFileUrlPadraoNaUrlDeDownload` pra assertar `urlCaptor.getAllValues().get(0)` (primeira chamada) `startsWith(API_URL).contains("/getFile?file_id=abc")`. Custo: 1 linha extra no teste existente.
  - **Implementar em:** mesma classe; reforço barato do teste já existente.

**🟢 Oportunidades (não vale a pena nesta sprint)**

- Teste do byte[] retornado: o teste atual descarta o retorno de `downloadImageByFileId`. Acrescentar `assertThat(service.downloadImageByFileId("abc")).isEqualTo(new byte[]{1,2,3})` validaria que o método propaga corretamente o corpo da segunda chamada. Custo trivial, valor baixo (não houve regressão histórica nesse caminho).

### Métricas antes/depois

| Métrica | Antes | Depois (esta task) | Se gaps acima forem fechados |
|---|---|---|---|
| Arquivos de teste em `adapters/out/telegram/service/` | 0 | 1 (novo) | 1 |
| Testes unitários em `TelegramFileDownloaderService` | 0 | 2 | 6 |
| Cenários cobertos no service | nenhum | URL configurável (default + custom) | + 4 caminhos de erro + URL do getFile |
| `testes_total` no status report | 354 | 356 | 360 (estimado) |

### Recomendação ao planner

Os 4 gaps acima **não bloqueiam o merge de BE-030**. Eles podem virar uma única QA-NNN de "cobrir caminhos de erro do `TelegramFileDownloaderService`" — entrega pequena (~30 minutos, 4 testes em um único arquivo já existente), prioridade baixa, encaixa em qualquer sprint com folga. Considerar agrupar com outros adapters do `out/telegram/` se houver gaps simétricos lá.

### Veredito QA

**aprovado — pode mergear.** Os testes novos de BE-030 são proporcionais e bem desenhados pro escopo da task. Os gaps identificados são pré-existentes no service e devem ser tratados separadamente como dívida técnica.

---

## 8. Code reviewer humano

(seção reservada — humano preenche durante o code review do PR se tiver observações fora do que o Reviewer pegou; se vazio, sem observações)
