# 0011 — Optional JUnit Adapter and Canonical javaspec Runner

## Status

Accepted

## Context

javaspec is intended to become usable as a practical test runner in other projects while preserving the Java 8 baseline and the zero-runtime-dependency policy from ADR 0002. The current CLI can discover and execute examples only when compiled spec classes are available on the effective classloader, and Phase 11 added dependency-free JSON reports and public formatter contracts.

Users need better project integration for local development and CI: a no-JUnit invocation path, build-tool classpath handling, CI-friendly reports, Maven and Gradle integration, and eventually IDE/CI integration through the JUnit Platform. At the same time, JUnit must not become required for core javaspec usage.

## Decision

The javaspec core runner remains canonical. CLI, Maven, Gradle, and JUnit Platform entry points are adapters over the canonical javaspec discovery, runner, result, formatter, and report model; they must not replace the core runner semantics.

No-JUnit execution remains first-class. The next integration foundation should provide an embeddable invocation API that does not call `System.exit`, accepts classpath input from callers or CLI options such as `--classpath` and `--classpath-file` or an equivalent mechanism, writes CI-friendly reports such as JUnit XML without depending on JUnit, and preserves stable exit/report behavior for command-line use.

Maven and Gradle integrations are future optional adapter artifacts. They may depend on their build-tool APIs in their own artifacts, but they must invoke the canonical javaspec runner and must not require JUnit in user projects.

A JUnit Platform engine, if implemented, is a separate optional module. It may depend on the JUnit Platform APIs in that module only. It is an IDE/CI integration path over javaspec specs and must not require changes to the existing javaspec spec style. The core runtime artifact must not gain a JUnit dependency.

## Consequences

Positive consequences:

- Users can adopt javaspec without adding JUnit to their runtime or test execution path.
- Build-tool plugins and IDE integrations share one canonical result model instead of diverging from the core runner.
- CI can use no-JUnit execution first, with dependency-free reports and stable exit behavior.
- A future JUnit Platform engine can improve IDE and CI compatibility without changing core semantics or spec style.

Negative consequences and limitations:

- Practical project integration requires additional adapter artifacts and compatibility testing across CLI, Maven, Gradle, and optional JUnit Platform modes.
- The core invocation API must be stable enough for adapters, which increases compatibility pressure on runner results, test identifiers, and report schemas.
- JUnit XML must be generated internally or through a non-core optional adapter because the core runtime cannot depend on JUnit or third-party XML/reporting libraries.
- The JUnit Platform engine cannot become the source of truth; any behavior that cannot map cleanly to JUnit Platform semantics must still preserve canonical javaspec behavior.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8. Concepts](../arc42/08-concepts.md), and [9. Architecture Decisions](../arc42/09-architecture-decisions.md).
