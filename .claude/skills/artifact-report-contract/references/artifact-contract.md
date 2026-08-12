# Artifact Contract (persisted files)

## Table of contents

- Purpose
- Artifact placement and naming
  - Slug rule
- Artifact ownership
- Cross-agent field compatibility
- Technical debt loop
- Evidence labels

## Purpose

The artifact contract is what gets written to disk under the sprint folder `docs/sprints/<NN>-<slug>/`, or under a global folder for the artifacts that outlive a sprint. It is persistent and is read later by other agents, so both the path and the section names are a hard interface.

## Artifact placement and naming

The organizing unit is the **sprint**, not the feature. A sprint folder is `docs/sprints/<NN>-<slug>/`, where `<NN>` is the zero-padded sprint number and `<slug>` its short name — for example `docs/sprints/04-instrumentacao-qualidade/`. There is no feature folder and no `F<NN>` prefix.

`<TASK-ID>` is the global sequential task identifier (`BE-017`, `QA-012`, `FE-014`); the sprint folder groups tasks, it does not renumber them.

| Artifact | Canonical path |
|---|---|
| Plan | `docs/sprints/<NN>-<slug>/plans/<TASK-ID>-<task-slug>.md` |
| Status | `docs/sprints/<NN>-<slug>/status/<TASK-ID>-<task-slug>.md` |
| Review | `docs/sprints/<NN>-<slug>/avaliacoes/review-<TASK-ID>-<task-slug>.md` |
| QA | `docs/sprints/<NN>-<slug>/avaliacoes/qa-<TASK-ID>-<task-slug>.md` |
| Code documentation | `docs/architecture/<topic>.md` |
| Human interventions | `docs/sprints/<NN>-<slug>/intervencoes.md` |
| Technical debt, sprint-scoped | `docs/sprints/<NN>-<slug>/pendencias-tecnicas.md` |
| Technical debt, global | `docs/PENDENCIAS-TECNICAS.md` |
| Backlog, sprint-scoped | `docs/sprints/<NN>-<slug>/backlog-s<NN>.md` |
| Backlog, global | `docs/plans/BACKLOG-produto.md` and `docs/plans/BACKLOG-evolucao-workflow.md` |

Placement rules:

- Review and QA artifacts share the `avaliacoes/` folder; the `review-` and `qa-` filename prefixes are what keep them apart.
- Every task-scoped artifact file name is `<TASK-ID>-<task-slug>.md`, optionally with the `review-` / `qa-` prefix. The `<TASK-ID>` alone is never a file name; see `### Slug rule`.
- The `review-` / `qa-` prefixes and the `-<task-slug>` suffix are **forward-only** conventions. Artifacts created before them keep their original names and are never renamed, so both `BE-17.md` and `QA-012-piloto-pit-mutation-testing.md` are valid names on disk; only new artifacts must follow the current form. The repository already applies forward-only conventions to branch and task identifiers.
- When you need to read an artifact written before the convention, resolve it by the `<TASK-ID>` prefix instead of assuming the full name.
- `intervencoes.md` is created only when the first human intervention of the sprint happens. Never create it empty.
- Technical debt bounded to the current sprint goes in the sprint register; debt that outlives the sprint goes in the global `docs/PENDENCIAS-TECNICAS.md`. When in doubt, record it in the sprint register and let the planner promote it.
- The sprint backlog holds the tasks of that sprint. Product evolution and workflow evolution go in the two global backlogs.
- Code documentation is global, not sprint-scoped: it describes the system as it is today. Files there are named by topic in kebab-case (`especificacao-tecnica.md`, `fluxo-autenticacao.md`), never by `TASK-ID`.
- Write every path with `/`, even on Windows.
- Never invent a folder. If an artifact type has no row in this table, write `PENDING_DEFINITION` for its path and report the gap instead of choosing a location.

### Slug rule

Two different slugs appear in the paths above and they are not interchangeable:

| Placeholder | Names | Fixed by | Example |
|---|---|---|---|
| `<slug>` in `<NN>-<slug>` | the sprint folder | the sprint name, set when the sprint opens; identical to the `integration/<NN>-<slug>` branch | `instrumentacao-qualidade` |
| `<task-slug>` in a file name | the task | the task title, set by the plan file | `piloto-pit-mutation-testing` |

