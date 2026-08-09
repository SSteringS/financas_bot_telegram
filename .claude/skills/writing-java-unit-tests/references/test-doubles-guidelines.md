# Test Doubles: Mock, Stub, and Fake

## Mock

An object that verifies interactions (`verify(...)`) happened as expected. Use for ports/collaborators whose calls matter to the behavior being tested (e.g. `OrderRepositoryPort.save`).

## Stub

An object that returns canned answers (`given(...).willReturn(...)`) without verifying interactions. Use when only the returned value matters, not whether or how it was called.

## Fake

A lightweight working implementation (e.g. an in-memory `Map`-based repository) used when a real mock/stub setup would be more complex than a small working substitute.

## When to avoid mocking

- Do not mock value objects, DTOs, or Java records (`OrderId`, `CustomerId`, `OrderRequest`); construct real instances instead.
- Do not mock the class under test.
- Do not mock simple data holders with no behavior; only mock collaborators that perform I/O or non-trivial logic (repositories, external clients, other services).

## Quick decision guide

| Situation | Use |
|---|---|
| Need to verify a method was called with specific arguments | Mock |
| Only need a fixed return value to drive the test | Stub |
| Real object is a plain data holder (record/DTO) | Real instance, not a double |
| In-memory substitute is simpler than mocking a repository | Fake |
