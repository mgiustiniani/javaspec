# 12. Glossary

- **ADR**: Architecture Decision Record. javaspec ADRs live in `docs/adr/` and follow the Context,
  Decision, Consequences structure.
- **Aggregate verification script**: `scripts/verify-all.sh`; Phase 19 local release verification
  script, extended in Phase 20 to run version alignment first and in Phase 21 to run standalone
  examples by default, that keeps root Maven verification core-only, installs the current core
  snapshot, verifies/audits standalone Maven plugin, Gradle plugin, and JUnit Platform engine
  artifacts explicitly, and then verifies adoption examples unless explicitly skipped.
- **Changelog**: `CHANGELOG.md`; Phase 20 release-readiness documentation for notable changes.
- **Version alignment check**: `scripts/check-version-alignment.sh`; Phase 20 script that checks
  root Maven, standalone Maven plugin, standalone JUnit Platform engine, Gradle plugin `version`,
  and Gradle plugin `javaspecCoreVersion` alignment.
- **Annotation element**: A no-argument method-like member of a Java annotation type. javaspec
  generates only compatible elements for annotation sources.
- **Argument matcher**: `org.javaspec.doubles.ArgumentMatcher`; a zero-dependency matcher for one
  interface-double argument. Factory aliases include `any()` / `anyArgument()`, nullable
  `any(Class<?>)` / `anyType(Class<?>)`, `isNull()`, `notNull()`, and array-aware `eq(...)` /
  `equalTo(...)`.
- **Bootstrap hook**: A configured fully qualified Java class name or ServiceLoader-discovered
  provider executed immediately before examples. Explicit hook classes must implement
  `org.javaspec.bootstrap.BootstrapHook`, have a public no-argument constructor, and be available on
  the run classloader/classpath. Top-level hooks run before selected-suite hooks, followed by
  discovered providers.
- **`BootstrapContext`**: Immutable context passed to bootstrap hooks. It exposes the run
  classloader and discovered specs selected for the run.
- **Class filter**: Repeatable `run --class <name>` filter matching described qualified/simple names
  or spec qualified/simple names.
- **Class-like type**: A described production type kind: class, final class, interface, enum,
  annotation, record, sealed class, or sealed interface.
- **Constructor policy**: `run` policy for unmatched constructors: `comment` (default), `preserve`,
  or explicit destructive `delete`.
- **Configuration-level report destination**: Optional top-level config value for JSON or JUnit
  XML-compatible report output. JSON aliases are `report`, `reportFile`, `report-file`,
  `jsonReport`, `jsonReportFile`, and `json-report-file`; JUnit XML aliases are `junitXml`,
  `junit-xml`, `junitXmlFile`, `junit-xml-file`, `junitXmlReportFile`, and `junit-xml-report-file`.
  Values are trimmed, must be non-blank when present, and are defaults overridden by CLI or explicit
  Maven/Gradle adapter report settings.
- **Core-only Maven verification**: Repository-root `mvn verify`; intentionally verifies only the
  zero-runtime-dependency core artifact, not standalone optional adapters.
- **Described type**: The production Java type inferred from or targeted by a specification.
- **`describe` / `desc`**: CLI command that creates specification/support skeletons only and never
  writes production source.
- **Direct matcher**: An `ObjectBehavior` convenience assertion such as `shouldReturn(actual,
  expected)` that delegates through `match(actual)`.
- **Dry-run**: `run --dry-run`; plans generation/update work without writes, prompts, or compilation
  and exits `1` when pending work exists.
- **Effective classloader**: The classloader visible to the CLI process. The runner can execute only
  spec classes available there unless an explicit classloader or compile-output-first classloader is
  selected.
- **Explicit classpath**: `run --classpath` or `--classpath-file` entries used to create the
  selected classloader for type existence checks and spec execution. Entries must point to already
  compiled classes or archives unless used as dependency entries with CLI `--compile`.
