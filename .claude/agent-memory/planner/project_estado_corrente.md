---
name: Estado corrente do projeto
description: Snapshot do estado de sprint e tasks em voo — verificar antes de usar (decai rápido)
type: project
---

**Data do snapshot:** 2026-06-05

**Sprint ativa:** 03 — Folha de pagamento

## Feature sprint 03 — CONCLUÍDA ✅ (mergeada em develop)

| Task | PR |
|---|---|
| BE-024..BE-029 + FE-015..FE-017 | PR #106 (integration→develop) |
| FIX-003 | PR #105 |
| FIX-004 (vo→enums) | PR #107 |

## Fase QA fase 1 — CONCLUÍDA ✅

QA-001..QA-008 + FIX-002 — todos mergeados em develop.

## ADRs recentes

- **ADR 0019** (reviewer→QA loop): `Accepted`
- **ADR 0020** (mock Telegram WireMock): `Accepted` — homologado pelo humano

## FIXes concluídos

| Task | PR | Observação |
|---|---|---|
| FIX-005 back (padronizar /api/v1 + JWT allowlist) | PR #109 → develop | ✅ |
| BE-030 (telegram.file.url configurável) | PR #113 → develop | ✅ |

## Fase QA cobertura + E2E expansão

| Task | Estado | PR |
|---|---|---|
| QA-009 cobertura-testes-backend | ✅ concluido | PR #115 → develop (61 testes novos, 417 total) |
| QA-010 cobertura-testes-frontend | 🔵 pronto-pra-execucao | — |
| QA-011 Sub-áreas B/C/D | 🔵 pronto-pra-execucao | — |
| QA-011 Sub-área A | ⏸ aguarda FIX-005 front | — |
| FIX-005 front (folha.ts /api/v1) | 🔴 pendente (produto quebrado) | — |

## integration branch

`integration/03-folha-pagamento` — ativa. Último merge: QA-009 (PR #116 integration→develop).

**Why:** QA-009 concluída e mergeada. Fila restante: FIX-005 front (urgente), QA-010, QA-011 B/C/D.

**How to apply:** FIX-005 front desbloqueia QA-011 Sub-área A — prioridade máxima. QA-010 e QA-011 B/C/D são independentes entre si e podem rodar em paralelo.
