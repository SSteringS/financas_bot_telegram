---
name: Fluxo de branches com integration branch (sprint 03+)
description: Features devem ser criadas a partir de integration/<NN>-<slug>, não de develop diretamente
type: feedback
---

A partir de 2026-06-01, o CLAUDE.md foi atualizado com um novo fluxo de branches de dois níveis:

**Regra:** `feature/<id>-<slug>` deve sair de `integration/<NN>-<slug>`, não de `develop`.

```
git fetch
git checkout -b feature/be-023-slug origin/integration/03-evo-09
```

**Why:** Permite que features sequenciais se encadeiem na integration branch antes de chegarem ao develop. O gate humano fica concentrado no PR `integration → develop` ao fim da sprint, não em cada feature.

**How to apply:**
- No boot de qualquer task BE-* da sprint 03, verificar se `integration/03-evo-09` existe
- Se não existir, pedir ao humano (planner) para criá-la antes de criar a feature branch
- FIX e HOTFIX continuam saindo de `develop` diretamente
- PR target: `feature → integration` (implementador pode aceitar), depois `integration → develop` (humano aceita)

**Contexto:** BE-023 foi criada de `develop` porque a integration branch não existia e o CLAUDE.md foi atualizado durante a sessão. Registrado como desvio aceitável para essa task.
