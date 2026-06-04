# Test and Quality Report

## Phase 20 verification update

Date: 2026-06-04

This report records the completed Phase 20 verification results provided by the Java tester for release-readiness scaffolding after license and maintainer metadata were confirmed. Phase 20 added version-alignment checking, changelog/releasing documentation, MIT license and confirmed maintainer/developer metadata, safe publication metadata, and local source/javadoc artifact readiness checks only. It did not run or configure publish/deploy/signing/portal credential commands, add secrets, add runtime dependencies, configure Central Portal publication, configure Gradle Plugin Portal publication/credentials, choose a final release version/tag, approve final publishing, or convert the repository to Maven multi-module. Phase 19 release/CI hardening evidence and earlier evidence remain below.

## Phase 20 executive summary

| Area | Result | Notes |
|---|---|---|
| Tester file modifications | PASS | Tester reported no file modifications. |
| Publish/deploy/signing commands | PASS | Tester reported no publish/deploy/signing commands were run. |
| License file | PASS | `LICENSE` is identical to `origin/main:LICENSE`, blob `b990d5492f3ef404ffc145890b83e51914351bb5`. |
| Script syntax/executable bits | PASS | `bash -n scripts/check-version-alignment.sh`, `bash -n scripts/verify-all.sh`, and combined/individual syntax checks passed; both scripts are executable (`-rwxr-xr-x`). |
| Version alignment | PASS | `bash scripts/check-version-alignment.sh` passed; root Maven, Maven plugin, JUnit engine, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` are aligned at `0.1.0-SNAPSHOT`. |
| Whitespace checks | PASS | `git diff --check`, `git diff --cached --check`, and untracked whitespace checks passed. |
| Effective POM generation | PASS | Effective POM generation passed for root, Maven plugin, and JUnit engine. |
| Maven POM metadata | PASS | Root, Maven plugin, and JUnit engine POM metadata contain MIT License, URL `https://opensource.org/licenses/MIT`, distribution `repo`, Mario Giustiniani email, and maintainer role. |
| Gradle generated POM metadata | PASS | `pluginMaven` and `javaspecPluginMarkerMaven` generated POMs include MIT license and maintainer metadata. |
| Root core verification | PASS | `mvn -q verify` passed; 386 tests, 0 failures, 0 errors, 0 skipped. |
| Root runtime dependency audit | PASS | `mvn dependency:tree -Dscope=runtime` passed; root runtime has no dependencies beyond `org.javaspec:javaspec`. |
| Root release artifact check | PASS | `mvn -q -Prelease-artifacts -DskipTests package` passed; root main, sources, and javadoc jars were non-empty. |
| Core install for standalone adapters | PASS | `mvn -q -DskipTests install` passed. |
| Maven plugin release artifact check | PASS | `mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package` passed; main, sources, and javadoc jars were non-empty. |
| JUnit Platform engine release artifact check | PASS | `mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests package` passed; main, sources, and javadoc jars were non-empty. |
| Standalone Maven plugin verification | PASS | `mvn -q verify` for the standalone Maven plugin passed with 12 tests. |
| Standalone JUnit engine verification | PASS | `mvn -q verify` for the standalone JUnit engine passed with 12 tests. |
| Gradle plugin POM generation/build/artifacts | PASS | Gradle publication POM generation passed; `/tmp/gradle-8.8/bin/gradle` was available; Gradle plugin `clean test build` passed with 11 tests, 0 failures/errors/skips, and non-empty main/sources/javadoc jars. Non-blocking Java 8 source/target obsolete warnings occurred on JDK 21. |
| Gradle runtime dependency audit | PASS | Gradle runtime dependencies contained only `org.javaspec:javaspec:0.1.0-SNAPSHOT`. |
| Full aggregate verification | PASS | `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed, covering version alignment, core verify, root audit, local install, Maven plugin verify/audit, JUnit engine verify/audit, and Gradle plugin build/audit. |
| Remote CI execution | NOT CLAIMED | Phase 20 has local verification only in this report. Phase 19 remote GitHub Actions success remains user-/maintainer-confirmed for HEAD `4d30e63` on `develop`. |
| Phase 20 metadata verification blockers | PASS | None reported. |
| Publication/deploy/signing status | POSTPONED BY DESIGN | MIT license and maintainer metadata are resolved; GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval remain unresolved and unimplemented. |

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
- `scripts/check-version-alignment.sh` verifies root Maven project version, standalone Maven plugin project version, standalone JUnit Platform engine project version, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` alignment.
- `scripts/verify-all.sh` now runs version alignment before aggregate core and adapter verification.
- `CHANGELOG.md` and `RELEASING.md` are documentation/checklist artifacts only.
- The MIT `LICENSE` was copied exactly from `origin/main`; tester verification confirmed identical blob `b990d5492f3ef404ffc145890b83e51914351bb5`.
- Maven POM metadata for root, Maven plugin, and JUnit engine includes MIT License, URL `https://opensource.org/licenses/MIT`, distribution `repo`, Mario Giustiniani email, and maintainer role.
- Gradle generated POMs `pluginMaven` and `javaspecPluginMarkerMaven` include MIT license and maintainer metadata.
- Maven `release-artifacts` profiles create local sources and javadocs for the root, Maven plugin, and JUnit Platform engine builds only.
- The Gradle plugin build is ready to produce source and javadoc jars.
- Safe URL, SCM, and GitHub Issues metadata is present in Maven/Gradle metadata.
- Signing configuration, deploy/publish configuration, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval were intentionally not added.
- No actual publishing, deployment, signing, secret usage, Maven multi-module conversion, runtime dependency additions, or remote Phase 20 CI result claim is part of this increment.

