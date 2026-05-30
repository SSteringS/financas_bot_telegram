# Papéis (sessões especializadas)

Arquivos de instrução por papel. Cada um especializa uma sessão Claude — reduz diluição de contexto e viés, e dá independência de revisão. Decisão: ADR `0005` (estende `0004`).

## Como usar (bootstrap de sessão)

Ao abrir uma sessão nova, mande ler o global + o papel + o doc da task:

> "Leia `CLAUDE.md` e `docs/roles/reviewer.md`. Você atuará como Reviewer. Revise a task X (`docs/plans/...` + `docs/sprints/<NN>/status/...`)."

## Regra de ouro destes arquivos

**São delta, não cópia.** `CLAUDE.md` é a fonte única das regras globais. Cada arquivo de papel contém só o específico do papel e **referencia** os canônicos (`CLAUDE.md`, `PRE-MERGE-CHECKLIST.md`, ADRs) — nunca os duplica. Se um arquivo de papel começar a repetir regra global, está errado: aponte pro canônico.

## Papéis

- [`planner.md`](planner.md) — coordena, specs, ADRs, docs. Dono da arquitetura. Não implementa, não é o revisor final.
- [`backend.md`](backend.md) — implementa o backend no seu território.
- [`frontend.md`](frontend.md) — implementa o frontend no seu território.
- [`reviewer.md`](reviewer.md) — revisão independente e adversarial; verifica contra a realidade, não contra o relatório.

`architect.md` e `qa.md` ficam adiados (ADR 0005) — arquitetura cabe no planner, QA no checklist do implementador + reviewer, até a dor justificar.

## Localização

Em `docs/roles/` (não `.claude/`, que é protegido pra escrita da sessão de planejamento). São lidos sob demanda, então a localização é funcionalmente indiferente. `.claude/agents/` fica reservado pra subagents nativos no futuro.
