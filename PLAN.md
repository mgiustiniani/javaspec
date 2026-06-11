# javaspec Implementation Plan

This plan defines the initial delivery path for javaspec, a Java 8-compatible, zero-runtime-dependency Java port inspired by phpspec.

## Current Implementation Status — Implemented and Verified

Phases 2 through 18 of the original numbered roadmap are implemented and verified. Phase 19 through Phase 29 are post-roadmap hardening/readiness/adoption/execution-semantics increments that are implemented and verified locally. Remote GitHub Actions success is user-/maintainer-confirmed only for pushed HEAD `5088e96` on `develop` after Phase 20/21/22 were pushed; no remote CI success is claimed here for later local commits through current HEAD `ddd7eb9`. Phase 12 compatibility and quality verification is fully complete through the Distrobox multi-JDK matrix for Java 8, 11, 17, 21, and 25; Phase 14 adds the no-JUnit integration foundation for programmatic invocation, explicit classpath execution, and JUnit XML-compatible reporting; Phase 15 adds a standalone optional Maven plugin adapter over the canonical runner; Phase 16 adds a standalone optional Gradle plugin adapter over the canonical runner; Phase 17 adds a standalone optional JUnit Platform engine adapter over the canonical runner; Phase 18 adds stable identifier, source-location, and report-consistency polish for IDE/CI consumers; Phase 19 adds non-disruptive aggregate release verification and a GitHub Actions CI workflow while preserving the current standalone-adapter architecture; Phase 20 adds release-readiness scaffolding with MIT license and maintainer metadata resolved, but without public publishing, signing, secrets, portal credentials, runtime dependencies, or Maven multi-module conversion; Phase 21 adds standalone adoption examples plus report-schema/golden-report documentation; Phase 22 adds zero-dependency explicit skipped/pending semantics; Phase 23 adds execution-availability diagnostics; Phase 24 adds configuration-level report destinations; Phase 25 adds ServiceLoader external formatter/extension discovery for CLI and Gradle run classloaders; Phase 26 adds target-profile enforcement before generation/update writes; Phase 27 adds bootstrap hook execution before examples; Phase 28 strengthens interface doubles; and Phase 29 adds CLI-only opt-in source/spec compilation.

- Phase 2 implemented the Java 8 Maven project, zero-runtime-dependency guard, PHPSpec-style `describe`/`run` split, specification/support skeletons, and gated production type/method generation.
- Phase 3 implemented Java LTS target profiles `java8`, `java11`, `java17`, and `java21`, plus the forward-looking `java25` profile, the profile catalog, API-symbol metadata, target-profile compatibility checks, and reflection-only API availability probes.
- Phase 4 implemented the zero-runtime-dependency line-based configuration format, `--config <file>` and `--suite <name>` integration, suite-level spec/source directories, suite package prefixes, naming convention integration, and suite/class/example discovery filters.
- The Phase 5/6 MVP implemented `org.javaspec.runner`, keeps `javaspec run` discovery/generation/update behavior, and executes filtered discovered examples when compiled spec classes are available on the effective classloader.
- Runner behavior: existing `DiscoveredSpec`/`SpecExample` metadata remains the execution source, so suite/class/example filters remain effective; each example gets a fresh spec instance; optional public no-arg `let()` runs before each example and optional public no-arg `letGo()` runs after each example.
- Result states are `PASSED`, `FAILED` for `AssertionError`, `BROKEN` for non-assertion throwables/lifecycle/reflection errors, `SKIPPED` for non-loadable spec classes, missing reflected example methods, or explicit skips, and `PENDING` for explicit pending examples. The CLI exits `1` for failed or broken executable examples; passed/skipped/pending-only runs remain successful.
- Phase 7 expanded `Matchable` with negated equality aliases, type/instance aliases, `shouldImplement`, string negations, count/empty helpers for arrays/collections/maps/character sequences/iterables, and map key/value helpers.
- Phase 7 expanded `ObjectBehavior` direct convenience assertion methods that delegate through `match(actual)`, kept `MatcherRegistry` zero-dependency with a default negated-equality matcher, and updated `SpecDiscovery` so expanded chained matcher names participate in method-discovery/default-return inference where applicable.
- Phase 8 added zero-runtime-dependency interface doubles under `org.javaspec.doubles` using JDK dynamic proxies. The MVP supports ordinary interface doubles, return-value stubbing by method name or exact arguments, null and array-content argument matching, call history, called/not-called/exact-count verification, deterministic `toString`/`equals`/`hashCode`, Java default returns for unstubbed methods, and `ObjectBehavior` double convenience APIs.
- Phase 9 expanded `javaspec run` with run-only controls: `--dry-run`, `--stop-on-failure`, `--formatter <progress|pretty>`, `--profile <java8|java11|java17|java21|java25>`, and `--verbose`. Dry-run never writes or prompts and reports pending would-generate/would-update actions; stop-on-failure stops after the first FAILED or BROKEN executable example; progress output is concise and summary-oriented; pretty output prints per-example status lines plus details; CLI formatter/profile options override valid config/default selections; verbose output prints selected run settings.
- Phase 10 implemented the current advanced code-generation increment for interface-style methods. Missing ordinary interface skeletons render discovered non-static method declarations and skip static descriptors; missing annotation skeletons render compatible no-arg non-static elements and skip incompatible descriptors; missing sealed-interface skeletons render root declarations plus nested permitted implementation bodies with Java default returns; existing ordinary interfaces and annotations can receive missing declarations/elements source-preservingly and idempotently; existing sealed-interface source updates are intentionally deferred.
- Phase 11 implemented public zero-dependency run formatter contracts and deterministic built-in formatter registration, preserving compatible `progress` and `pretty` CLI output; added minimal programmatic extension contracts through `JavaspecExtension`/`Extension` and `ExtensionContext`; and added `javaspec run --report <file>` plus alias `--report-file <file>` for UTF-8 JSON runner reports with `schemaVersion` 1, summary counts, specs, examples, nullable failures, throwable class/message, and stack trace lines.
- Phase 11 reporting behavior: `--report` is run-only and rejected by `describe`; `--verbose` prints the report path when specified; no-spec runs write a valid empty report; normal passing/failing/broken/skipped/pending runs write the report after summary rendering; failed or broken executable examples still exit `1` after the report write; dry-run pending generation/update exits before execution and does not write a report; report write failures print I/O diagnostics and exit `70`.
- Phase 14 implemented the no-JUnit integration foundation under `org.javaspec.invocation`: `JavaspecInvocation`, `JavaspecLauncher`, `JavaspecInvocationResult`, and `JavaspecExitCode` provide a programmatic no-`System.exit` invocation API over canonical discovery, `SpecRunner`, and `RunResult`; structured results expose discovered specs, runner results, success/failure state, and deterministic exit-code mapping where passing, skipped/pending-only, and no-spec runs are `0`, while failed or broken runs are `1`.
- Phase 14 expanded `javaspec run` with explicit classpath input and JUnit XML-compatible reports: `--classpath <path-list>` uses `File.pathSeparator`; `--classpath-file <file>` reads UTF-8 non-empty, non-comment entries; both options are run-only and rejected by `describe`; verbose output lists explicit classpath entries; the selected explicit classloader is used for production type existence checks and spec execution. `--junit-xml <file>` and alias `--junit-xml-file <file>` write dependency-free JUnit XML-compatible reports through `JUnitXmlReportWriter`; JSON and JUnit XML reports can be requested together; no-spec and executed run paths write requested reports after output, failed/broken examples still exit `1` after report writes, dry-run pending generation/update exits before execution and writes no reports, and report I/O failures exit `70` with path diagnostics.
- Phase 15 implemented `javaspec-maven-plugin/` as a standalone optional Maven plugin artifact, intentionally not registered as a root module so repository-root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact. The plugin packages `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin`, exposes goal prefix `javaspec`, provides `javaspec:run` bound by default to `verify`, requires test dependency resolution, uses the Maven test classpath, supports config/suite/specDir/specRoot selection, class/example filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible reports, Maven logging with pending counts, and delegates to the canonical no-JUnit `JavaspecLauncher` without `System.exit`.
- Phase 16 implemented `javaspec-gradle-plugin/` as a standalone optional Gradle plugin artifact, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact. The plugin id is `org.javaspec`; it registers extension `javaspec` and task `javaspecRun` in Gradle's `verification` group, defaults to the Java plugin `test` source set runtime classpath and `testClasses` dependency when available, supports skip/fail/stop controls, config/suite/specDir/specRoot, class/example filters, built-in `progress` and `pretty` formatters, JSON and JUnit XML-compatible report aliases, pending-aware summaries/reports inherited from core, logs through Gradle, and delegates to the canonical no-JUnit `JavaspecLauncher` without `System.exit`.
- Phase 17 implemented `javaspec-junit-platform-engine/` as a standalone optional JUnit Platform engine artifact, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact. The artifact is `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging `jar`, Java source/target `1.8`, and uses Java 8-compatible JUnit Platform `1.10.2` rather than JUnit Platform 6/JUnit 6. `JavaspecTestEngine` is discovered through ServiceLoader with engine id `javaspec`, uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`, supports configuration parameters and JUnit Platform class/package/method/unique-id selectors as filters over canonical discovery results, delegates execution to the canonical no-JUnit `JavaspecLauncher`, maps javaspec result states to JUnit Platform listener events including pending-to-skipped mapping, avoids `System.exit`, and does not require changes to javaspec spec authoring style.
- Phase 18 implemented an incremental IDE/CI polish focused on stable identifiers, source locations, and report consistency. Stable id aliases are available on `ExampleResult` (`id()`/`stableId()`/`getId()`/`getStableId()`, with `<specQualifiedName>#<methodName>` semantics matching `fullName()`), `SpecResult` (derived from spec qualified name), and `DiscoveredSpec` (derived from spec qualified name). `SpecExample` carries a 1-based `sourceLine`; discovery computes method declaration lines; `ExampleResult` carries source file path and source line when created from discovered specs/examples; and `SpecResult` carries the spec source file path.
- Phase 18 report updates are additive: JSON reports include spec `id`, `stableId`, and `sourceFile` plus example `id`, `stableId`, `fullName`, and `source { file, line }` while preserving existing fields; JUnit XML-compatible reports include `file` and `line` attributes on `<testcase>` when source data is available while preserving dependency-free JUnit XML-compatible output. The optional JUnit Platform engine retained its stable unique-id shape and MethodSource behavior, with descriptor reporting aligned to stable ids.
- Phase 19 implemented post-roadmap release/CI hardening without converting the repository to Maven multi-module. Root `mvn verify` remains a core-only build/audit. Standalone optional Maven plugin, Gradle plugin, and JUnit Platform engine artifacts remain outside the root Maven reactor. `scripts/verify-all.sh` provides an aggregate local verification path that verifies the core, audits runtime dependencies, installs the current core snapshot, verifies standalone adapters, audits adapter runtime dependencies, and supports `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, and explicit `JAVASPEC_SKIP_GRADLE=1`. `.github/workflows/ci.yml` adds GitHub Actions jobs for a Java 8/11/17/21/25 core matrix and Java 21 full verification through the aggregate script. No publishing, signing, secrets, or deployment behavior was added; remote GitHub Actions success was later user-/maintainer-confirmed for HEAD `4d30e63` on `develop`.
- Phase 20 implemented release-readiness scaffolding only. It added executable `scripts/check-version-alignment.sh`, updated `scripts/verify-all.sh` to run version alignment first, added `CHANGELOG.md` and `RELEASING.md`, copied the confirmed MIT `LICENSE` exactly from `origin/main`, added MIT license metadata and confirmed maintainer/developer metadata for `Mario Giustiniani <mariogiustiniani@gmail.com>` to the root, Maven plugin, and JUnit Platform engine POMs and Gradle generated POM metadata, added safe URL/SCM/GitHub Issues metadata and Maven `release-artifacts` profiles for sources/javadocs only in the root, Maven plugin, and JUnit Platform engine POMs, and added Gradle `maven-publish` plus source/javadoc jar readiness and safe POM URL/SCM/issues metadata. It did not run or configure actual publishing/deployment/signing, secrets, Central Portal publication, Gradle Plugin Portal publication/credentials, runtime dependencies, or Maven multi-module conversion. `scripts/check-version-alignment.sh` verifies root Maven version, Maven plugin version, JUnit engine version, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` alignment.
- Phase 21 implemented adoption assets only. It added the JSON report schema `docs/schemas/run-report-v1.schema.json`, golden report examples under `docs/examples/reports/`, and standalone consumer examples under `examples/` for the Maven plugin, Gradle plugin, and JUnit Platform engine paths. The examples are intentionally standalone and are not root Maven modules. It added executable `scripts/verify-examples.sh`, which installs the local core/plugin/engine snapshots needed by the examples, verifies Maven, JUnit Platform, and Gradle example execution, and asserts generated report markers including `schemaVersion`, stable id `spec.com.example.CalculatorSpec#it_adds_two_numbers`, `PASSED`, and source `line=11`. `scripts/verify-all.sh` now runs standalone examples verification by default after core/adapters; `JAVASPEC_SKIP_EXAMPLES=1` skips that section explicitly, and `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` skips only the Gradle example inside `verify-examples.sh`. Phase 21 did not change core production/test Java files and did not run or configure public publish/deploy/signing commands.
- Phase 22 implemented explicit skipped/pending semantics. It added zero-dependency runtime method annotations `@Skip` and `@Pending`, unchecked signals `SkipExampleException` and `PendingExampleException`, `ObjectBehavior.skip()`/`skip(String)`/`pending()`/`pending(String)` helpers, distinct `ExampleStatus.PENDING` and pending counts, pending-aware formatter summaries, JSON/JUnit XML-compatible report behavior, Maven summary logging, Gradle inherited report behavior, and JUnit Platform `executionSkipped` mapping for pending examples. `@Skip` takes precedence over `@Pending`; annotation-based skip/pending does not instantiate specs or run lifecycle/body code; runtime skip/pending from `let()` or examples runs `letGo()` and becomes BROKEN if `letGo()` fails.
- Phase 23 implemented classpath/execution availability diagnostics. It enriches non-executable spec/example reasons when source discovery finds specs that the runner classloader cannot execute, exposes deterministic `RunDiagnostics.executionAvailabilityLines(RunResult)` lines, prints CLI `Execution diagnostics:` only for availability issues, and logs Maven/Gradle `javaspec:` warnings with classpath element counts without changing exit-code or build-failure semantics.
- Phase 24 implemented configuration-level report destinations. Top-level config keys can provide JSON and JUnit XML-compatible report defaults; CLI report options and explicit Maven/Gradle adapter report settings take precedence; report schemas, writers, exit semantics, dry-run pending behavior, no-spec behavior, and standalone adapter boundaries remain unchanged.
- Phase 25 implemented ServiceLoader external formatter/extension discovery for CLI and Gradle run classloaders. `JavaspecExtensionLoader` loads built-in formatters first, then `RunFormatter`, `JavaspecExtension`, and alias `Extension` providers from the effective run classloader; duplicate extension implementation classes are configured once. Maven plugin formatter output controls, JUnit Platform formatter controls, configuration-driven activation, package scanning, plugin lookup, report schema/content changes, and runtime dependency additions remain out of scope.
- Phase 26 implemented target-profile enforcement before generation/update writes. `ProfileEnforcement`, `ProfileEnforcementReport`, and `ProfileViolation` check described type kinds and resolvable generated method signature API owners against the effective `TargetProfile`; violations exit before related-spec generation, support/source writes, prompts, execution, or report writing. Enforcement is conservative and source/generation-scoped, not compiler-grade integrated compilation.
- Phase 27 implemented bootstrap hook execution before examples. Configured top-level hooks run before selected-suite hooks, order and duplicates are preserved, hook classes load from the effective run classloader/classpath, and hooks receive immutable `BootstrapContext` data. CLI bootstrap failures exit `64` with `Error: Bootstrap execution failed`, skip examples, and write no reports. Bootstrap does not add ServiceLoader hook discovery, script engines, package scanning, dependency resolution, adapter-integrated compilation, or runtime dependencies.
- Phase 28 implemented stronger interface doubles. `ArgumentMatcher`, `ArgumentMatchers`, matcher aliases, argument-constrained stub priority, throwing stubs, and answer callbacks are available while preserving ordinary-interface-only JDK dynamic proxies, Java 8 compatibility, zero runtime dependencies, and no concrete/static/final/constructor/bytecode mocking.
- Phase 29 implemented CLI-only opt-in source/spec compilation. `javaspec run --compile` compiles source/spec `.java` files with the current JDK `javax.tools.JavaCompiler`; `--compile-output <dir>` selects the output directory and implies compilation. Compilation runs after discovery/profile/generation/update and before bootstrap/examples, no-spec and dry-run paths skip compilation, compiler-unavailable requests exit `64`, compile failures print `Compilation failed:`, exit `1`, and write no reports. No default-run behavior, config key, Maven/Gradle/JUnit adapter behavior, dependency resolution, incremental cache, forked `javac`, source-level/release management, report schema, or runtime dependency change was added.
- Known limitations: default CLI runs, programmatic invocation, and optional Maven/Gradle/JUnit Platform adapters remain classpath/reflection based; source-only or otherwise unavailable spec classes are skipped/not executable until compiled classes are present on the effective classloader, selected explicit classloader, adapter classpath, JUnit Platform runtime classpath, or a CLI run opts into successful `--compile` / `--compile-output <dir>` compilation. Phase 29 compilation is CLI-only and current-JDK `javax.tools.JavaCompiler` based; it does not add default compilation, config keys, dependency resolution, incremental caches, forked `javac`, source-level/release management, adapter behavior, report schema changes, or runtime dependencies. The optional Maven plugin, Gradle plugin, and JUnit Platform engine supply integration paths but are standalone artifacts. They are not verified by root `mvn verify` alone; use `scripts/verify-all.sh` or the explicit standalone verification commands after installing the current core snapshot. Phase 21 examples are standalone consumer projects, not root modules, and currently require local snapshot installation rather than public artifact resolution. `scripts/verify-all.sh` includes standalone examples by default, with explicit opt-outs through `JAVASPEC_SKIP_EXAMPLES=1` for all examples or `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` for the Gradle example inside `scripts/verify-examples.sh`. Projects that do not opt into the optional JUnit Platform engine still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths. Public publication remains intentionally postponed: the MIT `LICENSE` and confirmed maintainer metadata are resolved, but GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval remain unresolved and unimplemented. The Maven `release-artifacts` profile and Gradle source/javadoc jar readiness create local sources/javadocs only; they do not sign, stage, deploy, or publish. Target-profile enforcement is implemented before generation/update writes but remains conservative and source/generation-scoped rather than compiler-grade integrated compilation. ServiceLoader formatter/extension discovery is implemented for CLI and Gradle run classloaders, while configuration-driven extension activation, package scanning, plugin lookup, Maven plugin formatter controls, JUnit Platform formatter controls, and automatic classpath repair remain unimplemented. Bootstrap hooks execute explicit compiled Java classes from configuration; ServiceLoader hook discovery, script engines, package scanning, dependency resolution, adapter-integrated compilation, and runtime dependencies remain out of scope. JSON reporting remains schemaVersion 1 with Phase 18 additive identifier/source fields and Phase 22 additive pending counts/statuses; JUnit XML-compatible reporting remains intentionally minimal with additive `<testcase file="..." line="...">` attributes only when source data is available plus skipped/pending `<skipped>` mapping. Report destinations can come from CLI options, top-level config defaults, or adapter settings, with explicit entry-point settings taking precedence and report schemas/writers unchanged. Count and emptiness checks on a generic `Iterable` consume the iterable and can hang on infinite iterables. Existing sealed-interface source updates are skipped until nested permitted implementations can be updated source-preservingly. Interface doubles now support argument matchers, throwing stubs, and answer callbacks, but remain ordinary-interface-only JDK proxies with no concrete/static/final/constructor/bytecode mocking or default-interface-method invocation.
- Phase 12 Distrobox multi-JDK verification completed on 2026-06-03 with Distrobox `1.8.2.5` and Podman `5.8.2`: Java 8 (`1.8.0_492`), Java 11 (`11.0.31`), Java 17 (`17.0.19`), Java 21 (`21.0.11 LTS`), and Java 25 (`25.0.3 LTS`) containers each passed `mvn clean` and `mvn verify` with 364 tests, 0 failures, 0 errors, and 0 skipped.
- JDK 17+ matrix runs emitted only the expected `-source 8` / `-target 1.8` bootstrap/obsolete-option warnings.
- Runtime dependency verification completed in `javaspec-jdk25-matrix`: `mvn dependency:tree -Dscope=runtime` passed and showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.
- Java 25 Gatherer runtime verification completed in `javaspec-jdk25-matrix`: reflection probing passed for `java.util.stream.Gatherer`, `Gatherer$Downstream`, `Gatherer$Integrator`, `Gatherer$Integrator$Greedy`, and `java.util.stream.Gatherers`.
- Phase 14 tester verification completed on 2026-06-03: targeted Phase 14 tests passed with `mvn -q -Dtest=JavaspecLauncherTest,JUnitXmlReportWriterTest,MainPhase14CliTest test` (18 tests); full `mvn verify` passed with 382 tests, 0 failures, 0 errors, and 0 skipped; and `mvn dependency:tree -Dscope=runtime` passed with only the root artifact in runtime scope.
- Phase 15 tester verification completed on 2026-06-03: root `mvn -q verify` passed with 382 core tests; root `mvn -q -DskipTests install` passed to install the current core for standalone plugin verification; `mvn -q -f javaspec-maven-plugin/pom.xml -Dtest=JavaspecRunMojoTest test` passed with 12 plugin tests; `mvn -q -f javaspec-maven-plugin/pom.xml verify` passed with 12 plugin tests; root `mvn dependency:tree -Dscope=runtime` passed with only `org.javaspec:javaspec`; and plugin `mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime` passed with the plugin plus compile-scope core `org.javaspec:javaspec` only.
- Phase 16 tester verification completed on 2026-06-03: `mvn -q -DskipTests install` passed; `mvn -q verify` passed; `mvn dependency:tree -Dscope=runtime` passed with only `org.javaspec:javaspec`; `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin test` passed with 11 plugin tests; `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin build` passed; `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath` passed showing runtimeClasspath only `org.javaspec:javaspec:0.1.0-SNAPSHOT`; and `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration testRuntimeClasspath` passed showing javaspec, JUnit, and Hamcrest only. A cached Gradle 7.4.2 command was attempted but blocked by the installed Java 21 runtime with `Unsupported class file major version 65`; this is documented as an environment/tooling compatibility blocker for that cached executable, not as a javaspec feature failure.
- Phase 17 tester verification completed on 2026-06-04: `mvn -q -DskipTests install` passed; `mvn -q verify` passed with root Surefire reporting 382 tests, 0 failures, 0 errors, and 0 skipped; `mvn -q -f javaspec-junit-platform-engine/pom.xml -Dtest=JavaspecTestEnginePhase17Test test` passed with 12 tests, 0 failures, 0 errors, and 0 skipped; `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` passed with 12 tests, 0 failures, 0 errors, and 0 skipped; root `mvn dependency:tree -Dscope=runtime` passed with only `org.javaspec:javaspec`; and engine `mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime` passed with runtime dependencies core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`, with no runtime `junit-jupiter`, `junit-platform-launcher`, or `junit-platform-testkit`. Blockers: none.
- Phase 18 tester verification completed on 2026-06-04: targeted changed core tests passed with `mvn -q -Dtest=SpecDiscoveryNamingTest,SpecRunnerTest,RunReportWriterTest,JUnitXmlReportWriterTest,MainPhase11ReportCliTest,MainPhase14CliTest test`; full `mvn -q test` passed with 386 tests, 0 failures, 0 errors, and 0 skipped; full `mvn -q verify` passed with 386 tests, 0 failures, 0 errors, and 0 skipped; root runtime dependency audit passed with only `org.javaspec:javaspec`; root `mvn -q install` passed; standalone Maven plugin `verify` passed with 12 tests; standalone JUnit Platform engine `verify` passed with 12 tests; standalone Gradle plugin `clean test build` passed with 11 tests; and the Gradle plugin runtimeClasspath audit passed.
- Phase 19 tester verification completed on 2026-06-04 with no blockers and no tester file modifications: `bash -n scripts/verify-all.sh` passed and the script is executable; PyYAML parsed `.github/workflows/ci.yml` as a valid YAML mapping with top-level keys `name`, `on`, and `jobs` and jobs `core` and `full-verification` (actionlint/yamllint/yq were unavailable); `git diff --check`, `git diff --cached --check`, and a temp-index whitespace check including untracked `.github/` and `scripts/` passed; `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed. The aggregate script executed root `mvn -q verify` with 386 tests, root runtime audit with only `org.javaspec:javaspec`, root `mvn -q -DskipTests install`, standalone Maven plugin `verify` with 12 tests and runtime audit showing plugin plus core, standalone JUnit Platform engine `verify` with 12 tests and runtime audit showing core plus isolated JUnit Platform engine dependencies, Gradle plugin `clean test build` with 11 tests using Gradle 8.8, and Gradle plugin runtimeClasspath audit with only `org.javaspec:javaspec:0.1.0-SNAPSHOT`. After the CI workflow was pushed, the user/maintainer confirmed GitHub Actions was all green for HEAD `4d30e63` on `develop`; no GitHub run IDs, URLs, durations, or logs were independently queried from this environment.
- Phase 20 tester verification completed on 2026-06-04 with no Phase 20 metadata verification blockers, no tester file modifications, and no publish/deploy/signing commands run. `LICENSE` is identical to `origin/main:LICENSE` with blob `b990d5492f3ef404ffc145890b83e51914351bb5`; `bash -n scripts/check-version-alignment.sh`, `bash -n scripts/verify-all.sh`, and `bash scripts/check-version-alignment.sh` passed with all checked versions aligned at `0.1.0-SNAPSHOT`; `git diff --check`, `git diff --cached --check`, and untracked whitespace checks passed; effective POM generation passed for root, Maven plugin, and JUnit engine; Maven metadata checks passed for MIT License, URL `https://opensource.org/licenses/MIT`, distribution `repo`, Mario Giustiniani email, and maintainer role in the root, Maven plugin, and JUnit engine POMs; Gradle generated POMs `pluginMaven` and `javaspecPluginMarkerMaven` include MIT license and maintainer metadata; `mvn -q verify` passed with 386 tests, 0 failures, 0 errors, and 0 skipped; root runtime audit passed with no dependencies beyond `org.javaspec:javaspec`; root `mvn -q -Prelease-artifacts -DskipTests package` passed and produced non-empty main, sources, and javadoc jars; `mvn -q -DskipTests install` passed; Maven plugin and JUnit engine `-Prelease-artifacts -DskipTests package` checks passed and produced non-empty main/sources/javadoc jars; standalone Maven plugin and JUnit engine `mvn -q verify` each passed with 12 tests; Gradle plugin publication POM generation passed, and `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build` passed with 11 tests and produced non-empty jars, with non-blocking Java 8 source/target obsolete warnings on JDK 21; Gradle runtime dependencies contained only `org.javaspec:javaspec:0.1.0-SNAPSHOT`; and full aggregate `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed, covering version alignment, core verify, root audit, local install, Maven plugin verify/audit, JUnit engine verify/audit, and Gradle plugin build/audit.
- Phase 21 tester verification completed on 2026-06-04 with no blockers, no tester file modifications, and no publish/deploy/signing commands run. `git diff --name-only HEAD -- src/main/java src/test/java` was empty, confirming no core Java source/test changes. `git diff --check` passed; visible modified/untracked text files passed custom whitespace/EOF checks; `bash -n scripts/verify-examples.sh`, `bash -n scripts/verify-all.sh`, and `bash -n scripts/check-version-alignment.sh` passed; `scripts/check-version-alignment.sh`, `scripts/verify-all.sh`, and `scripts/verify-examples.sh` are executable (`755`); generated `examples/**/target`, `examples/**/build`, and `examples/**/.gradle` outputs were ignored and absent from `git ls-files --others --exclude-standard`. The schema JSON and golden JSON parsed; `jsonschema` validated the golden JSON against `docs/schemas/run-report-v1.schema.json`; the golden XML parsed as `<testsuite tests="1" failures="0">`; and structural sanity confirmed `schemaVersion=1`, one spec, one example, stable ids, source file, and `line=11`. `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh` passed, verifying Maven, JUnit Platform, and Gradle examples and asserting generated report markers. `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed and included the new `==> Standalone examples verification` section plus `PASS: standalone examples verification completed.` Core tests through `verify-all` remained 386 tests with 0 failures/errors/skips; Maven plugin, JUnit Platform engine, and Gradle plugin tests remained 12, 12, and 11 passing tests; the JUnit Platform example had 1 passing test; root/runtime dependency summaries and example runtime dependency checks stayed clean.
- Phase 22 tester verification completed on 2026-06-04 with no blockers and no production Java changes made by the tester. Targeted changed tests passed with 78 tests; `mvn -q test` passed with 399 tests; `mvn -q verify` passed; root runtime dependency audit showed no runtime dependencies beyond `org.javaspec:javaspec`; `mvn -q -DskipTests install` passed; standalone Maven plugin `verify` passed with 13 tests and runtime tree `org.javaspec:javaspec` only; standalone JUnit Platform engine `verify` passed with 13 tests and runtime tree core plus `junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`; standalone Gradle plugin `clean test build` passed with 12 tests and Java 8 obsolete source/target warnings only; Gradle runtimeClasspath contained only `org.javaspec:javaspec`; `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh` passed; and `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed. After Phase 20/21/22 were pushed, the user/maintainer confirmed remote GitHub Actions was green for HEAD `5088e96` on `develop`; no GitHub run IDs, URLs, durations, or logs were independently queried from this environment.
- Phases 23 through 29 were implemented in local commits after the last user-/maintainer-confirmed remote CI point. Implementation-phase local verification passed for execution-availability diagnostics, configuration report destinations, ServiceLoader formatter/extension discovery, target-profile enforcement, bootstrap hook execution, stronger interface doubles, and CLI opt-in compilation. This plan records those later checks generically because no detailed tester report with test counts is present here; no remote CI status is claimed for those local-only commits through current HEAD `ddd7eb9`.
- The original numbered roadmap is complete through Phase 18. Phase 19 through Phase 29 are post-roadmap increments: Phase 19 adds aggregate release/CI verification, Phase 20 adds release-readiness scaffolding without public publishing, Phase 21 adds standalone adoption examples plus report schema/golden documentation without core runtime changes, Phase 22 adds explicit skipped/pending semantics without adding runtime dependencies, Phase 23 adds execution-availability diagnostics, Phase 24 adds configuration-level report destinations, Phase 25 adds ServiceLoader formatter/extension discovery, Phase 26 adds target-profile enforcement before generation/update writes, Phase 27 adds bootstrap hook execution, Phase 28 strengthens interface doubles, and Phase 29 adds CLI-only opt-in source/spec compilation. Future feature work should be tracked as new roadmap or backlog items and must not imply that every conceivable IDE/CI, adoption, or release/publication feature is already implemented.

## ADR 0004 Correction Status — Implemented and Verified

ADR 0004 was recorded before correction planning, as required by the course-correction protocol. The blocking correction track has now been implemented and the user manual has been updated to describe the actual behavior.

### Verified PHPSpec Inputs

Planner verification after ADR 0004 confirmed these source inputs:

- PHPSpec cookbook pages for construction and matchers were accessible with a browser User-Agent at `https://phpspec.net/en/stable/cookbook/construction.html` and `https://phpspec.net/en/stable/cookbook/matchers.html`.
- Raw documentation was also verified from `phpspec/phpspec` main branch files `docs/cookbook/construction.rst` and `docs/cookbook/matchers.rst`.
- PHPSpec 8.3.0 source was verified through GitHub API/raw files under `src/PhpSpec/Matcher` and `src/PhpSpec/Wrapper`.
- Construction semantics to preserve: PHPSpec treats the subject as `$this`, creates it lazily, supports `beConstructedWith(...)`, `beConstructedThrough(method, args)`, dynamic named construction forms such as `beConstructedThroughNamed(...)` and `beConstructedNamed(...)`, allows construction configuration before instantiation, allows a later construction call inside an example to override an earlier rule before instantiation, rejects construction-method changes after instantiation, and uses `duringInstantiation()` for constructor/factory exceptions.
- Matcher semantics to model: expectations use `should*` and `shouldNot*`; identity/equality keywords include `return`, `be`, `equal`, and `beEqualTo`; comparison uses `beLike`; throw matching uses `shouldThrow(...).duringMethod(...)`, `during(method, args)`, and `duringInstantiation()`; string containment/start/end/regex checks and custom matchers are part of the documented subset.

