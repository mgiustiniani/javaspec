# 7. Deployment View

## 7.1 Runtime Artifact

javaspec core is packaged as a Maven-built Java artifact with CLI main class `org.javaspec.cli.Main`. The current repository build produces `target/javaspec-0.1.0-SNAPSHOT.jar`.

Phase 15 also provides a standalone optional Maven plugin artifact at `javaspec-maven-plugin/`, packaging `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin`. It is intentionally not registered as a root module so repository-root verification continues to build and audit only the core artifact.

Phase 16 provides a standalone optional Gradle plugin artifact at `javaspec-gradle-plugin/` with plugin id `org.javaspec`. It is intentionally not registered as a root Maven module and remains outside the core artifact.

Phase 17 provides a standalone optional JUnit Platform engine artifact at `javaspec-junit-platform-engine/`, packaging `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT` as a Java 8-compatible `jar` with engine id `javaspec`. It is intentionally not registered as a root Maven module and remains outside the core artifact.

Phase 19 adds deployment-time verification assets: executable `scripts/verify-all.sh` for aggregate local release verification and `.github/workflows/ci.yml` for GitHub Actions. Phase 20 adds release-readiness assets: executable `scripts/check-version-alignment.sh`, `CHANGELOG.md`, `RELEASING.md`, the confirmed MIT `LICENSE`, MIT license and maintainer metadata, Maven `release-artifacts` profiles for local sources/javadocs, Gradle source/javadoc jar readiness, and safe URL/SCM/GitHub Issues metadata. Phase 21 adds adoption-time assets: standalone examples under `examples/`, `scripts/verify-examples.sh`, JSON report schema docs, and golden report examples. Phase 22 updates those report/schema/golden assets for explicit skipped/pending semantics. Phase 23 adds runtime diagnostics for classpath/execution availability without changing artifact packaging or adding compilation. Phase 24 adds configuration-level report destinations without changing artifact packaging, report schemas, or writers. Phase 25 adds JDK ServiceLoader discovery for external run formatter/extension providers without changing artifact packaging, report schemas/content, integrated compilation, dependencies, publishing, or Maven plugin formatter controls. These assets do not change runtime packaging, do not publish artifacts, do not sign artifacts, do not add secrets, do not add portal publication/credentials, and do not convert the repository to a Maven multi-module reactor.

The core runtime artifact is intentionally small:

- Java 8 source/target compatibility.
- No third-party runtime dependencies.
- Package base `org.javaspec`.
- JDK APIs only at runtime.
- Post-Java-8 capabilities represented as metadata, source text, or reflection-only probes.

## 7.2 Deployment Environments

| Environment | Usage |
|---|---|
| Developer workstation | Run `describe` and `run`, review generated source, execute already-compiled specs, select built-in or ServiceLoader-discovered formatter names, inspect execution-availability diagnostics when compiled classes or dependencies are unavailable, write JSON or JUnit XML-compatible reports with stable ids, pending counts, and source metadata where available when requested by CLI or config destinations, and inspect standalone adoption examples. |
| Build tool or IDE classpath | Supplies compiled production/spec classes to the reflection runner through the process classpath, explicit CLI classpath entries, optional Maven plugin test classpath integration, optional Gradle plugin test source set runtime classpath integration, optional JUnit Platform engine runtime classpath integration, or programmatic invocation classloaders. For CLI and Gradle runs, the effective run classloader can also supply ServiceLoader formatter/extension providers. The CLI, invocation API, and JUnit Platform engine adapter do not compile source/spec files themselves; Phase 23 diagnostics report when discovered specs/examples are unavailable to the supplied classloader. |
| CI pipeline | Runs root `mvn verify`, dependency audits, optional standalone Maven/Gradle/JUnit Platform engine verification, `scripts/check-version-alignment.sh`, `scripts/verify-all.sh`, `scripts/verify-examples.sh`, optional local source/javadoc artifact checks, optional `javaspec run --dry-run`, optional explicit classpath runs, optional `javaspec run --report <file>` / `--junit-xml <file>` or equivalent config report destinations, optional ServiceLoader formatter jars on CLI/Gradle run classpaths, and consumes exit codes, stable report identifiers/source metadata/pending counts, execution-availability diagnostics, or JUnit Platform engine events when that optional engine is selected. The GitHub Actions workflow currently defines a Java 8/11/17/21/25 core matrix and a Java 21 full-verification job through `scripts/verify-all.sh`, including examples by default unless explicitly skipped, with no publishing or secrets; Phase 20/21/22 remote success for HEAD `5088e96` on `develop` is user-/maintainer-confirmed. |
| Java 8 runtime | Compatibility floor for the production binary. |
| Java 11/17/21/25 runtimes | Supported target/runtime matrix entries; newer APIs remain metadata/reflection-only unless generated source is compiled by a suitable JDK. |

