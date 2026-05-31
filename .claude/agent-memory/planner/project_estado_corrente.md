---
name: Estado corrente do projeto
description: Snapshot do estado de sprint e tasks em voo — verificar antes de usar (decai rápido)
type: project
---

**Data do snapshot:** 2026-05-30

**Sprint ativa:** 02b — Kaizen (workflow/processo)
Primeira leva concluída: WF-01, WF-02, WF-03 commitadas em `develop`. Push para `origin/develop` pendente (humano executa no terminal Windows).

Segunda leva (WF-04..WF-08) ainda a refinar:
- WF-04 — Adoção zero-padded geral (numeração BE/FE/DEP/EVO/CI)
- WF-05 — Roles × skills × workflows (ADR 0015 `Proposed`, pendente homologação)
- WF-06 — Ritual de métricas no fim de cada sprint
- WF-07 — Ligar branch protection (CI-01 bloqueante)
- WF-08 — Formalizar `pendencias_humano` no schema de status

**Sprint 02 (Canal WhatsApp):** ✅ fechada. FE-14 mergeado PR #80. PR `develop → main` para deploy ainda por abrir (não bloqueante).

**Parqueado (bloqueio externo):** BE-20, BE-21b, EVO-02 completa — aguardam chip WhatsApp Business dedicado + Business Verification da Meta.

**Why:** Rápida orientação entre sessões para não re-derivar o contexto de onde o projeto parou.

**How to apply:** Verificar `docs/STATE.md` e `docs/sprints/02b-kaizen-workflow/README.md` para confirmar se o estado ainda bate antes de propor ações. Este snapshot decai em horas se o humano fizer push e abrirmos a segunda leva.