### Implemented Correction Summary

1. `describe` now writes both `<Subject>Spec.java` and `<Subject>SpecSupport.java`. Concrete specs extend the generated support class. The support class extends `ObjectBehavior<Subject>` and passes `Subject.class` for lazy subject construction.
2. `javaspec run` syntax now includes `--constructor-policy <delete|preserve|comment>`; default policy is `comment`. `delete` is the explicit destructive opt-in, `preserve` keeps non-empty unmatched constructors, and `comment` comments non-empty unmatched constructors. Empty generated/no-op unmatched constructors may be removed when safe.
3. Construction semantics are implemented in `ObjectBehavior`: lazy subject construction, `beConstructedWith(...)`, `beConstructedThrough(...)`, `beConstructedNamed(...)`, `beConstructedThroughNamed(...)`, override-before-instantiation, failure on change after instantiation, and `shouldThrow(...).duringInstantiation()`.
4. Generation distinguishes constructor and factory construction markers: `beConstructedWith(...)` remains constructor descriptor generation, while `beConstructedThrough("create", args...)`, `beConstructedNamed("named", args...)`, and `beConstructedThroughNamed("createNamed", args...)` discover/generate static factory method skeletons returning the described type. Factory names must be string literals and valid Java identifiers; non-string-literal names are ignored for generation instead of creating empty constructor markers.
5. Typed proxy matcher syntax is supported through generated support classes, including calls such as `getRating().shouldReturn(5)`, `getTitle().shouldContain("Wizard")`, and `shouldThrow(IllegalArgumentException.class).duringSetRating(-3)`. Existing `match(value).should...` usage remains available.
6. Method generation discovers typed proxy calls and can generate missing instance method skeletons with Java 8-compatible default returns for body-bearing production kinds. Phase 10 extends the same descriptor flow to ordinary interface declarations, compatible annotation elements, and missing sealed-interface skeleton root/nested implementation methods. Generated typed spec support skips static factory descriptors because construction methods are not instance subject proxies. `run --generate` writes non-interactively; without `--generate`, `run` prompts before adding supported missing methods to an existing source file.
7. The matcher subset now includes identity/equality aliases and negations (`shouldBe`, `shouldNotBe`, `shouldEqual`, `shouldNotEqual`, `shouldReturn`, `shouldNotReturn`, `shouldBeLike`, `shouldNotBeLike`, `shouldBeEqualTo`, `shouldNotBeEqualTo`), type/instance aliases (`shouldHaveType`, `shouldBeAnInstanceOf`, `shouldReturnAnInstanceOf`), `shouldImplement`, string containment/start/end/pattern checks and their negations, count/empty helpers for arrays/collections/maps/character sequences/iterables, map key/value helpers, and custom matchers that can evaluate null subjects.
8. Known limitations remain: default and adapter execution paths are classpath/reflection based and execute only classloader-available compiled spec classes, while Phase 29 adds CLI-only opt-in source/spec compilation for `javaspec run`; lifecycle support is limited to configured bootstrap hooks plus optional public no-arg `let()`/`letGo()`; source parsing/generation uses Java 8-compatible heuristics rather than a full Java parser; generated post-Java-8 source forms still require an appropriate JDK to compile.
9. Verification passed: `mvn test` completed with 174 tests, and `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.

## Phase 1 Status

Phase 1 creates documentation only:

- README and architecture principles.
- ARC42 sections 1-4.
- ADRs for Java 8 baseline/LTS profiles, zero runtime dependencies, and the first-MVP class-creation suggestion course correction.
- Research notes for Java LTS data structures and phpspec features.
- Internet/source verification was performed on 2026-05-27 and research notes were updated against Oracle API docs, phpspec 8.3.0 source/Packagist metadata, phpspec.net docs, and the Prophecy README.

No source code, build files, test files, or scaffolding are created in this phase.

## Phase 2 Status

Phase 2 is completed and implemented as the first MVP, with ADR 0004 correction behavior now reflected in the current status:

- Maven Java 8 project with `source`/`target` compatibility set to 1.8.
- Zero runtime dependencies; JUnit is used only in test scope.
- Runtime entry point: `org.javaspec.cli.Main`.
- CLI commands: `javaspec describe <ClassName>` / `javaspec desc <ClassName>` and minimal `javaspec run`.
- PHPSpec-style command split: `describe` creates only specification/support skeletons; `run` discovers specs and owns generation/update for missing production class-like types, constructors, methods, and static factories.
- Options: `--spec-dir <dir>` / `--spec-root <dir>`, defaulting to `src/test/java`; `--source-dir <dir>` / `--source-root <dir>`, defaulting to `src/main/java` for `run`; `--generate` for `run`; `--constructor-policy <delete|preserve|comment>` for `run`, defaulting to `comment`; `--help` / `-h`.
- `describe`: exit `0`, write the PHPSpec-style `spec.*.*Spec.java` and `spec.*.*SpecSupport.java` skeletons when absent, never write production code.
- `run` with existing source/classpath class: exit `0` and report that the class exists; when a source file exists and specs describe constructors, static factories, or missing instance methods, `run` may update the source according to constructor policy and generation confirmation rules.
- `run` with missing production type without `--generate`: print the target path and ask `Y/n`; generate on yes, or exit `1` and write no production files on no/unavailable input.
- `run` with missing production type and `--generate`: exit `0` and write the production type skeleton without prompting.
- Supported generated production type kinds: Java 8 class/interface/enum/annotation plus post-Java 8 record, sealed class, and sealed interface source forms; specs can declare `shouldExtend(...)`, `shouldImplement(...)`, and sealed `shouldPermit(...)` relationships.
- Invalid arguments exit `64`; I/O errors exit `70`.

Implemented files/classes at a high level:

- `pom.xml` — Maven build, Java 8 compiler settings, jar main class, and runtime dependency leakage guard.
- `src/main/java/org/javaspec/cli/Main.java` — first-MVP CLI parsing, dispatch, diagnostics, and exit codes.
- `src/main/java/org/javaspec/model/DescribedClass.java`, `DescribedType.java`, `JavaTypeKind.java`, `ConstructorDescriptor.java`, and `MethodDescriptor.java` — described-name validation, source-path mapping, constructor/method descriptors, and Java 8/post-Java 8 class-like type kind modeling without post-Java 8 binary linkage.
- `src/main/java/org/javaspec/api/ObjectBehavior.java` — PHPSpec-inspired generic base class (`ObjectBehavior<T>`) with lazy subject construction, construction configuration, matcher wrapping, and throw expectations.
- `src/main/java/org/javaspec/matcher/**` — zero-dependency matcher registry, `Matchable<T>` expectation wrapper, match results, and custom matcher support.
- `src/main/java/org/javaspec/discovery/ClassExistenceChecker.java` / `ClassCheckResult.java` and `TypeExistenceChecker.java` / `TypeCheckResult.java` — source-root and classpath existence checks, including classpath kind detection for class-like types.
- `src/main/java/org/javaspec/discovery/SpecDiscovery.java` and `DiscoveredSpec.java` — deterministic `*Spec.java` discovery, described-type inference from the `spec.` namespace, kind and relationship markers, constructor/factory construction markers, typed proxy matcher calls, throw-proxy calls, and method descriptor inference.
- `src/main/java/org/javaspec/generation/ClassGenerationPlan.java`, `ClassSkeletonGenerator.java`, `ClassFileGenerator.java`, `TypeGenerationPlan.java`, `TypeSkeletonGenerator.java`, and `TypeFileGenerator.java` — production class-like skeleton planning and explicit file writing after prompt acceptance or `run --generate`, including inferred constructor, method-body, interface-declaration, annotation-element, sealed-interface nested implementation, and static factory method skeletons.
- `src/main/java/org/javaspec/generation/SpecGenerationPlan.java`, `SpecSkeletonGenerator.java`, `SpecFileGenerator.java`, and `SpecSupportFileGenerator.java` — PHPSpec-style specification and typed support skeleton planning/writing for `describe` and `run` support updates.
- `src/main/java/org/javaspec/generation/ConstructorPolicy.java`, `ClassConstructorUpdater.java`, and `ClassMethodUpdater.java` — constructor policy handling and source-preserving method-body/static-factory insertion plus ordinary-interface declaration and annotation-element insertion for existing production sources where supported.
- `src/test/java/org/javaspec/**` — JUnit tests across model, discovery, generation, CLI, build, compatibility, matchers, and API behavior, including post-Java-8 source-form generation and ADR 0004 correction behavior.

Verification summary:

- `mvn test` passed with 174 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`, confirming no runtime third-party dependency leakage.

## Phase 3 Status

Phase 3 is completed and implemented as the core domain model and LTS profile catalog.

Implementation summary:

1. Added Java 8-compatible profile/catalog domain classes under `org.javaspec.profile`: `TargetProfile`, `FeatureFlag`, `ApiSymbol`, `ApiSymbolKey`, `ApiSymbolKind`, `ApiSymbolCategory`, `ProfileCatalog`, and `DefaultProfileCatalogSymbols`.
2. Added Java 8-compatible compatibility boundary classes under `org.javaspec.compatibility`: `CompatibilityCheck`, `ProfileCompatibilityCheck`, `CompatibilityResult`, and `ApiAvailabilityProbe`.
3. Encoded target profiles `java8`, `java11`, `java17`, `java21`, and `java25` with deterministic ordering, parsing, lookup, and feature-support behavior.
4. Implemented immutable API-symbol metadata and catalog lookup by introduced profile, availability profile, owner, and owner/member key.
5. Populated representative data-structure and modeling metadata from `docs/research/java-lts-data-structures.md`, including Java 8 collection/container/array/stream symbols, Java 11 collection factories and related stream/optional additions, Java 17 stream/record/sealed metadata, Java 21 sequenced collections, and Java 25 stream gatherer metadata.
6. Preserved the Java 8 binary strategy: Java 11+ APIs remain metadata strings or reflection-only probes; production source has no direct post-Java-8 imports.
7. Added tests for the profile domain objects, catalog behavior, compatibility checks, and reflective API availability probing.

Verification summary:

- Initial Phase 3 tester verification reported `mvn test` BUILD SUCCESS with 212 tests run, 0 failures, 0 errors, and 0 skipped.
- Stabilization verification after Phase 4 reported `mvn verify` BUILD SUCCESS with 301 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.
- Phase 12 completed the Distrobox compatibility and quality matrix: Java 8, 11, 17, 21, and 25 containers all passed `mvn clean` and `mvn verify`; the Java 25 runtime Gatherer probe and runtime dependency audit also passed.

## Phase 4 Status

Phase 4 is completed and implemented as the configuration, naming, and discovery-filter integration. ADR 0005 records the restricted line-based configuration format decision.

Implementation summary:

1. Added the zero-dependency `org.javaspec.config` production package: `ConfigurationException`, `ConstructorPolicyParser`, `JavaspecSuiteConfiguration`, `JavaspecConfiguration`, and `JavaspecConfigurationParser`.
2. Added a restricted line-based config parser with no YAML/TOML/JSON runtime dependency. Blank lines and lines beginning with `#` are ignored; keys use `=` or `:` separators; duplicate, unknown, malformed, blank-required, invalid-profile, and invalid-constructor-policy input produces clear diagnostics.
3. Implemented inferred defaults through `JavaspecConfiguration.defaults()`: suite `default`, spec root `src/test/java`, source root `src/main/java`, spec package prefix `spec`, production package prefix empty, profile `java8`, formatter `progress`, constructor policy `comment`, and empty bootstrap hooks.
4. Modeled top-level config keys `profile`, `formatter`, `constructorPolicy`/`constructor-policy`, `defaultSuite`/`default-suite`, and `bootstrap`, plus suite keys for `specDir`/`spec-dir`, `sourceDir`/`source-dir`, `specPackagePrefix`/`spec-package-prefix`, `packagePrefix`/`package-prefix`, and `bootstrap`.
5. Integrated `--config <file>` and `--suite <name>` into `org.javaspec.cli.Main` for `describe` and `run`. The selected suite's paths and package prefixes drive naming unless paths are overridden by `--spec-dir`/`--spec-root` or `--source-dir`/`--source-root`.
6. Preserved `describe` as spec-only behavior: it still rejects command-line `--source-dir`, but a `sourceDir` in config is accepted because `describe` ignores source roots.
7. Integrated configuration with constructor handling: `run` uses the configured constructor policy unless command-line `--constructor-policy` overrides it; valid values remain `delete`, `preserve`, and `comment`.
8. Integrated suite package prefixes with `SpecNamingConvention`, `SpecDiscoveryRequest`, spec/support skeleton planning, and CLI describe/run flows so configured `specPackagePrefix` and `packagePrefix` map production classes to spec/support classes.
9. Added suite selection and run filters: `--suite <name>` selects the configured suite and spec root, repeatable `--class <name>` filters by described qualified/simple name or spec qualified/simple name, and repeatable `--example <name>` filters by example method name, display name, or source-order index.
10. Bootstrap values were introduced as comma-separated configured hook class-name values; Phase 27 now executes them during `run` when compiled hook classes are on the effective run classloader. Profile and formatter values were introduced as validated configuration values in Phase 4; Phase 9 selects active profiles and built-in formatter output, Phase 25 can discover external CLI/Gradle formatter names through ServiceLoader, and Phase 26 enforces effective profiles before generation/update writes. Phase 24 adds optional top-level JSON/JUnit XML-compatible report destinations. Phase 29 CLI compilation intentionally has no config keys.
11. Added tests: `src/test/java/org/javaspec/config/JavaspecConfigurationTest.java`, `src/test/java/org/javaspec/config/JavaspecConfigurationParserTest.java`, `src/test/java/org/javaspec/cli/MainConfigurationIntegrationTest.java`, `src/test/java/org/javaspec/discovery/SpecNamingConventionTest.java`, `src/test/java/org/javaspec/discovery/SpecDiscoveryNamingTest.java`, and `src/test/java/org/javaspec/generation/SpecSkeletonGeneratorNamingTest.java`.

Verification summary:

- `mvn verify` passed with 301 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

## Phase 5/6 MVP Reflection Runner Status — Implemented and Verified

ADR 0006 records the classpath reflection runner decision.

Implementation summary:

1. Added the zero-dependency `org.javaspec.runner` package with immutable example/spec/run result objects and failure details.
2. Integrated `SpecRunner` into `javaspec run` after discovery, related-spec handling, support updates, production type generation, constructor updates, and method updates.
3. Reused `DiscoveredSpec` and `SpecExample` metadata as the runner input, preserving suite selection, class filters, and example filters for execution.
4. Loaded compiled spec classes from the effective classloader. Non-loadable spec classes are not executable and their discovered examples are marked `SKIPPED`.
5. Executed each reflected example on a fresh spec instance.
6. Supported optional public no-argument `let()` before each example and optional public no-argument `letGo()` after each example, including after failures.
7. Mapped results to `PASSED`, `FAILED`, `BROKEN`, and `SKIPPED`: AssertionError from an example is `FAILED`; non-assertion throwables from examples, lifecycle hooks, instantiation, or reflection inspection are `BROKEN`; missing reflected example methods are `SKIPPED`.
8. Added CLI summary output with total, passed, failed, broken, and skipped counts plus failed/broken/skipped example details. `run` exits `1` when executable examples fail or break.
9. Preserved the zero-runtime-dependency policy and Java 8-compatible implementation style.

Verification summary:

- `mvn verify` passed with 307 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Known limitations:

- Default CLI runs and optional adapters are classpath/reflection based. Source-only or otherwise unavailable spec classes are skipped/not executable until an external build, IDE, launcher, explicit classpath, adapter classpath, or successful CLI `--compile` / `--compile-output <dir>` run puts compiled classes on the effective classloader.
- Lifecycle support is intentionally minimal: public no-argument `let()` and `letGo()` only.
- Bootstrap execution, deep profile-aware execution, and broader reporting beyond the implemented JSON/JUnit XML-compatible report outputs remain later work. Stop-on-failure and built-in progress/pretty formatter behavior are implemented in Phase 9 and routed through formatter contracts in Phase 11; explicit skipped/pending semantics are implemented in Phase 22.

## Phase 7 Matcher/Expectation Expansion Status — Implemented and Verified

Implementation summary:

1. Expanded `org.javaspec.matcher.Matchable` with identity/equality aliases and negations: `shouldBe`, `shouldNotBe`, `shouldEqual`, `shouldNotEqual`, `shouldReturn`, `shouldNotReturn`, `shouldBeLike`, `shouldNotBeLike`, `shouldBeEqualTo`, and `shouldNotBeEqualTo`.
2. Added type and assignability matcher aliases: `shouldHaveType`, `shouldBeAnInstanceOf`, `shouldReturnAnInstanceOf`, and `shouldImplement`.
3. Expanded string and containment helpers: `shouldContain`, `shouldNotContain`, `shouldStartWith`, `shouldNotStartWith`, `shouldEndWith`, `shouldNotEndWith`, `shouldMatchPattern`, and `shouldNotMatchPattern`.
4. Added count and emptiness helpers for arrays, collections, maps, character sequences, and generic iterables: `shouldHaveCount`, `shouldBeEmpty`, and `shouldNotBeEmpty`.
5. Added map-specific helpers: `shouldHaveKey`, `shouldNotHaveKey`, `shouldHaveValue`, and `shouldNotHaveValue`.
6. Expanded `ObjectBehavior` direct convenience assertion methods for equality/negation aliases, type/instance/implementation checks, containment, count/empty checks, map key/value checks, and string negations. These methods delegate through `match(actual)` so direct assertions and fluent `Matchable` assertions share the same implementation.
7. Kept `MatcherRegistry` zero-runtime-dependency and added a default `negated-equality` matcher available through the registry fallback while retaining the existing identity, equality, and negated-identity defaults.
8. Updated `SpecDiscovery` so expanded chained matcher names are recognized for method-discovery/default-return inference where applicable.
9. Preserved custom matcher support through `shouldMatch(...)` and registry registration without adding runtime dependencies.

Verification summary:

- `mvn verify` passed after the Phase 7 matcher/expectation expansion.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Known limitation:

- Count and emptiness checks on a generic `Iterable` iterate the iterable to compute a size. This consumes one-shot iterables and can hang on infinite iterables.

## Delegation Rule for Later Work

Further implementation work must be delegated to the appropriate workflow agents. The documenter must not create application source code, Maven build files, scaffolding, or tests. Future work should be delegated as follows by the parent workflow:

- Additional Java/Maven build changes: Java scaffolding/build agent.
- Production code under `org.javaspec`: Java implementation agent.
- Test code, compatibility matrix execution, and coverage reports: Java tester/quality agents.
- C4 diagrams, if requested: c4model via the documenter child delegation path.

## Requirement Traceability

| Requirement | Documentation artifact | Implementation status |
|---|---|---|
| Java 8-compatible binary | README, ARC42 constraints, ADR 0001, test report | Baseline implemented in Phase 2; Phase 12 Java 8 Distrobox verification passed on Temurin `1.8.0_492` with `mvn clean` and `mvn verify` running 364 tests, 0 failures, 0 errors, and 0 skipped. Supplemental compiler, bytecode, and constant-pool audits also preserved the Java 8 compatibility gate. |
| Zero runtime dependencies | README, ARC42 constraints, ADR 0002 | Implemented in Phase 2 and must be preserved through later phases. |
| Core dependency isolation | README, ADR 0002 | Implemented in Phase 2 with JUnit in core test scope only; later standalone optional adapters keep their own dependencies isolated from the core runtime artifact. |
| PHPSpec-style describe/run generation split | ADR 0003, this plan, user manual | Implemented: `describe` writes only spec/support files; `run` owns production type, constructor, static factory, and instance method generation/update; prompts are used where required, and `run --generate` answers yes non-interactively. |
| Constructor-policy correction | ADR 0004, this plan, user manual | Implemented and verified: exact states `delete`, `preserve`, `comment`; default `comment`; destructive deletion only with `--constructor-policy delete`; empty no-op unmatched constructors may be removed safely. |
| PHPSpec construction semantics | ADR 0004, verified PHPSpec construction docs/source, this plan, user manual | Runtime implemented in `ObjectBehavior`: lazy subject construction, `beConstructedWith`, factory/named construction forms, override-before-instantiation semantics, failure on change after instantiation, and `duringInstantiation()`. Generation implemented: `beConstructedWith(...)` remains constructor descriptor generation; factory/named forms with string-literal Java-identifier names generate static factory method skeletons returning the described type; non-string-literal factory names are ignored for generation. The MVP reflection lifecycle now runs compiled examples with fresh spec instances and optional public no-arg `let()`/`letGo()`; full PHPSpec parity remains future work. |
| Typed proxy matcher syntax | ADR 0004, verified PHPSpec matcher docs/source, this plan, user manual | Implemented: generated subject-specific support classes expose typed proxy methods and throw proxies while existing `match(value).should...` usage remains available. |
| Phase 7 matcher/expectation expansion | README, user manual, ARC42 section 5, this plan | Implemented and verified: `Matchable` includes expanded equality/negation, type/instance, implementation, string, count/empty, and map key/value helpers; `ObjectBehavior` direct convenience methods delegate through `match(actual)`; `MatcherRegistry` keeps a zero-dependency default negated-equality matcher; `SpecDiscovery` recognizes expanded chained matcher names for method-discovery/default-return inference where applicable. Generic `Iterable` count/empty checks consume the iterable and can hang on infinite iterables. |
| Phase 8/28 interface collaborators/doubles | README, user manual, ARC42 section 5, ADR 0007, ADR 0021, this plan | Implemented and verified: `org.javaspec.doubles` provides JDK-proxy ordinary-interface doubles, method-name and exact-argument return stubs, null and array-content argument matching, argument matchers, argument-constrained stub priority, throwing stubs, answer callbacks, immutable call history, called/not-called/exact-count verification, deterministic object methods, Java default returns for unstubbed methods, and `ObjectBehavior` double conveniences. Limitations are explicit: no concrete class/static/constructor/final-class/bytecode mocking or default-interface-method invocation. |
| Method generation | ADR 0004, this plan, user manual | Implemented and verified: discovery from typed proxy/throw calls, direct subject/setter calls, static factory construction markers, and the expanded chained matcher names where applicable; Java 8-compatible method-body and static factory skeleton generation for class/final/sealed class/enum/record sources remains unchanged; static factory descriptors are skipped by generated support proxies; `--generate` writes non-interactively and interactive `run` prompts before updating existing source files. |
| Phase 10 interface-style method generation | README, user manual, ARC42 section 5, this plan | Implemented and verified: missing ordinary interface skeletons render non-static method declarations and skip static descriptors; missing annotations render compatible no-arg non-static elements and skip incompatible descriptors; missing sealed interfaces render root method declarations and nested permitted implementations with Java default-return bodies; existing ordinary interfaces and annotations receive missing declarations/elements source-preservingly and idempotently; existing sealed-interface source updates remain deferred. |
| Configuration model and inferred defaults | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `JavaspecConfiguration.defaults()` provides the default suite `default`, Maven-style spec/source roots, `spec` package prefix, empty production package prefix, `java8` profile, `progress` formatter, `comment` constructor policy, and empty bootstrap hooks when no config file is supplied. |
| Constructor-policy config default | ADR 0004, ADR 0005, user manual, this plan | Implemented in Phase 4: config key `constructorPolicy`/`constructor-policy` accepts only `delete`, `preserve`, and `comment`; `comment` remains the inferred and config default, and `run --constructor-policy` overrides config explicitly. |
| Explicit suites, paths, profile, formatter, bootstrap, and report config | README, user manual, ARC42 section 5, ADR 0005, ADR 0017, ADR 0018, ADR 0019, ADR 0020, this plan | Implemented in Phase 4 and expanded later: `--config <file>` and `--suite <name>` select suite configuration; selected-suite `specDir`/`sourceDir` drive `describe`/`run` unless CLI path options override them; selected-suite `specPackagePrefix`/`packagePrefix` drive naming; config `profile` and `formatter` provide run defaults that valid CLI `--profile` and `--formatter` override; Phase 25 allows ServiceLoader-discovered CLI/Gradle formatter names; Phase 26 enforces the effective profile before generation/update writes; Phase 27 executes configured bootstrap hook class names before examples; Phase 24 report destinations provide JSON/JUnit XML-compatible defaults overridden by explicit CLI or adapter settings. |
| Naming convention integration | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `SpecNamingConvention` maps production names to spec/support packages using configured suite package prefixes, validates naming metadata, and is used by describe, discovery, and support generation. |
| Suite, class, and example filters | README, user manual, ARC42 section 5, this plan | Implemented in Phase 4 and reused by the Phase 5/6 MVP runner: `--suite` selects the configured suite; repeatable `--class` filters by described or spec class names; repeatable `--example` filters by example method name, display name, or source-order index; filtered `DiscoveredSpec`/`SpecExample` metadata controls both generation/update and reflection execution. |
| Phase 5/6 MVP reflection runner | README, user manual, ARC42 section 5, ADR 0006, this plan | Implemented and verified: after discovery/generation/update and any requested successful CLI compilation, `javaspec run` executes examples when compiled spec classes are available on the effective classloader; each example uses a fresh spec instance with optional public no-arg `let()` and `letGo()`; results are `PASSED`, `FAILED`, `BROKEN`, `SKIPPED`, or `PENDING`; CLI summary exits `1` for failed/broken executable examples; source-only/unavailable spec classes are skipped on default/adapters paths, with Phase 23 diagnostics explaining availability issues and Phase 29 CLI `--compile` available as an opt-in. Phase 9 adds `--stop-on-failure` to stop after the first FAILED or BROKEN executable example while the default remains executing all discovered metadata. |
| Phase 9 CLI expansion | README, user manual, ARC42 section 5, this plan | Implemented and verified: `run --dry-run` performs no writes and no prompts, reports would-generate/would-update actions for related specs/support, support updates, constructors, method bodies/declarations/elements, and missing production type generation, exits `1` when pending work exists, and exits `0` when no pending changes exist and examples pass, skip, or remain pending; `run --stop-on-failure` stops after the first FAILED or BROKEN executable example; `run --formatter <progress|pretty|custom>` selects built-in or ServiceLoader-discovered output and overrides config/default; `run --profile <java8|java11|java17|java21|java25>` selects the profile and Phase 26 enforces it before generation/update writes; `run --verbose` prints selected run settings; all Phase 9 controls are rejected for `describe`. |
| Phase 11/24/25 formatter, reporting, and extension increments | README, user manual, ARC42 section 5, ADR 0010, ADR 0017, ADR 0018, this plan | Implemented and verified: built-in output uses the public zero-dependency `RunFormatter` contract and deterministic `RunFormatterRegistry`; `progress` and `pretty` behavior remains compatible; `JavaspecExtension`/`Extension` and `ExtensionContext` allow formatter registration; Phase 25 adds ServiceLoader discovery for external `RunFormatter`, `JavaspecExtension`, and alias `Extension` providers on CLI/Gradle run classloaders; `javaspec run --report <file>` / `--report-file` and config/report-destination defaults write UTF-8 JSON reports with `schemaVersion` 1, summary counts, specs, examples, nullable failure details, throwable class/message, stack trace lines, stable ids/source fields, and pending counts/statuses. Reports are run-only, rejected by `describe` as CLI options, written for no-spec/passing/failing/broken/skipped/pending runs after normal output, skipped for dry-run pending generation/update, profile violations, compilation failures, or bootstrap failures before execution, and report write failures exit `70`. |
| Phase 14 no-JUnit invocation, explicit classpath, and JUnit XML reports | README, user manual, ARC42 sections 5-11, ADR 0011, test report, this plan | Implemented and verified: `org.javaspec.invocation` exposes `JavaspecInvocation`, `JavaspecLauncher`, `JavaspecInvocationResult`, and `JavaspecExitCode` for no-`System.exit` programmatic calls over canonical discovery/`SpecRunner`/`RunResult`; `javaspec run` accepts `--classpath`, `--classpath-file`, `--junit-xml`, and `--junit-xml-file`; JSON and JUnit XML-compatible reports can be requested together; report I/O failures exit `70`; passing, skipped/pending-only, and no-spec invocation paths map to `0`, while failed/broken execution maps to `1`. |
| Phase 15 optional Maven plugin integration | README, user manual, ARC42 sections 5-12, ADR 0011, test report, this plan | Implemented and verified as standalone optional artifact `javaspec-maven-plugin/`, not a root module. `JavaspecRunMojo` provides `javaspec:run` at default phase `verify`, uses Maven test dependency resolution and test classpath, supports config/suite/specDir/specRoot, class/example filters, stop/fail/skip controls, JSON and JUnit XML-compatible reports, Maven logging, and delegates to canonical `JavaspecLauncher` without `System.exit`. No JUnit is required in projects under test; JUnit is only a plugin test dependency. |
| Phase 16 optional Gradle plugin integration | README, user manual, ARC42 sections 5-12, ADR 0011, test report, this plan | Implemented and verified as standalone optional artifact `javaspec-gradle-plugin/`, not a root Maven module and outside the core artifact. The plugin id is `org.javaspec`; `JavaspecPlugin` registers extension `javaspec` and task `javaspecRun` in group `verification`; Java plugin/source-set defaults use the `test` source set runtime classpath and depend on `testClasses`; `JavaspecRunTask` supports skip/fail/stop controls, config/suite/specDir/specRoot, class/example filters, built-in formatter selection, JSON/JUnit XML-compatible reports, Gradle logging, and canonical `JavaspecLauncher` delegation without `System.exit`. No JUnit is required in projects under test; JUnit is only a plugin test dependency. |
| Phase 17 optional JUnit Platform engine integration | README, `javaspec-junit-platform-engine/README.md`, user manual, ARC42 sections 5-12, ADR 0011, test report, this plan | Implemented and verified as standalone optional artifact `javaspec-junit-platform-engine/`, not a root Maven module and outside the core artifact. The artifact `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT` packages a Java 8-compatible JUnit Platform `1.10.2` `TestEngine` with engine id `javaspec`, ServiceLoader registration, canonical `SpecDiscovery` / `SpecDiscoveryRequest` discovery, configuration parameters, class/package/method/unique-id selector filters, canonical `JavaspecLauncher` execution without `System.exit`, and javaspec-to-JUnit listener event mapping. Projects that do not opt into it still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths. |
| Phase 18 IDE/CI polish | README, user manual, ARC42 sections 5-11, ADR 0010, ADR 0011, test report, this plan | Implemented and verified as an incremental stable identifier/source-location/report polish. `ExampleResult`, `SpecResult`, and `DiscoveredSpec` expose stable id aliases; `SpecExample`, `ExampleResult`, and `SpecResult` carry source metadata where available; JSON reports add spec/example stable id and source fields while preserving existing fields; JUnit XML-compatible `<testcase>` elements add `file` and `line` attributes when source data is available; and the optional JUnit Platform engine retains its stable unique-id shape and MethodSource behavior with descriptor reporting aligned to stable ids. Later increments add pending examples (Phase 22), classpath/execution availability diagnostics (Phase 23), ServiceLoader formatter/extension discovery (Phase 25), and target-profile enforcement (Phase 26). |
| Phase 19 post-roadmap release/CI hardening | README, user manual, ARC42 sections 4-11, ADR 0012, test report, this plan | Implemented and verified as a non-disruptive aggregate verification increment. The repository was not converted to Maven multi-module; root `mvn verify` remains core-only; standalone adapters stay outside the root Maven reactor; `scripts/verify-all.sh` verifies core, runtime audits, current-core install, standalone Maven plugin, JUnit Platform engine, and Gradle plugin; `.github/workflows/ci.yml` defines a Java 8/11/17/21/25 core matrix plus Java 21 full verification through the script. Local script/YAML/whitespace verification passed; remote GitHub Actions success is user-/maintainer-confirmed for HEAD `4d30e63` on `develop`. |
| Phase 20 release-readiness scaffolding | README, user manual, `CHANGELOG.md`, `RELEASING.md`, ARC42 sections 4-11, ADR 0013, test report, this plan | Implemented and locally verified as release-readiness scaffolding only. `scripts/check-version-alignment.sh` verifies version alignment across root Maven, standalone Maven plugin, standalone JUnit Platform engine, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion`; `scripts/verify-all.sh` runs that check first. Maven `release-artifacts` profiles produce source/javadoc jars for root, Maven plugin, and JUnit engine; the Gradle plugin build produces source/javadoc jars. MIT license metadata and confirmed maintainer/developer metadata for `Mario Giustiniani <mariogiustiniani@gmail.com>` were added along with safe URL/SCM/GitHub Issues metadata; `LICENSE` matches `origin/main:LICENSE` blob `b990d5492f3ef404ffc145890b83e51914351bb5`. After Phase 20/21/22 were pushed, remote GitHub Actions success for HEAD `5088e96` on `develop` was user-/maintainer-confirmed. Public publication remains postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved. No publishing, deployment, signing, secrets, runtime dependencies, or Maven multi-module conversion were added. |
| Phase 21 adoption assets, examples, and report schema | README, user manual, `examples/README.md`, `docs/schemas/run-report-v1.schema.json`, `docs/examples/reports/*`, ARC42 sections 4-11, ADR 0014, test report, this plan | Implemented and locally verified as adoption assets only. Standalone Maven, Gradle, and JUnit Platform examples live under `examples/` and are not root modules. `scripts/verify-examples.sh` installs local snapshots, verifies the examples, and asserts JSON/JUnit XML-compatible report markers including `schemaVersion`, stable id `spec.com.example.CalculatorSpec#it_adds_two_numbers`, `PASSED`, and `line=11`. `scripts/verify-all.sh` runs examples by default after core/adapters; `JAVASPEC_SKIP_EXAMPLES=1` skips the examples section explicitly, and `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` skips only the Gradle example. The schema and golden report examples parsed/validated locally, and the current workflow includes examples by default unless skipped. After Phase 20/21/22 were pushed, remote GitHub Actions success for HEAD `5088e96` on `develop` was user-/maintainer-confirmed. No core Java source/test files, runtime dependencies, public publishing, deployment, or signing were added. |
| Phase 22 explicit skipped/pending semantics | README, user manual, `examples/README.md`, `docs/schemas/run-report-v1.schema.json`, `docs/examples/reports/*`, ARC42 sections 1-12, ADR 0015, test report, this plan | Implemented and locally verified: zero-dependency `@Skip`/`@Pending` annotations, `SkipExampleException`/`PendingExampleException`, `ObjectBehavior.skip(...)`/`pending(...)`, `@Skip` precedence, annotation no-instantiation/no-lifecycle/body execution, runtime skip/pending with `letGo()` handling, distinct `PENDING` status and `pendingCount()`, success exit for passed/skipped/pending-only runs, pending-aware formatter summaries, JSON `pending` counts and `status: "PENDING"`, JUnit XML-compatible skipped mapping, Maven/Gradle behavior, and JUnit Platform `executionSkipped` pending mapping. Targeted changed tests, root test/verify/runtime audit/install, standalone adapters, examples, and aggregate verification passed locally; after Phase 20/21/22 were pushed, remote GitHub Actions success for HEAD `5088e96` on `develop` was user-/maintainer-confirmed. |
| Phase 23 execution-availability diagnostics | README, user manual, ARC42 sections 5-11, ADR 0016, this plan | Implemented and locally verified: non-executable source/spec availability reasons are enriched in runner results; `RunDiagnostics.executionAvailabilityLines(RunResult)` returns deterministic user-facing lines; CLI prints `Execution diagnostics:` only for availability issues; Maven and Gradle log `javaspec:` warnings with classpath element counts; intentional `@Skip` and `PENDING` semantics are excluded; exit-code and build-failure semantics are unchanged. |
| Phase 24 configuration-level report destinations | README, user manual, ARC42 sections 5-11, ADR 0017, this plan | Implemented and locally verified: top-level config aliases provide JSON and JUnit XML-compatible report destination defaults; CLI options and explicit Maven/Gradle adapter settings override config defaults; `describe --config` accepts and ignores these keys; schemas, writers, exit codes, dry-run pending behavior, no-spec behavior, and standalone adapter boundaries are unchanged. |
| Phase 25 ServiceLoader formatter/extension discovery | README, user manual, ARC42 sections 5-11, ADR 0018, this plan | Implemented and locally verified: CLI and Gradle load built-ins first and then JDK ServiceLoader providers for `RunFormatter`, `JavaspecExtension`, and alias `Extension` from their effective run classloaders; duplicate extension implementations are configured once; invalid formatter/provider/extension loading diagnostics are explicit. Maven plugin formatter controls, JUnit Platform formatter controls, configuration-driven activation, package scanning, plugin lookup, report schema/content changes, publishing changes, and runtime dependency additions remain out of scope. |
| Phase 26 target-profile enforcement | README, user manual, ARC42 sections 5-11, ADR 0019, this plan | Implemented and locally verified: `ProfileEnforcement`, `ProfileEnforcementReport`, and `ProfileViolation` enforce the effective profile after discovery and before generation/update writes, prompts, execution, or reports. Enforcement rejects incompatible described type kinds and resolvable generated method signature API owners while ignoring unknown/ambiguous types to avoid false positives; it is conservative source/generation enforcement, not compiler-grade integrated compilation. |
| Phase 27 bootstrap hook execution | README, user manual, ARC42 sections 5-11, ADR 0020, this plan | Implemented and locally verified: configured top-level hook class names run before selected-suite hooks, order and duplicates are preserved, hooks load from the effective run classloader/classpath, and hooks receive immutable `BootstrapContext` data immediately before examples. CLI no-spec runs skip hooks; bootstrap failures exit before examples/reports with clear diagnostics. ServiceLoader hook discovery, scripts, package scanning, dependency resolution, adapter-integrated compilation, and runtime dependencies remain out of scope. |
| Phase 28 stronger interface doubles | README, user manual, ARC42 section 5, ADR 0021, this plan | Implemented and locally verified: interface doubles support argument matchers, matcher-constrained stub priority, throwing stubs, answer callbacks, and `Doubles`/`ObjectBehavior` aliases while preserving ordinary-interface-only JDK dynamic proxies, Java 8 compatibility, zero runtime dependencies, and no concrete/static/final/constructor/bytecode mocking. |
| Phase 29 CLI opt-in source/spec compilation | README, user manual, ARC42 sections 5-11, ADR 0022, this plan | Implemented and locally verified at current HEAD `ddd7eb9`: `javaspec run --compile` compiles source/spec files with the current JDK `javax.tools.JavaCompiler`; `--compile-output <dir>` implies compilation and selects output; compilation happens after discovery/profile/generation/update and before bootstrap/examples; no-spec and dry-run paths skip compilation; compiler-unavailable requests exit `64`; compile failures print `Compilation failed:`, exit `1`, skip bootstrap/examples, and write no reports. No default/adapters/config changes, dependency resolution, incremental cache, forked `javac`, source-level/release management, report schema change, or runtime dependency was added. |
| Missing-class flow with config | User manual, this plan | Implemented in Phase 4: `run` uses inferred defaults without a config file and selected-suite paths/naming with explicit config, preserving the existing missing-production prompt and `--generate` non-interactive generation behavior. |
| Maven implementation | This plan | Implemented in Phase 2. |
| Package base `org.javaspec` | README, this plan | Implemented in Phase 2 and retained for future work. |
| Target Java LTS profiles 8, 11, 17, 21, 25 | README, ARC42 section 5, ADR 0001, Java LTS research, this plan, test report | Implemented in Phase 3: `TargetProfile` and `ProfileCatalog` encode `java8`, `java11`, `java17`, `java21`, and `java25`; Phase 12 executed the full Distrobox runtime matrix for Java 8, 11, 17, 21, and 25, and every profile container passed `mvn clean` and `mvn verify`. |
| Post-Java 8 APIs as metadata/reflection | README, ARC42 section 5, ADR 0001, Java LTS research, this plan, test report | Implemented in Phase 3: Java 11+ API symbols are stored as metadata strings and probed only through `ApiAvailabilityProbe`; no post-Java-8 direct production imports are required. Phase 12 constant-pool audit passed with 0 direct post-Java-8 API references and 70 intentional metadata string hits. |
| Java 8 data-structure list | README, ARC42 section 5, Java LTS research, this plan | Implemented in Phase 3: representative Java 8 collection, container, array, optional, atomic/reference, and stream symbols are cataloged and tested. |
| Later LTS data-structure additions | README, ARC42 section 5, Java LTS research, this plan, test report | Implemented in Phase 3: representative Java 11 collection factories/collectors/optional/stream additions, Java 17 stream/record/sealed metadata, Java 21 sequenced collections, and Java 25 stream-support metadata are cataloged. Phase 12 verified the Java 25 Gatherer symbols with a Java 25 runtime reflection probe; ongoing synchronization remains future maintenance work. |
| Java 25 stream-support metadata | README, ARC42 section 5, Java LTS research, this plan, test report | Implemented in Phase 3: `STREAM_GATHERERS` and metadata for `java.util.stream.Gatherer`, nested gatherer types, and `java.util.stream.Gatherers` are present; Phase 12 Java 25 runtime reflection probing passed for `java.util.stream.Gatherer`, `Gatherer$Downstream`, `Gatherer$Integrator`, `Gatherer$Integrator$Greedy`, and `java.util.stream.Gatherers`. |
| Complete phpspec feature inventory | phpspec research and ADR 0004 follow-up verification | Phase 1 research completed; construction and matcher details were re-verified for ADR 0004; the ADR 0004 correction track is implemented, while broader PHPSpec feature parity remains future phased work. |

## Implementation Principles

- Compile production code with Java 8 `source` and `target` settings.
- Keep runtime code within package base `org.javaspec`.
- Use Maven for the project layout and lifecycle.
- Do not import or reference Java 9+ classes in production source.
- Store Java 9+ API names as strings in profile metadata.
- Use reflection only behind explicit compatibility boundaries.
- Keep optional integrations outside the core runtime.
- Keep optional build-tool and JUnit Platform integrations as separate artifacts from the zero-runtime-dependency core; when an adapter is intentionally standalone and not a root module, root verification continues to verify the core artifact only and adapter verification must be run separately or through the aggregate `scripts/verify-all.sh` release check.
- Keep project versions aligned across the root POM, standalone Maven plugin POM, standalone JUnit Platform engine POM, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion`; run `scripts/check-version-alignment.sh` directly or through `scripts/verify-all.sh` before release-candidate packaging.
- Keep standalone examples as adoption assets outside the root build modules; verify them through `scripts/verify-examples.sh` directly or through the default `scripts/verify-all.sh` examples section unless `JAVASPEC_SKIP_EXAMPLES=1` is explicitly selected, and use `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` only when skipping the Gradle example is intentional.
- Treat public publication as postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved outside normal local verification; preserve the confirmed MIT license and `Mario Giustiniani <mariogiustiniani@gmail.com>` maintainer metadata and do not invent additional legal or real-person metadata.
- Keep the javaspec core runner canonical; CLI, Maven, Gradle, and JUnit Platform entry points must be adapters over core discovery, execution, result, formatter, and report semantics.
- Keep no-JUnit execution first-class, including CLI usage and future build-tool adapters; never require JUnit to run javaspec specs.
- Keep JUnit Platform dependencies in the separate optional engine artifact, never in the core runtime artifact.
- Keep production generation governed by the ADR 0003 `describe`/`run` split and by explicit confirmation or documented non-interactive generation policy.
- Treat `describe` as the explicit command that writes specification skeletons, interactive `run` confirmation as the normal PHPSpec-style production generation authorization, and `run --generate` as the explicit non-interactive yes.
- Do not write generated source files unless the user explicitly confirms the action or a documented non-interactive policy allows it.
- Treat ADR 0004 as implemented correction behavior that must be preserved before expanding unrelated functionality.
- Keep constructor policy states limited to `delete`, `preserve`, and `comment`; use `comment` as the default and require explicit opt-in for destructive deletion.
- Keep configuration parsing restricted, line-based, and zero-dependency; do not add YAML/TOML/JSON parser dependencies to runtime.
- Treat missing config as `JavaspecConfiguration.defaults()` and apply command-line path/constructor-policy/report overrides over selected-suite or top-level config values while keeping selected-suite package prefixes in the active naming convention.
- Treat bootstrap hooks as explicit configured Java hook class names that execute only when compiled classes are available on the effective run classloader/classpath; do not add ServiceLoader hook discovery, script engines, package scanning, dependency resolution, adapter-integrated compilation, or runtime dependencies.
- Treat profile and formatter settings as active run selections: CLI overrides config/default values; Phase 26 enforces profiles before generation/update writes; Phase 25 loads built-in plus ServiceLoader-discovered CLI/Gradle formatters from the effective run classloader.
- Keep built-in formatter rendering behind the zero-dependency `RunFormatter` contract and deterministic `RunFormatterRegistry`; keep external formatter/extension discovery classloader-scoped and ServiceLoader-based, with no configuration-driven activation, plugin lookup, package scanning, Maven plugin formatter controls, or JUnit Platform formatter controls.
- Keep run reports dependency-free and based on the immutable runner result model; preserve existing report fields when adding stable identifiers, source metadata, or pending counts/statuses; allow destinations from CLI options, top-level config defaults, or explicit adapter settings; skip reports when dry-run exits before execution because pending generation/update work exists, when profile enforcement fails before writes, when opt-in CLI compilation fails, or when bootstrap execution fails before examples; treat report write failures as exit `70` I/O failures.
- Treat the Phase 11 extension API plus Phase 25 ServiceLoader loading as formatter/extension registration only; do not imply package scanning, plugin repository lookup, automatic classpath repair, Maven/JUnit Platform formatter controls, or broader plugin activation.
- Use `DiscoveredSpec` and `SpecExample` as the execution selection source so suite, class, example filters, bootstrap contexts, diagnostics, and runner execution remain aligned.
- Treat default CLI runs, programmatic invocation, and optional adapters as classpath reflection execution paths; source-only or unavailable spec classes are skipped/not executable until compiled classes are present. Phase 29 CLI `--compile` / `--compile-output <dir>` is an explicit current-JDK compiler step only and does not add default compilation, config keys, dependency resolution, incremental caches, forked `javac`, source-level/release management, adapter behavior, or runtime dependencies.
- Keep explicit skip/pending semantics zero-dependency: annotations skip lifecycle/body execution, runtime signals from `let()` or examples still run `letGo()`, `PENDING` remains distinct from `SKIPPED`, and JUnit-compatible outputs map pending to skipped only because those formats lack a distinct pending status.
- Prefer generated subject-specific typed support/proxy classes while keeping explicit `match(value)` style APIs available.
- Keep `Matchable`, `ObjectBehavior` direct convenience assertions, and `SpecDiscovery` matcher-name recognition synchronized when matcher names are added.
- Keep core doubles interface-only and JDK-proxy based unless a future ADR authorizes optional advanced integrations; Phase 28 argument matchers, throwing stubs, and answer callbacks stay inside this boundary, and unsupported double targets are rejected with clear diagnostics.
- Preserve exact-argument and matcher-constrained double semantics for `null` values and array contents, and keep `ObjectBehavior` double convenience APIs synchronized with `org.javaspec.doubles` control behavior.
- Document that count/empty checks on generic `Iterable` values consume the iterable and are unsafe for infinite iterables.
- Insert generated class/final-class/sealed-class/enum/record method bodies, interface method declarations, and annotation elements source-preservingly where supported, with confirmation or documented non-interactive behavior; use Java default returns where generated method bodies are required, and keep existing sealed-interface source updates deferred until nested permitted implementations can also be updated safely.
- Keep construction and matcher behavior aligned with the verified PHPSpec semantics for lazy construction, overrides before instantiation, negation, and exception matching.

## Phased Plan

### Phase 2 — First MVP: Java 8 Scaffolding and Missing Class Creation Suggestion (Completed)

**Status:** Completed and implemented.

**Relevant ADRs:** ADR 0001, ADR 0002, ADR 0003.

ADR 0003 moved the class-creation suggestion into the first MVP; the implemented CLI now follows PHPSpec's split where `describe` creates specs and `run` owns production-code generation. The implemented Phase 2 scope is:

1. Maven project layout with Java 8 `source`/`target` settings.
2. Runtime dependency leakage guard and no runtime third-party dependencies.
3. Production package base `org.javaspec`.
4. Minimal described-class model and validation.
5. Source-root and classpath existence checks.
6. CLI flow for `describe`/`desc` and minimal `run`.
7. PHPSpec-style specification and support skeleton generation from `describe`, using `ObjectBehavior<T>` through generated `<Subject>SpecSupport`, `it_is_initializable`, and `shouldHaveType(Subject.class)`.
8. Deterministic `spec.*.*Spec.java` discovery and described-class inference for `run`.
9. Missing production-class prompt output from `run`, including target path and `Y/n` confirmation.
10. Explicit `run --generate` skeleton writing for missing production class-like types without prompting.
11. Class-like kind markers in specs for interface, enum, and annotation generation.
12. Stable first-MVP exit codes: `0`, `1`, `64`, and `70`.

Implemented behavior:

- `describe`/`desc`: exit `0`, create Java 8-compatible PHPSpec-style `spec.*.*Spec.java` and `spec.*.*SpecSupport.java` skeletons when absent, and never generate production code.
- `describe`/`desc` when the spec already exists: exit `0`, report it, do not overwrite it, and generate the support file if it is missing.
- `run` with no specs: exit `0`, report that no specs were found.
- `run` with existing source/classpath class: exit `0` and report that the class exists; when a source file exists and specs describe constructors, static factories, or missing instance methods, `run` may update the source according to constructor policy and generation confirmation rules.
- `run` with missing production type without `--generate`: print the target path and ask whether to create it; yes generates and exits `0`, no/unavailable input exits `1` and writes no production files.
- `run` with missing production type and `--generate`: exit `0` and write the production type skeleton without prompting.
- `run --constructor-policy <delete|preserve|comment>` controls unmatched constructor handling; `comment` is the default and `delete` is the destructive opt-in.
- `run` discovers typed proxy matcher calls and factory construction markers, updates generated support classes, and can add missing Java 8-compatible instance method and static factory skeletons; without `--generate`, it prompts before updating an existing source file.
- `beConstructedWith(...)` remains constructor descriptor generation; `beConstructedThrough(...)`, `beConstructedNamed(...)`, and `beConstructedThroughNamed(...)` generate static factory methods only when the factory name is a string literal valid Java identifier. Generated support classes skip those static factory descriptors as non-instance construction methods.
- Specs can mark non-class production kinds with `shouldBeAFinalClass()`, `shouldBeAnInterface()`, `shouldBeAnEnum()`, `shouldBeAnAnnotation()`, `shouldBeARecord()`, `shouldBeASealedClass()`, or `shouldBeASealedInterface()`; generation writes the corresponding skeleton as source text.
- Specs can declare relationships with `shouldExtend(...)` and `shouldImplement(...)`; missing related production types get generated specs first, then production skeletons.
- Sealed class specs can declare explicit permitted subtypes with `shouldPermit(Circle.class, Rectangle.class)`; missing permitted subtypes get final-class specs extending the sealed root; otherwise a nested `Permitted` placeholder is generated so the sealed root has a syntactically valid permits clause.
- Sealed interface specs keep permitted implementations in the same production file; no separate specs or source files are generated for those local permitted implementations.
- Invalid arguments: exit `64`.
- I/O errors: exit `70`.

Verification:

- `mvn test` passed with 174 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` and no runtime third-party dependencies.

Still out of scope after the ADR 0004 correction:

- Complete PHPSpec-style runner lifecycle beyond the current reflection runner remains future work, but several formerly out-of-scope pieces are now implemented: stop-on-failure and built-in progress/pretty formatters in Phase 9, formatter contracts and JSON reports in Phase 11, JUnit XML-compatible reports in Phase 14, explicit skipped/pending semantics in Phase 22, execution-availability diagnostics in Phase 23, profile enforcement in Phase 26, bootstrap hook execution in Phase 27, and CLI opt-in compilation in Phase 29.
- Broader interface and annotation generation beyond the supported Phase 10 method declarations/elements, plus enum generation beyond minimal skeletons.
- Private constructor source generation and broader named-constructor customization beyond the current static factory skeleton support.
- Template systems beyond the minimal class-like/spec/support skeleton need.
- Return constant generation from expectations beyond Java 8-compatible default returns.
- Full PHPSpec matcher parity beyond the implemented Phase 7 matcher subset.
- Broader Prophecy-inspired or double functionality beyond Phase 28 interface-only argument matchers, throwing stubs, and answer callbacks.

### Phase 3 — Core Domain Model and LTS Profile Catalog (Completed)

**Status:** Completed and implemented.

**Implemented by:** Java implementation agent.

Implemented scope:

1. Extended the first-MVP domain model with immutable Java 8-compatible objects for target profiles, API symbols, feature flags, and compatibility checks.
2. Encoded profiles `java8`, `java11`, `java17`, `java21`, and `java25` in `TargetProfile` and exposed them through `ProfileCatalog`.
3. Added metadata for representative collection/data-structure APIs from `docs/research/java-lts-data-structures.md`.
4. Added reflection helpers that safely probe optional APIs by class, method, or field name without requiring those APIs on Java 8.
5. Included Java 25 stream gatherer metadata while keeping all Java 9+ APIs metadata/reflection-only.
6. Added deterministic catalog lookup by profile, owner, owner/member key, and feature flag.

Implemented files/classes at a high level:

- `src/main/java/org/javaspec/profile/TargetProfile.java` — ordered LTS profile keys, labels, major versions, parsing, and comparison helpers.
- `src/main/java/org/javaspec/profile/FeatureFlag.java` — profile-gated feature flags for Java type kinds, collection factories, streams, sequenced collections, and stream gatherers.
- `src/main/java/org/javaspec/profile/ApiSymbol.java`, `ApiSymbolKey.java`, `ApiSymbolKind.java`, and `ApiSymbolCategory.java` — immutable API-symbol metadata and lookup keys.
- `src/main/java/org/javaspec/profile/ProfileCatalog.java` and `DefaultProfileCatalogSymbols.java` — deterministic default catalog for Java 8, 11, 17, 21, and 25 metadata.
- `src/main/java/org/javaspec/compatibility/CompatibilityCheck.java`, `ProfileCompatibilityCheck.java`, and `CompatibilityResult.java` — target-profile compatibility checks for type kinds, feature flags, and API symbols.
- `src/main/java/org/javaspec/compatibility/ApiAvailabilityProbe.java` — reflection-only availability probe for optional APIs.
- `src/test/java/org/javaspec/profile/**` and `src/test/java/org/javaspec/compatibility/**` — tests for profiles, feature flags, symbols, catalog behavior, compatibility checks, and API probing.

Verification:

- Initial Phase 3 tester verification reported `mvn test` BUILD SUCCESS with 212 tests run, 0 failures, 0 errors, and 0 skipped.
- Stabilization verification after Phase 4 reported `mvn verify` BUILD SUCCESS with 301 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Acceptance criteria status:

- The profile catalog is usable on Java 8 by design and by Java 8-compatible production source.
- Java 11+ APIs are represented by metadata strings or reflective probes only.
- Profile behavior is deterministic and covered by tests.
- The first-MVP described-class model remains compatible with the expanded domain model.

### Phase 4 — Configuration, Naming, and Discovery Filters (Completed)

**Status:** Completed and implemented.

**Relevant ADRs:** ADR 0002, ADR 0005.

**Implemented by:** Java implementation agent.

Implemented scope:

1. Added the `org.javaspec.config` package with immutable top-level and suite configuration objects, configuration exceptions, a constructor-policy parser, and a restricted config parser.
2. Implemented `JavaspecConfiguration.defaults()` for the inferred no-config case: default suite `default`, spec root `src/test/java`, source root `src/main/java`, spec package prefix `spec`, empty production package prefix, profile `java8`, formatter `progress`, constructor policy `comment`, and no bootstrap hooks.
3. Implemented a zero-runtime-dependency line-based config format: blank lines and `#` comments are ignored, `=` and `:` are accepted separators, duplicate/unknown/malformed keys are rejected, and no YAML/TOML/JSON runtime parser is required.
4. Implemented top-level keys for `profile`, `formatter`, constructor policy, default suite, and bootstrap hook class names; implemented suite keys for spec/source roots, `specPackagePrefix`/`spec-package-prefix`, `packagePrefix`/`package-prefix`, and bootstrap hook class names. Phase 24 later adds top-level JSON/JUnit XML-compatible report destination keys.
5. Integrated `--config <file>` and `--suite <name>` with `describe` and `run` in `org.javaspec.cli.Main`.
6. Applied selected-suite paths unless overridden by command-line spec/source path options. `describe` still rejects command-line source-root options but accepts and ignores `sourceDir` loaded from config.
7. Applied configured package prefixes through `SpecNamingConvention` so `describe`, `run`, discovery, and spec/support generation map between production packages and spec packages consistently.
8. Added naming/discovery metadata for example methods: public `void` methods named `it_*` or `its_*` are extracted with source-order indexes and display names derived from underscores.
9. Added suite selection and run filters: repeatable `--class <name>` filters by described qualified name, described simple name, spec qualified name, or spec simple name; repeatable `--example <name>` filters by example method name, display name, or source-order index. Suite selection through `--suite <name>` selects the configured suite, spec root, source root, and naming convention.
10. Applied the configured constructor policy to `run` unless command-line `--constructor-policy` overrides it.
11. Preserved the missing-production prompt and `--generate` flow for inferred defaults and explicit config.
12. Initially parsed bootstrap values as comma-separated hook metadata; Phase 27 now executes those configured hook class names during `run` when compiled hook classes are on the effective run classloader. Profile and formatter values were parsed/validated configuration values in Phase 4, consumed by Phase 9 run selection, extended by Phase 25 ServiceLoader formatter discovery, and enforced for profiles by Phase 26 before generation/update writes.

Implemented files/classes at a high level:

- `src/main/java/org/javaspec/config/ConfigurationException.java`
- `src/main/java/org/javaspec/config/ConstructorPolicyParser.java`
- `src/main/java/org/javaspec/config/JavaspecSuiteConfiguration.java`
- `src/main/java/org/javaspec/config/JavaspecConfiguration.java`
- `src/main/java/org/javaspec/config/JavaspecConfigurationParser.java`
- `src/main/java/org/javaspec/discovery/SpecNamingConvention.java` and `src/main/java/org/javaspec/naming/SpecNamingConvention.java`
- `src/main/java/org/javaspec/discovery/SpecDiscoveryRequest.java`
- `src/main/java/org/javaspec/discovery/SpecExample.java`
- `src/main/java/org/javaspec/discovery/SpecDiscovery.java` updates for configured naming, class filters, example extraction, and example filters.
- `src/main/java/org/javaspec/generation/SpecSkeletonGenerator.java` updates for configured spec/support naming.
- `src/main/java/org/javaspec/cli/Main.java` updates for config loading, suite selection, path precedence, naming metadata, constructor-policy precedence, and `--class`/`--example` filters.
- `src/test/java/org/javaspec/config/JavaspecConfigurationTest.java`
- `src/test/java/org/javaspec/config/JavaspecConfigurationParserTest.java`
- `src/test/java/org/javaspec/cli/MainConfigurationIntegrationTest.java`
- `src/test/java/org/javaspec/discovery/SpecNamingConventionTest.java`
- `src/test/java/org/javaspec/discovery/SpecDiscoveryNamingTest.java`
- `src/test/java/org/javaspec/generation/SpecSkeletonGeneratorNamingTest.java`

Verification:

- `mvn verify` BUILD SUCCESS with 301 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Acceptance criteria status:

- A default configuration can be inferred with no config file.
- Explicit config can select suite, paths, target profile, formatter, constructor policy, spec package prefix, production package prefix, bootstrap hook class names, and optional report destinations. Current `describe`/`run` behavior uses selected suite paths, package prefixes, and constructor policy; Phase 9 `run` consumes configured profile and formatter defaults, Phase 24 consumes configured report destinations, Phase 25 validates formatter names against built-in plus ServiceLoader-discovered formatters, Phase 26 enforces the effective profile before generation/update writes, and Phase 27 executes configured bootstrap hooks before examples.
- Naming convention mapping works for default and configured package prefixes, including spec/support skeleton paths and discovery mapping back to described production classes.
- Suite selection, class filters, and example filters are implemented for the current discovery/generation flow and are reused by the MVP reflection runner.
- Invalid config, including an unknown constructor policy or invalid naming metadata, produces clear diagnostics.
- The missing-class suggestion flow works with both inferred defaults and explicit config.

### Phase 5 — Full Runner Discovery Expansion

**Status:** MVP implemented and verified; richer runner discovery diagnostics remain future work.

**Relevant ADRs:** ADR 0006.

Implemented scope:

1. The naming-convention and discovery-filter subset originally planned here was implemented during Phase 4 stabilization: default/configured package-prefix mapping, configured spec roots, described/spec class mapping, example metadata extraction, suite selection, class filters, and example filters.
2. The Phase 5/6 MVP runner now consumes the existing `DiscoveredSpec` and `SpecExample` metadata, so suite, class, and example filters remain effective for reflection execution.
3. Only discovered/filtered example metadata is executed; unrelated methods in a compiled spec class are not invoked by the runner.
4. Existing first-MVP described-type checks, generation prompts, related-spec handling, support updates, constructor updates, and method updates remain ahead of execution in `javaspec run`.

Remaining tasks:

1. Extend diagnostics and additional source-location use cases as needed by future reporting layers; Phase 18 now records method declaration source lines and propagates source file/line metadata where available.
2. Preserve the source-discovery metadata contract when future bootstrap and deeper profile behavior is added.
3. Keep Phase 9 stop-on-failure and formatter behavior aligned with the existing discovery metadata contract.

Acceptance criteria status:

- Deterministic discovery and configured naming behavior remains stable.
- A described class can be mapped to its spec class with default or configured package prefixes.
- Suite, class, and example filters work for generation/update and for MVP reflection execution.
- Existing first-MVP described-class checks remain stable before execution.

### Phase 6 — Runner and Example Lifecycle

**Status:** MVP implemented and verified; full PHPSpec-style lifecycle parity remains future work.

**Relevant ADRs:** ADR 0004, ADR 0006.

Implemented scope:

1. Added the `org.javaspec.runner` execution/result model: `SpecRunner`, `ExampleStatus`, `ExampleResult`, `SpecResult`, `RunResult`, and `FailureDetail`.
2. `javaspec run` executes examples after discovery/generation/update work and any requested successful CLI compilation have completed, and only when compiled spec classes are available on the effective classloader.
3. Each example runs on a fresh spec instance constructed through the spec class no-argument constructor.
4. Optional public no-argument `let()` runs before each example; optional public no-argument `letGo()` runs after each example, including when `let()` or the example fails.
5. Result states are implemented: `PASSED` for normal completion, `FAILED` for `AssertionError`, `BROKEN` for non-assertion throwables/lifecycle/instantiation/reflection errors, `SKIPPED` for non-loadable spec classes, missing reflected example methods, or explicit skips, and `PENDING` for explicit pending examples.
6. The CLI prints total, passed, failed, broken, skipped, and pending counts and exits `1` when executable examples fail or break.
7. The runner preserves the zero-runtime-dependency policy. Default execution remains classpath/reflection based; Phase 29 adds an explicit CLI compilation step before bootstrap/examples rather than changing the runner into a compiler.

Remaining tasks:

1. Bootstrap execution and target-profile enforcement are implemented in Phases 27 and 26. Broader PHPSpec lifecycle parity and additional reporting formats beyond the implemented JSON/JUnit XML-compatible outputs remain future work; stop-on-failure, verbosity, built-in/ServiceLoader formatter behavior, explicit skipped/pending semantics, and CLI opt-in compilation are implemented and routed through documented boundaries.
2. Expand failure-specific source-location diagnostics beyond the Phase 18 method/source metadata where useful.
3. Continue to refine typed proxy matcher diagnostics and method-generation reporting without forcing eager subject construction.
4. Keep ADR 0004 construction semantics stable as the runner grows beyond the MVP lifecycle.

Acceptance criteria status:

- Examples run with isolated spec instances.
- `let()`, example execution, and `letGo()` interact predictably for the MVP public no-arg lifecycle.
- Failed and broken examples include throwable summary details in CLI output.
- Exit code `1` is stable for failed/broken executable examples; skipped-only, pending-only, and skipped-plus-pending runs remain successful.
- Source-only or unavailable spec classes are skipped until compiled classes are present on the effective classloader, selected explicit classloader, adapter classpath, or successful CLI compile-output-first classloader.

### Phase 7 — Expectations and Matchers (Completed)

**Status:** Implemented and verified for the current zero-dependency matcher/expectation expansion. Full PHPSpec matcher parity, approximate equality, richer object-state matchers, iteration/yield variants, and extension registration beyond the current registry remain future work.

Implemented scope:

1. `Matchable<T>` exposes PHPSpec-inspired equality/identity aliases and negations: `shouldBe`, `shouldNotBe`, `shouldEqual`, `shouldNotEqual`, `shouldReturn`, `shouldNotReturn`, `shouldBeLike`, `shouldNotBeLike`, `shouldBeEqualTo`, and `shouldNotBeEqualTo`.
2. Type and assignability aliases are implemented: `shouldHaveType`, `shouldBeAnInstanceOf`, `shouldReturnAnInstanceOf`, and `shouldImplement`.
3. String and containment matchers are expanded: `shouldContain`, `shouldNotContain`, `shouldStartWith`, `shouldNotStartWith`, `shouldEndWith`, `shouldNotEndWith`, `shouldMatchPattern`, and `shouldNotMatchPattern`.
4. Count and emptiness helpers are implemented for arrays, collections, maps, character sequences, and iterables: `shouldHaveCount`, `shouldBeEmpty`, and `shouldNotBeEmpty`.
5. Map key/value helpers are implemented: `shouldHaveKey`, `shouldNotHaveKey`, `shouldHaveValue`, and `shouldNotHaveValue`.
6. `ObjectBehavior` direct convenience assertions were expanded and delegate through `match(actual)` so direct assertions share the same matcher behavior as fluent typed proxy or explicit wrapper usage.
7. `MatcherRegistry` keeps zero runtime dependencies and exposes a default negated-equality matcher while preserving custom matcher registration.
8. `SpecDiscovery` recognizes the expanded chained matcher names for method-discovery/default-return inference where applicable.

Verification:

- `mvn verify` passed.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Known limitation:

- Count and emptiness on a generic `Iterable` consume the iterable and can hang on infinite iterables.

Acceptance criteria status:

- Generated typed proxy/support classes continue to expose PHPSpec-like Java syntax through `Matchable<T>`.
- Negated equality, string, count/empty, and map key/value expectations are implemented for the documented subset.
- Throw matchers still support method calls and `duringInstantiation()` from the earlier runner/construction work.
- Custom matcher registration still requires no runtime dependencies.

### Phase 8 — Collaborators and Doubles (Completed)

**Status:** Implemented and verified for the current zero-dependency interface-doubles MVP.

**Relevant ADRs:** ADR 0002, ADR 0007.

Implementation summary:

1. Added `org.javaspec.doubles` with a JDK dynamic proxy implementation for ordinary interface doubles.
2. Added public factory/control/history/verification APIs through `Doubles`, `InterfaceDouble`, `DoubleControl`, `MethodStub`, `CallVerifier`, and `Call`.
3. Added `ObjectBehavior` convenience APIs for `doubleFor`, `interfaceDouble`, `doubleControl`/`inspectDouble`, call history, call counts, called/not-called assertions, and exact-count assertions.
4. Implemented stubbing by method name with any arguments and by method name with exact arguments; exact matching supports `null` values and array-content comparison.
5. Implemented call recording and immutable call-history snapshots, including method-name and exact-argument filtering.
6. Implemented verification for called, not called, called once, and exact call count checks through fluent and direct APIs.
7. Handled `toString`, `equals`, and `hashCode` deterministically in the proxy invocation handler.
8. Returned Java defaults for unstubbed methods: primitive defaults, `null` for reference types, and no-op behavior for `void` methods.
9. Added explicit unsupported-target diagnostics for `null`, primitives, arrays, annotations, enums, concrete classes, and final classes.
10. Preserved the zero-runtime-dependency policy; no bytecode, mocking, assertion, or callback libraries were added.

Verification:

- `mvn verify` passed with 328 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Known limitations:

- The core runtime supports ordinary interface doubles only.
- Concrete class, final class, static method, and constructor doubles are not implemented.
- Argument matchers, throwing stubs, and answer callbacks are implemented in Phase 28. Ordered sequences, broader side-effect stubbing, and any concrete/static/final/constructor/bytecode mocking remain future work outside the current interface-only core boundary.
- Default interface methods are not invoked by the proxy handler.
- Advanced doubles that require bytecode libraries must remain future optional integrations outside the core runtime unless a future ADR changes the policy.

Acceptance criteria status:

- Interface doubles work on Java 8 without third-party libraries.
- Limitations are explicit and tested.
- The API does not require non-JDK runtime artifacts.

### Phase 9 — CLI Expansion (Completed)

**Status:** Implemented and verified for the current run-control increment.

Implementation summary:

1. Preserved the existing `describe`/`desc` spec-only behavior and the `run` discovery/generation/update/execution flow.
2. Added `javaspec run --dry-run`: no files are written, no prompts are shown, and the CLI reports would-generate/would-update actions for related specs/support, support updates, constructor changes, supported method bodies/declarations/elements, and missing production type generation.
3. Defined dry-run exits: `1` when pending generation/update work exists; `0` when no pending changes exist and executable examples pass or are skipped/pending-only; failed or broken executable examples still produce exit `1`.
4. Added `javaspec run --stop-on-failure`: the runner stops after the first FAILED or BROKEN executable example. Without this flag, the default remains to process all discovered example metadata.
5. Added `javaspec run --formatter <progress|pretty>`: `progress` is concise and summary-oriented; `pretty` prints per-example status lines plus failed/broken/skipped details. Phase 22 extends built-in summaries/details with pending counts and pending examples. A valid CLI formatter overrides config; otherwise the configured formatter or default `progress` is used.
6. Added `javaspec run --profile <java8|java11|java17|java21|java25>`: the profile is validated and selected, and a valid CLI profile overrides config. Phase 26 now enforces the effective profile before generation/update writes while broader compiler-grade checks remain future work.
7. Added `javaspec run --verbose`: prints selected suite, spec root, source root, spec package prefix, production package prefix, constructor policy, profile, formatter, dry-run, and stop-on-failure.
8. Rejected the new run-only flags for `describe`/`desc`, along with existing run-only controls such as `--generate`, `--constructor-policy`, `--class`, and `--example`.
9. Preserved zero-runtime-dependency CLI parsing; no external CLI/formatter dependency was added.

Verification:

- `mvn verify` passed with 338 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Acceptance criteria status:

- CLI works on Java 8-compatible production code.
- Unknown commands and invalid options produce clear diagnostics.
- CLI help documents constructor policy, dry-run, stop-on-failure, formatter, profile, verbose, filters, and run/describe ownership.
- `describe` preserves first-MVP spec-only behavior, while `run` preserves missing-production-class and missing-method suggestion behavior.
- CLI generation-related actions remain gated by explicit confirmation, dry-run reporting, or documented non-interactive `--generate` behavior.

### Phase 10 — Advanced Code Generation (Completed Increment)

**Owner:** Java implementation agent.

**Status:** Implemented and verified for the current advanced code-generation increment focused on interface-style methods. Broader advanced generation remains future work.

Implementation summary:

1. Preserved the existing class, final-class, sealed-class, enum, and record method-body generation behavior.
2. Updated missing production `INTERFACE` skeleton rendering so discovered non-static methods are emitted as declarations ending in `;`; static descriptors are skipped.
3. Updated missing production `ANNOTATION` skeleton rendering so compatible no-argument non-static descriptors are emitted as annotation elements; incompatible descriptors are skipped.
4. Updated missing production `SEALED_INTERFACE` skeleton rendering so root method declarations are emitted and generated nested permitted classes implement those methods with Java default-return bodies, keeping generated Java 17 source forms valid.
5. Updated existing ordinary interface sources so missing method declarations can be inserted source-preservingly and idempotently.
6. Updated existing annotation sources so compatible missing elements can be inserted source-preservingly and idempotently.
7. Intentionally skipped existing sealed-interface source updates for now because source-preserving updates must also update nested permitted implementations safely.
8. Production files changed: `src/main/java/org/javaspec/generation/TypeSkeletonGenerator.java` and `src/main/java/org/javaspec/generation/ClassMethodUpdater.java`.
9. Test files changed: `src/test/java/org/javaspec/generation/TypeSkeletonGeneratorTest.java` and `src/test/java/org/javaspec/generation/ClassMethodUpdaterTest.java`.

Verification:

- `mvn -Dtest='org.javaspec.generation.*Test' test` passed with 84 generation tests.
- `mvn verify` passed with 345 tests.
- `mvn dependency:tree -Dscope=runtime` passed and showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.

Known limitations:

- Existing sealed-interface source updates are deferred until the updater can insert both root declarations and matching nested permitted implementation bodies source-preservingly.
- Annotation element generation only emits descriptors that are valid Java annotation elements; descriptors with parameters, static descriptors, `void`, `Object`, or otherwise incompatible return types are ignored for annotation sources.
- Source parsing and generation remain Java 8-compatible heuristics rather than a full Java parser.

Remaining advanced-generation work:

1. Keep the ADR 0004 correction behavior stable in spec/support/proxy generation, constructor policy handling, instance method skeleton generation, and static factory skeleton generation.
2. Refine static factory/named-constructor generation and add private constructor generation only after current construction and factory semantics remain stable.
3. Provide templates for spec classes, support/proxy classes, examples, and generated production code where useful.
4. Add return constant generation only after matcher and example semantics can justify it.
5. Extend enum generation and broader interface/annotation generation only when the production type skeleton flow remains stable and documented.
6. Confirm before writing files unless a non-interactive flag or documented policy is provided.
7. Keep generated Java source compatible with the configured target profile and Java 8 binary rules.

Acceptance criteria status:

- Generation remains deterministic and reviewable.
- Existing constructor updates still follow the ADR 0004 policy states and defaults.
- Generated typed support/proxy classes continue to preserve PHPSpec-like Java syntax.
- Missing method prompts remain actionable and reuse the existing `--generate` non-interactive behavior.
- Existing ordinary interface and annotation updates are source-preserving and idempotent.
- Advanced generation features do not expand the zero-runtime-dependency surface.

### Phase 11 — Formatters, Reporting, and Extensions (Completed Increment)

**Owner:** Java implementation agent.

**Status:** Implemented and verified for the current formatter/reporting/extension increment. External extension loading and broader plugin behavior remain future work.

Implementation summary:

1. Added the public zero-dependency `org.javaspec.formatter.RunFormatter` contract and deterministic `RunFormatterRegistry`.
2. Moved built-in run output behind `ProgressRunFormatter`, `PrettyRunFormatter`, and `RunFormatterSupport`, preserving Phase 9-compatible `progress` and `pretty` CLI output.
3. Added minimal extension lifecycle contracts under `org.javaspec.extension`: `JavaspecExtension`, short-name alias `Extension`, and `ExtensionContext`.
4. `ExtensionContext` exposes the run formatter registry through `runFormatterRegistry()` and `runFormatters()` so extensions can register formatters programmatically.
5. Added `org.javaspec.reporting.RunReportWriter`, a dependency-free UTF-8 JSON writer for immutable runner results.
6. Added `javaspec run --report <file>` and alias `--report-file <file>`.
7. JSON reports use `schemaVersion` 1 and include whole-run summary counts, specs, examples, nullable failure details, throwable class/message, and stack trace lines. Phase 18 additively includes spec `id`, `stableId`, and `sourceFile` plus example `id`, `stableId`, `fullName`, and `source { file, line }` while preserving existing fields; Phase 22 additively includes `pending` summary counts and `PENDING` example statuses.
8. `--report` is run-only and rejected by `describe`/`desc`; `--verbose` prints the report path when specified.
9. No-spec runs with `--report` write a valid empty report. Passing, failing, broken, skipped-only, and pending-only runs write reports after normal summary rendering; failed or broken executable examples still exit `1` after the report write.
10. Dry-run pending generation/update exits before execution and does not write a report. Report write failures produce I/O diagnostics, include the report path, and exit `70`.
11. External extension loading was not part of Phase 11. Phase 25 now implements JDK `ServiceLoader` discovery for external formatter/extension providers on CLI and Gradle run classloaders; configuration-driven activation, classpath/package scanning, plugin lookup, Maven plugin formatter controls, and JUnit Platform formatter controls remain out of scope.

Production files changed/added:

- `src/main/java/org/javaspec/cli/Main.java`
- `src/main/java/org/javaspec/formatter/RunFormatter.java`
- `src/main/java/org/javaspec/formatter/RunFormatterRegistry.java`
- `src/main/java/org/javaspec/formatter/ProgressRunFormatter.java`
- `src/main/java/org/javaspec/formatter/PrettyRunFormatter.java`
- `src/main/java/org/javaspec/formatter/RunFormatterSupport.java`
- `src/main/java/org/javaspec/extension/JavaspecExtension.java`
- `src/main/java/org/javaspec/extension/Extension.java`
- `src/main/java/org/javaspec/extension/ExtensionContext.java`
- `src/main/java/org/javaspec/reporting/RunReportWriter.java`

Test files added:

- `src/test/java/org/javaspec/formatter/RunFormatterRegistryTest.java`
- `src/test/java/org/javaspec/extension/ExtensionContextTest.java`
- `src/test/java/org/javaspec/reporting/RunReportWriterTest.java`
- `src/test/java/org/javaspec/cli/MainPhase11ReportCliTest.java`

Verification:

- `mvn -q -Dtest=RunFormatterRegistryTest,ExtensionContextTest,RunReportWriterTest,MainPhase11ReportCliTest test` passed.
- `mvn verify` passed with 364 tests, 0 failures, and 0 errors.
- `mvn dependency:tree -Dscope=runtime` passed and showed only `org.javaspec:javaspec` in runtime scope.

Known limitations:

- Formatter extension contracts are public and programmatic, and Phase 25 adds CLI/Gradle ServiceLoader discovery for provider names on the effective run classloader. Configuration-driven extension activation, package scanning, plugin lookup, Maven plugin formatter controls, and JUnit Platform formatter controls remain future work.
- JSON reporting remains schemaVersion 1 runner results with Phase 18 additive stable identifier/source fields and Phase 22 additive pending counts/statuses; Phase 24 adds config-level destinations only, not alternate schemas or streaming mode. Phase 14 adds a separate JUnit XML-compatible report path, Phase 18 additively emits `<testcase>` `file`/`line` attributes when source data is available, and Phase 22 maps skipped plus pending examples to JUnit XML `<skipped>` for compatibility.
- Reports are not written when dry-run exits before execution because pending generation/update work exists.

Acceptance criteria status:

- Human-readable output remains clear and compatible for local use.
- CI receives stable exit codes and optional zero-dependency machine-readable JSON output.
- Extensions can register run formatters programmatically without modifying core internals; broader extension capabilities remain future work.

### Phase 12 — Compatibility and Quality Matrix (Completed via Distrobox Multi-JDK Matrix)

**Owner:** Java tester/quality agents.

**Status:** Completed and verified on 2026-06-03 through Distrobox `1.8.2.5` with Podman `5.8.2`. See [`docs/test-report.md`](docs/test-report.md) for the consolidated quality matrix.

Verification summary:

1. Distrobox/Podman container tooling was used to run the full Java 8, 11, 17, 21, and 25 matrix with Maven Temurin images.
2. Each container executed `java -version`, `javac -version`, `mvn -version`, `mvn clean`, and `mvn verify` from `/home/paperboy/workspace/javaspec`.
3. Java 8 (`1.8.0_492`), Java 11 (`11.0.31`), Java 17 (`17.0.19`), Java 21 (`21.0.11 LTS`), and Java 25 (`25.0.3 LTS`) all passed with 364 tests, 0 failures, 0 errors, and 0 skipped.
4. Maven `3.9.16` was used in every matrix container.
5. JDK 17+ emitted only expected `-source 8` / `-target 1.8` bootstrap/obsolete-option warnings.
6. Java 25 runtime reflection probing passed for `java.util.stream.Gatherer`, `java.util.stream.Gatherer$Downstream`, `java.util.stream.Gatherer$Integrator`, `java.util.stream.Gatherer$Integrator$Greedy`, and `java.util.stream.Gatherers`.
7. Runtime dependency auditing in `javaspec-jdk25-matrix` with `mvn dependency:tree -Dscope=runtime` passed and showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.
8. Matrix containers were created and stopped, not removed: `javaspec-jdk8-matrix`, `javaspec-jdk11-matrix`, `javaspec-jdk17-matrix`, `javaspec-jdk21-matrix`, and `javaspec-jdk25-matrix`.
9. Blockers: none.

Distrobox JDK matrix:

| JDK | Image | Container | Runtime | Maven | Phase 12 result |
|---|---|---|---|---|---|
| Java 8 | `docker.io/library/maven:3.9-eclipse-temurin-8` | `javaspec-jdk8-matrix` | `1.8.0_492` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 11 | `docker.io/library/maven:3.9-eclipse-temurin-11` | `javaspec-jdk11-matrix` | `11.0.31` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 17 | `docker.io/library/maven:3.9-eclipse-temurin-17` | `javaspec-jdk17-matrix` | `17.0.19` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 21 | `docker.io/library/maven:3.9-eclipse-temurin-21` | `javaspec-jdk21-matrix` | `21.0.11 LTS` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 25 | `docker.io/library/maven:3.9-eclipse-temurin-25` | `javaspec-jdk25-matrix` | `25.0.3 LTS` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |

Acceptance criteria status:

- Java 8 remains a hard compatibility gate and now has a passing native Java 8 Distrobox run in addition to the compiler, bytecode, and constant-pool compatibility evidence.
- Runtime dependency audit passed with only the root project artifact in runtime scope.
- LTS profiles were exercised across Java 8, 11, 17, 21, and 25 containers, and every matrix entry passed.
- Java 25 stream Gatherer metadata was validated by runtime reflection on Java 25.
- First-MVP, ADR 0004 correction, Phase 4 configuration/filter, Phase 8 doubles, Phase 9 CLI, Phase 10 generation, and Phase 11 formatter/reporting/extension behavior remain covered by the automated test suite summarized above.

### Phase 13 — Documentation Completion

**Owner:** documenter, with C4 delegated to c4model if requested.

**Status:** Completed documentation pass for ARC42 sections 5-12, ADR coverage, user manual navigation, PHPSpec-to-Java migration notes, and Phase 12 report references. No C4 diagrams were required.

Tasks:

1. Extend ARC42 sections 5-12 after implementation architecture stabilizes.
2. Add ADRs for major design choices made during implementation.
3. Maintain and extend integrated test and quality reports produced by tester agents; Phase 12 is captured in [`docs/test-report.md`](docs/test-report.md).
4. Keep `docs/usermanual/Home.md` and `docs/usermanual/_Sidebar.md` synchronized after implementation begins.
5. Add user guide and migration notes from PHPSpec concepts to Java concepts.
6. Document configuration files, construction, constructor policy, typed matcher syntax, method generation including Phase 10 interface declarations and annotation elements, interface doubles, Phase 9 run controls, Phase 11 formatter/reporting/extension behavior, limitations, commands, examples, the first-MVP generator flow, and later advanced generator behavior according to what is implemented.

Acceptance criteria:

- Documentation reflects implemented behavior.
- `docs/usermanual/Home.md` and `docs/usermanual/_Sidebar.md` expose the same navigation topics.
- ADRs link to relevant ARC42 sections.
- Test and quality claims are backed by produced reports.

### Phase 14 — Test Integration Foundation Without JUnit (Completed)

**Owner:** Java implementation and tester/quality agents.

**Status:** Implemented and verified on 2026-06-03.

**Relevant ADRs:** ADR 0002, ADR 0006, ADR 0010, ADR 0011. ADR 0011 is sufficient for this phase; no new ADR was required because the implementation follows the accepted canonical-runner/no-JUnit integration decision.

Implemented scope:

1. Added the programmatic no-`System.exit` invocation API under `org.javaspec.invocation`:
   - `JavaspecInvocation` models either a `SpecDiscoveryRequest` or an already discovered spec list, the selected `ClassLoader`, and stop-on-failure behavior.
   - `JavaspecLauncher` delegates to canonical `SpecDiscovery`, `SpecRunner`, and `RunResult` semantics and returns structured results instead of terminating the JVM.
   - `JavaspecInvocationResult` exposes immutable discovered specs, the `RunResult`, exit code, success state, and failure presence helpers.
   - `JavaspecExitCode` maps passing, skipped/pending-only, and no-spec runs to `0`, and failed or broken runs to `1`.
2. Added explicit classpath input for `javaspec run`:
   - `--classpath <path-list>` reads entries separated by `File.pathSeparator`.
   - `--classpath-file <file>` reads UTF-8 entries, one per non-empty line, ignoring lines whose trimmed form begins with `#`.
   - Both options are run-only and rejected by `describe`/`desc`.
   - Verbose output lists explicit entries.
   - The selected explicit classloader is used for production type existence checks and spec execution.
3. Added dependency-free JUnit XML-compatible reporting:
   - `org.javaspec.reporting.JUnitXmlReportWriter` writes UTF-8 XML from immutable `RunResult` data without JUnit or XML/reporting library dependencies.
   - `javaspec run --junit-xml <file>` and alias `--junit-xml-file <file>` request the report.
   - No-spec and executed run paths write requested reports after normal output; failing or broken executable examples still exit `1` after report writes.
   - Dry-run pending generation/update exits before execution and writes no reports.
   - JUnit XML report I/O failures exit `70` and include path diagnostics.
   - Existing JSON `--report` / `--report-file` behavior remains unchanged, and JSON plus JUnit XML can be requested together.
   - Phase 18 additively emits `file` and `line` attributes on `<testcase>` when source data is available while preserving dependency-free JUnit XML-compatible output.
4. Preserved current `describe`/`run` generation behavior and the zero-runtime-dependency policy.
5. Kept programmatic invocation and adapter execution classpath-based. Phase 29 later adds a CLI-only opt-in compiler step; without that option, explicit classpath entries and adapter-supplied classpaths must point to already compiled classes or archives.

Verification:

- `mvn -q -Dtest=JavaspecLauncherTest,JUnitXmlReportWriterTest,MainPhase14CliTest test` passed with 18 tests.
- `mvn verify` passed with 382 tests, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` passed and the runtime tree contained only the root artifact.

Acceptance criteria status:

- Core runtime dependency auditing still shows no third-party runtime dependencies.
- Programmatic callers can invoke the canonical runner without `System.exit` terminating the host process.
- CLI no-JUnit CI can provide a classpath, run compiled specs, receive deterministic exit behavior, and collect JSON and/or JUnit XML-compatible reports.
- Existing `describe`/`run` generation behavior remains unchanged except for documented run-only classpath and report options.

### Phase 15 — Maven Plugin Optional Integration (Completed)

**Owner:** Maven/build integration agent as delegated by the parent workflow.

**Status:** Implemented and verified on 2026-06-03.

**Relevant ADRs:** ADR 0002, ADR 0011. ADR 0011 is sufficient for this phase: the plugin is an optional adapter over the canonical runner, keeps JUnit out of core and out of projects under test, and remains separate from the core runtime artifact.

Implemented scope:

1. Added standalone optional Maven plugin artifact `javaspec-maven-plugin/`, intentionally not registered as a root module so repository-root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact.
2. `javaspec-maven-plugin/pom.xml` packages `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin`, uses Java source/target `1.8`, goal prefix `javaspec`, Maven API baseline `3.6.3`, Maven API and plugin annotations in `provided` scope, JUnit in `test` scope, and a compile-scope dependency on core `org.javaspec:javaspec`.
3. `JavaspecRunMojo` provides `javaspec:run`, bound by default to phase `verify`, requires test dependency resolution, and uses the Maven test classpath for compiled production/spec classes.
4. The Mojo supports config, suite, `specDir`/`specRoot` selection, class/example filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible reports, and Maven logging.
5. The Mojo delegates to canonical no-JUnit `org.javaspec.invocation.JavaspecLauncher` and avoids `System.exit` and direct low-level runner coupling.
6. No JUnit is required in projects under test; JUnit remains only a plugin test dependency.
7. Typical local standalone plugin verification requires installing the current core first: root `mvn install` or `mvn -q -DskipTests install`, followed by `mvn -f javaspec-maven-plugin/pom.xml verify`.

Verification:

- `mvn -q verify` passed with 382 core tests.
- `mvn -q -DskipTests install` passed to install the current core for standalone plugin verification.
- `mvn -q -f javaspec-maven-plugin/pom.xml -Dtest=JavaspecRunMojoTest test` passed with 12 plugin tests.
- `mvn -q -f javaspec-maven-plugin/pom.xml verify` passed with 12 plugin tests.
- `mvn dependency:tree -Dscope=runtime` passed; root runtime tree contained only `org.javaspec:javaspec`.
- `mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime` passed; plugin runtime tree contained the plugin plus compile-scope core `org.javaspec:javaspec` only.
- Plugin test coverage includes JUnit XML report I/O failure handling, plugin POM dependency scopes, and a canonical launcher guard verifying Mojo delegation to `org.javaspec.invocation.JavaspecLauncher` without `System.exit` or direct low-level runner coupling.

Acceptance criteria status:

- Maven users can run javaspec specs through the optional plugin without adding JUnit.
- Plugin behavior is an adapter over the canonical javaspec invocation API and result model.
- Core runtime dependency and Java 8 compatibility gates remain unaffected because the plugin is a standalone optional artifact and root verification remains scoped to the core artifact.

### Phase 16 — Gradle Plugin Optional Integration (Completed)

**Owner:** Gradle/build integration agent as delegated by the parent workflow.

**Status:** Implemented and verified on 2026-06-03.

**Relevant ADRs:** ADR 0002, ADR 0011. ADR 0011 is sufficient for this phase: the plugin is an optional build-tool adapter over the canonical runner, keeps JUnit out of core and out of projects under test, and remains separate from the core runtime artifact.

Implemented scope:

1. Added standalone optional Gradle plugin artifact `javaspec-gradle-plugin/`, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact.
2. Added plugin scaffold files `settings.gradle`, `build.gradle`, plugin-local `.gitignore`, and plugin-local `README.md`.
3. `javaspec-gradle-plugin/build.gradle` uses `java-gradle-plugin`, group `org.javaspec`, version `0.1.0-SNAPSHOT`, Java source/target `1.8`, plugin id `org.javaspec`, implementation class `org.javaspec.gradle.JavaspecPlugin`, Maven local/core dependency `org.javaspec:javaspec:0.1.0-SNAPSHOT`, and plugin-local TestKit/JUnit test dependencies.
4. Added `JavaspecPlugin`, `JavaspecExtension`, and `JavaspecRunTask` under `javaspec-gradle-plugin/src/main/java/org/javaspec/gradle/`.
5. The plugin registers extension `javaspec` and task `javaspecRun` in Gradle's `verification` group. When the Gradle Java plugin/source sets are present, `javaspecRun` defaults to the `test` source set runtime classpath and depends on `testClasses`.
6. The task supports `skip`, `failOnFailure` defaulting to true, `stopOnFailure`, `configFile`, `suite`, `specDir`/`specRoot`, class filters, example filters, built-in formatter `progress|pretty`, JSON report file aliases (`reportFile`, `jsonReportFile`), and JUnit XML-compatible report file aliases (`junitXmlReportFile`, `junitXmlFile`).
7. The task loads javaspec configuration when configured, selects suites, builds `SpecDiscoveryRequest` with `SpecNamingConvention`, uses a `URLClassLoader` over the Gradle classpath, sets/restores the thread context classloader, closes the loader, writes reports via core writers, logs through Gradle, throws `GradleException` on failed/broken examples when `failOnFailure=true`, and delegates to canonical no-JUnit `JavaspecLauncher` without `System.exit`.
8. No JUnit is required in projects under test; JUnit is only a plugin test dependency.

Verification:

- Added `javaspec-gradle-plugin/src/test/java/org/javaspec/gradle/JavaspecGradlePluginTest.java` with 11 tests.
- Test coverage includes task registration/default `testClasses` wiring, plugin id/no-spec success, compiled passing spec via default test source set runtime classpath, JSON/JUnit XML report writing, failed/broken examples fail by default after reports, `failOnFailure=false`, `skip=true`, config/suite/class/example filters, explicit `specRoot`, invalid config diagnostics, invalid report path diagnostics, and canonical launcher/no-`System.exit` source guard.
- `mvn -q -DskipTests install` passed.
- `mvn -q verify` passed.
- `mvn dependency:tree -Dscope=runtime` passed; root runtime tree contains only `org.javaspec:javaspec`.
- A cached Gradle 7.4.2 command was attempted but blocked by the installed Java 21 runtime with `Unsupported class file major version 65`; this is an environment/tooling compatibility blocker for that cached executable, not a javaspec feature failure.
- Java 21-compatible workaround verification used Gradle 8.8 downloaded to `/tmp/gradle-8.8` and not committed. `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin test` passed with 11 tests; `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin build` passed; `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath` passed showing runtimeClasspath only `org.javaspec:javaspec:0.1.0-SNAPSHOT`; `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration testRuntimeClasspath` passed showing javaspec, JUnit, and Hamcrest only.

Acceptance criteria status:

- Gradle users can run javaspec specs through the optional plugin without adding JUnit to projects under test.
- Plugin behavior is an adapter over the canonical javaspec invocation API and result model.
- Core runtime dependency and Java 8 compatibility gates remain unaffected because the plugin is a standalone optional artifact and root Maven verification remains scoped to the core artifact.

### Phase 17 — Optional JUnit Platform Engine (Completed)

**Owner:** Optional integration agent as delegated by the parent workflow.

**Status:** Implemented and verified on 2026-06-04.

**Relevant ADRs:** ADR 0002, ADR 0006, ADR 0011. ADR 0011 is sufficient for this phase: the engine is a separate optional adapter over the canonical runner, keeps JUnit Platform dependencies out of the core runtime artifact, and preserves no-JUnit execution as first-class.

Implemented scope:

1. Added standalone optional JUnit Platform engine artifact `javaspec-junit-platform-engine/`, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact.
2. The artifact is `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging `jar`, Java source/target `1.8`, using Java 8-compatible JUnit Platform `1.10.2` and avoiding JUnit Platform 6/JUnit 6.
3. Runtime dependencies are isolated to the optional engine artifact: core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, and transitives `opentest4j`, `junit-platform-commons`, and `apiguardian-api`. Engine test-only dependencies include JUnit Platform Launcher, JUnit Platform TestKit, and JUnit Jupiter.
4. Main implementation is `javaspec-junit-platform-engine/src/main/java/org/javaspec/junit/platform/JavaspecTestEngine.java`.
5. ServiceLoader registration is provided by `javaspec-junit-platform-engine/src/main/resources/META-INF/services/org.junit.platform.engine.TestEngine`, containing `org.javaspec.junit.platform.JavaspecTestEngine`; the engine id is `javaspec`.
6. Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`; configuration parameters include `javaspec.configFile`, `javaspec.suite`, `javaspec.specDir`/`javaspec.specRoot`, `javaspec.classFilters`/`classFilter`/`class`, `javaspec.exampleFilters`/`exampleFilter`/`example`, and `javaspec.stopOnFailure`.
7. Class, package, method, and unique-id selectors are supported as filters over canonical discovery results.
8. Execution delegates to canonical no-JUnit `JavaspecLauncher` using discovered specs. It maps javaspec result states to JUnit Platform listener events: passed to successful, failed assertion results to failed assertion-style throwables, broken results to failed/error-style throwables, and skipped/pending/non-loadable results to skipped events; Phase 22 prefixes pending skip reasons with `Pending:`.
9. UniqueId segments use `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and `[example:<methodName>]`; Phase 18 retains this stable unique-id shape and MethodSource behavior while aligning descriptor reporting to stable ids.
10. The engine avoids `System.exit` and does not require changes to javaspec spec authoring style.
11. The engine is an optional IDE/CI adapter only. Projects that do not opt into it still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths.

Verification:

- Added `javaspec-junit-platform-engine/src/test/java/org/javaspec/junit/platform/JavaspecTestEnginePhase17Test.java` with 12 tests.
- Test coverage includes ServiceLoader engine id discovery, empty/no-spec discovery and execution, compiled passing spec success, assertion failure mapping, non-assertion throwable mapping, source-only/non-loadable spec skipped, config/suite/specRoot/specDir/class/example filters, class/package/method/unique-id selectors, stop-on-failure skip behavior, and canonical launcher/no-`System.exit`/no-CLI-adapter source guard.
- `mvn -q -DskipTests install` passed.
- `mvn -q verify` passed; root Surefire reported 382 tests, 0 failures, 0 errors, and 0 skipped.
- `mvn -q -f javaspec-junit-platform-engine/pom.xml -Dtest=JavaspecTestEnginePhase17Test test` passed with 12 tests, 0 failures, 0 errors, and 0 skipped.
- `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` passed with 12 tests, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` passed; root runtime tree is only `org.javaspec:javaspec`.
- `mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime` passed; runtime dependencies are core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`; runtime does not include `junit-jupiter`, `junit-platform-launcher`, or `junit-platform-testkit`.
- Blockers: none.

Acceptance criteria status:

- Projects that do not opt into the JUnit Platform engine still have no JUnit dependency and can keep using CLI/build-tool no-JUnit execution.
- Projects that opt into the separate engine can discover and execute javaspec specs through JUnit Platform-enabled tools using the `javaspec` engine id and ServiceLoader registration.
- The canonical javaspec runner remains the source of truth for execution behavior and result semantics.

### Phase 18 — IDE/CI Polish (Completed Increment)

**Owner:** Java implementation, documentation, and tester/quality agents as delegated by the parent workflow.

**Status:** Implemented and verified on 2026-06-04 as the stable identifier/source-location/report polish increment.

**Relevant ADRs:** ADR 0010, ADR 0011. No new ADR was required because the implementation stayed within the accepted zero-dependency reporting and canonical-runner/optional-adapter boundaries.

Implemented scope:

1. Added stable id aliases for result and discovery objects:
   - `ExampleResult.id()` / `stableId()` / `getId()` / `getStableId()` use `<specQualifiedName>#<methodName>` semantics matching existing `fullName()`.
   - `SpecResult.id()` / `stableId()` / `getId()` / `getStableId()` derive from the spec qualified name.
   - `DiscoveredSpec.id()` / `stableId()` / `getId()` / `getStableId()` derive from the spec qualified name.
2. Added source metadata where available:
   - `SpecExample` carries a 1-based `sourceLine`.
   - `SpecDiscovery.extractExamples` computes method declaration lines for discovered example methods.
   - `ExampleResult` carries `sourceFilePath` and `sourceLine` when created from discovered specs/examples.
   - `SpecResult` carries the spec source file path.
3. Updated reports additively while preserving existing fields:
   - JSON `RunReportWriter` includes spec `id`, `stableId`, and `sourceFile` plus example `id`, `stableId`, `fullName`, and `source { file, line }`.
   - `JUnitXmlReportWriter` includes `file` and `line` attributes on `<testcase>` when source data is available and remains dependency-free/JUnit XML-compatible.
4. Retained the optional JUnit Platform engine's stable unique-id shape and MethodSource behavior, with descriptor reporting aligned to stable ids.
5. Did not implement unrelated IDE/CI items in this increment. Pending examples were implemented later in Phase 22, broad classpath/execution availability diagnostics in Phase 23, external formatter/extension discovery in Phase 25, target-profile enforcement in Phase 26, bootstrap execution in Phase 27, and CLI opt-in compilation in Phase 29.

Verification:

- Targeted changed core tests passed: `mvn -q -Dtest=SpecDiscoveryNamingTest,SpecRunnerTest,RunReportWriterTest,JUnitXmlReportWriterTest,MainPhase11ReportCliTest,MainPhase14CliTest test`.
- Full core tests passed: `mvn -q test` — 386 tests, 0 failures, 0 errors, 0 skipped.
- Full core verification passed: `mvn -q verify` — 386 tests, 0 failures, 0 errors, 0 skipped.
- Root runtime dependency audit passed: `mvn dependency:tree -Dscope=runtime` — root runtime tree contains only `org.javaspec:javaspec`.
- Core install passed: `mvn -q install`.
- Standalone Maven plugin verification passed: `mvn -q -f javaspec-maven-plugin/pom.xml verify` — 12 tests.
- Standalone JUnit Platform engine verification passed: `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` — 12 tests.
- Standalone Gradle plugin verification passed: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build` — 11 tests.
- Gradle plugin runtimeClasspath audit passed: `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath`.

Acceptance criteria status:

- Stable identifiers are available in core discovery/results and are reflected in JSON reports and optional JUnit Platform descriptor reporting.
- Source file/line metadata is propagated from discovery into runner results and reports where available.
- Existing JSON and JUnit XML-compatible report consumers retain the previously documented fields/output shape, with Phase 18 fields added additively.
- No-JUnit CLI/programmatic/Maven/Gradle paths remain first-class, and the JUnit Platform engine remains optional.
- The original numbered roadmap is complete through Phase 18. Future feature work should be tracked as new roadmap/backlog items rather than as an implicit numbered phase.

### Phase 19 — Post-Roadmap Release/CI Hardening (Completed Increment)

**Owner:** Release/CI implementation, documentation, and tester/quality agents as delegated by the parent workflow.

**Status:** Implemented and verified on 2026-06-04 as a non-disruptive post-roadmap release/CI hardening increment.

**Relevant ADRs:** ADR 0012. ADR 0012 records the decision not to convert the repository to a mandatory Maven multi-module build in this increment.

Implemented scope:

1. Preserved the current architecture: repository-root `mvn verify` verifies only the zero-runtime-dependency core artifact; `javaspec-maven-plugin/`, `javaspec-gradle-plugin/`, and `javaspec-junit-platform-engine/` remain standalone optional artifacts outside the root Maven reactor.
2. Added executable `scripts/verify-all.sh` as an aggregate local release verification script. It resolves the repository root from the script path, runs root `mvn -q verify`, audits root runtime dependencies, installs the current core snapshot with `mvn -q -DskipTests install`, verifies and audits the standalone Maven plugin, verifies and audits the standalone JUnit Platform engine, runs the standalone Gradle plugin `clean test build`, and audits the Gradle plugin `runtimeClasspath`.
3. The script supports `MAVEN_BIN` for Maven selection, `JAVASPEC_GRADLE_BIN` for explicit Gradle executable selection, and explicit `JAVASPEC_SKIP_GRADLE=1` for intentionally skipping Gradle adapter verification. Without an explicit Gradle executable, it tries repository `./gradlew`, `/tmp/gradle-8.8/bin/gradle`, and then `gradle` on `PATH`; if none are available and Gradle is not explicitly skipped, it fails with a clear diagnostic.
4. Added `.github/workflows/ci.yml` for GitHub Actions. The workflow triggers on `push`, `pull_request`, and `workflow_dispatch`; runs a core job matrix over Java 8, 11, 17, 21, and 25 using Temurin and Maven cache; and runs a Java 21 full-verification job with Maven cache, Gradle setup using Gradle 8.8, and `scripts/verify-all.sh` with `JAVASPEC_GRADLE_BIN=gradle`.
5. No Maven multi-module conversion, publishing, signing, release deployment, or secrets were implemented. Remote GitHub Actions success was later user-/maintainer-confirmed for HEAD `4d30e63` on `develop`; this environment did not independently query GitHub run IDs, URLs, durations, or logs.

Verification:

- `bash -n scripts/verify-all.sh` passed, and the script is executable.
- GitHub Actions YAML validation: actionlint/yamllint/yq were unavailable; PyYAML parsed `.github/workflows/ci.yml` as a valid YAML mapping with top-level keys `name`, `on`, and `jobs`, including jobs `core` and `full-verification`.
- `git diff --check` passed.
- `git diff --cached --check` passed.
- Temp-index whitespace check including untracked `.github/` and `scripts/` passed.
- Full aggregate verification passed with `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh`; Gradle used `/tmp/gradle-8.8/bin/gradle`, Gradle 8.8.
- Script-executed results: root `mvn -q verify` passed with 386 tests, 0 failures, 0 errors, and 0 skipped; root runtime audit passed and contained only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`; root `mvn -q -DskipTests install` passed; Maven plugin `mvn -q -f javaspec-maven-plugin/pom.xml verify` passed with 12 tests; Maven plugin runtime audit passed with runtime summary `org.javaspec:javaspec-maven-plugin` plus `org.javaspec:javaspec`; JUnit Platform engine `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` passed with 12 tests; JUnit Platform engine runtime audit passed with core, `junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`; Gradle plugin `clean test build` passed with 11 tests, 0 failures, 0 errors, and 0 skipped plus non-blocking Java 8 source/target obsolete warnings on JDK 21; Gradle plugin runtimeClasspath audit passed with `org.javaspec:javaspec:0.1.0-SNAPSHOT`.
- Blockers: none.

Acceptance criteria status:

- The repository is not converted to Maven multi-module; root Maven verification remains core-only by design.
- Standalone adapters have an explicit aggregate verification path after refreshing the local core snapshot.
- Local release/CI verification instructions and results are documented in README, the user manual, ARC42, ADR 0012, and the test report.
- The GitHub Actions workflow is documented as configured, locally YAML-parsed, and user-/maintainer-confirmed green for HEAD `4d30e63` on `develop`; no independently observed GitHub run metadata is recorded.

### Phase 20 — Release-Readiness Scaffolding (Completed Increment)

**Owner:** Release-readiness implementation, documentation, and tester/quality agents as delegated by the parent workflow.

**Status:** Implemented and locally verified on 2026-06-04 as release-readiness scaffolding only.

**Relevant ADRs:** ADR 0013. ADR 0013 records the decision to add release-readiness checks, documentation, MIT license metadata, and confirmed maintainer metadata while postponing signing, portal publication, deploy automation, and public publishing.

Implemented scope:

1. Preserved the current architecture: no Maven multi-module conversion, no runtime dependency additions, no publishing/deployment, no secrets, and no signing automation.
2. Added executable `scripts/check-version-alignment.sh`. It checks the root Maven project version, `javaspec-maven-plugin/pom.xml` project version, `javaspec-junit-platform-engine/pom.xml` project version, `javaspec-gradle-plugin/build.gradle` `version`, and Gradle plugin `javaspecCoreVersion` against one baseline.
3. Updated `scripts/verify-all.sh` to run the version-alignment check first before core, adapter, and dependency-audit verification.
4. Added `CHANGELOG.md` and `RELEASING.md` as release-process documentation. `RELEASING.md` records that MIT license and maintainer metadata are resolved and explicitly postpones public publication until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved.
5. Copied the confirmed MIT `LICENSE` exactly from `origin/main` and updated root `pom.xml`, `javaspec-maven-plugin/pom.xml`, and `javaspec-junit-platform-engine/pom.xml` with MIT license metadata, confirmed maintainer/developer metadata, safe URL, SCM, and GitHub Issues metadata plus a `release-artifacts` Maven profile for sources and javadocs only.
6. Updated `javaspec-gradle-plugin/build.gradle` with `maven-publish`, source and javadoc jar readiness, and safe POM URL/SCM/issues/license/maintainer metadata for generated POMs.
7. No signing profile, deploy plugin configuration, Central Portal publication, Gradle Plugin Portal publication/credentials, final release tag/version, final publish approval, or actual publish flow was added.

Verification:

- Tester modified no files and ran no publish/deploy/signing commands.
- `LICENSE` is identical to `origin/main:LICENSE` with blob `b990d5492f3ef404ffc145890b83e51914351bb5`.
- `bash -n scripts/check-version-alignment.sh scripts/verify-all.sh` passed, and individual script syntax checks passed.
- `scripts/check-version-alignment.sh` and `scripts/verify-all.sh` are executable (`-rwxr-xr-x`).
- `bash scripts/check-version-alignment.sh` passed; all checked versions are aligned at `0.1.0-SNAPSHOT`.
- `git diff --check` and `git diff --cached --check` passed.
- Untracked whitespace checks passed.
- Effective POM generation passed for root, Maven plugin, and JUnit engine.
- Maven metadata checks passed for root, Maven plugin, and JUnit engine: MIT License, URL `https://opensource.org/licenses/MIT`, distribution `repo`, Mario Giustiniani email, and maintainer role.
- Gradle generated POMs `pluginMaven` and `javaspecPluginMarkerMaven` include MIT license and maintainer metadata.
- `mvn -q verify` passed with 386 tests, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` passed; root runtime has no dependencies beyond `org.javaspec:javaspec`.
- `mvn -q -Prelease-artifacts -DskipTests package` passed and produced root main, sources, and javadoc jars.
- `mvn -q -DskipTests install` passed.
- Maven plugin `mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package` passed and produced main, sources, and javadoc jars.
- JUnit engine `mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests package` passed and produced main, sources, and javadoc jars.
- Standalone Maven plugin `mvn -q verify` passed with 12 tests.
- Standalone JUnit engine `mvn -q verify` passed with 12 tests.
- Gradle plugin `clean test build` passed with 11 tests, 0 failures, and main/sources/javadoc jars found; non-blocking Java 8 source/target obsolete warnings occurred on JDK 21.
- Gradle runtime dependencies contained only `org.javaspec:javaspec:0.1.0-SNAPSHOT`.
- Full aggregate `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed, covering version alignment, core verify, root audit, local install, Maven plugin verify/audit, JUnit engine verify/audit, and Gradle plugin build/audit.
- Dependency summary: core has no runtime dependencies; Maven plugin runtime is `org.javaspec:javaspec:0.1.0-SNAPSHOT`; JUnit engine runtime is `org.javaspec:javaspec:0.1.0-SNAPSHOT`, `junit-platform-engine:1.10.2`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`; Gradle plugin runtime is `org.javaspec:javaspec:0.1.0-SNAPSHOT`.
- Verification blockers: none for Phase 20 metadata verification. Publication/deploy/signing remains intentionally unimplemented because GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval remain unresolved.

Acceptance criteria status:

- Release-readiness scaffolding exists and is locally verified without changing runtime dependencies or standalone adapter boundaries.
- Version alignment is an explicit release and aggregate-verification gate.
- Source and javadoc jar readiness is locally verified for the root artifact and standalone adapters.
- Public publishing is explicitly postponed until signing, portal publication/credentials, final release version/tag, and final publish approval are resolved; MIT license and maintainer metadata are already resolved.
- Phase 20 local verification is now supplemented by user-/maintainer-confirmed remote GitHub Actions success for HEAD `5088e96` on `develop` after Phase 20/21/22 were pushed; no GitHub run IDs, URLs, durations, or logs were independently queried from this environment.

### Phase 21 — Adoption Assets, Standalone Examples, and Report Schema (Completed Increment)

**Owner:** Adoption-assets implementation, documentation, and tester/quality agents as delegated by the parent workflow.

**Status:** Implemented and locally verified on 2026-06-04 as adoption assets only. After Phase 20/21/22 were pushed, remote GitHub Actions success for HEAD `5088e96` on `develop` was user-/maintainer-confirmed.

**Relevant ADRs:** ADR 0014. ADR 0014 records the decision to keep examples, schema docs, and golden reports as standalone adoption assets and to include examples in aggregate local verification by default with explicit opt-outs, without public publication or core runtime changes.

Implemented scope:

1. Added JSON report schema documentation at `docs/schemas/run-report-v1.schema.json` for schemaVersion 1 reports.
2. Added golden report examples at `docs/examples/reports/passing-run-report-v1.json` and `docs/examples/reports/passing-junit-report.xml`.
3. Added standalone consumer examples under `examples/`:
   - `examples/maven-basic/` uses `javaspec-maven-plugin`, a simple `Calculator`/`CalculatorSpec`, and writes JSON plus JUnit XML-compatible reports under `target/javaspec/`.
   - `examples/gradle-basic/` uses Gradle plugin id `org.javaspec` through `pluginManagement { includeBuild('../../javaspec-gradle-plugin') }`, a simple `Calculator`/`CalculatorSpec`, and writes JSON plus JUnit XML-compatible reports under `build/reports/javaspec/`.
   - `examples/junit-platform-basic/` uses the standalone JUnit Platform engine and Maven Surefire configured for `*Spec`, with a simple `Calculator`/`CalculatorSpec`.
   - `examples/README.md` documents that examples are standalone consumer projects, not root Maven modules.
   - `examples/.gitignore` ignores generated `target/`, `build/`, and `.gradle/` directories under examples.
4. Added executable `scripts/verify-examples.sh`. It installs local snapshots for the core, Maven plugin, and JUnit Platform engine; verifies the Maven, JUnit Platform, and Gradle examples; and asserts generated report markers including `schemaVersion`, stable id `spec.com.example.CalculatorSpec#it_adds_two_numbers`, `PASSED`, and `line=11`.
5. Updated `scripts/verify-all.sh` so aggregate verification runs standalone examples by default after core and adapter verification. `JAVASPEC_SKIP_EXAMPLES=1` explicitly skips examples; `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` explicitly skips only the Gradle example inside `scripts/verify-examples.sh`. `MAVEN_BIN` and `JAVASPEC_GRADLE_BIN` remain the tool-selection variables.
6. No core production Java files, core test Java files, POM dependency changes, Gradle build architecture changes, public publishing, deployment, signing, secrets, or generated build outputs are part of the intended Phase 21 adoption increment.

Verification:

- Tester modified no files and ran no publish/deploy/signing commands.
- `git diff --name-only HEAD -- src/main/java src/test/java` was empty, confirming no core production/test Java changes.
- `git diff --check` passed.
- Custom whitespace/EOF checks over visible untracked and modified text files passed.
- `bash -n scripts/verify-examples.sh`, `bash -n scripts/verify-all.sh`, and `bash -n scripts/check-version-alignment.sh` passed.
- `scripts/check-version-alignment.sh`, `scripts/verify-all.sh`, and `scripts/verify-examples.sh` have executable mode `755`.
- Generated outputs under `examples/**/target`, `examples/**/build`, and `examples/**/.gradle` are ignored and absent from `git ls-files --others --exclude-standard`.
- Schema JSON parsed, golden JSON parsed, and `jsonschema` validated the golden JSON against `docs/schemas/run-report-v1.schema.json`.
- Golden XML parsed with root `testsuite`, `tests=1`, and `failures=0`.
- Golden structural sanity passed with `schemaVersion=1`, one spec, one example, stable ids present, source file present, and `line=11` present.
- `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh` passed. Maven and Gradle example reports were generated and contained `schemaVersion`, stable id, `PASSED`, and `line=11`; the JUnit Platform example Surefire report was generated and mentioned `spec.com.example.CalculatorSpec` and `it_adds_two_numbers`.
- `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed. The new examples section executed with `==> Standalone examples verification` and `PASS: standalone examples verification completed.`
- Core tests through `verify-all` remained 386 tests with 0 failures, 0 errors, and 0 skipped.
- Standalone Maven plugin, JUnit Platform engine, and Gradle plugin verification through `verify-all` remained 12, 12, and 11 passing tests respectively; the JUnit Platform example had 1 passing test.
- Root runtime dependency tree remained only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.
- Root dependency/build files were unchanged by Phase 21.
- Example runtime dependency checks reported no main runtime dependencies for `examples/maven-basic` and `examples/junit-platform-basic`, and no dependencies on `examples/gradle-basic` `runtimeClasspath`.
- Verification blockers: none.

