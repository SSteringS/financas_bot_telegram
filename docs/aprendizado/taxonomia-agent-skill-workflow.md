# Taxonomia agent / role / skill / workflow em sistemas multi-agente

## Contexto da dúvida

Em 2026-05-30, o humano trouxe um estudo feito com o Gandalf (IA de estudo no ChatGPT) após concluir o curso oficial da Anthropic sobre Subagents. A modelagem do Gandalf separa quatro conceitos que costumam ser confundidos: **Agent**, **Role**, **Skill** e **Workflow**, e propôs aplicar isso à estrutura do nosso projeto (`.claude/agents/`, `.claude/skills/`, `.claude/workflows/`).

A discussão validou o núcleo da taxonomia, mas calibrou três pontos pra realidade do nosso setup multi-sessão. Materializada na ADR 0015 (`docs/decisions/0015-taxonomia-roles-skills-workflows.md`) e na spec `docs/skills/README.md`.

## Resumo destilado

Quatro conceitos com fronteiras claras (pergunta que cada um responde):

- **Role** — *quem o agente é*. Identidade persistente, território, postura.
- **Agent / Sessão / Subagent** — *quem executa*. É a **implementação** do role. Pode ser uma sessão Claude inteira (nosso caso hoje), um subagent nativo invocado via Task tool (Claude Code), ou um modelo dedicado.
- **Skill** — *o que o agente sabe fazer*. Capacidade técnica reutilizável, carregada sob demanda. Análoga a um módulo RAG: recupera-se quando o gatilho aparece.
- **Workflow** — *que sequência seguimos*. Processo operacional repetível que coordena agentes e skills. No nosso repo, **runbook** já cumpre esse papel.

Três calibragens importantes pra não cair em armadilha:

### 1. Role ≠ Agent (no nosso setup)

A simplificação "Role = Agent" só funciona em ambientes onde o subagent nativo é a única implementação do role (Claude Code com `.claude/agents/`). No nosso setup multi-sessão (planner em Cowork desktop, back/front em Claude Code no IntelliJ), o role é **lido sob demanda** por uma sessão que vira esse papel — a sessão é a implementação. Subagent nativo é evolução futura, não realidade atual. Confundir os três faz pensar que precisa migrar pra `.claude/agents/` agora, mas o ADR 0005 §1 deliberadamente escolheu não fazer isso (Cowork não escreve em `.claude/`, setup é multi-sessão).

### 2. Skill como context engineering / RAG

A intuição do Gandalf — "skill = módulo de contexto carregado sob demanda" — bate com o que a Anthropic publicou sobre o Skills system: SKILL.md curto carrega só metadata; corpo expande quando triggered (progressive disclosure). A nuance pro nosso caso: o "retriever" é o **agente lendo o arquivo quando o gatilho dispara**, não retrieval automatizado. Logo, **os gatilhos precisam ser explícitos** no role.md — instrução literal "se task X, leia skill Y". Não confiar em recall do modelo.

Vale o paralelo:

```
RAG:    Pergunta → Recupera doc → Injeta contexto → Responde
Skill:  Problema → Recupera skill → Injeta contexto → Executa
```

A diferença com o Skills system real é que no nosso caso o "recupera" é manual via leitura, não automatizado via embedding/match.

### 3. Workflow já existe como runbook — não criar pasta nova

`docs/runbooks/` (PRE-MERGE-CHECKLIST, PREP-WA, ROTEIRO-TESTES-BACKEND) é nosso workflow. Criar `docs/workflows/` sobrepõe e gera confusão. Critério: **runbook = processo organizacional + agentes; skill = conhecimento técnico que o agente aplica.**

## Pontos-chave

- **Role** responde *quem*; **Skill** responde *o quê*; **Runbook/Workflow** responde *como (passos)*; **ADR** responde *por que (decisão persistente)*.
- **No nosso setup:** Role (conceito) → Sessão especializada (implementação atual) → Subagent nativo (implementação futura possível). Três níveis distintos, não dois.
- **Skill é compartilhamento**, não decoração — só nasce com **regra do 2x** (≥2 roles ou ≥2 dispatches usando). Capacidade de um papel só fica no `role.md`.
- **Gatilho da skill é literal** no role — "se tocar camada X, leia skill Y". O agente não infere; ele lê o que está escrito.
- **Workflow = runbook** no nosso vocabulário. Não criar 5ª categoria.
- **Forward-only.** Migração nasce de dor + reuso, não de bonito. Role existente não é reescrito; skill nasce quando precisa.
- **Métrica pré-acordada com critério de parada** evita virar fim em si. Se em 1 mês acumular >8 skills sem reduzir tamanho de role/dispatch → audita.
- **Skill análoga a RAG**, mas com retrieval manual (gatilho explícito) em vez de automatizado.
- **Cowork Skills system** (docx, xlsx, etc.) é coisa diferente — são output-format/tool-handling. Nossas skills são domain knowledge interno do projeto. Não unificar.
- **5 skills candidatas** identificadas no item #10 do `BACKLOG-evolucao-workflow.md`, mas piloto primeiro: `leitura-arquitetura-hexagonal` (cobre item #8 do backlog).

## Pra aprofundar

- **Skills system da Anthropic** — progressive disclosure, schema do SKILL.md, gatilhos. O design da pasta `docs/skills/` é inspirado direto nele, com adaptação pra retrieval manual.
- **Subagents nativos do Claude Code** (`.claude/agents/`, Task tool) — implementação alternativa do role como subprocesso dentro de uma sessão. Útil quando o workflow precisar de delegação programática (ex.: Reviewer invocado automaticamente ao fim do dispatch). Hoje não compensa o setup, mas vira opção quando a dor justificar.
- **Context engineering** como disciplina — escolher *o que* carregar pra cada momento da execução é tão importante quanto *o que* o modelo sabe. Skills são uma materialização dessa disciplina.
- **Pattern catalog de sistemas multi-agente** — Crew AI, AutoGen, LangGraph têm taxonomias próprias (agent, tool, role, team, supervisor). Vale comparar as fronteiras pra ver onde nosso vocabulário converge/diverge.
- **ADR 0005, 0011, 0015** — trilha da nossa evolução: separação de papéis → adoção do arquiteto → separação role/skill. Cada uma resolve a dor da anterior conforme o repo cresce.
