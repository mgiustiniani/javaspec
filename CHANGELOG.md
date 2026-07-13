# Changelog

All notable changes to this project will be documented in this file.

## Unreleased

- Reconciled multi-component record constructor slots with stronger accessor evidence before stub
  planning, so local example names no longer define the record API when accessors differ; typed
  generic factory expectations can identify the component even when the expected value expression
  is intentionally different from the constructor expression.
- Added recursive structured generic type resolution and deterministic import rendering for record
  skeletons and generated SpecSupport, including nested generics and simple-name collisions.
- Made `--formatter json` reserve stdout for exactly one JSON document and route operational
  diagnostics to stderr, including generation-stop and compilation-failure paths.
- Added deterministic `--generation-report` output for applied, dry-run, stopped, and failed runs,
  with sorted source-relative pending-stub locations.
- Pending generated stubs now produce a synthetic BROKEN result independently of `--compile`.
- Record component/accessor equivalence now preserves all generic arguments, arrays, and wildcard
  bounds instead of comparing erased raw types.
- Constructor discovery now deduplicates by declaring type and ordered erased Java parameter
  signature while retaining full `ConstructorDescriptor` structural equality; incompatible generic
  requests with the same erasure fail with `CONFLICTING_CONSTRUCTOR_SIGNATURE`.
- Existing constructor parsing now uses balanced Java parameter/body scanning for nested generics,
  annotations, arrays, varargs, and multiline signatures without rewriting matched implementations.
- Class and record source synchronization is planned before mutation and passes through one explicit
  `--generate`/interactive authorization gate; non-interactive and dry-run commands are read-only.
- Generation reports now distinguish proposed actions from actual applied writes and derive
  `appliedWrites`/`NO_CHANGES` from recorded changed-file writes.
- Constructor synchronization now recognizes package-private and generic constructors, resolves
  type-variable erasure in source context, and preserves legal overloads whose parameter types have
  the same simple name in different packages.
- Added an internal Java-only `SpecLanguageFrontend -> BehaviorContract ->
  ProductionLanguageBackend` seam to prepare post-1.0 language adapters without adding a public
  language SPI or changing Java generation behavior. The contract now exposes portable subject
  shape, relationships, structured types, construction/callable signatures, invocation kind, and
  unknown-type evidence while retaining `DescribedType` as the Java compatibility bridge.
- Extracted constructor observation/identity handling, construction-argument inference, Java
  expression/type inference, callable discovery, subject declaration discovery, and example
  discovery from the public `SpecDiscovery` facade; the facade now contains only deterministic file
  traversal and component orchestration while preserving its API and generated output.
- Split Java inference into focused literal/factory inference, expression-argument splitting, source
  context parsing, and orchestration components. Expression commas deliberately ignore relational
  angle brackets while declaration parameters use the balanced generic-aware syntax splitter.
- Reduced `ClassMethodUpdater` to a stable source/file facade by extracting method eligibility,
  existing-member inventory, deterministic method/factory rendering, offset-preserving source
  editing, and sealed-interface synchronization.
- Consolidated internal Unicode-aware Java identifier checks for discovery and generation.
- Preserved `SEALED_INTERFACE` during production-signature refinement so CLI synchronization updates
  both sealed roots and in-file nested permitted implementations instead of degrading to a plain
  interface plan.
- Reduced `GenerationOrchestrator` by extracting fail-closed preflight validation, central CLI write
  authorization, dry-run change detection, related-spec generation, and prophecy generation while
  retaining atomic activity accounting and authorization outside language backends.
- Reused the balanced syntax splitter for generic method-parameter parsing as well as method and
  record-component source parsing, including generic types whose arguments contain commas.
- Excluded framework lifecycle calls from production-method discovery, resolved nested production
  member types ahead of colliding imports, and rejected unsafe owner-return inference from arbitrary
  local helper assignments; focused JUnit fixtures cover the corrected behavior without introducing
  Cucumber/Gherkin tooling.
- Added reusable test-only CLI project fixtures for source/spec trees, authorization input,
  compilation output, reports, and byte/SHA-256/mtime preservation assertions.
- Run aggregate Gradle verification without a persistent daemon so sequential Maven reinstalls of
  the same RC coordinate cannot leave Gradle tests using stale transformed core artifacts.
