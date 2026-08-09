# Advanced patterns for Skills

## Table of contents

- When to reach for this file
- Frontmatter fields beyond name and description
- Dynamic context injection
- Argument substitution
- Path-scoped skills
- Fork context (subagent execution)
- Nested skills
- Skills inside plugins
- Legacy compatibility

## When to reach for this file

Consult this file only after `SKILL.md`, `WORKFLOW.md`, and `DESIGN-RULES.md`. The patterns here cover features that are optional in the official spec (`docs/claude/skills/01-skills.md`) but that unlock important behavior when needed.

## Frontmatter fields beyond name and description

The official spec allows these optional frontmatter fields on top of the required `name` and `description`. Include them only when the skill actually needs the behavior; leave them out otherwise.

| Field | Purpose | When to include |
|---|---|---|
| `allowed-tools` | Pre-approve a list of tools the skill may invoke without prompting. | Skill runs deterministic operations and prompts would only add friction. |
| `disallowed-tools` | Block specific tools even if globally allowed. | Skill must never touch destructive tools even if the session allows them. |
| `disable-model-invocation` | Prevent Claude from selecting the skill autonomously. | Skill has side effects that require explicit user intent. |
| `user-invocable` | Set to `false` to hide from the slash-command list. | Skill is meant to be invoked by another agent only, not by the user. |
| `paths` | Glob patterns that scope when the skill triggers. | Skill applies only to certain file types or directories. |
| `model` | Model preference for skill execution. | Skill has a specific quality-vs-cost trade-off. |
| `effort` | Effort level (`low`, `medium`, `high`, `xhigh`, `max`). | Skill needs a specific reasoning budget. |
| `context` | Set to `fork` to run in a subagent-isolated context. | Skill reads many files and should not inflate the main context. |

Cite the field's use case in `SKILL.md` when it is present so a reader understands why the default was overridden.

## Dynamic context injection

Inside skill content you can inject the output of a shell command at load time:

    Current branch: !`git rev-parse --abbrev-ref HEAD`

The backtick-prefixed `!` runs the command in the same permission context as the skill. Use for:

- Injecting environment state that changes between sessions.
- Showing the current commit, branch, or config value.
- Rendering computed defaults into the skill's instructions.

Do not use for:

- Long-running commands (they block skill load).
- Secrets or credentials (they end up in the model's context).
- Commands with side effects (loading a skill should be safe).

## Argument substitution

Skills invoked as slash commands can consume arguments:

- `$ARGUMENTS` — the entire argument string as typed.
- `$0`, `$1`, `$2` — positional arguments (space-separated).
- `${CLAUDE_SKILL_DIR}` — absolute path of the current skill's directory.
- `${CLAUDE_PROJECT_DIR}` — absolute path of the project root.

Design the skill's instructions to fail loudly when a required argument is missing. Do not silently continue with an empty argument.

## Path-scoped skills

The `paths` frontmatter field limits when the skill is considered for triggering:

    paths:
      - "src/api/**/*.ts"
      - "src/api/**/*.tsx"

Use path scoping when the same repo has multiple layers with different conventions (for example an `api/` layer with input validation rules that do not apply to the `ui/` layer).

Do not confuse path-scoped skills with path-scoped rules — rules apply automatically when files match; skills still need to be triggered.

## Fork context (subagent execution)

Setting `context: fork` in the frontmatter makes the skill run in a fresh subagent context. The main conversation receives only the skill's final response.

Use when:

- The skill reads many files during its work.
- The skill produces a short answer that the main conversation needs.
- The skill's intermediate reasoning would clutter the main context.

Do not use when:

- The skill must remember state across turns of the main conversation.
- The user needs to see the intermediate reasoning.

## Nested skills

A skill can live inside a subdirectory of `.claude/skills/`. The name is qualified by the path segments:

    .claude/skills/database/reviewing-migrations/SKILL.md
    -> invocable as /database/reviewing-migrations

Use nesting to group related skills under a shared namespace. Prefer flat organization until the number of skills makes a namespace useful.

## Skills inside plugins

A plugin can ship skills under `<plugin-root>/skills/<skill-name>/SKILL.md`. Invocation is namespaced by plugin name:

    /my-plugin:my-skill

Notes:

- The name rules (lowercase, hyphens, no reserved words) still apply to the skill name.
- The plugin's own name adds another namespace prefix; keep both short.
- Reference files inside a plugin skill still resolve relative to the skill directory, not the plugin root.

## Legacy compatibility

Skills authored before the v2 section policy pass validation only if they opt in with:

    <!-- section-policy: legacy -->

placed anywhere in `SKILL.md`. The marker tells `validate_skill.py` to enforce the old rule (all 10 sections required) instead of the current rule (justifications per included and omitted recommended section).

New skills should not use the legacy marker. Migrate a legacy skill by:

1. Removing the marker.
2. Adding `<!-- inclusion-reason: ... -->` inside each recommended section that is present.
3. Adding an `<!-- omission-reason: ... -->` comment mentioning any recommended section that is absent.
4. Re-running `python scripts/validate_skill.py <skill-directory>`.