## Phase 20 dependency summary

| Artifact / area | Runtime dependency summary |
|---|---|
| Core runtime | No external runtime dependencies; root runtime tree contains only `org.javaspec:javaspec`. |
| Maven plugin runtime | `org.javaspec:javaspec:0.1.0-SNAPSHOT`. |
| JUnit Platform engine runtime | `org.javaspec:javaspec:0.1.0-SNAPSHOT`, `junit-platform-engine:1.10.2`, `opentest4j`, `junit-platform-commons`, `apiguardian-api`. |
| Gradle plugin runtime | `org.javaspec:javaspec:0.1.0-SNAPSHOT`. |

## Phase 19 verification update

Date: 2026-06-04

This report records the completed Phase 19 verification results provided by the Java tester for the post-roadmap release/CI hardening increment. Phase 19 added local aggregate verification and a GitHub Actions workflow without converting the repository to Maven multi-module. Phase 18 stable identifier/source-location/report evidence, Phase 17 JUnit Platform engine evidence, Phase 16 Gradle plugin evidence, Phase 15 Maven plugin evidence, Phase 14 no-JUnit invocation evidence, and Phase 12 Distrobox multi-JDK evidence remain below.

## Phase 19 executive summary

| Area | Result | Notes |
|---|---|---|
| Tester file modifications | PASS | Tester reported no file modifications. |
| Aggregate script syntax/executable bit | PASS | `bash -n scripts/verify-all.sh` passed; script is executable. |
| GitHub Actions YAML local parse | PASS | actionlint/yamllint/yq were unavailable; PyYAML parsed `.github/workflows/ci.yml` as a valid YAML mapping with top-level keys `name`, `on`, and `jobs`, including jobs `core` and `full-verification`. |
| Whitespace checks | PASS | `git diff --check`, `git diff --cached --check`, and a temp-index whitespace check including untracked `.github/` and `scripts/` passed. |
| Full aggregate verification | PASS | `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed using Gradle 8.8. |
| Root core verification | PASS | Script ran `mvn -q verify`; root tests 386, 0 failures, 0 errors, 0 skipped. |
| Root runtime dependency audit | PASS | Runtime tree contained only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`. |
| Core install for standalone adapters | PASS | Script ran `mvn -q -DskipTests install`. |
| Maven plugin verification/audit | PASS | Plugin `verify` passed with 12 tests; runtime audit showed `org.javaspec:javaspec-maven-plugin` plus `org.javaspec:javaspec`. |
| JUnit Platform engine verification/audit | PASS | Engine `verify` passed with 12 tests; runtime audit showed core plus `junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`. |
| Gradle plugin verification/audit | PASS | Gradle plugin `clean test build` passed with 11 tests; runtimeClasspath audit showed `org.javaspec:javaspec:0.1.0-SNAPSHOT`; non-blocking Java 8 source/target obsolete warnings occurred on JDK 21. |
| Remote CI execution | PASS (user-/maintainer-confirmed) | The user/maintainer confirmed GitHub Actions was all green for HEAD `4d30e63` on `develop` after the workflow was pushed; no run IDs, URLs, durations, or logs were independently queried here. |
| Blockers | PASS | None reported. |

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
- Repository-root `mvn verify` remains a core-only verification and runtime-audit path for the zero-runtime-dependency core artifact.
- `javaspec-maven-plugin/`, `javaspec-gradle-plugin/`, and `javaspec-junit-platform-engine/` remain standalone optional artifacts outside the root Maven reactor.
- `scripts/verify-all.sh` resolves the repository root from the script path and verifies the core and standalone adapters after refreshing the local core snapshot.
- The script supports `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, and explicit `JAVASPEC_SKIP_GRADLE=1`.
- Without `JAVASPEC_GRADLE_BIN`, Gradle resolution order is repository `./gradlew`, `/tmp/gradle-8.8/bin/gradle`, then `gradle` on `PATH`; if none are found and Gradle is not explicitly skipped, the script fails clearly.
- `.github/workflows/ci.yml` triggers on `push`, `pull_request`, and `workflow_dispatch`.
- The workflow core job runs a Temurin Java 8, 11, 17, 21, and 25 matrix with Maven cache, root `mvn -q verify`, and root runtime dependency audit.
- The workflow full-verification job runs on Java 21 with Maven cache and Gradle setup using Gradle 8.8, then runs `scripts/verify-all.sh` with `JAVASPEC_GRADLE_BIN=gradle`.
- The workflow adds no publishing, signing, secrets, or deployment behavior.
- Remote GitHub Actions success is user-/maintainer-confirmed for HEAD `4d30e63` on `develop` after the workflow was pushed; this report does not include independently queried GitHub run IDs, URLs, durations, or logs.

## Phase 19 dependency summary

| Artifact / area | Runtime dependency summary |
|---|---|
| Core runtime | No external runtime dependencies; root runtime tree contains only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`. |
| Maven plugin runtime | `org.javaspec:javaspec-maven-plugin` plus `org.javaspec:javaspec`. |
| JUnit Platform engine runtime | `org.javaspec:javaspec`, `junit-platform-engine`, `opentest4j`, `junit-platform-commons`, `apiguardian-api`. |
| Gradle plugin runtime | `org.javaspec:javaspec:0.1.0-SNAPSHOT`. |

