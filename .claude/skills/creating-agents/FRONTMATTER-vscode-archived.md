# `.agent.md` frontmatter reference

## Table of contents

- Fields
- Tool aliases
- Model field
- Handoffs
- Locations

## Fields

| Field | Required | Notes |
|---|---|---|
| `description` | Yes | Shown in the agent picker and used for subagent discovery. Third person, keyword-rich. |
| `name` | No | Defaults to the file name. |
| `argument-hint` | No | Hint text shown in the chat input to guide usage. |
| `tools` | No | List of tool aliases, specific tools, tool sets, or `<mcp-server>/*`. Omit for defaults, `[]` for none. |
| `agents` | No | Subagents allowed. `*` = all, `[]` = none, or a list of agent names. Requires the `agent` tool in `tools` if set. |
| `model` | No | Single model name, or an array for ordered fallback. Uses the picker default if omitted. |
| `user-invocable` | No | Default `true`. Set `false` to hide from the agent picker (subagent-only). |
| `disable-model-invocation` | No | Default `false`. Set `true` to prevent use as a subagent. |
| `target` | No | `vscode` or `github-copilot`. |
| `mcp-servers` | No | MCP server config for `target: github-copilot` agents. |
| `handoffs` | No | Suggested transitions to other agents after a response. See below. |
| `hooks` | No | Inline lifecycle hooks (`PreToolUse`, `PostToolUse`, etc.), scoped to this agent. |

`infer` is deprecated — use `user-invocable` and `disable-model-invocation` instead.

## Tool aliases

| Alias | Purpose |
|---|---|
| `execute` | Run shell commands |
| `read` | Read file contents |
| `edit` | Edit files |
| `search` | Search files or text |
| `agent` | Invoke custom agents as subagents |
| `web` | Fetch URLs and web search |
| `todo` | Manage task lists |

Common patterns:

```yaml
tools: [read, search]       # Read-only research
tools: [read, edit, search] # No terminal access
tools: []                   # Conversational only
```

## Model field

```yaml
model: "Claude Sonnet 4.5 (copilot)"
```

Or with fallback, tried in order until one is available:

```yaml
model: ["Claude Sonnet 4.5 (copilot)", "GPT-5 (copilot)"]
```

Use the qualified `Model Name (vendor)` format. Do not invent model names — confirm availability with the user or the model picker if unsure.

## Handoffs

```yaml
handoffs:
  - label: Start Implementation
    agent: implementation
    prompt: Now implement the plan outlined above.
    send: false
    model: GPT-5 (copilot)
```

`send: true` auto-submits the prompt to the target agent; default is `false` (button only).

## Locations

| Path | Scope |
|---|---|
| `.github/agents/*.agent.md` | Workspace |
| `<user-profile>/agents/*.agent.md` | User profile |
