# 5. Building Block View

## 5.1 Current Runtime Building Blocks

The implemented architecture now includes the Phase 2 first-MVP CLI/generation slice, the Phase 3 target-profile catalog and compatibility boundary, the Phase 4 configuration, naming, and discovery-filter model, the Phase 5/6 MVP reflection runner, the Phase 7 matcher/expectation expansion, the Phase 8 MVP collaborators/doubles slice, the Phase 9 CLI expansion, the Phase 10 advanced code-generation increment for interface-style methods, the Phase 11 formatter/reporting/extension increment, the Phase 14 no-JUnit integration foundation, the Phase 15 standalone optional Maven plugin adapter, the Phase 16 standalone optional Gradle plugin adapter, and the Phase 17 standalone optional JUnit Platform engine adapter.

| Building block | Package / artifact | Responsibility |
|---|---|---|
| CLI adapter | `org.javaspec.cli` | Parses first-MVP `describe`/`desc` and `run` commands, `--config`, `--suite`, path overrides, constructor-policy overrides, run `--class`/`--example` filters, Phase 9 run controls (`--dry-run`, `--stop-on-failure`, `--formatter`, `--profile`, `--verbose`), Phase 11 JSON report options (`--report`, `--report-file`), Phase 14 explicit classpath and JUnit XML options (`--classpath`, `--classpath-file`, `--junit-xml`, `--junit-xml-file`), diagnostics, and exit codes; invokes the runner after discovery/generation/update, renders output through the formatter registry, writes optional JSON and/or JUnit XML-compatible reports, and rejects run-only options for `describe`. |
| Configuration model | `org.javaspec.config` | Provides immutable default/configured suite settings and a restricted zero-runtime-dependency config parser. |
| Described type model | `org.javaspec.model` | Validates described Java type names, models class-like type kinds, constructor descriptors, and method descriptors used by discovery, generation, compatibility checks, and user diagnostics. |
| Spec discovery, naming, and generation | `org.javaspec.discovery`, `org.javaspec.naming`, `org.javaspec.generation` | Applies default/configured naming conventions, discovers `*Spec.java` files, extracts example metadata, recognizes expanded chained matcher names for method-discovery/default-return inference where applicable, applies suite selection and class/example filters, feeds runner metadata, and plans/writes or dry-run reports gated spec, support, production type, constructor, factory, method-body, interface-declaration, annotation-element, and missing sealed-interface skeleton generation. |
| Reflection runner | `org.javaspec.runner` | Executes filtered discovered examples reflectively when compiled spec classes are available on the effective or selected explicit classloader, records PASSED/FAILED/BROKEN/SKIPPED outcomes, supports stop-on-failure, and aggregates run/spec/example results for formatters, reports, and programmatic invocation. |
| Run formatters | `org.javaspec.formatter` | Defines the public zero-dependency `RunFormatter` contract, deterministic `RunFormatterRegistry`, built-in `progress` and `pretty` formatters, and formatter support code. |
| Run reporting | `org.javaspec.reporting` | Writes UTF-8 JSON runner reports with schemaVersion 1 and dependency-free JUnit XML-compatible reports from immutable `RunResult` data. |
| Invocation API | `org.javaspec.invocation` | Provides no-JUnit, no-`System.exit` programmatic invocation through `JavaspecInvocation`, `JavaspecLauncher`, `JavaspecInvocationResult`, and `JavaspecExitCode`, reusing canonical discovery, `SpecRunner`, and `RunResult`. |
| Optional Maven plugin adapter | `javaspec-maven-plugin/` | Standalone optional Maven plugin artifact `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT`, intentionally not a root module; provides goal prefix `javaspec` and `javaspec:run` as a Maven adapter over `JavaspecLauncher` using Maven test dependency resolution and test classpath, with Maven logging, filters, stop/fail/skip controls, and JSON/JUnit XML-compatible reports. |
| Optional Gradle plugin adapter | `javaspec-gradle-plugin/` | Standalone optional Gradle plugin artifact, intentionally not a root Maven module and outside the core artifact; provides plugin id `org.javaspec`, extension `javaspec`, and task `javaspecRun` in group `verification` as a Gradle adapter over `JavaspecLauncher` using the Gradle classpath, Java plugin test source set defaults when present, Gradle logging, filters, stop/fail/skip controls, and JSON/JUnit XML-compatible reports. |
| Optional JUnit Platform engine adapter | `javaspec-junit-platform-engine/` | Standalone optional JUnit Platform `TestEngine` artifact `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, intentionally not a root Maven module and outside the core artifact; provides engine id `javaspec`, ServiceLoader registration, JUnit Platform selector/configuration-parameter filtering over canonical discovery results, and execution through canonical `JavaspecLauncher` without `System.exit`. |
| Extension API | `org.javaspec.extension` | Provides the minimal programmatic extension lifecycle contract (`JavaspecExtension`/`Extension`) and `ExtensionContext` for registering run formatters; external extension discovery/loading is not implemented yet. |
| Object behavior and matchers | `org.javaspec.api`, `org.javaspec.matcher` | Provides the Java-facing specification base class, lazy construction support, expectation wrappers, expanded matcher helpers, direct convenience assertions that delegate through `match(actual)`, double convenience APIs, matcher contracts, and custom matcher registration without runtime dependencies. |
| Doubles engine | `org.javaspec.doubles` | Provides zero-runtime-dependency interface doubles using JDK dynamic proxies, return-value stubbing, call history, and called/not-called/exact-count verification. |
| Profile catalog | `org.javaspec.profile` | Stores deterministic Java LTS profile, feature-flag, and API-symbol metadata for Java 8, 11, 17, 21, and 25. |
| Compatibility boundary | `org.javaspec.compatibility` | Checks profile compatibility and reflectively probes optional APIs without direct post-Java-8 linkage. |

Phase 10 keeps advanced generation inside the existing discovery/generation boundary. Existing class, final-class, sealed-class, enum, and record method-body generation remains unchanged. Missing ordinary interface skeletons and existing ordinary interface sources use non-static method declarations, with existing-source updates performed source-preservingly. Missing annotation skeletons and existing annotation sources use compatible no-argument non-static elements. Missing sealed-interface skeletons use root declarations plus generated nested permitted implementation bodies with Java default returns. Existing sealed-interface source updates are intentionally skipped until nested permitted implementations can be updated source-preservingly.

Phase 11 keeps formatter rendering, JSON report writing, and extension registration as separate zero-dependency boundaries. The CLI still exposes only built-in formatter names because no external extension loading mechanism exists yet. JSON report writing happens after run summary rendering, uses the same runner result model as human-readable output, and is skipped when dry-run exits before execution because pending generation/update work exists.

Phase 14 adds the no-JUnit integration foundation without changing the canonical runner. Explicit CLI classpath entries are converted into a selected classloader used by type existence checks and spec execution. Programmatic invocation accepts a discovery request or already discovered specs plus a caller-supplied classloader and returns structured results instead of calling `System.exit`. JUnit XML-compatible reporting is generated internally from `RunResult` without a JUnit or XML/reporting library dependency, and can be requested together with the existing JSON report.

Phase 15 adds the optional Maven plugin as a standalone adapter artifact rather than a core module. Its POM packages a `maven-plugin` with Java source/target `1.8`, Maven API baseline `3.6.3`, Maven API and plugin annotations in `provided` scope, JUnit in `test` scope, and compile-scope dependency on core `org.javaspec:javaspec`. `JavaspecRunMojo` provides `javaspec:run` in the default `verify` phase, requires test dependency resolution, uses the Maven test classpath, supports config/suite/specDir/specRoot selection, class/example filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible reports, and Maven logging. It delegates to the canonical no-JUnit `JavaspecLauncher` without `System.exit`; projects under test do not need JUnit.

Phase 16 adds the optional Gradle plugin as a standalone adapter artifact rather than a root Maven module or core module. `build.gradle` uses `java-gradle-plugin`, group `org.javaspec`, version `0.1.0-SNAPSHOT`, Java source/target `1.8`, plugin id `org.javaspec`, implementation class `org.javaspec.gradle.JavaspecPlugin`, Maven-local core dependency `org.javaspec:javaspec:0.1.0-SNAPSHOT`, and plugin-local TestKit/JUnit test dependencies. `JavaspecPlugin` registers extension `javaspec` and task `javaspecRun` in group `verification`; when Gradle Java plugin source sets are present, the task defaults to the `test` source set runtime classpath and depends on `testClasses`. `JavaspecRunTask` supports `skip`, `failOnFailure`, `stopOnFailure`, config/suite/specDir/specRoot, class/example filters, `progress`/`pretty` formatting, JSON and JUnit XML-compatible report aliases, Gradle logging, `URLClassLoader` execution over the Gradle classpath, context classloader restore/close behavior, and canonical no-JUnit `JavaspecLauncher` delegation without `System.exit`. Projects under test do not need JUnit.

Phase 17 adds the optional JUnit Platform engine as a standalone adapter artifact rather than a root Maven module or core module. The artifact is `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging `jar`, Java source/target `1.8`, and uses Java 8-compatible JUnit Platform `1.10.2` rather than JUnit Platform 6/JUnit 6. `JavaspecTestEngine` is registered through `META-INF/services/org.junit.platform.engine.TestEngine` with engine id `javaspec`. Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`, configuration parameters for config/suite/spec root/class/example filters/stop-on-failure, and JUnit Platform class/package/method/unique-id selectors as filters over canonical discovery results. Execution delegates to canonical no-JUnit `JavaspecLauncher`, maps javaspec result states to JUnit Platform listener events, avoids `System.exit`, and requires no javaspec spec authoring style changes. Projects that do not opt into the engine keep no-JUnit CLI/programmatic/Maven/Gradle execution paths.

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

- `JavaspecConfiguration` represents top-level settings: target profile, formatter, constructor policy, default suite, bootstrap metadata, and configured suites. Phase 9 consumes profile and formatter as run selections; bootstrap remains metadata.
- `JavaspecSuiteConfiguration` represents one suite: suite name, spec root, source root, spec package prefix, production package prefix, and suite bootstrap metadata.
- `JavaspecConfigurationParser` reads a restricted line-based format with `=` or `:` separators, comment lines beginning with `#`, duplicate-key detection, unknown-key detection, required-value validation, and profile/constructor-policy validation.
- `ConstructorPolicyParser` keeps configuration-facing constructor-policy values limited to `delete`, `preserve`, and `comment`.
- `ConfigurationException` carries parse and validation diagnostics, including line numbers where available.