## Phase 18 verification update

Date: 2026-06-04

This report records the completed Phase 18 verification results provided by the Java tester for the stable identifier, source-location, and report-consistency polish increment. Phase 17 JUnit Platform engine evidence, Phase 16 Gradle plugin evidence, Phase 15 Maven plugin evidence, Phase 14 no-JUnit invocation evidence, and Phase 12 Distrobox multi-JDK evidence remain below.

## Phase 18 executive summary

| Area | Result | Notes |
|---|---|---|
| Targeted changed core tests | PASS | `mvn -q -Dtest=SpecDiscoveryNamingTest,SpecRunnerTest,RunReportWriterTest,JUnitXmlReportWriterTest,MainPhase11ReportCliTest,MainPhase14CliTest test` passed. |
| Core Maven tests | PASS | `mvn -q test` passed with 386 tests, 0 failures, 0 errors, and 0 skipped. |
| Core Maven verification | PASS | `mvn -q verify` passed with 386 tests, 0 failures, 0 errors, and 0 skipped. |
| Core runtime dependency audit | PASS | `mvn dependency:tree -Dscope=runtime` passed; root runtime tree contained only `org.javaspec:javaspec`. |
| Core install for standalone adapters | PASS | `mvn -q install` passed and refreshed the local core snapshot. |
| Maven plugin verification | PASS | `mvn -q -f javaspec-maven-plugin/pom.xml verify` passed with 12 tests. |
| JUnit Platform engine verification | PASS | `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` passed with 12 tests. |
| Gradle plugin verification | PASS | `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build` passed with 11 tests. |
| Gradle plugin runtimeClasspath audit | PASS | `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath` passed. |
| Blockers | PASS | None reported. |

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

