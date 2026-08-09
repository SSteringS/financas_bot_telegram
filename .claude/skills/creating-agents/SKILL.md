---
name: creating-agents
description: Creates, reviews, and refactors Claude Code subagent files in `.claude/agents/*.md`, following the official spec in `docs/claude/agentes/02-subagentes.md`. Use when defining a new subagent, choosing its tools, model, permission mode, memory scope, MCP servers, hooks, or isolation, and when auditing or migrating an existing agent file to the current spec.
---

# Creating Agents

## Objective

<!-- inclusion-reason: Anchors the skill to a single artifact type — a Claude Code subagent file — before any tooling decisions. -->

Produce and maintain single-responsibility Claude Code subagent files under `.claude/agents/*.md`, aligned with the official spec in `docs/claude/agentes/02-subagentes.md` and validated by `scripts/validate_agent.py`.

## When to use

<!-- inclusion-reason: Discovery trigger for the skill. -->

Use this Skill when:

- Creating a new Claude Code subagent (`.claude/agents/<name>.md`).
- Reviewing, refactoring, or migrating an existing agent file.
- Deciding the subagent's tools, model, permission mode, memory scope, MCP servers, hooks, or isolation.
- Fixing YAML frontmatter that fails `validate_agent.py`.
- Migrating a legacy `.agent.md` (VS Code format) to a Claude Code subagent `.md`.

## When not to use

<!-- inclusion-reason: Prevents this skill from absorbing responsibilities that belong to other skills or agents. -->

Do not use this Skill for:

- Creating or reviewing a Skill (`SKILL.md`) — use `creating-skills` instead.
- Deciding whether a workflow needs a subagent at all — that judgment belongs to `harness-workflow-analyst` (which analyzes primitives) or to the requesting agent (for example `ai-engineer`).
- Creating VS Code Custom Agents (`.agent.md` under `.github/agents/`) — see `FRONTMATTER-vscode-archived.md` for the historical reference; this skill no longer authors that format.
- Executing the domain task the subagent will perform once created.
- Creating instruction files (`*.instructions.md`) or prompt files (`*.prompt.md`).

## Minimum required data

<!-- inclusion-reason: A subagent authored without minimum data becomes vague and cannot be validated. -->

Identify, before writing:

1. Single responsibility in one sentence: "This subagent exists to <outcome> in <domain>, when <trigger>."
2. Trigger scenarios (when the parent agent or user should pick it).
3. Scope boundaries: in scope / out of scope.
4. Minimal tool set required (Claude Code uppercase names, or `[]` for none).
5. Model preference (`sonnet` / `opus` / `haiku` / `fable` / full ID / `inherit`), with fallback if applicable.
6. Permission mode (`default` / `acceptEdits` / `auto` / `dontAsk` / `bypassPermissions` / `plan` / `manual`).
7. Memory scope (`user` / `project` / `local`) or none.
8. Whether the subagent needs `mcpServers`, `hooks`, `skills` (preloaded), `isolation: worktree`, or `background`.
9. Expected output format.

## If information is missing

<!-- inclusion-reason: Prevents inventing frontmatter fields, tool names, or model names that are not confirmed in the docs. -->

- Do not invent tools, MCP servers, or model names not present in `docs/claude/`.
- Ask only when the missing data blocks a safe scope definition.
- State non-critical assumptions explicitly.
- Record indispensable missing information as `PENDING_DEFINITION`.

## Mandatory process

<!-- inclusion-reason: The ordering (scope first, then frontmatter, then body, then validation) prevents rework loops. -->

