# 10. Quality Requirements

## 10.1 Quality Tree

| Quality attribute | Scenario | Evidence / current status |
|---|---|---|
| Java 8 compatibility | The runtime artifact executes on Java 8 and does not link directly to Java 9+ APIs. | Phase 12 Java 8 Distrobox `mvn clean` and `mvn verify` passed; compiler source/target is 1.8; bytecode and constant-pool audits are summarized in [Test and Quality Report](../test-report.md). |
| Zero runtime dependencies | Runtime dependency scope contains only the javaspec artifact for core; optional adapters do not leak dependencies into core. | Phase 12 Java 25 runtime dependency audit passed with only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`; Phase 15 root runtime audit passed with only `org.javaspec:javaspec`, and Maven plugin runtime audit passed with the plugin plus compile-scope core only; Phase 16 root runtime audit passed with only `org.javaspec:javaspec`, and Gradle plugin runtimeClasspath contained only `org.javaspec:javaspec:0.1.0-SNAPSHOT`; Phase 17 root runtime audit passed with only `org.javaspec:javaspec`, and JUnit Platform engine runtime dependencies stayed isolated to the optional engine artifact; Phase 18 root runtime audit passed with only `org.javaspec:javaspec` and adapter runtime summaries remained isolated; Phase 19 aggregate verification repeated the root and adapter runtime audits with the same isolation; Phase 20 root and adapter runtime audits preserved the same isolation. |
| Deterministic CLI/build-tool/engine behavior | Commands, options, prompts, output modes, explicit classpath handling, stable ids/source metadata, report writing, Maven/Gradle plugin adapter behavior, JUnit Platform engine mapping, aggregate verification, release-readiness checks, and exit codes/events are stable for local and CI usage. | CLI and optional Maven/Gradle/JUnit Platform adapter behavior are documented in the user manual; Phase 12 ran 364 tests per JDK across the matrix; Phase 15 standalone Maven plugin verification passed with 12 plugin tests; Phase 16 standalone Gradle plugin verification passed with 11 plugin tests using Gradle 8.8; Phase 17 standalone JUnit Platform engine verification passed with 12 tests; Phase 18 core and adapter verification passed with stable id/source/report assertions; Phase 19 `scripts/verify-all.sh` full aggregate verification passed locally; Phase 20 version alignment, release-artifact packaging, and full aggregate verification passed locally. |
| Safe generation | Production source generation/update is gated by prompts, `--generate`, or `--dry-run` planning. | ADR 0003, ADR 0004, ADR 0008, and the user manual document generation ownership and policies. |
| Accurate implemented-feature documentation | Docs do not overstate unsupported behavior. | Limitations are recorded in the user manual, README, ARC42 section 11, and ADR consequences. |
| Extensibility without dependency cost | Formatter, extension, reporting, invocation contracts, optional adapters, release verification assets, and release-readiness scaffolding are public boundaries without adding libraries to the core runtime. | ADR 0010 documents programmatic-only extension behavior and lack of external CLI loading; ADR 0011 covers no-JUnit invocation and optional adapters; ADR 0012 covers aggregate release/CI verification without mandatory Maven multi-module conversion; ADR 0013 covers release-readiness scaffolding with resolved MIT license/maintainer metadata and postponed publishing/signing/portal decisions; Phase 15 verifies the standalone Maven plugin boundary, Phase 16 verifies the standalone Gradle plugin boundary, Phase 17 verifies the standalone JUnit Platform engine boundary, Phase 19 verifies the aggregate script boundary, and Phase 20 verifies the release-readiness boundary. |
| LTS awareness | Java 8, 11, 17, 21, and 25 profiles are modeled and verified where runtime probing is relevant. | Phase 12 matrix passed; Java 25 Gatherer reflection probe passed. |

## 10.2 Quality Scenarios

### Compatibility

- When running on Java 8, javaspec must start and execute the implemented CLI without `NoClassDefFoundError` caused by newer JDK APIs.
- When profile metadata references Java 11+ APIs, those references must be strings or reflected conditionally.
- When generated records or sealed types are emitted, the generator may produce source text that requires a newer JDK, but the javaspec binary itself must remain Java 8-compatible.

### Dependency Integrity

- `mvn dependency:tree -Dscope=runtime` must show no third-party runtime dependencies.
- Features that normally need libraries, such as configuration parsing, JSON/XML writing, doubles, invocation, or CLI parsing, must stay internal/JDK-based or move to optional integrations outside the core runtime.
- Optional build-tool adapters must not alter the core runtime dependency audit; their own runtime dependency trees must be documented and verified separately.

### Generation Safety

- `describe` must not write production source.
- `run` must prompt before production generation/update unless `--generate` is provided.
- `run --dry-run` must not write files and must not prompt.
- Constructor deletion must require explicit `--constructor-policy delete` or an equivalent explicit config/CLI selection.
- Existing sealed-interface updates must remain skipped until safe source-preserving nested implementation updates are implemented.

### Execution Determinism

- Discovery order, filter behavior, example selection, and result status mapping must remain deterministic.
- `AssertionError` is FAILED; non-assertion throwables are BROKEN; non-loadable specs are SKIPPED.
- `--stop-on-failure` stops after the first FAILED or BROKEN executable example.
- Skipped-only runs remain successful; failed or broken executable examples exit `1`.

### Reporting and CI

- Built-in `progress` and `pretty` formatters must remain deterministic.
- JSON report schemaVersion 1 must remain stable unless a future schema decision is made; additive stable id/source fields must preserve existing fields.
- JUnit XML-compatible reports must be generated without JUnit or XML/reporting runtime dependencies, with testcase file/line attributes only when source data is available.
- JSON and JUnit XML-compatible reports can be requested together for no-spec and executed run paths.
- Dry-run pending generation/update exits before execution and must not write reports.
- Report write failures must include the report path and exit `70`.
- Programmatic invocation must not call `System.exit` and must return structured results with deterministic exit-code mapping.
- Test and quality claims must cite produced tester/quality reports rather than invented results.
- Optional JUnit Platform engine execution must remain an adapter over canonical discovery and `JavaspecLauncher`, avoid `System.exit`, and map passed/failed/broken/skipped javaspec states to JUnit Platform events without requiring spec authoring changes.
- Aggregate release verification must keep root `mvn verify` core-only, verify standalone adapters explicitly after installing the current core snapshot, and fail clearly when a required local Gradle executable cannot be resolved.
- CI documentation must distinguish configured/local-validated workflow YAML from actual remote GitHub Actions results and must state remote status by phase.
- Release-readiness documentation must keep public publication postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved; local source/javadoc packaging must not be described as signing, staging, deployment, or publication.

## 10.3 Phase 12 Verification Summary

Phase 12 is the current authoritative cross-JDK compatibility evidence:

| JDK | Runtime | Result |
|---|---|---|
| Java 8 | `1.8.0_492` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 11 | `11.0.31` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 17 | `17.0.19` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 21 | `21.0.11 LTS` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 25 | `25.0.3 LTS` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |

Additional Phase 12 evidence:

- Distrobox `1.8.2.5` with Podman `5.8.2` was used.
- Maven `3.9.16` ran in every matrix container.
- JDK 17+ emitted only expected `-source 8` / `-target 1.8` warnings.
- Java 25 Gatherer runtime reflection passed for `java.util.stream.Gatherer`, required nested Gatherer types, and `java.util.stream.Gatherers`.
- Runtime dependency audit passed in the Java 25 container.

See [Test and Quality Report](../test-report.md) for the consolidated report.

## 10.4 Phase 14 Verification Summary

Phase 14 is the current authoritative verification for no-JUnit invocation, explicit classpath runs, and JUnit XML-compatible reporting:

| Command | Result |
|---|---|
| `mvn -q -Dtest=JavaspecLauncherTest,JUnitXmlReportWriterTest,MainPhase14CliTest test` | PASS — 18 tests |
| `mvn verify` | PASS — 382 tests, 0 failures, 0 errors, 0 skipped |
| `mvn dependency:tree -Dscope=runtime` | PASS — runtime tree contains only the root artifact |

Verified Phase 14 quality points:

- `JavaspecLauncher` returns structured results and does not terminate the JVM with `System.exit`.
- Exit-code mapping returns `0` for passing, skipped-only, and no-spec invocation paths, and `1` for failed or broken execution.
- `--classpath` and `--classpath-file` use explicit compiled-class entries for type existence checks and spec execution, with UTF-8 classpath files and comment/blank-line handling.
- `--junit-xml` and `--junit-xml-file` write no-spec and normal run reports; failing/broken runs write reports before exit `1`; dry-run pending generation/update writes no reports; report I/O failures exit `70` with path diagnostics.
- JSON `--report` / `--report-file` behavior remains compatible, and JSON plus JUnit XML-compatible reports can be requested together.

See [Test and Quality Report](../test-report.md) for details.

## 10.5 Phase 15 Verification Summary

Phase 15 is the current authoritative verification for the standalone optional Maven plugin integration:

| Command | Result |
|---|---|
| `mvn -q verify` | PASS — 382 core tests |
| `mvn -q -DskipTests install` | PASS — current core installed for standalone plugin verification |
| `mvn -q -f javaspec-maven-plugin/pom.xml -Dtest=JavaspecRunMojoTest test` | PASS — 12 plugin tests |
| `mvn -q -f javaspec-maven-plugin/pom.xml verify` | PASS — 12 plugin tests |
| `mvn dependency:tree -Dscope=runtime` | PASS — root runtime tree contained only `org.javaspec:javaspec` |
| `mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime` | PASS — plugin runtime tree contained the plugin plus compile-scope core `org.javaspec:javaspec` only |

Verified Phase 15 quality points:

- The Maven plugin is a standalone optional artifact at `javaspec-maven-plugin/` and is intentionally not registered as a root module.
- Root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact.
- The plugin packages as `maven-plugin` with Java source/target `1.8`, Maven API baseline `3.6.3`, Maven APIs and plugin annotations in `provided` scope, JUnit in `test` scope, and compile-scope core dependency.
- `JavaspecRunMojo` uses Maven test dependency resolution and Maven test classpath, supports the documented filters/options/reports, and delegates to `JavaspecLauncher` without `System.exit`.
- Plugin tests cover JUnit XML report I/O failure handling, plugin POM dependency scopes, and the canonical launcher delegation guard.
- Projects under test do not need JUnit.

See [Test and Quality Report](../test-report.md) for details.

## 10.6 Phase 16 Verification Summary

Phase 16 is the current authoritative verification for the standalone optional Gradle plugin integration:

| Command | Result |
|---|---|
| `mvn -q -DskipTests install` | PASS — current core installed for standalone plugin verification |
| `mvn -q verify` | PASS |
| `mvn dependency:tree -Dscope=runtime` | PASS — root runtime tree contained only `org.javaspec:javaspec` |
| `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin test` | PASS — 11 plugin tests |
| `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin build` | PASS |
| `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath` | PASS — runtimeClasspath contained only `org.javaspec:javaspec:0.1.0-SNAPSHOT` |
| `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration testRuntimeClasspath` | PASS — testRuntimeClasspath contained javaspec, JUnit, and Hamcrest only |
| Cached Gradle 7.4.2 command on installed Java 21 | BLOCKED — `Unsupported class file major version 65`; environment/tooling compatibility blocker for that cached executable, not a javaspec feature failure |

Verified Phase 16 quality points:

- The Gradle plugin is a standalone optional artifact at `javaspec-gradle-plugin/` and is intentionally not registered as a root Maven module.
- Root Maven verification continues to build and audit only the zero-runtime-dependency core artifact.
- The plugin uses `java-gradle-plugin`, Java source/target `1.8`, plugin id `org.javaspec`, implementation class `org.javaspec.gradle.JavaspecPlugin`, and a core dependency on `org.javaspec:javaspec:0.1.0-SNAPSHOT`.
- `JavaspecPlugin` registers extension `javaspec` and task `javaspecRun`; Java plugin/source-set defaults use the `test` source set runtime classpath and depend on `testClasses`.
- `JavaspecRunTask` supports the documented filters/options/reports, logs through Gradle, manages the run classloader, and delegates to `JavaspecLauncher` without `System.exit`.
- Projects under test do not need JUnit; JUnit is only a plugin test dependency.

See [Test and Quality Report](../test-report.md) for details.

## 10.7 Phase 17 Verification Summary

Phase 17 is the current authoritative verification for the standalone optional JUnit Platform engine integration:

| Command | Result |
|---|---|
| `mvn -q -DskipTests install` | PASS — current core installed for standalone engine verification |
| `mvn -q verify` | PASS — root Surefire: 382 tests, 0 failures, 0 errors, 0 skipped |
| `mvn -q -f javaspec-junit-platform-engine/pom.xml -Dtest=JavaspecTestEnginePhase17Test test` | PASS — 12 tests, 0 failures, 0 errors, 0 skipped |
| `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` | PASS — 12 tests, 0 failures, 0 errors, 0 skipped |
| `mvn dependency:tree -Dscope=runtime` | PASS — root runtime tree contained only `org.javaspec:javaspec` |
| `mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime` | PASS — engine runtime dependencies were core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`; no runtime `junit-jupiter`, `junit-platform-launcher`, or `junit-platform-testkit` |