The naming/discovery boundary is implemented by `SpecNamingConvention`, `SpecDiscoveryRequest`, and `SpecExample`:

- `SpecNamingConvention` maps described production classes to spec/support classes and source paths using the selected suite's `specPackagePrefix` and `packagePrefix`.
- `SpecDiscoveryRequest` carries the spec root, suite name, naming convention, class filters, and example filters.
- `SpecExample` records public `void` `it_*`/`its_*` example methods with display names and source-order indexes.

The CLI adapter applies the selected suite's spec/source paths and package prefixes unless command-line path options override paths. `run` uses the configured constructor policy, profile, and formatter unless command-line `--constructor-policy`, `--profile`, or `--formatter` overrides them, filters classes with repeatable `--class <name>`, and filters examples with repeatable `--example <name>`. The same filtered `DiscoveredSpec`/`SpecExample` metadata is passed to the reflection runner, so filters affect both generation/update work and executable example selection. Bootstrap hooks are currently metadata for later runner features; selected profiles are validated and reported but not deeply enforced yet.

## 5.5 Reflection Runner

`org.javaspec.runner` contains the Phase 5/6 MVP execution model:

- `SpecRunner` accepts discovered specs and an effective or explicitly selected classloader. It loads compiled spec classes reflectively and does not compile source or spec files itself.
- `DiscoveredSpec` and `SpecExample` remain the metadata source for which classes and examples may execute; source-discovery suite, class, and example filters therefore remain effective at execution time.
- By default the runner processes all discovered example metadata; with `--stop-on-failure`, execution stops after the first FAILED or BROKEN executable example.
- Each example receives a fresh spec instance from the spec class no-argument constructor.
- Optional public no-argument `let()` is invoked before each example; optional public no-argument `letGo()` is invoked after each example, including after failures.
- `ExampleStatus` defines `PASSED`, `FAILED`, `BROKEN`, and `SKIPPED`.
- `AssertionError` from the example body is `FAILED`; non-assertion throwables from examples, lifecycle methods, instantiation, or reflection inspection are `BROKEN`.
- Non-loadable spec classes and missing or non-public/no-arg reflected example methods are `SKIPPED`.
- `ExampleResult`, `SpecResult`, `RunResult`, and `FailureDetail` provide immutable result aggregation for the CLI progress/pretty output, public run formatters, JSON reports, JUnit XML-compatible reports, programmatic invocation results, and optional JUnit Platform engine event mapping.

