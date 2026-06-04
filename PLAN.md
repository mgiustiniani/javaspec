# javaspec Implementation Plan

This plan defines the initial delivery path for javaspec, a Java 8-compatible, zero-runtime-dependency Java port inspired by phpspec.

## Current Implementation Status — Implemented and Verified

Phases 2 through 18 of the original numbered roadmap are implemented and verified. Phase 19 and Phase 20 are post-roadmap hardening/readiness increments that are also implemented and verified locally. Phase 12 compatibility and quality verification is fully complete through the Distrobox multi-JDK matrix for Java 8, 11, 17, 21, and 25; Phase 14 adds the no-JUnit integration foundation for programmatic invocation, explicit classpath execution, and JUnit XML-compatible reporting; Phase 15 adds a standalone optional Maven plugin adapter over the canonical runner; Phase 16 adds a standalone optional Gradle plugin adapter over the canonical runner; Phase 17 adds a standalone optional JUnit Platform engine adapter over the canonical runner; Phase 18 adds stable identifier, source-location, and report-consistency polish for IDE/CI consumers; Phase 19 adds non-disruptive aggregate release verification and a GitHub Actions CI workflow while preserving the current standalone-adapter architecture; and Phase 20 adds release-readiness scaffolding with MIT license and maintainer metadata resolved, but without public publishing, signing, secrets, portal credentials, runtime dependencies, or Maven multi-module conversion.