Verified Phase 17 quality points:

- The JUnit Platform engine is a standalone optional artifact at `javaspec-junit-platform-engine/` and is intentionally not registered as a root Maven module.
- Root Maven verification continues to build and audit only the zero-runtime-dependency core artifact.
- The engine uses Java source/target `1.8` and JUnit Platform `1.10.2`, avoiding JUnit Platform 6/JUnit 6.
- `JavaspecTestEngine` is discovered through ServiceLoader with engine id `javaspec`.
- Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`, configuration parameters, and class/package/method/unique-id selectors as filters over canonical discovery results.
- Execution delegates to canonical no-JUnit `JavaspecLauncher`, avoids `System.exit`, maps javaspec result states to JUnit Platform listener events, and does not require spec authoring style changes.
- Projects that do not opt into the engine keep no-JUnit CLI/programmatic/Maven/Gradle execution paths.

See [Test and Quality Report](../test-report.md) for details.

## 10.8 Phase 18 Verification Summary

Phase 18 is the current authoritative verification for stable identifier, source-location, and report-consistency polish:

| Command | Result |
|---|---|
| `mvn -q -Dtest=SpecDiscoveryNamingTest,SpecRunnerTest,RunReportWriterTest,JUnitXmlReportWriterTest,MainPhase11ReportCliTest,MainPhase14CliTest test` | PASS |
| `mvn -q test` | PASS — 386 tests, 0 failures, 0 errors, 0 skipped |
| `mvn -q verify` | PASS — 386 tests, 0 failures, 0 errors, 0 skipped |
| `mvn dependency:tree -Dscope=runtime` | PASS — root runtime tree contained only `org.javaspec:javaspec` |
| `mvn -q install` | PASS |
| `mvn -q -f javaspec-maven-plugin/pom.xml verify` | PASS — 12 tests |
| `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` | PASS — 12 tests |
| `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build` | PASS — 11 tests |
| `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath` | PASS |

Verified Phase 18 quality points:

- Stable ids are exposed by `ExampleResult`, `SpecResult`, and `DiscoveredSpec`.
- `SpecExample`, `ExampleResult`, and `SpecResult` carry source metadata where available.
- JSON reports add stable id/source fields additively while preserving existing fields.
- JUnit XML-compatible reports add testcase `file`/`line` attributes only when source data is available and remain dependency-free.
- Optional JUnit Platform engine descriptor reporting is aligned to stable ids while retaining stable unique-id shape and MethodSource behavior.
- Core runtime has no external runtime dependencies; Maven and Gradle plugin runtimes depend on core `org.javaspec:javaspec`; JUnit Platform engine runtime dependencies remain isolated to the optional engine artifact.

See [Test and Quality Report](../test-report.md) for details.

## 10.9 Phase 19 Verification Summary

Phase 19 remains the authoritative verification for post-roadmap release/CI hardening:

| Command / check | Result |
|---|---|
| `bash -n scripts/verify-all.sh` | PASS — script syntax valid and executable bit present |
| Local PyYAML parse of `.github/workflows/ci.yml` | PASS — valid YAML mapping with jobs `core` and `full-verification`; actionlint/yamllint/yq unavailable |
| `git diff --check` / `git diff --cached --check` / temp-index whitespace check including untracked `.github/` and `scripts/` | PASS |
| `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` | PASS — Gradle 8.8 |
| Root `mvn -q verify` executed by script | PASS — 386 tests, 0 failures, 0 errors, 0 skipped |
| Root `mvn dependency:tree -Dscope=runtime` executed by script | PASS — only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` |
| Root `mvn -q -DskipTests install` executed by script | PASS |
| Maven plugin `verify` and runtime audit executed by script | PASS — 12 tests; runtime summary plugin plus core |
| JUnit Platform engine `verify` and runtime audit executed by script | PASS — 12 tests; runtime summary core plus isolated JUnit Platform engine dependencies |
| Gradle plugin `clean test build` and `runtimeClasspath` audit executed by script | PASS — 11 tests; runtimeClasspath only `org.javaspec:javaspec:0.1.0-SNAPSHOT` |

