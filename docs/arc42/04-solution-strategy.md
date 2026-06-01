# 4. Solution Strategy

## 4.1 Strategy Overview

javaspec is being built as a small Java 8-compatible core with clear extension boundaries. The implemented core already contains the first-MVP CLI/generation slice, profile metadata, compatibility checks/probes, Phase 4 configuration, naming, and discovery-filter integration, the Phase 5/6 MVP reflection runner, and the Phase 7 matcher/expectation expansion; later phases will add richer execution controls, active formatter/bootstrap behavior, doubles, and extension behavior needed for a fuller phpspec-inspired workflow.

## 4.2 Key Architectural Decisions

| Decision | ADR |
|---|---|
| Java 8 baseline with metadata-driven Java LTS target profiles | [ADR 0001](../adr/0001-java-8-baseline-with-lts-target-profiles.md) |
| Zero runtime dependency policy | [ADR 0002](../adr/0002-zero-runtime-dependency-policy.md) |
| First-MVP PHPSpec-style describe/run generator flow | [ADR 0003](../adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md) |
| Construction defaults, typed matcher proxies, and method generators course correction | [ADR 0004](../adr/0004-course-correction-construction-defaults-typed-matcher-proxies-and-method-generators.md) |
| Restricted line-based configuration format | [ADR 0005](../adr/0005-restricted-line-based-configuration-format.md) |
| Classpath reflection runner for executable examples | [ADR 0006](../adr/0006-classpath-reflection-runner.md) |

## 4.3 Core Strategy

1. **Java 8-compatible production code**
   Production code uses only Java 8 language and library APIs. Newer JDK APIs are never imported directly.

2. **Profile catalog**
   The implemented metadata catalog describes Java LTS profiles (`java8`, `java11`, `java17`, `java21`, `java25`), feature flags, and public API symbols relevant to javaspec.

3. **Restricted configuration and naming model**
   The implemented `org.javaspec.config` package loads inferred defaults and explicit line-based configuration without external parser dependencies. The CLI applies selected-suite spec/source paths, package-prefix naming, and configured constructor-policy defaults while keeping bootstrap hooks and profile/formatter behavior as metadata for later runner features.

4. **Classpath reflection runner**
   The implemented `org.javaspec.runner` package executes discovered examples only when compiled specification classes are available on the effective classloader. It reuses `DiscoveredSpec` and `SpecExample` metadata so suite, class, and example filters remain effective. The runner creates a fresh spec instance per example and supports optional public no-arg `let()`/`letGo()` hooks.

5. **Reflective compatibility boundary**
   Runtime probing of optional newer APIs is isolated in the implemented compatibility layer, which accepts class, method, and field names as strings.

6. **phpspec-inspired model adapted to Java**
   Concepts such as `describe`, examples, subject lifecycle, matchers, doubles, generators, formatters, and extensions are retained where useful but adapted to Java packages, classes, methods, constructors, and static typing.

7. **No dependency leakage**
   Runtime packaging is audited so third-party libraries are absent. Optional integrations must live outside the core or in test/build scopes.

8. **First-MVP PHPSpec-style describe/run slice**
   The implemented first-MVP keeps `describe`/`desc` focused on PHPSpec-style specification skeleton creation. The `run` command discovers `spec.*.*Spec.java` files, infers described production class-like types, reports missing types with target paths, asks whether to create them (`Y/n`), writes Java 8-compatible class/interface/enum/annotation plus source-text record/sealed skeletons only after confirmation or when `run --generate` is supplied, and then executes loadable discovered examples through the MVP reflection runner.

## 4.4 Building Blocks

The implemented architecture includes the Phase 2 first-MVP slice, the Phase 3 profile/catalog slice, the Phase 4 configuration slice, the Phase 5/6 MVP reflection-runner slice, and the Phase 7 matcher/expectation expansion. See [ARC42 section 5](05-building-block-view.md) for concise building-block notes.

