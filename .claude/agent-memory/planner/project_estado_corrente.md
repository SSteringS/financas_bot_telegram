---
name: Estado corrente do projeto
description: Snapshot do estado de sprint e tasks em voo — verificar antes de usar (decai rápido)
type: project
---

**Data do snapshot:** 2026-06-04 (overnight)

**Sprint ativa:** 03 — Folha de pagamento (EVO-09)

## Fase QA/FIX — CONCLUÍDA ✅

| Task | Estado | PR |
|---|---|---|
| QA-001 setup-playwright-base | ✅ concluido | #84 |
| QA-002 scripts-orquestracao-stack | ✅ concluido | #84 |
| QA-003 fixtures-banco-auth-payloads | ✅ concluido | #85/#86 |
| QA-004 specs-mvp (2 specs, 3 tests) | ✅ concluido | #88/92 |
| QA-005 doc-roteiro-e2e | ✅ executado (planner) | — |
| QA-006 pre-merge-add-e2e-gate | ✅ executado (planner) | — |
| QA-007 remove axe-core | ✅ concluido | #91 |
| QA-008 estabilizacao-suite (workers+update_id) | ✅ concluido | #93/#94 — e2e:full 3x verde |
| FIX-002 ci-aceitar-integration | ✅ concluido | #89/90 |

## Fase BE/FE — OVERNIGHT DESPACHADA

Mega-dispatches autônomos criados e em integration:
- `DISPATCH-OVERNIGHT-BACK.md` → BE-024→025→026→027→028→029 (sessão backend única, serial)
- `DISPATCH-OVERNIGHT-FRONT.md` → FE-015→016→017 (sessão frontend única, serial, com loops de espera por BE)

| Task | Estado |
|---|---|
| BE-024 entidades JPA | 🔵 pronto-pra-execucao (dispatch overnight) |
| BE-025 CRUD funcionário | ⏸ aguarda BE-024 |
| BE-026 cadastrar-vale | ⏸ aguarda BE-024 |
| BE-027 cadastrar-adiantamento | ⏸ aguarda BE-026 |
| BE-028 fechar-mes | ⏸ aguarda BE-024+026+027 |
| BE-029 testes-fechar-mes | ⏸ aguarda BE-028 |
| FE-015 tela-funcionarios | ⏸ aguarda BE-025 |
| FE-016 tela-folha-funcionario | ⏸ aguarda BE-028+FE-015 |
| FE-017 modal-fechamento | ⏸ aguarda FE-016 |

## Decisões resolvidas

- **§7 (vales na lista do Pedro):** ✅ **Opção A** (PO, 2026-06-04) — vales NÃO aparecem na lista.
  `GET /api/pedidos` e `Home.tsx` ficam intocados.
- **ADR 0016:** Accepted (homologado 2026-06-03)
- **ADR 0017:** Accepted (prefixo QA-NNN)
- **ADR 0018:** Proposed (a11y fora de escopo — qa-test-specialist)

## integration branch

`integration/03-folha-pagamento` — ativa, em sincronia com develop `dfa3561` (sync 2026-06-04 overnight).

**Why:** Sprint fase BE/FE despachada como overnight autônomo para execução sem intervenção.

**How to apply:** De manhã, verificar status das tasks via `git log origin/integration/03-folha-pagamento`.
Se todas mergeadas → abrir PR integration→develop e pedir revisão humana.
Se alguma falhou → status report em `docs/sprints/03-folha-pagamento/status/` explica o bloqueio.
