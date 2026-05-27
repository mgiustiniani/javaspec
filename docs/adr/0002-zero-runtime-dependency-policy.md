# 0002 — Zero Runtime Dependency Policy

## Status

Accepted

## Context

javaspec is intended to be a small Java specification framework that can be added to projects without introducing runtime dependency conflicts or transitive dependency cost.

Many features common in test frameworks are often implemented with external libraries: CLI parsing, YAML parsing, assertions, mocking, logging, dependency injection, code generation, bytecode manipulation, or report generation. The project requirement is stricter: runtime dependencies are not allowed. Dependencies may be used only in test scope.

## Decision

The javaspec runtime artifact will have no third-party runtime dependencies. It may use only the Java 8 standard library.

External dependencies may be introduced only when they are scoped to tests, compatibility verification, or build-time quality checks and do not become runtime dependencies of the distributed artifact.

Feature design must follow this order of preference:

1. Use Java 8 standard library APIs.
2. Implement a small internal capability when the maintenance cost is acceptable.
3. Expose an extension point so optional integrations can be provided outside the core runtime.
4. Defer or omit the feature from the core runtime.

## Consequences

Positive consequences:

- The runtime artifact remains lightweight and predictable.
- Users avoid transitive dependency conflicts.
- Java 8 compatibility is easier to preserve.
- Core behavior is easier to audit.

Negative consequences:

- The project must implement or simplify features that other frameworks delegate to libraries.
- Advanced mocking for concrete/final classes is limited without bytecode libraries.
- Rich configuration formats may need restricted parsers or optional extensions.
- Formatter and report features must avoid dependency-heavy implementations.

Follow-up actions:

- Add a build-time dependency audit after Maven scaffolding exists.
- Document feature limitations caused by the zero-dependency rule.
- Ensure test dependencies never leak into the runtime artifact.
