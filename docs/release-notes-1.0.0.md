# Release notes — 1.0.0 / RC1

This development line keeps the core artifact Java 8-compatible and zero-runtime-dependency while
moving dependency-heavy behavior into optional artifacts.

## Core `io.github.jvmspec:javaspec`

- Generated support and Prophecy wrapper sources are written to `target/generated-sources/javaspec`
  by default, not `src/`.
- `javaspec prophesize <Class>` supports interfaces and concrete/final classes. Generated wrappers
  extend `ObjectProphecy<T>` and expose typed `MethodProphecy` methods.
- `javaspec run --generate` can generate missing Prophecy wrappers and update the generated
  `*SpecSupport` class with typed helpers such as `prophesizeMailer()` / `prophecyMailer()`.
- Prophecy authoring styles:
  - Java 8+: `MailerProphecy mailer = prophesizeMailer();`
  - Java 10+: `var mailer = prophesizeMailer();`
- The standalone `Prophecies` factory makes Prophecy runtime usage independent of `ObjectBehavior`;
  `ObjectBehavior` remains only the spec-author convenience base.
- Interface doubles now support default interface methods, sequential returns, callback sequences,
  captors, ordered verification, and return-then-throw stubbing.
- CLI compilation gained local-POM dependency resolution (`--resolve-pom`), `--release <N>`, and
  incremental cache hits for unchanged inputs.
- Extension discovery includes `list-extensions` and classpath repair hints. The 1.0 extension SPI
  contract is frozen in `docs/extension-spi-1.0.md`; typed event model v2 is deferred, and extension
  activation temporarily sets/restores the effective run thread context classloader.
- Parser/updater generation is backed by a parser SPI and ignores signatures inside comments and
  literals, including Java text blocks. Direct-member scoping prevents methods inside local,
  anonymous, nested, or secondary top-level types from suppressing a required subject update.
- The JLC language-coverage harness inventories final constructs relevant to Java 8/11/17/21/25,
  emits deterministic `COVERED`/`PLANNED` evidence, and provides a strict stable-release gate while
  preserving the classic PHPSpec-inspired subject-centric workflow. Java 25 compact source files
  remain valid project files but are refused fail-closed as described subjects before generation
  writes, with guidance to use a named class-like subject. Source updates also retain CRLF style,
  missing final newlines, UTF-8 BOM, Unicode content, and local member indentation; injected atomic
  move failures preserve original bytes and clean temporary files.
- Discovery now infers more static argument types before generation, including casted nulls,
  class literals, array creation expressions, Java 10+ simple `var` initializers, constructed value
  objects, and likely value-object static factory calls such as `CertificateProfileId.of("abc")`;
  production-source refinement now avoids
  arbitrary signature changes when an unknown argument matches multiple overloads equally.
- Java 8 verification now adds the JDK `tools.jar` compiler tree API when needed and the AST-based
  discovery/refinement path falls back safely when those optional compiler classes are unavailable
  at runtime.
- Generation robustness hardening adds `docs/test-matrix-generation.md`, treats record component
  accessors as existing methods during record updates, evolves record headers from
  constructor-driven specs such as `beConstructedWith(...)` plus `value()`, refines existing source
  kind before updates so records are not evolved as classes, emits generated support default
  `beConstructedWith(...)` calls for evolved records with canonical constructors, and allows explicit
  legacy `beConstructedWith(...)` prefix arguments for records to be padded with Java defaults for
  newly added trailing canonical components while exact constructors still win. It preserves compact
  constructors while adding record methods/components, checks multiline sealed-class `permits`
  updates with Java 17 compilation, verifies `run --generate --compile --release 17` for generated
  record/sealed skeletons, and expands parser coverage for generic method type parameters and
  annotated varargs.
- `Doubles.controlFromHandler(...)` was added as a public adapter hook so optional adapters do not
  depend on package-private core implementation classes across build-tool classloaders.
- Phase 46 starts the PHPSpec-first JUnit-parity line with
  `docs/phpspec-compatibility-charter.md`, PHPSpec parity rows in
  `docs/test-matrix-generation.md`, and CLI, Maven plugin, Gradle plugin, and JUnit Platform adapter
  smoke coverage for the canonical subject-centric `let` / `subject()` /
  `match(...).shouldReturn(...)` authoring style.
- Phase 47 begins PHPSpec-style example data with zero-dependency core APIs: `row(...)`,
  `examples(...)`, `Example1`, and `Example2`. Rows execute inside the containing behavior example,
  failing rows include row number/value context, JSON reports include first-class
  `exampleDataRows` entries for row index, description, status, and detail, JUnit XML reports
  expose rows as testcase entries for CI-visible row diagnostics, and the optional JUnit Platform
  adapter publishes dynamic row descriptors during execution with row unique-id event filtering.
  The 1.0 row execution/reporting/selector contract is frozen in
  `docs/example-data-contract-1.0.md`: rows execute inline in the owning example; adapter row
  selectors filter descriptors/events without isolating per-row execution.
- The optional JUnit Platform engine contract is frozen in `docs/junit-platform-contract-1.0.md`,
  covering engine id, class/method/package/unique-id selectors, descriptor sources, unique-id shape,
  row projection, status mapping, discovery-only behavior, and IDE/Surefire/Gradle boundaries.
- Phase 48 starts PHPSpec-style collaborator injection: public `it_*` / `its_*`, `let()`, and
  `letGo()` methods may declare supported collaborator parameters. Ordinary interface parameters are
  injected as interface doubles, generated typed `*Prophecy` wrapper parameters are backed by the
  spec's shared prediction registry, and the same collaborator instance is reused across
  `let`/example/`letGo` for one example run through deterministic declared-parameter resolution.
  Automatic prediction checking now runs before `letGo` while still guaranteeing teardown execution;
  duplicate same-type collaborator parameters and ambiguous overloads report BROKEN diagnostics.
  `ObjectBehavior.sharedProphecyRegistry()` exposes the runner/generator adapter hook without
  reflective protected-method access.
