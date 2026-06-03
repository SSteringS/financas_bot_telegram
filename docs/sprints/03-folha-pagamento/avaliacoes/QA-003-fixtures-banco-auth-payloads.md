---
task: QA-003
sprint: 03-folha-pagamento
data: 2026-06-03
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/QA-003-fixtures-banco-auth-payloads.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado_com_observacoes
observacoes_count: 3
roteiro_executado: false
gates_verificados_contra_realidade: divergente
skills_eficazes: []
skills_gaps: []
---

# Avaliacao — QA-003 Fixtures banco + auth + global-setup + payloads-telegram

**Branch:** `feature/qa-003-fixtures-banco-auth-payloads`
**Implementador:** claude-front
**Plano:** `docs/sprints/03-folha-pagamento/plans/QA-003-fixtures-banco-auth-payloads.md`
**Status report:** `docs/sprints/03-folha-pagamento/status/QA-003-fixtures-banco-auth-payloads.md`

---

## 1. Analise de codigo (Reviewer le o diff)

O diff inclui 7 arquivos modificados no commit `57c493c`: 4 fixtures novas, remocao do `.gitkeep`, correcao do `tsconfig.json`, e o status report. Nenhum arquivo fora de `frontend/` ou `docs/sprints/03-folha-pagamento/status/` foi tocado.

### Prioridade 1 — SQL safety (CRITICO): verificado, sem vulnerabilidade

Todos os DELETEs em `banco.ts` foram lidos linha a linha:

- `DELETE FROM comprovantes WHERE pedido_id IN (SELECT id FROM pedidos_pagamento WHERE requisitante_id = 99)` — correto. A subquery e hardcoded; nenhum parametro externo entra na query. A cascata e necessaria porque `comprovantes` nao tem coluna propria `requisitante_id`.
- `DELETE FROM pedidos_pagamento WHERE requisitante_id = 99` — correto. Filtro direto.
- `DELETE FROM auth_token WHERE requisitante_id = 99` — correto. Filtro direto.

Tres DELETEs, tres filtros em `requisitante_id = 99`. Nenhum DELETE toca `requisitante_id != 99`. Gate critico: aprovado.

### Prioridade 2 — Contrato de auth: resolvido corretamente

A avaliacao de QA-001 identificou colisao de nomenclatura entre `E2E_ADMIN_KEY` (arquitetura) e `E2E_ADMIN_SECRET` (plano/runbook), e marcou como bloqueante para QA-003. A entrega de QA-003 resolveu pelo nome `E2E_ADMIN_SECRET` — alinhado com `.env.e2e.example` (linha 11) e com `ROTEIRO-E2E.md` (linha 42). O codigo em `auth.ts` le `process.env['E2E_ADMIN_SECRET']` e usa `'X-Admin-Key': ADMIN_SECRET` no header — que e o header correto validado pelo `AdminController.java` (linha 34: `@RequestHeader(value = "X-Admin-Key")`). Contrato front-back coerente, sem drift.

O `desenho-testes-automatizados.md` ainda usa `E2E_ADMIN_KEY` (linhas 451 e 556), mas e documentacao de arquitetura fora do escopo desta task — o planner pode corrigir separadamente.

### Prioridade 3 — Desvio 1 (mensagem_processada): justificativa solida

Verificado contra `V4__criar_mensagem_processada.sql`: a tabela tem `canal VARCHAR(20)` e `id_externo VARCHAR(255)`, sem coluna `telegram_user_id`. A spec do arquiteto que mostrava `DELETE WHERE telegram_user_id = '99'` estava incorreta — a coluna nunca existiu. A justificativa do implementador e tecnicamente correta: `update_id` unico por run elimina colisao entre execucoes distintas. A UNIQUE constraint `(canal, id_externo)` ja garante idempotencia no banco se o mesmo `update_id` for processado novamente.

Risco residual menor: se dois workers Playwright carregarem `payloads-telegram.ts` no mesmo milissegundo, `_updateCounter = Date.now()` geraria o mesmo valor inicial. Na configuracao atual do `playwright.config.ts` os testes rodam sequencialmente; e improvavel na pratica. Nao e bloqueante, mas QA-004 deve documentar se `fullyParallel` for ativado.

### Prioridade 4 — Desvio 2 (tsconfig allowImportingTsExtensions): correto

Verificado contra `frontend/tsconfig.app.json`: o pai ja tem `allowImportingTsExtensions: true`. O QA-001 havia sobrescrito para `false` no tsconfig e2e, causando erro `TS5097` ao importar arquivos `.ts` com extensao explicita. O QA-003 restaurou para `true`, consistente com o pai e necessario para o padrao ESM com `noEmit: true`. A flag nao afeta o build de producao (que usa `tsconfig.app.json`, nao `e2e/tsconfig.json`). Correto.

