# javaspec

javaspec is a Java 8-compatible, zero-runtime-dependency specification framework inspired by phpspec. Its goal is to bring a specification-first workflow to Java while preserving a small runtime footprint and a conservative compatibility baseline.

Phases 2 through 9 are implemented. Phase 2 provides a Maven-based CLI entry point, `org.javaspec.cli.Main`, with a PHPSpec-style split: `describe` creates specification/support skeletons, while `run` discovers specs and can generate missing class-like production type skeletons after confirmation or `--generate`. Phase 3 adds Java LTS target profiles `java8`, `java11`, `java17`, `java21`, and `java25`, the profile catalog, API-symbol metadata, compatibility checks, and reflective API availability probes. Phase 4 adds the zero-runtime-dependency line-based configuration model, `--config <file>` and `--suite <name>` integration, suite-level spec/source directories, package-prefix-driven naming conventions, suite selection for `describe` and `run`, and class/example filters for `run`. The Phase 5/6 MVP keeps the existing `run` discovery/generation/update behavior and then executes discovered examples when the compiled spec classes are available on the effective classloader. Phase 7 expands `Matchable`, `ObjectBehavior` convenience assertions, and matcher-name discovery for the documented zero-dependency matcher subset. Phase 8 adds zero-runtime-dependency interface doubles under `org.javaspec.doubles` using JDK dynamic proxies. Phase 9 expands `javaspec run` with run-only controls: `--dry-run`, `--stop-on-failure`, `--formatter <progress|pretty>`, `--profile <java8|java11|java17|java21|java25>`, and `--verbose`. Dry runs never write files or prompt and report pending would-generate/would-update actions; stop-on-failure stops after the first FAILED or BROKEN executable example; formatter/profile CLI options override valid config/default selections; verbose output reports the selected suite, paths, naming prefixes, constructor policy, profile, formatter, dry-run, and stop-on-failure settings. Known limitations: selected profiles are validated but not deeply enforced during execution yet; generic `Iterable` count/empty checks consume the iterable and can hang on infinite iterables; doubles are interface-only and do not support concrete class, final class, static, constructor, primitive, array, annotation, or enum doubles, wildcard matchers, exception/callback stubbing, bytecode libraries, or invocation of default interface methods. The binary remains Java 8-compatible; post-Java 8 forms such as records, sealed types, sequenced collections, and stream gatherers are modeled as source text or metadata/reflection only. Specs can declare `shouldExtend(...)`, `shouldImplement(...)`, and sealed `shouldPermit(...)`; missing related types get specs before production skeletons, except permitted implementations of sealed interfaces, which stay in the same production file.

## Project Goals

- Provide a Java port inspired by phpspec concepts: describing classes, discovering specifications, running examples, expectations, doubles, generation prompts, and extensibility.
- Compile and run on Java 8.
- Keep the runtime artifact free of third-party dependencies.
- Allow test-scope dependencies for the project test suite only.
- Model target Java LTS profiles for Java 8 and later LTS releases available as of 2026-05-27: 8, 11, 17, 21, and 25.
- Represent post-Java 8 APIs through metadata, strings, or reflection so the Java 8-compatible binary never directly links against APIs that do not exist on Java 8.

## Architectural Constraints

1. **Java 8 baseline**: all production code must compile with Java 8 source and target compatibility.
2. **No runtime dependencies**: the main artifact must depend only on the Java 8 standard library.
3. **Test-scope dependencies only**: external libraries may be used for tests, compatibility verification, or build-time quality checks when scoped outside the runtime artifact.
4. **LTS profile metadata**: Java 11, 17, 21, and 25 capabilities must be modeled as target profiles rather than as direct compile-time references.
5. **Package base**: production code uses the package base `org.javaspec`.
6. **Maven implementation**: the project uses Maven while preserving Java 8 bytecode compatibility.

## Architecture Principles

- **Compatibility first**: Java 8 compatibility is a release gate, not an optional mode.
- **Metadata over linkage**: newer JDK APIs are named and discovered through profile metadata and reflection, not imported directly by Java 8-compiled production code.
- **Small core**: the core runtime should contain only the specification model, runner, matcher contracts, profile catalog, and minimal utilities required to execute specs.
- **Explicit boundaries**: CLI, configuration, discovery, runner, expectations, doubles, generators, and extension points should have separate responsibilities.
- **Deterministic behavior**: spec discovery, execution order, output, and exit codes should be predictable for local and CI use.
- **Extensibility without dependency cost**: extension hooks should be exposed through Java interfaces and JDK mechanisms such as reflection or `ServiceLoader` where appropriate.

## Zero-Dependency Policy

