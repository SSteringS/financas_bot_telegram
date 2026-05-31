# Skills — capacidades técnicas reutilizáveis ou densas-mas-raras

> Pasta criada pela ADR `0015` (taxonomia roles × skills). Em adoção — primeira skill é piloto, refinar com o uso.

## O que é skill

**Skill é capacidade técnica que o agente carrega sob demanda.** Diferente de role (identidade) e runbook (sequência operacional).

| Categoria | Pergunta que responde | Exemplo |
|---|---|---|
| **Role** (`docs/roles/`) | *Quem é o agente?* | "O Reviewer revisa entregas adversarialmente" |
| **Skill** (`docs/skills/`) | *O que o agente sabe fazer (e quando carregar)?* | "Como detectar smells de violação hexagonal — carregar quando task toca application/ ou infra/" |
| **Runbook** (`docs/runbooks/`) | *Que sequência seguimos?* | "Checklist pré-merge: build → lint → testes → ..." |

Se um arquivo responde *quem*, é role. Se responde *o que sei fazer (e em que situação)*, é skill. Se responde *que passos sigo*, é runbook.

## Quando criar skill (regra do reuso OU regra do contexto raro)

Skill nasce quando **≥1 das duas condições** bate (ADR 0015 §4):

- **(a) Reuso (`load_pattern: shared`)** — ≥2 roles usam a capacidade **ou** ≥2 dispatches/master-prompts repetem o mesmo bloco. Exige `used_by` ≥ 2.
- **(b) Contexto raro (`load_pattern: contextual`)** — 1 role usa, mas só em situação específica (não em toda invocação) **e** o conteúdo é não-trivial (>30 linhas como heurística). Exige gatilho **literal** no `description` ("carregar quando ..."). Aceita `used_by` = 1.

Casos típicos que justificam skill:

- **Reuso (a):** capacidade técnica densa que aparece no checklist de **mais de um papel** (ex.: leitura de arquitetura hex eventualmente compartilhada Reviewer + Architect + planner); bloco de instruções **repetido literalmente** em ≥2 dispatches.
- **Contexto raro (b):** checklist denso usado por **um único role** mas só em situação específica que não acontece sempre (ex.: smells de arquitetura hex usados só pelo Reviewer em PRs que tocam camadas — não em PR de CSS).

Casos que NÃO justificam skill:

- Conhecimento usado por um único role **em toda invocação** → fica no `role.md`.
- Conhecimento que sempre tem que estar carregado → CLAUDE.md ou skill `always-on` no role (= `load_pattern: shared` listada em `skills:` no frontmatter do role).
- Conceito formativo pro humano → `docs/aprendizado/`.
- Processo organizacional fixo → `docs/runbooks/`.
- Decisão arquitetural canônica → ADR em `docs/decisions/`.

**Operacionalização do "≥2 dispatches":** a contagem precisa ser **citada literalmente no `description`** (`"... carregar quando ... (extraído de DISPATCH-X, role Y)"`). Sem cita, esses dispatches não contam — cai pra `contextual` ou viola a regra.

## Schema do SKILL.md

Cada skill é **um arquivo flat** em kebab-case (sem sub-pastas). O arquivo começa com **frontmatter YAML obrigatório** seguido do corpo em markdown.

### Frontmatter YAML (obrigatório)

Espelha o padrão do Anthropic Skills system (`name` + `description` são os campos canônicos lidos por progressive disclosure pra decidir o trigger) e estende com campos nossos (alinhados com ADR 0007).

```yaml
---
name: leitura-arquitetura-hexagonal
description: Detecta smells de violação de dependência na arquitetura hexagonal — application importando de infra, vazamento de internals de adapter pra application, repos/queries direto em controller. Carregar quando uma task toca código em application/ ou infra/, ou quando o Reviewer está avaliando PR que cruza camadas.
load_pattern: contextual    # shared (≥2 consumidores) | contextual (1 consumidor, uso raro)
used_by: [reviewer]         # tamanho 1 só é válido com load_pattern: contextual
created: 2026-05-30
adr: 0015
status: ativa               # ativa | deprecated | superseded
---
```

