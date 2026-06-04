# 0011 — Optional Build-Tool/JUnit Adapters and Canonical javaspec Runner

## Status

Accepted

## Context

javaspec is intended to become usable as a practical test runner in other projects while preserving the Java 8 baseline and the zero-runtime-dependency policy from ADR 0002. The current CLI can discover and execute examples only when compiled spec classes are available on the effective classloader, and Phase 11 added dependency-free JSON reports and public formatter contracts.

Users need better project integration for local development and CI: a no-JUnit invocation path, build-tool classpath handling, CI-friendly reports, Maven and Gradle integration, optional IDE/CI integration through the JUnit Platform, and stable identifiers/source metadata for tool consumers. At the same time, JUnit must not become required for core javaspec usage. Phase 15 implements the standalone optional Maven plugin adapter, Phase 16 implements the standalone optional Gradle plugin adapter, Phase 17 implements the standalone optional JUnit Platform engine adapter, and Phase 18 implements stable identifier/source-location/report polish within the existing boundaries.

## Decision

The javaspec core runner remains canonical. CLI, Maven, Gradle, and JUnit Platform entry points are adapters over the canonical javaspec discovery, runner, result, formatter, and report model; they must not replace the core runner semantics.

No-JUnit execution remains first-class. The next integration foundation should provide an embeddable invocation API that does not call `System.exit`, accepts classpath input from callers or CLI options such as `--classpath` and `--classpath-file` or an equivalent mechanism, writes CI-friendly reports such as JUnit XML without depending on JUnit, and preserves stable exit/report behavior for command-line use.

Maven and Gradle integrations are optional adapter artifacts. They may depend on their build-tool APIs in their own artifacts, but they must invoke the canonical javaspec runner and must not require JUnit in user projects. The Phase 15 Maven plugin is a standalone optional artifact at `javaspec-maven-plugin/`, intentionally not registered as a root module so repository-root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact. It packages `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin`, keeps Maven API/plugin annotations in `provided` scope, keeps JUnit in `test` scope, depends on core `org.javaspec:javaspec`, and delegates `javaspec:run` to `org.javaspec.invocation.JavaspecLauncher` without `System.exit`.

The Phase 16 Gradle plugin is a standalone optional artifact at `javaspec-gradle-plugin/`, intentionally not registered as a root Maven module and outside the zero-runtime-dependency core artifact. It uses `java-gradle-plugin`, plugin id `org.javaspec`, implementation class `org.javaspec.gradle.JavaspecPlugin`, Java source/target `1.8`, core dependency `org.javaspec:javaspec:0.1.0-SNAPSHOT`, and plugin-local TestKit/JUnit test dependencies. It registers extension `javaspec` and task `javaspecRun`, uses the Gradle classpath with Java plugin test source set defaults when present, supports the documented filters/options/reports, logs through Gradle, and delegates to `org.javaspec.invocation.JavaspecLauncher` without `System.exit`.

The Phase 17 JUnit Platform engine is a separate optional module at `javaspec-junit-platform-engine/`. It packages `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT` as a Java 8-compatible `jar`, uses JUnit Platform `1.10.2` rather than JUnit Platform 6/JUnit 6, and may depend on JUnit Platform APIs in that module only. `JavaspecTestEngine` is registered through ServiceLoader with engine id `javaspec`, uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`, supports configuration parameters and class/package/method/unique-id selectors as filters over canonical discovery results, delegates execution to the canonical no-JUnit `JavaspecLauncher`, maps javaspec result states to JUnit Platform listener events, avoids `System.exit`, and must not require changes to the existing javaspec spec style. Phase 18 retains the engine's stable unique-id shape and MethodSource behavior while aligning descriptor reporting to stable ids. The core runtime artifact must not gain a JUnit dependency, and projects that do not opt into the engine keep no-JUnit CLI/programmatic/Maven/Gradle execution paths.

## Consequences

Positive consequences:

- Users can adopt javaspec without adding JUnit to their runtime or test execution path.
- Build-tool plugins and IDE integrations share one canonical result model with stable identifiers and source metadata where available instead of diverging from the core runner.
- The standalone Maven plugin can use Maven test dependency resolution and test classpath integration without changing the core runtime dependency policy.
- The standalone Gradle plugin can use Gradle source set/runtime classpath integration without changing the core runtime dependency policy.
- CI can use no-JUnit execution first, with dependency-free reports and stable exit behavior.
- The optional JUnit Platform engine can improve JUnit Platform-based IDE/CI compatibility without changing core semantics or spec style.

Negative consequences and limitations:

- Practical project integration requires additional adapter artifacts and compatibility testing across CLI, Maven, Gradle, and optional JUnit Platform modes.
- Standalone adapters that are not root modules require explicit build and dependency-audit commands outside repository-root `mvn verify`.
- Gradle plugin verification depends on a Gradle executable compatible with the installed JDK; the cached Gradle 7.4.2 executable was blocked on Java 21 and must not be reported as a javaspec feature failure.
- The core invocation API must be stable enough for adapters, which increases compatibility pressure on runner results, JUnit Platform unique-id mapping, test identifiers, source metadata, and report schemas.
- JUnit XML must be generated internally or through a non-core optional adapter because the core runtime cannot depend on JUnit or third-party XML/reporting libraries.
- The JUnit Platform engine cannot become the source of truth; any behavior that cannot map cleanly to JUnit Platform semantics must still preserve canonical javaspec behavior.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8. Concepts](../arc42/08-concepts.md), and [9. Architecture Decisions](../arc42/09-architecture-decisions.md).
