---
name: artifact-report-contract
description: Defines the two output contracts used by delivery agents - the chat response contract and the persisted artifact contract - and guarantees field compatibility between the agent that writes an artifact and the agent that reads it later. Use when a planner, backend, reviewer, QA, or documentation agent produces a plan, status, review, QA result, code documentation, backlog, or technical debt artifact.
---

# Artifact Report Contract

<!-- section-policy: legacy -->
<!-- Migration note: opts into the legacy 10-section rule so validate_skill.py keeps passing without adding per-section inclusion-reason comments. Migrate to v2 by removing this marker and following the procedure in .claude/skills/creating-skills/ADVANCED-PATTERNS.md > Legacy compatibility. -->

## Objective

Keep agent outputs stable and machine-readable across handoffs by separating the ephemeral chat response from the persisted artifact, and by guaranteeing that every field one agent reads is a field another agent actually writes.

## When to use

Use this skill when:

- A planner, backend, reviewer, QA, or documentation agent is about to produce its final output.
- Writing or updating a plan, status, review, QA result, code documentation, sprint backlog, or technical debt register.
- Deciding where an artifact is saved and how its file is named.
- Deciding which sections belong in the chat answer versus in the file on disk.
- Reading an artifact produced by another agent and needing to know which sections are guaranteed present.

## When not to use

Do not use this skill for:

- Deciding whether a quality gate applies — use `workflow-gates-core`.
- Choosing a folder for an artifact type this skill does not list — placement is owned here, so an unlisted type is a gap to report as `PENDING_DEFINITION`, not a location to improvise.
- Producing the domain content itself (code, review findings, test strategy).

## Minimum required data

1. Agent role producing the output (planner, backend, reviewer, QA, documentation).
2. Sprint folder (`<NN>-<slug>`), `TASK-ID`, and the task slug already used in the plan file name.
3. Whether an artifact must be persisted or only a chat response is expected.

## If information is missing

- Missing sprint or `TASK-ID`: stop and ask; never write an artifact with a placeholder identifier.
- Missing evidence for a claim: keep the claim and label it `unknown`; do not delete it and do not present it as confirmed.
- Record blocking gaps as `PENDING_DEFINITION` inside the artifact rather than omitting the section.

## Mandatory process

1. Identify the role and the artifact type.
2. Emit the **response contract** for that role in chat — see [references/response-contract.md](references/response-contract.md).
3. Copy the matching template from `templates/` and fill it verbatim, keeping every section heading unchanged.
4. Label every factual statement as `confirmed`, `inferred`, or `unknown` in the sections that require it.
5. Verify the **cross-agent field compatibility table** in [references/artifact-contract.md](references/artifact-contract.md) — confirm that every section another agent will read is present and named exactly as expected.
6. Save the artifact at the canonical path defined in [references/artifact-contract.md](references/artifact-contract.md) `## Artifact placement and naming`; if the artifact type has no row there, write `PENDING_DEFINITION` and stop instead of choosing a folder.
7. Report the artifact path in the chat response.

## Mandatory rules

- The response contract and the artifact contract are different. Never write chat-only sections (`Execution Receipt`, `Handoff Status`, `Analysis Summary`) into the persisted artifact, and never omit artifact-only sections (`Next Step`, `Open Issues`, `Gate Outcomes`) from the file.
- Section headings come from the template verbatim. Renaming a heading breaks the agent that reads it.
- Chat responses and artifact content are written in Portuguese; section headings, field names, evidence labels, and verdict values stay verbatim in English as defined by the template.
- A status artifact must always contain `Open Issues` and `Next Step`, even when the value is `none`; the resume flow reads exactly these names.
- Backend and reviewer must fill `Technical Debt Identified`; the planner consolidates it into the sprint `pendencias-tecnicas.md`.
- `intervencoes.md` is created only when the first human intervention happens in the sprint; never create it empty.
- Artifacts are placed by sprint, at the canonical paths in [references/artifact-contract.md](references/artifact-contract.md) `## Artifact placement and naming`. Never write to a feature folder, and never invent a folder for an artifact type that has no row there.
- A task-scoped file name carries both the `TASK-ID` and the task slug (`<TASK-ID>-<task-slug>.md`), and the slug is copied verbatim from the plan file name rather than re-derived — see `### Slug rule` in the same reference.
- Every assumption, external contract field, and technical justification carries an explicit `confirmed | inferred | unknown` label.
- `Plan Deviations` is always present and states `none` when there is no deviation.
- Do not claim an execution result (tests, build, commands) without recording the command and its output in the artifact.
- A qualitative label (`high`/`low`, `strong`/`weak`, `ok`/`risk`, `approved`/`rejected`) must be derived from the quantitative value shown next to it and must never contradict it, including in summary tables indexed by those labels; the number is the authority.

## Additional resources

- Chat output structure per role: [references/response-contract.md](references/response-contract.md).
- Artifact placement and naming, structure, ownership, and cross-agent field compatibility: [references/artifact-contract.md](references/artifact-contract.md).
- Templates: `templates/plan.md`, `templates/status.md`, `templates/review.md`, `templates/qa.md`, `templates/code-documentation.md`, `templates/pendencias-tecnicas.md`, `templates/backlog.md`, `templates/intervencoes.md`.
- Gate semantics: `workflow-gates-core`.

## Stop criteria

- Stop if the sprint or `TASK-ID` is not confirmed.
- Stop if a required template section cannot be filled because the information does not exist; report the gap instead of inventing content.
- Do not persist an artifact whose section names diverge from the template.
- Do not persist an artifact whose destination is not a row of the placement table.

## Completion criteria

- Chat response follows the role's response contract.
- Artifact is saved at the canonical path for its type and sprint.
- Artifact follows its template with unchanged section headings.
- Every section listed as "read by" in the compatibility table is present.
- Evidence labels are applied where required.
- Artifact path is reported in the chat response.

## Output format

1. Response contract sections for the role, in order.
2. Artifact path written.
3. Evidence summary: counts of `confirmed`, `inferred`, and `unknown` items.
4. Compatibility check result: sections other agents depend on, confirmed present.
