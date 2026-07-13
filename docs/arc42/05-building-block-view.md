# 5. Building Block View

## 5.1 Current Runtime Building Blocks

The implemented architecture now includes the Phase 2 first-MVP CLI/generation slice, the Phase 3
target-profile catalog and compatibility boundary, the Phase 4 configuration, naming, and
discovery-filter model, the Phase 5/6 MVP reflection runner, the Phase 7 matcher/expectation
expansion, the Phase 8 MVP collaborators/doubles slice, the Phase 9 CLI expansion, the Phase 10
advanced code-generation increment for interface-style methods, the Phase 11
formatter/reporting/extension increment, the Phase 14 no-JUnit integration foundation, the Phase 15
standalone optional Maven plugin adapter, the Phase 16 standalone optional Gradle plugin adapter,
the Phase 17 standalone optional JUnit Platform engine adapter, the Phase 18 stable
identifier/source-location/report polish increment, the Phase 19 aggregate release/CI verification
assets, the Phase 20 release-readiness scaffolding assets, the Phase 21 standalone adoption/report
assets, the Phase 22 explicit skipped/pending semantics, the Phase 23 classpath/execution
availability diagnostics, the Phase 24 configuration-level report destinations, the Phase 25
ServiceLoader external formatter/extension discovery increment, the Phase 26 target-profile
enforcement increment, the Phase 27 bootstrap hook execution increment, the Phase 28
stronger-interface-doubles increment, the Phase 29 opt-in CLI compilation increment, Phases 30-36
known-limitations resolution, and the Phase 37 standalone optional bytecode-doubles adapter.

Current runtime building blocks:

- **CLI adapter** (`io.github.jvmspec.cli`)
  - Responsibility: Parses first-MVP `describe`/`desc` and `run` commands, `--config`, `--suite`,
    path overrides, constructor-policy overrides, run `--class`/`--example` filters, Phase 9 run
    controls (`--dry-run`, `--stop-on-failure`, `--formatter`, `--profile`, `--verbose`), Phase 11
    JSON report options (`--report`, `--report-file`), Phase 14 explicit classpath and JUnit XML
    options (`--classpath`, `--classpath-file`, `--junit-xml`, `--junit-xml-file`), Phase 24 config
    report destinations, Phase 26 profile enforcement, Phase 27 bootstrap execution, Phase 29
    compilation options (`--compile`, `--compile-output <dir>`), diagnostics, and exit codes;
    selects the effective run classloader, loads built-in plus ServiceLoader-provided
    formatters/extensions, enforces the effective profile after discovery and before
    generation/update writes, optionally compiles all `.java` files under the effective source/spec
    roots through the current JDK `javax.tools.JavaCompiler`, executes configured bootstrap hooks
    immediately before examples when specs exist, invokes the runner after
    generation/update/optional compilation, renders output through the formatter registry, writes
    optional JSON and/or JUnit XML-compatible reports from CLI or config destinations with CLI
    precedence, prints an `Execution diagnostics:` block only for execution-availability issues, and
    rejects run-only command-line options for `describe` while accepting config files that contain
    ignored profile/report/bootstrap hook entries.
- **Source compiler** (`io.github.jvmspec.compilation`)
  - Responsibility: Provides explicit opt-in compilation around JDK `javax.tools.JavaCompiler` for
    CLI and, through Phase 34 integration, programmatic/Maven/Gradle entry points. It collects all
    `.java` files under the effective source and spec roots, de-duplicates by normalized path,
    writes to `target/javaspec-classes` unless `--compile-output <dir>` is supplied, uses compile
    output then explicit CLI classpath entries then current process `java.class.path`, reports
    compiler-unavailable as exit `64`, and reports compiler failures as exit `1` with `Compilation
    failed:` and no reports.
- **Configuration model** (`io.github.jvmspec.config`)
  - Responsibility: Provides immutable default/configured suite settings, top-level
    constructor/profile/formatter defaults, extension activation entries, top-level and suite
    bootstrap hook class names, optional top-level JSON/JUnit XML-compatible report destinations,
    and a restricted zero-runtime-dependency config parser. There are no configuration keys for CLI
    compilation.
- **Described type model** (`io.github.jvmspec.model`)
  - Responsibility: Validates described Java type names, models class-like type kinds, constructor
    descriptors, and method descriptors used by discovery, generation, compatibility checks, and
    user diagnostics.
- **Spec discovery, naming, and generation** (`io.github.jvmspec.discovery`, `io.github.jvmspec.naming`,
  `io.github.jvmspec.generation`)
  - Responsibility: Applies default/configured naming conventions, discovers `*Spec.java` files,
    extracts example metadata including 1-based source lines, exposes stable ids for discovered
    specs, recognizes expanded chained matcher names for method-discovery/default-return inference
    where applicable, applies suite selection and class/example filters, feeds runner metadata, and
    plans/writes or dry-run reports gated spec, support, production type, constructor, factory,
    method-body, interface-declaration, annotation-element, and missing sealed-interface skeleton
    generation.
- **Reflection runner** (`io.github.jvmspec.runner`)
  - Responsibility: Executes filtered discovered examples reflectively when compiled spec classes
    are available on the effective, selected explicit, or CLI compile-output-first classloader,
    records PASSED/FAILED/BROKEN/SKIPPED/PENDING outcomes, enriches not-executable/skipped reasons
    for unavailable compiled spec classes, dependencies, or public no-argument example methods,
    supports stop-on-failure, and aggregates run/spec/example results with stable id aliases, source
    metadata, and separate skipped/pending counts where available for formatters, reports,
    diagnostics, and programmatic invocation.
