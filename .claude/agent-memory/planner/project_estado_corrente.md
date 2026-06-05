---
name: Estado corrente do projeto
description: Snapshot do estado de sprint e tasks em voo — verificar antes de usar (decai rápido)
type: project
---

**Data do snapshot:** 2026-06-04

**Sprint ativa:** 03 — Folha de pagamento

## Feature sprint 03 — CONCLUÍDA ✅ (mergeada em develop)

| Task | PR |
|---|---|
| BE-024..BE-029 + FE-015..FE-017 | PR #106 (integration→develop) |
| FIX-003 | PR #105 |
| FIX-004 (vo→enums) | PR #107 |

## Fase QA — CONCLUÍDA ✅

QA-001..QA-008 + FIX-002 — todos mergeados em develop.

## ADRs recentes

- **ADR 0019** (reviewer→QA loop): `Accepted`
- **ADR 0020** (mock Telegram WireMock): `Proposed` — aguarda homologação humana

## Tasks pendentes — planejadas, não commitadas ainda

| Task | Estado | Arquivo |
|---|---|---|
| QA-009 cobertura-testes-backend | pronto-pra-execucao | plans/QA-009-... |
| QA-010 cobertura-testes-frontend | pronto-pra-execucao | plans/QA-010-... |
| QA-011 expansao-e2e | pronto-pra-execucao (depende BE-030) | plans/QA-011-... |
| ADR 0020 | Proposed | decisions/0020-... |
| BE-030 telegram-file-url-configuravel | A PLANEJAR | — |

## Divergência de branch

- `develop` local está 1 commit atrás de `origin/develop` (PR #107 FIX-004 — `d9823f6`)
- `integration/03-folha-pagamento` ainda ativa no remoto

## integration branch

`integration/03-folha-pagamento` — ativa. QA-009/010/011 devem sair dela.

**Why:** Sprint 03 features mergeadas. QA de cobertura e E2E expandido ainda pendentes de execução.

**How to apply:** Antes de despachar QA-009/010/011: (1) pull develop local; (2) commitar os planos; (3) sync develop→integration; (4) criar plano BE-030 e despachar primeiro (desbloqueia QA-011 Sub-área A).