Acceptance criteria status:

- Report schema and golden examples are documented under `docs/` and locally validated.
- Standalone examples cover Maven plugin, Gradle plugin, and JUnit Platform engine adoption paths without becoming root modules.
- Aggregate local verification covers examples by default with explicit opt-outs.
- Phase 21 local verification is now supplemented by user-/maintainer-confirmed remote GitHub Actions success for HEAD `5088e96` on `develop` after Phase 20/21/22 were pushed; no GitHub run IDs, URLs, durations, or logs were independently queried from this environment.
- Public publishing remains postponed and no publish/deploy/signing command was run.

### Phase 22 — Explicit Skipped and Pending Semantics (Completed Increment)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Implemented and locally verified on 2026-06-04. After Phase 20/21/22 were pushed, remote GitHub Actions success for HEAD `5088e96` on `develop` was user-/maintainer-confirmed.

**Relevant ADRs:** ADR 0015. ADR 0015 records the decision to keep skipped/pending semantics zero-dependency, make `PENDING` distinct from `SKIPPED` in core/JSON, and map pending to skipped in JUnit-compatible formats.

Implemented scope:

1. Added zero-dependency public API markers under `org.javaspec.api`: runtime method annotations `@Skip` and `@Pending` with `value()` and `reason()` reason aliases, unchecked signals `SkipExampleException` and `PendingExampleException`, and `ObjectBehavior.skip()`/`skip(String)`/`pending()`/`pending(String)` helpers.
2. Defined deterministic precedence: `@Skip` wins over `@Pending` when both annotations are present.
3. Implemented annotation-based skip/pending without spec instantiation and without `let()`/example/`letGo()` execution.
4. Implemented runtime skip/pending from `let()` or an example method. Successful `letGo()` preserves SKIPPED/PENDING; `letGo()` failure after a skip/pending signal reports BROKEN.
5. Added `ExampleStatus.PENDING` as a distinct result state. `skippedCount()` remains SKIPPED-only, `pendingCount()` is separate, and skipped-plus-pending/non-executed helpers support JUnit XML-compatible reports.
6. Kept exit success when only passed, skipped, and pending examples exist.
7. Updated built-in formatter summaries to include pending counts and print pending examples; skipped examples are printed deterministically.
8. Updated JSON reports to add `pending` counts in run and spec summaries and use `status: "PENDING"` for pending examples while preserving existing fields and schemaVersion 1.
9. Updated JUnit XML-compatible reports to map both SKIPPED and PENDING to `<skipped>`, with the testsuite `skipped` attribute including skipped plus pending and pending messages using `Pending: <reason>` or `Pending by javaspec.`.
10. Updated Maven plugin summary logging to include `pending=`; Gradle plugin behavior inherits pending summaries/reports through core formatter/reporters.
11. Updated the JUnit Platform engine mapping so pending examples produce `executionSkipped` with `Pending:` reason while unique IDs/descriptors remain unchanged.