- **Run formatters** (`io.github.jvmspec.formatter`)
  - Responsibility: Defines the public zero-dependency `RunFormatter` contract, deterministic
    `RunFormatterRegistry`, built-in `progress` and `pretty` formatters, formatter support code, and
    registry behavior used by ServiceLoader-discovered providers.
- **Run reporting** (`io.github.jvmspec.reporting`)
  - Responsibility: Writes UTF-8 JSON runner reports with schemaVersion 1, additive stable id/source
    metadata fields, Phase 22 pending summary counts and `PENDING` statuses, and Phase 35 optional
    run-level `ReportMetadata` from default writers while preserving schemaVersion 1 compatibility.
    It also writes dependency-free JUnit XML-compatible reports with testsuite
    timestamp/hostname/time/properties metadata and testcase file/line attributes when source data
    is available. JUnit XML-compatible reports map both skipped and pending examples to `<skipped>`
    while preserving separate core counts in immutable `RunResult` data.
- **Run diagnostics** (`io.github.jvmspec.diagnostics`)
  - Responsibility: Provides dependency-free deterministic execution-availability diagnostic lines
    from `RunResult`, reporting non-executable specs and stale/missing compiled example methods
    while excluding explicit user `@Skip` and `PENDING` semantics. CLI `--compile` can reduce
    source-only availability issues, but diagnostics still apply to missing dependencies, stale
    classes, default runs, and adapter/programmatic classpaths.
- **Bootstrap API** (`io.github.jvmspec.bootstrap`)
  - Responsibility: Provides zero-dependency bootstrap hook execution through `BootstrapHook`,
    immutable `BootstrapContext`, `BootstrapRunner`, and `BootstrapException`; explicit hook classes
    load from the run classloader/classpath, require public no-argument constructors, execute before
    ServiceLoader-discovered providers immediately before examples after any requested successful
    compilation, and fail before reports.
- **Invocation API** (`io.github.jvmspec.invocation`)
  - Responsibility: Provides no-JUnit, no-`System.exit` programmatic invocation through
    `JavaspecInvocation`, `JavaspecLauncher`, `JavaspecInvocationResult`, and `JavaspecExitCode`,
    reusing canonical discovery, optional compilation, extension activation, bootstrap hook
    execution, `SpecRunner`, and `RunResult`. Default invocation remains classpath-based.
- **Optional Maven plugin adapter** (`javaspec-maven-plugin/`)
  - Responsibility: Standalone optional Maven plugin artifact
    `io.github.jvmspec:javaspec-maven-plugin:1.0.0-RC1`, intentionally not a root module; provides
    goal prefix `javaspec` and `javaspec:run` as a Maven adapter over `JavaspecLauncher` using Maven
    test dependency resolution and test classpath by default, with Maven logging, filters, extension
    activation, formatter selection, explicit opt-in compilation settings, top-level plus
    selected-suite bootstrap hooks plus discovered hooks, stop/fail/skip controls, JSON/JUnit
    XML-compatible reports, config report destinations as defaults when explicit plugin report
    settings are absent, clear bootstrap build-failure diagnostics, and `javaspec:` warning
    diagnostics including Maven test classpath element counts when execution availability issues
    exist.
- **Optional Gradle plugin adapter** (`javaspec-gradle-plugin/`)
  - Responsibility: Standalone optional Gradle plugin artifact, intentionally not a root Maven
    module and outside the core artifact; provides plugin id `io.github.jvmspec`, extension `javaspec`,
    and task `javaspecRun` in group `verification` as a Gradle adapter over `JavaspecLauncher` using
    the Gradle classpath by default, Java plugin test source set defaults when present, Gradle
    logging, filters, extension activation, formatter selection, explicit opt-in compilation
    settings, top-level plus selected-suite bootstrap hooks plus discovered hooks, stop/fail/skip
    controls, ServiceLoader external formatter discovery from the run classloader, JSON/JUnit
    XML-compatible reports, config report destinations as defaults when explicit extension/task
    report settings are absent, clear bootstrap task-failure diagnostics, and `javaspec:` warning
    diagnostics including Gradle classpath element counts when execution availability issues exist.
- **Optional JUnit Platform engine adapter** (`javaspec-junit-platform-engine/`)
  - Responsibility: Standalone optional JUnit Platform `TestEngine` artifact
    `io.github.jvmspec:javaspec-junit-platform-engine:1.0.0-RC1`, intentionally not a root Maven
    module and outside the core artifact; provides engine id `javaspec`, ServiceLoader registration,
    JUnit Platform selector/configuration-parameter filtering over canonical discovery results,
    stable unique-id shape with MethodSource behavior, descriptor reporting aligned to stable ids,
    and execution through canonical `JavaspecLauncher` without `System.exit`.
- **Release/CI verification and readiness assets** (`scripts/verify-all.sh`,
  `scripts/check-version-alignment.sh`, `.github/workflows/ci.yml`, `CHANGELOG.md`, `RELEASING.md`,
  `LICENSE`, Maven `release-artifacts` profiles, Gradle source/javadoc jar readiness)
  - Responsibility: Provide non-disruptive aggregate local and GitHub Actions verification plus
    local release-readiness checks. Root `mvn verify` remains core-only; standalone Maven plugin,
    Gradle plugin, and JUnit Platform engine verification run explicitly after installing the
    current core snapshot; version alignment is checked first; local sources/javadocs can be
    packaged; MIT license and maintainer metadata are present. No publishing, signing, secrets,
    portal publication/credentials, final release tag/version, final publish approval, or mandatory
    Maven multi-module conversion is introduced.
