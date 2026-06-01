# 5. Building Block View

## 5.1 Current Runtime Building Blocks

The implemented architecture now includes the Phase 2 first-MVP CLI/generation slice, the Phase 3 target-profile catalog and compatibility boundary, the Phase 4 configuration, naming, and discovery-filter model, the Phase 5/6 MVP reflection runner, the Phase 7 matcher/expectation expansion, and the Phase 8 MVP collaborators/doubles slice.

| Building block | Package | Responsibility |
|---|---|---|
| CLI adapter | `org.javaspec.cli` | Parses first-MVP `describe`/`desc` and `run` commands, `--config`, `--suite`, path overrides, constructor-policy overrides, run `--class`/`--example` filters, diagnostics, and exit codes; invokes the runner after discovery/generation/update and prints example summaries. |
| Configuration model | `org.javaspec.config` | Provides immutable default/configured suite settings and a restricted zero-runtime-dependency config parser. |
| Spec discovery, naming, and generation | `org.javaspec.discovery`, `org.javaspec.naming`, `org.javaspec.generation` | Applies default/configured naming conventions, discovers `*Spec.java` files, extracts example metadata, recognizes expanded chained matcher names for method-discovery/default-return inference where applicable, applies suite selection and class/example filters, feeds runner metadata, and plans/writes gated spec, support, production type, constructor, factory, and method skeletons. |
| Reflection runner | `org.javaspec.runner` | Executes filtered discovered examples reflectively when compiled spec classes are available on the effective classloader, records PASSED/FAILED/BROKEN/SKIPPED outcomes, and aggregates run/spec/example results. |
| Object behavior and matchers | `org.javaspec.api`, `org.javaspec.matcher` | Provides the Java-facing specification base class, lazy construction support, expectation wrappers, expanded matcher helpers, direct convenience assertions that delegate through `match(actual)`, double convenience APIs, matcher contracts, and custom matcher registration without runtime dependencies. |
| Doubles engine | `org.javaspec.doubles` | Provides zero-runtime-dependency interface doubles using JDK dynamic proxies, return-value stubbing, call history, and called/not-called/exact-count verification. |
| Profile catalog | `org.javaspec.profile` | Stores deterministic Java LTS profile, feature-flag, and API-symbol metadata for Java 8, 11, 17, 21, and 25. |
| Compatibility boundary | `org.javaspec.compatibility` | Checks profile compatibility and reflectively probes optional APIs without direct post-Java-8 linkage. |

## 5.2 Profile Catalog

`org.javaspec.profile` contains Java 8-compatible immutable metadata objects:

- `TargetProfile` defines ordered profile keys `java8`, `java11`, `java17`, `java21`, and `java25`.
- `FeatureFlag` models profile-gated language and library capabilities such as records, sealed types, collection factories, sequenced collections, and stream gatherers.
- `ApiSymbol`, `ApiSymbolKey`, `ApiSymbolKind`, and `ApiSymbolCategory` describe public JDK symbols by strings and categories.
- `ProfileCatalog` provides deterministic lookup by introduced profile, available profile, owner, owner/member key, and feature support.
- `DefaultProfileCatalogSymbols` populates representative metadata from the Java LTS data-structure research, including Java 25 stream gatherers.

The catalog has no runtime dependency outside the Java 8 standard library and does not import Java 9+ API types.

## 5.3 Compatibility Boundary

`org.javaspec.compatibility` separates profile decisions from runtime API probing:

- `ProfileCompatibilityCheck` checks whether a Java type kind, feature flag, or API symbol is allowed by a target profile.
- `CompatibilityResult` returns deterministic allowed/denied status, target profile, required profile, subject, and message.
- `ApiAvailabilityProbe` accepts class, method, and field names as strings and uses reflection to check availability on the current runtime. Missing classes, missing members, or linkage errors are treated as unavailable.

This boundary preserves the ADR 0001 rule that Java 11+ capabilities are metadata/reflection-only in production code while keeping later CLI and runner features able to make profile-aware decisions.

## 5.4 Configuration, Naming, and Discovery Filters

`org.javaspec.config` contains the Phase 4 configuration boundary:

- `JavaspecConfiguration` represents top-level settings: target profile, formatter, constructor policy, default suite, bootstrap metadata, and configured suites.
- `JavaspecSuiteConfiguration` represents one suite: suite name, spec root, source root, spec package prefix, production package prefix, and suite bootstrap metadata.
- `JavaspecConfigurationParser` reads a restricted line-based format with `=` or `:` separators, comment lines beginning with `#`, duplicate-key detection, unknown-key detection, required-value validation, and profile/constructor-policy validation.
- `ConstructorPolicyParser` keeps configuration-facing constructor-policy values limited to `delete`, `preserve`, and `comment`.
- `ConfigurationException` carries parse and validation diagnostics, including line numbers where available.

The naming/discovery boundary is implemented by `SpecNamingConvention`, `SpecDiscoveryRequest`, and `SpecExample`:

