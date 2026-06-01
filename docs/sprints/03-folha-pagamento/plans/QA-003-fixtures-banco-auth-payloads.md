---
task: QA-003
titulo: "Fixtures — banco, auth, global-setup, payloads-telegram"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-01
branch_alvo: feature/qa-003-fixtures-banco-auth-payloads
integration_branch: integration/03-folha-pagamento
prioridade: alta
esforco: medio
territorio: front
estado: pronto-pra-execucao
depende_de: [QA-001, BE-023]
bloqueia: [QA-004]
skills_dispatched: []
exige_e2e_full: false
lote: B
---

# QA-003 — Fixtures — banco, auth, global-setup, payloads-telegram

## Intake

- **Origem:** spec QA Fase 1 §4 (QA-003). **Lote B — depende de BE-023 mergeada em `develop`.**
- **Por quê agora:** QA-004 (specs) não pode funcionar sem os helpers de banco e auth.
- **Esforço:** médio — 4 arquivos, lógica de banco via `mysql2`, autenticação via magic link, factories de payloads Telegram.
- **Riscos resumidos:** ⚠️ `limparDadosE2E()` DELETE incorreto pode apagar dados do Pedro (`requisitante_id=1`) — risco crítico. Reviewer deve ser paranóico com este arquivo. `globalSetup.ts` falha silenciosamente se `.env.e2e` ausente — deve falhar ruidosamente.

---

## Contexto

Após BE-023 mergear em `develop`, a tabela `pedidos_pagamento` tem as novas colunas da V6 (incluindo `requisitante_id` que já existia). Os helpers de banco precisam saber o schema real pra fazer INSERT/DELETE corretamente.

O helper usa `requisitante_id=99` como sentinel de dados de teste (nunca é o Pedro real, que é `id=1`).

O `loginE2E` usa o fluxo de magic link existente: chama admin API pra gerar convite, faz exchange, recebe cookie `finbot_session`.

---

## Decisão / abordagem

**Quatro arquivos em `frontend/e2e/fixtures/`:**

`banco.ts` — conexão via `mysql2/promise` usando credenciais de `.env.e2e`. Funções:
- `garantirRequisitanteE2E()` — `INSERT IGNORE INTO requisitante (id, nome, ...) VALUES (99, 'E2E Test User', ...)`. Idempotente.
- `limparDadosE2E()` — `DELETE FROM pedidos_pagamento WHERE requisitante_id = 99` + tabelas relacionadas. **NUNCA** deleta onde `requisitante_id != 99`.
- `semearPedidos(pedidos: PedidoFixture[])` — insere pedidos de teste com `requisitante_id=99`.
- `querySql(sql: string, params?: any[])` — query genérica pra asserções.

`auth.ts` — `loginE2E(page: Page)`: (1) POST pra API admin pra gerar convite mágico com `requisitante_id=99`, (2) GET na URL de convite, (3) confirmar que cookie `finbot_session` foi setado no contexto do Playwright.

`global-setup.ts` — entry point chamado pelo `globalSetup` do playwright.config. Chama `garantirRequisitanteE2E()`. Falha ruidosamente se `.env.e2e` não existe: `throw new Error("Arquivo .env.e2e não encontrado. Copie .env.e2e.example e preencha.")`.

`payloads-telegram.ts` — factories de `Update` válidos conforme Telegram Bot API:
- `telegramUpdateTextoPuro({ fromUserId, text })` — Update com `message.text`.
- `telegramUpdateSticker({ fromUserId })` — Update com `message.sticker`.
- **NÃO incluir** `telegramUpdateFotoLegenda` — bloqueado pela decisão de mock de mídia (§3 da spec QA). Deixar comentário `// TODO Fase 1.1: adicionar após decisão de mock (ADR 00XX)`.

---

## Escopo / arquivos

