# Decision heuristics

## Table of contents

- How to use these heuristics
- Skill vs subagent vs hook vs MCP
- Interactive vs headless vs routine
- One session vs worktree vs agent teams
- CLAUDE.md vs skill vs rules vs auto-memory
- Permission mode selection
- When to add sandbox
- When to introduce a plugin
- Escalation ladder

## How to use these heuristics

Apply the heuristics in the order they appear here. Each one narrows the candidate set from `PRIMITIVOS.md`. If two heuristics conflict, prefer the one earlier in the list — it addresses a more fundamental property of the scenario.

## Skill vs subagent vs hook vs MCP

Ask, in order:

1. Must the behavior fire every time the event happens, without model discretion?
   - Yes → **hook**.
   - No → next question.
2. Does the work require an external service with state, auth, or dozens of operations?
   - Yes → **MCP server**.
   - No → next question.
3. Does the work read many files or need isolation from the main context?
   - Yes → **subagent** (with a skill loaded if the workflow is reusable).
   - No → next question.
4. Is the same procedure repeated across sessions or shared with the team?
   - Yes → **skill** (add a slash command if it will be invoked often).
   - No → inline the instruction in the current turn.

## Interactive vs headless vs routine

- Developer at the terminal, needs feedback → **interactive** session.
- Runs from CI, cron, or a script; must return structured output → **headless** (`claude -p`, `--output-format json`).
- Must run on a schedule or webhook, possibly while the developer is offline → **routine**.

Combine when appropriate: a routine can run headless and invoke a subagent internally.

## One session vs worktree vs agent teams

- One task, one branch → single session, no worktree.
- Parallel work on multiple branches or need to isolate an agent's edits from your workspace → **worktree**.
- Multiple concurrent workstreams that must exchange messages and share a task list → **agent teams** (experimental — accept the maturity risk).

## CLAUDE.md vs skill vs rules vs auto-memory

- Fact is a team convention that applies to the whole repo and rarely changes → **`CLAUDE.md`** (keep under ~200 lines).
- Fact is scoped to a directory or file type → **path-scoped rules** with a `paths` glob.
- Behavior is a multi-step procedure invoked when the topic comes up → **skill**.
- Fact is a build command, test command, or debug shortcut discovered from real runs → let **auto-memory** capture it.
- Fact must survive across projects and belongs to the developer, not the repo → user-level `CLAUDE.md`.

## Permission mode selection

Match the mode to trust and reversibility:

| Trust and reversibility | Mode |
|---|---|
| Unknown repo, first exploration | `default` |
| Trusted repo, edits are cheap to revert | `acceptEdits` |
| Long autonomous run on a trusted task | `auto` |
| CI or script where only preapproved actions are safe | `dontAsk` |
| Design-only conversation, no execution | `plan` |
| Hardened container, no host risk | `bypassPermissions` |

Combine with a targeted `allow` and `deny` list. Never recommend `bypassPermissions` on a developer host.

## When to add sandbox

Recommend sandbox when at least one of the following holds:

- The machine is shared or has credentials for shared systems.
- Auto mode is on and the workflow runs long.
- Policy or compliance requires filesystem or network isolation.
- The workflow will execute code fetched from external sources.

Do not recommend sandbox when the developer is on a personal machine, the repo is trusted, and the friction cost outweighs the isolation benefit.

## When to introduce a plugin

Introduce a plugin when:

- The skills, hooks, or agents will be reused across multiple repos.
- Version pinning matters.
- A marketplace (public or internal) will distribute the bundle.

Do not create a plugin for single-repo artifacts that still evolve daily — the packaging overhead is not amortized.

## Escalation ladder

When the initial recommendation is ambiguous or high-risk, escalate in this order:

1. Add a diagnostic question (from `DIAGNOSTICO.md`) and re-run.
2. Split the scenario into smaller decisions.
3. Recommend a **plan mode** run before executing.
4. Recommend an **advisor** call at the critical checkpoint.
5. Recommend a human review gate before the destructive step.
