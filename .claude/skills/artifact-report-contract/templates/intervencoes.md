# Human Interventions - Sprint <NN> <Sprint Title>

Created only when the first intervention of the sprint happens. Records corrections the human had to make over agent work, so the flow can be improved instead of repeating the same failure.

## Metadata
- sprint: `<NN>-<slug>`
- last_updated: `<YYYY-MM-DD>`

## Interventions

| # | Date | Task | Agent | What the agent got wrong | Correction applied | Probable cause | Candidate improvement |
|---|---|---|---|---|---|---|---|
| 1 | `<YYYY-MM-DD>` | `<TASK-ID>` | planner / backend / reviewer / qa | | | | |

Rules:

- One row per intervention. Do not merge two failures into one row.
- `Probable cause` must point at the missing rule or missing information, not at the agent.
- `Candidate improvement` must name the file that would prevent recurrence: a skill, an instruction, an agent, or a prompt.

## Minor Observations
Corrections that did not change an artifact location or a decision, but are worth tracking.

## Promoted to Decisions
Lessons that must survive this sprint, promoted to an ADR in `docs/decisions/`.

| Intervention | Decision document |
|---|---|