- Phase 2 implemented the Java 8 Maven project, zero-runtime-dependency guard, PHPSpec-style `describe`/`run` split, specification/support skeletons, and gated production type/method generation.
- Phase 3 implemented Java LTS target profiles `java8`, `java11`, `java17`, and `java21`, plus the forward-looking `java25` profile, the profile catalog, API-symbol metadata, target-profile compatibility checks, and reflection-only API availability probes.
- Phase 4 implemented the zero-runtime-dependency line-based configuration format, `--config <file>` and `--suite <name>` integration, suite-level spec/source directories, suite package prefixes, naming convention integration, and suite/class/example discovery filters.
- The Phase 5/6 MVP implemented `org.javaspec.runner`, keeps `javaspec run` discovery/generation/update behavior, and executes filtered discovered examples when compiled spec classes are available on the effective classloader.
- Runner behavior: existing `DiscoveredSpec`/`SpecExample` metadata remains the execution source, so suite/class/example filters remain effective; each example gets a fresh spec instance; optional public no-arg `let()` runs before each example and optional public no-arg `letGo()` runs after each example.
- Result states are `PASSED`, `FAILED` for `AssertionError`, `BROKEN` for non-assertion throwables/lifecycle/reflection errors, and `SKIPPED` for non-loadable spec classes or missing reflected example methods. The CLI exits `1` for failed or broken executable examples.
- Phase 7 expanded `Matchable` with negated equality aliases, type/instance aliases, `shouldImplement`, string negations, count/empty helpers for arrays/collections/maps/character sequences/iterables, and map key/value helpers.
- Phase 7 expanded `ObjectBehavior` direct convenience assertion methods that delegate through `match(actual)`, kept `MatcherRegistry` zero-dependency with a default negated-equality matcher, and updated `SpecDiscovery` so expanded chained matcher names participate in method-discovery/default-return inference where applicable.
- Phase 8 added zero-runtime-dependency interface doubles under `org.javaspec.doubles` using JDK dynamic proxies. The MVP supports ordinary interface doubles, return-value stubbing by method name or exact arguments, null and array-content argument matching, call history, called/not-called/exact-count verification, deterministic `toString`/`equals`/`hashCode`, Java default returns for unstubbed methods, and `ObjectBehavior` double convenience APIs.
- Phase 9 expanded `javaspec run` with run-only controls: `--dry-run`, `--stop-on-failure`, `--formatter <progress|pretty>`, `--profile <java8|java11|java17|java21|java25>`, and `--verbose`. Dry-run never writes or prompts and reports pending would-generate/would-update actions; stop-on-failure stops after the first FAILED or BROKEN executable example; progress output is concise and summary-oriented; pretty output prints per-example status lines plus details; CLI formatter/profile options override valid config/default selections; verbose output prints selected run settings.
- Phase 10 implemented the current advanced code-generation increment for interface-style methods. Missing ordinary interface skeletons render discovered non-static method declarations and skip static descriptors; missing annotation skeletons render compatible no-arg non-static elements and skip incompatible descriptors; missing sealed-interface skeletons render root declarations plus nested permitted implementation bodies with Java default returns; existing ordinary interfaces and annotations can receive missing declarations/elements source-preservingly and idempotently; existing sealed-interface source updates are intentionally deferred.
- Phase 11 implemented public zero-dependency run formatter contracts and deterministic built-in formatter registration, preserving compatible `progress` and `pretty` CLI output; added minimal programmatic extension contracts through `JavaspecExtension`/`Extension` and `ExtensionContext`; and added `javaspec run --report <file>` plus alias `--report-file <file>` for UTF-8 JSON runner reports with `schemaVersion` 1, summary counts, specs, examples, nullable failures, throwable class/message, and stack trace lines.
- Phase 11 reporting behavior: `--report` is run-only and rejected by `describe`; `--verbose` prints the report path when specified; no-spec runs write a valid empty report; normal passing/failing/broken/skipped runs write the report after summary rendering; failed or broken executable examples still exit `1` after the report write; dry-run pending generation/update exits before execution and does not write a report; report write failures print I/O diagnostics and exit `70`.
- Phase 14 implemented the no-JUnit integration foundation under `org.javaspec.invocation`: `JavaspecInvocation`, `JavaspecLauncher`, `JavaspecInvocationResult`, and `JavaspecExitCode` provide a programmatic no-`System.exit` invocation API over canonical discovery, `SpecRunner`, and `RunResult`; structured results expose discovered specs, runner results, success/failure state, and deterministic exit-code mapping where passing, skipped-only, and no-spec runs are `0`, while failed or broken runs are `1`.
- Phase 14 expanded `javaspec run` with explicit classpath input and JUnit XML-compatible reports: `--classpath <path-list>` uses `File.pathSeparator`; `--classpath-file <file>` reads UTF-8 non-empty, non-comment entries; both options are run-only and rejected by `describe`; verbose output lists explicit classpath entries; the selected explicit classloader is used for production type existence checks and spec execution. `--junit-xml <file>` and alias `--junit-xml-file <file>` write dependency-free JUnit XML-compatible reports through `JUnitXmlReportWriter`; JSON and JUnit XML reports can be requested together; no-spec and executed run paths write requested reports after output, failed/broken examples still exit `1` after report writes, dry-run pending generation/update exits before execution and writes no reports, and report I/O failures exit `70` with path diagnostics.
- Phase 15 implemented `javaspec-maven-plugin/` as a standalone optional Maven plugin artifact, intentionally not registered as a root module so repository-root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact. The plugin packages `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin`, exposes goal prefix `javaspec`, provides `javaspec:run` bound by default to `verify`, requires test dependency resolution, uses the Maven test classpath, supports config/suite/specDir/specRoot selection, class/example filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible reports, Maven logging, and delegates to the canonical no-JUnit `JavaspecLauncher` without `System.exit`.
- Phase 16 implemented `javaspec-gradle-plugin/` as a standalone optional Gradle plugin artifact, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact. The plugin id is `org.javaspec`; it registers extension `javaspec` and task `javaspecRun` in Gradle's `verification` group, defaults to the Java plugin `test` source set runtime classpath and `testClasses` dependency when available, supports skip/fail/stop controls, config/suite/specDir/specRoot, class/example filters, built-in `progress` and `pretty` formatters, JSON and JUnit XML-compatible report aliases, logs through Gradle, and delegates to the canonical no-JUnit `JavaspecLauncher` without `System.exit`.
- Phase 17 implemented `javaspec-junit-platform-engine/` as a standalone optional JUnit Platform engine artifact, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact. The artifact is `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging `jar`, Java source/target `1.8`, and uses Java 8-compatible JUnit Platform `1.10.2` rather than JUnit Platform 6/JUnit 6. `JavaspecTestEngine` is discovered through ServiceLoader with engine id `javaspec`, uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`, supports configuration parameters and JUnit Platform class/package/method/unique-id selectors as filters over canonical discovery results, delegates execution to the canonical no-JUnit `JavaspecLauncher`, maps javaspec result states to JUnit Platform listener events, avoids `System.exit`, and does not require changes to javaspec spec authoring style.
- Phase 18 implemented an incremental IDE/CI polish focused on stable identifiers, source locations, and report consistency. Stable id aliases are available on `ExampleResult` (`id()`/`stableId()`/`getId()`/`getStableId()`, with `<specQualifiedName>#<methodName>` semantics matching `fullName()`), `SpecResult` (derived from spec qualified name), and `DiscoveredSpec` (derived from spec qualified name). `SpecExample` carries a 1-based `sourceLine`; discovery computes method declaration lines; `ExampleResult` carries source file path and source line when created from discovered specs/examples; and `SpecResult` carries the spec source file path.
- Phase 18 report updates are additive: JSON reports include spec `id`, `stableId`, and `sourceFile` plus example `id`, `stableId`, `fullName`, and `source { file, line }` while preserving existing fields; JUnit XML-compatible reports include `file` and `line` attributes on `<testcase>` when source data is available while preserving dependency-free JUnit XML-compatible output. The optional JUnit Platform engine retained its stable unique-id shape and MethodSource behavior, with descriptor reporting aligned to stable ids.
- Phase 19 implemented post-roadmap release/CI hardening without converting the repository to Maven multi-module. Root `mvn verify` remains a core-only build/audit. Standalone optional Maven plugin, Gradle plugin, and JUnit Platform engine artifacts remain outside the root Maven reactor. `scripts/verify-all.sh` provides an aggregate local verification path that verifies the core, audits runtime dependencies, installs the current core snapshot, verifies standalone adapters, audits adapter runtime dependencies, and supports `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, and explicit `JAVASPEC_SKIP_GRADLE=1`. `.github/workflows/ci.yml` adds GitHub Actions jobs for a Java 8/11/17/21/25 core matrix and Java 21 full verification through the aggregate script. No publishing, signing, secrets, or deployment behavior was added; remote GitHub Actions success was later user-/maintainer-confirmed for HEAD `4d30e63` on `develop`.
- Phase 20 implemented release-readiness scaffolding only. It added executable `scripts/check-version-alignment.sh`, updated `scripts/verify-all.sh` to run version alignment first, added `CHANGELOG.md` and `RELEASING.md`, copied the confirmed MIT `LICENSE` exactly from `origin/main`, added MIT license metadata and confirmed maintainer/developer metadata for `Mario Giustiniani <mariogiustiniani@gmail.com>` to the root, Maven plugin, and JUnit Platform engine POMs and Gradle generated POM metadata, added safe URL/SCM/GitHub Issues metadata and Maven `release-artifacts` profiles for sources/javadocs only in the root, Maven plugin, and JUnit Platform engine POMs, and added Gradle `maven-publish` plus source/javadoc jar readiness and safe POM URL/SCM/issues metadata. It did not run or configure actual publishing/deployment/signing, secrets, Central Portal publication, Gradle Plugin Portal publication/credentials, runtime dependencies, or Maven multi-module conversion. `scripts/check-version-alignment.sh` verifies root Maven version, Maven plugin version, JUnit engine version, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` alignment.
- Known limitations: javaspec still does not compile source/spec files itself; source-only or otherwise unavailable spec classes are skipped/not executable, and explicit classpath entries or adapter-supplied classpaths must point to already compiled classes or archives. The optional Maven plugin, Gradle plugin, and JUnit Platform engine supply integration paths but are standalone artifacts. They are not verified by root `mvn verify` alone; use `scripts/verify-all.sh` or the explicit standalone verification commands after installing the current core snapshot. Projects that do not opt into the optional JUnit Platform engine still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths. Public publication remains intentionally postponed: the MIT `LICENSE` and confirmed maintainer metadata are resolved, but GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval remain unresolved and unimplemented. The Maven `release-artifacts` profile and Gradle source/javadoc jar readiness create local sources/javadocs only; they do not sign, stage, deploy, or publish. Selected profiles are validated and reported but not deeply enforced during execution yet. External extension discovery/loading is not implemented, so CLI formatter selection remains limited to built-in `progress` and `pretty` even though programmatic formatter registration APIs exist. JSON reporting remains schemaVersion 1 with Phase 18 additive identifier/source fields, JUnit XML-compatible reporting remains intentionally minimal with additive `<testcase file="..." line="...">` attributes only when source data is available, and report destinations are command-line options or build-tool task/plugin settings rather than config-level settings. Count and emptiness checks on a generic `Iterable` consume the iterable and can hang on infinite iterables. Existing sealed-interface source updates are skipped until nested permitted implementations can be updated source-preservingly. Phase 8 doubles do not support concrete class/static/constructor/final-class doubles, primitives, arrays, annotations, enums, wildcard argument matchers, exception/callback stubbing, bytecode libraries, or default-interface-method invocation.
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
- The original numbered roadmap is complete through Phase 18. Phase 19 and Phase 20 are post-roadmap hardening/readiness increments: Phase 19 adds aggregate release/CI verification, and Phase 20 adds release-readiness scaffolding without public publishing. Future feature work should be tracked as new roadmap or backlog items and must not imply that every conceivable IDE/CI or release/publication feature is already implemented.

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
8. Known limitations remain: the MVP reflection runner executes only compiled, classloader-available spec classes and does not compile source/spec files itself; lifecycle support is limited to optional public no-arg `let()`/`letGo()`; source parsing/generation uses Java 8-compatible heuristics rather than a full Java parser; generated post-Java-8 source forms still require an appropriate JDK to compile.
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
10. Bootstrap values are comma-separated metadata only and are not executed yet. Profile and formatter values were introduced as validated configuration values in Phase 4; Phase 9 now selects active profiles and built-in formatter output, while deep profile enforcement remains future work.
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

