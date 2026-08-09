---
task: QA-009
titulo: "Cobertura de testes backend — fechar gaps críticos de unit + integration"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-04
branch_alvo: feature/qa-009-cobertura-testes-backend
integration_branch: integration/03-folha-pagamento
prioridade: alta
esforco: alto
territorio: back
estado: concluido
depende_de: [QA-008, FIX-005]
bloqueia: []
skills_dispatched: [qualidade-de-testes, seguranca-backend]
fluxos_qa: []
---

# QA-009 — Cobertura de testes backend (controllers + services + adapters + integration)

> **Status: pronto-pra-execucao.** FIX-005 back mergeada (PR #109, 2026-06-05) — URLs corretas em `@RequestMapping`, JWT allowlist ativo. Sub-área A usa `/api/v1/funcionarios/**`; Sub-área D inclui autenticação via `postAutenticado()`/`getAutenticado()`. Ver §"Decisão / abordagem" para ajustes de URL vs plano original.

> **Atualização pós-merge do FIX-005 (planner faz):** remover a nota "não escrever testes 401/403" da Sub-área A (linhas abaixo) e adicionar cenários 401/403 explicitamente. Integration tests da Sub-área D devem chamar `autenticarComo()` em `@BeforeEach`.

---

## Intake

- **Origem:** análise de gaps de teste executada pelo qa-test-specialist em 2026-06-04, depois que a suíte E2E ficou estável (QA-008). Inventário de produção vs testes revelou ~12 gaps críticos no back. Esta task consolida **todos os gaps de back** num único bundle (Opção B do levantamento — granularidade média).
- **Por quê agora:** depois da EVO-09 (folha de pagamento, sprint 03), o produto cresceu mais rápido que a cobertura. `FolhaController` tem 7 endpoints sem teste de controller; adapters de saída do Telegram inteiros estão em 0% de cobertura. Risco de regressão silenciosa no canal mais importante do produto.
- **Esforço:** alto (~10-12h, ~1.5 dia). 11 arquivos de teste novos, distribuídos em 4 sub-áreas (controllers, services, adapters, integration). Cada sub-área pode ser commit separado pra facilitar review.
- **Riscos resumidos:** zero risco funcional (não toca código de produto). Risco de drift na execução por escopo grande — mitigado pela divisão em sub-áreas e pelo padrão referencial claro de cada tipo de teste (vide §"Decisão / abordagem").

---

## Contexto

Inventário cruzado (produção × teste) identificou os seguintes gaps no back:

### Sub-área A — Controllers REST

`FolhaController` (`adapters/in/rest/folha/FolhaController.java`) — 7 endpoints sem teste:
- `POST /api/v1/funcionarios/{id}/vales` — cadastrar vale
- `GET  /api/v1/funcionarios/{id}/vales?mes=YYYY-MM` — listar vales
- `POST /api/v1/funcionarios/{id}/adiantamentos` — cadastrar adiantamento
- `GET  /api/v1/funcionarios/{id}/adiantamentos` — listar adiantamentos
- `DELETE /api/v1/funcionarios/adiantamentos/{adiantamentoId}` — cancelar
- `POST /api/v1/funcionarios/{id}/fechamentos` — fechar mês
- `GET  /api/v1/funcionarios/{id}/fechamentos` — listar fechamentos

> **Atualização FIX-005 (2026-06-05):** URLs migradas para `/api/v1/funcionarios/**`. JWT allowlist ativo — endpoints agora **requerem** autenticação. Incluir cenários 401 por request sem cookie (usar MockMvc sem `Cookie` header).

### Sub-área B — Services unitários

`AtualizarFuncionarioServiceImpl` (`application/services/AtualizarFuncionarioServiceImpl.java`) — única implementação de service sem teste no back inteiro. Lógica de atualização de funcionário sem cobertura.

### Sub-área C — Adapters de saída

**Persistence (gaps):**
- `AdiantamentoRepositoryAdapter` — sem teste. Query custom `findAtivosParaFuncionario` sem validação.
- `FuncionarioRepositoryAdapter` — sem teste.

**Telegram (gaps de 100%):**
- `TelegramFileDownloaderService` — caminho crítico do registro de pagamento via foto (baixa mídia do Telegram). Sem teste = falha silenciosa possível.
- `TelegramMessageSenderService` — responde mensagens ao usuário no canal Telegram.
- `TelegramNotificadorImpl` — notificações disparadas por eventos de domínio.

**Assimetria observada:** o canal WhatsApp tem `WhatsAppMediaDownloaderServiceTest` e `WhatsAppMessageSenderServiceTest` cobrindo os equivalentes; o Telegram não. Coerência desejada: paridade de cobertura entre canais.

**Infra security (1 gap):**
- `Sha256HashService` (`infra/security/Sha256HashService.java`) — usado para hash de tokens de auth. Sem teste.

### Sub-área D — Integration tests (folha de pagamento)

A sprint 03 (EVO-09) entregou os fluxos de folha, mas só `FecharMesIntegrationTest` foi criado. Faltam:
- `ValeIntegrationTest` — fluxo HTTP+DB completo do POST/GET de vales.
- `AdiantamentoIntegrationTest` — POST + cancelamento + listagem com banco real.
- `FuncionarioCRUDIntegrationTest` — cadastrar/atualizar/listar funcionário ponta a ponta.

---

## Decisão / abordagem

Adotar **paridade com testes existentes** como guia de estilo. Cada gap tem ao menos um equivalente já no repo que serve de modelo direto.

### Sub-área A — `FolhaControllerTest`

Modelo referencial: `FuncionarioControllerTest.java` (mesmo padrão de mock MockMvc + ObjectMapper).

Estrutura mínima por endpoint (7 endpoints × ~4-5 cenários = ~30 testes):
- **Happy path** — 200/201 com body válido + Location header onde aplicável.
- **Validação de input** — 400 com body inválido (campos obrigatórios faltando, valor negativo onde proibido).
- **Recurso não encontrado** — 404 quando funcionário não existe (apenas para endpoints com `{id}`).
- **Parsing de `mes`** — 400 quando `mes` fora do formato `YYYY-MM` em `listarVales`/`listarFechamentos`.
- **401 sem autenticação** — request sem cookie JWT retorna 401 (FIX-005 tornou esses endpoints protegidos). Usar `mockMvc.perform()` sem `Cookie` header.

> **Remover** o comentário de pendência JWT que estava planejado — FIX-005 resolveu. O `FolhaControllerTest` agora testa 401 como cenário normal.

### Sub-área B — `AtualizarFuncionarioServiceImplTest`

Modelo referencial: `CadastrarFuncionarioServiceImplTest.java`. Mocks JUnit + Mockito.

Cenários mínimos:
- Atualização válida persiste e retorna entidade atualizada.
- Funcionário inexistente lança a exceção de domínio correta.
- Validação de invariantes do domínio (campos obrigatórios, valores válidos).

### Sub-área C — Adapters

**Persistence (Adiantamento + Funcionario):**
- Modelo referencial: `PedidoPagamentoRepositoryAdapterTest.java`.
- Padrão: usar `@DataJpaTest` ou Testcontainers conforme convenção do repo (ver `ComprovanteRepositoryAdapterTest.java`).
- Cenários por método público do adapter: salvar, buscar, listar, query custom.

**Telegram (FileDownloader + MessageSender + Notificador):**
- Modelo referencial: `WhatsAppMediaDownloaderServiceTest.java` e `WhatsAppMessageSenderServiceTest.java`.
- Padrão: mock do client HTTP (RestClient/WebClient/Telegram Bot API client conforme o que cada service usa).
- Cenários por service:
  - Sucesso (resposta 200 do Telegram, fluxo nominal).
  - Erro HTTP do Telegram (timeout, 5xx) — confirmar exceção do domínio (`TelegramFileDownloadException` ou similar).
  - Input inválido (ex: `file_id` vazio).

**Sha256HashService:**
- Modelo referencial: `JwtServiceTest.java` (mesma área).
- Cenários: hash de string conhecida produz output esperado (vetor de teste); strings diferentes produzem hashes diferentes; idempotência (mesmo input → mesmo output).

### Sub-área D — Integration tests folha

Modelo referencial: `FecharMesIntegrationTest.java`. Herda `AbstractIntegrationTest`, sobe Testcontainers MySQL.

> **Atualização FIX-005 (2026-06-05):** todos os endpoints da folha agora requerem JWT. Usar `postAutenticado()` / `getAutenticado()` / `deleteAutenticado()` de `AbstractIntegrationTest` (adicionados em FIX-005). Padrão: `String cookie = autenticarComo(99L)` no `@BeforeEach`.

Por suite:
- **ValeIntegrationTest** — POST vale + GET listagem filtrada por mês com `postAutenticado`/`getAutenticado`. URLs: `/api/v1/funcionarios/{id}/vales`. Asserções de banco (`pedidos_pagamento` com `tipo_categoria=VALE` e `mes_referencia` corretos).
- **AdiantamentoIntegrationTest** — POST adiantamento → entidade `adiantamento` + parcelas em `pedidos_pagamento` criadas. DELETE cancela (`deleteAutenticado`). GET lista só ativos. URLs: `/api/v1/funcionarios/{id}/adiantamentos`.
- **FuncionarioCRUDIntegrationTest** — POST cria + GET busca por id + PUT atualiza. Usar `postAutenticado`/`getAutenticado`. URLs: `/api/v1/funcionarios/**`.

---

## Escopo / arquivos

### Criar

**Controllers:**
- `financas_bot_telegram/src/test/java/.../adapters/in/rest/folha/FolhaControllerTest.java`

**Services:**
- `financas_bot_telegram/src/test/java/.../application/services/AtualizarFuncionarioServiceImplTest.java`

**Adapters out persistence:**
- `financas_bot_telegram/src/test/java/.../adapters/out/persistence/AdiantamentoRepositoryAdapterTest.java`
- `financas_bot_telegram/src/test/java/.../adapters/out/persistence/FuncionarioRepositoryAdapterTest.java`

**Adapters out telegram:**
- `financas_bot_telegram/src/test/java/.../adapters/out/telegram/service/TelegramFileDownloaderServiceTest.java`
- `financas_bot_telegram/src/test/java/.../adapters/out/telegram/service/TelegramMessageSenderServiceTest.java`
- `financas_bot_telegram/src/test/java/.../adapters/out/telegram/notificador/TelegramNotificadorImplTest.java`

**Infra security:**
- `financas_bot_telegram/src/test/java/.../infra/security/Sha256HashServiceTest.java`

**Integration tests:**
- `financas_bot_telegram/src/test/java/.../integration/ValeIntegrationTest.java`
- `financas_bot_telegram/src/test/java/.../integration/AdiantamentoIntegrationTest.java`
- `financas_bot_telegram/src/test/java/.../integration/FuncionarioCRUDIntegrationTest.java`

**Total:** 11 arquivos de teste novos.

### Não tocar

- **Zero código de produção** — esta task é puramente cobertura. Se durante a escrita de teste o implementador descobrir bug no produto, **abrir FIX-NNN separado** em vez de corrigir no mesmo PR (mantém esta task de "puramente cobertura" rastreável).
- **Pendência de JWT em `/api/funcionarios/**`** — está em `PENDENCIAS-TECNICAS.md`. Esta task documenta o estado atual (sem auth), não tenta corrigir.
- Testes existentes não precisam ser modificados, exceto se houver flakiness identificada — neste caso, **registrar em pendência** e seguir.

---

## Testes

A própria task entrega testes. Estimativa:

- **Sub-área A** (FolhaControllerTest): ~25 testes (7 endpoints × 3-4 cenários).
- **Sub-área B** (AtualizarFuncionarioServiceImplTest): ~4-6 testes.
- **Sub-área C** (6 testes de adapter/service/infra): ~3-4 testes cada = ~20-25 testes.
- **Sub-área D** (3 integration tests folha): ~3-5 testes cada = ~10-15 testes.

**`testes_novos` esperado:** 60-70 testes.
**`testes_total` esperado pós-task:** ~120-130 (era ~59 antes — back inteiro deve quase dobrar).

Cobertura JaCoCo alvo nas classes alvo:
- `FolhaController` ≥ 80%
- Adapters Telegram out ≥ 70% cada
- Services e adapters persistence ≥ 80% cada

---

## Critérios de aceitação

- [ ] 11 arquivos de teste criados nos paths declarados em §"Escopo / arquivos".
- [ ] `./mvnw test` verde com `testes_total` ≥ 415 (354 pré-task + ~60 novos) e `testes_novos` ≥ 55.
- [ ] `./mvnw test -Dtest=*IntegrationTest` verde — todos os integration tests incluindo os 3 novos da folha.
- [ ] JaCoCo report mostra:
  - `FolhaController` ≥ 80% line coverage
  - `TelegramFileDownloaderService`, `TelegramMessageSenderService`, `TelegramNotificadorImpl` cada ≥ 70%
  - `AdiantamentoRepositoryAdapter`, `FuncionarioRepositoryAdapter` cada ≥ 80%
  - `Sha256HashService` ≥ 90%
- [ ] `FolhaControllerTest` inclui cenário `401` por request sem cookie JWT em pelo menos 2 endpoints (validação que FIX-005 está ativo). **Sem** comentário de pendência JWT — essa pendência foi resolvida.
- [ ] `ValeIntegrationTest`, `AdiantamentoIntegrationTest`, `FuncionarioCRUDIntegrationTest` usam `postAutenticado()`/`getAutenticado()`/`deleteAutenticado()` de `AbstractIntegrationTest`.
- [ ] Todas as URLs nos novos testes usam `/api/v1/funcionarios/**` (não `/api/funcionarios/**`).
- [ ] Zero mudanças em código de produção (`src/main/java/`). Se algum bug for descoberto, **abrir FIX-NNN separado**.
- [ ] Branch `feature/qa-009-cobertura-testes-backend` saindo de `integration/03-folha-pagamento`.
- [ ] Status report `docs/sprints/03-folha-pagamento/status/QA-009-cobertura-testes-backend.md` com frontmatter válido. `testes_novos` ≥ 55; corpo lista por sub-área quantos testes foram adicionados.

---

## Fora de escopo (explicitamente)

- **FIX-005 front** (atualizar URLs de `/api/funcionarios/**` para `/api/v1/funcionarios/**` no front) — task separada pendente, não é desta task.
- **Refatorar `parseMes`/`parseMesYearMonth` duplicado** em `FolhaController` — pendência declarada, BE-NNN separado, esta task não toca produto.
- **Refatorar `DataIntegrityViolationException` em FecharMesServiceImpl** — pendência declarada, BE-NNN separado.
- **Testes de carga** — fora.
- **Security scan automatizado (OWASP ZAP)** — descopado pelo princípio geral (audiência fechada).
- **Mapeamento exaustivo de DTOs/value objects** — vários DTOs já têm teste (`AuthExchangeRequestTest`, etc.); criar testes para outros DTOs novos da folha (`ValeRequest`, `AdiantamentoRequest`, `FecharMesRequest`) só se faltarem campos validados — implementador decide caso a caso, não é critério duro.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Escopo de 11 arquivos drift — task vira saco-de-gatos | Média | Médio | Sub-áreas A/B/C/D são commits separados; cada commit fecha sozinho; review pode aprovar incremental |
| Implementador querer "aproveitar" e mudar código de produto | Baixa | Alto (quebra rastreabilidade) | Critério explícito proíbe; reviewer audita; bug encontrado vira FIX-NNN |
| Padrão de teste inconsistente entre sub-áreas | Média | Médio | Cada sub-área tem **modelo referencial** declarado em §"Decisão / abordagem"; reviewer compara |
| Testes de adapter Telegram complexos demais por causa do mock da lib externa | Média | Médio | Modelo é o WhatsApp equivalente — se WhatsApp conseguiu mockar, Telegram consegue |
| Integration tests folha demoram demais (Testcontainers) | Baixa | Baixo | Reusar `AbstractIntegrationTest` (subir banco 1x por suite) |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-010 (front — território totalmente disjunto), tasks BE-* de feature que não toquem em arquivos de teste novos (vide §"Escopo").
- **Depende sequencialmente de:** QA-008 (suíte E2E estável — pra confiar nos testes de regressão). Já mergeada.
- **Bloqueia:** confiança operacional na cobertura de back. Sem esta task, o gate `mvn test` no PR não captura regressão em fluxos novos.
- **Atenção pro Reviewer:** verificar que (a) cada arquivo segue o modelo referencial citado; (b) zero diff em `src/main/java/`; (c) cobertura JaCoCo bate o critério na classe alvo; (d) comentário sobre pendência JWT presente no FolhaControllerTest.
- **Após merge:** atualizar memória do qa-test-specialist (`project_estado_testes_2026-06-01.md`) com nova baseline (`testes_total` de back).

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, revisão do Reviewer.

`fluxos_qa: []` — task de tooling pura, sem fluxo de produto a validar via QA externo.

---

## Referências

- Análise de gaps original — conversa qa-test-specialist com humano em 2026-06-04 (sessão pós-QA-008).
- `docs/PENDENCIAS-TECNICAS.md` — pendências declaradas (JWT em folha, duplicação parseMes, DataIntegrityViolationException).
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` — pirâmide de testes do projeto.
- Modelos referenciais:
  - `financas_bot_telegram/src/test/.../adapters/in/rest/funcionario/FuncionarioControllerTest.java` (para FolhaControllerTest)
  - `financas_bot_telegram/src/test/.../adapters/out/whatsapp/service/WhatsAppMediaDownloaderServiceTest.java` (para TelegramFileDownloaderServiceTest)
  - `financas_bot_telegram/src/test/.../adapters/out/whatsapp/service/WhatsAppMessageSenderServiceTest.java` (para TelegramMessageSenderServiceTest)
  - `financas_bot_telegram/src/test/.../adapters/out/persistence/PedidoPagamentoRepositoryAdapterTest.java` (para os 2 adapters persistence)
  - `financas_bot_telegram/src/test/.../application/services/CadastrarFuncionarioServiceImplTest.java` (para AtualizarFuncionarioServiceImplTest)
  - `financas_bot_telegram/src/test/.../infra/security/JwtServiceTest.java` (para Sha256HashServiceTest)
  - `financas_bot_telegram/src/test/.../integration/FecharMesIntegrationTest.java` (para os 3 integration tests folha)
