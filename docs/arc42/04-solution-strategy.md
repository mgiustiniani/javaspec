# 4. Solution Strategy

## 4.1 Strategy Overview

javaspec should be built as a small Java 8-compatible core with clear extension boundaries. The core will provide the specification model, execution model, matcher contracts, configuration defaults, profile metadata, and CLI behavior needed for a phpspec-inspired workflow.

## 4.2 Key Architectural Decisions

| Decision | ADR |
|---|---|
| Java 8 baseline with metadata-driven Java LTS target profiles | [ADR 0001](../adr/0001-java-8-baseline-with-lts-target-profiles.md) |
| Zero runtime dependency policy | [ADR 0002](../adr/0002-zero-runtime-dependency-policy.md) |
| First-MVP PHPSpec-style describe/run generator flow | [ADR 0003](../adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md) |

## 4.3 Core Strategy

1. **Java 8-compatible production code**  
   Production code uses only Java 8 language and library APIs. Newer JDK APIs are never imported directly.

2. **Profile catalog**  
   A metadata catalog describes Java LTS profiles (`java8`, `java11`, `java17`, `java21`, `java25`) and the public API symbols relevant to javaspec.

3. **Reflective compatibility boundary**  
   Runtime probing of newer APIs is isolated in a small compatibility layer that accepts class and method names as strings.

4. **phpspec-inspired model adapted to Java**  
   Concepts such as `describe`, examples, subject lifecycle, matchers, doubles, generators, formatters, and extensions are retained where useful but adapted to Java packages, classes, methods, constructors, and static typing.

5. **No dependency leakage**  
   Runtime packaging is audited so third-party libraries are absent. Optional integrations must live outside the core or in test/build scopes.

6. **First-MVP PHPSpec-style describe/run slice**  
   The initial implementation keeps `describe`/`desc` focused on PHPSpec-style specification skeleton creation. The minimal `run` command discovers `spec.*.*Spec.java` files, infers described production class-like types, reports missing types with target paths, asks whether to create them (`Y/n`), and writes Java 8-compatible class/interface/enum/annotation skeletons only after confirmation or when `run --generate` is supplied.

## 4.4 Building Blocks

The first MVP implements a narrow slice of the planned architecture:

- **CLI adapter**: parses `describe`/`desc`, `run`, source/spec-root aliases, `--generate`, help, and first-MVP exit codes without external libraries.
- **Described-class model**: validates Java class names and maps them to source-relative paths.
- **Spec discovery model**: maps `spec.*.*Spec.java` files to described production class-like types deterministically, recognizes kind markers such as `shouldBeAnInterface()`, parses relationship markers such as `shouldExtend(...)` and `shouldImplement(...)`, and parses sealed permitted subtype markers such as `shouldPermit(Circle.class)`.
- **Discovery check**: detects source-root and classpath presence for a described class.
- **Object behavior base**: provides the minimal generic `ObjectBehavior<T>` type used by generated specs.
- **Spec skeleton generator**: plans and writes a minimal Java 8-compatible PHPSpec-style spec skeleton from `describe`.
- **Type skeleton generator**: plans and writes minimal production class/interface/enum/annotation/record/sealed skeletons, including `extends`, `implements`, and `permits` clauses, after an interactive `run` confirmation or non-interactive `run --generate`; post-Java 8 forms are emitted only as source text, and permitted implementations of sealed interfaces are kept in the same production file.

Later building blocks remain planned:

- **Configuration model**: loads defaults and explicit suite/profile/formatter settings.
- **Full spec discovery model**: maps packages/classes to specs, examples, locators, and filters.
- **Runner**: executes suites, specs, examples, and lifecycle hooks.
- **Expectation and matcher engine**: evaluates outcomes and produces diagnostics.
- **Doubles engine**: provides zero-dependency interface doubles and call predictions where feasible.
- **Advanced generator**: creates richer spec/source skeletons and missing-method snippets beyond the first-MVP skeletons.
- **Formatter/reporting layer**: emits progress, pretty, and optional machine-readable reports.
- **LTS profile catalog**: stores JDK profile metadata.
- **Extension API**: registers custom matchers, formatters, generators, and lifecycle hooks.

## 4.5 Risk Reduction Strategy

- Validate Java 8 bytecode and runtime behavior early.
- Add a runtime dependency audit as a build gate.
- Build the profile catalog from explicit research and verify Java 25 metadata before enabling assumptions.
- Implement doubles incrementally, starting with interface proxies because they are supported by Java 8 JDK APIs.
- Treat advanced generation and external integrations as later or extension features.
