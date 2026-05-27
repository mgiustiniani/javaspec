# javaspec

javaspec is a Java 8-compatible, zero-runtime-dependency specification framework inspired by phpspec. Its goal is to bring a specification-first workflow to Java while preserving a small runtime footprint and a conservative compatibility baseline.

The Phase 2 first MVP is implemented. It provides a Maven-based CLI entry point, `org.javaspec.cli.Main`, with a PHPSpec-style split: `describe` creates only a generic `ObjectBehavior<T>` specification skeleton, while `run` discovers specs and can generate missing class-like production type skeletons after confirmation or `--generate`. The binary remains Java 8-compatible; post-Java 8 forms such as records and sealed types are generated as source text only. Specs can declare `shouldExtend(...)`, `shouldImplement(...)`, and sealed `shouldPermit(...)`; missing related types get specs before production skeletons, except permitted implementations of sealed interfaces, which stay in the same production file.

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

Project tests may use external test dependencies, but those dependencies must not leak into runtime packaging. The current MVP uses JUnit in test scope only; the runtime dependency tree contains only the project artifact, aside from the JDK platform.

## First MVP: Build, Test, and CLI Usage

Build and test from the repository root:

```sh
mvn test
mvn package
mvn dependency:tree -Dscope=runtime
```

The Maven compiler configuration targets Java 8 (`source`/`target` 1.8). The packaged runtime has no third-party dependencies.

After packaging, run the CLI with the jar, or substitute an installed `javaspec` launcher when one exists:

```sh
java -jar target/javaspec-0.1.0-SNAPSHOT.jar --help
java -jar target/javaspec-0.1.0-SNAPSHOT.jar describe <ClassName> [--spec-dir <dir>]
java -jar target/javaspec-0.1.0-SNAPSHOT.jar desc <ClassName> [--spec-root <dir>]
java -jar target/javaspec-0.1.0-SNAPSHOT.jar run [--spec-dir <dir>] [--source-dir <dir>] [--generate]
```

`--spec-dir`/`--spec-root` default to `src/test/java`. `--source-dir`/`--source-root` default to `src/main/java` and are used by `run`.

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
```

Exit codes: `0` for success/help/generated-or-existing targets, `1` when missing production types are not generated after a prompt is declined or unavailable, `64` for invalid arguments, and `70` for I/O errors.

## Java LTS Targeting Concept

javaspec will run as a Java 8-compatible binary while understanding target profiles:

| LTS version | Profile key | Runtime strategy |
|---|---|---|
| Java 8 | `java8` | Direct use of Java 8 public APIs is allowed. |
| Java 11 | `java11` | APIs introduced in 9-11 are stored as metadata and reflected only when running on a compatible JDK. |
| Java 17 | `java17` | APIs introduced in 12-17 are metadata-driven; records and sealed types are modeled without compile-time linkage. |
| Java 21 | `java21` | sequenced collection APIs are modeled by names and reflected conditionally. |
| Java 25 | `java25` | inherits known profiles; additional public data-structure APIs are not assumed until verified against JDK 25 API docs. |

The initial Java data-structure catalog is documented in [`docs/research/java-lts-data-structures.md`](docs/research/java-lts-data-structures.md).

## Future Usage Vision

The implemented first MVP covers `describe`/`desc` for PHPSpec-style spec skeleton generation and a minimal `run` that maps discovered `spec.*.*Spec.java` files to described production classes, interfaces, enums, annotations, records, sealed classes, and sealed interfaces. Later phases are planned to add the full phpspec-inspired workflow, including examples, expectations, doubles, suites, formatters, and profile-aware runs. Planned command shapes include:

```text
javaspec run
javaspec run --profile java21
javaspec run --format pretty
javaspec run --suite core --stop-on-failure
```

The broader intended workflow remains:

1. Describe a Java type, generating or locating a matching specification class.
2. Run specs.
3. Let the run phase ask whether to generate missing production code, or answer yes non-interactively with `--generate`.
4. Run examples expressed with javaspec expectations.
5. Use collaborators/doubles where possible without runtime dependencies.
6. Report failures with actionable snippets and stable exit codes.

## Documentation Map

- [`docs/usermanual/Home.md`](docs/usermanual/Home.md) — user manual with CLI examples.
- [`PLAN.md`](PLAN.md) — phased implementation plan and requirement traceability.
- [`docs/arc42/01-introduction-and-goals.md`](docs/arc42/01-introduction-and-goals.md) — goals and quality requirements.
- [`docs/arc42/02-constraints.md`](docs/arc42/02-constraints.md) — technical and organizational constraints.
- [`docs/arc42/03-context-and-scope.md`](docs/arc42/03-context-and-scope.md) — system context and boundaries.
- [`docs/arc42/04-solution-strategy.md`](docs/arc42/04-solution-strategy.md) — initial architecture strategy.
- [`docs/adr/0001-java-8-baseline-with-lts-target-profiles.md`](docs/adr/0001-java-8-baseline-with-lts-target-profiles.md) — Java compatibility decision.
- [`docs/adr/0002-zero-runtime-dependency-policy.md`](docs/adr/0002-zero-runtime-dependency-policy.md) — dependency policy decision.
- [`docs/adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md`](docs/adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md) — first-MVP PHPSpec-style describe/run generator split.
- [`docs/research/phpspec-feature-inventory.md`](docs/research/phpspec-feature-inventory.md) — phpspec feature inventory for the Java port.