Verification:

- No production Java changes were made by the tester.
- Targeted changed tests across core, CLI, reporting, invocation, Maven plugin, Gradle plugin, and JUnit Platform engine passed with 78 tests.
- `mvn -q test` passed with 399 tests.
- `mvn -q verify` passed.
- `mvn dependency:tree -Dscope=runtime` passed; root has no runtime dependencies beyond `org.javaspec:javaspec`.
- `mvn -q -DskipTests install` passed.
- Maven plugin `mvn -q -f javaspec-maven-plugin/pom.xml verify` passed with 13 tests.
- Maven plugin runtime tree contained `org.javaspec:javaspec` only.
- JUnit Platform engine `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` passed with 13 tests.
- JUnit Platform engine runtime tree contained `org.javaspec:javaspec`, `junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`.
- Gradle plugin `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build` passed with 12 tests; Java 8 obsolete source/target warnings were non-blocking.
- Gradle plugin runtimeClasspath contained `org.javaspec:javaspec` only.
- `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh` passed.
- `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed.
- Verification blockers: none.
- Phase 22 local verification is now supplemented by user-/maintainer-confirmed remote GitHub Actions success for HEAD `5088e96` on `develop` after Phase 20/21/22 were pushed; no GitHub run IDs, URLs, durations, or logs were independently queried from this environment.

Acceptance criteria status:

- Explicit skipped/pending API and runner semantics are implemented and verified without adding runtime dependencies.
- `PENDING` remains distinct from `SKIPPED` in core result data and JSON reports.
- JUnit XML-compatible and JUnit Platform mappings remain compatible by representing pending examples as skipped events/elements with pending reason text.
- Formatter, CLI, Maven, Gradle, and JUnit Platform behavior is covered by tests.
- Public publishing remains postponed and no Phase 22 publish/deploy/signing command was run.

### Phase 23 — Classpath and Execution Availability Diagnostics (Completed Increment)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Implemented and locally verified after the last user-/maintainer-confirmed remote CI point. No remote CI success is claimed here for this local increment.

**Relevant ADRs:** ADR 0016. ADR 0016 records the decision to add deterministic execution-availability diagnostics while keeping compilation and classpath assembly external to default/adapters execution.

Implemented scope:

1. Enriched non-executable runner reasons when source discovery finds specs/examples that the selected classloader cannot execute because compiled spec classes, dependencies, or expected public no-argument example methods are unavailable or stale.
2. Added `org.javaspec.diagnostics.RunDiagnostics.executionAvailabilityLines(RunResult)` for deterministic human-readable availability diagnostics based on canonical runner results.
3. Excluded intentional user `@Skip` and `PENDING` semantics from availability diagnostics.
4. Added CLI `Execution diagnostics:` output only when availability lines exist, with hints for the current process classloader or explicit classpath entry count.
5. Added Maven and Gradle adapter `javaspec:` warning diagnostics with classpath element counts when availability lines exist.
6. Preserved existing exit-code and build-failure semantics; diagnostics alone do not make skipped-only or pending-only runs fail.
7. Did not add integrated source/spec compilation, automatic classpath repair, dependency resolution, new runtime dependencies, report schema/content changes, or publication changes.

Verification:

- Implementation-phase local verification passed for execution-availability diagnostics.
- No detailed tester report with counts is recorded in this plan for Phase 23.
- No remote GitHub Actions status is claimed for this local-only increment after confirmed HEAD `5088e96`.

Acceptance criteria status:

- Users receive actionable diagnostics when discovered specs/examples cannot execute because compiled classes or methods are unavailable.
- Programmatic hosts can reuse the deterministic diagnostics helper.
- Default and adapter execution remains classpath/reflection based.

### Phase 24 — Configuration-Level Report Destinations (Completed Increment)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Implemented and locally verified after the last user-/maintainer-confirmed remote CI point. No remote CI success is claimed here for this local increment.

**Relevant ADRs:** ADR 0017. ADR 0017 records the decision to add top-level config defaults for existing JSON and JUnit XML-compatible report destinations while preserving report behavior.

Implemented scope:

1. Added optional top-level config aliases for JSON report destinations: `report`, `reportFile`, `report-file`, `jsonReport`, `jsonReportFile`, and `json-report-file`.
2. Added optional top-level config aliases for JUnit XML-compatible destinations: `junitXml`, `junit-xml`, `junitXmlFile`, `junit-xml-file`, `junitXmlReportFile`, and `junit-xml-report-file`.
3. Trimmed configured values and rejected blank destination values when the keys are present.
4. Made `javaspec run --config <file>` use configured report destinations when no corresponding CLI report option is supplied.
5. Preserved explicit precedence: CLI report options override config values; explicit Maven/Gradle adapter report settings override config values.
6. Kept `describe --config <file>` accepting these config keys but ignoring them because `describe` does not execute examples or write reports.
7. Did not change report schemas, report writers, report contents, dry-run pending behavior, no-spec behavior, exit codes, build-failure semantics, adapter architecture, or runtime dependencies.

Verification:

- Implementation-phase local verification passed for configuration-level report destinations.
- No detailed tester report with counts is recorded in this plan for Phase 24.
- No remote GitHub Actions status is claimed for this local-only increment after confirmed HEAD `5088e96`.

Acceptance criteria status:

- Configuration can provide reusable JSON/JUnit XML-compatible output defaults.
- Explicit CLI and adapter settings remain higher precedence than config defaults.
- Existing report consumers keep the same schemas and writer behavior.

### Phase 25 — ServiceLoader External Formatter and Extension Discovery (Completed Increment)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Implemented and locally verified after the last user-/maintainer-confirmed remote CI point. No remote CI success is claimed here for this local increment.

**Relevant ADRs:** ADR 0018. ADR 0018 records the decision to use JDK `ServiceLoader` for classloader-scoped formatter/extension discovery without adding dependencies or broader plugin activation.

Implemented scope:

1. Added `org.javaspec.extension.JavaspecExtensionLoader.loadRunFormatterRegistry()` and `loadRunFormatterRegistry(ClassLoader)`.
2. Loaded built-in formatters first, then external providers from `META-INF/services/org.javaspec.formatter.RunFormatter`, `META-INF/services/org.javaspec.extension.JavaspecExtension`, and alias `META-INF/services/org.javaspec.extension.Extension`.
3. Configured duplicate extension implementation classes only once when listed under both extension service types.
4. Surfaced invalid service declarations, invalid formatter providers, and extension configuration failures through explicit loading diagnostics.
5. Updated CLI `javaspec run` to load providers after effective classloader selection, so providers can come from the process classpath or explicit `--classpath` / `--classpath-file` entries.
6. Updated Gradle `javaspecRun` to load providers from its run classloader and to validate formatter names against built-in plus discovered providers.
7. Did not add Maven plugin formatter output controls, JUnit Platform formatter controls, configuration-driven extension activation, package scanning, plugin lookup, report schema/content changes, integrated compilation, runtime dependencies, or publishing changes.

Verification:

- Implementation-phase local verification passed for ServiceLoader formatter/extension discovery.
- No detailed tester report with counts is recorded in this plan for Phase 25.
- No remote GitHub Actions status is claimed for this local-only increment after confirmed HEAD `5088e96`.

Acceptance criteria status:

- CLI and Gradle runs can use externally supplied formatter/extension providers from their effective run classloaders.
- Built-in formatter behavior remains deterministic and available without providers.
- The extension boundary remains zero-dependency and narrower than general plugin activation.

### Phase 26 — Target-Profile Enforcement Before Generation/Update Writes (Completed Increment)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Implemented and locally verified after the last user-/maintainer-confirmed remote CI point. No remote CI success is claimed here for this local increment.

**Relevant ADRs:** ADR 0019. ADR 0019 records the decision to enforce the effective target profile conservatively before `run` writes generated or updated source.

Implemented scope:

1. Added `org.javaspec.compatibility.ProfileEnforcement`, `ProfileEnforcementReport`, and `ProfileViolation`.
2. Enforced the effective CLI/config/default `TargetProfile` after discovery and before related-spec generation, support updates, production skeleton writes, constructor/method updates, prompts, execution, or report writing.
3. Rejected described `record`, `sealed class`, and `sealed interface` generation below `java17`.
4. Checked generated method return and parameter signatures only when their owners resolve to known profile-catalog Java API symbols introduced after the selected profile.
5. Intentionally ignored unknown project types, unknown catalog owners, and ambiguous or unresolvable simple type names to avoid false positives.
6. Reported violations with clear `Profile compatibility error` diagnostics and exited before writes.
7. Did not add compiler-grade integrated profile checks, source/spec compilation, report schema changes, dependencies, optional adapter architecture changes, or publishing behavior.

Verification:

- Implementation-phase local verification passed for target-profile enforcement.
- No detailed tester report with counts is recorded in this plan for Phase 26.
- No remote GitHub Actions status is claimed for this local-only increment after confirmed HEAD `5088e96`.

Acceptance criteria status:

- Incompatible generation/update work fails before source files are written.
- Java 8-compatible source remains the binary baseline while later LTS capabilities are enforced through metadata and conservative source-generation checks.
- Enforcement is explicit about its non-compiler-grade boundary.

### Phase 27 — Bootstrap Hook Execution Before Examples (Completed Increment)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Implemented and locally verified after the last user-/maintainer-confirmed remote CI point. No remote CI success is claimed here for this local increment.

**Relevant ADRs:** ADR 0020. ADR 0020 records the decision to execute explicitly configured compiled Java hook classes immediately before examples.

Implemented scope:

1. Added `org.javaspec.bootstrap.BootstrapHook` and immutable `BootstrapContext`.
2. Treated top-level and selected-suite `bootstrap` config entries as fully qualified hook class names.
3. Loaded hook classes from the effective run classloader/classpath, required `BootstrapHook`, constructed hooks through public no-argument constructors, and invoked them with the run classloader plus discovered specs in context.
4. Preserved execution order: top-level hooks run before selected-suite hooks, declaration order is preserved, and duplicates are preserved.
5. Executed hooks after discovery, profile enforcement, generation/update decisions, any successful generation/update work, and any requested successful CLI compilation, but immediately before examples and before reports.
6. Skipped hook execution for CLI no-spec runs.
7. Made CLI bootstrap failures print `Error: Bootstrap execution failed`, exit `64`, skip examples, and write no reports; Maven and Gradle adapters fail clearly through the canonical invocation path.
8. Did not add ServiceLoader hook discovery, script engines, package scanning, dependency resolution, adapter-integrated compilation, runtime dependencies, Java 8 compatibility exceptions, report schema changes, or optional adapter architecture changes.

Verification:

- Implementation-phase local verification passed for bootstrap hook execution.
- No detailed tester report with counts is recorded in this plan for Phase 27.
- No remote GitHub Actions status is claimed for this local-only increment after confirmed HEAD `5088e96`.

Acceptance criteria status:

- Configured hook classes execute at a deterministic point before examples.
- Hook failures stop before examples and reports with clear diagnostics.
- Bootstrap remains a compiled-class/classloader feature, not a script or plugin framework.

### Phase 28 — Stronger Interface Doubles (Completed Increment)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Implemented and locally verified after the last user-/maintainer-confirmed remote CI point. No remote CI success is claimed here for this local increment.

**Relevant ADRs:** ADR 0021. ADR 0021 records the decision to strengthen interface doubles while preserving the JDK-proxy-only, zero-dependency boundary.

Implemented scope:

1. Added `ArgumentMatcher`, `ArgumentMatchers`, and `Doubles` matcher aliases for matcher-aware argument constraints.
2. Preserved existing exact-argument and method-wide stubbing while adding deterministic argument-constrained stub priority over method-wide stubs.
3. Added throwing stubs through `MethodStub.thenThrow(...)` / `throwsException(...)`.
4. Added answer callbacks through `MethodStub.thenAnswer(...)` / `answers(...)` with immutable `DoubleInvocation` context data.
5. Kept call history, verification helpers, deterministic object-method handling, Java default returns, and `ObjectBehavior` double conveniences synchronized with the stronger stubbing behavior.
6. Preserved the ordinary-interface-only JDK dynamic proxy boundary.
7. Did not add concrete class doubles, static/final/constructor/bytecode mocking, default-interface-method invocation, CLI behavior changes, report/schema changes, dependency changes, optional adapter changes, generated assets, examples, or publishing behavior.

Verification:

- Implementation-phase local verification passed for stronger interface doubles.
- No detailed tester report with counts is recorded in this plan for Phase 28.
- No remote GitHub Actions status is claimed for this local-only increment after confirmed HEAD `5088e96`.

Acceptance criteria status:

- Interface doubles support argument matchers, throwing stubs, and answer callbacks without adding runtime dependencies.
- The original interface-only limitation remains explicit.
- No optional adapter or reporting behavior changes are implied by stronger doubles.

### Phase 29 — CLI-Only Opt-In Source/Spec Compilation (Completed Increment)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Implemented and locally verified at current HEAD `ddd7eb9` after the last user-/maintainer-confirmed remote CI point. No remote CI success is claimed here for this local increment.

**Relevant ADRs:** ADR 0022. ADR 0022 records the decision to add an explicit CLI-only compiler step through the current JDK compiler API while preserving default classpath execution and adapter boundaries.

Implemented scope:

1. Added run-only `javaspec run --compile` to compile source/spec `.java` files before bootstrap hooks and examples.
2. Added run-only `--compile-output <dir>`; selecting an output directory implies compilation, and the default output directory is `target/javaspec-classes`.
3. Compiles all `.java` files under the effective source root and effective spec root, de-duplicated by normalized path.
4. Uses the current JDK `javax.tools.JavaCompiler` from `ToolProvider`, not a forked `javac` and not a third-party dependency.
5. Runs compilation after discovery, profile enforcement, related-spec/support/source generation or updates, prompts, and `--generate` decisions, but before bootstrap hooks and example execution.
6. Skips compilation for no-spec runs and dry-run paths, preserving dry-run non-mutating behavior.
7. Orders the compiler classpath as compile output directory, explicit CLI classpath entries, then the current process `java.class.path`; successful execution uses a compile-output-first classloader.
8. Exits `64` when the compiler API is unavailable, exits `1` on compilation failure after printing `Compilation failed:`, skips bootstrap/example execution, and writes no JSON or JUnit XML-compatible reports on compilation failure.
9. Did not add default compilation, configuration keys, Maven/Gradle/JUnit Platform adapter changes, report schema changes, dependency resolution, incremental caches, source-level/release management, runtime dependency changes, optional adapter architecture changes, or publishing behavior.

Verification:

- Implementation-phase local verification passed for CLI opt-in compilation.
- `git diff --check` is the documentation validation for this Phase 30 docs-only synchronization, not a Phase 29 implementation test.
- No detailed tester report with counts is recorded in this plan for Phase 29.
- No remote GitHub Actions status is claimed for current local HEAD `ddd7eb9`.

Acceptance criteria status:

- Source-only CLI runs can opt into current-JDK compilation without changing default execution behavior.
- Build-tool and JUnit Platform adapters continue to rely on their host build/runtime classpaths.
- Compile failures stop before bootstrap/examples/reports, preserving deterministic failure boundaries.
- Java 8 compatibility and zero-runtime-dependency policies remain intact.

## Known-Limitations Resolution Program (Phases 30-36)

**Relevant ADRs:** ADR 0023. [ADR 0023](docs/adr/0023-course-correction-resolve-deferred-known-limitations.md) records the course correction: the maintainer requires that the documented README known limitations be resolved, replacing the previous intentional scope fences from Phases 25, 27, 28, and 29 and the ADR 0009 sealed-interface update deferral with a phased resolution program.

Program-wide gates, valid for every phase below:

- Java 8 source/target compatibility remains a release gate (ADR 0001).
- The core artifact keeps zero runtime dependencies (ADR 0002).
- The repository stays non-multi-module: root `mvn verify` remains core-only and standalone adapters stay outside the root Maven reactor.
- Public publishing, deploying, signing, and credential handling remain out of scope for every phase.
- README, CHANGELOG, and ARC42 synchronization for the whole program happens in the finalization documentation phase after implementation, not per phase.

### Excluded from the program

Two known limitations are explicitly excluded from Phases 30-36, as recorded in ADR 0023:

1. **Public publication remains postponed** — GPG signing keys, Central Portal credentials, Gradle Plugin Portal credentials, the final release version/tag, and final publish approval can only be supplied by the maintainer and are not resolvable by code.
2. **Concrete-class doubles — now resolved by maintainer decision** — originally deferred because a bytecode-manipulation library in the core would violate the zero-runtime-dependency policy (ADR 0002, ADR 0007); ADR 0023 listed the candidate options (status quo, `javax.tools` runtime subclass generation, standalone optional bytecode adapter artifact) without choosing one. The maintainer has since selected the standalone optional bytecode adapter artifact (option c), recorded in [ADR 0024](docs/adr/0024-standalone-optional-bytecode-doubles-adapter.md) and planned as Phase 37.

### Phase 30 — Bounded Iterable Matcher Checks (Completed)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Completed.

**Relevant ADRs:** ADR 0023.

Planned scope:

1. Fix `org.javaspec.matcher.Matchable` so `shouldBeEmpty()` and `shouldNotBeEmpty()` on a generic `Iterable` use `iterator().hasNext()` instead of fully consuming the iterable.
2. Fix `shouldHaveCount(int expected)` on a generic `Iterable` so it iterates at most `expected + 1` elements and fails fast with a "more than `<expected>` elements" style message when the bound is exceeded.
3. Keep behavior unchanged for `CharSequence`, `Collection`, `Map`, and arrays, which already have constant-time or size-based checks.

Acceptance criteria:

- Infinite iterables never hang count/empty checks.
- Existing matcher tests still pass.

### Phase 31 — Sealed-Interface Source Updates (Completed)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Completed.

**Relevant ADRs:** ADR 0023, ADR 0009. Phase 31 supersedes the deferral half of ADR 0009; the interface-style and annotation generation decisions in ADR 0009 remain in force.

Planned scope:

1. Implement source-preserving updates of existing sealed interfaces: insert generated method declarations into the sealed root and update nested permitted implementations with generated method bodies.
2. Retain the Java 8-compatible text-based generation heuristics; no full Java parser is introduced.

Acceptance criteria:

- Previously skipped sealed-interface updates are written source-preservingly.
- Java 8 text-based generation heuristics are retained.

### Phase 32 — Configuration-Driven Extension Activation and Adapter Formatter Controls (Completed)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Completed.

**Relevant ADRs:** ADR 0023, ADR 0018. Phase 32 relaxes the Phase 25 scope fence.

Planned scope:

1. Add configuration keys to activate discovered extensions/formatters from configuration.
2. Add Maven plugin formatter output controls.
3. Add JUnit Platform engine formatter configuration parameters.

Acceptance criteria:

- Formatter selection has parity across CLI, Maven, Gradle, and JUnit Platform.

### Phase 33 — ServiceLoader Bootstrap Hook Discovery (Completed)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Completed.

**Relevant ADRs:** ADR 0023, ADR 0020. Phase 33 relaxes the Phase 27 scope fence.

Planned scope:

1. Add optional `ServiceLoader` discovery of `org.javaspec.bootstrap.BootstrapHook` providers in addition to explicitly configured hooks.
2. Preserve explicit config hook order first; discovered hooks execute afterward in a deterministic order.
3. Add zero new dependencies.

Acceptance criteria:

- Explicitly configured hooks keep their order and run before discovered hooks.
- Discovered hook execution is deterministic.
- No new runtime dependencies are added.

### Phase 34 — Adapter and Programmatic Opt-In Compilation (Completed)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Completed.

**Relevant ADRs:** ADR 0023, ADR 0022. Phase 34 relaxes the Phase 29 CLI-only scope fence.

Planned scope:

1. Extend the Phase 29 `javax.tools` current-JDK opt-in compilation to `JavaspecInvocation`/`JavaspecLauncher` programmatic runs.
2. Extend the same opt-in compilation to the Maven and Gradle adapters as explicit opt-in settings.
3. Keep the Phase 29 boundaries: no dependency resolution, no forked `javac`, and no default compilation.

Acceptance criteria:

- Programmatic, Maven, and Gradle runs can explicitly opt into current-JDK compilation.
- Default behavior of all execution paths remains unchanged without the explicit opt-in.

### Phase 35 — Additive Report Enrichment (Completed)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Completed.

**Relevant ADRs:** ADR 0023, ADR 0010. Phase 35 keeps reporting additive and dependency-free.

Planned scope:

1. Enrich the dependency-free JUnit XML-compatible writer with `timestamp`, `hostname`, and `time` attributes plus a properties block.
2. Add additive JSON report fields while remaining schemaVersion 1-compatible.
3. Update `docs/schemas/run-report-v1.schema.json` additively and the golden reports under `docs/examples/reports/`.

Acceptance criteria:

- JSON reports remain schemaVersion 1-compatible with only additive fields.
- The JUnit XML-compatible writer remains dependency-free.
- Schema and golden-report documentation stay synchronized with the writers.

### Phase 36 — Deeper Target-Profile Enforcement (Completed)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Completed.

**Relevant ADRs:** ADR 0023, ADR 0019.

Planned scope:

1. Broaden the profile catalog coverage used by enforcement.
2. Broaden enforcement signature resolution.
3. Remain source/generation-scoped: enforcement is explicitly NOT an integrated compiler.

Acceptance criteria:

- More incompatible generation/update work is caught before writes.
- Enforcement remains conservative and explicit about its non-compiler-grade boundary.

### Phase 37 — Standalone Optional Bytecode Doubles Adapter (Completed)

**Owner:** Java implementation, tester/quality, and documenter agents as delegated by the parent workflow.

**Status:** Completed.

**Relevant ADRs:** ADR 0024, ADR 0007, ADR 0021, ADR 0002, ADR 0011. [ADR 0024](docs/adr/0024-standalone-optional-bytecode-doubles-adapter.md) records the maintainer decision (option c) that resolves exclusion (b) of ADR 0023.

Planned scope:

1. Core: add a zero-dependency doubles provider SPI in `org.javaspec.doubles` — interface-only contracts, `ServiceLoader`-discoverable, no behavior change when no provider is present.
2. New standalone artifact `javaspec-bytecode-doubles/` outside the root Maven reactor with ByteBuddy-based concrete-class double support for non-final classes, delegating matcher/stub/answer semantics to the existing core double contracts.
3. Extend `scripts/verify-all.sh` and the CI aggregate verification to the new artifact.
4. Examples and documentation are deferred to the finalization documentation phase.

Acceptance criteria:

- Root `mvn verify` stays green and the core runtime dependency tree is unchanged (enforcer passes).
- The new artifact builds and tests standalone after a core snapshot install.
- Interface doubles behavior is unchanged when the adapter is absent.
- Final-class, static, and constructor mocking are explicitly rejected with clear diagnostics.
