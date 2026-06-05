# 0016 — Classpath execution availability diagnostics without integrated compilation

## Status

Accepted

## Context

Update note: [ADR 0022](0022-opt-in-cli-source-spec-compilation.md) later adds CLI-only opt-in source/spec compilation. This ADR's no-compilation boundary remains applicable to default CLI runs, diagnostics, programmatic invocation, and optional Maven/Gradle/JUnit Platform adapters.

javaspec discovers specification sources before execution, but the reflection runner can execute examples only when the compiled specification class, its public no-argument example methods, production classes, and required dependencies are available to the selected runner classloader.

Before this decision, source discovery could find specs/examples while execution reported skipped results that were not always explicit enough about classpath or stale-compilation causes. Users need actionable diagnostics across the CLI and optional build-tool adapters, but the project must still preserve the Java 8 baseline, zero-runtime-dependency core, canonical no-JUnit runner, and existing build-tool responsibilities.

Integrated source/spec compilation is intentionally outside the current javaspec boundary. Maven, Gradle, IDEs, or user-provided launchers remain responsible for compiling specs and production code and for placing compiled outputs and dependencies on the javaspec classpath. Explicit user `@Skip` and `PENDING` semantics are intentional non-execution outcomes and must not be conflated with classpath/execution availability problems.

## Decision

Add execution availability diagnostics without adding integrated compilation.

- Enrich core skipped/not-executable reasons when source discovery found a spec/example but the runner classloader cannot load the compiled specification class, one of its dependencies, or the expected public no-argument example method.
- Add the zero-dependency helper `org.javaspec.diagnostics.RunDiagnostics.executionAvailabilityLines(RunResult)` so CLI, adapters, and custom launchers can derive deterministic human-readable diagnostic lines from the canonical `RunResult`.
- Keep the helper focused on execution availability: it reports non-executable specs and missing/stale compiled example methods, and excludes explicit user `@Skip` and `PENDING` results.
- Have the CLI print an `Execution diagnostics:` block only when the helper returns lines. If no explicit classpath was supplied, the CLI explains that the current process classloader was used and suggests `--classpath` or `--classpath-file` with compiled outputs and dependencies. If explicit classpath input was supplied, the CLI reports the explicit entry count and asks users to verify those entries.
- Have the optional Maven plugin log `javaspec:` warning diagnostics only when execution availability issues exist, including the Maven test classpath element count.
- Have the optional Gradle plugin log `javaspec:` warning diagnostics only when execution availability issues exist, including the Gradle classpath element count.
- Do not compile source/spec files as part of diagnostics, default runs, programmatic invocation, or optional adapters; do not add compile-time or runtime dependencies for compilation or classpath analysis; and do not change exit-code/build-failure semantics.

## Consequences

Positive consequences:

- Users get actionable classpath and stale-compilation guidance when specs/examples are discovered from source but are unavailable to the runner classloader.
- CLI, Maven, Gradle, and programmatic callers can share one deterministic diagnostic source derived from the canonical result model.
- The diagnostics preserve intentional skip/pending semantics by excluding explicit `@Skip` and `PENDING` outcomes.
- The zero-runtime-dependency policy and Java 8 compatibility are preserved because diagnostics are string/result-model based.
- Maven and Gradle remain responsible for compilation and classpath assembly, while javaspec reports when their supplied classpaths are insufficient for execution.

Negative consequences and limitations:

- Default runs, diagnostics, programmatic invocation, and optional adapters still do not compile source or spec files; users must run the normal project compilation lifecycle, supply compiled outputs/dependencies, or use the later CLI-only opt-in compilation from ADR 0022.
- Diagnostics identify likely classpath/execution availability causes but do not automatically repair classpaths or stale compiled classes.
- Build-tool and CLI output can include additional warning blocks, although only when execution availability lines exist.
- Optional adapters and custom launchers must continue to pass an appropriate classloader if they want executable results rather than diagnostic skipped results.
- Exit-code and build-failure behavior is intentionally unchanged, so skipped-only availability problems remain non-failing unless a caller adds stricter policy outside the current core semantics.

Related decisions: [ADR 0006](0006-classpath-reflection-runner.md), [ADR 0011](0011-optional-junit-adapter-and-canonical-javaspec-runner.md), [ADR 0015](0015-explicit-skipped-and-pending-semantics.md), and [ADR 0022](0022-opt-in-cli-source-spec-compilation.md).

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
