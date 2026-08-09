# Design rules for Skills

## Table of contents

- Conciseness
- Discovery
- Section policy
- Frontmatter fields
- Progressive context
- Terminology
- References
- Templates and examples
- Dependencies
- Tools and MCP
- Security
- Maintenance

## Conciseness

Assume the agent already has general knowledge.

Include only:

- Domain-specific rules.
- Internal conventions.
- Mandatory processes.
- Real technical constraints.
- Output formats.
- Decisions that cannot be safely inferred.

Remove basic explanations that do not affect execution.

## Discovery

The description must be specific, written in the third person, and include terms that appear in real requests.

Good:

    description: Reviews SQL migrations for PostgreSQL. Use when creating, altering, validating, or analyzing migration files, schemas, indexes, constraints, rollback, or database impact.

Bad:

    description: Helps with databases.

## Section policy

The official Skills spec (`docs/claude/skills/01-skills.md`) only requires a valid frontmatter with `name` and `description`. No body heading is mandatory. This repository enforces a stricter but auditable convention on top of the spec.

Two modes exist:

- **v2 (default)**: no section heading is required, but every heading listed in the recommended set below must carry an explicit justification.
  - If a recommended section is **present** in `SKILL.md`, place an HTML comment inside it:

        <!-- inclusion-reason: <one clause explaining why this skill needs this section> -->

  - If a recommended section is **absent** from `SKILL.md`, place an HTML comment anywhere in the file that mentions the section title:

        <!-- omission-reason: `## Additional resources` is intentionally omitted because ... -->

- **legacy**: opt-in via `<!-- section-policy: legacy -->` anywhere in `SKILL.md`. Enforces the older rule that 10 fixed sections must be present. Reserved for skills authored before v2 existed; new skills must not use it.

Recommended sections (both modes reference this list):

    Objective
    When to use
    When not to use
    Minimum required data
    If information is missing
    Mandatory process
    Mandatory rules
    Additional resources
    Stop criteria
    Completion criteria
    Output format

The validator (`scripts/validate_skill.py`) enforces this policy automatically.

## Frontmatter fields

Only two fields are required by the spec: `name` and `description`. The other supported fields are optional; include only what the skill actually needs. See [ADVANCED-PATTERNS.md](ADVANCED-PATTERNS.md) for the full list (`allowed-tools`, `disallowed-tools`, `disable-model-invocation`, `user-invocable`, `paths`, `model`, `effort`, `context: fork`) with usage guidance.

When you include an optional field, state its reason in `SKILL.md`. When you omit a field that would be expected for the skill's kind (for example a security-sensitive skill without `disallowed-tools`), state the reason too.

## Progressive context

Use this organization:

| Content | Location |
|---|---|
| Triggers, critical rules, and short flow | `SKILL.md` |
| Detailed processes | `WORKFLOW.md` |
| Factual knowledge by topic | `references/` |
| Examples | `examples/` |
| Standardized outputs | `templates/` |
| Repetitive operations | `scripts/` |
| Test scenarios | `evaluations/` or `EVALUATIONS.md` |

Do not create empty directories or files without a purpose.

## Terminology

Choose one term per concept and keep it consistent across all files.

Example:

| Preferred | Avoid alternating with |
|---|---|
| API endpoint | route, URL, path |
| field | attribute, box, control |
| validation | check, verification, inspection |
| migration | database script, SQL change |

## References

- Use descriptive names: `error-response-format.md`, not `doc2.md`.
- Use `/` paths.
- Reference files directly from `SKILL.md`.
- Include a table of contents in Markdown files longer than 100 lines.
- Do not hide critical rules in low-visibility references.
- If a file is needed in almost every task, move the essentials into `SKILL.md`.

## Templates and examples

Use templates when the output needs to be consistent.

Use input/output examples when style, tone, or structure are hard to describe.

For rigid requirements, use explicit language:

    Use exactly this structure.

For adaptable requirements, use:

    Use this structure as the default and adjust only when the context requires it.

## Dependencies

The Skill must state:

- Expected language and version, when relevant.
- Required packages.
- Origin of dependencies.
- Whether dependencies are available in the environment.
- Network, installation, and permission limitations.

Do not assume packages or tools are installed.

## Tools and MCP

When a Skill uses MCP tools, use the fully qualified name:

    Server:tool_name

Example:

    Use `GitHub:create_issue` to open an issue.
    Use `BigQuery:bigquery_schema` to query schemas.

## Security

Do not:

- Expose secrets, tokens, or personal data.
- Make unnecessary external requests.
- Run destructive commands by default.
- Install dependencies globally without authorization.
- Declare validation successful without running it.

For risky operations, use a validatable plan before execution.

## Maintenance

Avoid content dependent on dates.

Prefer:

    ## Current method

    Use the currently supported version according to the project's official reference.

    ## Legacy patterns

    Use only when the project already depends on them.

Update the Skill after observing real failures, environment changes, or new recurring patterns.
