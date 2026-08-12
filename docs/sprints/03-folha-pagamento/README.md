# Sprint 03 — Folha de Pagamento

**Status:** ✅ **fechada em 2026-08-12** — 23/25 tasks concluídas. QA-010 e QA-011 encerradas **sem execução**, com os gaps migrados para `docs/PENDENCIAS-TECNICAS.md`. Sem retrospectiva, por decisão do humano. Ver §Encerramento no fim deste arquivo.

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
| FIX-005 front | padronizar-api-v1-front | ✅ concluido (PR #117 → develop) |

### QA — Cobertura + E2E Expansão (2026-06-05)

| ID | Slug | Estado |
|----|------|--------|
| QA-009 | cobertura-testes-backend | ✅ concluido (PR #115 → develop) — 61 testes novos (417 total) |
| QA-010 | cobertura-testes-frontend | ⬛ **não executada** — migrada para `docs/PENDENCIAS-TECNICAS.md` |
| QA-011 | expansao-e2e-cenarios-positivos | ⬛ **não executada** — migrada para `docs/PENDENCIAS-TECNICAS.md` |
| BE-030 | telegram-file-url-configuravel | ✅ concluido (PR #113 → develop) |

---

## Encerramento

**Sprint fechada em 2026-08-12, por decisão do humano.** 23 das 25 tasks concluídas; a entrega que definia a sprint — gestão de folha de pagamento doméstica — está em produção.

**Duas tasks foram fechadas sem execução**, com o plano preservado e o gap registrado como débito técnico:

- **QA-010** (cobertura de testes frontend) — ~14 arquivos de front sem teste.
- **QA-011** (expansão E2E de cenários positivos) — o caminho positivo do produto segue descoberto pela suíte E2E.

Os planos continuam em `plans/` e seguem válidos; quem retomar deve revalidar o inventário, defasado desde 2026-06-04. O detalhamento do gap está em `docs/PENDENCIAS-TECNICAS.md`.

**FIX-006 e FIX-007** foram de `parcial` para `concluido` na mesma passagem: o código já estava em `main` (PRs #119 e #121, e daí #122) e o `parcial` refletia apenas pendências de mão humana, que foram migradas para o registro de débitos. **A rotação dos segredos do FIX-007 segue aberta e é o item de segurança mais urgente do repositório.**

**Sem retrospectiva** — decisão explícita do humano. As sprints 01, 02 e 02b têm retro; esta não terá, e a ausência é deliberada, não esquecimento.

> O `RUNBOOK-fechamento-sprint.md` não foi seguido integralmente: o fechamento foi por decisão direta, com duas tasks não executadas e sem retro.