Campos:

- **`name`** *(obrigatório, canônico Anthropic)* — kebab-case, **idêntico ao nome do arquivo sem `.md`**. Identificador único.
- **`description`** *(obrigatório, canônico Anthropic)* — descrição em terceira pessoa que diz **o que a skill faz + quando carregar**. Este texto é o que o agente lê pra decidir o trigger; precisa ser específico e direcionável. Em skill `contextual`, o gatilho precisa ser **literal** (não vago).
- **`load_pattern`** *(obrigatório, extensão nossa)* — `shared` (motivo reuso, exige `used_by` ≥ 2) **ou** `contextual` (motivo gestão de contexto, aceita `used_by` = 1). Veta `always` — se sempre carrega, fica no role.
- **`used_by`** *(obrigatório, extensão nossa)* — lista de roles que carregam esta skill. Se `load_pattern: shared`, **tamanho ≥ 2 obrigatório**. Se `load_pattern: contextual`, tamanho 1 OK.
- **`created`** *(obrigatório, extensão nossa)* — data ISO `YYYY-MM-DD` da criação. Pra rastreabilidade.
- **`adr`** *(opcional, extensão nossa)* — ADR que motivou a skill, se aplicável. Ex.: `0015` (sem prefixo `ADR-`).
- **`status`** *(opcional, extensão nossa)* — `ativa` (default), `deprecated` (substituída, vai sumir), `superseded` (apontar pra skill nova). Pra manutenção evolutiva sem deletar arquivos cegamente.

Outros campos (ex.: `tags`, `last_reviewed`) ficam livres conforme a dor aparecer — frontmatter YAML aceita.

### Corpo (markdown)

Após o frontmatter, o corpo segue esta estrutura:

```markdown
# Skill — <nome curto>

## Quando carregar (gatilho explícito)
Critérios literais que disparam o load. Repete e expande o que está no `description` do frontmatter — esta seção é pra leitura humana detalhada.

## Resumo da capacidade
Uma a três frases. O que esta skill ensina a fazer.

## Pontos-chave / checklist
- Bullets acionáveis.
- Cada item, idealmente, verificável.

## Exemplos (opcional)
Caso concreto do nosso repo, com link pro arquivo/commit.

## Ler junto (opcional)
- ADRs relacionadas
- Outras skills complementares
- Runbooks que usam esta skill
```

**Limite duro: ~150 linhas (frontmatter + corpo).** Se passou, virou doc — quebrar em skills menores ou extrair parte pra aprendizado/architecture.

## Como carregar (role → skill) — fonte da verdade no frontmatter do role

A lista canônica de skills que um role carrega vive no **frontmatter YAML do `docs/roles/<papel>.md`**, alinhada com o padrão Anthropic Skills + Claude Code subagent:

```yaml
---
name: <papel>
description: ...
tools: ...
skills: [skill-x, skill-y]              # always-on — carregadas no bootstrap junto do role
skills_available: [skill-a, skill-b]    # on-demand — agente lê metadata; corpo expande no gatilho
---
```

- **`skills:`** alinha com o canônico Anthropic. Migra direto pro subagent nativo quando o role virar `.claude/agents/<papel>.md`.
- **`skills_available:`** é extensão nossa, materializa o progressive disclosure manual: agente lê só o `description` (frontmatter da skill) por default; corpo carrega quando o gatilho bate.

O corpo do role tem seção `## Skills` que **explica o porquê** de cada carregamento (motivação + gatilho esperado), mas a **fonte da verdade da lista é o frontmatter** — atualiza num lugar só, parseável por script.

Isso replica o progressive disclosure do Skills system da Anthropic: agente lê só metadata (frontmatter) por default; corpo expande quando triggered.

