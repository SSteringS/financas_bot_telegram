---
name: creating-skills
description: Creates, reviews, and evolves file-based custom Skills. Use when defining SKILL.md, structuring instructions, references, templates, scripts, evaluations, context rules, or validations for a Skill.
---

# Creating Skills

## Objective

<!-- inclusion-reason: A skill that creates other skills must anchor its own outcome before anything else. -->

Create concise, specialized, testable, and predictable custom Skills that follow the official spec in `docs/claude/skills/01-skills.md`.

## When to use

<!-- inclusion-reason: Discovery trigger for the skill; must be explicit because other skills depend on this being reliably matched. -->

Use this Skill when:

- Creating a new Skill.
- Reviewing an existing Skill.
- Improving discovery, scope, or precision of a Skill.
- Creating `SKILL.md`, reference files, templates, or scripts.
- Reducing context usage.
- Creating evaluations and validation criteria.
- Defining workflows, rules, and output formats.

## When not to use

<!-- inclusion-reason: Prevents scope creep into agent creation, execution of the domain task, or third-party audit. -->

Do not use this Skill for:

- Executing the domain task of the Skill being created.
- Installing, downloading, or auditing third-party Skills.
- Creating a Skill without an identifiable objective, domain, or outcome.
- Replacing specialized security, architecture, or compliance review.
- Executing destructive operations without explicit confirmation.
- Creating a subagent (`.md` in `.claude/agents/`) — use `creating-agents` instead.

## Minimum required data

<!-- inclusion-reason: Skills produced without minimum data become generic and untestable. -->

Identify, before creating the Skill:

1. Main objective.
2. Domain and artifacts involved.
3. Requests that should trigger it.
4. Requests that should not trigger it.
5. Minimum inputs.
6. Expected output.
7. Mandatory rules.
8. Execution environment and dependencies.
9. Risk level and validation needs.
10. Real examples or evaluation scenarios.

## If information is missing

<!-- inclusion-reason: Prevents authoring a skill on invented premises when a critical input is missing. -->

- Do not invent business rules, integrations, permissions, or data sources.
- Ask questions only when the absence prevents safely defining the scope.
- Proceed with assumptions only when they are not critical.
- State every assumption under the `Assumptions` section.
- Record indispensable missing information as `PENDING_DEFINITION`.

## Mandatory process

<!-- inclusion-reason: The ordering (evaluations first, docs later) is load-bearing and must be prescribed, not left to interpretation. -->

1. Create representative evaluations before writing extensive documentation.
2. Define objective, single responsibility, and boundaries of the Skill.
3. Write name and description for discovery.
4. Define inputs, outputs, stop criteria, and completion criteria.
5. Decide which recommended sections apply and draft the `inclusion-reason` or `omission-reason` for each; see [DESIGN-RULES.md](DESIGN-RULES.md) `## Section policy`.
6. Choose the appropriate degree of freedom for each step.
7. Create a concise `SKILL.md`, with a maximum of 500 lines.
8. Separate details by topic into files directly referenced by `SKILL.md`.
9. Create scripts for deterministic operations and critical validations.
10. Add a validation cycle for tasks that require reliability.
11. Run `python scripts/validate_skill.py <skill-directory>`.
12. Fix failures and validate again.
13. Deliver the structure, complete files, evaluations, and pending items.

See [WORKFLOW.md](WORKFLOW.md) for the detailed process.
See [DESIGN-RULES.md](DESIGN-RULES.md) for architecture rules and the section policy.
See [ADVANCED-PATTERNS.md](ADVANCED-PATTERNS.md) for optional frontmatter fields, dynamic context, argument substitution, path scoping, forked context, nested skills, and plugin skills.
See [EVALUATIONS.md](EVALUATIONS.md) to create and run evaluations.
See [OUTPUT-STANDARD.md](OUTPUT-STANDARD.md) for the delivery standard.

## Mandatory rules

<!-- inclusion-reason: Hard invariants that must never be violated by any authored skill; centralizing them here prevents them from being lost in longer references. -->

- Each Skill must have a single main responsibility.
- The `name` must be at most 64 characters.
- The `name` must use only lowercase letters, numbers, and hyphens.
- The `name` must not contain `anthropic` or `claude`.
- Prefer consistent gerund-form names, such as `reviewing-code`.
- The `description` must be written in the third person.
- The `description` must state what the Skill does and when it should be used.
- The `description` must not be empty, contain XML tags, or exceed 1024 characters.
- The body of `SKILL.md` must remain below 500 lines.
- Reference files must be one level deep from `SKILL.md`.
- Markdown references longer than 100 lines must contain a table of contents.
- Use consistent terminology across all files.
- Use `/` paths, even on Windows environments.
- Define a primary pattern; do not present too many alternatives unnecessarily.
- Do not include time-sensitive information that will become outdated.
- Declare dependencies and confirm their availability in the execution environment.
- Do not declare a task complete if a mandatory validation fails.
- Every recommended section that is present in `SKILL.md` must carry an `<!-- inclusion-reason: ... -->` comment.
- Every recommended section that is absent from `SKILL.md` must be justified by an `<!-- omission-reason: ... -->` comment mentioning the section title.

## Script usage

<!-- section-policy-note: `## Script usage` is intentionally not in the recommended set. It stays here because scripts are central to this specific skill; another skill without scripts would omit this section without needing an omission-reason. -->

Use scripts for deterministic, repetitive, fragile, or critical operations.

When referencing a script, explicitly state the intent:

- Execution: `Run python scripts/validate_skill.py .`
- Reference: `Read scripts/validate_skill.py to understand the checked rules.`

Scripts must:

- Return exit code `0` on success.
- Return a non-`0` exit code on failure.
- Display specific and actionable errors.
- Handle predictable failures.
- Avoid destructive actions by default.
- Document non-obvious constants and parameters.

## Stop criteria

<!-- inclusion-reason: Prevents shipping a skill whose scope is not yet definable. -->

- Stop and ask for clarification if the main objective cannot be defined.
- Stop if there are conflicting requirements that change the outcome.
- Do not create destructive automation without explicit confirmation.
- Do not proceed to execution if the plan or intermediate validation fails.
- Do not declare readiness if there are critical pending items.

## Completion criteria

<!-- inclusion-reason: Objective definition of "done" so the skill is not shipped prematurely. -->

A Skill is ready when:

- It has explicit scope and exclusions.
- It has a specific, third-person, discovery-oriented description.
- It has a concise `SKILL.md` below 500 lines.
- It uses progressive context disclosure.
- It has objective rules and output format.
- It has stop criteria and gap handling.
- It has representative evaluations.
- It has validations for critical operations.
- It passes the structural validator.
- It documents assumptions, limitations, and pending items.
- Every recommended section present carries an `inclusion-reason`; every recommended section absent carries an `omission-reason`.

## Output format

<!-- inclusion-reason: The consumer (usually an agent orchestrating skill creation) needs a predictable delivery contract. -->

1. Skill summary.
2. Architecture decisions (including section policy choices and the reasons per included and omitted recommended section).
3. Directory structure.
4. Complete file contents.
5. Proposed or updated evaluations.
6. Validation commands and results.
7. Assumptions, limitations, and pending items.

<!-- omission-reason: `## Additional resources` is intentionally omitted as a separate top-level section. The links to WORKFLOW.md, DESIGN-RULES.md, ADVANCED-PATTERNS.md, EVALUATIONS.md and OUTPUT-STANDARD.md are inlined at the end of `## Mandatory process` (step 5 and the block that follows step 13). Duplicating them under a separate heading would fragment discovery — the reader is already at the point where they need the links. -->
