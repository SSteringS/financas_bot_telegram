---
name: workflow-gates-core
description: Defines the delivery gates and handoff sequence shared by planner, backend, reviewer, QA, and documentation agents, including the information gate that blocks execution when required data is missing. Use when deciding whether review or QA applies, when delegating between agents, or when a required input is unavailable.
---

# Workflow Gates Core

<!-- section-policy: legacy -->
<!-- Migration note: opts into the legacy 10-section rule so validate_skill.py keeps passing without adding per-section inclusion-reason comments. Migrate to v2 by removing this marker and following the procedure in .claude/skills/creating-skills/ADVANCED-PATTERNS.md > Legacy compatibility. -->

## Objective

Apply the shared delivery gates consistently across agents, so no code change escapes review, QA is decided explicitly rather than guessed, and missing information stops execution instead of being filled with assumptions.

## When to use

Use this skill when:

- Setting or reading `review_required`, `qa_required`, or `qa_rationale`.
- Delegating work from one agent to another.
- Deciding whether a task is complete.
- A required input, contract field, or technical fact is not available.

## When not to use

Do not use this skill for:

- Formatting outputs or artifacts — use `artifact-report-contract`.
- Deciding file placement or naming — that is in `.github/instructions/artifact-placement-and-naming.instructions.md`.
- Judging code quality — that belongs to the reviewer role and `reviewing-code-premises`.

## Minimum required data

1. Task type: `implementation`, `fix`, or `documentation/process-only`.
2. Whether the task changes code.
3. The plan's gate values when the agent is not the planner.

## If information is missing

- The information gate applies: stop and ask. Do not fill a gap with an assumption, a placeholder, or a defensive fallback.
- If the plan does not state `qa_required`, do not guess it; ask the planner or the user.
- Record unresolved gaps as `unknown` with `blocking: yes` in the artifact.

## Mandatory process

1. Classify the task type.
2. Apply the information gate before any execution: list what is required, mark each item `confirmed`, `inferred`, or `unknown`, and stop if any `unknown` is blocking.
3. Set or read the gates:
   - `review_required`: always `true` when the task changes code.
   - `qa_required`: decided explicitly during planning; never inferred by the implementer.
   - `qa_rationale`: required whenever `qa_required=true`.
4. Follow the canonical handoff sequence.
5. Record every gate outcome in the artifact.
6. Report technical debt found during execution so the planner can consolidate it.

## Canonical handoff sequence

1. Planner produces the plan, acceptance criteria, and gates.
2. Implementer executes within the plan scope.
3. Implementer invokes the reviewer for independent validation.
4. Implementer applies findings and requests a new review when needed.
5. QA validates required flows when `qa_required=true`.
6. Human approves merge, release, and durable decisions.

For `documentation/process-only` tasks the sequence may be shortened, but the agent must state explicitly why review and QA are not applicable.

## Mandatory rules

- Never mark a task complete before the reviewer handoff has an outcome.
- Never skip QA when `qa_required=true`.
- When `qa_required=false`, record QA as `not-applicable` with the rationale taken from the plan.
- Never claim a test or build passed without the command and its output.
- Never delegate implementation without a complete handoff packet.
- Never treat an assumption as a confirmed fact.
- A missing external contract field is a blocker, not a design problem to route around.
- Human approval remains mandatory for merge, release, architecture, and process decisions.

## Information gate

Execution stops when any of these is true:

- A required input is absent from the plan and cannot be verified in the repository.
- An external contract field name, type, or format is not documented in an authoritative source.
- A technical justification cannot be verified by execution, official documentation, or source code.
- The feature or `TASK-ID` is not confirmed.

When stopped, report: what is missing, where it was searched, and what is needed to unblock. Do not propose a fallback that masks the gap.

## Technical debt handling

- Backend reports debt found while implementing, in the status artifact.
- Reviewer reports debt found while reviewing, in the review artifact.
- Planner consolidates both into the feature technical debt register and decides whether each item becomes a task.
- Debt that is not reported by backend or reviewer never reaches the planner; reporting it is mandatory, not optional.

## Additional resources

- Output and artifact structure: `artifact-report-contract`.
- Premise verification during review: `reviewing-code-premises`.

## Stop criteria

- Stop when the information gate triggers.
- Stop when `qa_required` is undefined for a code-changing task.
- Stop when asked to mark a task complete without a reviewer outcome.

## Completion criteria

- Task type and gates are explicit.
- Information gate was applied and every required input is `confirmed` or non-blocking.
- Reviewer outcome is recorded.
- QA state is `approved`, `approved-with-notes`, `rejected`, or `not-applicable` with rationale.
- Technical debt found was reported.

## Output format

1. Task type.
2. Information gate result: `confirmed` / `inferred` / `unknown` counts and blocking status.
3. Gate values: `review_required`, `qa_required`, `qa_rationale`.
4. Handoff performed and its outcome.
5. Technical debt reported, or `none`.
