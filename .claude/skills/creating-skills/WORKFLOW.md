# Skill creation workflow

## Table of contents

- 1. Start with real evaluations
- 2. Identify the gap
- 3. Define single responsibility
- 4. Define discovery
- 5. Define boundaries
- 5.1 Decide sections and record justifications
- 6. Define the degree of freedom
- 7. Organize the context
- 8. Create feedback loops
- 9. Create useful scripts
- 10. Test and iterate

## 1. Start with real evaluations

Before creating extensive documentation, define at least three scenarios that represent real usage.

Each scenario must describe:

- User request.
- Input files or data.
- Expected behavior.
- Rules that cannot be violated.
- Observable success criteria.

Use [EVALUATIONS.md](EVALUATIONS.md) as a reference.

## 2. Identify the gap

Run representative tasks without the Skill and record:

- Information repeated by the user.
- Rules the agent forgets.
- Recurring errors.
- Inconsistent decisions.
- Inadequate output formats.
- Ignored critical steps.

Create the Skill only to resolve observed gaps or proven requirements.

## 3. Define single responsibility

Describe the purpose using this template:

> This Skill exists to [main outcome] in [domain], when [trigger].

Example:

> This Skill exists to review PostgreSQL migrations in Java applications when there is creation, alteration, validation, or analysis of migration scripts.

If the sentence lists independent capabilities, split the Skill.

## 4. Define discovery

Create a specific, consistent, and easy-to-locate name.

Preferred:

    reviewing-database-migrations
    creating-api-contracts
    analyzing-sales-data

Avoid:

    helper
    tools
    files
    database-utils

Write the description in the third person, including:

1. What the Skill does.
2. Relevant artifacts or technologies.
3. User intentions.
4. Contexts in which it should be triggered.

## 5. Define boundaries

Explicitly write:

- When to use.
- When not to use.
- Minimum data.
- Action when data is missing.
- Stop criteria.
- Completion criteria.

Do not rely on implicit boundaries.

## 5.1 Decide sections and record justifications

Section policy v2 makes every recommended section optional but requires justification for each choice. Before writing the body:

- List the recommended sections (see [DESIGN-RULES.md](DESIGN-RULES.md) `## Section policy`).
- For each section the skill will include, draft a one-clause `inclusion-reason` explaining why this skill needs it.
- For each section the skill will omit, draft a one-clause `omission-reason` explaining why it does not apply.
- Add both as `<!-- inclusion-reason: ... -->` and `<!-- omission-reason: ... -->` HTML comments in the appropriate places.

The validator will fail the skill if any recommended section is present without an inclusion-reason, or absent without an omission-reason mentioning it.

## 6. Define the degree of freedom

Choose the level of specificity based on risk and variability.

| Degree | When to use | Recommended form |
|---|---|---|
| High | There are several valid solutions | Principles and general steps |
| Medium | There is a preferred pattern with acceptable variation | Template, pseudocode, and parameters |
| Low | The task is fragile, critical, or sequential | Exact commands, scripts, and mandatory validation |

Example of low degree of freedom:

Run exactly:

    python scripts/validate_migration.py migration.sql

Do not proceed while the validation fails.

## 7. Organize the context

Keep in `SKILL.md`:

- Objective.
- Triggers.
- Boundaries.
- Essential flow.
- Critical rules.
- Direct links to additional content.

Move to separate files:

- Extensive references.
- Schemas.
- Catalogs.
- Advanced cases.
- Complete examples.
- Domain guides.

Keep all important references one level deep from `SKILL.md`.

Good:

    SKILL.md → references/errors.md
    SKILL.md → references/pagination.md
    SKILL.md → WORKFLOW.md

Avoid:

    SKILL.md → advanced.md → details.md → actual-rules.md

## 8. Create feedback loops

For critical operations, use:

    plan → validate plan → execute → verify result

For reviews without automation:

    produce → check checklist → fix → check again → complete

For automations:

    generate artifact → run validator → fix failures → run validator again

## 9. Create useful scripts

Scripts should solve predictable problems, not just fail.

A good script:

- Explains the error.
- Indicates the file, field, or rule violated.
- Suggests a fix when possible.
- Produces short output.
- Does not depend on dynamic installation without explicit instruction.
- Does not use magic numbers without justification.

## 10. Test and iterate

Test in real scenarios and, when applicable, on all models that will be used.

Verify:

- Was the Skill triggered when it should have been?
- Was it ignored when it should not be used?
- Did the agent find the correct files?
- Did the agent follow the references?
- Did the agent ignore critical rules?
- Is any file never read?
- Is any file read in almost every task and should be in `SKILL.md`?

Adjust the Skill based on observed behavior, not just hypotheses.