- Phase 18 implemented an incremental IDE/CI polish focused on stable identifiers, source locations, and report consistency.
- Stable id aliases were added to `ExampleResult`, `SpecResult`, and `DiscoveredSpec`.
- `ExampleResult.id()` / `stableId()` / `getId()` / `getStableId()` use `<specQualifiedName>#<methodName>` semantics matching existing `fullName()`.
- `SpecResult.id()` / `stableId()` / `getId()` / `getStableId()` are derived from the spec qualified name.
- `DiscoveredSpec.id()` / `stableId()` / `getId()` / `getStableId()` are derived from the spec qualified name.
- `SpecExample` carries a 1-based `sourceLine`.
- `SpecDiscovery.extractExamples` computes the method declaration line for discovered example methods.
- `ExampleResult` carries `sourceFilePath` and `sourceLine` when created from discovered specs/examples.
- `SpecResult` carries the spec source file path.
- JSON `RunReportWriter` includes spec `id`, `stableId`, and `sourceFile` plus example `id`, `stableId`, `fullName`, and `source { file, line }` while preserving existing fields.
- JUnit XML `JUnitXmlReportWriter` includes `file` and `line` attributes on `<testcase>` when source data is available while preserving dependency-free JUnit XML-compatible output.
- The optional JUnit Platform engine retained stable unique-id shape and MethodSource behavior, with descriptor reporting aligned to stable ids.
- Changed tests asserted stable ids/source metadata in discovery, runner results, JSON reports, JUnit XML reports, CLI reports, Maven plugin reports, and Gradle plugin reports.
- Changed test files were:
  - `src/test/java/org/javaspec/discovery/SpecDiscoveryNamingTest.java`
  - `src/test/java/org/javaspec/runner/SpecRunnerTest.java`
  - `src/test/java/org/javaspec/reporting/RunReportWriterTest.java`
  - `src/test/java/org/javaspec/reporting/JUnitXmlReportWriterTest.java`
  - `src/test/java/org/javaspec/cli/MainPhase11ReportCliTest.java`
  - `src/test/java/org/javaspec/cli/MainPhase14CliTest.java`
  - `javaspec-maven-plugin/src/test/java/org/javaspec/maven/JavaspecRunMojoTest.java`
  - `javaspec-gradle-plugin/src/test/java/org/javaspec/gradle/JavaspecGradlePluginTest.java`
- Unrelated Phase 18 items were not implemented: external extension loading, pending examples, deep profile enforcement, or broad new classpath diagnostics.

## Phase 18 dependency summary

| Artifact / area | Runtime dependency summary |
|---|---|
| Core runtime | No external runtime dependencies; root runtime tree contains only `org.javaspec:javaspec`. |
| Maven plugin runtime | `org.javaspec:javaspec`. |
| JUnit Platform engine runtime | `org.javaspec:javaspec`, `junit-platform-engine`, `opentest4j`, `junit-platform-commons`, `apiguardian-api`. |
| Gradle plugin runtime | `org.javaspec:javaspec`. |

## Phase 17 verification update

Date: 2026-06-04

This report records the completed Phase 17 verification results provided by the Java tester for the standalone optional JUnit Platform engine integration. Phase 16 Gradle plugin evidence, Phase 15 Maven plugin evidence, Phase 14 no-JUnit invocation evidence, and Phase 12 Distrobox multi-JDK evidence remain below.

## Phase 17 executive summary

