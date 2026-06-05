# 0021 — Stronger interface doubles

## Status

Accepted

## Context

ADR 0007 established a zero-runtime-dependency doubles boundary based on JDK dynamic proxies for ordinary Java interfaces. That boundary deliberately avoided concrete-class, final-class, static-method, constructor, and bytecode-based mocking so the core artifact could remain Java 8-compatible and dependency-free.

Phase 28 needs interface doubles to cover more common collaborator-specification scenarios without crossing that boundary. Users need argument matchers, deterministic precedence between method-wide and argument-constrained stubs, stubs that throw exceptions, and callback/answer stubs that can compute a result from invocation details. The change must keep existing exact argument behavior, null handling, array-aware equality, call history, verification helpers, and adapter/report surfaces stable.

## Decision

Keep core doubles as zero-dependency JDK dynamic proxies for ordinary interfaces only. The doubles API continues to reject unsupported targets such as `null`, primitives, arrays, annotations, enums, concrete classes, and final classes. Phase 28 does not add concrete class, final class, static method, constructor, default-interface-method, or bytecode mocking.

Add argument matcher support under `org.javaspec.doubles`:

- `ArgumentMatcher` matches one observed argument and can describe itself for diagnostics.
- `ArgumentMatchers` provides factories: `any()` / `anyArgument()`, nullable typed `any(Class<?>)` / `anyType(Class<?>)`, `isNull()`, `notNull()`, and `eq(Object)` / `equalTo(Object)` using javaspec's array-aware equality.
- `Doubles` exposes the same matcher factories as convenience aliases.

Existing vararg APIs interpret `ArgumentMatcher` arguments as matchers while continuing to treat ordinary values as exact expected values. This applies to stubbing (`when`), verification (`verify`, `verifyCalled`, `verifyNotCalled`, exact call counts), call queries (`calls`), and `Call.hasArguments`. Exact ordinary matching still supports `null` and compares arrays by contents rather than identity.

Use deterministic stub precedence: argument-constrained stubs, including matcher patterns, take priority over method-wide stubs. Within the same priority, the newest matching stub wins.

Add additional stub actions on `MethodStub`:

- `thenThrow(Throwable)` / `throwsException(Throwable)` make matching calls throw the supplied throwable. Calls are recorded before the throwable is thrown.
- `thenAnswer(StubAnswer)` / `answers(StubAnswer)` invoke a callback with an immutable `DoubleInvocation` context. `DoubleInvocation` exposes the reflective method, method name, immutable argument snapshots, defensive argument-array copies, and per-argument defensive copies where needed.

Answer return values use the same return type validation rules as `thenReturn(Object)`. Throwables raised by answer callbacks propagate from the proxy invocation.

No CLI behavior, report content, report schema, dependency policy, optional adapter behavior, or generated/example asset behavior changes as part of this decision.

## Consequences

Positive consequences:

- Interface doubles can express common collaborator expectations without pulling a mocking or bytecode library into the core runtime.
- Argument matcher factories keep matcher use explicit and zero-dependency while preserving existing exact/null/array argument semantics.
- Stub precedence is predictable: specific argument-constrained behavior overrides method-wide behavior, and newer matching stubs override older ones within the same priority.
- Throwing stubs and answer callbacks allow exception paths and computed responses to be specified while retaining call history.
- CLI, report, schema, dependency, and optional adapter boundaries remain unchanged.

Negative consequences and limitations:

- The core still cannot double concrete classes, final classes, static methods, constructors, primitives, arrays, annotations, or enums.
- Default interface methods are still not invoked by the proxy handler.
- Callback stubs execute user code and can introduce user-controlled side effects or throwables that javaspec cannot make reversible.
- Argument matcher callbacks should remain side-effect free; javaspec can provide defensive invocation snapshots but cannot prevent user matcher side effects.
- More expressive stubbing increases the need for clear diagnostics and tests around matcher descriptions, precedence, return validation, and call recording.

Related decisions: [ADR 0001](0001-java-8-baseline-with-lts-target-profiles.md), [ADR 0002](0002-zero-runtime-dependency-policy.md), and [ADR 0007](0007-jdk-proxy-only-interface-doubles.md).

Related ARC42 sections: [2. Constraints](../arc42/02-constraints.md), [4. Solution Strategy](../arc42/04-solution-strategy.md), [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