## 7.3 Build and Verification Commands

Repository core verification uses Maven:

```sh
mvn verify
mvn dependency:tree -Dscope=runtime
```

Repository-root `mvn verify` remains intentionally core-only. Release-readiness version alignment uses:

```sh
scripts/check-version-alignment.sh
```

Aggregate local release verification uses:

```sh
scripts/verify-all.sh
```

Standalone examples verification can also be run directly:

```sh
scripts/verify-examples.sh
```

`scripts/check-version-alignment.sh` checks root Maven, standalone Maven plugin, standalone JUnit Platform engine, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` alignment. `scripts/verify-all.sh` runs version alignment first, then runs root verify/audit, installs the current core snapshot, verifies and audits the standalone Maven plugin, verifies and audits the standalone JUnit Platform engine, verifies/audits the standalone Gradle plugin, and runs standalone examples verification by default. It supports `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, explicit `JAVASPEC_SKIP_GRADLE=1`, and explicit `JAVASPEC_SKIP_EXAMPLES=1`; `scripts/verify-examples.sh` also supports `JAVASPEC_SKIP_GRADLE_EXAMPLE=1`. Gradle resolution order is explicit `JAVASPEC_GRADLE_BIN`, repository `./gradlew`, `/tmp/gradle-8.8/bin/gradle`, then `gradle` on `PATH`.

Standalone optional Maven plugin verification first installs the current core, then verifies the plugin POM:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
```

Standalone optional Gradle plugin verification also installs the current core first, then verifies the plugin with a compatible Gradle executable:

```sh
mvn -q -DskipTests install
gradle -p javaspec-gradle-plugin build
gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
```

Standalone optional JUnit Platform engine verification installs the current core first, then verifies the engine POM:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
```

Optional local source/javadoc artifact checks are packaging checks only and do not sign, stage, deploy, or publish:

```sh
mvn -q -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests package
gradle -p javaspec-gradle-plugin clean test build
```

Phase 20 verification passed `LICENSE` identity against `origin/main` blob `b990d5492f3ef404ffc145890b83e51914351bb5`, script syntax/executable validation, version alignment at `0.1.0-SNAPSHOT`, whitespace checks including `git diff --check` and `git diff --cached --check`, effective POM generation, Maven MIT license/maintainer metadata checks, Gradle generated POM metadata checks, root `mvn -q verify` with 386 tests and no failures/errors/skips, root runtime dependency audit with only `org.javaspec:javaspec`, root/Maven plugin/JUnit engine `release-artifacts` packaging with non-empty main/sources/javadoc jars found, root `mvn -q -DskipTests install`, standalone Maven plugin `verify` with 12 tests, standalone JUnit Platform engine `verify` with 12 tests, standalone Gradle plugin POM generation and `clean test build` with 11 tests and non-empty main/sources/javadoc jars found, Gradle runtime dependency audit, and full aggregate verification through `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh`. Phase 21 verification passed no-core-Java-change checks, schema/golden parsing and validation, standalone examples verification, ignored generated-output checks, and full aggregate verification with the examples section. Phase 22 verification passed targeted changed tests, root test/verify/runtime audit/install, standalone Maven/JUnit Platform/Gradle adapter verification and runtime audits, examples verification, and aggregate verification. No publish/deploy/signing commands were run. After Phase 20/21/22 were pushed, remote GitHub Actions success for HEAD `5088e96` on `develop` was user-/maintainer-confirmed; no run IDs, URLs, durations, or logs were independently queried. Phase 19 remote GitHub Actions success remains user-/maintainer-confirmed for HEAD `4d30e63` on `develop`. Phase 18 stable id/source/report verification, Phase 17 JUnit Platform engine verification, Phase 16 Gradle plugin verification, Phase 15 Maven plugin verification, and Phase 12 Distrobox/Podman multi-JDK verification remain recorded in [Test and Quality Report](../test-report.md).

