# 6. Runtime View

This section describes the implemented runtime and verification scenarios without C4 diagrams. The
runtime is a Java 8-compatible CLI and library surface with no third-party runtime dependencies,
including Phase 14 no-JUnit invocation, explicit classpath execution, JUnit XML-compatible
reporting, the Phase 15 standalone optional Maven plugin adapter, the Phase 16 standalone optional
Gradle plugin adapter, the Phase 17 standalone optional JUnit Platform engine adapter, the Phase 18
stable identifier/source-location/report polish increment, the Phase 19 aggregate release/CI
verification workflow, the Phase 20 release-readiness scaffolding, the Phase 21 standalone adoption
examples/report documentation assets, the Phase 22 explicit skipped/pending semantics, the Phase 23
classpath/execution availability diagnostics, the Phase 24 configuration-level report destinations,
the Phase 25 ServiceLoader external formatter/extension discovery increment, the Phase 26
target-profile enforcement increment, the Phase 27 bootstrap hook execution increment, the Phase 28
stronger-interface-doubles increment, the Phase 29 opt-in CLI compilation increment, Phases 30-36
known-limitations resolution, and the Phase 37 optional bytecode-doubles adapter.

## 6.1 `describe` Scenario

1. The user runs `javaspec describe <ClassName>` or `javaspec desc <ClassName>`.
2. The CLI parses command-line arguments, loads an optional restricted configuration file, selects
   the active suite, and applies the selected suite's spec root and naming convention. Command-line
   spec-root options override paths only. Config report destinations may be present and are
   accepted, but `describe` does not write reports.
3. The described Java name is validated through the model boundary.
4. The naming convention maps the production type to `*Spec.java` and `*SpecSupport.java` under the
   active spec package prefix.
5. Missing spec/support skeletons are written. Existing spec files are not overwritten; missing
   support files may still be created.
6. No production source is generated or updated. Run-only controls are rejected.

Important runtime invariant: `describe` is specification/support generation only. Production type,
constructor, factory, method, profile enforcement, optional compilation, execution, formatter,
report writing, and dry-run behavior belong to `run`.

## 6.2 `run` Discovery, Generation, and Execution Scenario

1. The user runs `javaspec run` with optional config, suite, path overrides, explicit classpath
   input, opt-in compilation, generation flags, filters, run controls, bootstrap hooks, or report
   paths from CLI/config.
2. The CLI loads inferred defaults or the configured suite. `--constructor-policy`, `--profile`,
   `--formatter`, `--report`/`--report-file`, and `--junit-xml`/`--junit-xml-file` override valid
   configured/default values when supplied; CLI `--profile` overrides config profile before
   enforcement. `--compile` enables CLI source/spec compilation, and `--compile-output <dir>`
   selects the output directory while implying `--compile` (default `target/javaspec-classes`).
   Top-level bootstrap hooks and selected-suite bootstrap hooks are combined with top-level entries
   first, selected-suite entries second, duplicates preserved, and ServiceLoader-discovered hook
   providers afterward in deterministic order. `--classpath` and `--classpath-file` build a selected
   explicit classloader when present; the formatter registry is loaded from the effective selected
   classloader after this selection.
3. `SpecDiscovery` scans the selected spec root using the active naming convention and filters by
   suite, class filters, and example filters. If no specs are discovered, CLI `run` skips
   compilation and bootstrap execution and writes requested no-spec reports as before.
4. Discovery extracts described production type metadata, kind markers, relationship markers,
   construction markers, factory construction markers, typed proxy/throw proxy calls, direct
   subject/setter calls, and public `void` `it_*`/`its_*` example metadata including method
   declaration source lines.
5. Profile enforcement checks discovered described types and related types against the effective
   target profile before existence checks, generation/update planning output, prompts, execution, or
   reports. Violations print `Profile compatibility error`, selected profile, spec/type, and
   reasons, then exit `64` with no file or report writes.
6. Existence checks inspect the source root and effective or selected explicit classloader for
   described production types and related types.
7. Generation planning determines missing or updatable work: related specs/support, production type
   skeletons, support updates, constructors, static factory skeletons, class-like method bodies,
   ordinary-interface declarations, annotation elements, and missing sealed-interface skeleton
   declarations plus nested implementation bodies.
