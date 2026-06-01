# DISPATCH — QA-003-fixtures-banco-auth-payloads (single-task)

> **Lote B — despachar SOMENTE após BE-023 mergeada em `develop`.**
> Esta task toca SQL real nas tabelas da V6. Antes de despachar:
> `git log origin/develop --oneline | grep be-023`
> Se não aparecer, aguardar o merge da BE-023.

---

## Pré-condições (git)

- `feature/be-023-migracao-v6-folha-pagamento` mergeada em `develop` (V6 SQL disponível).
- `feature/qa-001-setup-playwright-base` mergeada em `integration/03-folha-pagamento` (estrutura `e2e/fixtures/` existe).

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: QA-003 — Fixtures (banco, auth, global-setup, payloads-telegram).

Localize e leia o plano:
  Glob("docs/sprints/03-folha-pagamento/plans/QA-003-fixtures-banco-auth-payloads.md")

Leia também:
- docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md §4 (QA-003)
- docs/architecture/desenho-testes-automatizados.md §7.2 (banco), §7.3 (global-setup), §7.4 (auth)
- Telegram Bot API schema de Update: https://core.telegram.org/bots/api#update

## ATENÇÃO — BRANCH

Branch a partir de `integration/03-folha-pagamento` (que já tem QA-001):
  git fetch
  git checkout -b feature/qa-003-fixtures-banco-auth-payloads origin/integration/03-folha-pagamento

## A TASK

Criar 4 arquivos em frontend/e2e/fixtures/:

### banco.ts (CRÍTICO — ler com atenção)
- Conexão via mysql2/promise usando credenciais de .env.e2e
- garantirRequisitanteE2E(): INSERT IGNORE com requisitante_id=99
- limparDadosE2E(): DELETE FROM pedidos_pagamento WHERE requisitante_id = 99
  ⚠️⚠️⚠️ NUNCA deletar onde requisitante_id != 99. Verificar 2x o SQL antes de commitar.
- semearPedidos(pedidos): INSERT com requisitante_id=99
- querySql(sql, params): query genérica

### auth.ts
- loginE2E(page: Page): Promise<void>
  1. POST para admin API para gerar convite com requisitante_id=99
  2. GET na URL do convite (magic link flow)
  3. Verificar que cookie finbot_session está no contexto

### global-setup.ts
- Entry point do playwright globalSetup
- Carregar .env.e2e via dotenv
- Se .env.e2e ausente: throw new Error("Arquivo .env.e2e não encontrado. Copie .env.e2e.example e preencha.")
- Chamar garantirRequisitanteE2E()

### payloads-telegram.ts
- telegramUpdateTextoPuro({ fromUserId: number, text: string }): TelegramUpdate
- telegramUpdateSticker({ fromUserId: number }): TelegramUpdate
- NÃO criar telegramUpdateFotoLegenda — comentar:
  // TODO Fase 1.1: adicionar após decisão de mock de download de mídia Telegram (ADR 00XX)

## REGRAS DURAS

1. Branch: `feature/qa-003-fixtures-banco-auth-payloads` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `frontend/`. Zero `financas_bot_telegram/`. Zero frontend/src/.
3. ⚠️ SEGURANÇA DE DADOS: limparDadosE2E() usa SOMENTE requisitante_id=99. Nenhum DELETE sem esse filtro.
4. Credenciais do banco: SOMENTE via process.env (carregado do .env.e2e). ZERO hardcoded.
5. 1 commit: `feat(QA-003): fixtures banco + auth + global-setup + payloads-telegram`.
6. PR: `feature/qa-003-fixtures-banco-auth-payloads → integration/03-folha-pagamento`.

## VERIFICAÇÕES ANTES DE COMMITAR

1. garantirRequisitanteE2E() — executar manualmente 2x, verificar que não lança erro na segunda vez.
2. limparDadosE2E() — rodar SELECT count(*) WHERE requisitante_id=1 ANTES e DEPOIS. Deve ser IGUAL.
3. loginE2E — testar que page.goto('/') após login não redireciona para tela de login.
4. global-setup.ts sem .env.e2e — deve lançar Error com mensagem clara.
5. Verificar que payloads-telegram.ts exporta EXATAMENTE 2 factories (grep export).

## SE QUEBRAR

Cenário — admin API para gerar convite não existe ou path errado:
  Verificar GET /v3/api-docs para encontrar endpoint. Se não existir, PARE e use AskUserQuestion.
  NÃO improvisar endpoint inexistente.

Cenário — banco.ts não consegue conectar (credenciais erradas):
  Verificar .env.e2e contra .env.e2e.example. Credenciais de DEV, não prod.

Pare ao final. PR para integration.
```

---

## ⚠️ Nota especial pro Reviewer desta task

**Prioridade máxima de revisão:** verificar linha a linha o SQL em `limparDadosE2E()`. Todo DELETE deve ter `WHERE requisitante_id = 99` ou equivalente. Uma linha sem filtro apaga dados reais do Pedro.

---

## Notas pro humano

- **Despachar somente após BE-023 em develop.**
- **Após merge:** despachar QA-004 (junto com QA-002 se ainda não mergeou).
- **Estimativa:** 1.5-2h.

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-003-fixtures-banco-auth-payloads.md`
- `docs/architecture/desenho-testes-automatizados.md` §7.2..7.4