Implemented building blocks:

- **CLI adapter**: parses `describe`/`desc`, `run`, `--config`, `--suite`, source/spec-root aliases, `--generate`, constructor policy, help, and first-MVP exit codes without external libraries; after discovery/generation/update it prints runner summaries and exits `1` for failed or broken executable examples.
- **Described-class model**: validates Java class names and maps them to source-relative paths.
- **Spec discovery model**: maps default or configured spec-package-prefix `*Spec.java` files to described production class-like types deterministically, recognizes kind markers such as `shouldBeAnInterface()`, parses relationship markers such as `shouldExtend(...)` and `shouldImplement(...)`, parses construction markers, expanded chained typed proxy matcher calls for method-discovery/default-return inference where applicable, throw proxy calls, sealed permitted subtype markers such as `shouldPermit(Circle.class)`, public `it_`/`its_` example metadata, and class/example filters.
- **Discovery check**: detects source-root and classpath presence for a described class.
- **Object behavior base and matcher engine**: provide the generic `ObjectBehavior<T>` type used by generated specs, lazy construction configuration, throw expectations, expanded `Matchable` expectation methods, direct convenience assertions that delegate through `match(actual)`, custom matcher registration, and a zero-dependency default matcher registry including negated equality.
- **Reflection runner**: uses the discovered spec/example metadata to load compiled spec classes from the effective classloader, execute filtered examples with fresh instances and optional `let()`/`letGo()`, and produce PASSED/FAILED/BROKEN/SKIPPED results.
- **Spec/support skeleton generator**: plans and writes Java 8-compatible PHPSpec-style spec and typed support skeletons from `describe` and `run` support updates.
- **Type/method skeleton generator**: plans and writes minimal production class/interface/enum/annotation/record/sealed skeletons, constructor/static-factory changes, and Java 8-compatible method skeletons after an interactive `run` confirmation or non-interactive `run --generate`; post-Java-8 forms are emitted only as source text.
- **Profile catalog**: `org.javaspec.profile` stores deterministic target profiles, feature flags, API symbols, symbol categories/kinds, and representative Java LTS data-structure metadata including Java 25 stream gatherers.
- **Configuration and naming model**: `org.javaspec.config` loads inferred defaults and explicit suite/profile/formatter/path/package-prefix/constructor-policy settings with a restricted line-based parser; `SpecNamingConvention` applies suite package prefixes to describe, discovery, and support generation.
- **Compatibility boundary**: `org.javaspec.compatibility` checks profile compatibility and probes optional APIs reflectively by string name, without direct Java 9+ production imports.

Later building blocks remain planned:

- **Runner expansion**: adds richer suite execution controls, stop-on-failure, pending examples, source locations, bootstrap execution, profile-aware behavior, and formatter integration beyond the MVP reflection runner.
- **Expectation and matcher engine follow-up**: expands beyond the Phase 7 subset with additional PHPSpec parity, diagnostics, approximate equality, richer object-state matchers, and iteration/yield variants where Java-compatible.
- **Doubles engine**: provides zero-dependency interface doubles and call predictions where feasible.
- **Advanced generator**: creates richer spec/source skeletons and missing-method snippets beyond the current generator subset.
- **Formatter/reporting layer**: emits progress, pretty, and optional machine-readable reports.
- **Extension API**: registers custom matchers, formatters, generators, and lifecycle hooks.

## 4.5 Risk Reduction Strategy

- Validate Java 8 bytecode and runtime behavior early.
- Add a runtime dependency audit as a build gate.
- Keep the profile catalog synchronized with explicit research and re-validate Java 25 stream gatherer metadata during quality-matrix work before relying on environment-specific behavior.
- Keep configuration syntax restricted and line-based unless a future zero-runtime-dependency-compatible design supersedes it.
- Implement doubles incrementally, starting with interface proxies because they are supported by Java 8 JDK APIs.
- Treat advanced generation and external integrations as later or extension features.