## 7.4 Classpath Requirements for Execution

`javaspec run` discovers source files but executes examples only when compiled spec classes are available on the effective classloader or selected explicit classloader. Phase 23 diagnostics make availability failures explicit; they do not compile source/spec files or repair classpaths.

Consequences for deployment:

- A source-only spec tree can be discovered but its examples are marked `SKIPPED` with execution-availability reasons.
- Users should compile the project or run javaspec from a launcher/classpath that includes compiled spec and production classes when they expect execution.
- CLI users can pass already compiled directories, dependency archives, and external formatter/extension provider jars with `--classpath <path-list>` or a UTF-8 `--classpath-file <file>`; path-list separators use `File.pathSeparator`.
- Programmatic callers can supply a classloader through `JavaspecInvocation` and can call `RunDiagnostics.executionAvailabilityLines(RunResult)` for deterministic availability lines.
- Build systems may run javaspec after compilation or use `--dry-run` earlier to detect pending generated work without requiring compiled specs.
- The optional Maven plugin uses Maven test dependency resolution and the Maven test classpath to invoke the canonical runner during its `javaspec:run` goal; when availability diagnostics exist, it logs `javaspec:` warnings and the Maven test classpath element count.
- The optional Gradle plugin uses the configured Gradle classpath and, when Java plugin source sets are present, defaults `javaspecRun` to the `test` source set runtime classpath and `testClasses` dependency; external formatter/extension provider jars can be present on that run classpath, and when availability diagnostics exist, it logs `javaspec:` warnings and the Gradle classpath element count.
- The optional JUnit Platform engine uses the JUnit Platform runtime classpath provided by the selected launcher and filters canonical discovery through JUnit Platform selectors/configuration parameters; it still requires compiled specs/classes/dependencies on that classpath.

## 7.5 Generated Source Deployment

The javaspec binary remains Java 8-compatible even when it generates source forms that require newer Java language levels:

| Generated source form | Minimum Java source level to compile generated source |
|---|---:|
| Class, final class, interface, enum, annotation | 8 |
| Record | 16 |
| Sealed class or sealed interface | 17 |

Generated post-Java-8 source forms are text output only from the Java 8-compatible generator. Compiling those generated files is the responsibility of the target project and requires an appropriate JDK/source level.

## 7.6 Runtime Dependency Deployment Constraint

The core runtime dependency audit must continue to show only the project artifact in runtime scope:

```text
org.javaspec:javaspec:jar:0.1.0-SNAPSHOT
```

Phase 14's programmatic invocation API and JUnit XML-compatible writer remain inside the core runtime without adding third-party runtime dependencies. Phase 18 stable id/source metadata and additive report fields also remain dependency-free. Phase 19 release/CI verification assets, Phase 20 release-readiness scaffolding, Phase 21 adoption/report assets, Phase 22 explicit skipped/pending semantics, Phase 23 execution-availability diagnostics, Phase 24 configuration-level report destinations, and Phase 25 ServiceLoader formatter/extension discovery do not add runtime dependencies and do not change runtime artifact packaging. The Phase 15 Maven plugin remains outside the core runtime as a standalone optional artifact: Maven API and plugin annotations are `provided`, JUnit is only a plugin test dependency, and the plugin runtime tree contains the plugin plus compile-scope core `org.javaspec:javaspec` only. The Phase 16 Gradle plugin also remains outside the core runtime as a standalone optional artifact: JUnit/TestKit are only plugin test dependencies, and the verified Gradle runtimeClasspath contains only core `org.javaspec:javaspec:0.1.0-SNAPSHOT`. The Phase 17 JUnit Platform engine remains outside the core runtime as a standalone optional artifact: its runtime dependencies are core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`, with no runtime `junit-jupiter`, `junit-platform-launcher`, or `junit-platform-testkit`. Projects that do not opt into the engine keep no-JUnit execution paths and no JUnit dependency.

The MIT `LICENSE` and confirmed maintainer metadata are available, but public publication remains intentionally postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are available. The current release-readiness profiles and Gradle publishing metadata are local readiness scaffolding only, not deployment automation.
