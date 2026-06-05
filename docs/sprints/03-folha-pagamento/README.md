# Sprint 03 — Folha de Pagamento

**Status:** 🟡 em andamento — fase BE/FE + FIX concluídas; QA de cobertura em execução (2026-06-05)

**Objetivo:** entregar a gestão de folha de pagamento doméstica — cadastro de funcionários, vales, adiantamentos parcelados e fechamento mensal.

**Spec técnica:** `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md`
**ADR:** `docs/decisions/0016-evo09-folha-pagamento.md` (`Accepted` — homologado 2026-06-03)

---

## Escopo

### Backend (7 tasks)

| ID | Slug | Porte | Depende de | Estado |
|----|------|-------|------------|--------|
| BE-023 | migracao-v6-folha-pagamento | P | — | ✅ concluido (develop PR #81) |
| BE-024 | entidades-jpa-repositorios-folha | M | BE-023 | 🔵 pronto-pra-execucao |
| BE-025 | crud-funcionario | M | BE-024 | ⏸ aguarda BE-024 |
| BE-026 | cadastrar-vale | P | BE-024 | ⏸ aguarda BE-024 |
| BE-027 | cadastrar-adiantamento | P | BE-024 | ⏸ aguarda BE-024 |
| BE-028 | fechar-mes | M | BE-024, BE-026, BE-027 | ⏸ aguarda BE-024+026+027 |
| BE-029 | testes-fechar-mes | M | BE-028 | ⏸ aguarda BE-028 |

### Frontend (3 tasks)

| ID | Slug | Porte | Depende de | Estado |
|----|------|-------|------------|--------|
| FE-015 | tela-funcionarios | M | BE-025 | ⏸ aguarda BE-025 |
| FE-016 | tela-folha-funcionario | G | BE-026, BE-027, BE-028 | ⏸ aguarda BE-026+027+028 |
| FE-017 | modal-fechamento | M | BE-028 | ⏸ aguarda BE-028 |

### QA — Suíte E2E Fase 1 + estabilização (8 tasks)

Spec completa: `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md`.
ADR: `docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md`.

| ID | Slug | Território | Estado |
|----|------|-----------|--------|
| QA-001 | setup-playwright-base | frontend | ✅ concluido (PR #84) |
| QA-002 | scripts-orquestracao-stack | frontend | ✅ concluido (PR #84) |
| QA-003 | fixtures-banco-auth-payloads | frontend | ✅ concluido (PRs #85/#86 — revisão informal, débito de processo) |
| QA-004 | specs-mvp-3-cenarios | frontend | ✅ concluido (PR #88/92 → develop) |
| QA-005 | doc-roteiro-e2e | plan | ✅ executado (planner) |
| QA-006 | pre-merge-add-e2e-gate | plan | ✅ executado (planner) |
| QA-007 | remover-axe-core-a11y-do-gate | frontend | ✅ concluido (PR #91 → develop) |
| QA-008 | estabilizacao-suite-e2e | frontend | ✅ concluido (PR #93/#94 → develop) — e2e:full 3x verde ✅ |

### FIX

| ID | Slug | Estado |
|----|------|--------|
| FIX-002 | ci-aceitar-integration-no-gate-de-branch | ✅ concluido (PR #89/90 → develop) |
| FIX-003 | qa-gaps-fechar-mes | ✅ concluido (PR #105 → develop) |
| FIX-004 | consolidar-pacote-vo-em-enums | ✅ concluido (PR #107 → develop) |
| FIX-005 back | padronizar-api-v1-jwt-allowlist | ✅ concluido (PR #109 → develop) |
| FIX-005 front | padronizar-api-v1-front | 🔴 pendente (folha.ts ainda usa `/api/funcionarios`) |

### QA — Cobertura + E2E Expansão (2026-06-05)

| ID | Slug | Estado |
|----|------|--------|
| QA-009 | cobertura-testes-backend | 🔵 pronto-pra-execucao |
| QA-010 | cobertura-testes-frontend | 🔵 pronto-pra-execucao |
| QA-011 | expansao-e2e-cenarios-positivos | 🔵 pronto-pra-execucao (Sub-áreas B/C/D; Sub-área A desbloqueia com FIX-005 front) |
| BE-030 | telegram-file-url-configuravel | ✅ concluido (PR #113 → develop) |

---

## Próximos passos (atual — QA de cobertura)

Fila de execução paralela disponível:

```
FIX-005 front     (front — produto quebrado sem isso; desbloqueia QA-011 Sub-área A)
QA-009            (back — independente)
QA-010            (front — independente)
QA-011 B/C/D      (front — independente; Sub-área A aguarda FIX-005 front)
```

BE-030 ✅ concluída — QA-011 Sub-área A só precisa ainda do FIX-005 front.

---

## Ritual de encerramento

Seguir `docs/runbooks/RUNBOOK-fechamento-sprint.md` quando todas as tasks estiverem concluídas.
