---
name: Estado corrente do projeto
description: Snapshot do estado de sprint e tasks em voo — verificar antes de usar (decai rápido)
type: project
---

**Data do snapshot:** 2026-06-01

**Sprint ativa:** 03 — Folha de pagamento (EVO-09)

**BE-023 — Migração V6 DDL:** ✅ implementada + Reviewer aprovado (2026-06-01). Branch `feature/be-023-migracao-v6-folha-pagamento` aguarda merge pelo humano. Após merge: despachar BE-024.

**`requisitante_id` em `pedidos_pagamento`: NOT NULL DEFAULT 1** (confirmado pelo back no status de BE-023). BE-028 vai precisar de migration V6b (ALTER para nullable) ou workaround antes de criar Pedido FOLHA.

**Fila de tasks e dependências git:**
```
BE-023 → (merge) → BE-024 → (merge) → BE-025 ║ BE-026
                                         ↓          ↓
                                       FE-015    BE-027 (após BE-026 mergear)
                                         ↓          ↓
                                       (merge)   BE-028 (após BE-024+026+027)
                                                     ↓
                                              BE-029 ║ FE-016 ║ FE-017
                                                     (FE-016 também espera FE-015)
                                                     (FE-017 também espera FE-016)
```

**Serialização intencional:** BE-026 → BE-027 (não paralelas) para evitar conflito em FolhaController.java. Documentado nos dispatches.

**Decisão §7 pendente** (vales na lista do Pedro — Opção A ou B): não bloqueia nenhuma task BE/FE; resolve como FIX separado após sprint.

**ADR 0016:** `Proposed` — homologar antes de despachar tasks de código Java (BE-024+).

**Dispatches prontos:** DISPATCH-BE-023..FE-017 em `docs/sprints/03-folha-pagamento/plans/`.

**Why:** Rápida orientação entre sessões.

**How to apply:** Verificar `docs/STATE.md` para estado atualizado. Este snapshot decai quando o humano mergear BE-023 e despachar BE-024.
