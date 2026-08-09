# Catalog of Claude Code harness primitives

## Table of contents

- How to read this catalog
- Skill
- Subagent
- Hook
- MCP server
- Slash command
- Plugin
- Output style
- Worktree
- Routine
- Agent teams
- Advisor
- Headless mode
- Fast mode
- Checkpoint
- Agent view
- Permission mode
- Sandbox
- CLAUDE.md and memory
- Path-scoped rules

## How to read this catalog

Each entry has: one-sentence purpose, when to use, when not to use, and the doc anchor. The catalog is a short-list source; final selection uses `HEURISTICAS.md`.

## Skill

- **Purpose**: Markdown file with reusable instructions and workflows, loaded on demand.
- **When to use**: The same procedure is repeated across sessions; reference material must be consulted; a slash command must invoke a defined flow.
- **When not to use**: The behavior must always fire (use a hook); the knowledge is fully generic (do not create a skill).
- **Doc**: `docs/claude/skills/01-skills.md`.

## Subagent

- **Purpose**: Isolated context with its own window, own tool set, and own model.
- **When to use**: Work reads many files but returns a short summary; parallel exploration; isolation from the main conversation; independent adversarial review.
- **When not to use**: Trivial task where token cost of a second model call outweighs isolation benefit.
- **Doc**: `docs/claude/agentes/02-subagentes.md`.

## Hook

- **Purpose**: Deterministic script, HTTP call, prompt, MCP tool, or subagent triggered on a lifecycle event.
- **When to use**: Behavior must fire every time an event occurs, without model discretion; block or allow an action deterministically; inject or re-inject context.
- **When not to use**: Behavior is consultative or depends on model judgment (use a skill).
- **Doc**: `docs/claude/hooks/01-guia-de-hooks.md`; event reference at `docs/claude/hooks/02-referencia-de-hooks.md`.

## MCP server

- **Purpose**: External process exposing tools, resources, and prompts via Model Context Protocol.
- **When to use**: The action requires an external service with OAuth or session state; there are dozens of related operations; resources must be linkable via `@resource`.
- **When not to use**: A single Bash command plus permission allowlist covers the case.
- **Doc**: `docs/claude/mcp/02-mcp-guia-completo.md`.

## Slash command

- **Purpose**: Short trigger typed by the user to invoke a skill or workflow.
- **When to use**: A skill is used often enough that autocomplete matters.
- **When not to use**: The skill is model-invoked only.
- **Doc**: `docs/claude/skills/02-comandos-slash.md`.

## Plugin

- **Purpose**: Packaging of skills, agents, hooks, and MCP references as a versioned unit distributable via a marketplace.
- **When to use**: The bundle is reused across multiple repositories or teams; versioning matters.
- **When not to use**: The artifacts are single-repo and evolving frequently.
- **Doc**: `docs/claude/skills/04-plugins.md`, `docs/claude/skills/05-referencia-de-plugins.md`.

## Output style

- **Purpose**: Alternate rendering of output (for example an interactive artifact page instead of terminal text).
- **When to use**: The output benefits from a visual surface: dashboards, timelines, side-by-side diffs.
- **When not to use**: Terminal output is sufficient.
- **Doc**: `docs/claude/skills/03-estilos-de-saida.md`.

## Worktree

- **Purpose**: Git worktree for an isolated session.
- **When to use**: Multiple parallel branches must be worked without checkout collisions; a subagent must operate on a copy.
- **When not to use**: Work is sequential on the same branch.
- **Doc**: `docs/claude/agentes/06-worktrees.md`.

## Routine

- **Purpose**: Scheduled agent run in Anthropic infrastructure (cron-like), triggerable by webhook.
- **When to use**: Task must run on a schedule even when the developer machine is offline; task must react to GitHub events; the action is autonomous or hand-back to a human.
- **When not to use**: Task requires local filesystem access that is not available in the routine environment.
- **Doc**: `docs/claude/agentes/07-rotinas-agendadas.md`.

