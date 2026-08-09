---
name: Status reports criados pelo planner em develop
description: Status reports das tasks de feature podem estar em develop (criados pelo planner), nao na branch de feature — comportamento esperado pelo modelo de worktrees
type: project
---

Status reports em `docs/sprints/<NN>/status/` podem ser criados pelo planner diretamente em `develop`, nao pelo implementador na branch de feature.

**Why:** O planner opera no worktree dedicado fixado em `develop`. Quando o planner cria o status report de uma task, ele commita em `develop` diretamente. O arquivo nao aparece no `git diff develop...feature/be-NNN` porque ja existe em `develop`.

**How to apply:** No gate `territorio` do Reviewer, nao sinalizar como erro a ausencia do status report no diff da branch de feature. Verificar que o arquivo existe em `develop` (ou na branch de feature) antes do merge — se existir em qualquer dos dois, o gate passa. A validacao relevante e: o arquivo existe e tem frontmatter valido?
