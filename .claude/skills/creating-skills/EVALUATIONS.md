# Skill evaluations

## Objective

Evaluations verify whether the Skill solves real problems better, more consistently, and more safely than an approach without the Skill.

Create evaluations before writing extensive documentation.

## Minimum quantity

Create at least three scenarios:

1. Main expected flow.
2. Case with incomplete, ambiguous, or conflicting information.
3. Edge case, error, or situation that should not trigger the Skill.

When applicable, add:

- High-risk case.
- Case with large files or multiple references.
- Case that requires a script.
- Case that requires returning to the validation cycle.

## Evaluation structure

Use the template at [templates/evaluation.template.json](templates/evaluation.template.json).

Each evaluation must contain:

- Scenario name.
- Request.
- Input files.
- Expected behaviors.
- Forbidden behaviors.
- Success criteria.
- Observed result.
- Required improvements.

## Evaluation process

1. Run the scenario without the Skill.
2. Record the baseline.
3. Run the same scenario with the Skill.
4. Compare result, safety, consistency, and time.
5. Fix observed gaps.
6. Run again.
7. Record the version of the Skill evaluated.

## Observation criteria

Evaluate:

- Discovery: was the Skill triggered at the right moment?
- Scope: did the Skill avoid tasks outside its domain?
- Context: did the agent consult only the relevant files?
- Process: were critical steps followed?
- Validation: were failures detected before completion?
- Output: was the format respected?
- Security: were assumptions and risky actions handled correctly?
- Efficiency: did the Skill bring in unnecessary context?

## Testing per model

Test on all models planned for use.

Verify especially:

- Economy models: do they have sufficient guidance?
- Balanced models: do they follow the flow without excessive context?
- Advanced models: do they avoid skipping rules out of overconfidence?

Do not assume that behavior observed in one model will be the same in another.

## Iteration based on real usage

After publishing the Skill:

1. Observe real tasks.
2. Record failures and unexpected decisions.
3. Identify the cause: description, structure, rule, reference, or script.
4. Make a minimal change.
5. Re-run related evaluations.
6. Keep the change only if there is an observable improvement.