| Area | Result | Notes |
|---|---|---|
| Core install for standalone engine verification | PASS | `mvn -q -DskipTests install` passed before engine verification. |
| Core Maven verification | PASS | `mvn -q verify` passed; root Surefire reported 382 tests, 0 failures, 0 errors, and 0 skipped. |
| Engine targeted tests | PASS | `mvn -q -f javaspec-junit-platform-engine/pom.xml -Dtest=JavaspecTestEnginePhase17Test test` passed with 12 tests, 0 failures, 0 errors, and 0 skipped. |
| Engine Maven verification | PASS | `mvn -q -f javaspec-junit-platform-engine/pom.xml verify` passed with 12 tests, 0 failures, 0 errors, and 0 skipped. |
| Core runtime dependency audit | PASS | `mvn dependency:tree -Dscope=runtime` passed; root runtime tree was only `org.javaspec:javaspec`. |
| Engine runtime dependency audit | PASS | `mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime` passed; runtime dependencies were core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`; no runtime `junit-jupiter`, `junit-platform-launcher`, or `junit-platform-testkit`. |
| Blockers | PASS | None reported. |

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

- Phase 17 is implemented as a standalone optional JUnit Platform engine artifact at `javaspec-junit-platform-engine/`, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact.
- The artifact is `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging `jar`, Java source/target `1.8`, and uses Java 8-compatible JUnit Platform `1.10.2`; it avoids JUnit Platform 6/JUnit 6.
- Runtime dependencies are isolated to the optional engine artifact: core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, and transitives `opentest4j`, `junit-platform-commons`, and `apiguardian-api`.
- Test-only dependencies in the engine artifact include JUnit Platform Launcher, JUnit Platform TestKit, and JUnit Jupiter.
- Main implementation is `javaspec-junit-platform-engine/src/main/java/org/javaspec/junit/platform/JavaspecTestEngine.java`.
- ServiceLoader registration is `javaspec-junit-platform-engine/src/main/resources/META-INF/services/org.junit.platform.engine.TestEngine`, containing `org.javaspec.junit.platform.JavaspecTestEngine`; engine id is `javaspec`.
- Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`.
- Configuration parameters include `javaspec.configFile`, `javaspec.suite`, `javaspec.specDir`/`javaspec.specRoot`, `javaspec.classFilters`/`classFilter`/`class`, `javaspec.exampleFilters`/`exampleFilter`/`example`, and `javaspec.stopOnFailure`.
- Class, package, method, and unique-id selectors are supported as filters over canonical discovery results.
- Execution delegates to canonical no-JUnit `JavaspecLauncher` using discovered specs.
- javaspec result states map to JUnit Platform listener events: passed -> successful, failed assertion results -> failed assertion-style throwable, broken results -> failed/error-style throwable, and skipped/non-loadable -> skipped.
- UniqueId segments use `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and `[example:<methodName>]`.
- The engine avoids `System.exit` and does not require changes to javaspec spec authoring style.
- The engine is an optional IDE/CI adapter only. Projects that do not opt into it still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths.
- Tester added `javaspec-junit-platform-engine/src/test/java/org/javaspec/junit/platform/JavaspecTestEnginePhase17Test.java` with 12 tests.
- Test coverage includes ServiceLoader engine id discovery, empty/no-spec discovery and execution, compiled passing spec success, assertion failure mapping, non-assertion throwable mapping, source-only/non-loadable spec skipped, config/suite/specRoot/specDir/class/example filters, class/package/method/unique-id selectors, stop-on-failure skip behavior, and canonical launcher/no-`System.exit`/no-CLI-adapter source guard.

## Phase 16 verification update

Date: 2026-06-03

This report records the completed Phase 16 verification results provided by the Java tester for the standalone optional Gradle plugin integration. Phase 15 Maven plugin evidence, Phase 14 no-JUnit invocation evidence, and Phase 12 Distrobox multi-JDK evidence remain below.

## Phase 16 executive summary

| Area | Result | Notes |
|---|---|---|
| Core install for standalone plugin verification | PASS | `mvn -q -DskipTests install` passed before Gradle plugin verification. |
| Core Maven verification | PASS | `mvn -q verify` passed. |
| Core runtime dependency audit | PASS | `mvn dependency:tree -Dscope=runtime` passed; root runtime tree contained only `org.javaspec:javaspec`. |
| Gradle plugin tests | PASS | `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin test` passed with 11 plugin tests. |
| Gradle plugin build | PASS | `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin build` passed. |
| Gradle plugin runtimeClasspath audit | PASS | `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath` passed and showed only `org.javaspec:javaspec:0.1.0-SNAPSHOT`. |
| Gradle plugin testRuntimeClasspath audit | PASS | `/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration testRuntimeClasspath` passed and showed javaspec, JUnit, and Hamcrest only. |
| Cached Gradle 7.4.2 on installed Java 21 | BLOCKED | Attempted command was blocked with `Unsupported class file major version 65`; this is an environment/tooling compatibility blocker for that cached executable, not a javaspec feature failure. Do not claim Gradle 7.4.2 verification passed. |

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

