# Test and Quality Report

## Post-RC4 constructor-safe restructuring verification

Date: 2026-07-13

The current `develop` restructuring preserves Java behavior while adding canonical constructor type
resolution and an internal Java-only spec-frontend/production-backend seam. Regression coverage now
freezes package-private constructors, generic constructor erasure (including bounded type variables
and compact source), distinct qualified overloads, generation-report no-change behavior, and direct
Java adapter parity.

Verification summary:

- `mvn -q clean verify`: PASS — 862 tests, 0 failures, 0 errors, 0 skipped.
- `scripts/check-version-alignment.sh`: PASS for the RC4 artifact set.
- `scripts/check-current-docs.sh`: PASS with RC-version, generation-report, and migration-link guards.
- `scripts/check-api-surface.sh`: PASS; `io.github.jvmspec.internal.language` remains `INTERNAL`.
- Fedora-container `scripts/verify-all.sh`: PASS including Maven, Gradle 9.6.1, optional adapters,
  dependency audits, and standalone examples.
- Fedora-container `scripts/verify-release-dry-run.sh`: PASS for the aligned RC4 artifact set and
  all external consumer examples.
- Fedora-container `mvn clean verify -Psecurity`: PASS; JaCoCo reported 78.44% lines, 65.58%
  branches, and 95.37% classes; OWASP Dependency-Check 12.2.2 scanned JUnit/Hamcrest with zero
  vulnerabilities and zero scan errors.

The public discovery API remains unchanged while package-private components now own constructor
observations and identity, construction-argument inference, Java expression/type inference, callable
discovery, subject declaration discovery, and example discovery. `SpecDiscovery` is reduced from
roughly 1,700 lines to about 150 lines of deterministic traversal, filtering, and orchestration. The
extraction added direct AST/fallback and declaration tests and corrected legacy generic
method-parameter splitting for types whose generic arguments contain commas. Java inference is now
split between literal/factory classification, expression-argument splitting, source method/import
context, and orchestration; direct tests freeze the deliberate difference between relational angle
brackets in expressions and generic angle brackets in declarations. Method synchronization now
separates Java type-kind eligibility and deterministic method/factory rendering from the
source-preserving updater; direct tests freeze enum, interface, annotation, factory, stub-marker,
and Unicode identifier behavior.

The internal behavior contract now projects portable subject shape, relationships, structured types,
construction/callable signatures, invocation kind, unknown-type evidence, and semantic equivalence
independently of Java body text while retaining the frozen descriptor bridge. No Kotlin or other
language implementation, CLI option, configuration key, dependency, or public SPI is introduced
before 1.0; ADR 0026 records the post-1.0 boundary.

Commit-qualified downstream replay used `1.0.0-RC4-dev-28ba661` (JAR SHA-256
`751a944f8a2c38936e34685426837d34bc4ab5a1374c2414773dbe00ef976d8c`) in
`localhost/magrathea-build:fedora42`:

- a Magrathea `SemanticPolicyHash(Map<String, String>)` generation fixture with two identical
  construction observations generated one constructor, produced the expected meaningful RED and
  pending-stub BROKEN results, then repeated with zero writes and an unchanged source hash;
- the current hand-written Magrathea `SemanticPolicyHash` passed its behavior with zero writes and
  unchanged source hash and mtime;
- `a.Token`/`b.Token` overloads both compiled and passed with zero writes;
- unauthorized constructor synchronization stopped with `PROPOSED`, zero writes, and unchanged hash
  and mtime; authorized synchronization preserved the hand-written body, and its repeat was a
  byte-for-byte and mtime-preserving no-op.

The discovery-component extraction was replayed with `1.0.0-RC4-dev-39fefdd` (JAR SHA-256
`72366df20b1070fdc778fb6c80f73ddcb82292e766609f753d6c17edbab2ebbe`) in the same Fedora
container. Duplicate generic construction evidence again produced exactly one constructor and a
zero-write stable-hash repeat with meaningful RED/BROKEN results. The current Magrathea generic
constructor and qualified overload fixtures passed with zero writes and stable source hashes; the
write-authorization scenario again produced `STOPPED` before authorization, `APPLIED` after explicit
authorization, and an mtime-preserving `NO_CHANGES` repeat.

Callable and subject-declaration extraction was replayed with `1.0.0-RC4-dev-8ac362f` (JAR SHA-256
`b2a66e7d528bbbb8c2dfe759b9597cf898ff943789932cf70f3d48da3b5de110`). A workflow fixture
covering factory construction, proxy return inference, throw targets, subject void calls, setters,
and state expectations generated production and support sources byte-for-byte identical to
`39fefdd`, followed by the expected meaningful RED and pending-stub BROKEN results. Generic
constructor, qualified-overload, authorization, hash, mtime, and zero-write replays also remained
unchanged.

Inference-component extraction was replayed with `1.0.0-RC4-dev-4394868` (JAR SHA-256
`d77586c3450c96a964ad70f8b0d166ea8adf7a1034387f3690425755ccbcedf1`). The callable workflow
fixture again generated production and support sources byte-for-byte identical to `8ac362f`, with
unchanged meaningful RED and pending-stub BROKEN results. Generic construction, current hand-written
generic source, qualified overload, write authorization, body preservation, hash, mtime, and
zero-write repeat evidence also remained unchanged.

Method-rendering extraction was replayed with `1.0.0-RC4-dev-75430b7` (JAR SHA-256
`8310f20bbb4ad87ec38d9af3590e427b4e31fdc43578c97c3ff60e04ca401c91`). The callable workflow
fixture again generated byte-for-byte identical production and support sources with unchanged RED and
BROKEN results. Generic constructors, qualified overloads, denied/authorized synchronization,
hand-written bodies, hashes, mtimes, and no-op repeats also remained unchanged.

## Phase 47 example-data API verification update

Date: 2026-07-09

This update records the first Phase 47 slice: PHPSpec-style example data without Jupiter
parameterized-test syntax. Core now exposes `row(...)`, `examples(...)`, `Example1`, and `Example2`
so one behavior example can execute a small set of concrete rows. Failing rows include row number and
row value context in the assertion message.

Verification summary:

- `mvn -q -Dtest=ObjectBehaviorTest,MainPhase47ExampleDataCliTest test` passed.
- `mvn -q -Dtest=MainPhase47ExampleDataCliTest test` passed after adding JSON/JUnit XML assertions
  for failing-row context.

Current reporting behavior: failing row context is included in pretty/progress output, JSON failure
messages, and JUnit XML failure messages. JSON reports also include first-class `exampleDataRows`
entries with row index, description, status, and detail on the containing example. JUnit XML reports
map recorded example-data rows to testcase entries named with the parent behavior method and row
number so CI output can identify the failing data row directly. The JUnit Platform adapter now
publishes dynamic row descriptors during execution and accepts row unique IDs by selecting the owning
example and filtering published row events to the selected row. Row selectors are descriptor/event
filters because PHPSpec-style rows execute inline inside the owning behavior method.

## Phase 46 PHPSpec compatibility charter verification update

Date: 2026-07-09

This update records completed Phase 46 PHPSpec-first JUnit-parity charter work. The phase now has a
compatibility charter, acceptance-matrix rows, and CLI/Maven/Gradle/JUnit Platform smoke tests for
canonical PHPSpec-style Java authoring: `let()`, `beConstructedWith(...)`, `subject()`, and
`match(...).shouldReturn(...)`.

Verification summary:

- `mvn -q -Dtest=MainPhase46PhpspecCompatibilityCliTest test` passed.
- `mvn -q -f javaspec-maven-plugin/pom.xml -Dtest=JavaspecRunMojoTest#compileTrueRunsCanonicalPhpspecStyleSubjectBehavior test` passed.
- `gradle test --tests io.github.jvmspec.gradle.JavaspecGradlePluginTest.canonicalPhpspecStyleSpecRunsThroughDefaultTestSourceSetRuntimeClasspath` passed.
- `mvn -q -f javaspec-junit-platform-engine/pom.xml -Dtest=JavaspecTestEnginePhase17Test#canonicalPhpspecStyleSpecIsDiscoveredAndExecutedSuccessfully test` passed.
- Full Gradle plugin tests and full JUnit Platform engine tests passed after adding the adapter smoke coverage.

No blockers were reported. Phase 47 is the next roadmap phase.

## Record component evolution hardening verification update

Date: 2026-07-09

This update records the completed follow-up hardening for constructor-driven Java record evolution.
When a spec combines `beConstructedWith(...)` with a record component accessor such as `value()`,
javaspec now evolves the record header instead of failing with `No matching constructor found`.

Verification summary:

- `mvn -q -Dtest=TypeSkeletonGeneratorTest,TypeFileGeneratorTest,ClassConstructorUpdaterTest test`
  passed for focused regression coverage.
- `mvn -q test` passed for the core test suite.
- `mvn -q -DskipTests install` passed for local snapshot installation.
- `./scripts/verify-all.sh` passed after commit `a88fa71`, including version alignment, root verify,
  Maven plugin, JUnit Platform engine, bytecode doubles adapter, bytecode agent adapter, Gradle
  plugin, and standalone examples.

No blockers were reported.

## Phase E documentation verification update

Date: 2026-06-12

This update records completed Phase E documentation for the Prophecy-style doubles API.

Phase E deliverables:

| Item | Status |
|---|---|
| E1 — README Prophecy section | PASS |
| E2 — Example project `examples/prophecy-basic/` | PASS |
| E3 — Migration guide `docs/migration-guide.md` | PASS |
| E4 — ByteBuddy note `docs/bytecode-doubles.md` | PASS |

New documentation and example files:
- `docs/migration-guide.md` — migrating from `Doubles.create()` / `DoubleControl` to Prophecy API
- `docs/bytecode-doubles.md` — concrete-class doubles and Prophecy wrapper usage notes
- `examples/prophecy-basic/pom.xml` — Maven build for prophecy example
- `examples/prophecy-basic/src/main/java/com/example/Mailer.java` — interface to prophesize
- `examples/prophecy-basic/src/test/java/spec/com/example/MailerSpec.java` — spec using prophecy API
- `examples/prophecy-basic/src/test/java/com/example/Verify.java` — standalone verification test

README.md was updated with a "Prophecy-Style Doubles" section covering reflective API, typed
wrapper API, CLI wrapper generation, auto-generation during `javaspec run`, argument matchers,
and auto-check predictions.

`examples/README.md` was updated to include the prophecy-basic example.

All existing tests remain unchanged. No new runtime dependencies were added.

## Finalization documentation and examples verification update

Date: 2026-06-11

This finalization pass synchronized documentation after the completed Phases 30-37 known-limitations
resolution program.

It includes Phase 37 bytecode doubles and `examples/bytecode-doubles-basic/`.
`scripts/verify-examples.sh` now verifies the bytecode doubles example and passed in this turn.

Final verification summary:

- Root: 549 tests, 0 failures, 0 errors, 0 skipped.
- Bytecode adapter: 17 tests, 0 failures, 0 errors, 0 skipped.
- Maven plugin: 30 tests, 0 failures, 0 errors, 0 skipped.
- JUnit Platform engine: 22 tests, 0 failures, 0 errors, 0 skipped.
- Gradle plugin: 32 tests, 0 failures, 0 errors, 0 skipped.
- Examples verification passed, including Maven basic (1), bytecode doubles basic (2), JUnit
  Platform example, and Gradle basic (1).
