# javaspec 1.0 API/SPI surface

This document classifies the Java packages and external contracts that are visible in the repository before the 1.0 API freeze. Public Java visibility alone does **not** make a type part of the supported 1.0 contract; only packages marked `PUBLIC_API` or `PUBLIC_SPI` below receive SemVer compatibility guarantees.

Compatibility policy:

- `PUBLIC_API`: supported for normal users. Breaking source/binary changes require a major version.
- `PUBLIC_SPI`: supported for integrations/extensions. Breaking changes require a major version unless the SPI is explicitly marked experimental in this document.
- `ADAPTER_API`: supported external adapter entrypoints and configuration surfaces. Compatibility is scoped to the adapter artifact.
- `GENERATED_API`: source shapes generated for user projects. Changes must preserve the PHPSpec-first authoring model and be documented in release notes.
- `INTERNAL`: implementation detail. It may be public for Java/module/tooling reasons but is not a supported compatibility surface.

## Core artifact: `io.github.jvmspec:javaspec`

| Package | Classification | 1.0 contract |
|---|---|---|
| `io.github.jvmspec.api` | `PUBLIC_API` | PHPSpec-first authoring API: `ObjectBehavior`, lifecycle helpers, construction helpers, skip/pending signals, direct expectation conveniences, example data rows, and generated-support base hooks. Example data semantics are frozen in `docs/example-data-contract-1.0.md`. |
| `io.github.jvmspec.matcher` | `PUBLIC_API` | `Matchable`, built-in matcher result/registry types, and custom matcher registration used by specs; see `docs/matcher-contract-1.0.md`. |
| `io.github.jvmspec.doubles` | `PUBLIC_API` / `PUBLIC_SPI` | User-facing interface double API is public API. `ConcreteDoubleProvider` is public SPI for optional concrete-double adapters. |
| `io.github.jvmspec.doubles.prophecy` | `PUBLIC_API` | Prophecy-style collaborator API: `ObjectProphecy`, generated-wrapper target types, argument tokens, promises, predictions, callbacks, and prediction context. |
| `io.github.jvmspec.runner` | `PUBLIC_API` | Programmatic result model and runner entrypoint: `SpecRunner`, `RunResult`, `SpecResult`, `ExampleResult`, `ExampleStatus`, `FailureDetail`. |
| `io.github.jvmspec.invocation` | `PUBLIC_API` | Programmatic launcher and exit-code mapping for embedding javaspec. |
| `io.github.jvmspec.reporting` | `PUBLIC_API` | JSON and JUnit XML report writers plus report metadata; schemas evolve additively within schemaVersion 1. |
| `io.github.jvmspec.bootstrap` | `PUBLIC_SPI` | Bootstrap hook SPI and context for project setup before execution; see `docs/extension-spi-1.0.md`. |
| `io.github.jvmspec.extension` | `PUBLIC_SPI` | ServiceLoader/configured extension SPI and extension activation context; see `docs/extension-spi-1.0.md`. |
| `io.github.jvmspec.formatter` | `PUBLIC_SPI` | Formatter SPI and built-in formatter registry; see `docs/extension-spi-1.0.md`. |
| `io.github.jvmspec.resolver` | `PUBLIC_SPI` | Dependency resolver SPI for optional build-tool integration; see `docs/extension-spi-1.0.md`. |
| `io.github.jvmspec.generation.parser` | `PUBLIC_SPI` | Source parser SPI used by generation; parser implementations shipped in core are not guaranteed as extension points unless named here. See `docs/extension-spi-1.0.md`. |
| `io.github.jvmspec.discovery` | `PUBLIC_API` | Discovery model and naming convention used by adapters. Internal parsing heuristics remain implementation details. |
| `io.github.jvmspec.model` | `PUBLIC_API` | Immutable descriptors for described production types, methods, constructors, and Java type kinds. |
| `io.github.jvmspec.naming` | `PUBLIC_API` | Backward-compatible public naming facade. |
| `io.github.jvmspec.generation` | `GENERATED_API` / `INTERNAL` | Generated source shapes and `// javaspec:stub` marker semantics are part of the workflow contract. Low-level generator classes are internal unless referenced by public docs/tests. |
| `io.github.jvmspec.compatibility` | `PUBLIC_API` | Profile compatibility result model used by CLI and adapters. |
| `io.github.jvmspec.profile` | `PUBLIC_API` | Target profile model and feature/API symbol metadata. |
| `io.github.jvmspec.config` | `PUBLIC_API` | Configuration model and parser for programmatic consumers. |
| `io.github.jvmspec.compilation` | `INTERNAL` | CLI compilation implementation detail. |
| `io.github.jvmspec.diagnostics` | `PUBLIC_API` | Diagnostic model surfaced by CLI and adapters. |
| `io.github.jvmspec.cli` | `ADAPTER_API` | Command-line entrypoint/options/exit codes are public CLI contract; individual parser/handler classes are internal Java API. |
| `io.github.jvmspec.cli.run` | `INTERNAL` | Run-command orchestration internals. |