- The CLI runner does not compile source/spec files itself. Source-only or otherwise unavailable spec classes are skipped/not executable until an external build, IDE, or launcher puts compiled classes on the effective classloader.
- Lifecycle support is intentionally minimal: public no-argument `let()` and `letGo()` only.
- Pending examples, bootstrap execution, deep profile-aware execution, and broader reporting beyond the implemented JSON/JUnit XML-compatible report outputs remain later work. Stop-on-failure and built-in progress/pretty formatter behavior are implemented in Phase 9 and routed through formatter contracts in Phase 11.

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
| Phase 8 MVP collaborators/doubles | README, user manual, ARC42 section 5, ADR 0007, this plan | Implemented and verified: `org.javaspec.doubles` provides JDK-proxy ordinary-interface doubles, method-name and exact-argument return stubs, null and array-content argument matching, immutable call history, called/not-called/exact-count verification, deterministic object methods, Java default returns for unstubbed methods, and `ObjectBehavior` double conveniences. Limitations are explicit: no concrete class/static/constructor/final-class doubles, primitives, arrays, annotations, enums, wildcard matchers, exception/callback stubbing, bytecode libraries, or default-interface-method invocation. |
| Method generation | ADR 0004, this plan, user manual | Implemented and verified: discovery from typed proxy/throw calls, direct subject/setter calls, static factory construction markers, and the expanded chained matcher names where applicable; Java 8-compatible method-body and static factory skeleton generation for class/final/sealed class/enum/record sources remains unchanged; static factory descriptors are skipped by generated support proxies; `--generate` writes non-interactively and interactive `run` prompts before updating existing source files. |
| Phase 10 interface-style method generation | README, user manual, ARC42 section 5, this plan | Implemented and verified: missing ordinary interface skeletons render non-static method declarations and skip static descriptors; missing annotations render compatible no-arg non-static elements and skip incompatible descriptors; missing sealed interfaces render root method declarations and nested permitted implementations with Java default-return bodies; existing ordinary interfaces and annotations receive missing declarations/elements source-preservingly and idempotently; existing sealed-interface source updates remain deferred. |
| Configuration model and inferred defaults | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `JavaspecConfiguration.defaults()` provides the default suite `default`, Maven-style spec/source roots, `spec` package prefix, empty production package prefix, `java8` profile, `progress` formatter, `comment` constructor policy, and empty bootstrap hooks when no config file is supplied. |
| Constructor-policy config default | ADR 0004, ADR 0005, user manual, this plan | Implemented in Phase 4: config key `constructorPolicy`/`constructor-policy` accepts only `delete`, `preserve`, and `comment`; `comment` remains the inferred and config default, and `run --constructor-policy` overrides config explicitly. |
| Explicit suites, paths, profile, and formatter config | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4 and expanded in Phase 9: `--config <file>` and `--suite <name>` select suite configuration; selected-suite `specDir`/`sourceDir` drive `describe`/`run` unless CLI path options override them; selected-suite `specPackagePrefix`/`packagePrefix` drive naming; config `profile` and `formatter` provide run defaults that valid CLI `--profile` and `--formatter` override. Profile selection is validated but not deeply enforced yet. |
| Naming convention integration | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `SpecNamingConvention` maps production names to spec/support packages using configured suite package prefixes, validates naming metadata, and is used by describe, discovery, and support generation. |
| Suite, class, and example filters | README, user manual, ARC42 section 5, this plan | Implemented in Phase 4 and reused by the Phase 5/6 MVP runner: `--suite` selects the configured suite; repeatable `--class` filters by described or spec class names; repeatable `--example` filters by example method name, display name, or source-order index; filtered `DiscoveredSpec`/`SpecExample` metadata controls both generation/update and reflection execution. |
| Phase 5/6 MVP reflection runner | README, user manual, ARC42 section 5, ADR 0006, this plan | Implemented and verified: after discovery/generation/update, `javaspec run` executes examples when compiled spec classes are available on the effective classloader; each example uses a fresh spec instance with optional public no-arg `let()` and `letGo()`; results are `PASSED`, `FAILED`, `BROKEN`, or `SKIPPED`; CLI summary exits `1` for failed/broken executable examples; source-only/unavailable spec classes are skipped because the CLI does not compile them. Phase 9 adds `--stop-on-failure` to stop after the first FAILED or BROKEN executable example while the default remains executing all discovered metadata. |
| Phase 9 CLI expansion | README, user manual, ARC42 section 5, this plan | Implemented and verified: `run --dry-run` performs no writes and no prompts, reports would-generate/would-update actions for related specs/support, support updates, constructors, method bodies/declarations/elements, and missing production type generation, exits `1` when pending work exists, and exits `0` when no pending changes exist and examples pass or skip; `run --stop-on-failure` stops after the first FAILED or BROKEN executable example; `run --formatter <progress|pretty>` selects concise or per-example output and overrides config/default; `run --profile <java8|java11|java17|java21|java25>` validates/selects the profile and overrides config without deep enforcement yet; `run --verbose` prints selected run settings; all Phase 9 controls are rejected for `describe`. |
| Phase 11 formatter/reporting/extension increment | README, user manual, ARC42 section 5, this plan | Implemented and verified: built-in output uses the public zero-dependency `RunFormatter` contract and deterministic `RunFormatterRegistry`; `progress` and `pretty` behavior remains compatible; `JavaspecExtension`/`Extension` and `ExtensionContext` allow programmatic run formatter registration; `javaspec run --report <file>` and alias `--report-file <file>` write UTF-8 JSON reports with `schemaVersion` 1, summary counts, specs, examples, nullable failure details, throwable class/message, and stack trace lines. Reports are run-only, rejected by `describe`, written for no-spec/passing/failing/broken/skipped runs after normal output, skipped for dry-run pending generation/update, and report write failures exit `70`. External extension discovery/loading is not implemented. |
| Phase 14 no-JUnit invocation, explicit classpath, and JUnit XML reports | README, user manual, ARC42 sections 5-11, ADR 0011, test report, this plan | Implemented and verified: `org.javaspec.invocation` exposes `JavaspecInvocation`, `JavaspecLauncher`, `JavaspecInvocationResult`, and `JavaspecExitCode` for no-`System.exit` programmatic calls over canonical discovery/`SpecRunner`/`RunResult`; `javaspec run` accepts `--classpath`, `--classpath-file`, `--junit-xml`, and `--junit-xml-file`; JSON and JUnit XML-compatible reports can be requested together; report I/O failures exit `70`; passing, skipped-only, and no-spec invocation paths map to `0`, while failed/broken execution maps to `1`. |
| Phase 15 optional Maven plugin integration | README, user manual, ARC42 sections 5-12, ADR 0011, test report, this plan | Implemented and verified as standalone optional artifact `javaspec-maven-plugin/`, not a root module. `JavaspecRunMojo` provides `javaspec:run` at default phase `verify`, uses Maven test dependency resolution and test classpath, supports config/suite/specDir/specRoot, class/example filters, stop/fail/skip controls, JSON and JUnit XML-compatible reports, Maven logging, and delegates to canonical `JavaspecLauncher` without `System.exit`. No JUnit is required in projects under test; JUnit is only a plugin test dependency. |
| Phase 16 optional Gradle plugin integration | README, user manual, ARC42 sections 5-12, ADR 0011, test report, this plan | Implemented and verified as standalone optional artifact `javaspec-gradle-plugin/`, not a root Maven module and outside the core artifact. The plugin id is `org.javaspec`; `JavaspecPlugin` registers extension `javaspec` and task `javaspecRun` in group `verification`; Java plugin/source-set defaults use the `test` source set runtime classpath and depend on `testClasses`; `JavaspecRunTask` supports skip/fail/stop controls, config/suite/specDir/specRoot, class/example filters, built-in formatter selection, JSON/JUnit XML-compatible reports, Gradle logging, and canonical `JavaspecLauncher` delegation without `System.exit`. No JUnit is required in projects under test; JUnit is only a plugin test dependency. |
| Phase 17 optional JUnit Platform engine integration | README, `javaspec-junit-platform-engine/README.md`, user manual, ARC42 sections 5-12, ADR 0011, test report, this plan | Implemented and verified as standalone optional artifact `javaspec-junit-platform-engine/`, not a root Maven module and outside the core artifact. The artifact `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT` packages a Java 8-compatible JUnit Platform `1.10.2` `TestEngine` with engine id `javaspec`, ServiceLoader registration, canonical `SpecDiscovery` / `SpecDiscoveryRequest` discovery, configuration parameters, class/package/method/unique-id selector filters, canonical `JavaspecLauncher` execution without `System.exit`, and javaspec-to-JUnit listener event mapping. Projects that do not opt into it still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths. |
| Phase 18 IDE/CI polish | README, user manual, ARC42 sections 5-11, ADR 0010, ADR 0011, test report, this plan | Implemented and verified as an incremental stable identifier/source-location/report polish. `ExampleResult`, `SpecResult`, and `DiscoveredSpec` expose stable id aliases; `SpecExample`, `ExampleResult`, and `SpecResult` carry source metadata where available; JSON reports add spec/example stable id and source fields while preserving existing fields; JUnit XML-compatible `<testcase>` elements add `file` and `line` attributes when source data is available; and the optional JUnit Platform engine retains its stable unique-id shape and MethodSource behavior with descriptor reporting aligned to stable ids. External extension loading, pending examples, deep profile enforcement, and broad new classpath diagnostics were not implemented in Phase 18. |
| Phase 19 post-roadmap release/CI hardening | README, user manual, ARC42 sections 4-11, ADR 0012, test report, this plan | Implemented and verified as a non-disruptive aggregate verification increment. The repository was not converted to Maven multi-module; root `mvn verify` remains core-only; standalone adapters stay outside the root Maven reactor; `scripts/verify-all.sh` verifies core, runtime audits, current-core install, standalone Maven plugin, JUnit Platform engine, and Gradle plugin; `.github/workflows/ci.yml` defines a Java 8/11/17/21/25 core matrix plus Java 21 full verification through the script. Local script/YAML/whitespace verification passed; remote GitHub Actions success is user-/maintainer-confirmed for HEAD `4d30e63` on `develop`. |
| Phase 20 release-readiness scaffolding | README, user manual, `CHANGELOG.md`, `RELEASING.md`, ARC42 sections 4-11, ADR 0013, test report, this plan | Implemented and locally verified as release-readiness scaffolding only. `scripts/check-version-alignment.sh` verifies version alignment across root Maven, standalone Maven plugin, standalone JUnit Platform engine, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion`; `scripts/verify-all.sh` runs that check first. Maven `release-artifacts` profiles produce source/javadoc jars for root, Maven plugin, and JUnit engine; the Gradle plugin build produces source/javadoc jars. MIT license metadata and confirmed maintainer/developer metadata for `Mario Giustiniani <mariogiustiniani@gmail.com>` were added along with safe URL/SCM/GitHub Issues metadata; `LICENSE` matches `origin/main:LICENSE` blob `b990d5492f3ef404ffc145890b83e51914351bb5`. Public publication remains postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved. No publishing, deployment, signing, secrets, runtime dependencies, or Maven multi-module conversion were added. |
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
- Treat missing config as `JavaspecConfiguration.defaults()` and apply command-line path/constructor-policy overrides over selected-suite values while keeping selected-suite package prefixes in the active naming convention.
- Treat bootstrap hooks as parsed metadata until bootstrap execution is implemented; treat profile and formatter settings as active run selections, with CLI overrides over config/default values, while profile selection remains validation/reporting only until deep enforcement is implemented.
- Keep built-in formatter rendering behind the zero-dependency `RunFormatter` contract and deterministic `RunFormatterRegistry`; keep CLI formatter selection limited to built-in names until external extension loading is explicitly implemented.
- Keep run reports dependency-free and based on the immutable runner result model; preserve existing report fields when adding stable identifiers or source metadata; write `--report` output only for run paths that reach no-spec handling or runner summary rendering, skip reports when dry-run exits before execution because pending work exists, and treat report write failures as exit `70` I/O failures.
- Treat the Phase 11 extension API as programmatic registration only; do not imply config-driven loading, classpath scanning, `ServiceLoader`, or plugin activation until implemented and documented.
- Use `DiscoveredSpec` and `SpecExample` as the execution selection source so suite, class, and example filters remain effective for the runner.
- Treat the CLI runner as a classpath reflection executor, not an in-process compiler; source-only or unavailable spec classes are skipped/not executable until compiled classes are present on the effective classloader.
- Prefer generated subject-specific typed support/proxy classes while keeping explicit `match(value)` style APIs available.
- Keep `Matchable`, `ObjectBehavior` direct convenience assertions, and `SpecDiscovery` matcher-name recognition synchronized when matcher names are added.
- Keep core doubles interface-only and JDK-proxy based unless a future ADR authorizes optional advanced integrations; reject unsupported double targets with clear diagnostics.
- Preserve exact-argument double semantics for `null` values and array contents, and keep `ObjectBehavior` double convenience APIs synchronized with `org.javaspec.doubles` control behavior.
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

- Complete PHPSpec-style runner lifecycle beyond the Phase 5/6 MVP reflection runner, including pending examples, bootstrap execution, deep profile-aware execution, and broader reporting beyond the implemented JSON/JUnit XML-compatible report outputs. Stop-on-failure and built-in progress/pretty formatters are implemented in Phase 9 and routed through formatter contracts in Phase 11.
- Broader interface and annotation generation beyond the supported Phase 10 method declarations/elements, plus enum generation beyond minimal skeletons.
- Private constructor source generation and broader named-constructor customization beyond the current static factory skeleton support.
- Template systems beyond the minimal class-like/spec/support skeleton need.
- Return constant generation from expectations beyond Java 8-compatible default returns.
- Full PHPSpec matcher parity beyond the implemented Phase 7 matcher subset.
- Broader Prophecy-inspired or double functionality.

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
4. Implemented top-level keys for `profile`, `formatter`, constructor policy, default suite, and bootstrap metadata; implemented suite keys for spec/source roots, `specPackagePrefix`/`spec-package-prefix`, `packagePrefix`/`package-prefix`, and bootstrap metadata.
5. Integrated `--config <file>` and `--suite <name>` with `describe` and `run` in `org.javaspec.cli.Main`.
6. Applied selected-suite paths unless overridden by command-line spec/source path options. `describe` still rejects command-line source-root options but accepts and ignores `sourceDir` loaded from config.
7. Applied configured package prefixes through `SpecNamingConvention` so `describe`, `run`, discovery, and spec/support generation map between production packages and spec packages consistently.
8. Added naming/discovery metadata for example methods: public `void` methods named `it_*` or `its_*` are extracted with source-order indexes and display names derived from underscores.
9. Added suite selection and run filters: repeatable `--class <name>` filters by described qualified name, described simple name, spec qualified name, or spec simple name; repeatable `--example <name>` filters by example method name, display name, or source-order index. Suite selection through `--suite <name>` selects the configured suite, spec root, source root, and naming convention.
10. Applied the configured constructor policy to `run` unless command-line `--constructor-policy` overrides it.
11. Preserved the missing-production prompt and `--generate` flow for inferred defaults and explicit config.
12. Kept bootstrap values as comma-separated metadata only. Profile and formatter values were parsed/validated configuration values in Phase 4 and are now consumed by Phase 9 run profile/formatter selection.

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
- Explicit config can select suite, paths, target profile, formatter, constructor policy, spec package prefix, and production package prefix. Current `describe`/`run` behavior uses selected suite paths, package prefixes, and constructor policy; Phase 9 `run` also consumes configured profile and formatter defaults, while bootstrap remains parsed metadata until later runner features.
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
2. `javaspec run` executes examples after discovery/generation/update work has completed and only when compiled spec classes are available on the effective classloader.
3. Each example runs on a fresh spec instance constructed through the spec class no-argument constructor.
4. Optional public no-argument `let()` runs before each example; optional public no-argument `letGo()` runs after each example, including when `let()` or the example fails.
5. Result states are implemented: `PASSED` for normal completion, `FAILED` for `AssertionError`, `BROKEN` for non-assertion throwables/lifecycle/instantiation/reflection errors, and `SKIPPED` for non-loadable spec classes or missing reflected example methods.
6. The CLI prints total, passed, failed, broken, and skipped counts and exits `1` when executable examples fail or break.
7. The runner preserves the zero-runtime-dependency policy and does not compile source/spec files itself.

Remaining tasks:

1. Add pending examples, bootstrap execution, deep profile-aware execution, and broader reporting beyond the implemented JSON/JUnit XML-compatible report outputs; stop-on-failure, verbosity, and built-in progress/pretty formatter behavior are implemented in Phase 9 and routed through formatter contracts in Phase 11.
2. Expand failure-specific source-location diagnostics beyond the Phase 18 method/source metadata where useful.
3. Continue to refine typed proxy matcher diagnostics and method-generation reporting without forcing eager subject construction.
4. Keep ADR 0004 construction semantics stable as the runner grows beyond the MVP lifecycle.

Acceptance criteria status:

- Examples run with isolated spec instances.
- `let()`, example execution, and `letGo()` interact predictably for the MVP public no-arg lifecycle.
- Failed and broken examples include throwable summary details in CLI output.
- Exit code `1` is stable for failed/broken executable examples; skipped-only runs remain successful.
- Source-only or unavailable spec classes are skipped until compiled classes are present on the effective classloader.

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
- Wildcard argument matchers, predicate matchers, exception stubbing, callback stubbing, sequences, and side-effect stubbing are not implemented.
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
3. Defined dry-run exits: `1` when pending generation/update work exists; `0` when no pending changes exist and executable examples pass or are skipped-only; failed or broken executable examples still produce exit `1`.
4. Added `javaspec run --stop-on-failure`: the runner stops after the first FAILED or BROKEN executable example. Without this flag, the default remains to process all discovered example metadata.
5. Added `javaspec run --formatter <progress|pretty>`: `progress` is concise and summary-oriented; `pretty` prints per-example status lines plus failed/broken/skipped details. A valid CLI formatter overrides config; otherwise the configured formatter or default `progress` is used.
6. Added `javaspec run --profile <java8|java11|java17|java21|java25>`: the profile is validated and selected, and a valid CLI profile overrides config. Deep profile enforcement is not implemented yet.
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
7. JSON reports use `schemaVersion` 1 and include whole-run summary counts, specs, examples, nullable failure details, throwable class/message, and stack trace lines. Phase 18 additively includes spec `id`, `stableId`, and `sourceFile` plus example `id`, `stableId`, `fullName`, and `source { file, line }` while preserving existing fields.
8. `--report` is run-only and rejected by `describe`/`desc`; `--verbose` prints the report path when specified.
9. No-spec runs with `--report` write a valid empty report. Passing, failing, broken, and skipped-only runs write reports after normal summary rendering; failed or broken executable examples still exit `1` after the report write.
10. Dry-run pending generation/update exits before execution and does not write a report. Report write failures produce I/O diagnostics, include the report path, and exit `70`.
11. No external extension loading is implemented yet: no config-driven activation, classpath scanning, `ServiceLoader`, plugin lookup, or CLI use of extension-provided formatter names.

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

- Formatter extension contracts are public and programmatic, but the CLI currently exposes only built-in `progress` and `pretty` because no external extension loading mechanism is implemented.
- JSON reporting remains schemaVersion 1 runner results with Phase 18 additive stable identifier/source fields; it has no config-level destination, alternate schemas, or streaming mode. Phase 14 adds a separate JUnit XML-compatible report path, and Phase 18 additively emits `<testcase>` `file`/`line` attributes when source data is available.
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
   - `JavaspecExitCode` maps passing, skipped-only, and no-spec runs to `0`, and failed or broken runs to `1`.
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
5. Kept javaspec as a classpath-based executor: it still does not compile source/spec files itself, so explicit classpath entries must point to already compiled classes or archives.

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
8. Execution delegates to canonical no-JUnit `JavaspecLauncher` using discovered specs. It maps javaspec result states to JUnit Platform listener events: passed to successful, failed assertion results to failed assertion-style throwables, broken results to failed/error-style throwables, and skipped/non-loadable results to skipped.
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
5. Did not implement unrelated IDE/CI items in this increment: external extension loading, pending example implementation, deep profile enforcement, or broad new classpath diagnostics.

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
- Phase 20 has no remote GitHub Actions success claim in the recorded evidence.
