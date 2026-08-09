---
name: writing-java-unit-tests
description: Writes and reviews unit and slice tests for Java Spring Boot code using JUnit 5, Mockito, and AssertJ. Use when asked to add test coverage, write tests for a class or method, review or fix existing tests, or verify isolation and mocking boundaries in a Java codebase.
---

# Writing Java Unit Tests

<!-- section-policy: legacy -->
<!-- Migration note: opts into the legacy 10-section rule so validate_skill.py keeps passing without adding per-section inclusion-reason comments. Migrate to v2 by removing this marker and following the procedure in .claude/skills/creating-skills/ADVANCED-PATTERNS.md > Legacy compatibility. -->

## Objective

Write or review unit and slice tests for Java Spring Boot classes (use cases, services, controllers, repositories) using JUnit 5, Mockito, and AssertJ, following the Arrange-Act-Assert structure and correct isolation boundaries.

## When to use

Use this skill when:

- Adding test coverage for a class or method just implemented or changed.
- Reviewing existing tests for over-mocking, weak assertions, or missing edge cases.
- Fixing a flaky or brittle test.
- Choosing the right test slice (`@ExtendWith(MockitoExtension.class)`, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`) for a given class.

## When not to use

Do not use this skill for:

- Deciding production code architecture or fixing SOLID violations — use `developing-java-spring-applications` first, then come back here.
- Full end-to-end/UI tests, or load/performance tests.
- Integration tests requiring Testcontainers or a real database/broker — not yet covered by this skill; flag it as out of scope instead of improvising conventions.
- Non-Java test stacks.

## Minimum required data

1. The class or method to test (or the diff being reviewed).
2. Its dependencies (constructor parameters), so mocking boundaries can be decided correctly.
3. Whether the project already has a test-naming convention in place.

## If information is missing

- Test naming: default to `should_ExpectedBehavior_when_StateUnderTest`; reuse the project's existing convention instead if one is found in nearby test files.
- If the class under test is not shown, ask for it rather than guessing its dependencies.
- Do not invent business assertions that cannot be derived from the code or the request.
- If a fixture requires an external payload whose real shape is unknown, stop and ask for the contract; do not build the fixture from the production code.

## Mandatory process

1. Identify the class under test and its real collaborators (constructor parameters).
2. Choose the narrowest correct test type: plain JUnit + Mockito unit test by default; `@WebMvcTest` only for controllers; `@DataJpaTest` only for repository/query logic; `@SpringBootTest` only when the scenario truly requires full context.
3. Read [references/java-junit-mockito.md](references/java-junit-mockito.md) for the exact setup pattern before writing the test class.
4. Write one test method per behavior, following Arrange-Act-Assert.
5. Mock only real collaborators (other beans/ports); never mock DTOs, value objects, or the class under test itself — see [references/test-doubles-guidelines.md](references/test-doubles-guidelines.md).
6. Use AssertJ fluent assertions (`assertThat(...)`) instead of plain JUnit asserts.
7. Cover the happy path, at least one edge case, and at least one failure/exception path per public method with meaningful logic.
8. Deliver the output in the format below.

## Mandatory rules

- One behavior asserted per test method; do not assert multiple unrelated behaviors in one test.
- Never test private methods directly; test them through the public method that uses them.
- Do not mock the class under test.
- Do not use `Thread.sleep` or timing-based waits; use deterministic setups.
- Test method and class names must state the behavior, not the implementation detail.
- Keep `verify(...)` calls limited to interactions that matter for the behavior being tested, not incidental calls.
- Fixtures and expected values that represent an external contract must come from the real contract source (provider specification or captured payload), never from the production code that consumes them. A test built on the same assumption as the code under test proves nothing.
- Do not copy field names out of the mapping class to build the payload under test; take them from the contract.
- Test names and display names must not cite process documents, plan sections, or artifact requirement numbers.

## Additional resources

- For concrete JUnit 5, Mockito, and AssertJ setup and examples, see [references/java-junit-mockito.md](references/java-junit-mockito.md).
- For guidance on mock vs stub vs fake and when to avoid mocking, see [references/test-doubles-guidelines.md](references/test-doubles-guidelines.md).
- For a full worked example, see [examples/order-service-test-example.md](examples/order-service-test-example.md).
- Use `developing-java-spring-applications` first if the production code still needs architecture/SOLID work.

## Stop criteria

- Stop and ask if the class under test (or its dependencies) is not available.
- Stop if the request wants Testcontainers/integration-database tests; state this is out of scope until defined.
- Do not fabricate business assertions unsupported by the code shown.
- Stop when an external payload fixture cannot be traced to an authoritative source.

## Completion criteria

- Every public method with non-trivial logic has at least one test.
- No test mocks a value object, DTO, or the class under test.
- All assertions use AssertJ.
- Test names describe behavior, not implementation.
- Every external contract fixture has a named source.

## Output format

1. Test class(es) created/changed with a one-line purpose each.
2. Complete test code.
3. Fixture sources: each external payload fixture with its authoritative source, or `none`.
4. Coverage notes: which behaviors are covered, which are intentionally out of scope and why.