## Optional artifacts

| Package | Artifact | Classification | 1.0 contract |
|---|---|---|---|
| `io.github.jvmspec.maven` | `javaspec-maven-plugin` | `ADAPTER_API` | Mojo goals, documented parameters, generated-sources registration, report paths, and failure propagation. Java classes are plugin implementation details. |
| `io.github.jvmspec.gradle` | Gradle plugin `io.github.jvmspec` | `ADAPTER_API` | Plugin id, extension/task properties, source-set integration, report paths, and failure propagation. Task/extension Java members are Gradle DSL surface. |
| `io.github.jvmspec.junit.platform` | `javaspec-junit-platform-engine` | `ADAPTER_API` | Engine id, descriptor hierarchy, selectors, unique-id behavior, source mapping, and status translation; see `docs/junit-platform-contract-1.0.md`. |
| `io.github.jvmspec.doubles` | `javaspec-bytecode-doubles` | `PUBLIC_API` / `PUBLIC_SPI` | Optional ByteBuddy concrete-double provider and capability reporting. |
| `io.github.jvmspec.doubles` | `javaspec-bytecode-agent` | `PUBLIC_API` | Optional final/static/constructor double entrypoints `BytecodeAgentDoubles`, `StaticDouble`, and `ConstructionDouble`. |
| `io.github.jvmspec.doubles.agent` | `javaspec-bytecode-agent` | `INTERNAL` | Bytecode instrumentation internals. |

## External non-Java contracts

| Surface | Classification | Freeze criteria |
|---|---|---|
| CLI command/options | `ADAPTER_API` | Documented options, config precedence, dry-run/generation semantics, filters, profile enforcement, and exit codes require compatibility tests before RC. |
| JSON report schema | `PUBLIC_API` | `schemaVersion: 1` is additive-compatible; breaking schema changes require a new schemaVersion. |
| JUnit XML report | `ADAPTER_API` | JUnit-compatible mapping remains stable for CI consumers. |
| JUnit Platform unique IDs | `ADAPTER_API` | Suite/spec/example/row unique-id shape must remain stable or receive documented migration. |
| Generated `*SpecSupport` source | `GENERATED_API` | Subject proxy, object-state helper, throw proxy, Prophecy helper, and `// javaspec:stub` semantics are part of the PHPSpec workflow contract. |
| Maven parameters | `ADAPTER_API` | Goal names and documented parameters require deprecation before removal. |
| Gradle plugin DSL | `ADAPTER_API` | Plugin id, extension properties, task names/properties require deprecation before removal. |

## 1.0 baseline procedure

Before the first 1.0 RC:

1. Run `scripts/check-api-surface.sh` to ensure every shipped Java package is classified here.
2. Run `scripts/verify-all.sh`.
3. Run `scripts/generate-api-baseline.sh` after all artifact builds complete. It writes the exact
   public/protected JVM signature inventory to `docs/history/api-baseline-1.0.0.md`.
4. Commit that inventory with the RC version cut. Regenerating it without API changes must produce
   no diff.
5. Compare future release candidates against that baseline. Additive public API changes are allowed
   before GA only when documented; removals or incompatible signature changes reset the RC.

## Intentional limits

- Public classes in `INTERNAL` packages are not supported API even when technically accessible.
- Generated code is source-level API, not a promise that low-level generator classes remain public API.
- Optional bytecode adapters may depend on third-party libraries; core remains zero-runtime-dependency.
