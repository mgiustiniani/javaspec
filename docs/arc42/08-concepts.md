# 8. Concepts

## 8.1 Java 8-Compatible Core

All production code is written for Java 8 source/target compatibility. Newer JDK capabilities are modeled as profile metadata, generated source text, or reflection-only probes. This keeps the runtime binary usable on Java 8 while allowing javaspec to understand Java 11, 17, 21, and 25 concepts.

## 8.2 Zero Runtime Dependencies

The core runtime depends only on the JDK. This affects every feature:

- Configuration uses a restricted internal line-based parser instead of YAML/TOML/JSON libraries.
- Doubles use JDK dynamic proxies instead of bytecode libraries.
- JSON reports are written by an internal UTF-8 writer instead of a JSON library, including stable id/source fields added in Phase 18 and pending counts/statuses added in Phase 22.
- JUnit XML-compatible reports are written internally instead of using JUnit or XML/reporting libraries, with testcase file/line attributes when source data is available and skipped-element mapping for both skipped and pending examples.
- CLI parsing, explicit classpath handling, formatting, matchers, invocation APIs, and extension contracts are implemented with JDK APIs.
- Optional adapters stay outside the core runtime; the Phase 15 Maven plugin uses Maven APIs as plugin-provided/build-tool dependencies, the Phase 16 Gradle plugin uses Gradle plugin APIs in its standalone artifact, and the Phase 17 JUnit Platform engine uses JUnit Platform APIs in its standalone artifact. Phase 19 release/CI verification assets, Phase 20 release-readiness scaffolding, and Phase 21 adoption/report assets invoke, package, or verify those standalone artifacts explicitly instead of adding their dependencies to the core runtime. Projects that do not opt into the JUnit Platform engine keep no-JUnit execution paths.

## 8.3 PHPSpec-Inspired Java Workflow

javaspec keeps the PHPSpec workflow shape but adapts it to Java:

| PHPSpec concept | javaspec Java concept |
|---|---|
| `describe` command | `javaspec describe` / `desc` creates Java spec/support skeletons only. |
| Subject as `$this` | `ObjectBehavior<T>` lazy subject plus generated typed support methods and explicit `subject()`. |
| PHP namespaces | Java packages plus configurable spec and production package prefixes. |
| Examples | Public `void` Java methods named `it_*` or `its_*`; explicit `@Skip`/`@Pending` annotations or runtime signals can mark examples skipped or pending. |
| Construction customization | `beConstructedWith(...)`, `beConstructedThrough(...)`, `beConstructedNamed(...)`, and `beConstructedThroughNamed(...)` before subject instantiation. |
| Matcher syntax | `getValue().shouldReturn(...)`, `match(value).should...`, and direct `ObjectBehavior` convenience assertions. |
| Collaborator doubles | Interface-only JDK-proxy doubles in the zero-dependency core. |
| Generation prompts | Production generation/update belongs to `run`, gated by confirmation or `--generate`; `--dry-run` reports planned work without writing. |
| No-JUnit execution | CLI, programmatic invocation, and optional Maven/Gradle plugins run compiled specs through the canonical javaspec runner without requiring JUnit. |
| Optional JUnit Platform execution | The standalone optional engine exposes canonical javaspec discovery/execution to JUnit Platform tools without changing spec authoring style or adding JUnit dependencies to core. |

The user manual contains practical migration notes for PHPSpec users.

## 8.4 Describe/Run Ownership Split

The architecture preserves a strict command split:

- `describe` creates or finds specification/support files and never writes production source.
- `run` discovers specs, handles generation/update planning, prompts or uses `--generate`, accepts explicit compiled-class classpath input, executes compiled examples when available, renders output, writes optional JSON and/or JUnit XML-compatible reports, and returns stable exit codes.
- The optional Maven `javaspec:run` goal, optional Gradle `javaspecRun` task, and optional JUnit Platform `javaspec` engine are adapters over the same canonical runner and result model, using host-supplied classpaths/selectors rather than replacing runner semantics.

This split is central to ADR 0003 and ADR 0008.

## 8.5 Configuration and Suite Naming

