# Claude Code subagent frontmatter reference

## Table of contents

- Source of truth
- Fields
- Model field
- Tools field
- Permission mode
- Skills field
- MCP servers field
- Memory field
- Hooks field
- Isolation and background
- Locations and file extension
- Deprecated and non-existent fields

## Source of truth

This reference reproduces the fields documented in `docs/claude/agentes/02-subagentes.md`. If the doc and this file disagree, the doc wins — update this file to match.

## Fields

| Field | Required | Purpose |
|---|---|---|
| `name` | No | Identifier. Defaults to the file name. Lowercase, hyphens preferred. |
| `description` | Yes | Shown to the model for subagent discovery. Third person, keyword-rich, at most 1024 characters. |
| `tools` | No | Comma-separated tool names the subagent may use. Omit to inherit the parent's tools; `disallowedTools` blocks specific tools. |
| `disallowedTools` | No | Comma-separated tool names to block, even if inherited. |
| `model` | No | Alias, full ID, or `inherit`. Fallback array also accepted. Defaults to the picker default. |
| `permissionMode` | No | One of `default`, `acceptEdits`, `auto`, `dontAsk`, `bypassPermissions`, `plan`, `manual`. |
| `maxTurns` | No | Cap on turns the subagent may run. |
| `skills` | No | Skills whose content should be preloaded into the subagent context. |
| `mcpServers` | No | MCP servers available to the subagent, inline or by reference. |
| `hooks` | No | Lifecycle hooks scoped to this subagent (`PreToolUse`, `PostToolUse`, `SubagentStart`, `SubagentStop`, etc.). |
| `memory` | No | Memory scope: `user`, `project`, or `local`. |
| `background` | No | Whether the subagent runs in background. Default is `true` from v2.1.198 onward. |
| `effort` | No | Reasoning effort: `low`, `medium`, `high`, `xhigh`, `max`. |
| `isolation` | No | Set to `worktree` to run in an isolated git worktree. |
| `color` | No | Display color in the agent view. |
| `initialPrompt` | No | Prompt automatically sent when the subagent starts as its own session. |

## Model field

Accepted values:

    model: sonnet            # alias
    model: opus              # alias
    model: haiku             # alias
    model: fable             # alias
    model: claude-opus-4-8   # full ID
    model: inherit           # use the parent session's model

Fallback array (tried in order until one is available):

    model: [opus, sonnet, haiku]

Do not use vendor-format model names such as `Claude Sonnet 4.5 (copilot)` or `GPT-5 (copilot)` in a Claude Code subagent. That format belongs to VS Code Custom Agents; see `FRONTMATTER-vscode-archived.md` for the archived reference.

## Tools field

Tool names are uppercase, as they appear in the Claude Code tool reference (`docs/claude/harness/04-referencia-de-ferramentas.md`). Comma-separated:

    tools: Read, Grep, Glob

To grant no tools:

    tools: []

To inherit the parent's tools, omit the field entirely.

Use `disallowedTools` to remove specific tools even when others are inherited:

    disallowedTools: Bash, WebFetch

## Permission mode

    permissionMode: default          # ask before each action (also aliased as `manual`)
    permissionMode: acceptEdits      # auto-accept edits, still prompt for Bash
    permissionMode: auto             # auto mode with classifier
    permissionMode: dontAsk          # only preapproved actions run
    permissionMode: bypassPermissions # no prompts — only in hardened containers
    permissionMode: plan             # design-only, no execution

## Skills field

Preloads the full body of listed skills into the subagent's context:

    skills:
      - reviewing-code-premises
      - workflow-gates-core

Use for skills whose content the subagent needs from the first turn. Do not list skills that would only ever be triggered by discovery — those should stay lazy.

## MCP servers field

Inline definition:

    mcpServers:
      github:
        transport: http
        url: https://api.github.com/mcp

Or reference an already-configured server by name:

    mcpServers: [github, sentry]

## Memory field

    memory: user       # persists across all projects for this user
    memory: project    # persists inside this repo, shared with team
    memory: local      # persists inside this repo, per user, ignored by git

Omit when the subagent should not persist across sessions.

## Hooks field

Inline hooks scoped to this subagent only:

    hooks:
      PreToolUse:
        - matcher: "Bash"
          hooks:
            - type: command
              command: ./scripts/validate-bash.sh

Refer to `docs/claude/hooks/02-referencia-de-hooks.md` for the full event list.

## Isolation and background

    isolation: worktree   # subagent runs in a git worktree copy of the repo
    background: true      # default from v2.1.198; set false to run synchronously

Nested subagents are supported up to 5 levels deep, per `docs/claude/agentes/02-subagentes.md`.

## Locations and file extension

| Path | Scope | Extension |
|---|---|---|
| `.claude/agents/*.md` | Project (shared with team) | `.md` |
| `~/.claude/agents/*.md` | User profile | `.md` |

The Claude Code subagent extension is `.md`. Files with `.agent.md` at that path are typically VS Code Custom Agents that Claude Code accepts leniently; validator flags them as legacy.

## Deprecated and non-existent fields

Do not use in a Claude Code subagent:

- `handoffs` — VS Code Custom Agent feature; not part of the Claude Code subagent spec. Use `SendMessage` for peer-to-peer messaging between agents in a team.
- `argument-hint` — VS Code Custom Agent feature.
- `target` — VS Code Custom Agent feature.
- `mcp-servers` (with hyphen) — Claude Code uses `mcpServers` (camelCase).
- `infer` — deprecated across both specs; use `user-invocable` / `disable-model-invocation` in VS Code, and appropriate discovery fields in Claude Code.
- Vendor-format `model` values (`Claude Sonnet 4.5 (copilot)`, `GPT-5 (copilot)`) — VS Code format. Use aliases or full IDs.