- `SpecNamingConvention` maps described production classes to spec/support classes and source paths using the selected suite's `specPackagePrefix` and `packagePrefix`.
- `SpecDiscoveryRequest` carries the spec root, suite name, naming convention, class filters, and example filters.
- `SpecExample` records public `void` `it_*`/`its_*` example methods with display names and source-order indexes.

The CLI adapter applies the selected suite's spec/source paths and package prefixes unless command-line path options override paths. `run` uses the configured constructor policy unless command-line `--constructor-policy` overrides it, filters classes with repeatable `--class <name>`, and filters examples with repeatable `--example <name>`. The same filtered `DiscoveredSpec`/`SpecExample` metadata is passed to the reflection runner, so filters affect both generation/update work and executable example selection. Bootstrap hooks and profile/formatter behavior are currently metadata for later runner and formatter features.

## 5.5 Reflection Runner

`org.javaspec.runner` contains the Phase 5/6 MVP execution model:

- `SpecRunner` accepts discovered specs and an effective classloader. It loads compiled spec classes reflectively and does not compile source or spec files itself.
- `DiscoveredSpec` and `SpecExample` remain the metadata source for which classes and examples may execute; source-discovery suite, class, and example filters therefore remain effective at execution time.
- Each example receives a fresh spec instance from the spec class no-argument constructor.
- Optional public no-argument `let()` is invoked before each example; optional public no-argument `letGo()` is invoked after each example, including after failures.
- `ExampleStatus` defines `PASSED`, `FAILED`, `BROKEN`, and `SKIPPED`.
- `AssertionError` from the example body is `FAILED`; non-assertion throwables from examples, lifecycle methods, instantiation, or reflection inspection are `BROKEN`.
- Non-loadable spec classes and missing or non-public/no-arg reflected example methods are `SKIPPED`.
- `ExampleResult`, `SpecResult`, `RunResult`, and `FailureDetail` provide immutable result aggregation for the CLI summary and future formatters.

The current limitation is deliberate: source-only or otherwise unavailable spec classes are skipped/not executable until an external build, IDE, or launcher puts compiled classes on the effective classloader.

## 5.6 Expectations and Matchers

`org.javaspec.matcher` contains the zero-dependency expectation wrapper and matcher registry:

- `Matchable<T>` provides PHPSpec-inspired chained assertions for identity/equality and their negations, return/equality aliases, type/instance aliases, `shouldImplement`, containment, string start/end/pattern checks and their negations, count/empty checks, and map key/value checks.
- Count and empty checks support arrays, collections, maps, character sequences, and generic iterables. Generic iterables are counted by iteration, so one-shot iterables are consumed and infinite iterables can hang.
- `MatcherRegistry` keeps the default registry free of runtime dependencies. It exposes identity, equality, negated identity, and a default negated-equality matcher while still allowing custom matchers to be registered.
- `ObjectBehavior<T>` keeps the generated-support-class base behavior and offers direct convenience assertion methods that delegate through `match(actual)`, so direct and fluent assertions share matcher behavior.
- `SpecDiscovery` recognizes expanded chained matcher method names in typed proxy calls for method-discovery/default-return inference where applicable.

No additional runtime component or third-party assertion library is introduced by this expansion.

## 5.7 Interface Doubles

`org.javaspec.doubles` contains the Phase 8 MVP collaborators/doubles implementation:

- `Doubles` creates interface proxies with `create`, `of`, and `proxy`, creates typed handles with `interfaceDouble`, detects javaspec doubles with `isDouble`, and returns a `DoubleControl` through `control`/`inspect`.
- `InterfaceDouble<T>` bundles the doubled interface type, proxy instance, control API, stubbing helpers, verification helpers, call-history queries, and reset operations.
- `DoubleControl`, `MethodStub`, and `CallVerifier` provide return-value stubbing, call counting, and called/not-called/exact-count verification.
- `Call` records immutable call snapshots with defensive argument copies and supports method-name and exact-argument matching.
- Exact argument matching supports `null` values and compares array contents rather than array identity.
- Interface method calls are recorded before a stubbed or default return is produced. Unstubbed methods return Java defaults: primitive defaults, `null` for reference types, and no action for `void`.
- `toString`, `equals`, and `hashCode` are handled deterministically by the invocation handler.
- `ObjectBehavior<T>` exposes convenience APIs such as `doubleFor`, `interfaceDouble`, `doubleControl`, `inspectDouble`, `doubleCalls`, `doubleCallCount`, `shouldHaveBeenCalled`, `shouldNotHaveBeenCalled`, and `shouldHaveBeenCalledTimes`.

The supported target is an ordinary interface. The type validator rejects `null`, primitive types, arrays, annotations, enums, concrete classes, and final classes with explicit diagnostics. The MVP deliberately excludes concrete class/final class/static/constructor doubles, wildcard argument matchers, exception/callback stubbing, bytecode-library integration in core, and default-interface-method invocation.
