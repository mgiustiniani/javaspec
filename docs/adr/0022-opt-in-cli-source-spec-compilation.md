# 0022 — Opt-in CLI source/spec compilation

## Status

Accepted

## Context

ADR 0006 and ADR 0016 established that javaspec discovers source specifications but executes examples only when compiled spec classes, production classes, and dependencies are available to the selected classloader. That boundary made build tools and launchers responsible for compilation and classpath assembly, while Phase 23 diagnostics explained source-only or stale compiled-class situations.

CLI users still need a lightweight way to run source-only specification trees without first invoking a separate build command. The feature must keep the zero-runtime-dependency core, Java 8-compatible binary, run/describe command split, report schemas, optional adapter boundaries, and existing default behavior intact. It must also avoid turning javaspec into a dependency resolver, an incremental compiler cache, a Maven/Gradle/JUnit adapter compiler, or a source-level/release-management tool.

## Decision

Add opt-in compilation to CLI `javaspec run` only:

- `--compile` enables compilation before executable examples.
- `--compile-output <dir>` selects the output directory and implies `--compile`.
- The default output directory is `target/javaspec-classes`.
- `describe` / `desc` rejects both compilation options as run-only controls.

The CLI uses the current JDK's `javax.tools.JavaCompiler` through `ToolProvider.getSystemJavaCompiler()`. It does not fork `javac`, add dependencies, resolve dependency artifacts, manage release/source levels, or maintain an incremental compilation cache. If the compiler API is unavailable, for example when launched from a JRE-like runtime, the CLI prints a compiler-unavailable diagnostic and exits `64`.

When compilation is requested and the run has discovered specs, compilation happens after discovery, profile enforcement, related-spec generation, support/source generation or updates, and generation/update prompt or `--generate` decisions, but before bootstrap hook execution and example execution. The compiler input is every `.java` file under the effective source root and effective spec root; files are de-duplicated by normalized absolute path. No-spec CLI runs skip compilation. Dry-run remains non-mutating and skips compilation even when `--compile` or `--compile-output` is present.

Compilation classpath order is deterministic: compile output directory first, then explicit CLI classpath entries from `--classpath` / `--classpath-file`, then the current process `java.class.path`. After successful compilation of at least one source file, the execution classloader puts the compile output directory before explicit CLI classpath entries; the current process classloader remains the parent.

Successful compilation does not change report schemas or runner result models. A compilation failure exits `1`, prints `Compilation failed:` followed by compiler diagnostics, skips bootstrap/example execution, and writes no JSON or JUnit XML-compatible reports.

No configuration keys are added. The optional Maven plugin, Gradle plugin, and JUnit Platform engine continue to rely on their host classpaths and do not gain source/spec compilation behavior.

## Consequences

Positive consequences:

- Source-only CLI runs can opt into compilation while default CLI runs remain classpath/reflection based.
- The implementation preserves the zero-runtime-dependency policy by using only the JDK compiler API available from the current JDK.
- The existing generation/profile/bootstrap ordering stays explicit: generated or updated sources can be compiled before hooks and examples, while dry-run remains non-mutating.
- Explicit classpath entries still matter for dependencies and formatter/extension jars; successful CLI compilation simply places generated classes ahead of those entries for execution.

Negative consequences and limitations:

- Users must run the CLI with a JDK that exposes `javax.tools.JavaCompiler`; otherwise compilation requests fail with exit `64`.
- The feature is not a dependency resolver, source-level/release manager, annotation-processing policy, incremental compilation cache, or build-tool replacement.
- Maven, Gradle, programmatic invocation, and JUnit Platform engine paths remain unchanged and still require their host/tooling classpaths to contain compiled specs, production classes, hooks, and dependencies.
- Compile failures intentionally do not produce reports, so report consumers see no partial runner result for failed compilation.
- Source-only CLI execution is improved only when users explicitly opt into `--compile` or `--compile-output`; default CLI behavior and adapter behavior continue to skip/not-execute unavailable compiled specs with diagnostics.

Related decisions: [ADR 0001](0001-java-8-baseline-with-lts-target-profiles.md), [ADR 0002](0002-zero-runtime-dependency-policy.md), [ADR 0006](0006-classpath-reflection-runner.md), [ADR 0008](0008-run-only-controls-and-non-mutating-dry-run-planning.md), [ADR 0010](0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md), [ADR 0011](0011-optional-junit-adapter-and-canonical-javaspec-runner.md), [ADR 0016](0016-classpath-execution-availability-diagnostics.md), [ADR 0019](0019-deep-target-profile-enforcement.md), and [ADR 0020](0020-bootstrap-hook-execution.md).

Related ARC42 sections: [2. Constraints](../arc42/02-constraints.md), [4. Solution Strategy](../arc42/04-solution-strategy.md), [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
