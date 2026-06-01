---
name: Estado corrente do projeto
description: Snapshot do estado de sprint e tasks em voo — verificar antes de usar (decai rápido)
type: project
---

**Data do snapshot:** 2026-06-01

**Sprint ativa:** 03 — Folha de pagamento (EVO-09)

## Estado BE-023

✅ **BE-023 mergeada em develop via PR #81** (2026-06-01). V6 SQL disponível.
**`requisitante_id` em `pedidos_pagamento`: NOT NULL DEFAULT 1** — BE-028 precisará de migration V6b (ALTER para nullable) antes de criar Pedido FOLHA. Documentado como "PASSO ZERO" no DISPATCH-BE-028.

## Fila BE/FE (prontas para despachar)

```
BE-024 → (merge) → BE-025 ║ BE-026
                              ↓          ↓
                            FE-015    BE-027 (após BE-026 mergear)
                              ↓          ↓
                            (merge)   BE-028 (após BE-024+026+027)
                                          ↓
                                   BE-029 ║ FE-016 ║ FE-017
                                          (FE-016 também espera FE-015)
                                          (FE-017 também espera FE-016)
```

**Serialização intencional:** BE-026 → BE-027 (não paralelas) — conflito em FolhaController.java.

## Fila QA — Suíte E2E Fase 1

**integration branch criada:** `integration/03-folha-pagamento` ✅ (2026-06-01)

### Lote A (despachar imediatamente)
- QA-001 → setup Playwright base → PR para integration
- QA-002 → scripts orquestração → aguarda QA-001 em integration
- QA-005 ✅ executado pelo planner (ROTEIRO-E2E.md criado)
- QA-006 ✅ executado pelo planner (PRE-MERGE-CHECKLIST atualizado)

### Lote B (aguarda BE-023 em develop ✅ + QA-001..002 em integration)
- QA-003 → fixtures banco + auth + payloads
- QA-004 → 3 specs MVP (4 testes) — foto+caption bloqueada

**⚠️ QA-004 cenário foto+caption:** bloqueado até sessão com arquiteto para decidir mock de mídia Telegram → ADR Proposed separado.

## Decisões pendentes

- **ADR 0016:** `Proposed` — homologar antes de despachar tasks de código Java (BE-024+).
- **Decisão §7 spec** (vales na lista do Pedro): não bloqueia tasks; resolver como FIX pós-sprint.
- **ADR mock mídia Telegram:** necessário para QA-004 foto+caption (Fase 1.1).
- **eng-ia:** backend.md e frontend.md initialPrompts ainda dizem `origin/develop` — precisam update para `origin/integration/<NN>-<slug>` para tasks QA.

## Commits desta sprint (develop)

- `b672e9a` — dispatches BE-024..FE-017 + status/avaliação BE-023
- `cc3b21a` — memory planner
- `00c9119` — qa-test-specialist agent
- `59e1177` — suíte E2E Fase 1 (ADR 0017, 6 planos QA, 4 dispatches, ROTEIRO-E2E, checklist)
- `Merge` — origin/develop (BE-023 DDL)

**Why:** Rápida orientação entre sessões.

**How to apply:** Verificar os dispatches em `docs/sprints/03-folha-pagamento/plans/DISPATCH-*.md` para próximo passo. Após merge de BE-023 (já feito), despachar BE-024 + QA-001.
