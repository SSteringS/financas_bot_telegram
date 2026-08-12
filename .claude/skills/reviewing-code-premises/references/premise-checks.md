# Premise Checks

## Table of contents

- Check 1 - Architecture conformance
- Check 2 - Technical justification veracity
- Check 3 - External contract origin
- Check 4 - Test independence
- Check 5 - Process references inside code
- Severity guide

## Check 1 - Architecture conformance

Compare where classes actually live against the style declared in the plan.

Ask:

- Does each class sit in the layer its responsibility belongs to?
- Does the dependency direction match the declared style?
- In hexagonal, does the domain import any framework type?

Real failure: request and response DTOs were placed in a `domain/dto` package while the plan declared MVC. The code compiled and the tests passed, and the review approved it. Correct placement was the controller layer, since those types belong to the web boundary and not to the domain.

Rule: report the violation even when behavior is correct. Working code in the wrong layer is still a finding.

## Check 2 - Technical justification veracity

Any claim about language, framework, or library behavior that is used to justify a decision must state how it was verified.

Accepted verification methods:

- executed and observed output
- official documentation with the specific behavior
- library or language source code

Real failure: a plan stated that multiplying a monetary value by one hundred produced a result with four decimal places and no change in magnitude, and used that claim to justify a conversion decision. The actual behavior multiplies the magnitude. The claim was then copied into a Javadoc comment and the review approved it, so a false statement became documentation.

Rule: an unverified claim used as justification is a finding at least `high`. A false claim that reached documentation or comments is `critical`.

## Check 3 - External contract origin

For each field consumed from or sent to an external system, require an authoritative source: the provider's specification, a captured real payload, or a contract file in the repository.

Ask:

- Where did this field name come from?
- Where did this format or type come from?
- Is there any fallback that would hide a mismatch?

Real failure: an integration invented field names for a payment provider response and added defensive aliases as a fallback for several alternative names. None of the six candidate names matched the real contract. Combined with a deserialization setting that ignores unknown properties, the real field arrived as null with no error, so the failure would be silent in production.

Rules:

- A field name chosen by the implementer without a source is `critical`.
- Defensive aliases or multi-name fallbacks used to cover uncertainty are `critical`, not robustness. The correct action is to stop and ask for the contract.
- Deserialization settings that ignore unknown properties turn a mismatch into a silent failure; when the contract is unverified, this combination must be flagged.

## Check 4 - Test independence

A test only provides evidence when the expected value has an origin independent from the code under test.

Ask:

- Where does the fixture payload come from?
- Was the expected value derived from the same assumption the code implements?
- Would this test fail if the assumption were wrong?

Real failure: the test for the invented contract above built its payload from the same invented field names. The suite was green while the integration was broken, because the test confirmed the assumption against itself.

Rule: when fixtures encode the same assumption as the production code, the test provides no evidence. Report it and require the fixture to be sourced from the real contract.

## Check 5 - Process references inside code

Code and test names must not cite process documents, plan sections, or numbered requirements from artifacts under `docs/`.

Real failure: a constant named after a document example and a test display name citing a plan section number. Those references break as soon as the document changes and are meaningless to anyone reading only the repository.

Rule: replace the reference with the behavior it describes.

## Severity guide

| Situation | Severity |
|---|---|
| Invented external contract field | critical |
| Defensive fallback masking an unknown contract | critical |
| False technical claim reaching code comments or docs | critical |
| Test fixture sharing the assumption of the code under test | high |
| Architecture violation with correct behavior | high |
| Unverified technical justification not yet in documentation | high |
| Process document reference inside code | medium |