The current limitation is deliberate: source-only or otherwise unavailable spec classes are skipped/not executable until an external build, IDE, launcher, or explicit classpath puts compiled classes on the selected classloader.

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

## 5.8 Formatter, Reporting, Invocation, and Extension Boundaries

`org.javaspec.formatter` contains the Phase 11 human-readable output contract:

- `RunFormatter` renders a `RunResult` to a `PrintStream` and exposes a formatter name.
- `RunFormatterRegistry` normalizes names, registers formatters deterministically, exposes built-in formatter names, and provides the built-in registry.
- `ProgressRunFormatter` and `PrettyRunFormatter` preserve the Phase 9 output behavior while moving rendering behind the public contract.
- `RunFormatterSupport` keeps shared rendering details out of the CLI adapter.

`org.javaspec.reporting` contains dependency-free machine-readable report writers:

- `RunReportWriter` writes dependency-free UTF-8 JSON without introducing a JSON library runtime dependency.
- The JSON report schema is versioned as `schemaVersion` 1 and contains top-level summary counts, spec results, example results, nullable failure details, throwable class/message, and stack trace lines.
- `JUnitXmlReportWriter` writes a single UTF-8 JUnit XML-compatible `<testsuite name="javaspec">` from `RunResult`, mapping FAILED to `<failure>`, BROKEN to `<error>`, and SKIPPED to `<skipped>` without introducing JUnit or XML/reporting library runtime dependencies.
- No-spec runs with requested reports produce valid empty reports; passing, failing, broken, and skipped-only runs write requested reports after normal output; dry-run pending generation/update exits before execution and does not write reports.
- Report write failures are I/O failures and cause exit `70` with path diagnostics.

`org.javaspec.invocation` contains the Phase 14 no-`System.exit` invocation contract:

- `JavaspecInvocation` carries either a `SpecDiscoveryRequest` or pre-discovered specs, a selected `ClassLoader`, and stop-on-failure behavior.
- `JavaspecLauncher` runs canonical discovery when requested, delegates execution to `SpecRunner`, and returns `JavaspecInvocationResult`.
- `JavaspecInvocationResult` exposes immutable discovered specs, the `RunResult`, the derived exit code, and convenience success/failure helpers.
- `JavaspecExitCode` maps passing, skipped-only, and no-spec runs to `0`, and failed or broken runs to `1`.

`org.javaspec.extension` contains the minimal Phase 11 extension contract:

- `JavaspecExtension` defines `configure(ExtensionContext)` and the default `register(ExtensionContext)` alias.
- `Extension` is a short-name alias for `JavaspecExtension`.
- `ExtensionContext` exposes the run formatter registry through `runFormatterRegistry()` and `runFormatters()`.
- Extension loading, classpath scanning, `ServiceLoader`, plugin discovery, and configuration-driven extension activation are not implemented in this increment.
