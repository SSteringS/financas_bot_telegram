---
name: Estado corrente do projeto
description: Snapshot do estado de sprint e tasks em voo — verificar antes de usar (decai rápido)
type: project
---

**Data do snapshot:** 2026-06-04

**Sprint ativa:** 03 — Folha de pagamento (EVO-09)

## Fase QA/FIX — CONCLUÍDA ✅

| Task | Estado | PR |
|---|---|---|
| QA-001 setup-playwright-base | ✅ concluido | #84 |
| QA-002 scripts-orquestracao-stack | ✅ concluido | #84 |
| QA-003 fixtures-banco-auth-payloads | ✅ concluido | #85/#86 (débito: revisão informal) |
| QA-004 specs-mvp (2 specs, 3 tests) | ✅ concluido | #88/92 |
| QA-005 doc-roteiro-e2e | ✅ executado (planner) | — |
| QA-006 pre-merge-add-e2e-gate | ✅ executado (planner) | — |
| QA-007 remove axe-core | ✅ concluido | #91 |
| QA-008 estabilizacao-suite (workers+update_id) | ✅ concluido | #93/#94 — e2e:full 3x verde |
| FIX-002 ci-aceitar-integration | ✅ concluido | #89/90 |

## Fase BE/FE — PENDENTE

| Task | Estado | Bloqueio |
|---|---|---|
| BE-024 entidades JPA | 🔵 pronto-pra-execucao | integration branch deletada |
| BE-025 CRUD funcionário | ⏸ | BE-024 |
| BE-026 cadastrar-vale | ⏸ | BE-024 |
| BE-027 cadastrar-adiantamento | ⏸ | BE-024; serializado após BE-026 |
| BE-028 fechar-mes | ⏸ | BE-024+026+027 |
| BE-029 testes-fechar-mes | ⏸ | BE-028 |
| FE-015 tela-funcionarios | ⏸ | BE-025 |
| FE-016 tela-folha-funcionario | ⏸ | BE-026+027+028+FE-015 |
| FE-017 modal-fechamento | ⏸ | BE-028 |

## Ação necessária antes de despachar BE-026/FE-016

1. **Decisão §7 da spec** (vales na lista do Pedro?) — bloqueia BE-026 e FE-016.

`integration/03-folha-pagamento` está ativa e em sincronia com develop (sync 2026-06-04).

**Why:** Sprint iniciou fase QA primeiro (infra E2E); fase BE/FE interrompida esperando dispatch.

**How to apply:** Ao planejar próxima sessão, verificar se integration branch foi recriada. Se não, recriar antes de despachar BE-024. Dispatches prontos em `docs/sprints/03-folha-pagamento/plans/DISPATCH-BE-024...`.
