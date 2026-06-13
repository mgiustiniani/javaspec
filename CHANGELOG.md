# Changelog

All notable changes to this project will be documented in this file.

## 0.1.0 — 2026-06-13

- Initial public release on Maven Central under `io.github.jvmspec`.
- Gradle plugin published on the Gradle Plugin Portal with plugin id `io.github.jvmspec`.
- All implementation phases A-E, D2, and publication are complete.
- Java 8-compatible, zero-runtime-dependency core artifact.
- CLI describe/run workflows, configuration, discovery, generation support, matchers, interface doubles, run controls, reports, and programmatic no-JUnit invocation.
- Standalone optional adapters: Maven plugin (`javaspec-maven-plugin`), Gradle plugin (`javaspec-gradle-plugin`), JUnit Platform engine (`javaspec-junit-platform-engine`), and bytecode doubles adapter (`javaspec-bytecode-doubles`).
- Prophecy-style doubles API with ObjectProphecy, MethodProphecy, Promise, Prediction, and typed wrapper generation.
- Aggregate local/CI verification for core, standalone adapters, and examples.
- Release-readiness scaffolding with MIT license, maintainer metadata, version-alignment checks, and source/javadoc artifact profiles.
- Adoption examples, report schema, golden reports, and full documentation.
- Bounded generic Iterable matcher checks, source-preserving sealed-interface updates, config-driven extension activation, ServiceLoader bootstrap hook discovery, programmatic/Maven/Gradle opt-in compilation, additive report metadata/properties, deeper target-profile enforcement, and optional ByteBuddy concrete-class doubles.

## Unreleased

- Finalized documentation for the Phases 30-37 known-limitations resolution program, including
  bytecode doubles usage and example verification notes.
- Added Phases 30-37 resolution updates:
  - bounded generic `Iterable` matcher checks;
  - source-preserving sealed-interface updates;
  - config-driven extension activation and formatter controls;
  - ServiceLoader bootstrap hook discovery;
  - programmatic/Maven/Gradle opt-in compilation;
  - additive report metadata/properties;
  - deeper target-profile enforcement;
  - the standalone optional `javaspec-bytecode-doubles` adapter with
    `examples/bytecode-doubles-basic/`.
- Added the MIT `LICENSE` file and release-readiness scaffolding: version-alignment verification,
  release-artifact profiles, MIT license and maintainer publication metadata, and a release
  checklist; public publishing/deployment/signing/portal credentials were resolved for the 0.1.0
  release.
- Kept the repository layout non-reactor/non-multi-module: the core, Maven plugin, Gradle plugin,
  and JUnit Platform engine remain standalone builds.
- Added explicit skipped/pending semantics: zero-dependency `@Skip`/`@Pending` annotations, runtime
  skip/pending signals and `ObjectBehavior` helpers, distinct `PENDING` counts/status, pending-aware
  formatter summaries, JSON reports, JUnit XML-compatible `<skipped>` mapping, Maven/Gradle report
  behavior, and JUnit Platform skipped-event mapping.
- Added Phase 23 classpath/execution availability diagnostics: enriched not-executable reasons,
  deterministic `RunDiagnostics.executionAvailabilityLines(RunResult)` helper lines, CLI `Execution
  diagnostics:` output, and Maven/Gradle `javaspec:` warning diagnostics with classpath element
  counts, without adding source compilation or changing exit/build-failure semantics.
- Added Phase 24 configuration-level report destinations: top-level JSON/JUnit XML-compatible report
  path aliases in config, `run --config` default report writing, CLI and explicit Maven/Gradle
  adapter override precedence, verbose effective report path display, and unchanged report
  schemas/writers/exit/build semantics.
- Added Phase 25 ServiceLoader external formatter/extension discovery:
  - zero-dependency `JavaspecExtensionLoader` helpers;
  - `RunFormatter`/`JavaspecExtension`/alias `Extension` service types;
  - CLI and Gradle custom formatter selection from effective run classloaders;
  - duplicate extension de-duplication;
  - clear `ExtensionLoadingException` diagnostics;
  - no report schema/content, compilation, dependency, publishing, or Maven plugin formatter-control
    changes.
- Added Phase 26 target-profile enforcement before generation/update writes:
  - `javaspec run` enforces the effective config or CLI `--profile` selection;
  - incompatible described type kinds and resolvable later-JDK API signature types exit `64`;
  - dry-run stays non-mutating and writes no reports on violations;
  - the `org.javaspec.compatibility` `ProfileEnforcement` report API was added;
  - compilation, report schemas, dependencies, and optional adapter architecture remain unchanged.
- Added Phase 27 bootstrap hook execution: configured top-level hooks now run before selected suite
  hooks immediately before examples, receive immutable `BootstrapContext`, preserve order and
  duplicates, fail CLI runs with exit `64` and no reports on bootstrap errors, and remain
  zero-dependency/Java 8-compatible without ServiceLoader, script engines, integrated compilation,
  package scanning, or dependency resolution.
- Added Phase 28 stronger interface doubles: zero-dependency JDK dynamic proxies remain
  interface-only, while `org.javaspec.doubles` now includes argument matchers (`ArgumentMatcher`,
  `ArgumentMatchers`, and `Doubles` aliases), argument-constrained stub priority, throwing stubs,
  and answer callbacks with immutable invocation context, without
  CLI/report/schema/dependency/adapters changes.
- Added Phase 29 opt-in CLI source/spec compilation:
  - `javaspec run --compile` compiles discovered run source/spec trees with the current JDK
    `javax.tools.JavaCompiler` into `target/javaspec-classes` by default;
  - `--compile-output <dir>` implies compilation;
  - compile failures exit `1` without reports;
  - compiler-unavailable runs exit `64`;
  - config keys, Maven/Gradle/JUnit adapter behavior, report schemas, dependency resolution,
    incremental caches, and release/source-level management remain unchanged.

## 0.1.0-SNAPSHOT

- Implemented the Java 8-compatible, zero-runtime-dependency javaspec core with CLI describe/run
  workflows, configuration, discovery, generation support, matchers, interface doubles, run
  controls, reports, and programmatic no-JUnit invocation.
- Added standalone optional adapters for Maven, Gradle, and JUnit Platform, all outside the core
  runtime artifact.
- Added aggregate local/CI verification for the core and standalone adapters while preserving the
  zero-runtime-dependency core policy.
