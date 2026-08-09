---
name: harness-workflow-analyst
description: Analyzes development scenarios that use the Claude Code harness and recommends the best combination of harness primitives (skills, subagents, hooks, MCP servers, slash commands, plugins, output styles, worktrees, routines, permission modes, sandbox, memory, rules) with explicit trade-offs, guardrails, and adoption criteria. Use when a user or another agent asks how to structure a workflow with Claude Code, which primitive fits a scenario, how to combine primitives, or when reviewing an existing Claude Code workflow.
---

# Harness Workflow Analyst

<!-- section-policy: The 10 headings below are currently required by scripts/validate_skill.py in .claude/skills/creating-skills/. After the Phase-2 refactor of creating-skills, only the official spec fields will be required and each optional section will keep the inclusion-reason / omission-reason HTML comment. The comments below are already in the target format. -->

## Objective

<!-- inclusion-reason: Anchors when the skill should fire versus be skipped. -->

Recommend the best combination of Claude Code harness primitives for a given development scenario, with explicit trade-offs, guardrails, and adoption criteria, grounded in official documentation under `docs/claude/`.

## When to use

<!-- inclusion-reason: Discovery signal for model-invocation and user invocation. -->

Use this Skill when the request is about the Claude Code harness and involves:

- Choosing between skill, subagent, hook, MCP server, slash command, plugin, output style, worktree, routine, agent view, or advisor for a scenario.
- Choosing a permission mode (`default`, `acceptEdits`, `auto`, `dontAsk`, `bypassPermissions`, `plan`) or sandbox configuration.
- Structuring `CLAUDE.md`, memory scopes, path-scoped rules, or instruction files.
- Combining primitives (for example routine + hook + permission rule, or subagent + skill, or MCP + hook).
- Comparing a Claude Code approach against an external tool or manual process.
- Reviewing an existing workflow that already uses Claude Code primitives.

## When not to use

<!-- inclusion-reason: Prevents scope inflation into general AI engineering and into artifact production. -->

Do not use this Skill for:

- General AI engineering that is not specific to Claude Code (RAG design, LLM-agnostic prompt engineering, LLM-agnostic tool integration) — that belongs to the `ai-engineer` agent.
- Actually creating a Skill file — delegate to `creating-skills`.
- Actually creating a subagent file — delegate to `creating-agents`.
- Executing the recommended workflow; this Skill only recommends.
- Choosing between LLM vendors or comparing Claude versus other models.
- Product or infrastructure architecture decisions unrelated to the harness.

## Minimum required data

<!-- inclusion-reason: A recommendation without minimum context degrades to generic advice. -->

Before producing a recommendation, identify:

1. Scenario objective in one sentence.
2. Frequency (one-off, recurring on schedule, on-event).
3. Interaction mode (interactive, headless/CI, scheduled/routine).
4. Trust level (local dev, shared team, production infrastructure).
5. Human-in-the-loop requirement (approval before action, review after action, or none).
6. Destructive potential (can it delete data, force-push, deploy?).
7. Context size (does the work read few files or many?).
8. Existing artifacts in the repo (skills, agents, hooks, MCP servers, CLAUDE.md contents).

## If information is missing

<!-- inclusion-reason: Prevents recommendations grounded on assumptions the user did not authorize. -->

- Do not invent Claude Code fields, primitives, or behaviors that are not present in `docs/claude/`.
- Ask for the specific missing data only when its absence blocks a safe recommendation.
- State non-critical assumptions explicitly under an `Assumptions` block in the output.
- Record indispensable gaps as `PENDING_DEFINITION` in the output.

## Mandatory process

<!-- inclusion-reason: The order of analysis is load-bearing — diagnostic must precede primitive selection, which must precede orchestration. -->

1. Read [DIAGNOSTICO.md](DIAGNOSTICO.md) and run its diagnostic questions against the scenario.
2. Consult [PRIMITIVOS.md](PRIMITIVOS.md) to short-list candidate primitives.
3. Apply [HEURISTICAS.md](HEURISTICAS.md) decision rules to narrow the candidates.
4. Check [TRADE-OFFS.md](TRADE-OFFS.md) for known costs and interactions of the combination.
5. Match against a canonical pattern in [PADROES.md](PADROES.md) when applicable.
6. Cross-reference every claim against `docs/claude/` and cite the exact file and section that supports it.
7. Produce the output in the format defined under `## Output format`.
8. If the scenario matches an entry in `evaluations/`, record the observed result there.

## Mandatory rules

<!-- inclusion-reason: These are invariants that a harness analyst must never violate. -->

- Every recommendation must cite the exact `docs/claude/` file that supports the claim.
- Never recommend `bypassPermissions` outside an isolated container or sandbox scenario.
- Never recommend a hook that blocks execution without stating the exit-code contract (`exit 2` blocks; `exit 0` proceeds; JSON `permissionDecision` for fine control).
- Never recommend an MCP server when native tools plus a permission allowlist solve the same case with less overhead.
- Always separate: (a) primitive choice, (b) orchestration, (c) guardrails, (d) rollout — do not conflate them.
- Do not recommend deprecated or unconfirmed features; if `docs/claude/` is silent on a behavior, label it `UNCONFIRMED`.
- Do not respond about Claude Code behavior from memory; every technical claim must be traceable to a doc path.

## Stop criteria

<!-- inclusion-reason: Prevents partial deliverables that look complete. -->

- Stop and ask if the scenario objective is not stated.
- Stop if the scenario is not Claude-Code-specific and redirect to the `ai-engineer` agent.
- Stop if the request is to actually create the artifact and redirect to `creating-skills` or `creating-agents`.
- Do not deliver a recommendation that includes an unconfirmed field or primitive.

## Completion criteria

<!-- inclusion-reason: Objective definition of "done" prevents premature closure. -->

A recommendation is complete when:

- Scenario is restated in one sentence.
- Diagnostic questions are answered.
- Chosen primitive(s) are listed with doc citations.
- At least one rejected alternative is stated with reason.
- Orchestration between primitives is described when more than one is selected.
- Guardrails (permissions, sandbox, hooks, memory scope) are specified.
- Rollout is ordered and includes a rollback path.
- Adoption metric with baseline and target is proposed.
- Assumptions and pending items are separated from the recommendation.

## Output format

<!-- inclusion-reason: The consumer is the ai-engineer agent, which needs a stable contract to compose downstream artifacts. -->

Deliver the response in this order:

1. **Scenario restated** — one sentence.
2. **Diagnostic answers** — bullets from `DIAGNOSTICO.md`.
3. **Recommended primitive(s)** — with doc citations in the form `docs/claude/<path> section <heading>`.
4. **Rejected alternatives** — at least one, with reason.
5. **Orchestration** — how primitives combine, in order.
6. **Guardrails** — permissions, sandbox, hooks, memory scope.
7. **Rollout** — ordered steps and rollback path.
8. **Adoption metric** — baseline, target, adjust rule.
9. **Assumptions and pending items** — separated list.

<!-- omission-reason: `## Additional resources` is intentionally not a separate top-level section. All reference files are already linked from step-by-step positions inside `## Mandatory process`, which preserves the "read this now" ordering the analysis depends on. Duplicating the links in a trailing section would fragment discovery. -->
