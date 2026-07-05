# 0020 — Bootstrap hook execution before examples

## Status

Accepted

## Context

ADR 0005 introduced top-level and suite-level `bootstrap` configuration entries as parsed values, but earlier phases treated them as metadata only. Phase 27 needs those entries to become executable run lifecycle hooks while preserving existing configuration syntax, suite selection, class/example filtering, no-JUnit execution paths, Java 8 compatibility, and the zero-runtime-dependency core policy.

The hook mechanism must run against the same compiled-class view used for examples. Default runs and adapters do not compile source/spec files for hooks, and javaspec does not scan packages or add a plugin framework or dependency resolver. ADR 0022 later adds CLI-only opt-in compilation before bootstrap/examples. Failures must be clear and must stop before examples and report writing so partially initialized runs do not produce misleading results.

## Decision

Add a zero-dependency bootstrap boundary under `io.github.jvmspec.bootstrap`:

- `BootstrapHook` is the user hook contract. Hook classes must implement `io.github.jvmspec.bootstrap.BootstrapHook` and provide a public no-argument constructor.
- `BootstrapContext` is immutable and exposes the run classloader plus the discovered specs selected for the run.
- `BootstrapRunner` loads hook classes from the effective run classloader/classpath, instantiates them, and executes them in configured order immediately before examples run.
- `BootstrapException` represents load, construction, type, or execution failures.

Top-level `bootstrap` entries run before the selected suite's `bootstrap` entries. Declaration order is preserved, and duplicate class names are not de-duplicated.

For CLI `javaspec run --config`, bootstrap execution happens after discovery, profile enforcement, generation/update decisions, any successful generation/update work, and any later requested successful CLI compilation, but before example execution and before JSON/JUnit XML-compatible report writing. If discovery finds no specs, the CLI does not execute hooks. A bootstrap failure exits `64`, starts diagnostics with `Error: Bootstrap execution failed`, and writes no reports.

The Maven and Gradle adapters pass the combined top-level plus selected-suite hook list into the canonical `JavaspecInvocation` / `JavaspecLauncher` path. Bootstrap failures fail the Maven build or Gradle task with explicit `javaspec bootstrap execution failed` diagnostics.

Phase 27 deliberately does not add ServiceLoader-based bootstrap discovery, script engines, adapter-integrated source/spec compilation, package scanning, dependency resolution, or third-party runtime dependencies. Hook classes must be compiled and available on the effective run classloader/classpath supplied by the CLI, build tool, host invocation, or the later CLI-only compilation boundary from ADR 0022.

## Consequences

Positive consequences:

- Existing `bootstrap` config entries now have an executable lifecycle meaning.
- Users can prepare test data or environment state immediately before examples without adding dependencies to javaspec core.
- Hook execution uses the same classloader boundary as example execution, keeping CLI, Maven, Gradle, and programmatic paths aligned.
- Ordering is deterministic: top-level hooks first, then selected-suite hooks, with duplicates preserved.
- Bootstrap failures fail early and do not produce reports that could imply examples ran successfully.

Negative consequences and limitations:

- Hooks must be ordinary compiled Java classes visible to the run classloader; default/adapters paths do not compile, scan packages, resolve dependencies, or load scripts for them.
- Hook construction requires a public no-argument constructor, which limits dependency injection styles unless users arrange dependencies externally.
- Hook failures share CLI exit code `64` with other usage/configuration/profile failures.
- Hook side effects are user controlled; javaspec provides an immutable context but cannot make external state changes reversible.
- No ServiceLoader or implicit provider discovery was added for bootstrap hooks, so class names remain explicit config values.

Related decisions: [ADR 0001](0001-java-8-baseline-with-lts-target-profiles.md), [ADR 0002](0002-zero-runtime-dependency-policy.md), [ADR 0005](0005-restricted-line-based-configuration-format.md), [ADR 0006](0006-classpath-reflection-runner.md), [ADR 0010](0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md), [ADR 0011](0011-optional-junit-adapter-and-canonical-javaspec-runner.md), [ADR 0017](0017-configuration-level-report-destinations.md), [ADR 0019](0019-deep-target-profile-enforcement.md), and [ADR 0022](0022-opt-in-cli-source-spec-compilation.md).

Related ARC42 sections: [2. Constraints](../arc42/02-constraints.md), [4. Solution Strategy](../arc42/04-solution-strategy.md), [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