Configuration is suite-oriented. Each suite provides spec/source roots and package-prefix naming metadata. The active `SpecNamingConvention` maps production classes to spec/support classes and maps discovered spec classes back to described production types.

Path options can override selected-suite roots, but naming still comes from the selected suite. Constructor policy, profile, and formatter defaults are loaded from config and can be overridden by run CLI options. Bootstrap hooks are parsed metadata only and are not executed yet.

## 8.6 Construction Semantics

Generated support classes pass the described subject `Class<T>` to `ObjectBehavior<T>`. The subject is constructed lazily on first access, so construction rules can be configured before instantiation.

Rules:

- `beConstructedWith(...)` describes constructor arguments.
- `beConstructedThrough(...)`, `beConstructedNamed(...)`, and `beConstructedThroughNamed(...)` describe static factories at runtime and generate factory skeletons only when the factory name is a string-literal valid Java identifier.
- The last construction rule before instantiation wins.
- Changing construction after instantiation is an error.
- `shouldThrow(...).duringInstantiation()` specifies constructor or factory exceptions.

## 8.7 Matcher and Assertion Semantics

`Matchable<T>` is the fluent expectation wrapper for typed proxies and explicit `match(actual)` calls. It includes the implemented subset of equality/identity aliases, negations, type/instance/implementation checks, containment, string helpers, count/empty helpers, and map key/value helpers.

`ObjectBehavior` direct convenience assertions delegate through `match(actual)`, which keeps direct and fluent matcher semantics synchronized. Custom matchers can be registered without adding runtime dependencies and may evaluate null subjects.

Known matcher limitation: count and emptiness checks on a generic `Iterable` consume the iterable and can hang on infinite iterables.

## 8.8 Generation Semantics

Generation is deterministic and reviewable:

- Missing production class-like type skeletons are generated only by `run` after confirmation or `--generate`.
- Constructor handling follows the selected policy: `comment` (default), `preserve`, or `delete`; destructive deletion requires explicit `delete`.
- Empty generated/no-op unmatched constructors may be removed when safe.
- Method generation uses Java default returns for generated method bodies.
- Interface-style generation emits declarations or annotation elements where valid.
- Missing sealed-interface skeletons include nested permitted implementation bodies; existing sealed-interface updates are deferred.

Source parsing/generation uses Java 8-compatible heuristics rather than a full Java parser.

## 8.9 Runner, Results, Invocation, Formatters, and Reports

The runner result model separates discovery and execution from output and process termination:

- `SpecRunner` produces immutable `RunResult`, `SpecResult`, and `ExampleResult` data.
- Explicit `@Skip`/`@Pending` annotations are resolved before instantiation/lifecycle/body execution; `@Skip` takes precedence over `@Pending`.
- Runtime `SkipExampleException`/`PendingExampleException` from `let()` or an example mark the example skipped/pending after successful `letGo()`; `letGo()` failure after such a signal is `BROKEN`.
- `PENDING` is a distinct `ExampleStatus`; `skippedCount()` remains skipped-only, `pendingCount()` is separate, and skipped-plus-pending helpers support JUnit-compatible report adapters.
- `DiscoveredSpec`, `SpecResult`, and `ExampleResult` expose stable id aliases; example stable ids use `<specQualifiedName>#<methodName>` and match `fullName()`.
- `SpecExample`, `SpecResult`, and `ExampleResult` carry source metadata where discovery supplied it.
- `JavaspecLauncher` returns `JavaspecInvocationResult` for no-`System.exit` programmatic callers while reusing canonical discovery and `SpecRunner` semantics.
- Built-in `progress` and `pretty` output render results through `RunFormatter` implementations and include pending counts/details.
- JSON reports with `schemaVersion` 1 are written from the same results and include additive stable id/source fields plus pending counts and `PENDING` statuses.
- JUnit XML-compatible reports are also written from `RunResult`, mapping FAILED to failures, BROKEN to errors, and SKIPPED/PENDING to skipped test cases, with testcase file/line attributes when source data is available. The testsuite skipped attribute includes skipped plus pending and pending messages use `Pending: <reason>` or `Pending by javaspec.`.
- Report failures are I/O failures and exit `70` for CLI runs.