Rules for `<task-slug>`:

- Derive it from the task title in kebab-case: lowercase, ASCII only, accents stripped (`instrumentação` becomes `instrumentacao`), every run of spaces or punctuation collapsed into a single `-`, no leading or trailing `-`.
- Keep it short: 3 to 6 words that identify the task. It is a handle, not the full title.
- Never repeat the `<TASK-ID>` inside the slug; the id is already the prefix.
- **One task, one slug.** The plan file name fixes it. Status, review, and QA copy that exact string from the plan file name; they never re-derive it from the title, because a re-derivation that differs by one word produces an artifact no other agent can find.
- If the task has no plan artifact, the first artifact written fixes the slug and every later artifact copies it.
- If the plan file name predates this convention and carries no slug, keep the task on the old form for its remaining artifacts rather than mixing two names for the same task.

## Artifact ownership

| Artifact | Template | Written by | Read by |
|---|---|---|---|
| Plan | `templates/plan.md` | planner | backend, reviewer, QA |
| Status | `templates/status.md` | backend | reviewer, QA, planner |
| Review | `templates/review.md` | reviewer | backend, planner |
| QA | `templates/qa.md` | QA | backend, planner |
| Code documentation | `templates/code-documentation.md` | documentation analyst | all |
| Technical debt register | `templates/pendencias-tecnicas.md` | planner | planner, backend, reviewer |
| Sprint backlog | `templates/backlog.md` | planner | planner, backend |
| Human interventions | `templates/intervencoes.md` | planner, from a human correction | planner, all agents in the next sprint |

## Cross-agent field compatibility

These section names are a contract. Renaming any of them breaks the consumer.

| Section | Lives in | Written by | Read by | Consequence if missing or renamed |
|---|---|---|---|---|
| `Next Step` | status | backend | planner (resume flow) | resume cannot report the next action |
| `Open Issues` | status | backend | planner (resume flow), reviewer | open blockers disappear from the briefing |
| `Gate Outcomes` | status | backend | reviewer, QA, planner | gate state becomes unverifiable |
| `Acceptance Criteria` | plan | planner | backend, reviewer, QA | review has nothing objective to validate against |
| `Quality Gates` | plan | planner | backend, reviewer, QA | backend cannot decide QA handoff |
| `Handoff Packet` | plan | planner | backend | implementation starts with ambiguity |
| `Verdict` | review | reviewer | backend, planner | completion cannot be determined |
| `Blocked Validations / Uncertainty` | review | reviewer | planner, backend | unverified claims get treated as approved |
| `Technical Debt Identified` | status, review | backend, reviewer | planner | debt never reaches `pendencias-tecnicas.md` |
| `Plan Deviations` | plan, status, review | all | planner | scope drift becomes invisible |
| `Metadata` > `sprint` | every artifact with a `Metadata` block | the writing agent | every agent resolving the sprint folder | the artifact cannot be traced back to its sprint |

Before saving, confirm every row whose artifact you are writing.

The `Metadata` field is `sprint: <NN>-<slug>`. It was named `feature: F<NN>-<slug>` in templates imported from another project; that name is retired and must not be reintroduced.

## Technical debt loop

Technical debt is detected during execution and consolidated during planning:

1. Backend records debt found while implementing in `Technical Debt Identified` of the status artifact.
2. Reviewer records debt found while reviewing in `Technical Debt Identified` of the review artifact.
3. Planner reads both, consolidates into the sprint technical debt register, and decides whether each item becomes a task or is promoted to the global register.

The planner owns the register but is not the detector. Debt not reported by backend or reviewer never reaches the register.

## Evidence labels

Every factual statement in the sections below carries one label:

| Label | Meaning |
|---|---|
| `confirmed` | verified directly in code, executed output, or an authoritative source; cite the source |
| `inferred` | plausible but not verified; state the reasoning |
| `unknown` | required and not available; state whether it blocks |

Required in:

- plan: `Confirmed Facts / Assumptions / Unknowns`, `Verified Technical Claims`
- status: `External Contract Sources`
- review: `Premise Checks`
- code documentation: `Confirmed Links`, `Inferred Links`, `Unknowns / Missing Evidence`

An `unknown` marked as blocking is a stop condition, not a note.
