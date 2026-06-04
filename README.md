# javaspec

[![CI](https://github.com/mgiustiniani/javaspec/actions/workflows/ci.yml/badge.svg)](https://github.com/mgiustiniani/javaspec/actions/workflows/ci.yml)

javaspec is a Java 8-compatible, zero-runtime-dependency specification framework inspired by phpspec. Its goal is to bring a specification-first workflow to Java while preserving a small runtime footprint and a conservative compatibility baseline.

Phases 2 through 18 of the original roadmap are implemented, Phase 19 adds post-roadmap release/CI hardening, Phase 20 adds release-readiness scaffolding, Phase 21 adds standalone adoption examples plus report schema/golden documentation, Phase 22 adds explicit skipped/pending example semantics, Phase 23 adds classpath/execution availability diagnostics, Phase 24 adds optional configuration-level report destinations, and Phase 12 compatibility/quality verification is complete through the Distrobox multi-JDK matrix for Java 8, 11, 17, 21, and 25. Phase 2 provides a Maven-based CLI entry point, `org.javaspec.cli.Main`, with a PHPSpec-style split: `describe` creates specification/support skeletons, while `run` discovers specs and can generate missing class-like production type skeletons after confirmation or `--generate`. Phase 3 adds Java LTS profiles and metadata/reflection probes. Phase 4 adds zero-dependency configuration, suites, naming, and filters. The Phase 5/6 MVP executes compiled discovered examples when available. Phase 7 expands matchers, Phase 8 adds JDK-proxy interface doubles, Phase 9 adds run controls, Phase 10 adds interface-style generation, and Phase 11 adds formatter contracts, programmatic extension contracts, and JSON reports. Phase 14 adds the no-JUnit integration foundation: `org.javaspec.invocation` provides `JavaspecInvocation`, `JavaspecLauncher`, `JavaspecInvocationResult`, and `JavaspecExitCode`; `javaspec run` accepts explicit compiled-class classpath input through `--classpath` / `--classpath-file`; and `--junit-xml` / `--junit-xml-file` writes dependency-free JUnit XML-compatible reports. Phase 15 adds the standalone optional Maven plugin at `javaspec-maven-plugin/`, Phase 16 adds the standalone optional Gradle plugin at `javaspec-gradle-plugin/`, Phase 17 adds the standalone optional JUnit Platform engine at `javaspec-junit-platform-engine/`, and Phase 18 adds stable identifier, source-location, and report-consistency polish. Phase 19 intentionally does not convert the repository to Maven multi-module: root `mvn verify` remains core-only, standalone adapters remain outside the root Maven reactor, and `scripts/verify-all.sh` plus `.github/workflows/ci.yml` provide aggregate verification. Phase 20 adds `CHANGELOG.md`, `RELEASING.md`, `scripts/check-version-alignment.sh`, Maven `release-artifacts` profiles for local sources/javadocs, Gradle source/javadoc jar readiness, safe URL/SCM/GitHub Issues metadata, a confirmed MIT `LICENSE`, MIT license metadata, and confirmed maintainer/developer metadata for `Mario Giustiniani <mariogiustiniani@gmail.com>`. It does not publish, deploy, sign, add secrets, add runtime dependencies, configure Central Portal publication, configure Gradle Plugin Portal publication/credentials, choose a final release version/tag, approve final publishing, or convert to Maven multi-module. Phase 21 adds `docs/schemas/run-report-v1.schema.json`, golden reports under `docs/examples/reports/`, standalone consumer examples under `examples/`, and `scripts/verify-examples.sh`; `scripts/verify-all.sh` now runs those standalone examples by default after core/adapters with explicit `JAVASPEC_SKIP_EXAMPLES=1` opt-out. Phase 22 adds zero-dependency `@Skip`/`@Pending` annotations, `SkipExampleException`/`PendingExampleException`, `ObjectBehavior.skip(...)`/`pending(...)` helpers, distinct `PENDING` result counts, pending-aware JSON/JUnit XML-compatible reports, Maven summary logging, and JUnit Platform skipped-event mapping. Phase 23 adds enriched skipped/not-executable reasons when discovered source specs/examples are unavailable to the runner classloader, deterministic `RunDiagnostics.executionAvailabilityLines(RunResult)` programmatic diagnostics, CLI `Execution diagnostics:` output, and Maven/Gradle `javaspec:` warning diagnostics with classpath element counts, without adding source compilation or changing exit-code/build-failure semantics. Phase 24 adds optional top-level configuration defaults for JSON and JUnit XML-compatible report destinations; CLI and explicit build-tool adapter report settings override those config defaults, and report schemas, writer behavior, exit codes, build-failure semantics, dry-run pending behavior, no-spec behavior, and standalone adapter boundaries remain unchanged. All three adapters remain outside the zero-runtime-dependency core artifact and delegate to the canonical no-JUnit `JavaspecLauncher` without `System.exit`; projects that do not opt into the JUnit Platform engine still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths. Known limitations: javaspec still does not compile source or spec files itself; users must still compile specs normally and place compiled outputs/dependencies on the javaspec classpath; public publication remains intentionally postponed because GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval remain unresolved; selected profiles are validated but not deeply enforced during execution yet; external extension discovery/loading is not implemented; JSON reporting remains schemaVersion 1 with additive stable id/source/pending fields and JUnit XML-compatible reporting remains intentionally minimal with testcase file/line attributes plus skipped-element mapping for skipped and pending examples; existing sealed-interface source updates are intentionally skipped; generic `Iterable` count/empty checks consume the iterable and can hang on infinite iterables; doubles are interface-only. The binary remains Java 8-compatible; post-Java 8 forms such as records, sealed types, sequenced collections, and stream gatherers are modeled as source text or metadata/reflection only.

## Project Goals

- Provide a Java port inspired by phpspec concepts: describing classes, discovering specifications, running examples, expectations, doubles, generation prompts, and extensibility.
- Compile and run on Java 8.
- Keep the runtime artifact free of third-party dependencies.
- Keep external dependencies out of the core runtime artifact; allow core test-scope dependencies and isolated optional-adapter dependencies only.
- Model target Java LTS profiles for Java 8 and later LTS releases available as of 2026-05-27: 8, 11, 17, 21, and 25.
- Represent post-Java 8 APIs through metadata, strings, or reflection so the Java 8-compatible binary never directly links against APIs that do not exist on Java 8.

## Architectural Constraints

1. **Java 8 baseline**: all production code must compile with Java 8 source and target compatibility.
2. **No runtime dependencies**: the main artifact must depend only on the Java 8 standard library.
3. **Core dependency isolation**: external libraries may be used for tests, compatibility verification, build-time quality checks, or standalone optional adapters, but they must stay outside the core runtime artifact.
4. **LTS profile metadata**: Java 11, 17, 21, and 25 capabilities must be modeled as target profiles rather than as direct compile-time references.
5. **Package base**: production code uses the package base `org.javaspec`.
6. **Maven implementation**: the project uses Maven while preserving Java 8 bytecode compatibility.

## Architecture Principles

- **Compatibility first**: Java 8 compatibility is a release gate, not an optional mode.
- **Metadata over linkage**: newer JDK APIs are named and discovered through profile metadata and reflection, not imported directly by Java 8-compiled production code.
- **Small core**: the core runtime should contain only the specification model, runner, matcher contracts, profile catalog, and minimal utilities required to execute specs.
- **Explicit boundaries**: CLI, configuration, discovery, runner, expectations, doubles, generators, and extension points should have separate responsibilities.
- **Deterministic behavior**: spec discovery, execution order, output, and exit codes should be predictable for local and CI use.
- **Extensibility without dependency cost**: extension hooks should be exposed through Java interfaces and programmatic contracts without adding runtime dependencies; external CLI extension loading is not implemented yet.

## Zero-Dependency Policy

The runtime artifact must not require libraries such as YAML parsers, bytecode manipulation libraries, assertion libraries, logging frameworks, or dependency injection containers. If a capability normally depends on such libraries, javaspec should either:

- implement a small internal equivalent using Java 8 APIs,
- expose an extension point so users can integrate optional tools outside the core runtime, or
- defer the feature until it can be implemented without violating the policy.

Project tests may use external test dependencies, but those dependencies must not leak into runtime packaging. The current repository test suite uses JUnit in test scope only; using javaspec specs does not require JUnit. Phase 14's JUnit XML-compatible writer is dependency-free and does not introduce a JUnit runtime dependency. The Phase 15 Maven plugin is a separate optional artifact: Maven APIs and plugin annotations are `provided`, JUnit is only a plugin test dependency, and the plugin runtime tree contains the plugin plus compile-scope core `org.javaspec:javaspec` only. The Phase 16 Gradle plugin is also a separate optional artifact: JUnit/TestKit are only plugin test dependencies, and the verified Gradle `runtimeClasspath` contains only core `org.javaspec:javaspec:0.1.0-SNAPSHOT`. The Phase 17 JUnit Platform engine is a separate optional artifact; its JUnit Platform runtime dependencies are isolated to `javaspec-junit-platform-engine/` and do not enter the core runtime artifact. Phase 8 doubles use JDK dynamic proxies rather than bytecode libraries. The core runtime dependency tree contains only the project artifact, aside from the JDK platform.

## Profile Catalog and Java 8 Compatibility Strategy

The implemented profile/catalog model lives in `org.javaspec.profile`. `TargetProfile`, `FeatureFlag`, `ApiSymbol`, `ApiSymbolKey`, `ApiSymbolKind`, `ApiSymbolCategory`, and `ProfileCatalog` provide deterministic metadata for Java 8, 11, 17, 21, and 25. The default catalog covers representative data-structure APIs from [`docs/research/java-lts-data-structures.md`](docs/research/java-lts-data-structures.md), including Java 11 collection factories, Java 17 stream/record/sealed metadata, Java 21 sequenced collections, and Java 25 stream gatherers.

Compatibility checks live behind `org.javaspec.compatibility`. `ProfileCompatibilityCheck` evaluates whether type kinds, feature flags, or API symbols fit a target profile, while `ApiAvailabilityProbe` uses class, method, and field names to probe optional APIs reflectively. Production code does not import Java 9+ APIs directly, so the runtime artifact can remain Java 8-compatible while understanding newer LTS capabilities.

## Build, Test, and CLI Usage

Build and test the core from the repository root:

```sh
mvn verify
mvn dependency:tree -Dscope=runtime
```

Repository-root `mvn verify` is intentionally core-only. The optional Maven plugin, Gradle plugin, and JUnit Platform engine remain standalone artifacts outside the root Maven reactor. For release-readiness version alignment, run:

```sh
scripts/check-version-alignment.sh
```

For local aggregate release verification, run:

```sh
scripts/verify-all.sh
```

`scripts/check-version-alignment.sh` verifies that the root Maven version, standalone Maven plugin version, standalone JUnit Platform engine version, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` are aligned. `scripts/verify-all.sh` runs that check first, then resolves the repository root from the script path, runs root `mvn -q verify`, audits root runtime dependencies, installs the current core snapshot, verifies and audits the standalone Maven plugin and JUnit Platform engine, runs the standalone Gradle plugin `clean test build` plus `runtimeClasspath` audit, and runs standalone examples verification by default. Environment variables: `MAVEN_BIN` selects Maven, `JAVASPEC_GRADLE_BIN` selects Gradle, explicit `JAVASPEC_SKIP_GRADLE=1` skips Gradle adapter verification, explicit `JAVASPEC_SKIP_EXAMPLES=1` skips all standalone examples from `verify-all`, and explicit `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` skips only the Gradle example inside `scripts/verify-examples.sh`. Without `JAVASPEC_GRADLE_BIN`, the script tries repo `./gradlew`, `/tmp/gradle-8.8/bin/gradle`, then `gradle` on `PATH`; if none are found, it fails clearly unless Gradle is explicitly skipped.

Standalone adoption examples are consumer projects, not root modules. To verify only examples after local snapshot installation by the script, run:

```sh
scripts/verify-examples.sh
```

The examples live in `examples/maven-basic/`, `examples/gradle-basic/`, and `examples/junit-platform-basic/`. They demonstrate Maven plugin, Gradle plugin, and JUnit Platform engine adoption paths and generate JSON/JUnit XML-compatible reports under their own `target/` or `build/` directories. See [`examples/README.md`](examples/README.md). The JSON report schema is documented at [`docs/schemas/run-report-v1.schema.json`](docs/schemas/run-report-v1.schema.json), with golden JSON and JUnit XML-compatible examples under [`docs/examples/reports/`](docs/examples/reports/).

Local release-artifact readiness checks are packaging checks only; they do not sign, stage, deploy, or publish anything:

```sh
mvn -q -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests package
gradle -p javaspec-gradle-plugin clean test build
```

Latest Phase 20 verification: `LICENSE` is identical to `origin/main:LICENSE` (blob `b990d5492f3ef404ffc145890b83e51914351bb5`); script syntax checks, executable-bit checks, version alignment at `0.1.0-SNAPSHOT`, whitespace checks including `git diff --check` and `git diff --cached --check`, effective POM generation, Maven metadata checks for MIT License (`https://opensource.org/licenses/MIT`, distribution `repo`) and Mario Giustiniani maintainer metadata, Gradle generated POM metadata checks, root `mvn -q verify` with 386 tests, root runtime dependency audit, Maven `release-artifacts` packaging for root/Maven plugin/JUnit engine with non-empty main/sources/javadoc jars, standalone Maven plugin and JUnit engine `verify` with 12 tests each, Gradle plugin generated POMs plus `clean test build` with 11 tests and non-empty jars, Gradle runtime dependency audit, and full aggregate `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` all passed locally. No publish/deploy/signing/portal credential commands were run or configured.

Latest Phase 20/21/22 verification: Phase 20 release-readiness checks, Phase 21 adoption/example checks, and Phase 22 explicit skipped/pending checks all passed locally; Phase 22 included targeted changed tests with 78 tests, root `mvn -q test` with 399 tests, root `mvn -q verify`, root runtime dependency audit, core install, standalone Maven/JUnit Platform/Gradle adapter verification and runtime audits, `scripts/verify-examples.sh`, and `scripts/verify-all.sh` with Gradle 8.8. The GitHub Actions workflow remains `.github/workflows/ci.yml`: a Java 8/11/17/21/25 core matrix plus Java 21 full verification through `scripts/verify-all.sh`, including examples by default unless explicitly skipped. After Phase 20/21/22 were pushed, remote GitHub Actions success for HEAD `5088e96` on `develop` was user-/maintainer-confirmed; this environment did not independently query GitHub run IDs, URLs, durations, or logs. No production Java changes were made by the tester, no blockers were reported, and no publish/deploy/signing/portal credential commands were run or configured. See [`docs/test-report.md`](docs/test-report.md).

The Maven compiler configuration targets Java 8 (`source`/`target` 1.8). The packaged runtime has no third-party dependencies.

After packaging, run the CLI with the jar, or substitute an installed `javaspec` launcher when one exists:

```sh
java -jar target/javaspec-0.1.0-SNAPSHOT.jar --help
java -jar target/javaspec-0.1.0-SNAPSHOT.jar describe <ClassName> [--config <file>] [--suite <name>] [--spec-dir <dir>]
java -jar target/javaspec-0.1.0-SNAPSHOT.jar desc <ClassName> [--config <file>] [--suite <name>] [--spec-root <dir>]
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run [--config <file>] [--suite <name>] [--spec-dir <dir>] [--source-dir <dir>] [--classpath <path-list>] [--classpath-file <file>] [--generate] [--dry-run] [--stop-on-failure] [--formatter <progress|pretty>] [--profile <java8|java11|java17|java21|java25>] [--verbose] [--report <file>] [--report-file <file>] [--junit-xml <file>] [--junit-xml-file <file>] [--constructor-policy <delete|preserve|comment>] [--class <name>] [--example <name>]
```

Without `--config`, javaspec infers defaults: suite `default`, spec root `src/test/java`, source root `src/main/java`, spec package prefix `spec`, production package prefix empty, profile `java8`, formatter `progress`, constructor policy `comment`, no JSON/JUnit XML-compatible report destination, and empty bootstrap hooks. `--config <file>` loads a restricted line-based configuration file, and `--suite <name>` selects a configured suite. Selected-suite paths and package prefixes drive spec/source naming unless overridden by `--spec-dir`/`--spec-root` or `--source-dir`/`--source-root`. `run` uses the configured constructor policy, profile, formatter, and optional report destinations unless CLI options override them; repeatable `--class <name>` and `--example <name>` filters limit discovery and execution by described/spec class and example method/display name/order index. Run-only command-line options such as `--classpath`, `--classpath-file`, `--generate`, `--dry-run`, `--stop-on-failure`, `--formatter`, `--profile`, `--verbose`, `--report`, `--report-file`, `--junit-xml`, `--junit-xml-file`, `--constructor-policy`, `--class`, and `--example` are rejected for `describe`; `describe --config <file>` accepts config files containing report destinations but does not write reports.

`run --classpath <path-list>` adds explicit classpath entries separated by `File.pathSeparator` (`:` on Unix-like systems, `;` on Windows). `run --classpath-file <file>` reads UTF-8 classpath entries, one per non-empty line; lines whose trimmed form begins with `#` are ignored. When explicit classpath entries are supplied, javaspec creates a selected classloader over those entries and uses it for production type existence checks and spec execution. `--verbose` lists the explicit entries. javaspec still does not compile source or spec files, so explicit entries must point to already compiled classes or archives.

When source discovery finds specs/examples but the runner classloader cannot load the compiled spec class, one of its dependencies, or the expected public no-argument example method, core result details explain that the item is not executable. The CLI prints an `Execution diagnostics:` block only when those execution-availability issues exist. With no explicit classpath it notes that the current process classloader was used and suggests `--classpath`/`--classpath-file`; with explicit classpath input it reports the explicit entry count. These diagnostics exclude intentional user `@Skip` and `PENDING` semantics, do not compile source/spec files, and do not change exit-code semantics.

After discovery, generation, and source updates complete without declined prompts, `run` invokes the reflection runner for discovered examples. PASSED examples complete normally; AssertionError is FAILED; non-assertion throwables, lifecycle errors, instantiation errors, and reflection errors are BROKEN; non-loadable spec classes and missing reflected example methods are SKIPPED with execution-availability reasons; explicitly pending examples are PENDING. By default every discovered example metadata entry is processed; `--stop-on-failure` stops after the first FAILED or BROKEN executable example. `--formatter progress` prints concise summary-oriented output, while `--formatter pretty` prints per-example status lines plus details for failed, broken, skipped, and pending examples. Built-in output now uses the public `RunFormatter` contract and deterministic `RunFormatterRegistry`, but the CLI does not yet load external extension-provided formatters. `--profile` selects and validates one of the configured LTS profile keys but does not yet perform deep profile enforcement.

Skip or mark examples pending without extra dependencies:

```java
import org.javaspec.api.Pending;
import org.javaspec.api.Skip;

public class PaymentSpec extends PaymentSpecSupport {
    @Skip(reason = "external gateway unavailable")
    public void it_refunds_a_charge() {
    }

    @Pending("waiting for billing provider")
    public void it_charges_a_card() {
    }

    public void it_can_stop_at_runtime() {
        pending("replace stub with contract test");
        // or: skip("requires test data")
    }
}
```

`@Skip` takes precedence when both annotations are present. Annotation-based skip/pending does not instantiate the spec or run `let()`/example/`letGo()`. Runtime `skip(...)` or `pending(...)` from `let()` or an example marks the example after successful `letGo()`; a `letGo()` failure after a skip/pending signal is BROKEN.

`run --report <file>` writes a UTF-8 JSON runner report; `--report-file <file>` is an alias. A config file can also provide a default JSON destination using `report`, `reportFile`, `report-file`, `jsonReport`, `jsonReportFile`, or `json-report-file`; values are trimmed, must be non-blank when present, default to absent/null, and CLI report options override the configured value. The JSON report contains `schemaVersion: 1`, aggregate summary counts including `pending`, specs, examples, stable spec/example ids, source file/line metadata where available, nullable failure details, throwable class/message, and stack trace lines. The documented schema is [`docs/schemas/run-report-v1.schema.json`](docs/schemas/run-report-v1.schema.json), and golden passing and pending JSON/JUnit XML-compatible report examples are in [`docs/examples/reports/`](docs/examples/reports/). `run --junit-xml <file>` writes a UTF-8 JUnit XML-compatible report; `--junit-xml-file <file>` is an alias. A config file can also provide a default JUnit XML-compatible destination using `junitXml`, `junit-xml`, `junitXmlFile`, `junit-xml-file`, `junitXmlReportFile`, or `junit-xml-report-file`; values are trimmed, must be non-blank when present, default to absent/null, and CLI JUnit XML options override the configured value. JUnit XML `<testcase>` elements include `file` and `line` attributes when source data is available. JSON and JUnit XML reports can be requested together from CLI options, config destinations, or a mix of both. Passing, failing, broken, skipped/pending-only, and no-spec runs write requested reports after normal summary/no-spec output; failed or broken executable examples still exit `1` after the reports are written. `--verbose` shows the effective report paths, whether they came from config or CLI options. JUnit XML-compatible reports map both SKIPPED and PENDING examples to `<skipped>`; the testsuite `skipped` attribute includes skipped plus pending, and pending messages are prefixed with `Pending:` when a reason is present. Dry-run pending generation/update exits before execution and does not write reports. Report write failures print I/O diagnostics and exit `70`.

`run --dry-run` performs discovery and planning without writes and without prompts. It reports would-generate/would-update actions for related specs/support, support updates, constructor changes, method bodies/declarations/elements, and missing production type generation. Dry-run exits `1` when pending generation/update work exists; when no pending changes exist, executable examples are handled normally and passed or skipped/pending-only runs exit `0`.

Examples:

```sh
# Describe creates only the PHPSpec-style spec skeleton.
java -jar target/javaspec-0.1.0-SNAPSHOT.jar describe org.example.Calculator --spec-dir /tmp/javaspec-demo/src/test/java
# creates /tmp/javaspec-demo/src/test/java/spec/org/example/CalculatorSpec.java

# Run discovers specs. If the described production type is missing, it asks whether to create it.
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --spec-dir /tmp/javaspec-demo/src/test/java --source-dir /tmp/javaspec-demo/src/main/java
# Do you want me to create org.example.Calculator for you? [Y/n]

# Explicit generation belongs to run and answers yes non-interactively.
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --spec-dir /tmp/javaspec-demo/src/test/java --source-dir /tmp/javaspec-demo/src/main/java --generate

# Configured suite paths can be selected with --config and --suite.
java -jar target/javaspec-0.1.0-SNAPSHOT.jar describe org.example.Calculator --config javaspec.conf --suite domain
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --config javaspec.conf --suite domain --generate

# Run controls, explicit classpath input, and reports are run-only.
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --dry-run
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --stop-on-failure --formatter pretty --profile java17 --verbose
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --classpath target/classes:target/test-classes --verbose
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --classpath-file target/javaspec-classpath.txt
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --report target/javaspec-report.json
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --junit-xml target/javaspec-report.xml
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --report-file target/javaspec-report.json --junit-xml-file target/javaspec-report.xml --verbose
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --config javaspec.conf --verbose # uses configured report destinations unless CLI report options override them
```

Exit codes: `0` for success/help/generated-or-existing targets, no-spec runs, successful dry-runs with no pending generation/update work, and successful or skipped/pending-only executable runs; `1` when missing production types or method updates are not generated after a prompt is declined or unavailable, when dry-run finds pending generation/update work, or when executable examples fail or break; `64` for invalid arguments; and `70` for I/O errors, including classpath-file read failures and JSON/JUnit XML report write failures.

## Programmatic No-JUnit Invocation

`org.javaspec.invocation` exposes a no-`System.exit` API for launchers, build tools, and CI adapters that want to invoke the canonical javaspec discovery and runner inside the current JVM:

```java
SpecDiscoveryRequest request = SpecDiscoveryRequest.of(new File("src/test/java"));
JavaspecInvocation invocation = JavaspecInvocation.discovering(request, classLoader)
        .withStopOnFailure(true);
JavaspecInvocationResult result = JavaspecLauncher.run(invocation);
int exitCode = result.exitCode();
RunResult runResult = result.runResult();
```

`JavaspecInvocation` accepts either a `SpecDiscoveryRequest` or already discovered `DiscoveredSpec` values plus the selected `ClassLoader`. `JavaspecLauncher` reuses canonical `SpecDiscovery`, `SpecRunner`, and `RunResult` behavior and returns `JavaspecInvocationResult` with discovered specs, the run result, success/failure helpers, and an exit code. `DiscoveredSpec`, `SpecResult`, and `ExampleResult` expose stable id aliases, and runner results carry source metadata where discovery supplied it. `JavaspecExitCode` maps passing, skipped/pending-only, and no-spec runs to `0`, and failed or broken runs to `1`. The programmatic launcher is still classpath-based and does not compile source/spec files itself.

For host tooling that wants the same classpath guidance as the CLI/adapters, `org.javaspec.diagnostics.RunDiagnostics.executionAvailabilityLines(RunResult)` returns deterministic human-readable lines for not-executable specs and missing compiled example methods. It intentionally excludes explicit user `@Skip` and `PENDING` results so callers can separate classpath/execution availability problems from intentional non-execution.

## Optional Maven Plugin

Phase 15 provides a standalone optional Maven plugin artifact at `javaspec-maven-plugin/`. It is not registered as a root module, so repository-root `mvn verify` continues to verify only the core artifact. For local standalone plugin verification, install the current core first, then verify the plugin:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
```

The plugin packages `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin` with goal prefix `javaspec`. A consuming Maven build can declare it as optional project tooling:

```xml
<plugin>
  <groupId>org.javaspec</groupId>
  <artifactId>javaspec-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</plugin>
```

Its `javaspec:run` goal is bound to Maven's `verify` phase by default, requires test dependency resolution, uses the Maven test classpath, supports config/suite/specDir/specRoot selection, class/example filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible reports, and Maven logging with pending counts. Configured report destinations are used as defaults when explicit plugin report settings are absent; explicit Maven plugin report settings override config values. When execution-availability issues exist, it logs `javaspec:` warning diagnostics plus the Maven test classpath element count. The diagnostics do not add source compilation and do not change `failOnFailure` or build-failure semantics. It delegates to `org.javaspec.invocation.JavaspecLauncher` without `System.exit`. Projects under test do not need JUnit.

## Optional Gradle Plugin

Phase 16 provides a standalone optional Gradle plugin artifact at `javaspec-gradle-plugin/`. It is not registered as a root Maven module, so repository-root `mvn verify` continues to verify only the core artifact. For local standalone plugin verification, install the current core first, then run the Gradle plugin build with a compatible Gradle executable:

```sh
mvn -q -DskipTests install
gradle -p javaspec-gradle-plugin build
```

The Java 21 verification used Gradle 8.8 downloaded to `/tmp/gradle-8.8` and not committed. The cached Gradle 7.4.2 executable was blocked by Java 21 class-file compatibility and is not counted as a javaspec failure.

In a consuming Gradle build where the plugin artifact is available, apply plugin id `org.javaspec` and configure the optional extension/task:

```groovy
plugins {
    id 'java'
    id 'org.javaspec' version '0.1.0-SNAPSHOT'
}

javaspec {
    suite = 'default'
    formatter = 'progress'
    reportFile = file("$buildDir/reports/javaspec/report.json")
    junitXmlReportFile = file("$buildDir/test-results/javaspec.xml")
}

tasks.named('javaspecRun') {
    stopOnFailure = true
    failOnFailure = true
}
```

The plugin registers extension `javaspec` and task `javaspecRun` in the `verification` group. With the Gradle Java plugin/source sets present, `javaspecRun` defaults to the `test` source set runtime classpath and depends on `testClasses`. It supports `skip`, `failOnFailure`, `stopOnFailure`, `configFile`, `suite`, `specDir`/`specRoot`, class/example filters, `progress` or `pretty` formatter selection, JSON report aliases (`reportFile`, `jsonReportFile`), and JUnit XML-compatible report aliases (`junitXmlReportFile`, `junitXmlFile`), inheriting pending-aware summaries and reports from the core runner/writers. Configured report destinations are used as defaults when explicit extension/task report settings are absent; explicit Gradle adapter settings override config values. When execution-availability issues exist, it logs `javaspec:` warning diagnostics plus the Gradle classpath element count. The diagnostics do not add source compilation and do not change `failOnFailure` or build-failure semantics. It delegates to `JavaspecLauncher` without `System.exit`; projects under test do not need JUnit.

## Optional JUnit Platform Engine

Phase 17 provides a standalone optional JUnit Platform engine artifact at `javaspec-junit-platform-engine/`. It is not registered as a root Maven module and remains outside the zero-runtime-dependency core artifact. For local standalone engine verification, install the current core first, then verify the engine:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
```

The engine artifact is `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging `jar`, Java source/target `1.8`, using Java 8-compatible JUnit Platform `1.10.2` rather than JUnit Platform 6/JUnit 6. It registers `org.javaspec.junit.platform.JavaspecTestEngine` through ServiceLoader with engine id `javaspec`.

To opt in, place the engine artifact on the JUnit Platform test runtime classpath used by the chosen IDE/CI/build launcher. Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`; configuration parameters include `javaspec.configFile`, `javaspec.suite`, `javaspec.specDir`/`javaspec.specRoot`, `javaspec.classFilters`/`classFilter`/`class`, `javaspec.exampleFilters`/`exampleFilter`/`example`, and `javaspec.stopOnFailure`. Class, package, method, and unique-id selectors act as filters over canonical discovery results. The engine retains stable unique-id segments `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and `[example:<methodName>]`, preserves MethodSource behavior, and aligns descriptor reporting to stable ids. Execution delegates to the canonical no-JUnit `JavaspecLauncher`, maps passed results to successful, failed/broken results to failed/error-style events, and skipped or pending results to `executionSkipped` with the existing unique-id/descriptors preserved; pending skip reasons are prefixed with `Pending:`. The engine avoids `System.exit` and does not require changes to javaspec spec authoring style. It still relies on the JUnit Platform test runtime classpath to contain compiled spec classes, production classes, and dependencies; it does not compile source/spec files itself.

Projects that do not opt into this engine still have no JUnit dependency and can keep using CLI, programmatic, Maven plugin, or Gradle plugin no-JUnit execution paths.

## Interface Doubles

`org.javaspec.doubles` provides zero-runtime-dependency interface doubles built with `java.lang.reflect.Proxy`. Create a proxy with `Doubles.create(Foo.class)`, `Doubles.of(Foo.class)`, or `Doubles.proxy(Foo.class)`, or create a typed handle with `Doubles.interfaceDouble(Foo.class)`. `ObjectBehavior` also exposes `doubleFor`, `interfaceDouble`, `doubleControl`/`inspectDouble`, call-history, call-count, and called/not-called/count assertion helpers.

Doubles support ordinary interfaces only. Unsupported inputs are rejected with clear diagnostics: `null`, primitives, arrays, annotations, enums, concrete classes, and final classes. Stubs can match any arguments by method name or exact arguments with `when(...).thenReturn(...)`, `returns(...)`, and `returnsFor(...)`; exact argument comparison handles `null` values and array contents. Unstubbed interface methods return Java defaults and `void` methods are no-ops. Calls are recorded as `Call` snapshots and can be inspected or verified through `called()`, `notCalled()`, `calledOnce()`, `times(n)`, and convenience methods. `toString`, `equals`, and `hashCode` are deterministic.

Limitations: no concrete class/final class/static/constructor doubles, no wildcard argument matchers, no exception/callback stubbing, no bytecode-library integration in core, and default interface methods are not invoked.

## Java LTS Targeting Concept

javaspec runs as a Java 8-compatible binary while understanding target profiles:

| LTS version | Profile key | Runtime strategy |
|---|---|---|
| Java 8 | `java8` | Direct use of Java 8 public APIs is allowed and representative data-structure symbols are cataloged. |
| Java 11 | `java11` | APIs introduced in 9-11 are stored as metadata and reflected only when running on a compatible JDK. |
| Java 17 | `java17` | APIs introduced in 12-17 are metadata-driven; stream additions, records, and sealed types are modeled without compile-time linkage. |
| Java 21 | `java21` | Sequenced collection APIs are modeled by names and reflected conditionally. |
| Java 25 | `java25` | Stream gatherer metadata is implemented for `java.util.stream.Gatherer`, nested gatherer types, and `java.util.stream.Gatherers`; runtime availability is still probed reflectively. |

The implemented catalog is based on the Java data-structure research in [`docs/research/java-lts-data-structures.md`](docs/research/java-lts-data-structures.md).

## Future Usage Vision

The implemented MVP covers `describe`/`desc` for PHPSpec-style spec/support skeleton generation, a `run` command that maps discovered `*Spec.java` files under the active naming convention to described production classes, interfaces, enums, annotations, records, sealed classes, and sealed interfaces, Phase 4 configuration files for defaults, suite path selection, package-prefix naming, class/example filters, selected profile/formatter defaults, bootstrap metadata, and constructor-policy defaults, the Phase 5/6 reflection runner for executable examples that are already compiled and available on the effective or explicit classloader, the Phase 7 matcher/expectation expansion, Phase 8 interface doubles, Phase 9 run controls for dry-run planning, stop-on-failure, progress/pretty formatting, profile selection, and verbose diagnostics, the Phase 10 interface-style method generation increment for missing/existing ordinary interfaces and annotations plus missing sealed-interface skeletons, the Phase 11 formatter/reporting/extension increment with JSON run reports and minimal programmatic extension contracts, the Phase 14 no-JUnit integration foundation with programmatic no-`System.exit` invocation, explicit CLI classpath input, and JUnit XML-compatible reports, the Phase 15 optional Maven plugin, Phase 16 optional Gradle plugin, Phase 17 optional JUnit Platform engine adapters, Phase 18 stable identifier/source-location/report polish, Phase 19 post-roadmap release/CI hardening through `scripts/verify-all.sh` and `.github/workflows/ci.yml`, Phase 20 release-readiness scaffolding through `CHANGELOG.md`, `RELEASING.md`, `scripts/check-version-alignment.sh`, Maven `release-artifacts` source/javadoc packaging checks, and Gradle source/javadoc jar readiness, Phase 21 adoption assets through standalone examples, `scripts/verify-examples.sh`, report schema documentation, and golden reports, Phase 22 explicit skipped/pending semantics through zero-dependency markers/signals and report/adapter mappings, Phase 23 classpath/execution availability diagnostics through enriched not-executable reasons, `RunDiagnostics`, and CLI/Maven/Gradle diagnostic output, and Phase 24 configuration-level report destinations for reusable JSON/JUnit XML-compatible output paths. The original numbered roadmap is complete through Phase 18; Phase 19, Phase 20, Phase 21, Phase 22, Phase 23, and Phase 24 add post-roadmap hardening/readiness/adoption/execution-semantics/diagnostics/configuration increments, and future feature work should be tracked as new roadmap/backlog items. JUnit remains optional: the core runner stays in javaspec, no-JUnit CLI/programmatic/Maven/Gradle execution is first-class, the JUnit Platform engine is a separate optional adapter, and the core runtime must not gain a JUnit dependency. The MIT `LICENSE` and confirmed maintainer metadata are resolved, but public publication remains postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved.

The stabilized discovery and execution flow honors configured suite package prefixes through `SpecNamingConvention`, maps described classes to generated specs/support files with those prefixes, selects suites with `--suite`, filters classes with repeatable `--class <qualified-or-simple-name>`, filters examples with repeatable `--example <method-name|display-name|order-index>`, and applies `run`-only controls before generation and reflection execution. The broader intended workflow remains:

1. Describe a Java type, generating or locating a matching specification class.
2. Run specs.
3. Let the run phase ask whether to generate missing production code, or answer yes non-interactively with `--generate`.
4. Run examples expressed with javaspec expectations.
5. Use interface collaborators/doubles where possible without runtime dependencies.
6. Render human-readable failures and execution-availability diagnostics, and, when requested, write JSON and/or JUnit XML-compatible runner reports with stable exit codes, stable ids, pending counts, and source file/line metadata where available.

## Documentation Map

- [`docs/usermanual/Home.md`](docs/usermanual/Home.md) — user manual with CLI examples and PHPSpec-to-Java migration notes.
- [`CHANGELOG.md`](CHANGELOG.md) — release-change log scaffold.
- [`RELEASING.md`](RELEASING.md) — local release-readiness checklist and explicit public-publication blockers.
- [`javaspec-gradle-plugin/README.md`](javaspec-gradle-plugin/README.md) — standalone optional Gradle plugin build, usage, and classpath diagnostic notes.
- [`javaspec-junit-platform-engine/README.md`](javaspec-junit-platform-engine/README.md) — standalone optional JUnit Platform engine build and usage notes.
- [`PLAN.md`](PLAN.md) — phased implementation plan and requirement traceability.
- [`examples/README.md`](examples/README.md) — standalone consumer examples for Maven plugin, Gradle plugin, and JUnit Platform engine adoption paths.
- [`docs/schemas/run-report-v1.schema.json`](docs/schemas/run-report-v1.schema.json) — JSON schema for `schemaVersion` 1 run reports.
- [`docs/examples/reports/`](docs/examples/reports/) — golden passing and pending JSON/JUnit XML-compatible report examples.
- [`docs/test-report.md`](docs/test-report.md) — Phase 12 compatibility matrix plus Phase 14, Phase 15, Phase 16, Phase 17, Phase 18, Phase 19, Phase 20, Phase 21, and Phase 22 verification results.
- [`docs/arc42/01-introduction-and-goals.md`](docs/arc42/01-introduction-and-goals.md) — goals and quality requirements.
- [`docs/arc42/02-constraints.md`](docs/arc42/02-constraints.md) — technical and organizational constraints.
- [`docs/arc42/03-context-and-scope.md`](docs/arc42/03-context-and-scope.md) — system context and boundaries.
- [`docs/arc42/04-solution-strategy.md`](docs/arc42/04-solution-strategy.md) — architecture strategy.
- [`docs/arc42/05-building-block-view.md`](docs/arc42/05-building-block-view.md) — building-block notes, including the generation boundary, CLI run controls, invocation API, optional Maven/Gradle/JUnit Platform adapters, formatter/reporting/extension contracts, configuration model, profile catalog, and compatibility boundary.
- [`docs/arc42/06-runtime-view.md`](docs/arc42/06-runtime-view.md) — implemented runtime scenarios for describe, run, execution, generation, doubles, reporting, explicit classpath, programmatic invocation, optional Maven/Gradle/JUnit Platform execution, and profile probes.
- [`docs/arc42/07-deployment-view.md`](docs/arc42/07-deployment-view.md) — runtime artifact, optional Maven/Gradle/JUnit Platform artifacts, deployment environments, classpath requirements, and verification/dependency constraints.
- [`docs/arc42/08-concepts.md`](docs/arc42/08-concepts.md) — cross-cutting concepts, including PHPSpec-to-Java mapping, no-JUnit invocation, optional adapters, reporting, and extension boundaries.
- [`docs/arc42/09-architecture-decisions.md`](docs/arc42/09-architecture-decisions.md) — ADR index.
- [`docs/arc42/10-quality-requirements.md`](docs/arc42/10-quality-requirements.md) — quality requirements with Phase 12, Phase 14, Phase 15, Phase 16, Phase 17, Phase 18, Phase 19, Phase 20, Phase 21, and Phase 22 evidence.
- [`docs/arc42/11-risks-and-technical-debt.md`](docs/arc42/11-risks-and-technical-debt.md) — current limitations, risks, and mitigation actions.
- [`docs/arc42/12-glossary.md`](docs/arc42/12-glossary.md) — glossary of javaspec architecture and user-facing terms.
- [`docs/adr/0001-java-8-baseline-with-lts-target-profiles.md`](docs/adr/0001-java-8-baseline-with-lts-target-profiles.md) — Java compatibility decision.
- [`docs/adr/0002-zero-runtime-dependency-policy.md`](docs/adr/0002-zero-runtime-dependency-policy.md) — dependency policy decision.
- [`docs/adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md`](docs/adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md) — first-MVP PHPSpec-style describe/run generator split.
- [`docs/adr/0004-course-correction-construction-defaults-typed-matcher-proxies-and-method-generators.md`](docs/adr/0004-course-correction-construction-defaults-typed-matcher-proxies-and-method-generators.md) — implemented construction, typed matcher proxy, and method-generator correction.
- [`docs/adr/0005-restricted-line-based-configuration-format.md`](docs/adr/0005-restricted-line-based-configuration-format.md) — restricted zero-dependency configuration format decision.
- [`docs/adr/0006-classpath-reflection-runner.md`](docs/adr/0006-classpath-reflection-runner.md) — classpath reflection runner decision for the Phase 5/6 MVP.
- [`docs/adr/0007-jdk-proxy-only-interface-doubles.md`](docs/adr/0007-jdk-proxy-only-interface-doubles.md) — JDK proxy-only interface doubles decision for the Phase 8 MVP.
- [`docs/adr/0008-run-only-controls-and-non-mutating-dry-run-planning.md`](docs/adr/0008-run-only-controls-and-non-mutating-dry-run-planning.md) — run-only controls and dry-run planning decision for Phase 9.
- [`docs/adr/0009-interface-style-method-generation-and-sealed-interface-update-deferral.md`](docs/adr/0009-interface-style-method-generation-and-sealed-interface-update-deferral.md) — Phase 10 interface/annotation/sealed-interface generation decision.
- [`docs/adr/0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md`](docs/adr/0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md) — Phase 11 formatter, JSON reporting, and programmatic extension boundary decision.
- [`docs/adr/0011-optional-junit-adapter-and-canonical-javaspec-runner.md`](docs/adr/0011-optional-junit-adapter-and-canonical-javaspec-runner.md) — canonical javaspec runner, no-JUnit integration foundation, optional Maven/Gradle plugin adapters, and optional JUnit Platform engine decision.
- [`docs/adr/0012-non-disruptive-aggregate-release-ci-verification.md`](docs/adr/0012-non-disruptive-aggregate-release-ci-verification.md) — non-disruptive aggregate release/CI verification decision instead of mandatory Maven multi-module conversion.
- [`docs/adr/0013-release-readiness-scaffolding-with-publication-blockers.md`](docs/adr/0013-release-readiness-scaffolding-with-publication-blockers.md) — release-readiness scaffolding decision with resolved MIT license/maintainer metadata and postponed signing/portal publication.
- [`docs/adr/0014-standalone-adoption-assets-and-default-examples-verification.md`](docs/adr/0014-standalone-adoption-assets-and-default-examples-verification.md) — standalone adoption assets and default examples verification decision.
- [`docs/adr/0015-explicit-skipped-and-pending-semantics.md`](docs/adr/0015-explicit-skipped-and-pending-semantics.md) — explicit skipped and pending semantics decision.
- [`docs/adr/0016-classpath-execution-availability-diagnostics.md`](docs/adr/0016-classpath-execution-availability-diagnostics.md) — classpath execution availability diagnostics without integrated compilation decision.
- [`docs/adr/0017-configuration-level-report-destinations.md`](docs/adr/0017-configuration-level-report-destinations.md) — configuration-level report destinations and report destination precedence decision.
- [`docs/research/phpspec-feature-inventory.md`](docs/research/phpspec-feature-inventory.md) — phpspec feature inventory for the Java port.
