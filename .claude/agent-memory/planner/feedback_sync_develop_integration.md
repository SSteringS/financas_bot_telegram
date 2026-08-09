---
name: Sync develop → integration após commits
description: Após cada sessão de commits em develop, propagar docs para a integration branch da sprint ativa via gh API
type: feedback
---

Após cada push/commit em `develop`, fazer o merge de develop na branch `integration/<NN>-<slug>` da sprint ativa, para que os Claude implementadores vejam os docs atualizados ao dar `git fetch`.

**Why:** Implementadores criam branches a partir de `origin/integration` — se os dispatches e planos estiverem só em develop, eles não os veem. Descoberto em 2026-06-03 quando os dispatches corrigidos (branch source/PR target) ficaram presos em develop.

**How to apply:** No final de cada sessão (depois do último commit), rodar:

```bash
gh api --method POST repos/SSteringS/financas_bot_telegram/merges \
  -f base="integration/<NN>-<slug>" \
  -f head="develop" \
  -f commit_message="chore(sync): merge develop docs into integration/<NN>-<slug>"
```

- 204 = já em sincronia (ok)
- 409 = conflito → reportar ao humano
- Sem sprint ativa → pular
- Regra canônica documentada em `docs/roles/planner.md §Sincronização develop → integration`
