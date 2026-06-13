# 0006 — Classpath Reflection Runner

## Status

Accepted

## Context

The Phase 5/6 MVP adds executable examples to `javaspec run` after the existing discovery, generation, and source-update workflow. The project must preserve Java 8 compatibility and the zero-runtime-dependency policy. It already has deterministic discovery metadata through `DiscoveredSpec` and `SpecExample`, including suite selection plus class and example filters.

The CLI currently receives Java source and spec roots but does not own project compilation. Source-only specifications may be discovered even when their compiled classes are not available to the CLI process.

## Decision

javaspec implements the MVP example runner as a dependency-free reflection runner in `org.javaspec.runner`.

After `javaspec run` completes discovery, generation, and source updates without declined prompts, it executes examples only when the compiled specification class is available on the effective classloader. The runner reuses `DiscoveredSpec` and `SpecExample` metadata, so suite, class, and example filters remain effective and unrelated examples are not executed.

For each reflected example, the runner creates a fresh specification instance. If a public no-argument `let()` method exists, it is invoked before the example. If a public no-argument `letGo()` method exists, it is invoked after the example, including after failures.

Example outcomes are:

- `PASSED` when the example completes normally.
- `FAILED` when the example throws `AssertionError`.
- `BROKEN` when the example, lifecycle method, instantiation, or reflection inspection fails with a non-assertion throwable.
- `SKIPPED` when the specification class cannot be loaded or the reflected example method is missing or not public no-arg.

The CLI prints a summary of total, passed, failed, broken, and skipped examples. It exits with code `1` when executable examples fail or break. Skipped-only executable runs do not fail the process.

## Consequences

Positive consequences:

- The runner preserves Java 8 compatibility and adds no runtime dependencies.
- Discovery, suite selection, class filters, and example filters remain the single source of truth for what may execute.
- Reflection execution is simple enough for the MVP and works with compiled project classes supplied by Maven, IDEs, custom launchers, or another classpath setup.
- Fresh instances and optional `let()`/`letGo()` hooks provide an initial deterministic lifecycle.

Negative consequences and limitations:

- The CLI runner does not compile source or spec files itself. Source-only or otherwise unavailable specification classes are skipped rather than executed.
- Lifecycle support is intentionally minimal: public no-argument `let()` and `letGo()` only.
- Missing reflected example methods are skipped because the source-discovered metadata and compiled class can diverge.
- Full runner features such as stop-on-failure, bootstrap execution, active formatter selection, richer reporting, and profile-aware execution were outside the original MVP. Stop-on-failure/formatter/reporting and explicit skipped/pending semantics are now covered by later ADRs and phases.

Verification:

- `mvn verify` passed with 307 tests.
- `mvn dependency:tree -Dscope=runtime` still showed only `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT`.

Implementation follow-up:

- Phase 9 implemented stop-on-failure, dry-run planning, active built-in formatter selection, profile selection, and verbose diagnostics; see [ADR 0008](0008-run-only-controls-and-non-mutating-dry-run-planning.md).
- Phase 11 implemented optional JSON runner reports and public formatter contracts; see [ADR 0010](0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md).
- Phase 22 implemented explicit skipped/pending semantics; see [ADR 0015](0015-explicit-skipped-and-pending-semantics.md).
- Bootstrap execution and deep profile-aware execution remain future work.
