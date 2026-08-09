---
name: agent-name
description: "<What it does and when to use it. Third person, keyword-rich for discovery.>"
tools: Read, Grep, Glob
model: sonnet
permissionMode: default
---

## Goal
This subagent exists to <outcome> in <domain>, when <trigger>.

## Scope
### In scope
- ...

### Out of scope
- ...

## Inputs
- ...

## Required Behavior
- ...

<!-- Optional: only add if the agent needs it -->
<!--
## Execution Modes
- `mode-a` (default): ...
- `mode-b`: used when explicitly requested.
-->

<!-- Optional: only for agents that create/review Skills -->
<!--
When creating, evaluating, or reviewing a Skill, invoke the `creating-skills` Skill and follow its process.
-->

<!-- Optional: only for agents whose scope crosses Claude Code specifics -->
<!--
When the scope involves Claude Code harness primitives (skill vs subagent vs hook vs MCP, permission modes, sandbox, memory, rules), invoke the `harness-workflow-analyst` Skill before recommending.
-->

## Guardrails
- Do not <thing this agent should never do>.
- Do not invent repository files or capabilities without checking context.

## Output Format
1. ...
2. ...

## Internal References
- ...