- Root runtime dependency tree remains only `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT`.
- Bytecode adapter runtime tree includes adapter + core + `net.bytebuddy:byte-buddy:jar:1.14.18`.
- Gradle `runtimeClasspath` remains only `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.

No blockers were reported. Artifacts are published on Maven Central under `io.github.jvmspec`.
The Gradle plugin is published on the Gradle Plugin Portal with plugin id `io.github.jvmspec`.

## Phase 37 verification update

Date: 2026-06-11

This report records completed Phase 37 verification for the standalone optional bytecode doubles
adapter.

Phase 37 behavior and boundaries:

- A new `javaspec-bytecode-doubles` artifact provides concrete-class double creation via ByteBuddy
  subclass generation without touching the zero-runtime-dependency core.
- The core SPI (`ConcreteDoubleProvider`, `ConcreteDoubleRegistry`) was added with no new runtime
  dependencies.
- `Doubles.concreteDouble(Class<T>)` and alias `classDouble(Class<T>)` delegate to a
  `ServiceLoader`-discovered provider, or throw `IllegalStateException` when no provider is
  registered.
- `BytebuddyConcreteDoubleProvider`, in the standalone adapter artifact, uses ByteBuddy subclass
  generation with `InvocationHandlerAdapter`.
- The provider obtains the invocation handler through `Doubles.newDoubleHandler(Class<?>)` and
  returns the handle through `Doubles.assembleFromHandler(Class<T>, T, InvocationHandler)`.
- The provider avoids package-private access problems across plugin and run classloader boundaries
  by using public core APIs for handler creation and handle assembly.
- It rejects interface, final, enum, array, annotation, and primitive types.
- A `META-INF/services` file registers the provider for `ServiceLoader` discovery.
- `scripts/verify-all.sh` was updated to verify and dependency-audit the new adapter module.

All tests pass and no Java 8 compatibility regressions or unintended runtime dependency additions to
the core were reported. No blockers were reported.

## Phase 37 executive summary

- **Core SPI — `ConcreteDoubleProvider` interface**
  - Result: PASS
  - Notes: Public SPI interface added to zero-dependency core; discovered via `ServiceLoader` with
    thread context classloader.
- **Core SPI — `ConcreteDoubleRegistry`**
  - Result: PASS
  - Notes: Package-private internal registry; returns first `supports()`-matching provider or null
    when absent.
- **`Doubles.concreteDouble()` / `classDouble()` API**
  - Result: PASS
  - Notes: Validates type, looks up provider, delegates, or throws `IllegalStateException("No
    ConcreteDoubleProvider is registered...")` when absent.
- **Bytecode adapter artifact — `javaspec-bytecode-doubles`**
  - Result: PASS
  - Notes: Standalone artifact; Java 8, ByteBuddy 1.14.18, javaspec core compile-scope.
- **`supports()` correctness**
  - Result: PASS
  - Notes: Accepts non-final concrete classes only; rejects interface, final, enum, array,
    annotation, and primitive types.
- **`createDouble()` ByteBuddy subclass generation**
  - Result: PASS
  - Notes: Uses ByteBuddy subclass + `InvocationHandlerAdapter`, obtains handlers through
    `Doubles.newDoubleHandler(Class<?>)`, and assembles handles through
    `Doubles.assembleFromHandler(...)`.
- **Stub/verify delegation**
  - Result: PASS
  - Notes: Stub and call-history/verify semantics are provided by the core handler/control assembled
    through `Doubles`.
- **ServiceLoader discovery**
  - Result: PASS
  - Notes: `META-INF/services/io.github.jvmspec.doubles.ConcreteDoubleProvider` registration file
    present; provider loaded automatically when adapter is on classpath.
- **Scripts update**
  - Result: PASS
  - Notes: `scripts/verify-all.sh` updated to include bytecode doubles adapter verify and dependency
    tree audit steps.
- **Runtime compatibility**
  - Result: PASS
  - Notes: Root Maven runtime tree unchanged: `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT` only.
    Adapter runtime tree carries ByteBuddy intentionally as an adapter-scope dependency, not in
    core.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 37 verified commands

```bash
mvn -q test
# PASS: root Surefire 549 tests, 0 failures, 0 errors, 0 skipped

mvn -q -DskipTests install
# PASS

mvn -q -f javaspec-bytecode-doubles/pom.xml verify
# PASS: 17 tests, 0 failures, 0 errors, 0 skipped

mvn -q -f javaspec-maven-plugin/pom.xml verify
# PASS: 30 tests, 0 failures, 0 errors, 0 skipped

mvn -q -f javaspec-junit-platform-engine/pom.xml verify
# PASS: 19 tests, 0 failures, 0 errors, 0 skipped

/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test
# PASS: 32 tests, 0 failures, 0 errors, 0 skipped
```

Runtime dependency audit outputs:

- Root Maven runtime tree: `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT` only.
- Bytecode doubles adapter runtime tree: `io.github.jvmspec:javaspec-bytecode-doubles:jar:0.1.0-SNAPSHOT`
  with `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT:compile` and
  `net.bytebuddy:byte-buddy:jar:1.14.18:compile` (ByteBuddy intentionally included as adapter-scope
  dependency, not in core).
- Gradle `runtimeClasspath`: only `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.

## Phase 36 verification update

Date: 2026-06-11

This report records completed Phase 36 verification for deeper target-profile enforcement.
`ProfileEnforcement` now checks super/extends/implements/permits relationship-type references in
`DescribedType` against the profile catalog. Type-string normalization was improved to handle
annotations, modifiers, type-parameter bounds, intersection bounds, and `$`-spelled nested class
names. `DefaultProfileCatalogSymbols` was broadened with additional Java 11, 17, 21, and 25 types.
Nine new tests were added to `ProfileEnforcementTest` covering relationship-type reference
violations, super type location labeling, intersection bounds, and method type-parameter bounds. All
tests pass and no new runtime dependencies or Java 8 compatibility regressions were reported. No
blockers were reported.

## Phase 36 executive summary

- **Profile enforcement — relationship-type references**
  - Result: PASS
  - Notes: `ProfileEnforcement` now checks super/extends/implements/permits type references in
    `DescribedType` against the profile catalog.
- **Type-string normalization**
  - Result: PASS
  - Notes: Improved to strip annotations, modifiers, type-parameter bounds, intersection bounds, and
    `$`-spelled nested class names.
- **Catalog broadening — Java 11**
  - Result: PASS
  - Notes: Added HTTP Client types: `java.net.http.*`.
- **Catalog broadening — Java 17**
  - Result: PASS
  - Notes: Added `java.time.InstantSource`, `java.util.random.RandomGeneratorFactory`.
- **Catalog broadening — Java 21**
  - Result: PASS
  - Notes: Added virtual-thread `Thread.Builder` variants: `Thread.Builder.OfVirtual`,
    `Thread.Builder.OfPlatform`.
- **Catalog broadening — Java 25**
  - Result: PASS
  - Notes: Added `java.util.stream.Gatherer`, `Gatherer$Downstream`, `Gatherer$Integrator`,
    `Gatherer$Integrator$Greedy`, `java.util.stream.Gatherers`.
- **Tests**
  - Result: PASS
  - Notes: Root core: 544 tests, 0 failures, 0 errors. Maven plugin: 30 tests. JUnit Platform
    engine: 19 tests. Gradle plugin: 32 tests.
- **Runtime compatibility**
  - Result: PASS
  - Notes: No new runtime dependencies; Java 8 compatibility preserved.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 36 verified commands

```bash
mvn -q verify
# PASS: root Surefire 544 tests, 0 failures, 0 errors, 0 skipped

mvn -q -DskipTests install
# PASS

mvn -q -f javaspec-maven-plugin verify
# PASS: 30 tests, 0 failures, 0 errors, 0 skipped

mvn -q -f javaspec-junit-platform-engine verify
# PASS: 19 tests, 0 failures, 0 errors, 0 skipped

/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build
# PASS: 32 tests, 0 failures, 0 errors, 0 skipped
```

Runtime dependency audit outputs:

- Root Maven runtime dependency tree: `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT` only.
- Gradle `runtimeClasspath`: only `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.

## Phase 35 verification update

Date: 2026-06-11

This report records completed Phase 35 verification for additive, dependency-free report enrichment.
JSON reports remain `schemaVersion: 1`, JUnit XML-compatible reports gained additive suite-level
metadata, existing schemaVersion 1 reports without metadata remain valid, and no new runtime
dependencies or Java 8 compatibility regressions were reported. No blockers were reported.

## Phase 35 executive summary

- **Report metadata model**
  - Result: PASS
  - Notes: Added `ReportMetadata` with current/default metadata and deterministic factory overloads.
- **JSON reports**
  - Result: PASS
  - Notes: Still emit `schemaVersion: 1` and add optional top-level `metadata` with `timestamp`,
    `hostname`, `time`, and ordered report properties.
- **JUnit XML-compatible reports**
  - Result: PASS
  - Notes: Root `<testsuite>` adds `timestamp`, `hostname`, and `time`, followed by an immediate
    `<properties>` block; testcase `time="0"` remains because runner models do not carry
    per-example duration.
- **Schema, goldens, and docs**
  - Result: PASS
  - Notes: Updated additively; older schemaVersion 1 reports without metadata still validate.
- **Verification scripts**
  - Result: PASS
  - Notes: Updated to assert dynamic metadata robustly.
- **Runtime compatibility**
  - Result: PASS
  - Notes: No new runtime dependencies; Java 8 compatibility preserved.
- **Verification**
  - Result: PASS
  - Notes: Root, adapter, engine, Gradle plugin, and example verification all passed.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 35 verified commands

```bash
mvn -q verify
# PASS: root Surefire 535 tests, 0 failures, 0 errors, 0 skipped

mvn -q -DskipTests install
# PASS

mvn -q -f javaspec-maven-plugin verify
# PASS: 30 tests, 0 failures, 0 errors, 0 skipped
# Note: tester used this pi-guard-safe Maven-equivalent form; equivalent to -f javaspec-maven-plugin/pom.xml

mvn -q -f javaspec-junit-platform-engine verify
# PASS: 19 tests, 0 failures, 0 errors, 0 skipped

/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build
# PASS: 32 tests, 0 failures, 0 errors, 0 skipped

JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh full
# PASS: examples XML 3 tests, 0 failures, 0 errors, 0 skipped
```

Runtime dependency audit outputs:

- Root Maven runtime dependency tree: `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT` only.
- Maven plugin runtime tree: `io.github.jvmspec:javaspec-maven-plugin:maven-plugin:0.1.0-SNAPSHOT` with
  only `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT:compile`.
- Gradle `runtimeClasspath`: only `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.

## Phase 35 verification details

- Schema/golden sanity: `docs/schemas/run-report-v1.schema.json` and both golden JSON reports parsed
  with Python `json`.
- `jsonschema` 4.26.0 validated both golden JSON reports.
- Aggregate XML count reported by tester: 75 XML files, 619 tests, 0 failures, 0 errors, 0 skipped.

## Phase 34 verification update

Date: 2026-06-11

This report records completed Phase 34 verification for opt-in source/spec compilation in
programmatic invocation and the Maven/Gradle adapters. JUnit Platform was regression-only for Phase
34; no Phase 34 production changes were made there. Existing behavior remains disabled by default,
and Phase 34 intentionally does not add dependency resolution, forked `javac`, source-level
management, or incremental compilation caching. No blockers were reported.

## Phase 34 executive summary

- **Core programmatic API**
  - Result: PASS
  - Notes: Added explicit compilation opt-in through `JavaspecInvocation.withCompilation(...)`,
    exposes `SourceCompilationResult` from `JavaspecInvocationResult`, and throws
    `SourceCompilationException` for unavailable compiler, compilation failure, or I/O errors.
- **Launcher behavior**
  - Result: PASS
  - Notes: Discovers specs first, skips compilation for no-spec runs, compiles before
    bootstrap/examples, and uses a temporary classloader with compilation output first.
- **Maven adapter**
  - Result: PASS
  - Notes: Supports `javaspec.compile`, `javaspec.compileOutput`, and alias
    `javaspec.compileOutputDirectory`; output override implies compilation; default output is
    `target/javaspec-classes`; compilation failures fail before report writes and diagnostics are
    logged.
- **Gradle adapter**
  - Result: PASS
  - Notes: Supports DSL/task/project opt-in `compile` / `javaspec.compile` and `compileOutput` /
    `javaspec.compileOutput`; precedence is task > extension > project property; default output is
    `build/javaspec-classes`; compilation failures fail before report writes and diagnostics are
    visible.
- **JUnit Platform**
  - Result: PASS
  - Notes: Regression-only for Phase 34; `mvn -q -f javaspec-junit-platform-engine/pom.xml verify`
    passed with 19 tests and no failures/errors/skips.
- **Root verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed; root surefire total 528 tests, 0 failures/errors/skips.
- **Adapter verification**
  - Result: PASS
  - Notes: Maven plugin 30 tests, JUnit Platform engine 19 tests, and Gradle plugin 32 tests all
    passed.
- **Runtime dependency audits**
  - Result: PASS
  - Notes: Root runtime tree contains only core `io.github.jvmspec:javaspec`; Maven plugin runtime tree
    contains only the plugin plus core javaspec; Gradle `runtimeClasspath` contains only core
    javaspec.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 34 verified commands

```bash
mvn -q verify
# PASS: 528 tests, 0 failures, 0 errors, 0 skipped

mvn -q -DskipTests install
# PASS

mvn -q -f javaspec-maven-plugin/pom.xml verify
# PASS: 30 tests, 0 failures, 0 errors, 0 skipped

mvn -q -f javaspec-junit-platform-engine/pom.xml verify
# PASS: 19 tests, 0 failures, 0 errors, 0 skipped

/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build
# PASS: 32 tests, 0 failures, 0 errors, 0 skipped
```

Runtime dependency audit outputs:

- Root Maven runtime dependency tree: `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT` only.
- Maven plugin runtime tree: `io.github.jvmspec:javaspec-maven-plugin:maven-plugin:0.1.0-SNAPSHOT` with
  only `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT:compile`.
- Gradle `runtimeClasspath`: only `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.

## Phase 34 verification details

- Root core tests cover invocation model defaults, validation, defensive copies, reset/preservation,
  launcher source-only compile success, bootstrap visibility, no-spec compile skip, compile failure
  exception/diagnostics/no bootstrap, and unchanged default-disabled behavior.
- Maven plugin tests cover opt-in compile success, output override/alias/precedence, no-spec skip,
  compile failure diagnostics, and no report writes on compilation failure.
- Gradle plugin tests cover project/DSL/task opt-in compile success, output override precedence,
  no-spec skip, compile failure diagnostics/no reports, and `runtimeClasspath` remaining core-only.

## Phase 33 verification update

Date: 2026-06-11

This report records completed Phase 33 verification for opt-in `ServiceLoader` discovery of
`BootstrapHook` providers. The Java tester added targeted core, CLI, Maven, Gradle, and JUnit
Platform tests only; no production code or `docs/test-report.md` changes were made by the tester. No
blockers were reported.

## Phase 33 executive summary

- **Tester production modifications**
  - Result: PASS
  - Notes: Tester made no production changes. Production `git status --porcelain` shows only
    implementation-agent changes.
- **Config parser/model coverage**
  - Result: PASS
  - Notes: Covered top-level `bootstrapDiscovery`, aliases, suite-scoped forms, defaults, true/false
    parsing, invalid/blank rejection, effective OR semantics, and value semantics.
- **BootstrapRunner coverage**
  - Result: PASS
  - Notes: Covered explicit-before-discovered ordering, explicit duplicates, discovered class-name
    sorting, disabled discovery, enabled/no-provider no-op, and provider load/execution failure
    wrapping.
- **Invocation/launcher coverage**
  - Result: PASS
  - Notes: Covered `withBootstrapDiscovery(true)`, default false behavior, discovered hook execution
    before examples, and bootstrap failure preventing examples.
- **CLI coverage**
  - Result: PASS
  - Notes: Covered config-enabled discovery, failure exit `64` with `Error: Bootstrap execution
    failed`, no report writes on failure, and verbose `Bootstrap discovery: true` only when enabled.
- **Maven adapter coverage**
  - Result: PASS
  - Notes: Covered config opt-in, `javaspec.bootstrapDiscovery` property opt-in, default false, and
    invalid property diagnostics.
- **Gradle adapter coverage**
  - Result: PASS
  - Notes: Covered DSL, task, and project-property opt-ins, default false, and invalid
    project-property diagnostics.
- **JUnit Platform coverage**
  - Result: PASS
  - Notes: Covered `javaspec.bootstrapDiscovery=true`, absent/false behavior, and invalid parameter
    diagnostics.
- **Root verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed; root tests `506 -> 521`, with `521` tests and `0`
    failures/errors/skipped.
- **Adapter verification**
  - Result: PASS
  - Notes: Maven plugin `26` tests, JUnit Platform engine `19` tests, Gradle plugin `28` tests all
    passed.
- **Runtime dependency audits**
  - Result: PASS
  - Notes: Root runtime tree has no external runtime dependencies; Maven plugin runtime tree
    contains only core javaspec; Gradle runtimeClasspath contains only core javaspec.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 33 verified commands

```bash
mvn -q verify
# PASS: 521 tests, 0 failures, 0 errors, 0 skipped

mvn -q -DskipTests install
# PASS

mvn -q -f javaspec-maven-plugin/pom.xml verify
# PASS: 26 tests

mvn -q -f javaspec-junit-platform-engine/pom.xml verify
# PASS: 19 tests

/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build
# PASS: 28 tests; Java 8 obsolete source/target warnings only

mvn dependency:tree -Dscope=runtime
# PASS: io.github.jvmspec:javaspec only

mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
# PASS: io.github.jvmspec:javaspec-maven-plugin -> io.github.jvmspec:javaspec only

mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
# PASS: expected core + JUnit Platform engine runtime dependencies

/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
# PASS: io.github.jvmspec:javaspec:0.1.0-SNAPSHOT only

git status --porcelain \
  src/main/java \
  javaspec-maven-plugin/src/main/java \
  javaspec-gradle-plugin/src/main/java \
  javaspec-junit-platform-engine/src/main/java
# PASS: only implementation-agent production changes present; no tester production changes
```

## Phase 33 verification details

- Added `src/test/java/org/javaspec/cli/MainPhase33BootstrapDiscoveryCliTest.java` with 3 CLI
  integration tests.
- Extended root tests:
  - `JavaspecConfigurationParserTest`: +2 tests.
  - `JavaspecConfigurationTest`: +1 test.
  - `BootstrapRunnerTest`: +5 tests.
  - `JavaspecInvocationTest`: +1 test.
  - `JavaspecLauncherBootstrapHookTest`: +3 tests.
- Extended adapter tests:
  - `javaspec-maven-plugin/src/test/java/org/javaspec/maven/JavaspecRunMojoTest.java`: +4 tests.
  - `javaspec-gradle-plugin/src/test/java/org/javaspec/gradle/JavaspecGradlePluginTest.java`: +3
    tests.
  - `javaspec-junit-platform-engine/src/test/java/org/javaspec/junit/platform/JavaspecTestEnginePhase17Test.java`:
    +3 tests.
- Total targeted Phase 33 test additions: 25 test methods.
- All tests follow existing Java 8/JUnit style per module.

## Phase 32 verification update

Date: 2026-06-11

This report records the completed Phase 32 verification results for configuration-driven extension
activation and adapter formatter controls. The Java tester added targeted core, CLI, Maven, Gradle,
and JUnit Platform tests only; no production code or `docs/test-report.md` changes were made by the
tester. No blockers were reported.

## Phase 32 executive summary

- **Tester production modifications**
  - Result: PASS
  - Notes: Tester made no production changes. Production `git status --porcelain` shows only
    implementation-agent changes, including `JavaspecExtensionActivator.java` and Phase 32
    adapter/core files.
- **Config parser/model coverage**
  - Result: PASS
  - Notes: Top-level `extensions`/`extension`, suite-scoped extensions, order, duplicates, blank
    rejection, immutability, and value semantics covered.
- **Activator/loader coverage**
  - Result: PASS
  - Notes: Supplied classloader loading, configured order, duplicate preservation, `Extension`
    alias, wrong type, missing class diagnostics, no public no-arg constructor, configure failure,
    custom formatter registration, and empty-list ServiceLoader regression covered.
- **Invocation/launcher coverage**
  - Result: PASS
  - Notes: `withExtensions`, registry exposure, activation before examples, and activation failure
    before example execution covered.
- **CLI coverage**
  - Result: PASS
  - Notes: Configured extension formatter selection and activation failure exit `64` before reports
    covered.
- **Maven adapter coverage**
  - Result: PASS
  - Notes: Formatter parameter precedence, configured + suite + `javaspec.extensions` ordering,
    extension formatter output, invalid formatter names, and activation failure/no reports covered.
- **Gradle adapter coverage**
  - Result: PASS
  - Notes: DSL extensions, project-property extensions, extension formatter output, activation
    failure/no reports, and runtimeClasspath core-only declaration covered.
- **JUnit Platform coverage**
  - Result: PASS
  - Notes: `javaspec.extensions`, semicolon-delimited values, `javaspec.formatter`, extension
    formatter output, invalid formatter diagnostics, and activation failure `JUnitException`
    diagnostics covered.
- **Root verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed; root tests inferred `488 -> 506`, with `506` tests and `0`
    failures/errors/skipped after Phase 32 tests.
- **Adapter verification**
  - Result: PASS
  - Notes: Maven plugin `22` tests, JUnit Platform engine `16` tests, Gradle plugin `25` tests all
    passed.
- **Runtime dependency audits**
  - Result: PASS
  - Notes: Root runtime tree has no external runtime dependencies; Maven plugin runtime tree
    contains only core javaspec; Gradle runtimeClasspath contains only core javaspec.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 32 verified commands

```bash
mvn -q verify
# PASS: 506 tests, 0 failures, 0 errors, 0 skipped

mvn -q -DskipTests install
# PASS

mvn -q -f javaspec-maven-plugin/pom.xml verify
# PASS: 22 tests

mvn -q -f javaspec-junit-platform-engine/pom.xml verify
# PASS: 16 tests

cd javaspec-gradle-plugin && /tmp/gradle-8.8/bin/gradle clean test build
# PASS: 25 tests; Java 8 obsolete source/target warnings only

mvn dependency:tree -Dscope=runtime
# PASS: io.github.jvmspec:javaspec only

mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
# PASS: io.github.jvmspec:javaspec-maven-plugin -> io.github.jvmspec:javaspec only

mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
# PASS: expected core + JUnit Platform engine runtime dependencies

cd javaspec-gradle-plugin && \
  /tmp/gradle-8.8/bin/gradle dependencies --configuration runtimeClasspath
# PASS: io.github.jvmspec:javaspec:0.1.0-SNAPSHOT only

git status --porcelain \
  src/main/java \
  javaspec-maven-plugin/src/main/java \
  javaspec-gradle-plugin/src/main/java \
  javaspec-junit-platform-engine/src/main/java
# PASS: only implementation-agent production changes present; no tester production changes
```

## Phase 32 verification details

- Added `JavaspecExtensionActivatorTest` with five JUnit 4 tests for configured extension activation
  semantics and diagnostics.
- Added or extended root tests across configuration parsing/model, extension loader, invocation,
  launcher, and CLI integration for configuration-driven formatter registration and failure paths.
- Extended Maven plugin tests with four JUnit 4 cases covering formatter override precedence,
  extension ordering, custom formatter availability, invalid formatter diagnostics, and activation
  failure before report writes.
- Extended Gradle plugin tests with four JUnit 4/TestKit cases covering DSL extensions,
  project-property extensions, activation failure before reports, and runtimeClasspath core-only
  declaration.
- Extended JUnit Platform engine tests with three JUnit Jupiter cases covering configuration
  parameter extension activation, formatter selection/output, invalid formatter diagnostics, and
  activation failure diagnostics.
- All tests follow existing Java 8 conventions and adapter-specific test styles.

## Phase 31 verification update

Date: 2026-06-10

This report records the completed Phase 31 verification results provided by the Java tester for
PLAN.md Phase 31 / ADR 0023 (superseding the deferral half of ADR 0009) — source-preserving updates
of existing sealed interfaces in `io.github.jvmspec.generation.ClassMethodUpdater`. The tester made no
production changes; the only `src/main/java` changes are the implementation agent's
`ClassMethodUpdater.java` (Phase 31) and `Matchable.java` (Phase 30). The obsolete ADR-0009 no-op
skip test was replaced with Phase 31 behavior tests. No blockers were reported.

## Phase 31 executive summary

- **Tester production modifications**
  - Result: PASS
  - Notes: Tester made no production changes; `git status --porcelain src/main/java` shows only the
    implementation agent's `ClassMethodUpdater.java` and the Phase 30 `Matchable.java`.
- **Obsolete test replacement**
  - Result: PASS
  - Notes:
    `ClassMethodUpdaterTest.skipsSealedInterfaceExistingSourceUpdatesEvenWhenMethodsWouldBeMissing`
    (asserting the superseded ADR-0009 no-op skip) replaced by
    `updatesSealedInterfaceExistingSourceWhenMethodsAreMissing`.
- **New test class**
  - Result: PASS
  - Notes: `src/test/java/org/javaspec/generation/ClassMethodUpdaterSealedInterfaceTest.java` with
    10 JUnit 4 tests following existing fixture conventions.
- **Sealed root insertion**
  - Result: PASS
  - Notes: Missing non-static method declarations (ending `;`) are inserted into the sealed root
    body; static descriptors are never inserted.
- **Nested permitted class bodies**
  - Result: PASS
  - Notes: In-file nested permitted class implementations (named in the permits clause or described
    permitted type names) receive `public` method bodies with Java default returns; existing
    implementations are preserved.
- **Generated default `Permitted` implementation**
  - Result: PASS
  - Notes: A nested `Permitted` class receives bodies even when not named in the permits clause or
    described permitted type names.
- **Nested permitted interface declarations**
  - Result: PASS
  - Notes: Nested permitted interfaces receive declarations (`;`), never bodies or default returns.
- **Per-scope de-duplication**
  - Result: PASS
  - Notes: A method declared only in the root is added only to nested bodies, and a method
    implemented only in a nested body is added only to the root; second pass is a no-op.
- **Out-of-file permitted types**
  - Result: PASS
  - Notes: A permits clause referencing a type not declared in the file succeeds deterministically:
    root and in-file nested scopes updated, the out-of-file name remains a permits-clause mention
    only.
- **Idempotency and no-rewrite**
  - Result: PASS
  - Notes: Double `updateSource` yields identical output; a complete sealed interface returns
    unchanged source and `updateFile` does not rewrite the file (last-modified timestamp preserved).
- **`hasMissingMethods` consistency**
  - Result: PASS
  - Notes: Returns true before update, false after update and for complete sealed-interface sources.
- **Other type-kind regression**
  - Result: PASS
  - Notes: All pre-existing `ClassMethodUpdaterTest` tests for CLASS, INTERFACE, ANNOTATION, and
    static factories pass unchanged.
- **Root verification**
  - Result: PASS
  - Notes: Root `mvn -q verify` passed with totals 478 (1 failure, obsolete skip test) → 488 tests
    (0 failures/errors/skipped), including the runtime dependency enforcer.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 31 verified commands

```bash
mvn -q surefire:test -Dtest=ClassMethodUpdaterTest
# Baseline: 7 tests, 1 failure (obsolete ADR-0009 skip test); suite baseline 478 tests, 1 failure
mvn -q test-compile surefire:test -Dtest='ClassMethodUpdaterTest,ClassMethodUpdaterSealedInterfaceTest'
# 17 tests, 0 failures
mvn -q verify
# After: 488 tests, 0 failures, 0 errors, 0 skipped; runtime dependency enforcer passed
git status --porcelain src/main/java
```

## Phase 31 verification details

- The 10 new test methods are:
  `insertsMissingNonStaticDeclarationsIntoSealedRootBodyAndSkipsStaticDescriptors`,
  `insertsDefaultReturnBodiesIntoNestedPermittedClassImplementations`,
  `insertsBodiesIntoGeneratedDefaultNestedPermittedImplementation`,
  `insertsDeclarationsNotBodiesIntoNestedPermittedInterface`,
  `deduplicatesSignaturesPerScopeAcrossRootAndNestedImplementations`,
  `leavesOutOfFilePermittedTypesUntouchedAndUpdatesInFileScopesDeterministically`,
  `updateSourceIsIdempotentAndLeavesCompleteSealedInterfaceUnchanged`,
  `updateFileWritesUpdatedSealedInterfaceSource`,
  `updateFileDoesNotRewriteSealedInterfaceFileWithoutMissingMethods`, and
  `hasMissingMethodsIsConsistentForSealedInterfacesBeforeAndAfterUpdate`.
- The replacement test in `ClassMethodUpdaterTest`,
  `updatesSealedInterfaceExistingSourceWhenMethodsAreMissing`, reuses the old skip-test fixture
  (single-line nested `{ }` body) and asserts root declarations, nested bodies, idempotency, and
  `hasMissingMethods` true-before/false-after.
- File-based assertions use `TemporaryFolder`; the no-rewrite check pins the file's last-modified
  timestamp before `updateFile` and asserts it is unchanged afterward.
- The test classes follow JUnit 4.13.2 / Java 8 / English-comment conventions matching the existing
  `ClassMethodUpdaterTest`.
- De-duplication note: ordinary INTERFACE update regression is already covered by the existing
  `insertsMissingInterfaceDeclarationsAndDoesNotDuplicateOnSecondPass` test, which passes unchanged;
  no duplicate regression test was added.

## Phase 30 verification update

Date: 2026-06-10

This report records the completed Phase 30 verification results provided by the Java tester for
PLAN.md Phase 30 / ADR 0023 item 1 — bounded generic `Iterable` matcher checks in
`io.github.jvmspec.matcher.Matchable`. The tester made no production changes; the only `src/main/java`
change is the implementation agent's `Matchable.java`. No blockers were reported.

## Phase 30 executive summary

- **Tester production modifications**
  - Result: PASS
  - Notes: Tester made no production changes; the only `src/main/java` change is the implementation
    agent's `Matchable.java`.
- **New test class**
  - Result: PASS
  - Notes: `src/test/java/org/javaspec/matcher/MatchableBoundedIterableTest.java` with 10 JUnit 4
    tests, timeout-guarded.
- **Infinite-iterable safety**
  - Result: PASS
  - Notes: Empty check fails fast with "at least one element", not-empty check passes, count check
    fails fast with "more than 3 elements".
- **Bounded consumption proof**
  - Result: PASS
  - Notes: ≤ 4 elements consumed for count 3; ≤ 1 `hasNext()` / 0 `next()` for emptiness checks.
- **Finite generic-iterable regression**
  - Result: PASS
  - Notes: Exact count passes; undercount reports "count 3 ... got 2"; overcount reports "more than
    1 elements".
- **Null handling**
  - Result: PASS
  - Notes: "Expected a countable value but got null" for all three methods, newly covered.
- **Collection/Map/CharSequence/array regression**
  - Result: PASS
  - Notes: Covered by existing unchanged `MatchableTest`.
- **Targeted tests**
  - Result: PASS
  - Notes: Targeted tests pass.
- **Root verification**
  - Result: PASS
  - Notes: Root `mvn -q verify` passed with totals 468 → 478 tests (0 failures/errors/skipped both
    runs), including the runtime dependency enforcer.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 30 verified commands

```bash
mvn -q verify
# Baseline: 468 tests
mvn -q -Dtest=MatchableBoundedIterableTest,MatchableTest test
mvn -q verify
# After: 478 tests
git status --porcelain src/main/java
```

## Phase 30 verification details

- The 10 test methods are: `shouldBeEmptyFailsFastOnInfiniteIterable`,
  `shouldNotBeEmptyPassesOnInfiniteIterableWithoutHanging`,
  `shouldHaveCountFailsFastOnInfiniteIterable`,
  `shouldHaveCountConsumesAtMostExpectedPlusOneElements`, `emptinessChecksConsumeAtMostOneProbe`,
  `shouldHaveCountPassesForExactFiniteGenericIterable`,
  `shouldHaveCountReportsActualCountForUndercountedFiniteGenericIterable`,
  `shouldHaveCountFailsFastForOvercountedFiniteGenericIterable`,
  `shouldBeEmptyFailureOnFiniteGenericIterableMentionsAtLeastOneElement`, and
  `nullSubjectStillFailsCountAndEmptinessChecksWithCountableMessage`.
- The tests use an infinite-iterable fixture and a `CountingIterable` consumption-counting wrapper.
- The test class follows JUnit 4.13.2 / Java 8 / English-comment conventions matching
  `MatchableTest`.
- De-duplication note: existing `MatchableTest` count/emptiness tests already cover
  Collection/Map/CharSequence/array and finite pass cases and still pass unchanged.

## Remote GitHub Actions status update after Phase 20/21/22 push

Date: 2026-06-04

After Phase 20/21/22 were pushed, the user/maintainer reported GitHub Actions green. This report
records that as user-/maintainer-confirmed remote GitHub Actions success for HEAD `5088e96` on
`develop`. This environment did not independently query GitHub, so no run IDs, URLs, durations, or
logs are recorded. The workflow scope remains `.github/workflows/ci.yml`: a Java 8/11/17/21/25 core
matrix and Java 21 full verification through `scripts/verify-all.sh`, including examples by default
unless explicitly skipped. Publishing is complete; no publish, deploy, signing, secrets,
portal credentials, final release version/tag, or final publish approval are claimed.

## Phase 22 verification update

Date: 2026-06-04

This report records the completed Phase 22 verification results provided by the Java tester for
explicit skipped/pending semantics. Phase 22 added zero-dependency public skip/pending annotations,
unchecked skip/pending signals, `ObjectBehavior` helpers, distinct `PENDING` result status/counts,
pending-aware formatter/report/adapter behavior, and schema/golden documentation updates. No
production Java changes were made by the tester, no blockers were reported, and remote GitHub
Actions success for HEAD `5088e96` on `develop` is user-/maintainer-confirmed after the Phase
20/21/22 push.

## Phase 22 executive summary

- **Tester production Java modifications**
  - Result: PASS
  - Notes: Tester reported no production Java changes made by tester.
- **Public API coverage**
  - Result: PASS
  - Notes: Coverage includes `@Skip`, `@Pending`, `SkipExampleException`, `PendingExampleException`,
    `ObjectBehavior.skip(...)`, and `ObjectBehavior.pending(...)` messages/reasons.
- **Annotation semantics**
  - Result: PASS
  - Notes: Defaults, `value()`, `reason()`, `reason()` precedence, `@Skip` precedence over
    `@Pending`, and annotation no-instantiation/no-lifecycle/body execution are covered.
- **Runtime signal semantics**
  - Result: PASS
  - Notes: Runtime skip/pending from example methods, `ObjectBehavior` helpers, and `let()` are
    covered; successful `letGo()` preserves SKIPPED/PENDING, while `letGo()` failure after a signal
    is BROKEN.
- **Result counts and exits**
  - Result: PASS
  - Notes: `PENDING` is distinct from `SKIPPED`; `skippedCount()` remains skipped-only;
    `pendingCount()` is separate; skipped-plus-pending helpers exist for JUnit XML use; runs with
    only passed/skipped/pending examples remain successful.
- **Formatter/CLI/report behavior**
  - Result: PASS
  - Notes: Built-in summaries include pending counts and print pending examples; skipped examples
    are printed deterministically; JSON reports include pending counts and `status: "PENDING"`;
    JUnit XML-compatible reports map skipped and pending to `<skipped>` with skipped attribute
    including both.
- **Maven/Gradle adapters**
  - Result: PASS
  - Notes: Maven summary logging includes `pending=`; Gradle inherits pending summary/report
    behavior through core formatter/reporters.
- **JUnit Platform adapter**
  - Result: PASS
  - Notes: Pending maps to `executionSkipped` with `Pending:` reason; unique IDs/descriptors are
    unchanged.
- **Targeted changed tests**
  - Result: PASS
  - Notes: 78 tests passed.
- **Root tests**
  - Result: PASS
  - Notes: `mvn -q test`: 399 tests passed.
- **Root verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed.
- **Root runtime dependency audit**
  - Result: PASS
  - Notes: `mvn dependency:tree -Dscope=runtime`: root has no runtime dependencies beyond itself.
- **Core install**
  - Result: PASS
  - Notes: `mvn -q -DskipTests install` passed.
- **Maven plugin verification**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-maven-plugin/pom.xml verify`: 13 tests passed. Runtime tree:
    `io.github.jvmspec:javaspec` only.
- **JUnit Platform engine verification**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-junit-platform-engine/pom.xml verify`: 13 tests passed. Runtime tree:
    `io.github.jvmspec:javaspec`, `junit-platform-engine`, `opentest4j`, `junit-platform-commons`,
    `apiguardian-api`.
- **Gradle plugin verification**
  - Result: PASS
  - Notes: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build`: 12 tests passed;
    Java 8 obsolete source/target warnings only. Runtime classpath: `io.github.jvmspec:javaspec` only.
- **Examples verification**
  - Result: PASS
  - Notes: `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh` passed.
- **Aggregate verification**
  - Result: PASS
  - Notes: `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed.
- **Remote CI execution**
  - Result: PASS (user-/maintainer-confirmed)
  - Notes: After Phase 20/21/22 were pushed, the user/maintainer confirmed remote GitHub Actions
    success for HEAD `5088e96` on `develop`; no run IDs, URLs, durations, or logs were independently
    queried here.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 22 verified commands

```bash
# Targeted changed tests across core, CLI, reporting, invocation, Maven plugin, Gradle plugin, and JUnit Platform engine: 78 tests passed
mvn -q test
mvn -q verify
mvn dependency:tree -Dscope=runtime
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh
JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh
```

## Phase 22 verification details

- Phase 22 implements explicit skipped/pending semantics while preserving the
  zero-runtime-dependency policy.
- Tester verification covered updated tests across core, CLI, reporting, invocation, Maven plugin,
  Gradle plugin, and JUnit Platform engine areas.
- New public API markers under `io.github.jvmspec.api` are runtime method annotations `@Skip` and
  `@Pending`, unchecked signals `SkipExampleException` and `PendingExampleException`, and
  `ObjectBehavior.skip()`/`skip(String)`/`pending()`/`pending(String)` helpers.
- `@Skip` takes precedence over `@Pending` when both annotations are present.
- Annotation-based skip/pending does not instantiate the spec and does not run `let()`, the example
  body, or `letGo()`.
- Runtime skip/pending signals from `let()` or an example mark the example SKIPPED/PENDING after
  successful `letGo()`; `letGo()` failure after a skip/pending signal is BROKEN.
- `ExampleStatus.PENDING` is distinct from `SKIPPED`; skipped-only and pending-only counts are
  separate, with skipped-plus-pending helpers for JUnit XML-compatible reports.
- Exit code remains success when only passed/skipped/pending examples exist.
- JSON reports add `pending` counts to run/spec summaries and use `status: "PENDING"` for pending
  examples while preserving existing fields.
- JUnit XML-compatible reports map both skipped and pending examples to `<skipped>` and include both
  in the testsuite `skipped` attribute. Pending messages use `Pending: <reason>` or `Pending by
  javaspec.`.
- Maven plugin summary logging includes `pending=`; Gradle plugin behavior is inherited through core
  formatter/reporters.
- JUnit Platform pending examples map to `executionSkipped` with `Pending:` reason and unchanged
  unique IDs/descriptors.
- Remote GitHub Actions success for HEAD `5088e96` on `develop` is user-/maintainer-confirmed after
  the Phase 20/21/22 push; no run IDs, URLs, durations, or logs were independently queried here.
- Public publishing is complete. Artifacts are published on Maven Central under
  `io.github.jvmspec`. The Gradle plugin is published on the Gradle Plugin Portal with plugin id
  `io.github.jvmspec`.

## Phase 22 dependency summary

- **Core runtime**: No external runtime dependencies; root runtime tree contains only
  `io.github.jvmspec:javaspec`.
- **Maven plugin runtime**: `io.github.jvmspec:javaspec` only.
- **JUnit Platform engine runtime**: `io.github.jvmspec:javaspec`, `junit-platform-engine`, `opentest4j`,
  `junit-platform-commons`, `apiguardian-api`.
- **Gradle plugin runtime**: `io.github.jvmspec:javaspec` only.

## Phase 21 verification update

Date: 2026-06-04

This report records the completed Phase 21 verification results provided by the Java tester for
adoption assets: report schema documentation, golden report examples, standalone consumer examples,
`scripts/verify-examples.sh`, and the `scripts/verify-all.sh` examples-by-default update. Phase 21
did not change core production/test Java files, did not add runtime dependencies, and did not run
publish/deploy/signing commands. After Phase 20/21/22 were pushed, remote GitHub Actions success for
HEAD `5088e96` on `develop` was user-/maintainer-confirmed.

## Phase 21 executive summary

- **Tester file modifications**
  - Result: PASS
  - Notes: Tester reported no file modifications.
- **Publish/deploy/signing commands**
  - Result: PASS
  - Notes: Tester reported no publish/deploy/signing commands were run.
- **Core Java source/test changes**
  - Result: PASS
  - Notes: `git diff --name-only HEAD -- src/main/java src/test/java` was empty.
- **Whitespace checks**
  - Result: PASS
  - Notes: `git diff --check` and custom whitespace/EOF checks over visible untracked plus modified
    text files passed.
- **Script syntax**
  - Result: PASS
  - Notes: `bash -n scripts/verify-examples.sh`, `bash -n scripts/verify-all.sh`, and `bash -n
    scripts/check-version-alignment.sh` passed.
- **Script executable bits**
  - Result: PASS
  - Notes: `scripts/check-version-alignment.sh`, `scripts/verify-all.sh`, and
    `scripts/verify-examples.sh` all have mode `755`.
- **Generated example output ignore rules**
  - Result: PASS
  - Notes: Generated `examples/**/target`, `examples/**/build`, and `examples/**/.gradle` outputs
    are ignored and absent from `git ls-files --others --exclude-standard`.
- **JSON schema and golden JSON**
  - Result: PASS
  - Notes: Schema JSON and golden JSON parsed; `jsonschema` validated
    `docs/examples/reports/passing-run-report-v1.json` against
    `docs/schemas/run-report-v1.schema.json`.
- **Golden JUnit XML-compatible report**
  - Result: PASS
  - Notes: XML parsed with root `testsuite`, `tests=1`, and `failures=0`.
- **Golden structural sanity**
  - Result: PASS
  - Notes: `schemaVersion=1`, one spec, one example, stable ids, source file, and `line=11` were
    present.
- **Standalone examples verification**
  - Result: PASS
  - Notes: `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh` passed.
- **Aggregate verification with examples**
  - Result: PASS
  - Notes: `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed and
    included `==> Standalone examples verification` plus `PASS: standalone examples verification
    completed.`
- **Core tests through aggregate script**
  - Result: PASS
  - Notes: Root/core verification reported 386 tests, 0 failures, 0 errors, 0 skipped.
- **Standalone adapters through aggregate script**
  - Result: PASS
  - Notes: Maven plugin: 12 tests pass; JUnit Platform engine: 12 tests pass; Gradle plugin: 11
    tests pass.
- **JUnit Platform example**
  - Result: PASS
  - Notes: 1 example test passed through Surefire/JUnit Platform.
- **Dependency summaries**
  - Result: PASS
  - Notes: Root runtime tree only `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT`; root dependency/build
    files unchanged; example runtime checks reported no main runtime dependencies for Maven/JUnit
    examples and no Gradle example runtimeClasspath dependencies.
- **Remote CI execution**
  - Result: PASS (user-/maintainer-confirmed)
  - Notes: After Phase 20/21/22 were pushed, the user/maintainer confirmed remote GitHub Actions
    success for HEAD `5088e96` on `develop`; no run IDs, URLs, durations, or logs were independently
    queried here.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 21 verified commands

```bash
git diff --name-only HEAD -- src/main/java src/test/java
git diff --check
# Custom whitespace/EOF check over visible untracked + modified text files
bash -n scripts/verify-examples.sh
bash -n scripts/verify-all.sh
bash -n scripts/check-version-alignment.sh
# Script mode checks for scripts/check-version-alignment.sh, scripts/verify-all.sh, scripts/verify-examples.sh
# Ignore verification for generated examples/**/target, examples/**/build, examples/**/.gradle outputs
# Schema JSON parse, golden JSON parse, JSON Schema validation when jsonschema was available
# Golden XML parse and structural sanity checks
JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh
JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh
```

Commands covered by the passing standalone examples run:

```bash
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml -DskipTests install
mvn -q -f examples/maven-basic/pom.xml verify
mvn -q -f examples/junit-platform-basic/pom.xml test
/tmp/gradle-8.8/bin/gradle -p examples/gradle-basic clean javaspecRun
```

## Phase 21 verification details

- Phase 21 is an adoption-assets increment only.
- `docs/schemas/run-report-v1.schema.json` documents schemaVersion 1 JSON run reports.
- `docs/examples/reports/passing-run-report-v1.json` and
  `docs/examples/reports/passing-junit-report.xml` are golden passing report examples.
- `examples/maven-basic/`, `examples/gradle-basic/`, and `examples/junit-platform-basic/` are
  standalone consumer examples, not root modules and not part of the repository-root Maven reactor.
- `scripts/verify-examples.sh` installs local snapshots needed by examples, verifies the Maven
  plugin example, JUnit Platform engine example, and Gradle plugin example, and asserts generated
  report markers: `schemaVersion`, stable id `spec.com.example.CalculatorSpec#it_adds_two_numbers`,
  `PASSED`, and `line=11`.
- The Maven example generated `target/javaspec/run-report.json` and
  `target/javaspec/junit-report.xml` with the expected markers.
- The JUnit Platform example generated Surefire reports mentioning `spec.com.example.CalculatorSpec`
  and `it_adds_two_numbers`.
- The Gradle example generated `build/reports/javaspec/run-report.json` and
  `build/reports/javaspec/junit-report.xml` with the expected markers.
- `scripts/verify-examples.sh` supports `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, and
  `JAVASPEC_SKIP_GRADLE_EXAMPLE=1`.
- `scripts/verify-all.sh` runs standalone examples by default after core/adapters;
  `JAVASPEC_SKIP_EXAMPLES=1` is the explicit all-examples opt-out.
- Remote GitHub Actions success for HEAD `5088e96` on `develop` is user-/maintainer-confirmed after
  the Phase 20/21/22 push; no run IDs, URLs, durations, or logs were independently queried here.
- No public publishing, deployment, signing, secrets, runtime dependency additions, or Maven
  multi-module conversion is part of this increment.

## Phase 21 dependency summary

- **Core runtime**: No external runtime dependencies; root runtime tree contains only
  `io.github.jvmspec:javaspec`.
- **Example Maven project**: No main runtime dependencies reported.
- **Example JUnit Platform project**: No main runtime dependencies reported.
- **Example Gradle project**: `runtimeClasspath` has no dependencies.
- **Existing standalone adapters**: Maven plugin, JUnit Platform engine, and Gradle plugin
  dependency summaries remain as in Phase 20/Phase 19 aggregate verification.

## Phase 20 verification update

Date: 2026-06-04

This report records the completed Phase 20 verification results provided by the Java tester for
release-readiness scaffolding after license and maintainer metadata were confirmed. Phase 20 added
version-alignment checking, changelog/releasing documentation, MIT license and confirmed
maintainer/developer metadata, safe publication metadata, and local source/javadoc artifact
readiness checks only. It did not run or configure publish/deploy/signing/portal credential
commands, add secrets, add runtime dependencies, configure Central Portal publication, configure
Gradle Plugin Portal publication/credentials, choose a final release version/tag, approve final
publishing, or convert the repository to Maven multi-module. Phase 19 release/CI hardening evidence
and earlier evidence remain below.

## Phase 20 executive summary

- **Tester file modifications**
  - Result: PASS
  - Notes: Tester reported no file modifications.
- **Publish/deploy/signing commands**
  - Result: PASS
  - Notes: Tester reported no publish/deploy/signing commands were run.
- **License file**
  - Result: PASS
  - Notes: `LICENSE` is identical to `origin/main:LICENSE`, blob
    `b990d5492f3ef404ffc145890b83e51914351bb5`.
- **Script syntax/executable bits**
  - Result: PASS
  - Notes: `bash -n scripts/check-version-alignment.sh`, `bash -n scripts/verify-all.sh`, and
    combined/individual syntax checks passed; both scripts are executable (`-rwxr-xr-x`).
- **Version alignment**
  - Result: PASS
  - Notes: `bash scripts/check-version-alignment.sh` passed; root Maven, Maven plugin, JUnit engine,
    Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` are aligned at
    `0.1.0-SNAPSHOT`.
- **Whitespace checks**
  - Result: PASS
  - Notes: `git diff --check`, `git diff --cached --check`, and untracked whitespace checks passed.
- **Effective POM generation**
  - Result: PASS
  - Notes: Effective POM generation passed for root, Maven plugin, and JUnit engine.
- **Maven POM metadata**
  - Result: PASS
  - Notes: Root, Maven plugin, and JUnit engine POM metadata contain MIT License, URL
    `https://opensource.org/licenses/MIT`, distribution `repo`, Mario Giustiniani email, and
    maintainer role.
- **Gradle generated POM metadata**
  - Result: PASS
  - Notes: `pluginMaven` and `javaspecPluginMarkerMaven` generated POMs include MIT license and
    maintainer metadata.
- **Root core verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed; 386 tests, 0 failures, 0 errors, 0 skipped.
- **Root runtime dependency audit**
  - Result: PASS
  - Notes: `mvn dependency:tree -Dscope=runtime` passed; root runtime has no dependencies beyond
    `io.github.jvmspec:javaspec`.
- **Root release artifact check**
  - Result: PASS
  - Notes: `mvn -q -Prelease-artifacts -DskipTests package` passed; root main, sources, and javadoc
    jars were non-empty.
- **Core install for standalone adapters**
  - Result: PASS
  - Notes: `mvn -q -DskipTests install` passed.
- **Maven plugin release artifact check**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package` passed;
    main, sources, and javadoc jars were non-empty.
- **JUnit Platform engine release artifact check**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests
    package` passed; main, sources, and javadoc jars were non-empty.
- **Standalone Maven plugin verification**
  - Result: PASS
  - Notes: `mvn -q verify` for the standalone Maven plugin passed with 12 tests.
- **Standalone JUnit engine verification**
  - Result: PASS
  - Notes: `mvn -q verify` for the standalone JUnit engine passed with 12 tests.
- **Gradle plugin POM generation/build/artifacts**
  - Result: PASS
  - Notes: Gradle publication POM generation passed; `/tmp/gradle-8.8/bin/gradle` was available;
    Gradle plugin `clean test build` passed with 11 tests, 0 failures/errors/skips, and non-empty
    main/sources/javadoc jars. Non-blocking Java 8 source/target obsolete warnings occurred on JDK
    21.
- **Gradle runtime dependency audit**
  - Result: PASS
  - Notes: Gradle runtime dependencies contained only `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.
- **Full aggregate verification**
  - Result: PASS
  - Notes: `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed, covering
    version alignment, core verify, root audit, local install, Maven plugin verify/audit, JUnit
    engine verify/audit, and Gradle plugin build/audit.
- **Remote CI execution**
  - Result: PASS (user-/maintainer-confirmed)
  - Notes: After Phase 20/21/22 were pushed, the user/maintainer confirmed remote GitHub Actions
    success for HEAD `5088e96` on `develop`; no run IDs, URLs, durations, or logs were independently
    queried here.
- **Phase 20 metadata verification blockers**
  - Result: PASS
  - Notes: None reported.
- **Publication/deploy/signing status**
  - Result: POSTPONED BY DESIGN
  - Notes: MIT license and maintainer metadata are resolved; GPG signing, Central Portal
    publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final
    publish approval remain unresolved and unimplemented.

## Phase 20 verified commands

```bash
# LICENSE blob comparison against origin/main:LICENSE
bash -n scripts/check-version-alignment.sh scripts/verify-all.sh
# Individual script syntax checks
bash scripts/check-version-alignment.sh
git diff --check
git diff --cached --check
# Untracked whitespace checks
# Effective POM generation and Maven metadata checks for root, Maven plugin, and JUnit engine
mvn -q verify
mvn dependency:tree -Dscope=runtime
mvn -q -Prelease-artifacts -DskipTests package
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-maven-plugin/pom.xml verify
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin generatePomFileForPluginMavenPublication generatePomFileForJavaspecPluginMarkerMavenPublication
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build
# Gradle runtime dependency audit
JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh
```

Commands covered by the passing full aggregate run:

```bash
scripts/check-version-alignment.sh
mvn -q verify
mvn dependency:tree -Dscope=runtime
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
/tmp/gradle-8.8/bin/gradle clean test build
/tmp/gradle-8.8/bin/gradle dependencies --configuration runtimeClasspath
```

## Phase 20 verification details

- Phase 20 is release-readiness scaffolding only.
- `scripts/check-version-alignment.sh` verifies root Maven project version, standalone Maven plugin
  project version, standalone JUnit Platform engine project version, Gradle plugin `version`, and
  Gradle plugin `javaspecCoreVersion` alignment.
- `scripts/verify-all.sh` now runs version alignment before aggregate core and adapter verification.
- `CHANGELOG.md` and `RELEASING.md` are documentation/checklist artifacts only.
- The MIT `LICENSE` was copied exactly from `origin/main`; tester verification confirmed identical
  blob `b990d5492f3ef404ffc145890b83e51914351bb5`.
- Maven POM metadata for root, Maven plugin, and JUnit engine includes MIT License, URL
  `https://opensource.org/licenses/MIT`, distribution `repo`, Mario Giustiniani email, and
  maintainer role.
- Gradle generated POMs `pluginMaven` and `javaspecPluginMarkerMaven` include MIT license and
  maintainer metadata.
- Maven `release-artifacts` profiles create local sources and javadocs for the root, Maven plugin,
  and JUnit Platform engine builds only.
- The Gradle plugin build is ready to produce source and javadoc jars.
- Safe URL, SCM, and GitHub Issues metadata is present in Maven/Gradle metadata.
- Signing configuration, deploy/publish configuration, Central Portal publication, Gradle Plugin
  Portal publication/credentials, final release version/tag, and final publish approval were
  intentionally not added.
- No actual publishing, deployment, signing, secret usage, Maven multi-module conversion, or runtime
  dependency additions are part of this increment. Remote GitHub Actions success for HEAD `5088e96`
  on `develop` is user-/maintainer-confirmed after the Phase 20/21/22 push; no run IDs, URLs,
  durations, or logs were independently queried here.

## Phase 20 dependency summary

- **Core runtime**: No external runtime dependencies; root runtime tree contains only
  `io.github.jvmspec:javaspec`.
- **Maven plugin runtime**: `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.
- **JUnit Platform engine runtime**: `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`,
  `junit-platform-engine:1.10.2`, `opentest4j`, `junit-platform-commons`, `apiguardian-api`.
- **Gradle plugin runtime**: `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.

## Phase 19 verification update

Date: 2026-06-04

This report records the completed Phase 19 verification results provided by the Java tester for the
post-roadmap release/CI hardening increment. Phase 19 added local aggregate verification and a
GitHub Actions workflow without converting the repository to Maven multi-module. Phase 18 stable
identifier/source-location/report evidence, Phase 17 JUnit Platform engine evidence, Phase 16 Gradle
plugin evidence, Phase 15 Maven plugin evidence, Phase 14 no-JUnit invocation evidence, and Phase 12
Distrobox multi-JDK evidence remain below.

## Phase 19 executive summary

- **Tester file modifications**
  - Result: PASS
  - Notes: Tester reported no file modifications.
- **Aggregate script syntax/executable bit**
  - Result: PASS
  - Notes: `bash -n scripts/verify-all.sh` passed; script is executable.
- **GitHub Actions YAML local parse**
  - Result: PASS
  - Notes: actionlint/yamllint/yq were unavailable; PyYAML parsed `.github/workflows/ci.yml` as a
    valid YAML mapping with top-level keys `name`, `on`, and `jobs`, including jobs `core` and
    `full-verification`.
- **Whitespace checks**
  - Result: PASS
  - Notes: `git diff --check`, `git diff --cached --check`, and a temp-index whitespace check
    including untracked `.github/` and `scripts/` passed.
- **Full aggregate verification**
  - Result: PASS
  - Notes: `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed using
    Gradle 8.8.
- **Root core verification**
  - Result: PASS
  - Notes: Script ran `mvn -q verify`; root tests 386, 0 failures, 0 errors, 0 skipped.
- **Root runtime dependency audit**
  - Result: PASS
  - Notes: Runtime tree contained only `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT`.
- **Core install for standalone adapters**
  - Result: PASS
  - Notes: Script ran `mvn -q -DskipTests install`.
- **Maven plugin verification/audit**
  - Result: PASS
  - Notes: Plugin `verify` passed with 12 tests; runtime audit showed
    `io.github.jvmspec:javaspec-maven-plugin` plus `io.github.jvmspec:javaspec`.
- **JUnit Platform engine verification/audit**
  - Result: PASS
  - Notes: Engine `verify` passed with 12 tests; runtime audit showed core plus
    `junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`.
- **Gradle plugin verification/audit**
  - Result: PASS
  - Notes: Gradle plugin `clean test build` passed with 11 tests; runtimeClasspath audit showed
    `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`; non-blocking Java 8 source/target obsolete warnings
    occurred on JDK 21.
- **Remote CI execution**
  - Result: PASS (user-/maintainer-confirmed)
  - Notes: The user/maintainer confirmed GitHub Actions was all green for HEAD `4d30e63` on
    `develop` after the workflow was pushed; no run IDs, URLs, durations, or logs were independently
    queried here.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 19 verified commands

```bash
bash -n scripts/verify-all.sh
# PyYAML parse of .github/workflows/ci.yml; actionlint/yamllint/yq unavailable
git diff --check
git diff --cached --check
# Temp-index whitespace check including untracked .github/ and scripts/
JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh
```

Commands executed by `scripts/verify-all.sh` during the passing aggregate run:

```bash
mvn -q verify
mvn dependency:tree -Dscope=runtime
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
/tmp/gradle-8.8/bin/gradle clean test build
/tmp/gradle-8.8/bin/gradle dependencies --configuration runtimeClasspath
```

## Phase 19 verification details

- Phase 19 is a non-disruptive release/CI hardening increment.
- The repository was not converted to Maven multi-module.
- Repository-root `mvn verify` remains a core-only verification and runtime-audit path for the
  zero-runtime-dependency core artifact.
- `javaspec-maven-plugin/`, `javaspec-gradle-plugin/`, and `javaspec-junit-platform-engine/` remain
  standalone optional artifacts outside the root Maven reactor.
- `scripts/verify-all.sh` resolves the repository root from the script path and verifies the core
  and standalone adapters after refreshing the local core snapshot.
- The script supports `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, and explicit `JAVASPEC_SKIP_GRADLE=1`.
- Without `JAVASPEC_GRADLE_BIN`, Gradle resolution order is repository `./gradlew`,
  `/tmp/gradle-8.8/bin/gradle`, then `gradle` on `PATH`; if none are found and Gradle is not
  explicitly skipped, the script fails clearly.
- `.github/workflows/ci.yml` triggers on `push`, `pull_request`, and `workflow_dispatch`.
- The workflow core job runs a Temurin Java 8, 11, 17, 21, and 25 matrix with Maven cache, root `mvn
  -q verify`, and root runtime dependency audit.
- The workflow full-verification job runs on Java 21 with Maven cache and Gradle setup using Gradle
  8.8, then runs `scripts/verify-all.sh` with `JAVASPEC_GRADLE_BIN=gradle`.
- The workflow adds no publishing, signing, secrets, or deployment behavior.
- Remote GitHub Actions success is user-/maintainer-confirmed for HEAD `4d30e63` on `develop` after
  the workflow was pushed; this report does not include independently queried GitHub run IDs, URLs,
  durations, or logs.

## Phase 19 dependency summary

- **Core runtime**: No external runtime dependencies; root runtime tree contains only
  `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT`.
- **Maven plugin runtime**: `io.github.jvmspec:javaspec-maven-plugin` plus `io.github.jvmspec:javaspec`.
- **JUnit Platform engine runtime**: `io.github.jvmspec:javaspec`, `junit-platform-engine`, `opentest4j`,
  `junit-platform-commons`, `apiguardian-api`.
- **Gradle plugin runtime**: `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.

## Phase 18 verification update

Date: 2026-06-04

This report records the completed Phase 18 verification results provided by the Java tester for the
stable identifier, source-location, and report-consistency polish increment. Phase 17 JUnit Platform
engine evidence, Phase 16 Gradle plugin evidence, Phase 15 Maven plugin evidence, Phase 14 no-JUnit
invocation evidence, and Phase 12 Distrobox multi-JDK evidence remain below.

## Phase 18 executive summary

- **Targeted changed core tests**
  - Result: PASS
  - Notes: `mvn -q
    -Dtest=SpecDiscoveryNamingTest,SpecRunnerTest,RunReportWriterTest,JUnitXmlReportWriterTest,MainPhase11ReportCliTest,MainPhase14CliTest
    test` passed.
- **Core Maven tests**
  - Result: PASS
  - Notes: `mvn -q test` passed with 386 tests, 0 failures, 0 errors, and 0 skipped.
- **Core Maven verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed with 386 tests, 0 failures, 0 errors, and 0 skipped.
- **Core runtime dependency audit**
  - Result: PASS
  - Notes: `mvn dependency:tree -Dscope=runtime` passed; root runtime tree contained only
    `io.github.jvmspec:javaspec`.
- **Core install for standalone adapters**
  - Result: PASS
  - Notes: `mvn -q install` passed and refreshed the local core snapshot.
- **Maven plugin verification**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-maven-plugin/pom.xml verify` passed with 12 tests.
- **JUnit Platform engine verification**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` passed with 12 tests.
- **Gradle plugin verification**
  - Result: PASS
  - Notes: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build` passed with 11
    tests.
- **Gradle plugin runtimeClasspath audit**
  - Result: PASS
  - Notes: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration
    runtimeClasspath` passed.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 18 verified commands

```bash
mvn -q -Dtest=SpecDiscoveryNamingTest,SpecRunnerTest,RunReportWriterTest,JUnitXmlReportWriterTest,MainPhase11ReportCliTest,MainPhase14CliTest test
mvn -q test
mvn -q verify
mvn dependency:tree -Dscope=runtime
mvn -q install
mvn -q -f javaspec-maven-plugin/pom.xml verify
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
```

## Phase 18 verification details

- Phase 18 implemented an incremental IDE/CI polish focused on stable identifiers, source locations,
  and report consistency.
- Stable id aliases were added to `ExampleResult`, `SpecResult`, and `DiscoveredSpec`.
- `ExampleResult.id()` / `stableId()` / `getId()` / `getStableId()` use
  `<specQualifiedName>#<methodName>` semantics matching existing `fullName()`.
- `SpecResult.id()` / `stableId()` / `getId()` / `getStableId()` are derived from the spec qualified
  name.
- `DiscoveredSpec.id()` / `stableId()` / `getId()` / `getStableId()` are derived from the spec
  qualified name.
- `SpecExample` carries a 1-based `sourceLine`.
- `SpecDiscovery.extractExamples` computes the method declaration line for discovered example
  methods.
- `ExampleResult` carries `sourceFilePath` and `sourceLine` when created from discovered
  specs/examples.
- `SpecResult` carries the spec source file path.
- JSON `RunReportWriter` includes spec `id`, `stableId`, and `sourceFile` plus example `id`,
  `stableId`, `fullName`, and `source { file, line }` while preserving existing fields.
- JUnit XML `JUnitXmlReportWriter` includes `file` and `line` attributes on `<testcase>` when source
  data is available while preserving dependency-free JUnit XML-compatible output.
- The optional JUnit Platform engine retained stable unique-id shape and MethodSource behavior, with
  descriptor reporting aligned to stable ids.
- Changed tests asserted stable ids/source metadata in discovery, runner results, JSON reports,
  JUnit XML reports, CLI reports, Maven plugin reports, and Gradle plugin reports.
- Changed test files were:
  - `src/test/java/org/javaspec/discovery/SpecDiscoveryNamingTest.java`
  - `src/test/java/org/javaspec/runner/SpecRunnerTest.java`
  - `src/test/java/org/javaspec/reporting/RunReportWriterTest.java`
  - `src/test/java/org/javaspec/reporting/JUnitXmlReportWriterTest.java`
  - `src/test/java/org/javaspec/cli/MainPhase11ReportCliTest.java`
  - `src/test/java/org/javaspec/cli/MainPhase14CliTest.java`
  - `javaspec-maven-plugin/src/test/java/org/javaspec/maven/JavaspecRunMojoTest.java`
  - `javaspec-gradle-plugin/src/test/java/org/javaspec/gradle/JavaspecGradlePluginTest.java`
- Phase 18 scope covered stable identifier/source-location/report polish; Phases 30-37 are implemented.

## Phase 18 dependency summary

- **Core runtime**: No external runtime dependencies; root runtime tree contains only
  `io.github.jvmspec:javaspec`.
- **Maven plugin runtime**: `io.github.jvmspec:javaspec`.
- **JUnit Platform engine runtime**: `io.github.jvmspec:javaspec`, `junit-platform-engine`, `opentest4j`,
  `junit-platform-commons`, `apiguardian-api`.
- **Gradle plugin runtime**: `io.github.jvmspec:javaspec`.

## Phase 17 verification update

Date: 2026-06-04

This report records the completed Phase 17 verification results provided by the Java tester for the
standalone optional JUnit Platform engine integration. Phase 16 Gradle plugin evidence, Phase 15
Maven plugin evidence, Phase 14 no-JUnit invocation evidence, and Phase 12 Distrobox multi-JDK
evidence remain below.

## Phase 17 executive summary

- **Core install for standalone engine verification**
  - Result: PASS
  - Notes: `mvn -q -DskipTests install` passed before engine verification.
- **Core Maven verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed; root Surefire reported 382 tests, 0 failures, 0 errors, and 0
    skipped.
- **Engine targeted tests**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-junit-platform-engine/pom.xml -Dtest=JavaspecTestEnginePhase17Test
    test` passed with 12 tests, 0 failures, 0 errors, and 0 skipped.
- **Engine Maven verification**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` passed with 12 tests, 0
    failures, 0 errors, and 0 skipped.
- **Core runtime dependency audit**
  - Result: PASS
  - Notes: `mvn dependency:tree -Dscope=runtime` passed; root runtime tree was only
    `io.github.jvmspec:javaspec`.
- **Engine runtime dependency audit**
  - Result: PASS
  - Notes: `mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime` passed;
    runtime dependencies were core `io.github.jvmspec:javaspec`,
    `org.junit.platform:junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and
    `apiguardian-api`; no runtime `junit-jupiter`, `junit-platform-launcher`, or
    `junit-platform-testkit`.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 17 verified commands

```bash
mvn -q -DskipTests install
mvn -q verify
mvn -q -f javaspec-junit-platform-engine/pom.xml -Dtest=JavaspecTestEnginePhase17Test test
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
mvn dependency:tree -Dscope=runtime
mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
```

## Phase 17 verification details

- Phase 17 is implemented as a standalone optional JUnit Platform engine artifact at
  `javaspec-junit-platform-engine/`, intentionally not registered as a root Maven module and outside
  the zero-runtime-dependency core artifact.
- The artifact is `io.github.jvmspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging `jar`,
  Java source/target `1.8`, and uses Java 8-compatible JUnit Platform `1.10.2`; it avoids JUnit
  Platform 6/JUnit 6.
- Runtime dependencies are isolated to the optional engine artifact: core `io.github.jvmspec:javaspec`,
  `org.junit.platform:junit-platform-engine`, and transitives `opentest4j`,
  `junit-platform-commons`, and `apiguardian-api`.
- Test-only dependencies in the engine artifact include JUnit Platform Launcher, JUnit Platform
  TestKit, and JUnit Jupiter.
- Main implementation is
`javaspec-junit-platform-engine/src/main/java/org/javaspec/junit/platform/JavaspecTestEngine.java`.
- ServiceLoader registration is
`javaspec-junit-platform-engine/src/main/resources/META-INF/services/org.junit.platform.engine.TestEngine`,
  containing `io.github.jvmspec.junit.platform.JavaspecTestEngine`; engine id is `javaspec`.
- Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`.
- Configuration parameters include `javaspec.configFile`, `javaspec.suite`,
  `javaspec.specDir`/`javaspec.specRoot`, `javaspec.classFilters`/`classFilter`/`class`,
  `javaspec.exampleFilters`/`exampleFilter`/`example`, and `javaspec.stopOnFailure`.
- Class, package, method, and unique-id selectors are supported as filters over canonical discovery
  results.
- Execution delegates to canonical no-JUnit `JavaspecLauncher` using discovered specs.
- javaspec result states map to JUnit Platform listener events: passed -> successful, failed
  assertion results -> failed assertion-style throwable, broken results -> failed/error-style
  throwable, and skipped/non-loadable -> skipped.
- UniqueId segments use `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and
  `[example:<methodName>]`.
- The engine avoids `System.exit` and does not require changes to javaspec spec authoring style.
- The engine is an optional IDE/CI adapter only. Projects that do not opt into it still have no
  JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths.
- Tester added
`javaspec-junit-platform-engine/src/test/java/org/javaspec/junit/platform/JavaspecTestEnginePhase17Test.java`
  with 12 tests.
- Test coverage includes ServiceLoader engine id discovery, empty/no-spec discovery and execution,
  compiled passing spec success, assertion failure mapping, non-assertion throwable mapping,
  source-only/non-loadable spec skipped, config/suite/specRoot/specDir/class/example filters,
  class/package/method/unique-id selectors, stop-on-failure skip behavior, and canonical
  launcher/no-`System.exit`/no-CLI-adapter source guard.

## Phase 16 verification update

Date: 2026-06-03

This report records the completed Phase 16 verification results provided by the Java tester for the
standalone optional Gradle plugin integration. Phase 15 Maven plugin evidence, Phase 14 no-JUnit
invocation evidence, and Phase 12 Distrobox multi-JDK evidence remain below.

## Phase 16 executive summary

- **Core install for standalone plugin verification**
  - Result: PASS
  - Notes: `mvn -q -DskipTests install` passed before Gradle plugin verification.
- **Core Maven verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed.
- **Core runtime dependency audit**
  - Result: PASS
  - Notes: `mvn dependency:tree -Dscope=runtime` passed; root runtime tree contained only
    `io.github.jvmspec:javaspec`.
- **Gradle plugin tests**
  - Result: PASS
  - Notes: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin test` passed with 11 plugin tests.
- **Gradle plugin build**
  - Result: PASS
  - Notes: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin build` passed.
- **Gradle plugin runtimeClasspath audit**
  - Result: PASS
  - Notes: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration
    runtimeClasspath` passed and showed only `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.
- **Gradle plugin testRuntimeClasspath audit**
  - Result: PASS
  - Notes: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration
    testRuntimeClasspath` passed and showed javaspec, JUnit, and Hamcrest only.
## Phase 16 verified commands

```bash
mvn -q -DskipTests install
mvn -q verify
mvn dependency:tree -Dscope=runtime
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin test
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin build
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration testRuntimeClasspath
```

## Phase 16 verification details

- Phase 16 is implemented as a standalone optional Gradle plugin artifact at
  `javaspec-gradle-plugin/`, intentionally not registered as a root Maven module and outside the
  zero-runtime-dependency core artifact.
- Plugin scaffold files include `settings.gradle`, `build.gradle`, plugin-local `.gitignore`, and
  plugin-local `README.md`.
- `build.gradle` uses `java-gradle-plugin`, group `io.github.jvmspec`, version `0.1.0-SNAPSHOT`, Java
  source/target `1.8`, plugin id `io.github.jvmspec`, implementation class
  `io.github.jvmspec.gradle.JavaspecPlugin`, Maven local/core dependency
  `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`, and plugin-local TestKit/JUnit test dependencies.
- Main implementation files are `JavaspecPlugin`, `JavaspecExtension`, and `JavaspecRunTask` in
  `javaspec-gradle-plugin/src/main/java/org/javaspec/gradle/`.
- The plugin registers extension `javaspec` and task `javaspecRun` in group `verification`.
- When the Gradle Java plugin/source sets are present, `javaspecRun` defaults to the `test` source
  set runtime classpath and depends on `testClasses`.
- The task supports `skip`, `failOnFailure` defaulting to true, `stopOnFailure`, `configFile`,
  `suite`, `specDir`/`specRoot`, class filters, example filters, built-in formatter
  `progress|pretty`, JSON report file aliases (`reportFile`, `jsonReportFile`), and JUnit
  XML-compatible report file aliases (`junitXmlReportFile`, `junitXmlFile`).
- The task loads javaspec configuration when configured, selects suites, builds
  `SpecDiscoveryRequest` with `SpecNamingConvention`, uses a `URLClassLoader` over the Gradle
  classpath, sets/restores the thread context classloader, closes the loader, writes reports via
  core writers, logs through Gradle, throws `GradleException` on failed/broken examples when
  `failOnFailure=true`, and delegates to canonical no-JUnit `JavaspecLauncher` without
  `System.exit`.
- No JUnit is required in projects under test; JUnit is only a plugin test dependency.
- Tester added
  `javaspec-gradle-plugin/src/test/java/org/javaspec/gradle/JavaspecGradlePluginTest.java` with 11
  tests.
- Test coverage includes task registration/default `testClasses` wiring, plugin id/no-spec success,
  compiled passing spec via default test source set runtime classpath, JSON/JUnit XML report
  writing, failed/broken examples fail by default after reports, `failOnFailure=false`, `skip=true`,
  config/suite/class/example filters, explicit `specRoot`, invalid config diagnostics, invalid
  report path diagnostics, and canonical launcher/no-`System.exit` source guard.

## Phase 15 verification update

Date: 2026-06-03

This report records the completed Phase 15 verification results provided by the Java tester for the
standalone optional Maven plugin integration. Phase 14 no-JUnit invocation evidence and Phase 12
Distrobox multi-JDK evidence remain below.

## Phase 15 executive summary

- **Core Maven verification**
  - Result: PASS
  - Notes: `mvn -q verify` passed with 382 core tests.
- **Core install for standalone plugin verification**
  - Result: PASS
  - Notes: `mvn -q -DskipTests install` passed before plugin verification.
- **Plugin targeted tests**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-maven-plugin/pom.xml -Dtest=JavaspecRunMojoTest test` passed with 12
    plugin tests.
- **Plugin Maven verification**
  - Result: PASS
  - Notes: `mvn -q -f javaspec-maven-plugin/pom.xml verify` passed with 12 plugin tests.
- **Core runtime dependency audit**
  - Result: PASS
  - Notes: `mvn dependency:tree -Dscope=runtime` passed; root runtime tree contained only
    `io.github.jvmspec:javaspec`.
- **Plugin runtime dependency audit**
  - Result: PASS
  - Notes: `mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime` passed; plugin
    runtime tree contained the plugin plus compile-scope core `io.github.jvmspec:javaspec` only.
- **Plugin adapter behavior**
  - Result: PASS
  - Notes: Tester added coverage for JUnit XML report I/O failure handling, plugin POM dependency
    scopes, and a guard that the Mojo delegates to `io.github.jvmspec.invocation.JavaspecLauncher`
    without `System.exit` or direct low-level runner coupling.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 15 verified commands

```bash
mvn -q verify
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml -Dtest=JavaspecRunMojoTest test
mvn -q -f javaspec-maven-plugin/pom.xml verify
mvn dependency:tree -Dscope=runtime
mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
```

## Phase 15 verification details

- Phase 15 is implemented as a standalone optional Maven plugin artifact at
  `javaspec-maven-plugin/`, intentionally not registered as a root module so repository-root `mvn
  verify` continues to build and audit only the zero-runtime-dependency core artifact.
- `javaspec-maven-plugin/pom.xml` packages `io.github.jvmspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as
  `maven-plugin`, uses Java source/target `1.8`, goal prefix `javaspec`, Maven API baseline `3.6.3`,
  Maven API and plugin annotations in `provided` scope, JUnit in `test` scope, and compile-scope
  dependency on core `io.github.jvmspec:javaspec`.
- Typical local standalone plugin build sequence is root `mvn install` or `mvn -q -DskipTests
  install` before `mvn -f javaspec-maven-plugin/pom.xml verify`.
- `JavaspecRunMojo` provides `javaspec:run`, default phase `verify`, requires test dependency
  resolution, uses Maven test classpath, supports config/suite/specDir/specRoot selection,
  class/example filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit
  XML-compatible reports, Maven logging, and delegates to canonical no-JUnit `JavaspecLauncher`
  without `System.exit`.
- No JUnit is required in projects under test; JUnit remains only a plugin test dependency.

## Phase 14 verification update

Date: 2026-06-03

This report records the completed Phase 14 verification results provided by the Java tester for the
no-JUnit invocation, explicit classpath, and JUnit XML-compatible reporting increment. Phase 12
Distrobox multi-JDK evidence remains the cross-JDK compatibility matrix below.

## Phase 14 executive summary

- **Phase 14 targeted tests**
  - Result: PASS
  - Notes: `mvn -q -Dtest=JavaspecLauncherTest,JUnitXmlReportWriterTest,MainPhase14CliTest test`
    passed with 18 tests.
- **Full Maven verification**
  - Result: PASS
  - Notes: `mvn verify` passed with 382 tests, 0 failures, 0 errors, and 0 skipped.
- **Runtime dependency audit**
  - Result: PASS
  - Notes: `mvn dependency:tree -Dscope=runtime` passed; runtime tree contains only the root
    artifact.
- **Programmatic invocation**
  - Result: PASS
  - Notes: Tester verified the no-`System.exit` `io.github.jvmspec.invocation` API behavior through
    `JavaspecLauncherTest`.
- **Explicit classpath CLI**
  - Result: PASS
  - Notes: Tester verified `--classpath`, `--classpath-file`, verbose entry listing, UTF-8
    classpath-file parsing, and `describe` rejection through `MainPhase14CliTest`.
- **JUnit XML-compatible reporting**
  - Result: PASS
  - Notes: Tester verified no-spec, normal, failing, dry-run, alias, combined JSON/XML, and I/O
    failure behavior through `JUnitXmlReportWriterTest` and `MainPhase14CliTest`.
- **Blockers**
  - Result: PASS
  - Notes: None reported.

## Phase 14 verified commands

```bash
mvn -q -Dtest=JavaspecLauncherTest,JUnitXmlReportWriterTest,MainPhase14CliTest test
mvn verify
mvn dependency:tree -Dscope=runtime
```

## Phase 14 verification details

- Programmatic no-JUnit invocation under `io.github.jvmspec.invocation` returns structured
  `JavaspecInvocationResult` values and does not call `System.exit`.
- `JavaspecExitCode` maps passing, skipped/pending-only, and no-spec runs to exit code `0`, and
  failed or broken runs to exit code `1`.
- `javaspec run --classpath <path-list>` uses `File.pathSeparator` entries, and `--classpath-file
  <file>` reads UTF-8 non-empty, non-comment entries; explicit entries are used for type existence
  checks and spec execution.
- `describe` rejects classpath and JUnit XML report options as run-only usage.
- `io.github.jvmspec.reporting.JUnitXmlReportWriter` writes dependency-free UTF-8 JUnit XML-compatible
  reports from `RunResult`.
- `--junit-xml` and `--junit-xml-file` write no-spec and normal run reports after output, including
  failing/broken runs before exit `1`.
- Dry-run pending generation/update exits before execution and does not write reports.
- JUnit XML report I/O failures exit `70` and include path diagnostics.
- Existing JSON `--report` / `--report-file` behavior remains compatible, and JSON plus JUnit
  XML-compatible reports can be requested together.
- Limitation confirmed for documentation: javaspec still does not compile source/spec files itself;
  explicit classpath entries must point to already compiled classes or archives.

## Phase 12 consolidated quality matrix

Date: 2026-06-03

This section records the completed Phase 12 verification results provided by the Java tester. The
earlier host-only unavailable runtime matrix is superseded by the completed Distrobox multi-JDK
matrix below. The tester reported no production or test code changes for Phase 12.

## Phase 12 executive summary

| Area | Result | Notes |
|---|---|---|
| Container toolchain | PASS | Distrobox `1.8.2.5` with Podman `5.8.2` was used for the multi-JDK matrix. |
| Multi-JDK Maven verification | PASS | Java 8, 11, 17, 21, and 25 containers each passed `mvn clean` and `mvn verify`. |
| Test totals | PASS | Each container ran 364 tests with 0 failures, 0 errors, and 0 skipped. |
| Phase 12 runtime dependency audit | PASS | Java 25 container runtime scope contained only `io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT`. |
| Java 25 Gatherer runtime probe | PASS | Java 25 runtime reflection found `Gatherer`, nested Gatherer types, and `Gatherers`. |
| Warning review | PASS | JDK 17+ emitted only expected `-source 8` / `-target 1.8` bootstrap/obsolete-option warnings. |
| Blockers | PASS | None reported. |

## Container environment

| Item | Value |
|---|---|
| Distrobox | `1.8.2.5` |
| Podman | `5.8.2` |
| Java 8 image | `docker.io/library/maven:3.9-eclipse-temurin-8` |
| Java 11 image | `docker.io/library/maven:3.9-eclipse-temurin-11` |
| Java 17 image | `docker.io/library/maven:3.9-eclipse-temurin-17` |
| Java 21 image | `docker.io/library/maven:3.9-eclipse-temurin-21` |
| Java 25 image | `docker.io/library/maven:3.9-eclipse-temurin-25` |

Per-container command:

```bash
distrobox enter --name <container> --no-tty -- bash -lc \
  'cd /home/paperboy/workspace/javaspec &&
   java -version &&
   javac -version &&
   mvn -version &&
   mvn clean &&
   mvn verify'
```

The matrix containers were created and stopped, not removed: `javaspec-jdk8-matrix`,
`javaspec-jdk11-matrix`, `javaspec-jdk17-matrix`, `javaspec-jdk21-matrix`, and
`javaspec-jdk25-matrix`.

## Distrobox multi-JDK matrix

| JDK | Container | Java runtime | Maven | Result | Test totals |
|---|---|---|---|---|---|
| Java 8 | `javaspec-jdk8-matrix` | `1.8.0_492` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 11 | `javaspec-jdk11-matrix` | `11.0.31` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 17 | `javaspec-jdk17-matrix` | `17.0.19` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 21 | `javaspec-jdk21-matrix` | `21.0.11 LTS` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 25 | `javaspec-jdk25-matrix` | `25.0.3 LTS` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |

JDK 17, 21, and 25 emitted only the expected Maven compiler bootstrap/obsolete-option warnings for
the Java 8 source/target settings.

## Supplemental compatibility and regression audits

The Distrobox matrix is the authoritative runtime matrix. Earlier Phase 12 compatibility and
regression audits remain part of the consolidated evidence:

- Targeted regression tests passed for `TypeSkeletonGeneratorTest`, `ClassMethodUpdaterTest`,
  `MainPhase11ReportCliTest`, `RunFormatterRegistryTest`, `ExtensionContextTest`, and
  `RunReportWriterTest` with 43 tests, 0 failures, 0 errors, and 0 skipped.
- Maven compiler `source` and `target` remain `1.8`.
- Production sources compiled with `javac --release 8` on JDK 21.
- 103 production class files were inspected and all used Java 8 classfile major version `52`.
- The constant-pool audit found 82 unique direct `java.*` class references, 70 intentional
  post-Java-8 metadata string hits, and 0 direct post-Java-8 API references.

## Runtime dependency audit

The runtime dependency audit was run in `javaspec-jdk25-matrix` with:

```bash
mvn dependency:tree -Dscope=runtime
```

Result: PASS. Runtime scope contained only the root artifact:

```text
io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT
```

No third-party runtime dependency leakage was found.

## Java 25 Gatherer runtime validation

The Java 25 runtime reflection probe passed in `javaspec-jdk25-matrix` for all expected stream
gatherer types:

- `java.util.stream.Gatherer`
- `java.util.stream.Gatherer$Downstream`
- `java.util.stream.Gatherer$Integrator`
- `java.util.stream.Gatherer$Integrator$Greedy`
- `java.util.stream.Gatherers`

## Related documentation

- [ARC42 quality requirements](arc42/10-quality-requirements.md) summarize the quality scenarios
  backed by this report.
- [ARC42 risks and technical debt](arc42/11-risks-and-technical-debt.md) records remaining
  limitations that are not test failures.
- [README](../README.md) and the [user manual](usermanual/Home.md) reference the Phase 21
  verification, Phase 20 verification, Phase 19 verification, Phase 18 verification, Phase 17
  verification, Phase 16 verification, Phase 15 verification, Phase 14 verification, and Phase 12
  matrix for user-facing verification claims.

## Conclusion

Phase 22 verification is complete for explicit skipped/pending semantics: targeted changed tests,
root test/verify/runtime audit/install, standalone adapters, examples verification, and aggregate
verification all passed locally. Phase 21 verification is complete for adoption assets: no core
production/test Java files changed, schema and golden reports parsed/validated locally,
`scripts/verify-examples.sh` passed with Maven, JUnit Platform, and Gradle examples, full aggregate
`scripts/verify-all.sh` passed with the standalone examples section, examples generated expected
report markers, dependency summaries stayed clean, and no publish/deploy/signing commands were run.
Phase 20 verification is complete for release-readiness scaffolding: the MIT `LICENSE` matches
`origin/main`, MIT license and Mario Giustiniani maintainer metadata passed Maven and Gradle
generated-POM checks, version alignment, script syntax/executable checks, whitespace checks,
effective POM generation, root verify/runtime audit, root/Maven plugin/JUnit engine source/javadoc
artifact checks, standalone Maven plugin and JUnit engine verification, Gradle publication POM
generation, Gradle plugin build/artifact checks, Gradle runtime audit, and full aggregate
verification passed locally. After Phase 20/21/22 were pushed, the user/maintainer confirmed remote
GitHub Actions success for HEAD `5088e96` on `develop`; no GitHub run IDs, URLs, durations, or logs
were independently queried here. The workflow scope remains `.github/workflows/ci.yml`: a Java
8/11/17/21/25 core matrix and Java 21 full verification through `scripts/verify-all.sh`, including
examples by default unless explicitly skipped. Artifacts are published on Maven Central under `io.github.jvmspec`. The Gradle plugin is
published on the Gradle Plugin Portal with plugin id `io.github.jvmspec`. GPG signing, Central
Portal publication, Gradle Plugin Portal publication/credentials, and final release version/tag Phase 19 verification is complete for the
post-roadmap release/CI hardening increment: script syntax/executable validation, local GitHub
Actions YAML parse, whitespace checks, and full local aggregate verification through
`scripts/verify-all.sh` passed with no blockers; the user/maintainer also confirmed green GitHub
Actions status for HEAD `4d30e63` on `develop`. Phase 18 verification is complete for the stable
identifier/source-location/report polish increment: targeted changed tests, full core test/verify
runs, root runtime dependency audit, core install, standalone Maven plugin verification, standalone
JUnit Platform engine verification, standalone Gradle plugin verification, and Gradle
runtimeClasspath audit passed with no blockers. Phase 17 verification is complete for the standalone
optional JUnit Platform engine. Phase 16 verification is complete for the standalone optional Gradle
plugin using Gradle 8.8 on the installed Java 21 runtime. Phase 15 verification is complete for the standalone optional Maven plugin. Phase 14
verification is complete for no-JUnit invocation, explicit classpath input, and JUnit XML-compatible
reporting. Phase 12 is fully completed through the Distrobox multi-JDK matrix. Java 8, 11, 17, 21,
and 25 containers all passed `mvn clean` and `mvn verify` with identical clean test totals. The Java
25 runtime Gatherer probe and Java 25 runtime dependency audit also passed.