8. If `--dry-run` is active, the CLI reports pending work without writing files, prompting, or
   compiling. Pending work exits `1`; if no pending work exists, execution may proceed without
   compilation even when `--compile` was supplied.
9. If `--generate` is active, supported missing generation/update work is written non-interactively.
   Otherwise `run` prompts before production generation/update where required.
10. After generation/update work completes without a declined or unavailable prompt, requested CLI
    compilation runs through the current JDK `javax.tools.JavaCompiler` before bootstrap/examples.
    It compiles all `.java` files under the effective source root and spec root, de-duplicated by
    normalized path. The compiler classpath is compile output directory, explicit CLI classpath
    entries, then current process `java.class.path`.
11. If the compiler API is unavailable, the CLI exits `64`. If compilation fails, the CLI prints
    `Compilation failed:`, exits `1`, skips bootstrap/example execution, and writes no reports.
12. Configured bootstrap hooks execute through `BootstrapRunner` from the effective, selected
    explicit, or compile-output-first classloader. Hook classes must implement
    `org.javaspec.bootstrap.BootstrapHook`, have a public no-argument constructor, and receive an
    immutable `BootstrapContext` with the run classloader and discovered specs.
13. Bootstrap failures print `Error: Bootstrap execution failed`, exit `64`, and stop before example
    execution and report writing.
14. The reflection runner attempts to load compiled spec classes from the effective, selected
    explicit, or compile-output-first classloader.
15. Loadable specs execute filtered examples. Source-only or otherwise unavailable specs are marked
    `SKIPPED` with enriched execution-availability reasons unless CLI `--compile` successfully made
    them available.
16. Explicit `@Skip`/`@Pending` annotations or runtime skip/pending signals may mark examples
    `SKIPPED` or `PENDING`; pending is distinct from skipped in core results and excluded from
    availability diagnostics.
17. Human-readable output is rendered through the selected built-in or ServiceLoader-discovered run
    formatter. If `RunDiagnostics.executionAvailabilityLines(RunResult)` returns lines, the CLI
    prints an `Execution diagnostics:` block with either current-process-classloader guidance,
    compile-output guidance, or explicit classpath entry count guidance. Optional JSON and JUnit
    XML-compatible reports are written after no-spec output or runner summary rendering when the run
    reaches reportable execution/no-spec handling; effective report destinations can come from
    config or CLI, and reports include stable ids, source metadata, pending counts/statuses, and
    Phase 35 run-level metadata/properties where available.
18. The process exits with the documented code: `0`, `1`, `64`, or `70`; execution diagnostics do
    not change exit-code semantics.

## 6.3 Example Execution Scenario

The reflection runner uses source-discovered metadata as the execution source of truth:

1. For each filtered `DiscoveredSpec`, the runner attempts to load the compiled spec class from the
   run classloader, which may be the effective classloader, selected explicit classloader, or CLI
   compile-output-first classloader.
2. A non-loadable spec yields skipped examples with a not-executable reason explaining that the
   compiled spec class or one of its dependencies is unavailable to the runner classloader.
3. For each executable example, a fresh spec instance is created through the spec class no-argument
   constructor.
4. If the example method has `@Skip` or `@Pending`, the runner returns the skipped/pending result
   without constructing the spec or running lifecycle/body code; `@Skip` takes precedence when both
   annotations are present.
5. Optional public no-argument `let()` runs before the executable example.
6. The public no-argument example method runs.
7. Optional public no-argument `letGo()` runs after the example, including after failures or runtime
   skip/pending signals.
8. `AssertionError` is reported as `FAILED`.
9. Non-assertion throwables from lifecycle, example body, instantiation, or reflection are reported
   as `BROKEN`.
10. Runtime `SkipExampleException` or `PendingExampleException` from `let()` or the example marks
    the example after successful `letGo()`; `letGo()` failure after such a signal is `BROKEN`.
11. Missing reflected example methods are reported as `SKIPPED` with an availability reason
    explaining that the discovered source may not match the compiled specification class.
12. With `--stop-on-failure`, execution stops after the first FAILED or BROKEN executable example.

## 6.4 Construction and Typed Matcher Scenario

1. Generated support classes extend `ObjectBehavior<Subject>` and pass `Subject.class` to the base
   class.
2. The subject is constructed lazily on first access.
3. `beConstructedWith(...)`, `beConstructedThrough(...)`, `beConstructedNamed(...)`, and
   `beConstructedThroughNamed(...)` configure construction before instantiation.