- Added JaCoCo HTML/XML/CSV coverage reporting and an opt-in OWASP Dependency-Check security profile
  with official NVD feeds and a CVSS 7.0 failure threshold.

## 1.0.0-RC4 — 2026-07-11

- Fixed record-component inference so literal constructor examples are never emitted as identifiers;
  exact zero-argument accessor expectations now provide component names for boolean, numeric,
  string, enum, negative, and null-like values.
- Added fail-closed `AMBIGUOUS_RECORD_COMPONENT_NAME` diagnostics for missing, illegal, duplicate,
  or otherwise unmappable names before production or generated-support writes.
- Preserved RC3 source-first Maven support generation and matcher-only enriched-enum regeneration.

## 1.0.0-RC3 — 2026-07-11

- Added the Maven `javaspec:generate` goal, bound by consumers to `generate-test-sources`, so clean
  builds discover specs from source, regenerate required support, register generated test sources,
  and compile without tracked generated output.
- Added clean mixed record/enriched-enum regression coverage for base support generation,
  compilation, execution, zero pending/stubs, and second-pass idempotence.

## 1.0.0-RC2 — 2026-07-11

- Added the JLC Java-language coverage manifest/harness and per-JDK CI fixture gate for final Java
  constructs through Java 25, with progressive `COVERED`/`PLANNED` evidence and strict stable mode.
- Hardened source-preserving updates for text blocks and direct-member scope: fake braces/signatures
  in literals, local/anonymous/nested methods, and secondary top-level types no longer suppress a
  required subject method update.
- Added Java 25 preservation evidence for unnamed patterns, flexible constructors, module imports,
  Markdown documentation comments, and Stream Gatherers; compact source files now fail closed as
  javaspec subjects before writes and recommend a normal named subject type.
- Closed cross-cutting source-fidelity coverage for CRLF, missing final newlines, UTF-8 BOM, Unicode,
  local indentation, dry-run parity, atomic failure cleanup, idempotence, and diagnostics.
- Added fail-closed spec-lambda target inference: explicit casts and typed locals/parameters produce
  deterministic standard or custom SAM signatures, one unique production signature refines inline
  lambdas, and missing or overloaded-ambiguous targets refuse before source/support writes.
- Fixed clean-output regeneration for matcher-only specifications that extend generated support but
  infer no subject methods, constructors, or enum constants.

## 1.0.0-RC1 — 2026-07-10

- Added generated-source hygiene for spec support and Prophecy wrappers: generated support classes
  and `*Prophecy` wrappers live under `target/generated-sources/javaspec` by default, not `src/`.
- Unified Prophecy-style doubles for interfaces and concrete/final classes: `prophesize` accepts
  concrete types, generated wrappers extend `ObjectProphecy<T>`, and `run --generate` can add typed
  `prophesizeFoo()` / `prophecyFoo()` helpers to generated support classes. Java 8 specs can use
  explicit `FooProphecy` variables; Java 10+ specs can use `var` while keeping typed
  `foo.method(...).willReturn(...)` syntax.
- Added standalone `Prophecies` factory under `org.javaspec.doubles.prophecy`; `ObjectBehavior`
  delegates to it as spec convenience rather than owning Prophecy runtime behavior.
- Added optional `javaspec-bytecode-agent` adapter for final-class, static-method, and
  construction-aware doubles, plus `examples/bytecode-agent-basic/`.
- Added CLI run improvements: `--resolve-pom`, `--release <N>`, incremental compilation cache,
  parser SPI/comment-safe source updates, `list-extensions`, and classpath repair hints.
- Added stronger doubles APIs: default interface methods, sequential returns, callback sequences,
  argument captors, ordered verification, and return-then-throw stubbing.
- Added public `Doubles.controlFromHandler(...)` adapter hook so optional adapters avoid direct
  package-private implementation access across isolated Maven/Gradle classloaders.
- Added release notes for the current development line in `docs/release-notes-1.0.0.md`.
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

- Implemented the Java 8-compatible, zero-runtime-dependency javaspec core with CLI describe/run
  workflows, configuration, discovery, generation support, matchers, interface doubles, run
  controls, reports, and programmatic no-JUnit invocation.
- Added standalone optional adapters for Maven, Gradle, and JUnit Platform, all outside the core
  runtime artifact.
- Added aggregate local/CI verification for the core and standalone adapters while preserving the
  zero-runtime-dependency core policy.

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
