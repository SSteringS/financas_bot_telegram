---
name: reviewing-code-premises
description: Validates the premises behind a code change before judging the code itself, covering architecture conformance, veracity of technical justifications, origin of external contract fields, and whether tests validate an assumption against itself. Use when reviewing an implementation, auditing a technical claim, or checking that field names and formats came from an authoritative source.
---

# Reviewing Code Premises

<!-- section-policy: legacy -->
<!-- Migration note: opts into the legacy 10-section rule so validate_skill.py keeps passing without adding per-section inclusion-reason comments. Migrate to v2 by removing this marker and following the procedure in .claude/skills/creating-skills/ADVANCED-PATTERNS.md > Legacy compatibility. -->

## Objective

Catch the failure class where the code is internally consistent but built on a wrong premise: a violated architecture, a false technical justification, an invented external contract, or a test that confirms the same assumption as the code it tests.

## When to use

Use this skill when:

- Reviewing an implementation before judging style, naming, or structure.
- A change consumes or produces fields of an external system.
- A design decision is justified by a technical claim about behavior.
- Tests pass but the correctness of the expected values has not been established.

## When not to use

Do not use this skill for:

- Formatting the review artifact — use `artifact-report-contract`.
- Deciding whether review or QA applies — use `workflow-gates-core`.
- Writing the fix; this skill validates premises, it does not implement.

## Minimum required data

1. The plan and its declared architecture style.
2. The diff or file scope under review.
3. The status artifact, including `External Contract Sources`.

## If information is missing

- If the declared architecture style is unknown, ask; do not assume MVC.
- If the source of an external contract field is unknown, mark the check `fail`, not `not-checked`.
- Any check that could not be performed goes into `Blocked Validations / Uncertainty` in the review artifact.

## Mandatory process

Run these four checks before reviewing code quality. Details and worked cases are in [references/premise-checks.md](references/premise-checks.md).

1. **Architecture conformance.** Compare the actual placement of classes against the declared style. Report a violation even when the code works.
2. **Technical justification veracity.** For each claim about language, framework, or library behavior, require verification by execution, official documentation, or source code. An unverified claim used to justify a decision is a finding.
3. **External contract origin.** For each field consumed from or sent to an external system, require an authoritative source. A field whose name was chosen by the implementer is a critical finding.
4. **Test independence.** Check that expected values and fixtures come from the real contract source and not from the same assumption embedded in the code under test.

Then record each check as `pass`, `fail`, or `not-checked` in `Premise Checks`.

## Mandatory rules

- Never approve a change whose external contract fields have no authoritative source.
- Never accept a technical claim because it sounds plausible; require the verification method.
- Report an architecture violation even when the behavior is correct.
- Treat defensive fallbacks that mask an unknown contract as a critical finding, not as robustness.
- A green test suite is not evidence of correctness when the fixture and the code share the same assumption.
- Never edit the code under review; produce findings and required fixes.
- A check that was not performed is `not-checked` and must appear under blocked validations; it is never silently `pass`.

## Additional resources

- Check details and real failure cases: [references/premise-checks.md](references/premise-checks.md).
- Review artifact structure: `artifact-report-contract`.
- Architecture rules for Java: `developing-java-spring-applications`.
- Test rules for Java: `writing-java-unit-tests`.

## Stop criteria

- Stop and request the source when an external contract field has no documented origin.
- Stop when the declared architecture style is unknown.
- Do not issue a verdict of `approved` while any premise check is `fail`.

## Completion criteria

- All four checks are recorded as `pass`, `fail`, or `not-checked`.
- Every `fail` has a required fix.
- Every `not-checked` appears under blocked validations.
- The verdict is consistent with the checks.

## Output format

1. Premise Checks table with the four results.
2. Evidence consulted per check.
3. Findings by severity.
4. Required fixes.
5. Blocked validations.
6. Verdict.