4. A later construction rule before first subject access overrides an earlier rule. Changing
   construction after instantiation is an error.
5. `shouldThrow(...).duringInstantiation()` captures constructor or factory failures.
6. Generated support methods expose typed subject proxies such as `getRating().shouldReturn(5)` and
   typed throw proxies such as `shouldThrow(...).duringSetRating(-3)`.
7. Explicit `match(actual).should...` usage remains available and shares matcher behavior with typed
   proxies.

## 6.5 Method Generation Scenario

Method generation is driven by discovered construction and typed subject syntax:

- `beConstructedWith(...)` describes constructors.
- `beConstructedThrough("name", ...)`, `beConstructedNamed("name", ...)`, and
  `beConstructedThroughNamed("name", ...)` describe static factory methods when the name is a string
  literal and valid Java identifier.
- Typed proxy, throw proxy, direct `subject().method(...)`, and simple setter calls can describe
  missing instance methods.

Generation output depends on the described production kind:

- **Class, final class, sealed class, enum, record**: Java method bodies with Java default returns
  where supported.
- **Ordinary interface**: Non-static method declarations ending in `;`; static descriptors are
  skipped.
- **Annotation**: Compatible no-argument non-static elements; incompatible descriptors are ignored.
- **Missing sealed interface**: Root declarations plus nested permitted implementation bodies with
  Java default returns.
- **Existing sealed interface**: Source-preserving root declaration updates plus nested permitted
  implementation method bodies where supported.

## 6.6 Interface Double Scenario

1. User code creates a double for an ordinary interface through `Doubles` or `ObjectBehavior`
   convenience APIs.
2. The doubles engine validates that the target is an ordinary interface.
3. A JDK dynamic proxy records calls through the invocation handler.
4. Stubs are resolved by method name with any arguments or by method name with argument constraints.
   Argument constraints may contain ordinary exact values or `ArgumentMatcher` values from
   `ArgumentMatchers` / `Doubles` aliases such as `any()`, nullable `any(Class<?>)`, `isNull()`,
   `notNull()`, and array-aware `eq(...)`.
5. Argument-constrained stubs, including matcher patterns, take priority over method-wide stubs;
   within one priority, the newest matching stub wins.
6. A matching stub may return a validated value, throw a configured throwable, or invoke a
   `StubAnswer` callback with an immutable `DoubleInvocation` context. Calls are recorded before
   returns, throws, answer results, or default values are produced.
7. Unstubbed methods return Java defaults.
8. Call snapshots can be inspected and verified for called, not called, called once, or exact count,
   using the same matcher-aware vararg semantics as stubbing. `Call.hasArguments(...)` is
   matcher-aware while ordinary exact values, `null`, and array-content comparison remain supported.
9. `toString`, `equals`, and `hashCode` are handled deterministically and are not treated as user
   collaborator calls.

Unsupported core target kinds fail fast with diagnostics. Non-final concrete-class doubles are
available only through the standalone bytecode adapter; final, static, and constructor mocking
remain unsupported.

### 6.6.1 Optional Bytecode Concrete-Class Double Scenario

1. A test runtime includes `javaspec-bytecode-doubles` in addition to core.
2. `Doubles.concreteDouble(DataStore.class)` asks the core `ConcreteDoubleProvider` registry for a
   provider through `ServiceLoader`.
3. The ByteBuddy provider accepts a non-final concrete class and generates a subclass that delegates
   eligible calls to the core double invocation handler.
4. The returned `InterfaceDouble<DataStore>` exposes the same `control()`, stub, call-history, and
   verification APIs as interface doubles.
5. If no provider is registered, core throws `IllegalStateException` with guidance. If the target is
   final, enum, array, annotation, primitive, or an interface, creation is rejected clearly.

## 6.7 Reporting and Extension Runtime Scenario

- CLI output is selected from built-in `progress`/`pretty` or ServiceLoader-discovered formatter
  names and rendered through `RunFormatter` implementations registered in `RunFormatterRegistry`.
- `--report` and `--report-file` write UTF-8 JSON reports with `schemaVersion` 1 from the immutable
  runner result model, including stable spec/example ids, source file/line fields where available,
  separate `pending` counts, `PENDING` example statuses, and Phase 35 run-level `metadata`
  (`timestamp`, `hostname`, `time`, and string-valued `properties`) from default writers. The
  metadata object is optional for schemaVersion 1 additive compatibility. Config aliases `report`,
  `reportFile`, `report-file`, `jsonReport`, `jsonReportFile`, and `json-report-file` provide
  defaults when CLI JSON report options are absent.
