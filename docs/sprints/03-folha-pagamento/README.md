# Sprint 03 — Folha de Pagamento

**Status:** 🟡 em andamento (2026-06-01)

**Objetivo:** entregar a gestão de folha de pagamento doméstica — cadastro de funcionários, vales, adiantamentos parcelados e fechamento mensal.

**Spec técnica:** `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md`
**ADR:** `docs/decisions/0016-evo09-folha-pagamento.md` (`Proposed` — aguarda homologação)

---

## Escopo

### Backend (7 tasks)

| ID | Slug | Porte | Depende de | Estado |
|----|------|-------|------------|--------|
| BE-023 | migracao-v6-folha-pagamento | P | — | ✅ concluido |
| BE-024 | entidades-jpa-repositorios-folha | M | BE-023 | aguarda dispatch |
| BE-025 | crud-funcionario | M | BE-024 | aguarda dispatch |
| BE-026 | cadastrar-vale | P | BE-024 | aguarda dispatch |
| BE-027 | cadastrar-adiantamento | P | BE-024 | aguarda dispatch |
| BE-028 | fechar-mes | M | BE-024, BE-026, BE-027 | aguarda dispatch |
| BE-029 | testes-fechar-mes | M | BE-028 | aguarda dispatch |

### Frontend (3 tasks)

| ID | Slug | Porte | Depende de | Estado |
|----|------|-------|------------|--------|
| FE-015 | tela-funcionarios | M | BE-025 | aguarda dispatch |
| FE-016 | tela-folha-funcionario | G | BE-026, BE-027, BE-028 | aguarda dispatch |
| FE-017 | modal-fechamento | M | BE-028 | aguarda dispatch |

### QA — Suíte E2E Fase 1 (6 tasks)

Branch de integração: `integration/03-folha-pagamento` (feature branches QA-NNN vão para cá).
Spec completa: `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md`.
ADR: `docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md`.

#### Lote A — independente de produto (despachar já)

| ID | Slug | Porte | Depende de | Território | Estado |
|----|------|-------|------------|-----------|--------|
| QA-001 | setup-playwright-base | P | — | frontend | ✅ concluido (integration → develop PR #84) |
| QA-002 | scripts-orquestracao-stack | M | QA-001 (integration) | frontend | ✅ concluido (integration → develop PR #84) |
| QA-005 | doc-roteiro-e2e | P | — | plan | ✅ executado (planner) |
| QA-006 | pre-merge-add-e2e-gate | P | QA-005 | plan | ✅ executado (planner) |

#### Lote B — aguarda BE-023 em develop + QA-001..002 em integration

| ID | Slug | Porte | Depende de | Estado |
|----|------|-------|------------|--------|
| QA-003 | fixtures-banco-auth-payloads | M | BE-023 (develop) + QA-001 (integration) | ✅ concluido (develop PR #85/#86 — revisão informal, débito de processo) |
| QA-004 | specs-mvp-3-cenarios | M | QA-002 + QA-003 (integration) | aguarda dispatch |

> ⚠️ **QA-004 cenário foto+caption bloqueado** — aguarda sessão com arquiteto para decidir estratégia de mock de download de mídia Telegram (vira ADR Proposed separado). Texto puro + sticker implementados normalmente.

---

## Ordem de despacho sugerida

1. **BE-023** ✅ já mergeada em develop
2. **QA-001** (Lote A — dispatch imediato, vai para `integration/03-folha-pagamento`)
3. **BE-024** (após BE-023 em develop)
4. **QA-002** (após QA-001 em integration)
5. **BE-025 + BE-026 + BE-027** (paralelos, após BE-024)
   - ⚠️ BE-026 e BE-027 NÃO podem ser paralelas — ambas tocam FolhaController.java. BE-026 cria, BE-027 adiciona após merge.
6. **QA-003** (Lote B — após BE-023 em develop + QA-001 em integration)
7. **BE-028** (após BE-024 + BE-026 + BE-027)
8. **BE-029** (após BE-028)
9. **QA-004** (após QA-002 + QA-003 em integration)
10. **FE-015** (pode iniciar em paralelo com BE-025)
11. **FE-016 + FE-017** (após BE-028 e FE-015)

---

## Decisão pendente

> ⚠️ Seção 7 da spec: **vales aparecem na lista do Pedro?** (Opção A — não / Opção B — com tag visual). Bloqueia BE-026 e FE-016. Resolver com PO antes de despachar essas duas tasks.

---

## Fluxo de git desta sprint

- **BE/FE/QA:** todas as features saem de `origin/integration/03-folha-pagamento` e fazem PR para `integration`. Develop só aceita de integration ao final da sprint (gate do humano). Regra canônica em `CLAUDE.md §Fluxo de branches`.
- **Exceção QA-004:** parte de `develop` (QA-003 foi para develop diretamente — integration não tem as fixtures).

---

## Ritual de encerramento

Seguir `docs/runbooks/RUNBOOK-fechamento-sprint.md` — inclui seção `## Agents & Skills` (primeira coleta do feedback loop, estreia na sprint 03).
