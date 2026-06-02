---
name: Feedback — branch sempre a partir de develop
description: Feature branches FE devem sempre partir de origin/develop, nunca de outra feature branch
type: feedback
---

Criar branch nova sempre com: `git fetch && git checkout -b feature/fe-NNN-slug origin/develop`

**Why:** Incidente documentado no CLAUDE.md — FE-13 foi criada a partir de `feature/ci-01-gate-pr-develop` em vez de `develop`, trazendo commits não relacionados para o PR. Isso poluiu o histórico e gerou retrabalho.

**How to apply:** Antes de criar qualquer branch, verificar com `git merge-base --is-ancestor origin/develop HEAD` que o ponto de partida é develop. Nunca fazer `git checkout` de outra feature branch antes de criar a nova.