Source-only or non-loadable compiled spec classes produce skipped examples because javaspec is not an in-process compiler. CLI `--classpath` / `--classpath-file` and programmatic invocation classloaders can supply compiled classes explicitly, but the entries must already be compiled.

## 8.10 Interface Doubles Concept

Core doubles intentionally support ordinary Java interfaces only. The design favors explicit limits over hidden dependencies:

- JDK dynamic proxies implement interface doubles.
- Stubbing is by method name or exact arguments.
- Calls are recorded as immutable snapshots.
- Verification supports called/not-called/called-once/exact-count checks.
- Unstubbed methods return Java defaults.

Concrete class, final class, static method, constructor, primitive, array, annotation, and enum doubles are outside the core runtime.

## 8.11 Explicit Classpath and No-JUnit Integration Boundary

Phase 14 makes no-JUnit execution first-class without changing compilation ownership, and Phases 15 through 17 use that boundary for optional Maven, Gradle, and JUnit Platform adapters:

- `--classpath` accepts a `File.pathSeparator`-separated path list.
- `--classpath-file` reads UTF-8 non-empty, non-comment entries.
- The selected classloader is used for type existence checks and spec execution.
- `org.javaspec.invocation` allows host processes to provide a discovery request or pre-discovered specs and a classloader, then receive structured results.
- Passing, skipped/pending-only, and no-spec invocation paths map to exit code `0`; failed or broken paths map to `1`.
- Neither CLI nor programmatic invocation compiles source/spec files.
- `JavaspecRunMojo` delegates to `JavaspecLauncher` with Maven's test classpath and does not call `System.exit`.
- `JavaspecRunTask` delegates to `JavaspecLauncher` with the Gradle classpath, manages a `URLClassLoader` and thread context classloader, and does not call `System.exit`.
- `JavaspecTestEngine` delegates to `JavaspecLauncher` with discovered specs, applies JUnit Platform selectors as filters over canonical discovery, keeps the stable unique-id shape and MethodSource behavior, aligns descriptor reporting to stable ids, maps results to listener events, maps pending to `executionSkipped` with a `Pending:` reason, and does not call `System.exit`.

## 8.12 Optional Maven Plugin Boundary

The Phase 15 Maven plugin is an optional adapter artifact, not part of the core runtime artifact and not registered as a root module. This preserves root `mvn verify` as a core build/audit while allowing standalone plugin verification after the current core has been installed.

The plugin boundary principles are:

- The plugin packages `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin` with Java source/target `1.8` and goal prefix `javaspec`.
- Maven API and plugin annotations are `provided`; JUnit is only a plugin test dependency.
- The only plugin runtime dependency beyond the plugin itself is compile-scope core `org.javaspec:javaspec`.
- `javaspec:run` uses Maven test dependency resolution and the Maven test classpath.
- The Mojo supports config/suite/specDir/specRoot selection, class/example filters, stop/fail/skip controls, JSON reports, JUnit XML-compatible reports, and Maven logging.
- The Mojo delegates to canonical `JavaspecLauncher` and avoids `System.exit` or direct low-level runner coupling.
- Projects under test do not need JUnit.

## 8.13 Optional Gradle Plugin Boundary

The Phase 16 Gradle plugin is an optional adapter artifact, not part of the core runtime artifact and not registered as a root Maven module. This preserves root `mvn verify` as a core build/audit while allowing standalone plugin verification after the current core has been installed.

The plugin boundary principles are:

- The plugin uses `java-gradle-plugin`, group `org.javaspec`, version `0.1.0-SNAPSHOT`, Java source/target `1.8`, plugin id `org.javaspec`, and implementation class `org.javaspec.gradle.JavaspecPlugin`.
- The plugin depends on core `org.javaspec:javaspec:0.1.0-SNAPSHOT`; verified runtimeClasspath contains only that core dependency.
- JUnit and TestKit are plugin test dependencies only; projects under test do not need JUnit.
- `javaspecRun` uses the configured Gradle classpath and defaults to the Java plugin `test` source set runtime classpath plus `testClasses` dependency when source sets are present.
- The extension/task supports skip/fail/stop controls, config/suite/specDir/specRoot, class/example filters, built-in formatter selection, JSON reports, JUnit XML-compatible reports, and Gradle logging.
- The task delegates to canonical `JavaspecLauncher`, avoids `System.exit`, restores the thread context classloader, and closes its `URLClassLoader`.

