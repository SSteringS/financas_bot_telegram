# Known trade-offs

## Table of contents

- Context window vs subagent isolation
- Automation vs human review
- Permission strictness vs speed
- CLAUDE.md size vs effectiveness
- Hook determinism vs skill flexibility
- MCP capability vs native tool simplicity
- Routine autonomy vs local visibility
- Worktree isolation vs coordination cost
- Auto mode reach vs blast radius

## Context window vs subagent isolation

- Subagent isolates context, so the main conversation stays lean.
- Cost: an extra model call, extra latency, and a summary that may drop nuance.
- Rule: subagent wins when the reading load is high or the output is a distilled summary; loses when the work is short and the details matter downstream.

## Automation vs human review

- Hooks fire deterministically; skills are consultative.
- Cost of automation: silent failure surface grows; a broken hook blocks the workflow without model reasoning.
- Rule: automate only what is truly invariant. Everything with judgment stays in a skill, gated by a permission mode.

## Permission strictness vs speed

- `default` mode is safe but slow; `auto` is fast but delegates classification.
- Cost: `auto` can approve an action that the user would have questioned if asked; `default` can drain patience and reduce careful review.
- Rule: pick strictness by blast radius and reversibility. Combine with a `deny` list for the few actions that are non-negotiable.

## CLAUDE.md size vs effectiveness

- Every line in `CLAUDE.md` costs context tokens on every turn.
- Beyond roughly 200 lines, the model starts to lose rules in the noise.
- Rule: move procedural knowledge to skills (load on demand), and move file-type conventions to path-scoped rules.

## Hook determinism vs skill flexibility

- A hook always runs; a skill is invoked when discovery matches.
- Cost of hook: rigidity; it cannot adapt to context beyond its script logic.
- Cost of skill: it can be skipped or misapplied by the model.
- Rule: use a hook to enforce; use a skill to guide.

## MCP capability vs native tool simplicity

- MCP unlocks external services with rich schemas.
- Cost: an extra process to start, monitor, and secure; another surface for context and permissions.
- Rule: prefer native tools (`Bash`, `Read`, `Edit`, `WebFetch`) plus a permission allowlist until the shape of the work justifies MCP.

## Routine autonomy vs local visibility

- A routine runs when the developer is offline and can act on webhooks.
- Cost: harder to observe; failures may go unnoticed until output review; secrets management is more sensitive.
- Rule: recommend a routine only when the schedule or event trigger is a hard requirement; add a notification step (Slack, GitHub comment) so failures are visible.

## Worktree isolation vs coordination cost

- Worktrees keep parallel branches from stepping on each other.
- Cost: each worktree is a separate working directory; changes must be reconciled at the end.
- Rule: use for genuine parallelism, not for sequential tasks that happen to be on different branches.

## Auto mode reach vs blast radius

- Auto mode lets long tasks run without permission prompts.
- Cost: a bad classification silently authorizes something the user would have blocked.
- Rule: pair auto mode with an explicit `deny` list of destructive patterns (`Bash(rm -rf *)`, `Bash(git push --force *)`, `Edit(.env)`), and pair with sandbox when the machine is shared.