- **Opt-in compilation**: Explicit source/spec compilation through the current JDK
  `javax.tools.JavaCompiler` after discovery/profile/generation/update and before
  bootstrap/examples. CLI uses `run --compile` / `--compile-output <dir>`; programmatic, Maven, and
  Gradle entry points use their documented APIs/settings. No-spec and dry-run paths skip
  compilation.
- **Compile output**: Directory for CLI compilation output, default `target/javaspec-classes`. It is
  placed first in the compiler classpath and first in the execution classloader before explicit CLI
  classpath entries.
- **Example**: A public `void` Java spec method named `it_*` or `its_*`.
- **Example status**: Runtime outcome: `PASSED`, `FAILED`, `BROKEN`, `SKIPPED`, or `PENDING`.
- **Execution availability diagnostics**: Phase 23 diagnostics derived by
  `RunDiagnostics.executionAvailabilityLines(RunResult)` for source-discovered specs/examples that
  are not executable because compiled spec classes, expected public no-argument methods, or
  dependencies are unavailable to the runner classloader. They exclude explicit `@Skip` and
  `PENDING` semantics; CLI `--compile` can reduce source-only diagnostics but does not repair
  missing dependencies or stale compiled classes.
- **External formatter/extension provider**: A jar visible to the effective CLI or Gradle run
  classloader with `META-INF/services/org.javaspec.formatter.RunFormatter`,
  `META-INF/services/org.javaspec.extension.JavaspecExtension`, or alias
  `META-INF/services/org.javaspec.extension.Extension` entries. Providers are loaded by JDK
  `ServiceLoader` and can add formatter names to the run formatter registry.
- **GitHub Actions workflow**: `.github/workflows/ci.yml`; CI configuration with a Java
  8/11/17/21/25 core matrix and Java 21 full-verification job through `scripts/verify-all.sh`,
  including examples by default unless explicitly skipped. Phase 19 remote success is
  user-/maintainer-confirmed for HEAD `4d30e63` on `develop`; Phase 20/21/22 remote success is
  user-/maintainer-confirmed for HEAD `5088e96` on `develop`, with no independently queried run IDs,
  URLs, durations, or logs.
- **Stable id**: Identifier exposed by discovery/result objects and reports. Spec ids derive from
  the spec qualified name; example ids use `<specQualifiedName>#<methodName>` and match
  `ExampleResult.fullName()`.
- **Extension API**: Minimal contracts `JavaspecExtension`/`Extension` and `ExtensionContext`, plus
  `JavaspecExtensionLoader` and `ExtensionLoadingException` for JDK ServiceLoader provider discovery
  and diagnostics.
- **Formatter**: A `RunFormatter` implementation. The CLI supports built-in `progress` and `pretty`
  names plus ServiceLoader-discovered names available on the effective run classloader.
- **Gradle plugin adapter**: Standalone optional artifact `javaspec-gradle-plugin/` with plugin id
  `org.javaspec`, extension `javaspec`, and task `javaspecRun`. It is not a root Maven module and
  does not require JUnit in projects under test.
- **Gradle test source set runtime classpath**: The compiled `test` source set runtime classpath
  used by the optional Gradle plugin by default when the Gradle Java plugin/source sets are present.
- **Interface double**: A JDK dynamic proxy double for an ordinary Java interface under
  `org.javaspec.doubles`, supporting method-wide and matcher-aware argument-constrained stubs,
  return/throw/answer behavior, call history, and verification while excluding
  concrete/static/final/constructor/bytecode mocking from core.
- **JavaCompiler**: The current JDK `javax.tools.JavaCompiler` used only by opt-in CLI compilation.
  If unavailable, `run --compile` exits `64`; javaspec does not fork `javac`.
- **Invocation API**: `org.javaspec.invocation` no-`System.exit` programmatic API over canonical
  discovery, `SpecRunner`, and `RunResult`.
- **`DoubleInvocation`**: Immutable context passed to `StubAnswer` callbacks for interface doubles.
  It exposes the reflective method, method name, immutable argument snapshots, defensive
  argument-array copies, individual argument access, and argument count.