- **Adoption examples and report documentation assets** (`examples/`, `scripts/verify-examples.sh`,
  `docs/schemas/run-report-v1.schema.json`, `docs/examples/reports/`)
  - Responsibility: Provide standalone consumer examples for Maven plugin, Gradle plugin, and JUnit
    Platform engine adoption paths plus a versioned JSON report schema and golden JSON/JUnit
    XML-compatible report examples. Examples are not root modules and use local snapshots until
    public artifacts are available. `scripts/verify-all.sh` runs examples by default with explicit
    `JAVASPEC_SKIP_EXAMPLES=1` and Gradle-example opt-out `JAVASPEC_SKIP_GRADLE_EXAMPLE=1`.
- **Extension API** (`io.github.jvmspec.extension`)
  - Responsibility: Provides the minimal extension lifecycle contract
    (`JavaspecExtension`/`Extension`), `ExtensionContext` for registering run formatters,
    `ExtensionLoadingException`, and `JavaspecExtensionLoader` for JDK ServiceLoader discovery of
    `RunFormatter`, `JavaspecExtension`, and alias `Extension` providers.
- **Object behavior and matchers** (`io.github.jvmspec.api`, `io.github.jvmspec.matcher`)
  - Responsibility: Provides the Java-facing specification base class, lazy construction support,
    explicit `skip(...)`/`pending(...)` runtime signals, expectation wrappers, expanded matcher
    helpers, direct convenience assertions that delegate through `match(actual)`, double convenience
    APIs, matcher contracts, and custom matcher registration without runtime dependencies.
- **Doubles engine** (`io.github.jvmspec.doubles`)
  - Responsibility: Provides zero-runtime-dependency interface doubles using JDK dynamic proxies,
    matcher-aware argument constraints, return/throw/answer stubbing, call history,
    called/not-called/exact-count verification, and a zero-dependency `ConcreteDoubleProvider` SPI
    used by optional adapters.
- **Optional bytecode doubles adapter** (`javaspec-bytecode-doubles/`)
  - Responsibility: Standalone optional artifact outside the root reactor. It depends on ByteBuddy
    1.14.18, implements `ConcreteDoubleProvider`, creates subclasses for non-final concrete classes,
    delegates to core `DoubleControl` semantics, and rejects final classes, enums, arrays,
    annotations, primitives, interfaces, static mocking, and constructor mocking.
- **Profile catalog** (`io.github.jvmspec.profile`)
  - Responsibility: Stores deterministic Java LTS profile, feature-flag, and API-symbol metadata for
    Java 8, 11, 17, 21, and 25.
- **Compatibility boundary** (`io.github.jvmspec.compatibility`)
  - Responsibility: Checks profile compatibility, exposes the additive `ProfileEnforcement` report
    API, enforces described type kinds and resolvable cataloged API signature owners before
    generation/update writes, and reflectively probes optional APIs without direct post-Java-8
    linkage.

Phase 10 keeps advanced generation inside the existing discovery/generation boundary. Existing
class, final-class, sealed-class, enum, and record method-body generation remains unchanged. Missing
ordinary interface skeletons and existing ordinary interface sources use non-static method
declarations, with existing-source updates performed source-preservingly. Missing annotation
skeletons and existing annotation sources use compatible no-argument non-static elements. Missing
sealed-interface skeletons use root declarations plus generated nested permitted implementation
bodies with Java default returns. Existing sealed-interface source updates are now performed
source-preservingly together with nested permitted implementation method bodies.

Phase 11 keeps formatter rendering, JSON report writing, and extension registration as separate
zero-dependency boundaries. Phase 25 extends the formatter/extension boundary with ServiceLoader
provider discovery while preserving built-in formatter defaults and zero runtime dependencies. JSON
report writing happens after run summary rendering, uses the same runner result model as
human-readable output, and is skipped when dry-run exits before execution because pending
generation/update work exists, when profile enforcement exits before writes, or when bootstrap
execution fails before examples/reports.

Phase 14 adds the no-JUnit integration foundation without changing the canonical runner. Explicit
CLI classpath entries are converted into a selected classloader used by type existence checks and
spec execution. Programmatic invocation accepts a discovery request or already discovered specs plus
a caller-supplied classloader and returns structured results instead of calling `System.exit`. JUnit
XML-compatible reporting is generated internally from `RunResult` without a JUnit or XML/reporting
library dependency, and can be requested together with the existing JSON report.

Phase 15 adds the optional Maven plugin as a standalone adapter artifact rather than a core module.
Its POM packages a `maven-plugin` with Java source/target `1.8`, Maven API baseline `3.6.3`, Maven
API and plugin annotations in `provided` scope, JUnit in `test` scope, and compile-scope dependency
on core `io.github.jvmspec:javaspec`. `JavaspecRunMojo` provides `javaspec:run` in the default `verify`
phase, requires test dependency resolution, uses the Maven test classpath, supports
config/suite/specDir/specRoot selection, class/example filters, top-level plus selected-suite
bootstrap hooks, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible
reports, config report destinations as defaults when explicit plugin settings are absent, and Maven
logging. It delegates to the canonical no-JUnit `JavaspecLauncher` without `System.exit`; bootstrap
failures fail the build with clear diagnostics, and projects under test do not need JUnit.

Phase 16 adds the optional Gradle plugin as a standalone adapter artifact rather than a root Maven
module or core module. `build.gradle` uses `java-gradle-plugin`, group `io.github.jvmspec`, version
`1.0.0-RC1`, Java source/target `1.8`, plugin id `io.github.jvmspec`, implementation class
`io.github.jvmspec.gradle.JavaspecPlugin`, Maven-local core dependency
`io.github.jvmspec:javaspec:1.0.0-RC1`, and plugin-local TestKit/JUnit test dependencies.
`JavaspecPlugin` registers extension `javaspec` and task `javaspecRun` in group `verification`; when
Gradle Java plugin source sets are present, the task defaults to the `test` source set runtime
classpath and depends on `testClasses`. `JavaspecRunTask` supports `skip`, `failOnFailure`,
`stopOnFailure`, config/suite/specDir/specRoot, class/example filters, top-level plus selected-suite
bootstrap hooks, built-in or ServiceLoader-discovered formatter selection, JSON and JUnit
XML-compatible report aliases, config report destinations as defaults when explicit extension/task
report settings are absent, Gradle logging, `URLClassLoader` execution over the Gradle classpath,
context classloader restore/close behavior, and canonical no-JUnit `JavaspecLauncher` delegation
without `System.exit`. Bootstrap failures fail the task with clear diagnostics, and projects under
test do not need JUnit.