### Prioridade 5 — Type assertions sem validacao

Dois `as` identificados:

1. `auth.ts` linha 51: `(await conviteRes.json()) as { url: string }`. Sem validacao de runtime. Se o back retornar shape diferente, o erro so apareceria no `new URL(url)` com mensagem pouco descritiva. O guard `if (!token)` subsequente captura o caso de URL invalida, mas nao captura `url` undefined. Observacao menor para contexto de test helper.

2. `banco.ts` linha 150: `return rows as T[]`. Padrao aceitavel para query generica de asercoes — o chamador e quem define `T`. Documentado no JSDoc. Aceitavel.

### Verificacoes adicionais

**Schema de banco vs migrations V1-V6:** todos os campos usados no INSERT de `semearPedidos` verificados contra as migrations:
- `requisitante_id` (V2, NOT NULL DEFAULT 1), `data_pedido DATE NOT NULL` (V2 — fornecido), `tipo ENUM('BOLETO','PIX','TED','AGENDAMENTO','OUTRO')` (V2 — tipos corretos no `PedidoFixture`), `data_criacao DATETIME` (V1 — preenchido com `NOW()`), `status VARCHAR(50)` (V1 — valores 'PENDENTE'/'PAGO' usados pelo back). Colunas V6 (`categoria`, `funcionario_id`) sao NULL opcionais; a constraint `chk_pedido_folha CHECK (categoria IS NULL OR ...)` passa com NULL. Schema consistente.

**INSERT de requisitante:** inclui `canal_preferido = 'TELEGRAM'` adicionado na V5. Correto.

**Endpoint admin:** `POST /admin/api/v1/requisitantes/99/convite` em `auth.ts` bate com `@RequestMapping("/admin/api/v1")` + `@PostMapping("/requisitantes/{id}/convite")` no `AdminController.java`. Sem drift.

**Response de convite:** `{ url: string }` em `auth.ts` bate com o record `GerarConviteResponse(String url)`. Correto.

**Singleton de conexao:** `_conn` e module-level; cada worker Node.js (processo separado do Playwright) tem seu proprio modulo e abre sua propria conexao. O `fecharConexao()` no `global-setup.ts` libera o socket apos o setup. Correto.

---

### Veredito de codigo: aprovado com observacoes

A implementacao e tecnicamente solida no que importa: SQL safety intacto, contrato auth correto, schema verificado, territorio respeitado. As tres observacoes abaixo sao menores — nenhuma e bloqueante para o merge.

### Observacoes materiais

**Observacao 1 — Hash no status report nao corresponde ao commit da branch**

- **O que:** O frontmatter do status report declara `commits: - e4ed7e5`. O commit na ponta da branch e `57c493c`. O `e4ed7e5` existe no repositorio mas nao e ancestral da branch atual — e um commit abandonado de uma iteracao anterior de rebase/amend. A cadeia foi: `3ae6d3e` (original) → amend → `e4ed7e5` → amend → `57c493c` (atual). Cada amend atualizou o hash no status report mas o ultimo amend (`57c493c`) nao o atualizou de `e4ed7e5` para o proprio hash.
- **Onde:** `docs/sprints/03-folha-pagamento/status/QA-003-fixtures-banco-auth-payloads.md` linha 18.
- **Por que importa:** Rastro de auditoria incorreto. Um script que parseia o frontmatter e tenta `git show e4ed7e5` obtem um commit de conteudo ligeiramente diferente (status report com hash ainda mais antigo). Nao e regressao funcional — o diff real da branch esta correto — mas viola o contrato de rastreabilidade do schema de status.
- **Sugestao:** Corrigir para `- 57c493c` antes do merge, ou aceitar como divergencia documentada.

**Observacao 2 — Type assertion sem guard em `auth.ts` linha 51**

- **O que:** `(await conviteRes.json()) as { url: string }` nao valida o shape em runtime. Se o back retornar `{ "link": "..." }` ou shape vazio, `url` sera `undefined` e `new URL(url)` lancara `TypeError: Failed to construct 'URL': Invalid URL` — mensagem que nao cita o passo da autenticacao onde falhou.
- **Onde:** `frontend/e2e/fixtures/auth.ts` linha 51.
- **Por que importa:** Diagnostico de falha fica obscuro quando a API muda de contrato. O guard `if (!token)` captura URL invalida mas nao `url` undefined.
- **Sugestao:** Adicionar guard `if (!url || typeof url !== 'string') throw new Error('Admin API retornou shape inesperado (campo url ausente)')` antes de `new URL(url)`. Baixa urgencia para helper de teste.

**Observacao 3 — desenho-testes-automatizados.md ainda usa E2E_ADMIN_KEY**

