# Response Contract (chat output)

## Table of contents

- Purpose
- Shared header
- Planner
- Backend
- Reviewer
- QA
- Code documentation

## Purpose

The response contract is what the agent prints in the conversation. It is ephemeral, addressed to the human, and never written into the persisted artifact.

Because it is addressed to the human, the text is written in Portuguese. The section labels below are structural and stay verbatim in English.

## Shared header

Every delivery agent starts its response with an `Execution Receipt`:

```
agent: <agent name>
prompt_path: <path or none>
mode: <execution mode>
sprint: <NN-slug or missing>
task_id: <TASK-ID or missing>
code_scope: <scope or not-applicable>
```

`Execution Receipt` exists only in chat. It never appears in a persisted artifact under `docs/`.

## Planner

1. Execution Receipt
2. Objective and Scope
3. Confirmed Facts / Assumptions / Unknowns
4. Task Breakdown
5. Acceptance Criteria
6. Implementation Lane
7. Architecture Fit
8. Quality Gates
9. Handoff Packet
10. Risks and Dependencies
11. Execution Order
12. Plan Artifact Path
13. Plan Deviations

## Backend

1. Execution Receipt
2. Task Understanding
3. Architecture Fit
4. Implemented Changes
5. Tests Executed (command and result)
6. Reviewer Handoff Status
7. QA Handoff Status (`required` | `not-applicable` | `approved` | `rejected`)
8. Technical Debt Identified
9. Status Artifact Path
10. Open Issues
11. Next Step
12. Plan Deviations

## Reviewer

1. Execution Receipt
2. Review Scope
3. Premise Checks
4. Architecture Conformance Check
5. What Was Validated (with evidence consulted)
6. Findings by severity
7. Required Fixes
8. Optional Improvements
9. Technical Debt Identified
10. Blocked Validations / Uncertainty
11. Review Artifact Path
12. Verdict (`approved` | `approved-with-notes` | `rejected`)

## QA

1. Execution Receipt
2. QA Scope and Risks
3. Architecture-Sensitive Risks
4. Flows Executed
5. Findings by Severity
6. Coverage Gaps
7. Required Fixes
8. QA Artifact Path
9. QA Verdict (`approved` | `approved-with-notes` | `rejected` | `not-applicable`)

## Code documentation

1. Execution Receipt
2. Analysis Summary
3. Generated/Updated Artifacts
4. Confirmed Links
5. Inferred Links
6. Unknowns / Missing Evidence
7. Confidence and Assumptions
8. Open Questions
9. Documentation Artifact Path
10. Plan Deviations