- **`JavaspecRunMojo`**: Phase 15 Maven plugin goal implementation for `javaspec:run`; it uses Maven
  test dependency resolution/classpath, passes top-level plus selected-suite bootstrap hooks to
  canonical invocation, uses Phase 24 config report destinations as defaults when explicit plugin
  report settings are absent, logs Phase 23 `javaspec:` execution-availability warnings with Maven
  test classpath element counts when needed, fails clearly on bootstrap failures, and delegates to
  `JavaspecLauncher` without `System.exit`.
- **`JavaspecRunTask`**: Phase 16 Gradle plugin task implementation for `javaspecRun`; it uses the
  Gradle classpath, manages a `URLClassLoader` and thread context classloader, loads Phase 25
  ServiceLoader formatter/extension providers from the run classloader, passes top-level plus
  selected-suite bootstrap hooks to canonical invocation, writes reports through core writers, uses
  Phase 24 config report destinations as defaults when explicit extension/task report settings are
  absent, logs Phase 23 `javaspec:` execution-availability warnings with Gradle classpath element
  counts when needed, fails clearly on bootstrap failures, and delegates to `JavaspecLauncher`
  without `System.exit`.
- **`JavaspecTestEngine`**: Phase 17 optional JUnit Platform `TestEngine` implementation with engine
  id `javaspec`; it is registered through ServiceLoader, filters canonical discovery by JUnit
  Platform selectors/configuration parameters, and delegates execution to `JavaspecLauncher` without
  `System.exit`.
- **JUnit Platform engine adapter**: Standalone optional artifact `javaspec-junit-platform-engine/`
  packaging `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`; it is not a root Maven
  module and does not add JUnit Platform dependencies to the core runtime artifact.
- **JUnit Platform selector**: Class, package, method, or unique-id selector supplied by JUnit
  Platform and applied by the optional engine as a filter over canonical javaspec discovery results.
- **JUnit XML-compatible report**: Dependency-free UTF-8 XML report written by `run --junit-xml` /
  `--junit-xml-file`, configuration-level report destinations, Maven/Gradle plugin report settings,
  or core report writers from `RunResult`; it does not require JUnit. SKIPPED and PENDING both map
  to `<skipped>`, and the testsuite `skipped` attribute includes both. Phase 21 adds a golden
  passing XML report under `docs/examples/reports/`; Phase 22 adds a pending XML golden.
- **LTS profile**: Target Java profile key: `java8`, `java11`, `java17`, `java21`, or `java25`;
  `run` enforces the effective profile before generation/update writes.