The runtime artifact must not require libraries such as YAML parsers, bytecode manipulation libraries, assertion libraries, logging frameworks, or dependency injection containers. If a capability normally depends on such libraries, javaspec should either:

- implement a small internal equivalent using Java 8 APIs,
- expose an extension point so users can integrate optional tools outside the core runtime, or
- defer the feature until it can be implemented without violating the policy.

Project tests may use external test dependencies, but those dependencies must not leak into runtime packaging. The current MVP uses JUnit in test scope only; Phase 8 doubles use JDK dynamic proxies rather than bytecode libraries. The runtime dependency tree contains only the project artifact, aside from the JDK platform.

## Profile Catalog and Java 8 Compatibility Strategy

The implemented profile/catalog model lives in `org.javaspec.profile`. `TargetProfile`, `FeatureFlag`, `ApiSymbol`, `ApiSymbolKey`, `ApiSymbolKind`, `ApiSymbolCategory`, and `ProfileCatalog` provide deterministic metadata for Java 8, 11, 17, 21, and 25. The default catalog covers representative data-structure APIs from [`docs/research/java-lts-data-structures.md`](docs/research/java-lts-data-structures.md), including Java 11 collection factories, Java 17 stream/record/sealed metadata, Java 21 sequenced collections, and Java 25 stream gatherers.

Compatibility checks live behind `org.javaspec.compatibility`. `ProfileCompatibilityCheck` evaluates whether type kinds, feature flags, or API symbols fit a target profile, while `ApiAvailabilityProbe` uses class, method, and field names to probe optional APIs reflectively. Production code does not import Java 9+ APIs directly, so the runtime artifact can remain Java 8-compatible while understanding newer LTS capabilities.

## First MVP: Build, Test, and CLI Usage

Build and test from the repository root:

```sh
mvn verify
mvn dependency:tree -Dscope=runtime
```

Latest verification after the completed Phase 9 CLI expansion: `mvn verify` passed with 338 tests, and `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

The Maven compiler configuration targets Java 8 (`source`/`target` 1.8). The packaged runtime has no third-party dependencies.

After packaging, run the CLI with the jar, or substitute an installed `javaspec` launcher when one exists:

```sh
java -jar target/javaspec-0.1.0-SNAPSHOT.jar --help
java -jar target/javaspec-0.1.0-SNAPSHOT.jar describe <ClassName> [--config <file>] [--suite <name>] [--spec-dir <dir>]
java -jar target/javaspec-0.1.0-SNAPSHOT.jar desc <ClassName> [--config <file>] [--suite <name>] [--spec-root <dir>]
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run [--config <file>] [--suite <name>] [--spec-dir <dir>] [--source-dir <dir>] [--generate] [--dry-run] [--stop-on-failure] [--formatter <progress|pretty>] [--profile <java8|java11|java17|java21|java25>] [--verbose] [--constructor-policy <delete|preserve|comment>] [--class <name>] [--example <name>]
```

Without `--config`, javaspec infers defaults: suite `default`, spec root `src/test/java`, source root `src/main/java`, spec package prefix `spec`, production package prefix empty, profile `java8`, formatter `progress`, constructor policy `comment`, and empty bootstrap hooks. `--config <file>` loads a restricted line-based configuration file, and `--suite <name>` selects a configured suite. Selected-suite paths and package prefixes drive spec/source naming unless overridden by `--spec-dir`/`--spec-root` or `--source-dir`/`--source-root`. `run` uses the configured constructor policy, profile, and formatter unless CLI options override them; repeatable `--class <name>` and `--example <name>` filters limit discovery and execution by described/spec class and example method/display name/order index. Run-only options such as `--generate`, `--dry-run`, `--stop-on-failure`, `--formatter`, `--profile`, `--verbose`, `--constructor-policy`, `--class`, and `--example` are rejected for `describe`.

After discovery, generation, and source updates complete without declined prompts, `run` invokes the reflection runner for discovered examples. PASSED examples complete normally; AssertionError is FAILED; non-assertion throwables, lifecycle errors, instantiation errors, and reflection errors are BROKEN; non-loadable spec classes and missing reflected example methods are SKIPPED. By default every discovered example metadata entry is processed; `--stop-on-failure` stops after the first FAILED or BROKEN executable example. `--formatter progress` prints concise summary-oriented output, while `--formatter pretty` prints per-example status lines plus details. `--profile` selects and validates one of the configured LTS profile keys but does not yet perform deep profile enforcement. The CLI runner does not compile source or spec files itself, so source-only or otherwise unavailable spec classes are discovered but skipped rather than executed.

`run --dry-run` performs discovery and planning without writes and without prompts. It reports would-generate/would-update actions for related specs/support, support updates, constructor changes, method skeletons, and missing production type generation. Dry-run exits `1` when pending generation/update work exists; when no pending changes exist, executable examples are handled normally and passed or skipped-only runs exit `0`.

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

# Phase 9 run controls are run-only.
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --dry-run
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run --stop-on-failure --formatter pretty --profile java17 --verbose
```