## Agent teams

- **Purpose**: Multiple independent sessions with a shared task list and peer-to-peer messaging.
- **When to use**: Concurrent hypotheses; multi-piece build where parts progress independently.
- **When not to use**: A single subagent is sufficient; coordination overhead exceeds the parallelism benefit. Feature is experimental.
- **Doc**: `docs/claude/agentes/04-equipes-de-agentes.md`.

## Advisor

- **Purpose**: Escalation to a stronger model at explicit decision points.
- **When to use**: A checkpoint decision is high stakes and the running model is small; second opinion on architecture or risk.
- **When not to use**: The running model is already the strongest available.
- **Doc**: `docs/claude/agentes/08-ferramenta-advisor.md`.

## Headless mode

- **Purpose**: Non-interactive execution (`claude -p "prompt"`) returning structured output.
- **When to use**: CI pipeline; fan-out over many files; scripted invocation.
- **When not to use**: Task requires human back-and-forth.
- **Doc**: `docs/claude/harness/09-modo-headless.md`.

## Fast mode

- **Purpose**: Latency-optimized execution mode.
- **When to use**: Not fully specified in the current docs — treat as `UNCONFIRMED` and validate against `docs/claude/harness/10-fast-mode.md` before recommending.
- **When not to use**: Same caveat.
- **Doc**: `docs/claude/harness/10-fast-mode.md`.

## Checkpoint

- **Purpose**: Automatic snapshot of the conversation and workspace before each prompt, enabling `/rewind`.
- **When to use**: Exploratory work where alternate paths must be tried; recovery from a bad edit.
- **When not to use**: Never a recommendation on its own — it is always available; only mention when the workflow benefits from explicit rewind.
- **Doc**: `docs/claude/harness/08-checkpoints.md`.

## Agent view

- **Purpose**: Visual dashboard for monitoring parallel background sessions.
- **When to use**: Multiple concurrent subagents or teams whose progress must be observed.
- **When not to use**: Single-session interactive work.
- **Doc**: `docs/claude/agentes/03-visualizacao-de-agentes.md`.

## Permission mode

- **Purpose**: Policy that controls how many actions require user confirmation.
- **Values**: `default`, `acceptEdits`, `auto`, `dontAsk`, `bypassPermissions`, `plan`.
- **When to use**: Every recommendation that runs tools must pick one; combine with an allowlist.
- **Never**: `bypassPermissions` outside a hardened container.
- **Doc**: `docs/claude/configuracao/04-modos-de-permissao.md`, `docs/claude/configuracao/03-permissoes.md`.

## Sandbox

- **Purpose**: OS-level isolation for filesystem and network access from Bash tool calls.
- **When to use**: Shared machine; untrusted code execution; policy requires isolation; auto mode is on.
- **When not to use**: Purely local single-developer machine with trusted repo — sandbox may add friction without benefit.
- **Doc**: `docs/claude/configuracao/12-sandboxing.md`.

## CLAUDE.md and memory

- **Purpose**: Persistent per-project or per-user context.
- **When to use CLAUDE.md**: Team-shared conventions, architecture decisions, small (< 200 lines).
- **When to use auto-memory**: Claude discovers a recurring project pattern (build commands, test commands, debug tricks).
- **When not to use**: The rule is scoped to a file type or path — use path-scoped rules instead.
- **Doc**: `docs/claude/configuracao/07-memoria-e-claude-md.md`.

## Path-scoped rules

- **Purpose**: Instructions that fire only when Claude touches files matching a glob.
- **When to use**: The rule is specific to a directory or file type (for example `api/**/*.ts`).
- **When not to use**: The rule is repository-wide.
- **Doc**: Cross-references in `docs/claude/configuracao/07-memoria-e-claude-md.md` and `docs/claude/skills/01-skills.md` (`paths` frontmatter).