Phase 17 adds the optional JUnit Platform engine as a standalone adapter artifact rather than a root
Maven module or core module. The artifact is
`io.github.jvmspec:javaspec-junit-platform-engine:1.0.0-RC1`, packaging `jar`, Java source/target
`1.8`, and uses Java 8-compatible JUnit Platform `1.10.2` rather than JUnit Platform 6/JUnit 6.
`JavaspecTestEngine` is registered through `META-INF/services/org.junit.platform.engine.TestEngine`
with engine id `javaspec`. Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`,
configuration parameters for config/suite/spec root/class/example filters/stop-on-failure, and JUnit
Platform class/package/method/unique-id selectors as filters over canonical discovery results.
Execution delegates to canonical no-JUnit `JavaspecLauncher`, maps javaspec result states to JUnit
Platform listener events, avoids `System.exit`, and requires no javaspec spec authoring style
changes. Projects that do not opt into the engine keep no-JUnit CLI/programmatic/Maven/Gradle
execution paths.

Phase 18 adds stable identifier and source-location metadata across discovery, runner results,
reports, and optional JUnit Platform descriptor reporting. `DiscoveredSpec` and `SpecResult` stable
ids derive from the spec qualified name; `ExampleResult` stable ids use
`<specQualifiedName>#<methodName>` and match `fullName()`. `SpecExample` carries 1-based source
lines, runner results carry source file/line metadata where created from discovered specs/examples,
JSON reports add stable id/source fields without removing existing fields, and JUnit XML-compatible
reports add `<testcase>` file/line attributes when source data is available. At Phase 18 this did
not add external extension loading, profile enforcement, pending examples, or classpath/execution
availability diagnostics; pending examples were implemented later in Phase 22, execution
availability diagnostics later in Phase 23, ServiceLoader formatter/extension discovery later in
Phase 25, and target-profile enforcement later in Phase 26.

