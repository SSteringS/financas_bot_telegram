# STATE — onde o projeto está agora

> **Doc vivo de orientação.** Existe pra uma sessão que começa fria (planner, back, front ou reviewer) se situar em 1 minuto, sem re-derivar contexto. **Curto de propósito.** Detalhe mora nos planos (`docs/plans/`), status reports (`docs/sprints/<NN>/status/`) e ADRs (`docs/decisions/`).
>
> **Última atualização:** 2026-06-01 (sprint 03 em execução — BE-023 mergeada; suíte E2E Fase 1 planejada; integration branch criada; 16 dispatches prontos).
> **Fonte:** este resumo é derivado dos status reports em `docs/sprints/<NN>/status/`. O **estado real de merge em `develop` é do humano** (ele é o integrador — ADR 0004).

---

## Sprint atual

**Sprint 03 — Folha de pagamento** (`docs/sprints/03-folha-pagamento/README.md`).

**Modo:** 🟠 **em execução** — BE-023 ✅ mergeada em develop (PR #81, 2026-06-01). Próximo: despachar BE-024 + QA-001.

**Branch de integração:** `integration/03-folha-pagamento` ✅ criada e pushada. Destino dos PRs das tasks QA-001..004 (e outras features da sprint antes de develop).

**Dispatches disponíveis:** 16 tasks em `docs/sprints/03-folha-pagamento/plans/DISPATCH-*.md`.

---

## Sprint 03 — estado das tasks

### ✅ Mergeado em develop

- **BE-023** — V6 DDL folha de pagamento (PR #81). ⚠️ Pendência: rodar `SHOW CREATE TABLE` no banco dev pra confirmar DDL (item do humano).

### 🔵 Habilitadas — despachar agora

- **BE-024** — Entidades JPA + repositórios (depende de BE-023 ✅). `DISPATCH-BE-024-*.md`.
- **QA-001** — Setup Playwright base (Lote A, independente). `DISPATCH-QA-001-*.md`. **PR destino: `integration/03-folha-pagamento`** (não develop).

### ⏳ Aguarda dispatch sequencial

| Task | Depende de | Dispatch |
|---|---|---|
| BE-025 | BE-024 ✅ em develop | pronto |
| BE-026 | BE-024 ✅ em develop | pronto ⚠️ cria FolhaController |
| BE-027 | BE-024 + **BE-026 mergeada** | pronto (serializada) |
| BE-028 | BE-024 + BE-026 + BE-027 | pronto ⚠️ PASSO ZERO: nullable |
| BE-029 | BE-028 | pronto |
| FE-015 | BE-025 | pronto |
| FE-016 | BE-028 + FE-015 | pronto |
| FE-017 | FE-016 | pronto |
| QA-002 | QA-001 em integration | pronto |
| QA-003 | BE-023 ✅ + QA-001 em integration | pronto (Lote B) |
| QA-004 | QA-002 + QA-003 em integration | pronto (Lote B) |

### ✅ Executado pelo planner (sem dispatch)

- **QA-005** — `docs/runbooks/ROTEIRO-E2E.md` criado.
- **QA-006** — `PRE-MERGE-CHECKLIST.md` atualizado com gate `exige_e2e_full`.

### Decisão pendente

- **§7 — vales na lista do Pedro:** Opção A (filtrar GET) ou B (tag visual). Não bloqueia tasks atuais — resolve como FIX pós-sprint.
- **ADR 0016:** `Proposed` — homologar antes de despachar BE-025+ (código Java novo).
- **ADR mock mídia Telegram:** necessário para QA-004 cenário foto+caption (Fase 1.1 da suíte E2E).

---

## Sprints anteriores

**Sprint 02b — Kaizen (workflow/processo):** ✅ **fechada** em 2026-05-31. WF-01..WF-08 concluídos. Retro em `docs/retrospectivas/RETRO-02b-kaizen-workflow.md`.

**Sprint 02 — Canal WhatsApp:** ✅ **fechada** em 2026-05-30 com FE-14 mergeado (PR #80). Falta abrir PR `develop → main` pro deploy (não bloqueante).

**Sprint 01 — MVP Fase 3:** ✅ concluída (histórico em `docs/sprints/01-mvp/`).

---

## Fluxo de branches desta sprint

```
develop ──── integration/03-folha-pagamento ──── feature/qa-001-setup-playwright-base
             (destino PRs QA-001..004)           feature/qa-002-scripts-orquestracao-stack
                                                  feature/qa-003-fixtures-...
                                                  feature/qa-004-specs-mvp-...

develop ──── feature/be-024-...  (PR direto pra develop)
             feature/be-025-...
             feature/fe-015-...
             etc.
```

- **feature/qa-NNN → integration:** implementador abre e pode auto-aceitar o PR.
- **feature/be-NNN / fe-NNN → develop:** PR + Reviewer obrigatório.
- **integration → develop:** planner abre no fim da sprint; humano homologa.

---

## Pendências abertas (não bloqueantes)

- **DEP-07:** PR #64 aguarda merge + `terraform apply` (in-place confirmado).
- **DEP-08:** webhook via Caddy/LE — Fase 2, aguarda ADR de topologia TLS.
- **PR `develop → main`** (deploy sprint 02): não aberto ainda.
- **BE-20 / PREP-WA fases 4,8,9:** bloqueado por chip WhatsApp Business + Business Verification (externo).
- Demais débitos: `docs/PENDENCIAS-TECNICAS.md`.

---

## Mapa rápido de onde mora o quê

| Preciso de… | Vou em… |
|---|---|
| O que construir (spec de task) | `docs/sprints/03-folha-pagamento/plans/` |
| Dispatch pronto pra colar no agente | `docs/sprints/03-folha-pagamento/plans/DISPATCH-*.md` |
| O que foi feito (execução) | `docs/sprints/<NN>/status/` |
| Decisão arquitetural canônica | `docs/decisions/` (ADRs) |
| Regra que o agente obedece | `CLAUDE.md` |
| Conceito pra revisitar | `docs/aprendizado/` |
| Definição de pronto / gates | `docs/runbooks/PRE-MERGE-CHECKLIST.md` |
| Como rodar a suíte E2E | `docs/runbooks/ROTEIRO-E2E.md` |
| Instruções por papel | `docs/roles/` |
| Débito técnico conhecido | `docs/PENDENCIAS-TECNICAS.md` |