## Métricas de adoção (ADR 0015 §6)

Pré-acordadas pra evitar adotar por estética. Baseline tirado **antes** de extrair a primeira skill.

| Métrica | Alvo |
|---|---|
| Tamanho médio de `docs/roles/*.md` | cair ≥20% após 3 skills extraídas |
| Tamanho médio de master-prompt | ~80 linhas → ~30 |
| Skills duplicadas em 2+ roles | zero |
| Drift detectado-pelo-humano cobertos por role/skill | tendência queda |
| Skills com `used_by` tamanho 1 **e** `load_pattern: shared` | zero (violação da regra do reuso) |

Skills com `used_by` tamanho 1 **e** `load_pattern: contextual` **não são violação** — são exatamente o caso da gestão de contexto.

**Critério de parada:** se em 1 mês acumularmos >8 skills e nenhuma métrica mexer → audita.

## Convenções

- **Forward-only.** Roles existentes não são reescritos. Skill nasce quando dor + reuso (ou contexto raro denso) aparece.
- **Sem sub-pastas** dentro de `docs/skills/`. Estrutura plana ajuda a navegar.
- **Nome em kebab-case.** Descritivo, sem prefixo de categoria (não usar `arch-leitura-hex.md` — usar `leitura-arquitetura-hexagonal.md`). `name` no frontmatter = nome do arquivo sem `.md`.
- **Atualizar em vez de duplicar.** Se nuance nova, edita; se assunto diferente, cria skill separada.
- **Frontmatter YAML obrigatório.** Permite scripts (futuros) lerem metadata sem parsear markdown, igual o `metricas_status.py` faz com status reports.

## Localização canônica

As skills vivem em `.claude/skills/<name>/SKILL.md` — formato canônico Anthropic, descoberto automaticamente pelo Claude Code via semantic matching. Esta pasta (`docs/skills/`) mantém apenas este README como índice e referência de convenções.

## Índice

| Skill (em `.claude/skills/`) | load_pattern | used_by | Descrição curta |
|---|---|---|---|
| `padroes-qualidade-codigo/SKILL.md` | shared | backend, architect, reviewer | SOLID + design patterns + boas práticas. Quando usar cada pattern e o problema que ele resolve. |
| `arquitetura-hexagonal/SKILL.md` | shared | backend, architect, reviewer, planner | Estrutura de camadas, regras de dependência, como implementar/projetar uma feature, detecção de violações. Planner carrega ao escrever spec de feature que cruza camadas. |
| `ciclo-de-sprint/SKILL.md` | contextual | planner | Abertura, condução, fechamento, retro e kaizen — workflow específico do projeto (não Scrum genérico). |
| `escrita-de-dispatch/SKILL.md` | contextual | planner | Como escrever DISPATCH/MASTER-PROMPT sem boilerplate. Resolve o problema dos ~70% genérico identificado no BACKLOG #10. |
| `escrita-de-plano-completo/SKILL.md` | contextual | planner | Como derivar critérios de aceite verificáveis, identificar riscos reais e definir território. Complementa o template — não substitui. |

## Relação com outras pastas

- `docs/roles/` — quem carrega as skills (via frontmatter `skills:` / `skills_available:`). Mantém identidade e território.
- `docs/runbooks/` — sequência operacional. Pode referenciar skills (ex.: runbook de revisão referencia skill de arquitetura).
- `docs/aprendizado/` — conceito formativo pro humano. Skill é instrução pro **agente**; aprendizado é resumo pro **humano**. Podem coexistir e referenciar um ao outro.
- `docs/decisions/` — ADR define quando uma skill nasce/morre se a decisão for arquitetural (ex.: esta pasta nasce na ADR 0015).
- `docs/architecture/` — arquitetura do **produto/sistema**. Não confundir com arquitetura do nosso processo (que é aqui).