- **Matchable**: Fluent expectation wrapper returned by typed proxy methods and `match(actual)`.
- **Maven plugin adapter**: Standalone optional artifact `javaspec-maven-plugin/` packaging
  `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as a Maven plugin with goal prefix `javaspec`.
  It is not a root module and does not require JUnit in projects under test.
- **Maven test classpath**: The compiled test-scope classpath supplied by Maven to the optional
  plugin and used as input to the canonical javaspec runner.
- **Maven `release-artifacts` profile**: Phase 20 Maven profile on root, Maven plugin, and JUnit
  Platform engine builds that creates local sources and javadocs only; it does not sign, stage,
  deploy, or publish.
- **Bytecode doubles adapter**: Standalone optional artifact `javaspec-bytecode-doubles/`;
  implements `ConcreteDoubleProvider`, depends on ByteBuddy 1.14.18 outside core, and supports
  non-final concrete-class doubles with core `DoubleControl` semantics.
- **`ConcreteDoubleProvider`**: Core zero-dependency SPI discovered by ServiceLoader for optional
  concrete-class double providers.
- **Sealed-interface update**: Source-preserving generation/update support for sealed-interface root
  declarations plus nested permitted implementation bodies.
- **Pending example**: An example intentionally marked as pending through `@Pending`,
  `PendingExampleException`, or `ObjectBehavior.pending(...)`. It is counted separately from skipped
  in core results and JSON, but maps to skipped in JUnit-compatible outputs.
- **PHPSpec-inspired**: Modeled after PHPSpec workflow concepts while adapted to Java packages,
  classes, static typing, compilation, and interfaces.
- **Profile catalog**: Metadata model for Java LTS profiles, feature flags, and API symbols under
  `org.javaspec.profile`.
- **Profile enforcement**: Compatibility boundary under `org.javaspec.compatibility` that checks
  described type kinds, resolvable cataloged Java API owners in generated method signatures, and
  relationship references before generation/update writes. It is conservative and ignores unknown
  project types plus ambiguous or unresolvable names.
- **`ProfileEnforcement`**: Additive programmatic API that enforces a `TargetProfile` against a
  `DescribedType` and returns a `ProfileEnforcementReport`.
- **`ProfileEnforcementReport`**: Immutable report for one described type/profile pair, exposing
  allowed/denied status and `ProfileViolation` values.
- **`ProfileViolation`**: Immutable profile enforcement violation for one described type location
  and denied `CompatibilityResult`.
- **Reflection runner**: Dependency-free runner that executes compiled spec examples by Java
  reflection after discovery/generation/update work and any requested successful CLI compilation.
- **Release checklist**: `RELEASING.md`; Phase 20 local release-readiness checklist that documents
  verification steps and explicit blockers before public publication.
- **Publication blockers**: Required decisions, credentials, or approvals that remain intentionally
  unresolved before public release: GPG signing, Central Portal publication, Gradle Plugin Portal
  publication/credentials, final release version/tag, and final publish approval. The MIT `LICENSE`
  and maintainer metadata are already confirmed.
- **Report**: Optional machine-readable output written by `run`, currently JSON via `--report` /
  `--report-file` or config JSON destination aliases, and JUnit XML-compatible XML via `--junit-xml`
  / `--junit-xml-file` or config JUnit XML destination aliases. Reports include additive stable
  id/source, pending, and Phase 35 metadata/properties while remaining schemaVersion 1 for JSON.
- **Report schema**: `docs/schemas/run-report-v1.schema.json`; Phase 21 JSON Schema documentation
  for `schemaVersion` 1 run reports, updated in Phase 22 with optional additive `pending` counts and
  `PENDING` status.
- **Standalone examples**: Consumer projects under `examples/` for Maven plugin, Gradle plugin, and
  JUnit Platform engine adoption paths. They are not root modules and are verified by
  `scripts/verify-examples.sh` or by the default examples section in `scripts/verify-all.sh`.
- **`StubAnswer`**: Interface double callback contract that computes a stubbed result from a
  `DoubleInvocation` or throws to make the proxy invocation throw. Answer return values use existing
  return type validation.
- **`run`**: CLI command that discovers specs, owns production generation/update, can opt into
  source/spec compilation with `--compile` / `--compile-output <dir>`, executes configured bootstrap
  hooks before examples when specs exist, can execute compiled examples, renders output, and can
  write reports.
- **Source location metadata**: Source file path and 1-based source line information captured from
  discovered specs/examples and propagated to runner results and reports where available.
- **Skip annotation/signal**: `@Skip`, `SkipExampleException`, or `ObjectBehavior.skip(...)`;
  explicitly marks an example skipped without adding dependencies. Annotation-based skip does not
  instantiate the spec or run lifecycle/body code.
- **Source-only spec**: A discovered spec source file whose compiled class is unavailable to the
  runner; examples are reported as skipped with execution-availability diagnostics when appropriate
  unless a CLI `--compile` run successfully compiles it and its dependencies are available.
- **Spec package prefix**: Suite naming prefix for generated/discovered spec classes, default
  `spec`.
- **Spec root**: File-system root searched for specification sources, default `src/test/java`.
- **Suite**: Configuration grouping for spec root, source root, spec package prefix, production
  package prefix, suite-level bootstrap hook class names, and suite-level extension activation class
  names.
- **Typed proxy**: Generated support method that returns `Matchable<T>` for a subject method,
  enabling syntax like `getRating().shouldReturn(5)`.
- **UniqueId segment**: Optional JUnit Platform engine identifier segment. The Phase 17 engine uses
  `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and `[example:<methodName>]`.
- **Zero runtime dependency**: Architectural policy that the runtime artifact depends only on the
  JDK and the project artifact itself.
