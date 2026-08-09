# Diagnostic questions

## Table of contents

- Purpose
- Question set
- How to record the answers
- What to do when the user cannot answer

## Purpose

Answer the questions below before short-listing any primitive. A recommendation produced without these answers will drift toward generic advice.

## Question set

Answer each question with a single line. If a question does not apply, write `n/a` and explain in one clause why.

### 1. Objective

- What is the scenario trying to accomplish, in one sentence?
- What signals success or failure after the workflow runs?

### 2. Frequency

- Is this a one-off task, a recurring task on a schedule, or a task triggered by an event?
- If recurring, what is the cadence (per commit, per PR, hourly, daily)?

### 3. Interaction mode

- Interactive session (developer at the terminal or IDE)?
- Headless or CI (non-interactive, must return structured output)?
- Scheduled routine (must run when the developer machine is offline)?

### 4. Trust level

- Local developer machine only?
- Shared with a team through the repo?
- Reaches production infrastructure or shared credentials?

### 5. Human-in-the-loop requirement

- Must a human approve before the action runs?
- Must a human review after the action runs?
- Fully autonomous acceptable?

### 6. Destructive potential

- Can the action delete data, force-push, deploy, revoke access, or mutate shared state?
- If yes, what is the blast radius (single file, repo, environment, tenant)?

### 7. Context size

- Roughly how many files must be read to make the decision or produce the output?
- Does the work risk pushing the main conversation context past a compaction boundary?

### 8. Existing artifacts

- Are there skills, subagents, hooks, MCP servers, or `CLAUDE.md` rules already covering part of this scenario?
- Does the existing set already contradict itself or duplicate responsibility?

## How to record the answers

Include the answers as bullets in section 2 of the output (`Diagnostic answers`). Keep each answer to one line so the downstream reasoning stays legible.

## What to do when the user cannot answer

- If the missing answer changes the recommendation (for example: trust level, destructive potential), stop and ask the user directly.
- If the missing answer is a nuance (for example: exact cadence), state the assumption in `Assumptions and pending items` and continue.
- Never guess trust level or destructive potential — those two answers gate the safety recommendations and must be confirmed.