1. Confirm the target location: project (`.claude/agents/`) or user profile (`~/.claude/agents/`).
2. Write the single-responsibility sentence. If it needs "and" to join unrelated outcomes, split into separate subagents.
3. Choose frontmatter fields following [FRONTMATTER.md](FRONTMATTER.md). Only `description` is required; include optional fields only when the subagent needs the behavior.
4. Draft the body using [templates/AGENT.template.md](templates/AGENT.template.md). Keep only the sections the role actually needs.
5. Write explicit in-scope / out-of-scope boundaries, guardrails, and output format.
6. If the subagent creates, reviews, or evolves Skills, add a body instruction to invoke `creating-skills`.
7. If the subagent's scope crosses Claude Code harness decisions (skill vs subagent vs hook vs MCP, permission mode, sandbox, memory), add a body instruction to invoke `harness-workflow-analyst`.
8. Save as `.md` (Claude Code) in `.claude/agents/`. If migrating a legacy `.agent.md`, rename after confirming the tooling picks up the new name.
9. Run `python scripts/validate_agent.py <path-to-agent-file>`.
10. Fix reported errors. Warnings on legacy fields (vendor-format model, lowercase tools, `.agent.md`, `handoffs`) are acceptable during migration but must be tracked as pending items.
11. Deliver the file, the decisions made, and any pending items.

## Mandatory rules

<!-- inclusion-reason: Invariants that a subagent author must never violate. -->

- One subagent, one role — do not merge unrelated responsibilities.
- `description` is required, third person, keyword-rich, at most 1024 characters.
- `tools` uses Claude Code uppercase names (`Read`, `Grep`, `Bash`) — not VS Code lowercase aliases.
- `model` uses aliases (`sonnet`, `opus`, `haiku`, `fable`), full IDs (`claude-*`), or `inherit` — not vendor-format (`Model Name (vendor)`).
- `permissionMode` must be one of `default`, `acceptEdits`, `auto`, `dontAsk`, `bypassPermissions`, `plan`, `manual`.
- Never set `permissionMode: bypassPermissions` outside a hardened container.
- Do not include VS Code-only fields (`handoffs`, `argument-hint`, `target`, `mcp-servers`, `user-invocable`, `disable-model-invocation`, `infer`) in a Claude Code subagent.
- Scope boundaries (in scope / out of scope) must be explicit, not implicit.
- Do not reference files, tools, models, or MCP servers that are not confirmed to exist in the repo or the environment.
- Keep the body implementable in the current repo structure.

## Additional resources

<!-- inclusion-reason: References are load-bearing enough that a top-level section improves discovery for reviewers. -->

- Frontmatter field reference: [FRONTMATTER.md](FRONTMATTER.md).
- Legacy VS Code Custom Agent reference (archived, not for new work): [FRONTMATTER-vscode-archived.md](FRONTMATTER-vscode-archived.md).
- Default body structure: [templates/AGENT.template.md](templates/AGENT.template.md).
- Representative scenarios: [evaluations/](evaluations/).
- Structural validator: run `python scripts/validate_agent.py <agent-file-or-directory>`.

## Stop criteria

<!-- inclusion-reason: Prevents shipping an ambiguous or misclassified artifact. -->

- Stop if the single responsibility cannot be stated without joining unrelated outcomes.
- Stop if the request actually describes a Skill, an Instructions file, or a Prompt file — redirect to the right primitive.
- Stop if the recommendation is to create the subagent, but the workflow-level decision (should this be a subagent at all?) has not been made — delegate to `harness-workflow-analyst`.
- Do not declare the agent ready if the validator reports any error.

## Completion criteria

<!-- inclusion-reason: Objective "done" definition prevents shipping incomplete files. -->

A subagent file is ready when:

- It has an explicit single responsibility and scope boundaries.
- `description` is present, third person, and discovery-oriented.
- `tools`, `model`, and `permissionMode` are minimal and justified.
- Body sections match only what the role needs (no forced boilerplate).
- It passes `scripts/validate_agent.py` with no errors.
- Any remaining warnings (legacy format during migration) are documented as pending items.
- Assumptions and pending items are documented.

## Output format

<!-- inclusion-reason: The consumer (usually the ai-engineer agent) needs a stable delivery contract. -->

1. Agent summary (role, scope, main triggers).
2. Frontmatter decisions (tools, model, permission mode, memory, hooks, MCP servers, isolation) and rationale for each field included or omitted.
3. Complete file content.
4. Validation command and result.
5. Assumptions, limitations, and pending items (including any legacy-format migration items).
