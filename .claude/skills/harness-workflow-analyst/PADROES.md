# Canonical workflow patterns

## Table of contents

- How to match a pattern
- Explore, plan, code, commit
- Test-driven development
- Visual iteration
- Writer and reviewer
- Headless CI fan-out
- Multi-Claude investigation
- Scheduled review on event
- Blocking guardrail
- Long-running autonomous task

## How to match a pattern

Compare the scenario against each pattern's "signals" list. If two or more signals match, use the pattern's primitive combination as the starting point and adapt only the differences.

## Explore, plan, code, commit

- **Signals**: unfamiliar area of the codebase, risk of solving the wrong problem, need to reason before editing.
- **Primitives**: `plan` permission mode → skill for the domain → normal session for the edit → commit.
- **Doc**: `docs/claude/melhores-praticas/01-melhores-praticas.md`.

## Test-driven development

- **Signals**: behavior is easy to describe as a test; regression risk is high.
- **Primitives**: skill that scaffolds the test; interactive session; permission allowlist for the test runner; commit after green.
- **Doc**: `docs/claude/melhores-praticas/02-fluxos-de-trabalho-comuns.md`.

## Visual iteration

- **Signals**: UI or CSS work; the goal is defined by a screenshot or mock.
- **Primitives**: output style if the artifact benefits from a visual surface; skill that owns the compare-and-refine loop.
- **Doc**: `docs/claude/melhores-praticas/02-fluxos-de-trabalho-comuns.md`.

## Writer and reviewer

- **Signals**: independent review must not be biased by the author's own reasoning.
- **Primitives**: subagent (writer) plus subagent (reviewer with a review skill loaded); optional advisor at the merge decision.
- **Doc**: `docs/claude/agentes/02-subagentes.md`, `docs/claude/melhores-praticas/04-revisao-de-codigo.md`.

## Headless CI fan-out

- **Signals**: same operation must run against N files, N services, or N PRs.
- **Primitives**: headless mode with `--output-format json`; script parses output; permission mode `dontAsk` with a strict allowlist.
- **Doc**: `docs/claude/harness/09-modo-headless.md`.

## Multi-Claude investigation

- **Signals**: investigation reads dozens of files; result should not inflate the main conversation.
- **Primitives**: subagent for reading and summarizing; main session receives only the summary.
- **Doc**: `docs/claude/agentes/02-subagentes.md`, `docs/claude/harness/06-janela-de-contexto.md`.

## Scheduled review on event

- **Signals**: reaction to a webhook (for example a GitHub PR opened) or a fixed schedule.
- **Primitives**: routine → skill for the review procedure → notification (Slack, PR comment) as the final step.
- **Doc**: `docs/claude/agentes/07-rotinas-agendadas.md`.

## Blocking guardrail

- **Signals**: a class of actions must never occur (touching `.env`, running `rm -rf`, force-pushing).
- **Primitives**: hook on `PreToolUse` with an exit-2 script; permission `deny` rule as a backstop.
- **Doc**: `docs/claude/hooks/01-guia-de-hooks.md`, `docs/claude/configuracao/03-permissoes.md`.

## Long-running autonomous task

- **Signals**: task takes many turns; user cannot supervise every step.
- **Primitives**: permission mode `auto` with a `deny` list; sandbox on shared machines; notification hook on `Stop`; optional checkpoint markers between phases.
- **Doc**: `docs/claude/configuracao/04-modos-de-permissao.md`, `docs/claude/configuracao/13-modo-automatico.md`, `docs/claude/configuracao/12-sandboxing.md`.