Phase 19 adds release/CI verification assets without changing runtime building blocks.
`scripts/verify-all.sh` runs root core verification, root runtime dependency audit, current-core
snapshot install, standalone Maven plugin verification and runtime audit, standalone JUnit Platform
engine verification and runtime audit, standalone Gradle plugin `clean test build`, and Gradle
runtimeClasspath audit. It supports `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, and explicit
`JAVASPEC_SKIP_GRADLE=1`. `.github/workflows/ci.yml` runs a Java 8/11/17/21/25 core matrix and a
Java 21 full-verification job through the script. This preserves the decision that the standalone
adapters are not mandatory root Maven modules.

Phase 20 adds release-readiness scaffolding without changing runtime building blocks.
`scripts/check-version-alignment.sh` verifies version alignment across the root POM, standalone
Maven plugin POM, standalone JUnit Platform engine POM, Gradle plugin `version`, and Gradle plugin
`javaspecCoreVersion`; `scripts/verify-all.sh` runs it first. `CHANGELOG.md` and `RELEASING.md`
document release changes and blockers. The MIT `LICENSE` is copied exactly from `origin/main`, and
MIT license plus confirmed maintainer/developer metadata are present in Maven POMs and Gradle
generated POM metadata. Maven `release-artifacts` profiles create local sources/javadocs for the
root, Maven plugin, and JUnit Platform engine builds; the Gradle plugin build is ready to create
source and javadoc jars. Safe URL, SCM, and GitHub Issues metadata is allowed, but signing,
deploy/publish configuration, Central Portal publication, Gradle Plugin Portal
publication/credentials, final release version/tag, and final publish approval remain intentionally
absent until owner decisions are made.

Phase 21 adds adoption and report documentation assets without changing runtime building blocks.
`docs/schemas/run-report-v1.schema.json` documents schemaVersion 1 JSON reports;
`docs/examples/reports/passing-run-report-v1.json` and
`docs/examples/reports/passing-junit-report.xml` provide golden passing report fixtures.
`examples/maven-basic/`, `examples/gradle-basic/`, and `examples/junit-platform-basic/` are
standalone consumer projects and are not root modules. `scripts/verify-examples.sh` installs local
snapshots, runs examples, and asserts generated report markers. `scripts/verify-all.sh` invokes
examples verification by default after core/adapters unless `JAVASPEC_SKIP_EXAMPLES=1` is set.

Phase 22 adds explicit skipped/pending semantics to existing runtime building blocks.
`io.github.jvmspec.api.Skip` and `Pending` are runtime method annotations; `SkipExampleException` and
`PendingExampleException` are unchecked signals; `ObjectBehavior` exposes `skip()`/`skip(String)`
and `pending()`/`pending(String)`. The reflection runner resolves annotation-based skip/pending
before instantiation and lifecycle execution, gives `@Skip` precedence over `@Pending`, processes
runtime skip/pending signals from `let()` or examples after successful `letGo()`, and reports
`BROKEN` when `letGo()` fails after such a signal. `ExampleStatus.PENDING` is distinct from
`SKIPPED`; `RunResult`/`SpecResult` expose separate `pendingCount()` plus skipped-plus-pending
helpers for JUnit XML-compatible reports. Built-in formatters and Maven logging include pending
counts. The optional JUnit Platform engine maps pending to `executionSkipped` with a `Pending:`
reason while preserving unique IDs/descriptors.

Phase 23 adds classpath/execution availability diagnostics without changing the compilation
boundary. The reflection runner enriches non-executable spec and missing/stale example-method
reasons when source discovery found specs/examples that the runner classloader cannot execute.
`io.github.jvmspec.diagnostics.RunDiagnostics.executionAvailabilityLines(RunResult)` returns
deterministic human-readable lines for those availability issues while excluding explicit user
`@Skip` and `PENDING` results. The CLI prints an `Execution diagnostics:` block only when such lines
exist, with hints for either the current process classloader or explicit classpath entry count. The
Maven and Gradle adapters log `javaspec:` warning diagnostics plus their classpath element counts.
Source/spec compilation and classpath assembly remain external build-tool or launcher
responsibilities, and exit-code/build-failure semantics are unchanged.

Phase 24 adds optional top-level report destinations to the configuration model without changing
report writer boundaries. JSON aliases are `report`, `reportFile`, `report-file`, `jsonReport`,
`jsonReportFile`, and `json-report-file`; JUnit XML-compatible aliases are `junitXml`, `junit-xml`,
`junitXmlFile`, `junit-xml-file`, `junitXmlReportFile`, and `junit-xml-report-file`. Values are
trimmed, non-blank when present, and default to absent/null. `run --config <file>` writes configured
reports when no corresponding CLI report option is supplied; CLI report options override config
values. `describe --config <file>` accepts those config keys but does not write reports. Maven and
Gradle adapters use config destinations as defaults when explicit adapter report settings are
absent, and explicit adapter settings override config values. Report schemas, content, writers,
dry-run pending behavior, no-spec behavior, exit codes, build-failure semantics, and standalone
adapter boundaries are unchanged.

Phase 25 adds ServiceLoader external formatter/extension discovery without changing runner/report
semantics. `JavaspecExtensionLoader.loadRunFormatterRegistry()` and
`loadRunFormatterRegistry(ClassLoader)` create a registry with built-ins first, then providers from
`META-INF/services/io.github.jvmspec.formatter.RunFormatter`,
`META-INF/services/io.github.jvmspec.extension.JavaspecExtension`, and alias
`META-INF/services/io.github.jvmspec.extension.Extension`. Duplicate extension implementation classes
listed under both extension service types are configured once. Invalid service declarations, invalid
formatter providers, and extension configuration failures raise `ExtensionLoadingException`. CLI
`javaspec run` loads providers after effective classloader selection, so process classpath entries
and explicit `--classpath` / `--classpath-file` entries can supply formatter jars. Gradle
`javaspecRun` loads providers from its run classloader. Phase 32 later adds config-driven extension
activation and adapter formatter controls where implemented; report schema/content changes, runtime
dependency additions, and publishing changes are not part of Phase 25 itself.

Phase 26 adds target-profile enforcement before generation/update writes.
`ProfileEnforcement.defaultEnforcement()` checks each discovered described type and related type
against the effective `TargetProfile`. Violations stop `javaspec run` with exit `64` before
related-spec generation, support updates, production skeleton writes, constructor/method updates,
prompts, runner execution, or report writing. Enforcement rejects `record`, `sealed class`, and
`sealed interface` below `java17`, and checks generated method return/parameter types only when
their owners resolve to known profile-catalog Java API symbols. Unknown project types and ambiguous
or unresolvable simple type names are ignored deliberately to avoid false positives.

Phase 27 adds bootstrap hook execution before examples. `bootstrap` config entries are fully
qualified hook class names. `BootstrapRunner` loads each hook from the effective run
classloader/classpath, verifies that it implements `io.github.jvmspec.bootstrap.BootstrapHook`,
constructs it through a public no-argument constructor, and calls it with an immutable
`BootstrapContext` containing the run classloader and discovered specs. Top-level hooks run before
the selected suite hooks, declaration order and duplicates are preserved, and CLI no-spec runs skip
hook execution. Phase 33 adds ServiceLoader-discovered hook providers after explicit configured
hooks in deterministic order. CLI bootstrap failures print `Error: Bootstrap execution failed`, exit
`64`, and write no reports; Maven and Gradle adapters fail their build/task clearly through the
canonical invocation path. Bootstrap does not add script engines, package scanning, dependency
resolution, runtime dependencies, or Java 8 compatibility exceptions.

Phase 28 strengthens interface doubles inside the existing `io.github.jvmspec.doubles` boundary.
`ArgumentMatcher`, `ArgumentMatchers`, and `Doubles` factory aliases add matcher-aware argument
constraints; argument-constrained stubs take priority over method-wide stubs;
`MethodStub.thenThrow(...)` / `throwsException(...)` add throwing stubs; and
`MethodStub.thenAnswer(...)` / `answers(...)` add callback stubs through immutable
`DoubleInvocation` contexts. Phase 28 does not change CLI behavior, report schemas/content,
dependencies, generated assets, examples, optional adapter boundaries, or the JDK-proxy-only
interface target.

Phase 29 adds opt-in CLI compilation inside the `io.github.jvmspec.compilation` boundary and the CLI
adapter; Phase 34 extends explicit opt-in compilation to programmatic, Maven, and Gradle entry
points. `--compile` compiles source/spec `.java` files before bootstrap/examples, and
`--compile-output <dir>` implies compilation while defaulting to `target/javaspec-classes`
otherwise. Compilation is skipped for no-spec and dry-run runs. The compiler is the current JDK
`javax.tools.JavaCompiler`, not a forked `javac` or dependency. The compiler classpath is output
directory, explicit CLI classpath entries, then current process `java.class.path`; the execution
classloader also places output before explicit entries. Compiler-unavailable runs exit `64`; compile
failures exit `1`, print `Compilation failed:`, write no reports, and skip bootstrap/examples. The
compilation boundary does not add config keys, JUnit Platform compilation, report schema/content
changes, dependency resolution, incremental caches, or source-level/release management.

## 5.2 Profile Catalog

`io.github.jvmspec.profile` contains Java 8-compatible immutable metadata objects:

- `TargetProfile` defines ordered profile keys `java8`, `java11`, `java17`, `java21`, and `java25`.
- `FeatureFlag` models profile-gated language and library capabilities such as records, sealed
  types, collection factories, sequenced collections, and stream gatherers.
- `ApiSymbol`, `ApiSymbolKey`, `ApiSymbolKind`, and `ApiSymbolCategory` describe public JDK symbols
  by strings and categories.
- `ProfileCatalog` provides deterministic lookup by introduced profile, available profile, owner,
  owner/member key, and feature support.
- `DefaultProfileCatalogSymbols` populates representative metadata from the Java LTS data-structure
  research, including Java 25 stream gatherers.

The catalog has no runtime dependency outside the Java 8 standard library and does not import Java
9+ API types.

## 5.3 Compatibility Boundary

`io.github.jvmspec.compatibility` separates profile decisions from runtime API probing:

- `ProfileCompatibilityCheck` checks whether a Java type kind, feature flag, or API symbol is
  allowed by a target profile.
- `ProfileEnforcement` enforces a target profile against a described type by combining type-kind
  checks with conservative generated method signature checks.
- `ProfileEnforcementReport` and `ProfileViolation` expose immutable programmatic enforcement
  results and violation summaries.
- `CompatibilityResult` returns deterministic allowed/denied status, target profile, required
  profile, subject, and message.
- `ApiAvailabilityProbe` accepts class, method, and field names as strings and uses reflection to
  check availability on the current runtime. Missing classes, missing members, or linkage errors are
  treated as unavailable.

This boundary preserves the ADR 0001 rule that Java 11+ capabilities are metadata/reflection-only in
production code while keeping later CLI and runner features able to make profile-aware decisions. It
is intentionally not a compiler and does not reject unknown project types or ambiguous/unresolvable
simple names.

## 5.4 Configuration, Naming, and Discovery Filters

`io.github.jvmspec.config` contains the Phase 4 configuration boundary:

- `JavaspecConfiguration` represents top-level settings: target profile, formatter, constructor
  policy, default suite, optional JSON/JUnit XML-compatible report destinations, executable
  bootstrap hook class names, and configured suites. Phase 9 consumes profile and formatter as run
  selections; Phase 26 enforces the effective profile before generation/update writes; Phase 25
  validates formatter selections against built-in plus ServiceLoader-discovered names on the
  effective run classloader; Phase 24 consumes report destinations as run defaults; Phase 27
  executes configured bootstrap hooks before examples.
- `JavaspecSuiteConfiguration` represents one suite: suite name, spec root, source root, spec
  package prefix, production package prefix, and suite bootstrap hook class names.
- `JavaspecConfigurationParser` reads a restricted line-based format with `=` or `:` separators,
  comment lines beginning with `#`, duplicate-key detection, unknown-key detection, required-value
  validation, report-destination non-blank validation, and profile/constructor-policy validation.