## 8.14 Optional JUnit Platform Engine Boundary

The Phase 17 JUnit Platform engine is an optional adapter artifact, not part of the core runtime artifact and not registered as a root Maven module. This preserves root `mvn verify` as a core build/audit while allowing standalone engine verification after the current core has been installed.

The engine boundary principles are:

- The engine packages `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT` as a Java 8-compatible `jar` using JUnit Platform `1.10.2`, not JUnit Platform 6/JUnit 6.
- `JavaspecTestEngine` is registered by ServiceLoader with engine id `javaspec`.
- Runtime dependencies are isolated to the engine artifact: core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`.
- Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`, with configuration parameters and class/package/method/unique-id selectors acting as filters over canonical discovery results.
- UniqueId segments are `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and `[example:<methodName>]`; Phase 18 retains this shape and MethodSource behavior while aligning descriptor reporting to stable ids.
- Execution delegates to canonical no-JUnit `JavaspecLauncher`, avoids `System.exit`, maps javaspec outcomes to JUnit Platform listener events, and does not require changes to javaspec spec authoring style.
- Projects that do not opt into the engine still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths.

## 8.15 Release/CI Verification and Publication Boundary

Phase 19 keeps release verification non-disruptive, Phase 20 adds release-readiness scaffolding without public publication, Phase 21 adds adoption examples/report documentation without core runtime changes, and Phase 22 keeps skipped/pending semantics zero-dependency while updating docs/schema/goldens:

- Root `mvn verify` remains the core-only build and runtime dependency gate.
- `scripts/check-version-alignment.sh` verifies root Maven, standalone Maven plugin, standalone JUnit Platform engine, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` alignment.
- `scripts/verify-all.sh` is the aggregate local release check for core plus standalone adapters and examples; it runs version alignment first and standalone examples verification by default at the end.
- The script installs the current core snapshot locally before verifying standalone adapters.
- The script verifies Maven plugin and JUnit Platform engine artifacts through their own POMs and verifies the Gradle plugin with a resolved Gradle executable.
- `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, explicit `JAVASPEC_SKIP_GRADLE=1`, explicit `JAVASPEC_SKIP_EXAMPLES=1`, and `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` make tool selection and skips explicit.
- `.github/workflows/ci.yml` defines a Java 8/11/17/21/25 core matrix and a Java 21 full-verification job that runs the aggregate script with Gradle 8.8.
- `CHANGELOG.md` and `RELEASING.md` document release notes, checks, and blockers.
- Maven `release-artifacts` profiles and the Gradle plugin build provide local source/javadoc jar readiness checks only; they do not sign, stage, deploy, or publish.
- Safe URL, SCM, GitHub Issues, MIT license, and confirmed maintainer/developer metadata can be present.
- Standalone examples under `examples/`, `scripts/verify-examples.sh`, `docs/schemas/run-report-v1.schema.json`, and golden reports under `docs/examples/reports/` are adoption assets, not root modules or publication evidence; Phase 22 keeps those schema/goldens synchronized with pending-aware report output.
- No publishing, signing, secrets, mandatory Maven multi-module conversion, portal publication/credentials, final release version/tag, final publish approval, or Phase 20/Phase 21/Phase 22 remote CI success claim is part of the implemented increments.
- Public publication remains postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved.

## 8.16 Extension Boundary

The current extension API is programmatic. `JavaspecExtension`/`Extension` can configure an `ExtensionContext`, and the context exposes the run formatter registry.

Not implemented in the current architecture:

- Configuration-driven extension activation.
- External CLI extension discovery/loading.
- Classpath scanning.
- `ServiceLoader` integration for extensions.
- Plugin lookup.
- CLI formatter selection for extension-provided names.