- `--junit-xml` and `--junit-xml-file` write UTF-8 JUnit XML-compatible reports from the same
  `RunResult` without JUnit dependencies; Phase 35 default writers add testsuite `timestamp`,
  `hostname`, run-level `time`, and an immediate `<properties>` block, while testcase `file`,
  `line`, and `time="0"` attributes are included as before when source data is available. Config
  aliases `junitXml`, `junit-xml`, `junitXmlFile`, `junit-xml-file`, `junitXmlReportFile`, and
  `junit-xml-report-file` provide defaults when CLI JUnit XML report options are absent. Both
  `SKIPPED` and `PENDING` map to `<skipped>`, the testsuite skipped attribute includes skipped plus
  pending, and pending messages use `Pending: <reason>` or `Pending by javaspec.`.
- JSON and JUnit XML-compatible reports can be requested together from CLI options, config
  destinations, or a mix of both; CLI options override config values.
- No-spec, passing, failing, broken, skipped-only, and pending-only runs write requested reports
  after normal output. Dry-run pending generation/update, profile compatibility violations, opt-in
  CLI compilation failures, and bootstrap execution failures exit before report writing and do not
  write reports.
- Report write failures are I/O failures and exit `70` with path diagnostics.
- Verbose run configuration shows effective JSON/JUnit XML-compatible report paths whether they came
  from config or CLI.
- `RunDiagnostics.executionAvailabilityLines(RunResult)` derives deterministic human-readable
  availability diagnostics for non-executable specs and missing/stale compiled example methods while
  excluding explicit `@Skip` and `PENDING` results.
- `JavaspecExtension`/`Extension` and `ExtensionContext` support formatter registration.
  `JavaspecExtensionLoader.loadRunFormatterRegistry()` and `loadRunFormatterRegistry(ClassLoader)`
  load built-ins first, then ServiceLoader providers for `org.javaspec.formatter.RunFormatter`,
  `org.javaspec.extension.JavaspecExtension`, and alias service type
  `org.javaspec.extension.Extension`.
- Duplicate extension implementation classes listed under both extension service types are
  configured once. Invalid providers raise `ExtensionLoadingException` with clear diagnostics.
- ServiceLoader discovery is classpath-based only; configuration-driven activation and adapter
  formatter controls are implemented where documented, while package scanning, plugin lookup, and
  automatic classpath repair are not implemented. Bootstrap hooks are explicit config class names
  plus ServiceLoader providers, not scripts, generated classes, package scans, or
  dependency-resolved plugins.

## 6.8 Explicit Classpath Runtime Scenario

1. `run --classpath <path-list>` splits the path list by `File.pathSeparator` and trims entries.
2. `run --classpath-file <file>` reads UTF-8 lines, ignores empty lines and lines whose trimmed form
   begins with `#`, and treats remaining lines as entries.
3. The CLI creates a `URLClassLoader` over explicit entries with the existing effective classloader
   as parent.
4. Type existence checks, ServiceLoader formatter/extension discovery, bootstrap hook class loading,
   and `SpecRunner` execution use the selected explicit classloader unless successful CLI
   compilation creates a compile-output-first classloader.
5. `--verbose` lists explicit entries.
6. If execution availability diagnostics exist, the CLI reports the explicit classpath entry count
   and asks the user to verify compiled spec classes and dependencies; without explicit entries, it
   reports that the current process classloader was used and suggests `--classpath`,
   `--classpath-file`, or CLI `--compile` for source-only runs.
7. Invalid classpath-file reads are I/O failures and exit `70`; classpath options are rejected by
   `describe` as run-only.

### 6.8.1 Opt-in CLI Compilation Runtime Scenario

1. `run --compile` enables the CLI compilation step. `run --compile-output <dir>` selects the output
   directory and implies `--compile`; when no output is supplied, the directory is
   `target/javaspec-classes`.
2. Compilation is explicit opt-in. No configuration keys are added; programmatic, Maven, and Gradle
   paths have separate opt-in settings, while the JUnit Platform engine remains host-classpath
   based.