- `ConstructorPolicyParser` keeps configuration-facing constructor-policy values limited to
  `delete`, `preserve`, and `comment`.
- `ConfigurationException` carries parse and validation diagnostics, including line numbers where
  available.

The pre-1.0 internal language seam is implemented under `io.github.jvmspec.internal.language`.
`JavaSpecLanguageFrontend` delegates canonical Java spec discovery. Behind the stable
`SpecDiscovery` facade, constructor observation and identity, construction-argument inference, Java
expression/type inference, callable discovery, subject declaration discovery, and example discovery
are isolated in package-private components. Java inference further separates literal/factory
classification, expression-argument splitting, generic-aware source method/import context parsing,
and orchestration. The facade owns only deterministic traversal, filtering, and orchestration.
Method synchronization separately selects Java type-kind eligibility, inventories existing direct
members, and renders deterministic class, interface, annotation, and factory skeletons before the
source-preserving insertion step.
`BehaviorContract` retains the current immutable Java descriptor as a compatibility bridge while
separately exposing portable subject shape, relationships, structured type references, construction signatures, callable
signatures, invocation kind, and unknown-type evidence. `JavaProductionLanguageBackend` plans
constructor-then-method synchronization entirely in memory.
Only Java is registered; selection is not exposed as public API, SPI, CLI, or configuration before
1.0. The CLI continues to own authorization and atomic application of every planned source write.

The naming/discovery boundary is implemented by `SpecNamingConvention`, `SpecDiscoveryRequest`, and
`SpecExample`:

- `SpecNamingConvention` maps described production classes to spec/support classes and source paths
  using the selected suite's `specPackagePrefix` and `packagePrefix`.
- `SpecDiscoveryRequest` carries the spec root, suite name, naming convention, class filters, and
  example filters.
- `SpecExample` records public `void` `it_*`/`its_*` example methods with display names,
  source-order indexes, and 1-based source lines.

The CLI adapter applies the selected suite's spec/source paths and package prefixes unless
command-line path options override paths. `run` uses the configured constructor policy, profile,
formatter, bootstrap hooks, and report destinations unless command-line `--constructor-policy`,
`--profile`, `--formatter`, `--report`/`--report-file`, or `--junit-xml`/`--junit-xml-file`
overrides the settings that have CLI overrides, filters classes with repeatable `--class <name>`,
and filters examples with repeatable `--example <name>`. The same filtered
`DiscoveredSpec`/`SpecExample` metadata is passed to bootstrap hooks and the reflection runner, so
filters affect generation/update work, bootstrap context, and executable example selection.
Top-level bootstrap hooks run before selected-suite hooks immediately before examples; selected
profiles are enforced by `run` before generation/update writes while `describe` ignores configured
profile and bootstrap hook entries.