- Phase 16 is implemented as a standalone optional Gradle plugin artifact at `javaspec-gradle-plugin/`, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact.
- Plugin scaffold files include `settings.gradle`, `build.gradle`, plugin-local `.gitignore`, and plugin-local `README.md`.
- `build.gradle` uses `java-gradle-plugin`, group `org.javaspec`, version `0.1.0-SNAPSHOT`, Java source/target `1.8`, plugin id `org.javaspec`, implementation class `org.javaspec.gradle.JavaspecPlugin`, Maven local/core dependency `org.javaspec:javaspec:0.1.0-SNAPSHOT`, and plugin-local TestKit/JUnit test dependencies.
- Main implementation files are `JavaspecPlugin`, `JavaspecExtension`, and `JavaspecRunTask` in `javaspec-gradle-plugin/src/main/java/org/javaspec/gradle/`.
- The plugin registers extension `javaspec` and task `javaspecRun` in group `verification`.
- When the Gradle Java plugin/source sets are present, `javaspecRun` defaults to the `test` source set runtime classpath and depends on `testClasses`.
- The task supports `skip`, `failOnFailure` defaulting to true, `stopOnFailure`, `configFile`, `suite`, `specDir`/`specRoot`, class filters, example filters, built-in formatter `progress|pretty`, JSON report file aliases (`reportFile`, `jsonReportFile`), and JUnit XML-compatible report file aliases (`junitXmlReportFile`, `junitXmlFile`).
- The task loads javaspec configuration when configured, selects suites, builds `SpecDiscoveryRequest` with `SpecNamingConvention`, uses a `URLClassLoader` over the Gradle classpath, sets/restores the thread context classloader, closes the loader, writes reports via core writers, logs through Gradle, throws `GradleException` on failed/broken examples when `failOnFailure=true`, and delegates to canonical no-JUnit `JavaspecLauncher` without `System.exit`.
- No JUnit is required in projects under test; JUnit is only a plugin test dependency.
- Tester added `javaspec-gradle-plugin/src/test/java/org/javaspec/gradle/JavaspecGradlePluginTest.java` with 11 tests.
- Test coverage includes task registration/default `testClasses` wiring, plugin id/no-spec success, compiled passing spec via default test source set runtime classpath, JSON/JUnit XML report writing, failed/broken examples fail by default after reports, `failOnFailure=false`, `skip=true`, config/suite/class/example filters, explicit `specRoot`, invalid config diagnostics, invalid report path diagnostics, and canonical launcher/no-`System.exit` source guard.

## Phase 15 verification update

Date: 2026-06-03

This report records the completed Phase 15 verification results provided by the Java tester for the standalone optional Maven plugin integration. Phase 14 no-JUnit invocation evidence and Phase 12 Distrobox multi-JDK evidence remain below.

## Phase 15 executive summary

| Area | Result | Notes |
|---|---|---|
| Core Maven verification | PASS | `mvn -q verify` passed with 382 core tests. |
| Core install for standalone plugin verification | PASS | `mvn -q -DskipTests install` passed before plugin verification. |
| Plugin targeted tests | PASS | `mvn -q -f javaspec-maven-plugin/pom.xml -Dtest=JavaspecRunMojoTest test` passed with 12 plugin tests. |
| Plugin Maven verification | PASS | `mvn -q -f javaspec-maven-plugin/pom.xml verify` passed with 12 plugin tests. |
| Core runtime dependency audit | PASS | `mvn dependency:tree -Dscope=runtime` passed; root runtime tree contained only `org.javaspec:javaspec`. |
| Plugin runtime dependency audit | PASS | `mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime` passed; plugin runtime tree contained the plugin plus compile-scope core `org.javaspec:javaspec` only. |
| Plugin adapter behavior | PASS | Tester added coverage for JUnit XML report I/O failure handling, plugin POM dependency scopes, and a guard that the Mojo delegates to `org.javaspec.invocation.JavaspecLauncher` without `System.exit` or direct low-level runner coupling. |
| Blockers | PASS | None reported. |

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

- Phase 15 is implemented as a standalone optional Maven plugin artifact at `javaspec-maven-plugin/`, intentionally not registered as a root module so repository-root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact.
- `javaspec-maven-plugin/pom.xml` packages `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin`, uses Java source/target `1.8`, goal prefix `javaspec`, Maven API baseline `3.6.3`, Maven API and plugin annotations in `provided` scope, JUnit in `test` scope, and compile-scope dependency on core `org.javaspec:javaspec`.
- Typical local standalone plugin build sequence is root `mvn install` or `mvn -q -DskipTests install` before `mvn -f javaspec-maven-plugin/pom.xml verify`.
- `JavaspecRunMojo` provides `javaspec:run`, default phase `verify`, requires test dependency resolution, uses Maven test classpath, supports config/suite/specDir/specRoot selection, class/example filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible reports, Maven logging, and delegates to canonical no-JUnit `JavaspecLauncher` without `System.exit`.
- No JUnit is required in projects under test; JUnit remains only a plugin test dependency.

## Phase 14 verification update

Date: 2026-06-03

This report records the completed Phase 14 verification results provided by the Java tester for the no-JUnit invocation, explicit classpath, and JUnit XML-compatible reporting increment. Phase 12 Distrobox multi-JDK evidence remains the cross-JDK compatibility matrix below.

