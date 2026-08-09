---
name: developing-java-spring-applications
description: Implements and refactors Java Spring Boot backend code following SOLID principles and the project's chosen architecture (MVC or Hexagonal/ports-and-adapters). Use when creating or changing controllers, services, repositories, use cases, ports, adapters, DTOs, entities, Spring configuration, dependency injection, exception handling, or REST endpoints in a Java/Spring Boot codebase.
---

# Developing Java Spring Applications

<!-- section-policy: legacy -->
<!-- Migration note: opts into the legacy 10-section rule so validate_skill.py keeps passing without adding per-section inclusion-reason comments. Migrate to v2 by removing this marker and following the procedure in .claude/skills/creating-skills/ADVANCED-PATTERNS.md > Legacy compatibility. -->

## Objective

Implement or refactor Java Spring Boot backend code that follows SOLID principles and is structured according to the project's chosen architecture (MVC or Hexagonal), producing idiomatic, testable, framework-correct code.

## When to use

Use this skill when:

- Creating or changing a REST endpoint, controller, service, use case, repository, port, adapter, DTO, entity, or Spring configuration class.
- Refactoring existing Java/Spring code to fix a SOLID violation (SRP, OCP, LSP, ISP, DIP).
- Deciding how to structure a new feature (package layout, layering) in an existing MVC or Hexagonal codebase.
- Reviewing Java/Spring code for architecture or dependency-direction violations.

## When not to use

Do not use this skill for:

- Writing or reviewing unit/integration tests only — use `writing-java-unit-tests` instead, after the production code is implemented here.
- CI/CD pipelines, Dockerfiles, Kubernetes manifests, or other deployment/infra work.
- Defining business/domain requirements — this skill structures the code, it does not decide the business rule.
- Non-Java languages or non-Spring frameworks.
- Writing raw SQL migrations — only persistence-mapping conventions (entities/repositories) are in scope.

## Minimum required data

1. Target architecture: MVC or Hexagonal.
2. Whether an existing codebase convention already exists (package layout, base classes) to keep consistency with.
3. Whether Lombok/MapStruct are already in use in the project.

## If information is missing

- Architecture: never assume; ask the user to choose MVC or Hexagonal before generating code that spans more than one class.
- Lombok/MapStruct: check the build file (`pom.xml` or `build.gradle`) for existing dependencies first; ask only if detection is inconclusive.
- Java/Spring Boot version: assume Java 21 and Spring Boot 3.x unless the build file states otherwise.
- Do not invent business rules, validation constraints, or persistence details not provided by the user.
- External contract fields: never choose a field name, type, or format yourself. Stop and ask for the provider specification or a captured real payload.

## Mandatory process

1. Determine the architecture (ask if unknown) and confirm before generating multi-class output.
2. Detect Lombok/MapStruct usage from the build file; fall back to asking only if inconclusive.
3. Read the matching reference file before writing code: [references/mvc-architecture.md](references/mvc-architecture.md) or [references/hexagonal-architecture.md](references/hexagonal-architecture.md), plus [references/spring-conventions.md](references/spring-conventions.md).
4. Generate code layer by layer, respecting the dependency direction of the chosen architecture.
5. Apply the SOLID checklist in [references/solid-principles.md](references/solid-principles.md) to every new or changed class.
6. Point out (do not silently skip) any SOLID violation found in code being refactored, with a concrete before/after.
7. State that test coverage should be added with the `writing-java-unit-tests` skill; do not write tests here.
8. Deliver the output in the format below.

## Mandatory rules

- Use constructor injection exclusively; never field (`@Autowired` on a field) or setter injection.
- In Hexagonal architecture, the domain package must not import Spring, JPA, or any framework type.
- DTOs are never the same class as JPA entities or domain objects; always map explicitly (manually or via MapStruct).
- Every REST error path is handled through a centralized `@RestControllerAdvice`, never inline `try/catch` returning raw stack traces.
- Public inputs are validated with Bean Validation annotations (`@Valid`, `@NotNull`, etc.) at the boundary (controller/adapter), not deep in the domain.
- Keep one reason to change per class (SRP); split a class the moment it serves two unrelated responsibilities.
- Prefer constructor-based immutability (`final` fields, records for value objects) over mutable setters.
- Never mask an unknown external contract with defensive code. Multiple candidate field names, alias annotations covering several spellings, silent null fallbacks, and permissive deserialization over an unverified payload all hide the failure instead of surfacing it.
- Every field consumed from or sent to an external system must trace back to an authoritative source; record that source so it can be reported in the status artifact.
- Do not reference process documents, plan sections, or artifact requirement numbers in class names, constant names, or comments; describe the behavior instead.

## Additional resources

- For the SOLID checklist and before/after examples, see [references/solid-principles.md](references/solid-principles.md).
- For MVC package layout and a layer example, see [references/mvc-architecture.md](references/mvc-architecture.md).
- For Hexagonal package layout and a ports/adapters example, see [references/hexagonal-architecture.md](references/hexagonal-architecture.md).
- For Spring DI, validation, exceptions, transactions, and Spring Data conventions, see [references/spring-conventions.md](references/spring-conventions.md).
- For a complete feature example, see [examples/order-mvc-example.md](examples/order-mvc-example.md) and [examples/order-hexagonal-example.md](examples/order-hexagonal-example.md).
- After implementing production code, use the `writing-java-unit-tests` skill to add unit/slice tests.

## Stop criteria

- Stop and ask before generating code if the architecture (MVC vs Hexagonal) is not specified and the change spans more than one class.
- Stop if the request also asks to write tests inside this skill; redirect to `writing-java-unit-tests` instead of merging responsibilities.
- Do not invent persistence schema, external API contracts, or business validation rules not provided.
- Stop when an external contract field name, type, or format cannot be confirmed in an authoritative source; report what is missing instead of implementing a fallback.

## Completion criteria

- Generated code compiles conceptually (correct imports, valid Spring annotations) and respects the chosen architecture's dependency direction.
- No SOLID violation from the checklist is left unaddressed without an explicit note.
- Constructor injection is used everywhere; no field injection.
- DTO/entity separation is respected.
- Every external contract field has a named source, or the work was stopped.
- Output explicitly names which files should receive tests next.

## Output format

1. Architecture confirmed (MVC or Hexagonal) and why.
2. List of files created/changed with a one-line purpose each.
3. Complete code for each file.
4. SOLID notes: violations found (if refactor) and how they were fixed.
5. External contract sources: each external field with its authoritative source, or `none`.
6. Next step: point to the `writing-java-unit-tests` skill for coverage.