3. The current JDK `javax.tools.JavaCompiler` is obtained from `ToolProvider`; javaspec does not
   fork `javac` and does not add compiler dependencies.
4. If no specs are discovered, the CLI skips compilation and writes requested empty reports. If
   `--dry-run` is active, the CLI skips compilation and creates no compile output.
5. After discovery, profile enforcement, generation/update decisions, and any successful writes, the
   compiler receives all `.java` files under the effective source root and effective spec root,
   de-duplicated by normalized path.
6. Compiler classpath order is compile output directory, explicit CLI classpath entries, then
   current process `java.class.path`.
7. Successful compilation prints the compiled source count and output directory, then creates an
   execution classloader whose URL entries put the output directory before explicit CLI classpath
   entries; the current process classloader remains the parent.
8. If the compiler API is unavailable, the CLI exits `64`. If compilation fails, the CLI prints
   `Compilation failed:`, exits `1`, skips bootstrap/examples, and writes no reports.
9. The feature does not resolve dependencies, manage source/release levels, maintain an incremental
   cache, change report schemas, or change Maven/Gradle/JUnit adapter behavior.

## 6.9 Programmatic Invocation Runtime Scenario

1. A host process creates `JavaspecInvocation` from either a `SpecDiscoveryRequest` or
   pre-discovered `DiscoveredSpec` values, supplies a `ClassLoader`, and may add bootstrap hook
   class names.
2. `JavaspecLauncher` runs canonical discovery when needed, executes configured bootstrap hooks
   through `BootstrapRunner`, and delegates execution to `SpecRunner`.
3. The launcher returns `JavaspecInvocationResult` with discovered specs, `RunResult`, and an exit
   code, without calling `System.exit`.
4. `JavaspecExitCode` maps passing, skipped/pending-only, and no-spec runs to `0`, and failed or
   broken runs to `1`.
5. The invocation API remains classpath-based and does not compile source/spec files itself.
6. Host tools can call `RunDiagnostics.executionAvailabilityLines(result.runResult())` to obtain the
   same deterministic availability lines used by CLI/Maven/Gradle diagnostics.
7. Host tools that need built-in plus ServiceLoader-provided formatters can call
   `JavaspecExtensionLoader.loadRunFormatterRegistry(classLoader)` with the same classloader, and
   can opt into compilation through `JavaspecInvocation.withCompilation(...)`.

## 6.10 Optional Maven Plugin Runtime Scenario

1. A Maven build configures or invokes the optional
   `io.github.jvmspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` artifact and its `javaspec:run` goal.
2. The Mojo participates as a standalone Maven plugin, not as a root repository module. Its default
   phase is `verify`, and it requires Maven test dependency resolution.
3. Maven supplies the test classpath; compiled production/spec classes from the project under test
   are passed through the plugin adapter to the canonical javaspec invocation path.
4. The Mojo applies config/suite/specDir/specRoot selection, class/example filters, top-level plus
   selected-suite bootstrap hooks, `stopOnFailure`, `skip`, `failOnFailure`, JSON report settings,
   and JUnit XML-compatible report settings. Configured report destinations are defaults when
   explicit plugin report settings are absent; explicit plugin settings override config values.
   Formatter selection is available through config or `javaspec.formatter`, and extension activation
   through config or `javaspec.extensions`, where implemented.
5. Output and diagnostics are emitted through Maven logging. When execution availability lines
   exist, the Mojo logs `javaspec:` warnings and the Maven test classpath element count. Bootstrap
   failures fail the build with clear `javaspec bootstrap execution failed` diagnostics.
6. The Mojo delegates to `org.javaspec.invocation.JavaspecLauncher`, receives structured results,
   and avoids `System.exit` and direct low-level runner coupling.
7. No JUnit is required in the project under test. Source/spec compilation remains Maven's
   responsibility by default; `javaspec.compile`, `javaspec.compileOutput`, or
   `javaspec.compileOutputDirectory` explicitly opt into current-JDK javaspec compilation.

## 6.11 Optional Gradle Plugin Runtime Scenario

1. A Gradle build applies the optional `io.github.jvmspec` plugin artifact from `javaspec-gradle-plugin/`
   when that standalone artifact is available to Gradle.
2. The plugin registers extension `javaspec` and task `javaspecRun` in Gradle's `verification`
   group.
3. When the Gradle Java plugin/source sets are present, `javaspecRun` uses the `test` source set
   runtime classpath by default and depends on `testClasses`.