- Phase 49 completes the current Prophecy parity slice with identity (`same` / `identicalTo`),
  membership (`in` / `notIn`), custom callback (`matching`), and named custom `ArgumentToken`
  argument tokens through core `Doubles` / `ArgumentMatchers` and prophecy `Argument` / `Arg`
  aliases. Generated typed `*Prophecy` wrappers include same-name `Object` argument-token overloads,
  so matcher-token calls compile without falling back to reflective `method("...")` syntax.
  `MethodProphecy.should(...)` supports custom prediction callbacks receiving a `PredictionContext`
  with matching calls, all calls, method name, argument pattern, and call count. Prediction and
  verification failure messages include recorded/matching call context, including ordered-verification
  failures and same-method calls with different arguments. The generated-wrapper token overload audit
  now covers primitives, arrays, varargs, bounded generics, bridge/synthetic methods, duplicate
  overloads, and mixed exact/token call compilation.
- Phase 50 matcher parity includes Java-adapted approximate numeric expectations, iterator-backed
  collection/count/emptiness expectations, and generated object-state expectations such as
  `shouldBeActive()` and `shouldHaveTitle(...)` in generated `*SpecSupport` helpers. The 1.0 custom
  matcher scope is frozen in `docs/matcher-contract-1.0.md`: programmatic registry/`shouldMatch(...)`
  support is stable, while configuration-file and inline dynamic custom matcher conveniences are
  deferred.
- Generated stubs are fail-closed: compiled runs that still contain `// javaspec:stub` markers add a
  synthetic `BROKEN` result so generated skeletons cannot accidentally produce final GREEN.
- Mutating generation paths use atomic source-file writes and the generation contract is frozen in
  `docs/generation-contract-1.0.md`.

## Optional subclass adapter `javaspec-bytecode-doubles`

- Remains the lightweight ByteBuddy subclass adapter for non-final concrete classes.
- Capability diagnostics clearly explain unsupported targets such as final classes, arrays,
  primitives, annotations, enums, and interfaces.

## Optional agent adapter `javaspec-bytecode-agent`

- Adds instrumentation-backed concrete doubles for final classes.
- Adds scoped static-method doubles via `BytecodeAgentDoubles.staticDouble(...)`.
- Adds construction-aware doubles via `BytecodeAgentDoubles.mockConstruction(...)`.
- Dynamic self-attach uses ByteBuddy Agent when the JVM allows it; `-javaagent` remains available for
  stricter environments.
- A Maven example is available at `examples/bytecode-agent-basic/`.

## Verification and release notes

- Version alignment checks cover core, Maven plugin, Gradle plugin, JUnit Platform engine,
  `javaspec-bytecode-doubles`, and `javaspec-bytecode-agent`.
- `scripts/verify-examples.sh` verifies Maven, Prophecy, bytecode-doubles, bytecode-agent,
  JUnit Platform, and Gradle examples. Use `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` when no Gradle
  executable is available locally.
- `scripts/verify-all.sh` remains the full aggregate check. This repository intentionally does not
  vendor a Gradle Wrapper in this development line; set `JAVASPEC_GRADLE_BIN` to a local Gradle
  executable or `JAVASPEC_SKIP_GRADLE=1` when verifying without Gradle.
- `scripts/verify-release-dry-run.sh` packages and verifies core, Maven plugin, JUnit Platform
  engine, bytecode doubles, bytecode agent, and Gradle plugin artifacts, including source/Javadoc
  jars, bytecode-agent manifest entries, SHA-256 checksums, and external consumer examples.
- `scripts/check-release-preflight.sh` fails RC/final publication unless the release version, tag,
  and absence of `SNAPSHOT` build-file references are aligned.
- `scripts/generate-api-baseline.sh` creates the deterministic public/protected JVM signature
  inventory archived with RC1 for later compatibility comparisons.
- Core `mvn verify` runs Animal Sniffer against the Java 8 API signature to prevent accidental
  direct linkage to post-Java-8 APIs; `com.sun.source.*` is explicitly allowed for JDK 8 javac tree
  API use.
- `scripts/check-core-java8-bytecode.sh` verifies core classfiles stay at Java 8-compatible major
  version 52.
- 1.0 documentation includes compatibility policy, Java compatibility matrix, migration guide,
  JUnit-to-javaspec guide, Cucumber/Gherkin boundary, and troubleshooting pages.
- RC1 evidence passed with aligned `1.0.0-RC1` artifacts, no build-file `SNAPSHOT` references,
  release dry-run consumers, checksums, a deterministic archived API inventory, and the Java
  8/11/17/21/25 matrix plus full Java 21 verification. The latest full branch run is
  [29139032096](https://github.com/mgiustiniani/javaspec/actions/runs/29139032096).
- Releases follow Git Flow through `release/<version>`, a non-fast-forward merge into production
  branch `main`, an annotated tag on that merge commit, and a merge back into `develop`.
- Release workflow
  [run 29146746362](https://github.com/mgiustiniani/javaspec/actions/runs/29146746362) passed all
  preflight, artifact/consumer, GPG signing, and Maven Central steps. All five Maven artifacts and
  their POM, main/source/Javadoc JARs, and signatures were verified directly. Corrected Gradle Plugin
  Portal submission completed in
  [run 29148854181](https://github.com/mgiustiniani/javaspec/actions/runs/29148854181); public
  visibility remains under first-publication review.
