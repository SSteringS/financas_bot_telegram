# Delivery standard

## 1. Summary

Provide:

- Skill name.
- Objective in one sentence.
- Scope.
- Main triggers.
- Main exclusions.

## 2. Architecture decisions

Objectively explain:

- Files created.
- Reason for each file.
- Content kept in `SKILL.md`.
- Content moved to references.
- Degree of freedom adopted in critical steps.
- Scripts and validations included.

## 3. Directory structure

Present the complete tree.

    skill-name/
    ├── SKILL.md
    └── ...

## 4. Complete files

Provide the complete content of each new or changed file.

Do not omit parts necessary for use, validation, or execution.

## 5. Evaluations

Include:

- Evaluation scenarios.
- Expected behavior.
- Forbidden behavior.
- Result of validations run.
- Pending improvements.

## 6. Validation

Provide:

    python scripts/validate_skill.py <skill-directory>

Record:

- Result.
- Errors found.
- Corrections applied.
- Items requiring manual validation.

## 7. Assumptions and pending items

List separately:

- Assumptions adopted.
- External dependencies.
- Environment limitations.
- Missing information.
- Next steps.
