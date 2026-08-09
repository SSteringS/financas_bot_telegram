# YAML — colon-space (`: `) em plain scalar causa "Nested mappings" no frontmatter

## Contexto da dúvida

Durante diagnóstico de por que os agentes `backend` e `frontend` não apareciam no `/agents`
do Claude Code (2026-05-31), o Claude Code exibia o erro:

```
Failed to parse frontmatter
Nested mappings are not allowed in compact mappings at line 2, column 14
```

A causa era a sequência `execução: código em` dentro do campo `description` dos arquivos
`.claude/agents/backend.md` e `.claude/agents/frontend.md` — valor não-quotado com `: ` interno.

## Resumo destilado

Em YAML, um valor de chave em **block context** é lido como **plain scalar** (string
sem aspas). O parser do Claude Code usa **compact mapping** pra parsear o frontmatter, e
nesse modo `: ` (dois-pontos + espaço) dentro de um plain scalar é interpretado como
**separador de chave-valor**, tentando criar um mapeamento aninhado. Compact mappings
não permitem isso → erro de parse → arquivo ignorado silenciosamente.

```yaml
# ❌ QUEBRA — `: ` dentro de plain scalar
description: Implementa quando estiver pronta pra execução: código em src/

# ✅ OK — valor quotado: `: ` é literal dentro de double-quoted string
description: "Implementa quando estiver pronta pra execução: código em src/"
```

## Pontos-chave

- **Regra prática:** sempre que o valor de um campo YAML puder conter `: ` (colon-space),
  use aspas duplas (`"..."`) em volta do valor inteiro.
- **Aplica a:** `description` em `.claude/agents/*.md`, `description` em `.claude/skills/*/SKILL.md`,
  qualquer frontmatter YAML no projeto (status reports, ADRs, planos).
- **Por que outros agentes não quebraram:** `planner`, `architect`, `reviewer`,
  `engenheiro-de-ia` — nenhum tinha `: ` dentro do valor `description` não-quotado.
  Apenas `backend` e `frontend` tinham `execução: código em`.
- **Sintoma:** agente/skill não aparece na lista (`/agents`, progressive disclosure),
  sem mensagem de erro óbvia no terminal principal. O erro aparece apenas no "preview"
  da UI ou com `--debug`.
- **Fix aplicado (2026-05-31):** campo `description` de `backend.md` e `frontend.md`
  envolvido em `"..."`.
- **Caracteres YAML especiais pra ficar de olho em plain scalars:**
  `: ` (colon-space), `#` (comentário), `{`, `}`, `[`, `]`, `&`, `*`, `!`, `|`, `>`.
  Em dúvida, sempre quote.

## Regra de bolso (checklist ao criar/editar frontmatter YAML)

- [ ] Campo `description` contém `: `? → colocar entre `"..."`.
- [ ] Campo `description` contém `#`? → colocar entre `"..."`.
- [ ] Campo `description` contém `[` ou `{`? → colocar entre `"..."`.
- [ ] Em geral: se o valor tem qualquer caracter da lista acima, quote.

## Pra aprofundar

- YAML 1.2 spec — seção 7.3.3 Plain Scalars (compact mapping e flow-mode scalars)
- `docs/aprendizado/taxonomia-agent-skill-workflow.md` — contexto de como agent files funcionam
