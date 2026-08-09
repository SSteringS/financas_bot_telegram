# Artifact Contract (persisted files)

## Table of contents

- Purpose
- Artifact ownership
- Cross-agent field compatibility
- Technical debt loop
- Evidence labels

## Purpose

The artifact contract is what gets written to disk under `ia-docs/features/<FEATURE-FOLDER>/`. It is persistent and is read later by other agents, so section names are a hard interface.

## Artifact ownership

| Artifact | Template | Written by | Read by |
|---|---|---|---|
| Plan | `templates/plan.md` | planner | backend, reviewer, QA |
| Status | `templates/status.md` | backend | reviewer, QA, planner |
| Review | `templates/review.md` | reviewer | backend, planner |
| QA | `templates/qa.md` | QA | backend, planner |
| Code documentation | `templates/code-documentation.md` | documentation analyst | all |
| Technical debt register | `templates/pendencias-tecnicas.md` | planner | planner, backend, reviewer |
| Feature backlog | `templates/backlog.md` | planner | planner, backend |
| Human interventions | `templates/intervencoes.md` | planner, from a human correction | planner, all agents in the next feature |

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

Before saving, confirm every row whose artifact you are writing.

## Technical debt loop

Technical debt is detected during execution and consolidated during planning:

1. Backend records debt found while implementing in `Technical Debt Identified` of the status artifact.
2. Reviewer records debt found while reviewing in `Technical Debt Identified` of the review artifact.
3. Planner reads both, consolidates into `pendencias-tecnicas.md`, and decides whether each item becomes a task.

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
