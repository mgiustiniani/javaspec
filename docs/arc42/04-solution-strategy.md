# 4. Solution Strategy

## 4.1 Strategy Overview

javaspec is being built as a small Java 8-compatible core with clear extension boundaries. The implemented core already contains the first-MVP CLI/generation slice, profile metadata, compatibility checks/probes, Phase 4 configuration, naming, and discovery-filter integration, the Phase 5/6 MVP reflection runner, the Phase 7 matcher/expectation expansion, Phase 8 JDK-proxy interface doubles, Phase 9 run controls, Phase 10 interface-style method generation, and Phase 11 formatter/reporting/programmatic extension contracts. Later phases may add bootstrap execution, deeper profile enforcement, pending examples, external extension loading, optional advanced doubles, and other behavior needed for a fuller phpspec-inspired workflow.

## 4.2 Key Architectural Decisions

| Decision | ADR |
|---|---|
| Java 8 baseline with metadata-driven Java LTS target profiles | [ADR 0001](../adr/0001-java-8-baseline-with-lts-target-profiles.md) |
| Zero runtime dependency policy | [ADR 0002](../adr/0002-zero-runtime-dependency-policy.md) |
| First-MVP PHPSpec-style describe/run generator flow | [ADR 0003](../adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md) |
| Construction defaults, typed matcher proxies, and method generators course correction | [ADR 0004](../adr/0004-course-correction-construction-defaults-typed-matcher-proxies-and-method-generators.md) |
| Restricted line-based configuration format | [ADR 0005](../adr/0005-restricted-line-based-configuration-format.md) |
| Classpath reflection runner for executable examples | [ADR 0006](../adr/0006-classpath-reflection-runner.md) |
| JDK proxy-only interface doubles | [ADR 0007](../adr/0007-jdk-proxy-only-interface-doubles.md) |
| Run-only controls and dry-run planning | [ADR 0008](../adr/0008-run-only-controls-and-non-mutating-dry-run-planning.md) |
| Interface-style method generation and sealed-interface update deferral | [ADR 0009](../adr/0009-interface-style-method-generation-and-sealed-interface-update-deferral.md) |
| Formatter, reporting, and programmatic extension boundary | [ADR 0010](../adr/0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md) |

## 4.3 Core Strategy

1. **Java 8-compatible production code**
   Production code uses only Java 8 language and library APIs. Newer JDK APIs are never imported directly.

2. **Profile catalog**
   The implemented metadata catalog describes Java LTS profiles (`java8`, `java11`, `java17`, `java21`, `java25`), feature flags, and public API symbols relevant to javaspec.

3. **Restricted configuration and naming model**
   The implemented `org.javaspec.config` package loads inferred defaults and explicit line-based configuration without external parser dependencies. The CLI applies selected-suite spec/source paths, package-prefix naming, configured constructor-policy defaults, and configured profile/formatter defaults. Bootstrap hooks remain metadata; selected profiles are validated/reported but not deeply enforced yet.

4. **Classpath reflection runner**
   The implemented `org.javaspec.runner` package executes discovered examples only when compiled specification classes are available on the effective classloader. It reuses `DiscoveredSpec` and `SpecExample` metadata so suite, class, and example filters remain effective. The runner creates a fresh spec instance per example, supports optional public no-arg `let()`/`letGo()` hooks, and can stop after the first FAILED or BROKEN executable example when `run --stop-on-failure` is selected.

5. **Reflective compatibility boundary**
   Runtime probing of optional newer APIs is isolated in the implemented compatibility layer, which accepts class, method, and field names as strings.

6. **phpspec-inspired model adapted to Java**
   Concepts such as `describe`, examples, subject lifecycle, matchers, doubles, generators, formatters, and extensions are retained where useful but adapted to Java packages, classes, methods, constructors, and static typing.

7. **Interface proxy doubles**
   The implemented doubles boundary uses Java 8 JDK dynamic proxies for ordinary interfaces, records calls, verifies simple call predictions, returns Java defaults for unstubbed methods, and rejects unsupported target kinds with explicit diagnostics.

8. **No dependency leakage**
   Runtime packaging is audited so third-party libraries are absent. Optional integrations must live outside the core or in test/build scopes.

9. **First-MVP PHPSpec-style describe/run slice**
   The implemented first-MVP keeps `describe`/`desc` focused on PHPSpec-style specification skeleton creation. The `run` command discovers `spec.*.*Spec.java` files, infers described production class-like types, reports missing types with target paths, asks whether to create them (`Y/n`), writes Java 8-compatible class/interface/enum/annotation plus source-text record/sealed skeletons only after confirmation or when `run --generate` is supplied, and then executes loadable discovered examples through the MVP reflection runner.

## 4.4 Building Blocks

The implemented architecture includes the Phase 2 first-MVP slice, the Phase 3 profile/catalog slice, the Phase 4 configuration slice, the Phase 5/6 MVP reflection-runner slice, the Phase 7 matcher/expectation expansion, the Phase 8 interface-doubles slice, the Phase 9 CLI expansion, the Phase 10 interface-style method generation increment, and the Phase 11 formatter/reporting/extension increment. See [ARC42 section 5](05-building-block-view.md) for concise building-block notes.

