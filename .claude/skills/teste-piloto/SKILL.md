---
name: teste-piloto
description: Skill de validação do piloto VS Code + Claude Code. Confirma que .claude/skills/ aceita escrita e que o ambiente consegue descobrir skills canônicas Anthropic. Usar apenas para validação do piloto — não é skill de produção.
---

# Skill — Piloto de Validação

Esta skill existe apenas para confirmar que o ambiente VS Code + Claude Code consegue:

1. Escrever em `.claude/skills/<name>/SKILL.md` via Write tool
2. Descobrir a skill via semantic matching no boot (se `/skills` listar esta skill, confirmado)

**Não usar em sessões de produção.** Pode ser removida após confirmação do piloto.

## Quando carregar

Nunca — é skill de validação, não de produção.