- **O que:** O documento `docs/architecture/desenho-testes-automatizados.md` linhas 451 e 556 referencia `E2E_ADMIN_KEY`. A nomenclatura canonizada pela implementacao e `E2E_ADMIN_SECRET`.
- **Onde:** `docs/architecture/desenho-testes-automatizados.md` linhas 451 e 556 (fora do diff desta task — territorio do planner).
- **Por que importa:** Divergencia documental. Quem ler a arquitetura e montar `.env.e2e` baseado nela vai escrever a variavel errada. O `ROTEIRO-E2E.md` (linha 42) ja usa `E2E_ADMIN_SECRET` — a arquitetura ficou desatualizada.
- **Sugestao:** Planner atualiza `desenho-testes-automatizados.md` para `E2E_ADMIN_SECRET`. Nao e bloqueante para o merge desta task.

---

## 2. Gates verificados contra a realidade

| Gate | Status diz | Reviewer reproduziu | Divergencia? |
|---|---|---|---|
| build | ok | ok (`npm run build` — exit 0, vite 4.49s, sem erros TS) | nao |
| lint | ok | ok (`npm run lint` — sem output de erros, exit 0) | nao |
| testes | ok (61 total, 0 novos) | ok (61 passed, 12 arquivos; DOMException pre-existente no happy-dom) | nao |
| branch_convencao | ok | ok (`feature/qa-003-fixtures-banco-auth-payloads`; `origin/integration/03-folha-pagamento` e ancestral — verificado com `merge-base --is-ancestor`) | nao |
| territorio | ok | ok (apenas `frontend/e2e/fixtures/`, `frontend/e2e/tsconfig.json`, `docs/sprints/03-folha-pagamento/status/`) | nao |
| commit hash no frontmatter | `e4ed7e5` | commit real na branch e `57c493c`; `e4ed7e5` existe mas nao e ancestral | **sim** |

**Divergencia:** `gates_verificados_contra_realidade: divergente` pelo hash incorreto no frontmatter (Observacao 1). Todos os gates funcionais (build/lint/testes/branch/territorio) estao corretos e reproduzidos.

---

## 3. Roteiro de validacao manual

Nao aplicavel: task de infraestrutura de teste. Nenhuma UI ou comportamento visual. Os criterios de aceitacao funcionais (SQL safety, contrato auth, schema de banco) foram verificados via leitura de codigo e diff de migrations. Os criterios que requerem banco+backend rodando (idempotencia de `garantirRequisitanteE2E`, login real, preservacao de dados do Pedro) estao explicitamente diferidos para o smoke de QA-004, conforme declarado no plano e no status report.

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Analise de codigo | aprovado com observacoes |
| Gates contra a realidade | divergente (hash no frontmatter — nao funcional) |
| Roteiro manual | n/a (infraestrutura de teste; sem UI) |
| **Veredito final** | **APROVADO COM RESSALVAS** |

**Bloqueantes:** nenhum.

**Ressalvas (nao bloqueam merge; idealmente tratadas antes ou durante QA-004):**
1. Hash no frontmatter do status report incorreto (`e4ed7e5` declarado, `57c493c` e o real) — corrigir para rastreabilidade ou aceitar como divergencia documentada.
2. Type assertion sem guard em `auth.ts` linha 51 — diagnostico de falha obscuro se o back mudar shape.
3. `desenho-testes-automatizados.md` ainda usa `E2E_ADMIN_KEY` — planner deve sincronizar.

---

## 5. Skills — feedback loop

Sem observacao de skills nesta task. Task de infraestrutura de teste (TypeScript/Node.js); nenhuma camada hexagonal da arquitetura back-end envolvida.

`skills_eficazes: []`, `skills_gaps: []`

---

## 6. Para o planner (proximos passos)

1. **Corrigir hash no status report (opcional pre-merge):** `docs/sprints/03-folha-pagamento/status/QA-003-fixtures-banco-auth-payloads.md` linha 18 — substituir `e4ed7e5` por `57c493c`. Pode ser feito como commit de fixup no proprio status report ou aceito como divergencia conhecida.

2. **Sincronizar `desenho-testes-automatizados.md`:** atualizar linhas 451 e 556 de `E2E_ADMIN_KEY` para `E2E_ADMIN_SECRET`. Territorio do planner.

3. **QA-004 pode iniciar:** as 4 fixtures estao corretas e o contrato auth e coerente. A validacao de integracao real (idempotencia, login, preservacao de dados do Pedro) deve acontecer no smoke de QA-004.

4. **Edge case de workers paralelos:** se QA-004 ativar `fullyParallel: true` no Playwright, documentar o risco de colisao de `update_id` entre workers (ambos inicializados no mesmo milissegundo) ou substituir `Date.now()` por `Date.now() * 1000 + workerId`.

5. **Nenhuma pendencia de FIX separado:** as ressalvas sao todas de documentacao ou melhoria defensiva menor.