Implemented building blocks:

- **CLI adapter**: parses `describe`/`desc`, `run`, `--config`, `--suite`, source/spec-root aliases, `--generate`, `--dry-run`, `--stop-on-failure`, `--formatter <progress|pretty>`, `--profile <java8|java11|java17|java21|java25>`, `--verbose`, `--report`/`--report-file`, constructor policy, filters, help, and exit codes without external libraries; after discovery/generation/update it renders selected progress/pretty output, writes optional JSON reports, and exits `1` for failed or broken executable examples or pending dry-run work.
- **Described-class model**: validates Java class names and maps them to source-relative paths.
- **Spec discovery model**: maps default or configured spec-package-prefix `*Spec.java` files to described production class-like types deterministically, recognizes kind markers such as `shouldBeAnInterface()`, parses relationship markers such as `shouldExtend(...)` and `shouldImplement(...)`, parses construction markers, expanded chained typed proxy matcher calls for method-discovery/default-return inference where applicable, throw proxy calls, sealed permitted subtype markers such as `shouldPermit(Circle.class)`, public `it_`/`its_` example metadata, and class/example filters.
- **Discovery check**: detects source-root and classpath presence for a described class.
- **Object behavior base and matcher engine**: provide the generic `ObjectBehavior<T>` type used by generated specs, lazy construction configuration, throw expectations, expanded `Matchable` expectation methods, direct convenience assertions that delegate through `match(actual)`, custom matcher registration, `ObjectBehavior` double conveniences, and a zero-dependency default matcher registry including negated equality.
- **Reflection runner**: uses the discovered spec/example metadata to load compiled spec classes from the effective classloader, execute filtered examples with fresh instances and optional `let()`/`letGo()`, produce PASSED/FAILED/BROKEN/SKIPPED results, and stop after the first failed/broken executable example when requested.
- **Spec/support skeleton generator**: plans and writes Java 8-compatible PHPSpec-style spec and typed support skeletons from `describe` and `run` support updates.
- **Type/method skeleton generator**: plans and writes minimal production class/interface/enum/annotation/record/sealed skeletons, constructor/static-factory changes, Java 8-compatible method bodies, ordinary-interface declarations, compatible annotation elements, and missing sealed-interface skeleton declarations plus nested permitted implementation bodies after an interactive `run` confirmation or non-interactive `run --generate`; post-Java-8 forms are emitted only as source text.
- **Profile catalog**: `org.javaspec.profile` stores deterministic target profiles, feature flags, API symbols, symbol categories/kinds, and representative Java LTS data-structure metadata including Java 25 stream gatherers.
- **Configuration and naming model**: `org.javaspec.config` loads inferred defaults and explicit suite/profile/formatter/path/package-prefix/constructor-policy settings with a restricted line-based parser; `SpecNamingConvention` applies suite package prefixes to describe, discovery, and support generation; run CLI options override valid configured profile/formatter/constructor-policy selections where applicable.
- **Compatibility boundary**: `org.javaspec.compatibility` checks profile compatibility and probes optional APIs reflectively by string name, without direct Java 9+ production imports.
- **Doubles engine**: `org.javaspec.doubles` creates ordinary-interface doubles through JDK dynamic proxies, configures return-value stubs by method name or exact arguments, records immutable call snapshots, verifies simple call expectations, and returns Java defaults for unstubbed methods.
- **Formatter/reporting/extension boundaries**: `org.javaspec.formatter`, `org.javaspec.reporting`, and `org.javaspec.extension` provide built-in formatter contracts/registry, dependency-free JSON reports, and minimal programmatic extension registration. External CLI extension loading is not implemented.

Later building blocks remain planned:

- **Runner expansion**: pending examples, source locations, bootstrap execution, and deeper profile-aware behavior.
- **Expectation and matcher engine follow-up**: additional PHPSpec parity, diagnostics, approximate equality, richer object-state matchers, and iteration/yield variants where Java-compatible.
- **Advanced doubles extensions**: richer argument matching, exception/callback stubbing, or concrete-class/final-class/static/constructor doubles outside the zero-dependency core if a future ADR permits optional integrations.
- **Advanced generator**: richer spec/source templates, safe existing sealed-interface source updates, and missing-method snippets beyond the current generator subset.
- **External extension loading**: configuration/classpath/plugin activation for extension-provided behavior, to be designed separately before implementation.

## 4.5 Risk Reduction Strategy

- Validate Java 8 bytecode and runtime behavior early.
- Add a runtime dependency audit as a build gate.
- Keep the profile catalog synchronized with explicit research and re-validate Java 25 stream gatherer metadata during quality-matrix work before relying on environment-specific behavior.
- Keep configuration syntax restricted and line-based unless a future zero-runtime-dependency-compatible design supersedes it.
- Keep MVP doubles interface-only and JDK-proxy based; require a future ADR before adding optional advanced doubles that need external tooling.
- Treat safe sealed-interface existing-source updates, deeper profile enforcement, bootstrap execution, advanced doubles, and external extension loading as later features requiring explicit design before implementation.