## 5.5 Reflection Runner

`io.github.jvmspec.runner` contains the Phase 5/6 MVP execution model:

- `SpecRunner` accepts discovered specs and an effective, explicitly selected, or CLI
  compile-output-first classloader. It loads compiled spec classes reflectively and does not compile
  source or spec files itself.
- `DiscoveredSpec` and `SpecExample` remain the metadata source for which classes and examples may
  execute; source-discovery suite, class, and example filters therefore remain effective at
  execution time.
- By default the runner processes all discovered example metadata; with `--stop-on-failure`,
  execution stops after the first FAILED or BROKEN executable example.
- Each example receives a fresh spec instance from the spec class no-argument constructor.
- Optional public no-argument `let()` is invoked before each example; optional public no-argument
  `letGo()` is invoked after each example, including after failures.
- `ExampleStatus` defines `PASSED`, `FAILED`, `BROKEN`, `SKIPPED`, and `PENDING`.
- `AssertionError` from the example body is `FAILED`; non-assertion throwables from examples,
  lifecycle methods, instantiation, or reflection inspection are `BROKEN`.
- Non-loadable spec classes and missing or non-public/no-arg reflected example methods are `SKIPPED`
  with enriched execution-availability reasons.
- Explicit `@Skip`/`@Pending` annotations and runtime skip/pending signals mark examples `SKIPPED`
  or `PENDING` according to ADR 0015, with separate skipped and pending counts.
- `ExampleResult`, `SpecResult`, `RunResult`, and `FailureDetail` provide immutable result
  aggregation for the CLI progress/pretty output, public run formatters, JSON reports, JUnit
  XML-compatible reports, programmatic invocation results, and optional JUnit Platform engine event
  mapping. `ExampleResult` and `SpecResult` expose stable id aliases and carry source metadata where
  available.

The current limitation is deliberate for default and adapter paths: source-only or otherwise
unavailable spec classes are skipped/not executable until an external build, IDE, launcher, explicit
classpath, or CLI `--compile` output puts compiled classes on the selected classloader. Phase 23
diagnostics make those availability problems explicit, and Phase 29 gives CLI users an opt-in
compiler step, and Phase 34 extends opt-in compilation to programmatic/Maven/Gradle paths without
changing defaults, JUnit Platform behavior, or automatic classpath repair.

## 5.6 Expectations and Matchers

`io.github.jvmspec.matcher` contains the zero-dependency expectation wrapper and matcher registry:

- `Matchable<T>` provides PHPSpec-inspired chained assertions for identity/equality and their
  negations, return/equality aliases, type/instance aliases, `shouldImplement`, containment, string
  start/end/pattern checks and their negations, count/empty checks, and map key/value checks.
- Count and empty checks support arrays, collections, maps, character sequences, and generic
  iterables. Generic iterable empty checks use `iterator().hasNext()`, and count checks iterate at
  most `expected + 1` elements so infinite iterables fail fast when they exceed the expected count;
  one-shot iterables may still be advanced by bounded checks.
- `MatcherRegistry` keeps the default registry free of runtime dependencies. It exposes identity,
  equality, negated identity, and a default negated-equality matcher while still allowing custom
  matchers to be registered.
- `ObjectBehavior<T>` keeps the generated-support-class base behavior and offers direct convenience
  assertion methods that delegate through `match(actual)`, so direct and fluent assertions share
  matcher behavior.
- `SpecDiscovery` recognizes expanded chained matcher method names in typed proxy calls for
  method-discovery/default-return inference where applicable.

No additional runtime component or third-party assertion library is introduced by this expansion.

## 5.7 Interface Doubles

`io.github.jvmspec.doubles` contains the Phase 8 interface-doubles implementation plus the Phase 28
strengthening:

- `Doubles` creates interface proxies with `create`, `of`, and `proxy`, creates typed handles with
  `interfaceDouble`, detects javaspec doubles with `isDouble`, returns a `DoubleControl` through
  `control`/`inspect`, and exposes matcher factory aliases.
- `InterfaceDouble<T>` bundles the doubled interface type, proxy instance, control API, stubbing
  helpers, verification helpers, call-history queries, and reset operations.
- `ArgumentMatcher` matches one observed argument; `ArgumentMatchers` and `Doubles` provide `any()`
  / `anyArgument()`, nullable typed `any(Class<?>)` / `anyType(Class<?>)`, `isNull()`, `notNull()`,
  and array-aware `eq(Object)` / `equalTo(Object)` factories.
- `DoubleControl`, `MethodStub`, and `CallVerifier` provide method-wide and argument-constrained
  stubbing, call counting, and called/not-called/exact-count verification. Existing vararg APIs
  interpret `ArgumentMatcher` values as matchers while ordinary values remain exact expected
  arguments.
- Argument-constrained stubs, including matcher patterns, take priority over method-wide stubs.
  Within the same priority, the newest matching stub wins.
- `MethodStub.thenReturn(...)` / `returns(...)` configure returned values; `thenThrow(Throwable)` /
  `throwsException(Throwable)` configure throwing stubs; `thenAnswer(StubAnswer)` /
  `answers(StubAnswer)` configure callback stubs.
- `StubAnswer` receives an immutable `DoubleInvocation` context with the reflective method, method
  name, immutable argument snapshots, defensive argument-array copies, individual argument access,
  and argument count. Answer return values use existing return type validation, and thrown
  exceptions propagate.
- `Call` records immutable call snapshots with defensive argument copies and supports matcher-aware
  `hasArguments(...)` plus method-name and argument matching.