Verified Phase 19 quality points:

- The repository was not converted to Maven multi-module.
- Root `mvn verify` remains a core-only quality gate.
- `scripts/verify-all.sh` provides an explicit aggregate verification path for standalone adapters after refreshing the local core snapshot.
- `.github/workflows/ci.yml` defines a Temurin Java 8/11/17/21/25 core matrix and Java 21 full-verification job with Gradle 8.8 setup.
- No publishing, signing, release deployment, secrets, or Phase 19 implementation-time remote CI result claim was added; remote success was later user-/maintainer-confirmed for HEAD `4d30e63` on `develop`.

See [Test and Quality Report](../test-report.md) for details.

## 10.10 Phase 20 Verification Summary

Phase 20 is the current authoritative verification for release-readiness scaffolding:

| Command / check | Result |
|---|---|
| `bash -n scripts/check-version-alignment.sh scripts/verify-all.sh` and individual script syntax checks | PASS |
| Script executable bits | PASS — both scripts executable (`-rwxr-xr-x`) |
| `bash scripts/check-version-alignment.sh` | PASS — all checked versions aligned at `0.1.0-SNAPSHOT` |
| `git diff --check`, `git diff --cached --check`, and untracked whitespace checks | PASS |
| Effective POM generation for root, Maven plugin, and JUnit engine | PASS |
| `LICENSE` identity against `origin/main:LICENSE` | PASS — blob `b990d5492f3ef404ffc145890b83e51914351bb5` |
| Maven MIT license and maintainer metadata checks for root, Maven plugin, and JUnit engine | PASS |
| Gradle generated POM MIT license and maintainer metadata checks for `pluginMaven` and `javaspecPluginMarkerMaven` | PASS |
| `mvn -q verify` | PASS — 386 tests, 0 failures, 0 errors, 0 skipped |
| `mvn dependency:tree -Dscope=runtime` | PASS — no root runtime dependencies beyond `org.javaspec:javaspec` |
| `mvn -q -Prelease-artifacts -DskipTests package` | PASS — root main/sources/javadoc jars non-empty |
| `mvn -q -DskipTests install` | PASS |
| Maven plugin `-Prelease-artifacts -DskipTests package` | PASS — main/sources/javadoc jars non-empty |
| JUnit engine `-Prelease-artifacts -DskipTests package` | PASS — main/sources/javadoc jars non-empty |
| Standalone Maven plugin `mvn -q verify` | PASS — 12 tests |
| Standalone JUnit engine `mvn -q verify` | PASS — 12 tests |
| Gradle plugin generated POMs and `clean test build` | PASS — generated POMs passed metadata checks; 11 tests and main/sources/javadoc jars non-empty; non-blocking Java 8 source/target obsolete warnings on JDK 21 |
| Gradle runtime dependency audit | PASS — only `org.javaspec:javaspec:0.1.0-SNAPSHOT` |
| `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` | PASS — covered version alignment, core verify, root audit, local install, Maven plugin verify/audit, JUnit engine verify/audit, and Gradle plugin build/audit |

