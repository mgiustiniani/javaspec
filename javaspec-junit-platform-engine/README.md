# javaspec JUnit Platform Engine

Standalone optional JUnit Platform `TestEngine` adapter for javaspec.

This artifact is intentionally not registered as a root Maven module and remains outside the zero-runtime-dependency core artifact. Projects that do not opt into this engine still have no JUnit dependency and can keep CLI, programmatic, Maven plugin, or Gradle plugin no-JUnit execution paths.

## Artifact

- Coordinates: `io.github.jvmspec:javaspec-junit-platform-engine:1.0.0-SNAPSHOT`
- Packaging: `jar`
- Java source/target: `1.8`
- JUnit Platform baseline: `1.10.2` (not JUnit Platform 6/JUnit 6)
- Engine id: `javaspec`

Runtime dependencies are isolated to this optional engine artifact: core `io.github.jvmspec:javaspec`, `org.junit.platform:junit-platform-engine`, and transitives `opentest4j`, `junit-platform-commons`, and `apiguardian-api`. Test-only dependencies include JUnit Platform Launcher, JUnit Platform TestKit, and JUnit Jupiter.

## Local build and verification

Install the current core first, then verify the standalone engine:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
```

Phase 18 verification passed standalone engine `verify` with 12 tests after refreshing the local core snapshot.

Optional runtime dependency audit:

```sh
mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
```

## Usage

Place this artifact on the JUnit Platform test runtime classpath used by the selected IDE/CI/build launcher. The engine is discovered through `META-INF/services/org.junit.platform.engine.TestEngine`, which registers `io.github.jvmspec.junit.platform.JavaspecTestEngine`.

Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`. Supported configuration parameters:

- `javaspec.configFile`
- `javaspec.suite`
- `javaspec.specDir` / `javaspec.specRoot`
- `javaspec.classFilters` / `javaspec.classFilter` / `javaspec.class`
- `javaspec.exampleFilters` / `javaspec.exampleFilter` / `javaspec.example`
- `javaspec.stopOnFailure`

Class, package, method, and unique-id selectors are supported as filters over canonical discovery results. UniqueId segments use `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and `[example:<methodName>]`; Phase 18 retains this stable shape and MethodSource behavior while aligning descriptor reporting to stable ids.

Execution delegates to canonical no-JUnit `JavaspecLauncher`, avoids `System.exit`, and does not require changes to javaspec spec authoring style. Result mapping is: passed -> successful, failed assertion results -> failed assertion-style throwable, broken results -> failed/error-style throwable, and skipped/non-loadable/pending -> skipped.

The engine relies on the JUnit Platform test runtime classpath supplied by the IDE, CI job, or build launcher. It does not compile source/spec files itself; ensure compiled spec classes, production classes, and dependencies are present when source-discovered specs are reported as skipped or non-loadable.