### Criar
- `frontend/e2e/fixtures/banco.ts`
- `frontend/e2e/fixtures/auth.ts`
- `frontend/e2e/fixtures/global-setup.ts`
- `frontend/e2e/fixtures/payloads-telegram.ts`

### Remover
- `frontend/e2e/fixtures/.gitkeep` (criado em QA-001 como placeholder — pode remover quando os arquivos reais chegarem)

### Não tocar
- `frontend/src/` — zero código de produção.
- `financas_bot_telegram/` — zero mudanças no back.

---

## Testes

Não aplicável: fixtures validadas por smoke da QA-004. Testes unitários de helpers de banco seria over-engineering conforme spec §4.QA-003.

`testes_total` esperado: sem mudança de testes unitários. `testes_novos: 0`.

---

## Critérios de aceitação

- [ ] `garantirRequisitanteE2E()` é idempotente — rodar 2x não falha nem duplica linha.
- [ ] `limparDadosE2E()` deleta APENAS rows com `requisitante_id=99`. Validar: `SELECT count(*)` de pedidos com `requisitante_id=1` deve ser IGUAL antes e depois do cleanup.
- [ ] `loginE2E(page)` injeta cookie `finbot_session`; chamada subsequente `page.goto('/')` resolve sem redirecionar para login.
- [ ] `global-setup.ts` com `.env.e2e` ausente → mensagem de erro clara com instrução de copiar o exemplo.
- [ ] `payloads-telegram.ts` exporta exatamente 2 factories no MVP (texto puro, sticker); comentário TODO para foto+caption presente.
- [ ] Banco usa credenciais de `.env.e2e` (zero hardcoded no código).
- [ ] `npm test` (Vitest) continua verde.
- [ ] Branch: `feature/qa-003-fixtures-banco-auth-payloads` saindo de `integration/03-folha-pagamento` (que já contém código da QA-001).
- [ ] Status report `docs/sprints/03-folha-pagamento/status/QA-003-fixtures-banco-auth-payloads.md` com frontmatter válido.

---

## Fora de escopo

- `telegramUpdateFotoLegenda` — bloqueado (decisão §3 da spec QA, aguarda ADR de mock de mídia).
- Testes unitários dos helpers de banco.
- Mudanças no back (admin API para gerar convite deve existir; se não existir, abrir FIX separado).

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| `limparDadosE2E()` apaga dados do Pedro (`requisitante_id=1`) | Baixa | **Crítico** | Reviewer paranóico — verificar SQL exato; teste manual antes do PR: SELECT count antes/depois com requisitante_id=1 deve ser igual |
| Schema de `pedidos_pagamento` ainda sem V6 (BE-023 não mergeada) | Média | Alto | Pré-condição: verificar `git log origin/develop | grep be-023` antes de iniciar |
| `loginE2E` falha porque admin API não existe ou path mudou | Média | Médio | Verificar endpoint admin antes de implementar; se não existir, abrir FIX-NNN para expô-lo |
| `.env.e2e` ausente causa falha silenciosa | Média | Médio | `global-setup.ts` valida presença e lança Error claro |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-002 (lote A) após ambas iniciarem.
- **Depende sequencialmente de:** QA-001 (estrutura de pastas `e2e/fixtures/`) + **BE-023 mergeada em `develop`** (schema V6 disponível).
- **Bloqueia:** QA-004 (specs precisam dos fixtures).
- **⚠️ Atenção especial pro Reviewer:** revisão paranóica do `limparDadosE2E()` — é a salvaguarda dos dados reais do Pedro. Verificar que o filtro `requisitante_id = 99` está presente em TODOS os DELETEs.
- **Após merge:** QA-004 pode iniciar imediatamente.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR `feature → integration/03-folha-pagamento`.

`exige_e2e_full: false`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4 (QA-003)
- `docs/architecture/desenho-testes-automatizados.md` §7.2 (banco), §7.3 (global-setup), §7.4 (auth)
- Telegram Bot API: `https://core.telegram.org/bots/api#update` (schema de Update)