Verified Phase 20 quality points:

- Tester modified no files and ran no publish/deploy/signing commands.
- Version alignment is enforced before aggregate verification.
- Local main/sources/javadoc artifact readiness is verified for root, Maven plugin, JUnit engine, and Gradle plugin.
- Safe URL/SCM/GitHub Issues metadata is present with confirmed MIT license and maintainer/developer metadata.
- No runtime dependencies, secrets, Maven multi-module conversion, actual publication, or remote Phase 20 CI result claim was added.
- Publication remains intentionally postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved.

See [Test and Quality Report](../test-report.md) for details.

## 10.11 Quality Gates for Future Work

Future implementation phases should preserve these gates:

1. `mvn verify` passes on the supported JDK matrix when tester resources are available.
2. Runtime dependency audit remains clean.
3. Java 8 bytecode/source compatibility remains enforced.
4. New architectural decisions are recorded as ADRs before implementation where they change core boundaries.
5. User manual, README, ARC42, ADR references, and test/quality reports remain synchronized with implemented behavior.
6. Standalone optional adapters remain covered by `scripts/verify-all.sh` or an explicitly documented equivalent aggregate verification path unless a future ADR changes the build/release architecture.
7. Version alignment remains checked before release-candidate packaging.
8. Public publication/signing automation is not added until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved and documented in an ADR; the confirmed MIT license and maintainer metadata must remain consistent.