4. The task applies extension/task/project-property options for `skip`, `failOnFailure`,
   `stopOnFailure`, `configFile`, `suite`, `specDir`/`specRoot`, class/example filters, top-level
   plus selected-suite bootstrap hooks, built-in or ServiceLoader-discovered formatter selection,
   JSON report aliases, and JUnit XML-compatible report aliases. Formatter precedence is task
   setting, extension setting, project property `javaspec.formatter`, config formatter, then default
   `progress`. Configured report destinations are defaults when explicit extension/task report
   settings are absent; explicit Gradle adapter settings override config values.
5. The task loads javaspec configuration when configured, selects suites, builds
   `SpecDiscoveryRequest` with `SpecNamingConvention`, creates a `URLClassLoader` over the Gradle
   classpath, loads built-in plus ServiceLoader formatter/extension providers from that run
   classloader, passes bootstrap hooks into canonical invocation, sets and restores the thread
   context classloader, and closes the loader.
6. The task delegates to canonical no-JUnit `JavaspecLauncher`, renders selected formatter output
   through Gradle logging, writes reports via core writers, logs through Gradle, logs `javaspec:`
   warning diagnostics plus the Gradle classpath element count when execution availability lines
   exist, throws `GradleException` for bootstrap failures with clear `javaspec bootstrap execution
   failed` diagnostics, and throws `GradleException` for failed or broken examples when
   `failOnFailure=true`.
7. No JUnit is required in the project under test. Source/spec compilation remains Gradle's
   responsibility by default; `compile` / `javaspec.compile` and `compileOutput` /
   `javaspec.compileOutput` explicitly opt into current-JDK javaspec compilation.

## 6.12 Optional JUnit Platform Engine Runtime Scenario

1. A JUnit Platform launcher has the optional
   `io.github.jvmspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT` artifact, compiled spec classes,
   production classes, and dependencies on its test runtime classpath.
2. JUnit Platform discovers `org.javaspec.junit.platform.JavaspecTestEngine` through
   `META-INF/services/org.junit.platform.engine.TestEngine`; the engine id is `javaspec`.
3. The engine reads supported configuration parameters: `javaspec.configFile`, `javaspec.suite`,
   `javaspec.specDir`/`javaspec.specRoot`, `javaspec.classFilters`/`classFilter`/`class`,
   `javaspec.exampleFilters`/`exampleFilter`/`example`, and `javaspec.stopOnFailure`.
4. The engine builds canonical `SpecDiscoveryRequest` input and runs `SpecDiscovery`. JUnit Platform
   class, package, method, and unique-id selectors are applied as filters over canonical discovery
   results.