## Phase 14 executive summary

| Area | Result | Notes |
|---|---|---|
| Phase 14 targeted tests | PASS | `mvn -q -Dtest=JavaspecLauncherTest,JUnitXmlReportWriterTest,MainPhase14CliTest test` passed with 18 tests. |
| Full Maven verification | PASS | `mvn verify` passed with 382 tests, 0 failures, 0 errors, and 0 skipped. |
| Runtime dependency audit | PASS | `mvn dependency:tree -Dscope=runtime` passed; runtime tree contains only the root artifact. |
| Programmatic invocation | PASS | Tester verified the no-`System.exit` `org.javaspec.invocation` API behavior through `JavaspecLauncherTest`. |
| Explicit classpath CLI | PASS | Tester verified `--classpath`, `--classpath-file`, verbose entry listing, UTF-8 classpath-file parsing, and `describe` rejection through `MainPhase14CliTest`. |
| JUnit XML-compatible reporting | PASS | Tester verified no-spec, normal, failing, dry-run, alias, combined JSON/XML, and I/O failure behavior through `JUnitXmlReportWriterTest` and `MainPhase14CliTest`. |
| Blockers | PASS | None reported. |

## Phase 14 verified commands

```bash
mvn -q -Dtest=JavaspecLauncherTest,JUnitXmlReportWriterTest,MainPhase14CliTest test
mvn verify
mvn dependency:tree -Dscope=runtime
```

## Phase 14 verification details

- Programmatic no-JUnit invocation under `org.javaspec.invocation` returns structured `JavaspecInvocationResult` values and does not call `System.exit`.
- `JavaspecExitCode` maps passing, skipped-only, and no-spec runs to exit code `0`, and failed or broken runs to exit code `1`.
- `javaspec run --classpath <path-list>` uses `File.pathSeparator` entries, and `--classpath-file <file>` reads UTF-8 non-empty, non-comment entries; explicit entries are used for type existence checks and spec execution.
- `describe` rejects classpath and JUnit XML report options as run-only usage.
- `org.javaspec.reporting.JUnitXmlReportWriter` writes dependency-free UTF-8 JUnit XML-compatible reports from `RunResult`.
- `--junit-xml` and `--junit-xml-file` write no-spec and normal run reports after output, including failing/broken runs before exit `1`.
- Dry-run pending generation/update exits before execution and does not write reports.
- JUnit XML report I/O failures exit `70` and include path diagnostics.
- Existing JSON `--report` / `--report-file` behavior remains compatible, and JSON plus JUnit XML-compatible reports can be requested together.
- Limitation confirmed for documentation: javaspec still does not compile source/spec files itself; explicit classpath entries must point to already compiled classes or archives.

## Phase 12 consolidated quality matrix

Date: 2026-06-03

This section records the completed Phase 12 verification results provided by the Java tester. The earlier host-only unavailable runtime matrix is superseded by the completed Distrobox multi-JDK matrix below. The tester reported no production or test code changes for Phase 12.

## Phase 12 executive summary