Exit codes: `0` for success/help/generated-or-existing targets, successful dry-runs with no pending generation/update work, and successful or skipped-only executable runs; `1` when missing production types or method updates are not generated after a prompt is declined or unavailable, when dry-run finds pending generation/update work, or when executable examples fail or break; `64` for invalid arguments; and `70` for I/O errors.

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

The implemented MVP covers `describe`/`desc` for PHPSpec-style spec/support skeleton generation, a `run` command that maps discovered `*Spec.java` files under the active naming convention to described production classes, interfaces, enums, annotations, records, sealed classes, and sealed interfaces, Phase 4 configuration files for defaults, suite path selection, package-prefix naming, class/example filters, selected profile/formatter defaults, bootstrap metadata, and constructor-policy defaults, the Phase 5/6 reflection runner for executable examples that are already compiled and available on the effective classloader, the Phase 7 matcher/expectation expansion, Phase 8 interface doubles, and Phase 9 run controls for dry-run planning, stop-on-failure, progress/pretty formatting, profile selection, and verbose diagnostics. Later phases are planned to expand the phpspec-inspired workflow with bootstrap execution, deeper profile-aware enforcement, pending examples, optional reports, advanced double integrations, and extension behavior.

The stabilized discovery and execution flow honors configured suite package prefixes through `SpecNamingConvention`, maps described classes to generated specs/support files with those prefixes, selects suites with `--suite`, filters classes with repeatable `--class <qualified-or-simple-name>`, filters examples with repeatable `--example <method-name|display-name|order-index>`, and applies `run`-only controls before generation and reflection execution. The broader intended workflow remains:

1. Describe a Java type, generating or locating a matching specification class.
2. Run specs.
3. Let the run phase ask whether to generate missing production code, or answer yes non-interactively with `--generate`.
4. Run examples expressed with javaspec expectations.
5. Use interface collaborators/doubles where possible without runtime dependencies.
6. Report failures with actionable snippets and stable exit codes.

## Documentation Map

- [`docs/usermanual/Home.md`](docs/usermanual/Home.md) — user manual with CLI examples.
- [`PLAN.md`](PLAN.md) — phased implementation plan and requirement traceability.
- [`docs/arc42/01-introduction-and-goals.md`](docs/arc42/01-introduction-and-goals.md) — goals and quality requirements.
- [`docs/arc42/02-constraints.md`](docs/arc42/02-constraints.md) — technical and organizational constraints.
- [`docs/arc42/03-context-and-scope.md`](docs/arc42/03-context-and-scope.md) — system context and boundaries.
- [`docs/arc42/04-solution-strategy.md`](docs/arc42/04-solution-strategy.md) — initial architecture strategy.
- [`docs/arc42/05-building-block-view.md`](docs/arc42/05-building-block-view.md) — building-block notes, including the CLI run controls, configuration model, profile catalog, and compatibility boundary.
- [`docs/adr/0001-java-8-baseline-with-lts-target-profiles.md`](docs/adr/0001-java-8-baseline-with-lts-target-profiles.md) — Java compatibility decision.
- [`docs/adr/0002-zero-runtime-dependency-policy.md`](docs/adr/0002-zero-runtime-dependency-policy.md) — dependency policy decision.
- [`docs/adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md`](docs/adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md) — first-MVP PHPSpec-style describe/run generator split.
- [`docs/adr/0004-course-correction-construction-defaults-typed-matcher-proxies-and-method-generators.md`](docs/adr/0004-course-correction-construction-defaults-typed-matcher-proxies-and-method-generators.md) — implemented construction, typed matcher proxy, and method-generator correction.
- [`docs/adr/0005-restricted-line-based-configuration-format.md`](docs/adr/0005-restricted-line-based-configuration-format.md) — restricted zero-dependency configuration format decision.
- [`docs/adr/0006-classpath-reflection-runner.md`](docs/adr/0006-classpath-reflection-runner.md) — classpath reflection runner decision for the Phase 5/6 MVP.
- [`docs/adr/0007-jdk-proxy-only-interface-doubles.md`](docs/adr/0007-jdk-proxy-only-interface-doubles.md) — JDK proxy-only interface doubles decision for the Phase 8 MVP.
- [`docs/research/phpspec-feature-inventory.md`](docs/research/phpspec-feature-inventory.md) — phpspec feature inventory for the Java port.
