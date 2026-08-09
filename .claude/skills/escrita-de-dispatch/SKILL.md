---
name: escrita-de-dispatch
description: Como escrever um DISPATCH ou MASTER-PROMPT que um agente implementador executa sem ambiguidade e sem boilerplate desnecessário. Carregar quando o humano pede "despacha a task X", "escreve o prompt pra overnight", "prepara o dispatch de FIX-NNN", ou quando o planner vai criar um arquivo DISPATCH-*.md ou MASTER-PROMPT-*.md.
load_pattern: contextual
used_by: [planner]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Escrita de Dispatch

## Quando carregar

- Criar `DISPATCH-<task>.md` para task single-task.
- Criar `MASTER-PROMPT-overnight-*.md` para sessão multi-task.
- Qualquer pedido de "escreva o prompt que vou colar no Claude do back/front".

## O problema que esta skill resolve

Análise de 2026-05-29 (BACKLOG #10) mostrou que ~70% do conteúdo dos DISPATCHes era boilerplate genérico: boot sequence, regras duras, criação de branch, escrita de status. Isso já está no `initialPrompt` do agent (`.claude/agents/<papel>.md`) — repetir no dispatch infla e cria drift quando o protocolo muda.

**Regra central:** o dispatch só contém o que é **específico desta task**. Tudo que vale pra toda task do papel já está no agent.

## Template de dispatch single-task (~15 linhas)

```
Você é o Claude do [back/front]. Execute a task [TASK-ID].

**Plano:** docs/sprints/<NN>/plans/<arquivo>.md — leia inteiro antes de começar.

**Específico desta task:**
- Branch: feature/<id>-<slug> a partir de develop
- [Decisão aberta: se o plano não cobre, listar aqui — senão omitir]
- [Dependência de branch: se task depende de outra não mergeada — senão omitir]
- [Atenção especial: risco real identificado no plano — senão omitir]

Ao concluir: chame @reviewer para validar antes de reportar ao humano.
```

Isso é tudo. O agent já sabe: ler CLAUDE.md, criar branch, escrever status report, PRE-MERGE-CHECKLIST.

## Template de dispatch multi-task overnight

```
Você é o Claude do [back/front]. Modo overnight — execute as tasks em sequência,
parando para revisão humana entre cada uma.

**Sequência:**
1. [TASK-ID-A] — plano em docs/sprints/<NN>/plans/<arquivo>.md
2. [TASK-ID-B] — plano em docs/sprints/<NN>/plans/<arquivo>.md

**Específico desta sessão:**
- Regra de parada: após cada task, commitar + anunciar "TASK X commitada. Aguardando revisão."
- Não iniciar a próxima sem sinal do humano.
- [dependência ou decisão cross-task, se houver]

@reviewer valida cada task antes de avançar (ou humano autoriza explicitamente pular).
```

## Quando adicionar contexto extra (exceções justificadas)

Adicionar **apenas** se:
- A task usa skill que o agente não tem no `skills_available` → listar: `"Ao tocar application/, carregar skill arquitetura-hexagonal."`
- Há decisão de produto aberta que o plano não resolveu → listar como `AskUserQuestion` antes de começar.
- Dependência de branch não mergeada → instrução explícita de como criar branch a partir dela + documentar a exceção.

## Anti-patterns

- ❌ Copiar CLAUDE.md ou seções dele no dispatch — o agent lê no boot.
- ❌ Repetir "não faça push", "não faça terraform apply" — já está no `initialPrompt` do agent.
- ❌ Listar o PRE-MERGE-CHECKLIST inline — o agent sabe onde buscar.
- ❌ Escrever 80 linhas pra task que cabe em 15 — sinal de que o **plano** está incompleto. Resolver no plano, não no dispatch.

## Ler junto

- Skill `escrita-de-plano-completo` — se o plano está vago, o dispatch não resolve
- `.claude/agents/<papel>.md` — verificar o `initialPrompt` antes de escrever o dispatch