- Exact ordinary argument matching supports `null` values and compares array contents rather than
  array identity.
- Interface method calls are recorded before a stubbed return, thrown exception, answer result, or
  default return is produced. Unstubbed methods return Java defaults: primitive defaults, `null` for
  reference types, and no action for `void`.
- `toString`, `equals`, and `hashCode` are handled deterministically by the invocation handler.
- `ObjectBehavior<T>` exposes convenience APIs such as `doubleFor`, `interfaceDouble`,
  `doubleControl`, `inspectDouble`, `doubleCalls`, `doubleCallCount`, `shouldHaveBeenCalled`,
  `shouldNotHaveBeenCalled`, and `shouldHaveBeenCalledTimes`; vararg argument helpers accept
  ordinary values or argument matchers.

The supported target is an ordinary interface. The type validator rejects `null`, primitive types,
arrays, annotations, enums, concrete classes, and final classes with explicit diagnostics. The core
deliberately excludes concrete class/final class/static/constructor doubles, bytecode-library
integration, default-interface-method invocation, and CLI/report/schema/dependency/generated-asset
changes. Non-final concrete-class doubles are available only through the standalone optional
bytecode adapter.

## 5.8 Formatter, Reporting, Invocation, and Extension Boundaries

`io.github.jvmspec.formatter` contains the Phase 11 human-readable output contract:

- `RunFormatter` renders a `RunResult` to a `PrintStream` and exposes a formatter name.
- `RunFormatterRegistry` normalizes names, registers formatters deterministically, exposes formatter
  names, and provides the built-in registry used before ServiceLoader providers are added.
- `ProgressRunFormatter` and `PrettyRunFormatter` preserve the Phase 9 output behavior while moving
  rendering behind the public contract.
- `RunFormatterSupport` keeps shared rendering details out of the CLI adapter.

`io.github.jvmspec.reporting` contains dependency-free machine-readable report writers:

- `RunReportWriter` writes dependency-free UTF-8 JSON without introducing a JSON library runtime
  dependency.
- The JSON report schema is versioned as `schemaVersion` 1 and contains top-level summary counts,
  spec results, example results, nullable failure details, throwable class/message, stack trace
  lines, Phase 18 additive stable id/source fields, Phase 22 additive pending counts/statuses, and
  optional Phase 35 run-level metadata (`timestamp`, `hostname`, `time`, and string-valued
  `properties`). Phase 21 documents this shape in `docs/schemas/run-report-v1.schema.json` and
  provides golden report examples in `docs/examples/reports/`; Phase 22 adds pending goldens and
  keeps `pending` optional, while Phase 35 keeps `metadata` optional for additive compatibility with
  older schemaVersion 1 reports.
- `JUnitXmlReportWriter` writes a single UTF-8 JUnit XML-compatible `<testsuite name="javaspec">`
  from `RunResult`, mapping FAILED to `<failure>`, BROKEN to `<error>`, and SKIPPED/PENDING to
  `<skipped>` without introducing JUnit or XML/reporting library runtime dependencies; Phase 18 adds
  `<testcase>` `file`/`line` attributes when source data is available, and Phase 35 adds testsuite
  `timestamp`, `hostname`, run-level `time`, and an immediate `<properties>` block from
  `ReportMetadata`. The testsuite `skipped` attribute includes skipped plus pending, and pending
  messages use `Pending: <reason>` or `Pending by javaspec.`.
- No-spec runs with requested reports produce valid empty reports; passing, failing, broken,
  skipped-only, and pending-only runs write requested reports after normal output; dry-run pending
  generation/update, profile compatibility violations, opt-in CLI compilation failures, and
  bootstrap execution failures exit before report writing and do not write reports. Report
  destinations can come from CLI options or Phase 24 config defaults; destination precedence does
  not change writer behavior.
- Report write failures are I/O failures and cause exit `70` with path diagnostics.

`io.github.jvmspec.invocation` contains the Phase 14 no-`System.exit` invocation contract:

- `JavaspecInvocation` carries either a `SpecDiscoveryRequest` or pre-discovered specs, a selected
  `ClassLoader`, and stop-on-failure behavior.
- `JavaspecLauncher` runs canonical discovery when requested, delegates execution to `SpecRunner`,
  and returns `JavaspecInvocationResult`.
- `JavaspecInvocationResult` exposes immutable discovered specs, the `RunResult`, the derived exit
  code, and convenience success/failure helpers.
- `JavaspecExitCode` maps passing, skipped/pending-only, and no-spec runs to `0`, and failed or
  broken runs to `1`.

`io.github.jvmspec.diagnostics` contains the Phase 23 execution-availability diagnostics contract:

- `RunDiagnostics.executionAvailabilityLines(RunResult)` returns deterministic human-readable lines
  for non-executable specs and missing/stale compiled example methods.
- The helper excludes explicit user `@Skip` and `PENDING` results so diagnostics distinguish
  classpath/compilation availability from intentional non-execution.

`io.github.jvmspec.extension` contains the minimal Phase 11 extension contract:

- `JavaspecExtension` defines `configure(ExtensionContext)` and the default
  `register(ExtensionContext)` alias.
- `Extension` is a short-name alias for `JavaspecExtension` and an alias ServiceLoader service type.
- `ExtensionContext` exposes the run formatter registry through `runFormatterRegistry()` and
  `runFormatters()`.
- `JavaspecExtensionLoader` loads a `RunFormatterRegistry` with built-ins first plus ServiceLoader
  providers from a supplied or default classloader.
- `ExtensionLoadingException` reports invalid service declarations, invalid formatter providers, and
  extension configuration failures.
- Package scanning, plugin lookup, and automatic classpath repair remain outside this increment;
  configuration-driven extension activation and formatter controls are handled by the later
  known-limitations resolution phases where implemented.
