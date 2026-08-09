---
task: QA-003
sprint: 03-folha-pagamento
data: 2026-06-03
avaliador: informal
status_report: docs/sprints/03-folha-pagamento/status/QA-003-fixtures-banco-auth-payloads.md
veredito_codigo: aprovado
veredito_final: aprovado
observacoes_count: 0
roteiro_executado: false
gates_verificados_contra_realidade: ok
skills_eficazes: []
skills_gaps: []
---

# Avaliação — QA-003 Fixtures banco + auth + payloads

**Branch:** `feature/qa-003-fixtures-banco-auth-payloads`
**Implementador:** claude-front
**Plano:** `docs/sprints/03-folha-pagamento/plans/QA-003-fixtures-banco-auth-payloads.md`
**Status report:** `docs/sprints/03-folha-pagamento/status/QA-003-fixtures-banco-auth-payloads.md`

---

> ⚠️ **Débito de processo (2026-06-03):** esta task foi revisada informalmente e mergeada diretamente em `develop` (PR #85/#86) sem avaliação escrita pelo Reviewer. Aceito como débito de processo pelo humano. Este arquivo é o registro retroativo dessa decisão — não substitui uma revisão adversarial.
>
> Para tasks futuras de porte M+, a avaliação escrita permanece obrigatória (ADR 0005).

---

## Resumo da entrega (baseado no status report + commit `adc137a`)

O commit entregou 4 helpers de infraestrutura E2E em `frontend/e2e/fixtures/`:

- **`banco.ts`** — conexão `mysql2/promise`, `garantirRequisitanteE2E` (idempotente), `limparDadosE2E` (DELETE apenas `requisitante_id=99`), `semearPedidos`, `querySql`, `fecharConexao`.
- **`auth.ts`** — `loginE2E`: fluxo magic link completo (POST convite admin API → exchange → cookie `finbot_session` injetado no contexto Playwright).
- **`global-setup.ts`** — substitui stub de QA-001; valida `.env.e2e` presente, falha ruidosamente se ausente, chama `garantirRequisitanteE2E`.
- **`payloads-telegram.ts`** — 2 factories de Update sintético (`textoPuro`, `sticker`); `telegramUpdateFotoLegenda` com TODO comentado (aguarda ADR de mock de mídia).

Correção colateral: `e2e/tsconfig.json` — `allowImportingTsExtensions false→true` (resolve TS5097 registrado em `PENDENCIAS-TECNICAS.md`).

Território: apenas `frontend/e2e/` + `docs/sprints/03-folha-pagamento/status/`. Zero vazamento.

---

## Para o planner

- QA-004 pode ser despachada — suas dependências (QA-002 + QA-003) estão em `develop`.
- `telegramUpdateFotoLegenda` permanece bloqueada por ADR de mock de mídia (registrado no plano QA-004).
- Correção do `e2e/tsconfig.json` resolve o item de PENDENCIAS-TECNICAS.md "scripts e2e sem type-check estático" — marcar como resolvido.