5. Descriptors use UniqueId segments `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and
   `[example:<methodName>]`; Phase 18 retains this stable shape and MethodSource behavior while
   aligning descriptor reporting to stable ids.
6. Execution delegates to canonical no-JUnit `JavaspecLauncher` using discovered specs and avoids
   `System.exit`.
7. javaspec result states are mapped to JUnit Platform listener events: passed to successful, failed
   assertion results to failed assertion-style throwables, broken results to failed/error-style
   throwables, and skipped, pending, or non-loadable results to `executionSkipped`; pending reasons
   are prefixed with `Pending:`.
8. The engine does not compile source/spec files, does not require changes to javaspec spec
   authoring style, and remains an optional IDE/CI adapter only.

Projects that do not opt into the engine still use CLI, programmatic invocation, Maven plugin, or
Gradle plugin no-JUnit execution paths without adding a JUnit dependency.

## 6.13 Profile Enforcement and Compatibility Probe Scenario

1. The active profile is loaded from defaults, config, or `--profile`; CLI `--profile` overrides
   config.
2. Profile values are validated against `java8`, `java11`, `java17`, `java21`, and `java25`.
3. Profile metadata and API-symbol catalog lookups use strings and Java 8-compatible domain objects.
4. `ProfileEnforcement` checks described type kinds before writes: Java 8-compatible kinds pass
   under `java8`, while `record`, `sealed class`, and `sealed interface` require at least `java17`.
5. Generated method return/parameter types and relationship type references such as
   super/extends/implements/permits are checked when their owners resolve to known cataloged Java
   API symbols; unknown project types and ambiguous or unresolvable names are ignored to avoid false
   positives.
6. Violations exit `64` before generation/update writes, dry-run output, execution, or report
   writing.
7. Optional runtime availability checks use `ApiAvailabilityProbe` with class, method, or field
   names.
8. Post-Java-8 APIs are never imported directly by production code, and no integrated compilation is
   performed.

## 6.14 Aggregate Verification, Release-Readiness, and Examples Scenario

1. A maintainer runs `scripts/check-version-alignment.sh` directly for release-readiness version
   checks, runs `scripts/verify-examples.sh` directly for standalone examples, or runs
   `scripts/verify-all.sh` locally for the aggregate path. GitHub Actions is configured to run the
   aggregate script in the Java 21 `full-verification` job with `JAVASPEC_GRADLE_BIN=gradle`.
2. `scripts/check-version-alignment.sh` verifies the root Maven project version, standalone Maven
   plugin version, standalone JUnit Platform engine version, Gradle plugin `version`, and Gradle
   plugin `javaspecCoreVersion` against one baseline.
3. `scripts/verify-all.sh` runs the version-alignment check first, resolves the repository root from
   its own path, and uses `MAVEN_BIN` or default `mvn` for Maven commands.
4. Root `mvn -q verify` and root `mvn dependency:tree -Dscope=runtime` verify the
   zero-runtime-dependency core artifact only.
5. Root `mvn -q -DskipTests install` refreshes the local core snapshot for standalone adapters.
6. The script verifies and audits the standalone Maven plugin and standalone JUnit Platform engine
   with their own Maven POMs.
7. Unless `JAVASPEC_SKIP_GRADLE=1` is set, the script resolves Gradle through explicit
   `JAVASPEC_GRADLE_BIN`, repository `./gradlew`, `/tmp/gradle-8.8/bin/gradle`, or `gradle` on
   `PATH`, then runs the standalone Gradle plugin `clean test build` and `runtimeClasspath` audit.
8. If Gradle is required but unavailable, the script fails with a clear diagnostic rather than
   silently skipping adapter verification.
9. Unless `JAVASPEC_SKIP_EXAMPLES=1` is set, `scripts/verify-all.sh` runs
   `scripts/verify-examples.sh` after core and adapter checks. The examples script installs local
   snapshots, runs Maven, JUnit Platform, bytecode doubles, and Gradle consumer examples, and
   asserts generated report markers. `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` skips only the Gradle example
   inside that examples script.
10. Optional local release-artifact checks use Maven `-Prelease-artifacts -DskipTests package` for
    root, Maven plugin, and JUnit Platform engine main/sources/javadoc jars, plus the Gradle plugin
    `clean test build` for Gradle main/sources/javadoc jars.
11. `CHANGELOG.md` and `RELEASING.md` document release changes, local checks, and public-publication
    blockers.

The GitHub Actions workflow also has a separate core matrix job for Java 8, 11, 17, 21, and 25 that
runs root core verification and root runtime dependency audit. The workflow has no
publishing/signing steps and uses no secrets. Phase 19 remote GitHub Actions success is
user-/maintainer-confirmed for HEAD `4d30e63` on `develop`; after Phase 20/21/22 were pushed, remote
GitHub Actions success for HEAD `5088e96` on `develop` is also user-/maintainer-confirmed. No GitHub
run IDs, URLs, durations, or logs were independently queried from this environment. The MIT license
and maintainer metadata are resolved, but public publication remains postponed until GPG signing,
Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag,
and final publish approval are resolved.

## 6.15 Standalone Examples and Report Documentation Scenario

1. A new adopter reads `examples/README.md` and chooses a standalone consumer example: Maven plugin,
   Gradle plugin, JUnit Platform engine, or bytecode doubles.
2. The example builds consume local snapshots because public artifacts are not published yet.
3. Maven, Gradle, and bytecode doubles examples run simple specs and write JSON plus JUnit
   XML-compatible reports to their own generated output directories.
4. The JUnit Platform example runs through Maven Surefire configured for `*Spec` and the optional
   engine; the bytecode doubles example uses `javaspec-bytecode-doubles` to double a non-final
   concrete `DataStore`.
5. Report tooling authors can validate against `docs/schemas/run-report-v1.schema.json` and compare
   with passing and pending golden reports under `docs/examples/reports/`.
6. Generated example `target/`, `build/`, and `.gradle/` outputs remain ignored and are not
   source-controlled artifacts.