| Area | Result | Notes |
|---|---|---|
| Container toolchain | PASS | Distrobox `1.8.2.5` with Podman `5.8.2` was used for the multi-JDK matrix. |
| Multi-JDK Maven verification | PASS | Java 8, 11, 17, 21, and 25 containers each passed `mvn clean` and `mvn verify`. |
| Test totals | PASS | Each container ran 364 tests with 0 failures, 0 errors, and 0 skipped. |
| Phase 12 runtime dependency audit | PASS | Java 25 container runtime scope contained only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`. |
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

The matrix containers were created and stopped, not removed: `javaspec-jdk8-matrix`, `javaspec-jdk11-matrix`, `javaspec-jdk17-matrix`, `javaspec-jdk21-matrix`, and `javaspec-jdk25-matrix`.

## Distrobox multi-JDK matrix

| JDK | Container | Java runtime | Maven | Result | Test totals |
|---|---|---|---|---|---|
| Java 8 | `javaspec-jdk8-matrix` | `1.8.0_492` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 11 | `javaspec-jdk11-matrix` | `11.0.31` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 17 | `javaspec-jdk17-matrix` | `17.0.19` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 21 | `javaspec-jdk21-matrix` | `21.0.11 LTS` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 25 | `javaspec-jdk25-matrix` | `25.0.3 LTS` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |

JDK 17, 21, and 25 emitted only the expected Maven compiler bootstrap/obsolete-option warnings for the Java 8 source/target settings.

## Supplemental compatibility and regression audits

The Distrobox matrix is the authoritative runtime matrix. Earlier Phase 12 compatibility and regression audits remain part of the consolidated evidence:

- Targeted regression tests passed for `TypeSkeletonGeneratorTest`, `ClassMethodUpdaterTest`, `MainPhase11ReportCliTest`, `RunFormatterRegistryTest`, `ExtensionContextTest`, and `RunReportWriterTest` with 43 tests, 0 failures, 0 errors, and 0 skipped.
- Maven compiler `source` and `target` remain `1.8`.
- Production sources compiled with `javac --release 8` on JDK 21.
- 103 production class files were inspected and all used Java 8 classfile major version `52`.
- The constant-pool audit found 82 unique direct `java.*` class references, 70 intentional post-Java-8 metadata string hits, and 0 direct post-Java-8 API references.

## Runtime dependency audit

The runtime dependency audit was run in `javaspec-jdk25-matrix` with:

```bash
mvn dependency:tree -Dscope=runtime
```

Result: PASS. Runtime scope contained only the root artifact:

```text
org.javaspec:javaspec:jar:0.1.0-SNAPSHOT
```

No third-party runtime dependency leakage was found.

## Java 25 Gatherer runtime validation

The Java 25 runtime reflection probe passed in `javaspec-jdk25-matrix` for all expected stream gatherer types:

- `java.util.stream.Gatherer`
- `java.util.stream.Gatherer$Downstream`
- `java.util.stream.Gatherer$Integrator`
- `java.util.stream.Gatherer$Integrator$Greedy`
- `java.util.stream.Gatherers`

## Related documentation

- [ARC42 quality requirements](arc42/10-quality-requirements.md) summarize the quality scenarios backed by this report.
- [ARC42 risks and technical debt](arc42/11-risks-and-technical-debt.md) records remaining limitations that are not test failures.
- [README](../README.md) and the [user manual](usermanual/Home.md) reference the Phase 20 verification, Phase 19 verification, Phase 18 verification, Phase 17 verification, Phase 16 verification, Phase 15 verification, Phase 14 verification, and Phase 12 matrix for user-facing verification claims.

## Conclusion

Phase 20 verification is complete for release-readiness scaffolding: the MIT `LICENSE` matches `origin/main`, MIT license and Mario Giustiniani maintainer metadata passed Maven and Gradle generated-POM checks, version alignment, script syntax/executable checks, whitespace checks, effective POM generation, root verify/runtime audit, root/Maven plugin/JUnit engine source/javadoc artifact checks, standalone Maven plugin and JUnit engine verification, Gradle publication POM generation, Gradle plugin build/artifact checks, Gradle runtime audit, and full aggregate verification passed locally. No publish/deploy/signing commands were run, and Phase 20 has no remote CI success claim. Publication remains intentionally postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved. Phase 19 verification is complete for the post-roadmap release/CI hardening increment: script syntax/executable validation, local GitHub Actions YAML parse, whitespace checks, and full local aggregate verification through `scripts/verify-all.sh` passed with no blockers; the user/maintainer also confirmed green GitHub Actions status for HEAD `4d30e63` on `develop`. No GitHub run IDs, URLs, durations, or logs were independently queried here. Phase 18 verification is complete for the stable identifier/source-location/report polish increment: targeted changed tests, full core test/verify runs, root runtime dependency audit, core install, standalone Maven plugin verification, standalone JUnit Platform engine verification, standalone Gradle plugin verification, and Gradle runtimeClasspath audit passed with no blockers. Phase 17 verification is complete for the standalone optional JUnit Platform engine. Phase 16 verification is complete for the standalone optional Gradle plugin using Gradle 8.8 on the installed Java 21 runtime; the cached Gradle 7.4.2 executable was blocked by Java 21 with `Unsupported class file major version 65`, which remains an environment/tooling compatibility blocker for that cached executable, not a javaspec feature failure. Phase 15 verification is complete for the standalone optional Maven plugin. Phase 14 verification is complete for no-JUnit invocation, explicit classpath input, and JUnit XML-compatible reporting. Phase 12 is fully completed through the Distrobox multi-JDK matrix. Java 8, 11, 17, 21, and 25 containers all passed `mvn clean` and `mvn verify` with identical clean test totals. The Java 25 runtime Gatherer probe and Java 25 runtime dependency audit also passed.
