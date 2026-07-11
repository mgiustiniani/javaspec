# 9. Architecture Decisions

Architecture decisions are recorded as ADRs in `docs/adr/`.

- **[ADR 0001](../adr/0001-java-8-baseline-with-lts-target-profiles.md)**: Java 8 baseline with
  metadata-driven Java LTS target profiles
- **[ADR 0002](../adr/0002-zero-runtime-dependency-policy.md)**: Zero runtime dependency policy
- **[ADR 0003](../adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md)**:
  Course correction: move class-creation suggestion into the first MVP
- **[ADR 0004](../adr/0004-course-correction-construction-defaults-typed-matcher-proxies-and-method-generators.md)**:
  Course correction: construction defaults, typed matcher proxies, and method generators
- **[ADR 0005](../adr/0005-restricted-line-based-configuration-format.md)**: Restricted line-based
  configuration format
- **[ADR 0006](../adr/0006-classpath-reflection-runner.md)**: Classpath reflection runner for
  executable examples
- **[ADR 0007](../adr/0007-jdk-proxy-only-interface-doubles.md)**: JDK proxy-only interface doubles
- **[ADR 0008](../adr/0008-run-only-controls-and-non-mutating-dry-run-planning.md)**: Run-only
  controls and non-mutating dry-run planning
- **[ADR 0009](../adr/0009-interface-style-method-generation-and-sealed-interface-update-deferral.md)**:
  Interface-style method generation and sealed-interface update deferral
- **[ADR 0010](../adr/0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md)**:
  Zero-dependency formatter, reporting, and programmatic extension boundary
- **[ADR 0011](../adr/0011-optional-junit-adapter-and-canonical-javaspec-runner.md)**: Optional
  build-tool/JUnit adapters and canonical javaspec runner
- **[ADR 0012](../adr/0012-non-disruptive-aggregate-release-ci-verification.md)**: Non-disruptive
  aggregate release and CI verification
- **[ADR 0013](../adr/0013-release-readiness-scaffolding-with-publication-blockers.md)**:
  Release-readiness scaffolding with resolved metadata (publication completed)
- **[ADR 0014](../adr/0014-standalone-adoption-assets-and-default-examples-verification.md)**:
  Standalone adoption assets and default examples verification
- **[ADR 0015](../adr/0015-explicit-skipped-and-pending-semantics.md)**: Explicit skipped and
  pending semantics
- **[ADR 0016](../adr/0016-classpath-execution-availability-diagnostics.md)**: Classpath execution
  availability diagnostics without integrated compilation
- **[ADR 0017](../adr/0017-configuration-level-report-destinations.md)**: Configuration-level report
  destinations and precedence
- **[ADR 0018](../adr/0018-serviceloader-external-formatter-extension-discovery.md)**: ServiceLoader
  external formatter and extension discovery
- **[ADR 0019](../adr/0019-deep-target-profile-enforcement.md)**: Deep target-profile enforcement
  before generation/update writes
- **[ADR 0020](../adr/0020-bootstrap-hook-execution.md)**: Bootstrap hook execution before examples
- **[ADR 0021](../adr/0021-stronger-interface-doubles.md)**: Stronger interface doubles
- **[ADR 0022](../adr/0022-opt-in-cli-source-spec-compilation.md)**: Opt-in CLI source/spec
  compilation
- **[ADR 0023](../adr/0023-course-correction-resolve-deferred-known-limitations.md)**: Course
  correction: resolve deferred known limitations
- **[ADR 0024](../adr/0024-standalone-optional-bytecode-doubles-adapter.md)**: Standalone optional
  bytecode doubles adapter

## 9.1 Decision Coverage by Architecture Area

- **Compatibility and target profiles**: ADR 0001
- **Dependency policy**: ADR 0002
- **PHPSpec-style describe/run generation split**: ADR 0003
- **Construction semantics, typed proxies, and method generation foundation**: ADR 0004
- **Configuration and suite naming**: ADR 0005
- **Reflection execution and runner result semantics**: ADR 0006
- **Interface and concrete-class doubles**: ADR 0007, ADR 0021, and ADR 0024
- **Phase 9 run controls and dry-run behavior**: ADR 0008
- **Phase 10 interface/annotation/sealed-interface generation**: ADR 0009
- **Phase 11 formatter, JSON reporting, and programmatic extension contracts**: ADR 0010
- **Phase 14 no-JUnit invocation, explicit classpath input, JUnit XML-compatible reports, Phase 15
  standalone optional Maven plugin, Phase 16 standalone optional Gradle plugin, Phase 17 standalone
  optional JUnit Platform engine, and Phase 18 stable identifier/source-location/report polish**:
  ADR 0011, with report field details also covered by ADR 0010
- **Phase 19 non-disruptive release/CI hardening and no mandatory Maven multi-module conversion**:
  ADR 0012
- **Phase 20 release-readiness scaffolding with resolved MIT license/maintainer metadata and
  completed signing/portal publication**: ADR 0013
- **Phase 21 standalone examples, report schema/golden documentation, and examples-by-default
  aggregate verification**: ADR 0014
- **Phase 22 explicit skipped/pending API, distinct PENDING result status, and JUnit-compatible
  pending mapping**: ADR 0015
- **Phase 23 classpath/execution availability diagnostics without integrated compilation**: ADR
  0016, with runner boundary details also covered by ADR 0006 and adapter boundary details by ADR
  0011
- **Phase 24 configuration-level JSON/JUnit XML-compatible report destinations and precedence**: ADR
  0017, with configuration format details also covered by ADR 0005 and report writer/schema details
  by ADR 0010
- **Phase 25 ServiceLoader external formatter/extension discovery for CLI and Gradle**: ADR 0018,
  with dependency policy also covered by ADR 0002, formatter/extension contracts by ADR 0010,
  adapter boundaries by ADR 0011, and formatter config precedence by ADR 0017
- **Phase 26 target-profile enforcement before generation/update writes**: ADR 0019, with Java
  8/profile metadata also covered by ADR 0001, dependency policy by ADR 0002, run/dry-run boundaries
  by ADR 0008, generation boundaries by ADR 0009, report behavior by ADR 0010 and ADR 0017, and
  adapter boundaries by ADR 0011
- **Phase 27 bootstrap hook execution before examples**: ADR 0020, with Java 8 and zero-dependency
  policy also covered by ADR 0001 and ADR 0002, configuration syntax by ADR 0005, classpath
  reflection by ADR 0006, report/no-report behavior by ADR 0010 and ADR 0017, adapter invocation
  boundaries by ADR 0011, and profile preconditions by ADR 0019
- **Phase 28 stronger interface doubles**: ADR 0021, with Java 8 and zero-dependency policy also
  covered by ADR 0001 and ADR 0002, and the original JDK proxy-only doubles boundary by ADR 0007
- **Phase 29 opt-in CLI source/spec compilation**: ADR 0022, with Java 8 and zero-dependency policy
  also covered by ADR 0001 and ADR 0002, reflection execution by ADR 0006, run/dry-run boundaries by
  ADR 0008, reporting by ADR 0010, adapter boundaries by ADR 0011, execution-availability
  diagnostics by ADR 0016, profile preconditions by ADR 0019, and bootstrap ordering by ADR 0020
- **Phases 30-36 known-limitations resolution**: ADR 0023, with affected earlier boundaries
  documented by ADRs 0009, 0010, 0018, 0019, 0020, 0021, and 0022
- **Phase 37 standalone optional bytecode doubles adapter**: ADR 0024, with Java 8 and
  zero-dependency policy also covered by ADR 0001 and ADR 0002 and doubles boundaries by ADR 0007
  and ADR 0021

## 9.2 Decision Notes and Boundaries

- No new ADR was needed for Phase 14 because the implemented no-`System.exit` invocation API,
  explicit classpath input, and dependency-free JUnit XML-compatible reports follow ADR 0011's
  accepted canonical-runner/no-JUnit boundary.
- No new ADR was needed for Phase 15 because the standalone optional Maven plugin follows the same
  adapter boundary: it stays outside the core runtime artifact, delegates to `JavaspecLauncher`,
  avoids `System.exit`, and does not require JUnit in projects under test.
- No new ADR was needed for Phase 16 because the standalone optional Gradle plugin follows the same
  adapter boundary: it stays outside the core runtime artifact and root Maven module list, delegates
  to `JavaspecLauncher`, avoids `System.exit`, and does not require JUnit in projects under test.
- No new ADR was needed for Phase 17 because the standalone optional JUnit Platform engine follows
  the same adapter boundary: it stays outside the core runtime artifact and root Maven module list,
  uses JUnit Platform APIs only in the optional engine artifact, delegates to `JavaspecLauncher`,
  avoids `System.exit`, and does not require changes to javaspec spec authoring style.
- No new ADR was needed for Phase 18 because stable id/source metadata and additive report fields
  stay within ADR 0010's zero-dependency reporting boundary and ADR 0011's
  canonical-runner/optional-adapter boundary.
- ADR 0012 records the Phase 19 decision to use aggregate local/CI verification while preserving
  standalone adapter boundaries instead of converting the root build to mandatory Maven multi-module
  now.
- ADR 0013 records the Phase 20 decision to add version alignment, changelog/releasing
  documentation, the confirmed MIT `LICENSE`, MIT license metadata, confirmed maintainer/developer
  metadata, safe metadata, and local source/javadoc artifact readiness while leaving GPG signing,
  Central Portal publication, Gradle Plugin Portal publication/credentials, final release
  version/tag, and final publish approval are resolved. Artifacts are published on Maven Central
  and the Gradle Plugin Portal.
  decisions are made.
- ADR 0014 records the Phase 21 decision to keep report schemas, golden reports, and
  Maven/Gradle/JUnit Platform examples as standalone adoption assets and to run examples by default
  from `scripts/verify-all.sh` with explicit opt-outs, without public publication or core runtime
  changes.
- ADR 0015 records the Phase 22 decision to implement explicit skipped/pending semantics with
  zero-dependency annotations/signals/helpers, keep `PENDING` distinct from `SKIPPED`, add pending
  counts to JSON and summaries, and map pending to skipped in JUnit XML/JUnit Platform-compatible
  adapters.
- ADR 0016 records the Phase 23 decision to add classpath/execution availability diagnostics from
  the canonical `RunResult` while keeping compilation external to javaspec and preserving
  exit/build-failure semantics.
- ADR 0017 records the Phase 24 decision to add top-level config defaults for JSON/JUnit
  XML-compatible report destinations, with CLI and explicit Maven/Gradle adapter settings taking
  precedence while report schemas, writers, exit codes, build-failure semantics, dry-run behavior,
  and no-spec behavior remain unchanged.
- ADR 0018 records the Phase 25 decision to use JDK `ServiceLoader` for external run
  formatter/extension discovery on CLI and Gradle run classloaders, keeping built-ins first,
  avoiding new dependencies, de-duplicating extension implementations listed under both extension
  service types, and leaving report schemas/content, integrated compilation, Maven plugin formatter
  controls, JUnit Platform formatter controls, and publishing unchanged.
- ADR 0019 records the Phase 26 decision to enforce the effective target profile during `javaspec
  run` before generation/update writes, checking described type kinds and conservatively checking
  generated method signatures against known profile-catalog API owners while ignoring unknown or
  ambiguous types to avoid false positives.
- ADR 0020 records the Phase 27 decision to execute explicitly configured bootstrap hook class names
  immediately before examples, using the effective run classloader, preserving
  top-level-before-suite order and duplicates, and failing before reports when bootstrap execution
  fails.
- ADR 0021 records the Phase 28 decision to strengthen interface doubles with argument matchers,
  argument-constrained stub precedence, throwing stubs, and answer callbacks while preserving the
  zero-dependency JDK dynamic proxy and interface-only boundary.
- ADR 0022 records the Phase 29 decision to add opt-in CLI-only source/spec compilation through the
  current JDK `javax.tools.JavaCompiler`, with `--compile-output <dir>` implying `--compile`,
  compilation after discovery/profile/generation/update and before bootstrap/examples, deterministic
  classpath ordering, no reports on compile failure, and no
  config/schema/dependency-resolver/incremental-cache/source-level-release-management changes.
- ADR 0023 records the maintainer-requested course correction to resolve deferred known limitations
  in Phases 30-36 while keeping public publication out of scope.
- ADR 0024 records the maintainer decision to resolve concrete-class doubles with a standalone
  optional ByteBuddy adapter instead of adding bytecode dependencies to core.
- Package scanning, plugin lookup, script engines, automatic classpath repair, broader
  compiler-grade profile checks, actual publishing/signing automation, final/static/constructor
  doubles, new report formats/schema changes, and any future multi-module conversion remain future
  work and require new or updated ADRs before implementation if they change the current architecture
  boundaries.
- ADR 0011 fixes the current boundary that javaspec core remains canonical and no-JUnit execution
  stays first-class; ADR 0012 fixes the current release/CI boundary that root Maven verification
  remains core-only and aggregate verification is explicit; ADR 0013 fixes the current publication
  boundary that resolved license/maintainer metadata and local release-readiness scaffolding must
  not imply public publication readiness.
- ADR 0014 fixes the current adoption-assets boundary that standalone examples and schema/golden
  report docs must not imply root-module conversion, public publication, or remote CI success.
- ADR 0015 fixes the current explicit non-execution semantics boundary: skipped and pending remain
  zero-dependency core statuses, pending remains distinct in core/JSON, and JUnit-compatible outputs
  use skipped mappings for compatibility.
- ADR 0016 fixes the diagnostics boundary: javaspec may report classpath/execution availability
  problems deterministically, while default/adapters compilation and classpath assembly remain the
  user's build-tool or launcher responsibility unless a later CLI-only opt-in compilation boundary
  applies.
- ADR 0017 fixes the current report-destination boundary: configuration can provide default
  JSON/JUnit XML-compatible output paths, but explicit entry-point settings win and report
  schemas/writers remain unchanged.
- ADR 0018 fixes the extension-discovery boundary: external formatter/extension discovery is
  ServiceLoader-based and classloader-scoped; ADR 0023 records later config-driven activation and
  adapter formatter controls where implemented, while package scanning, plugin lookup, and automatic
  classpath repair remain out of scope.
- ADR 0019 fixes the current profile-enforcement boundary: `run` fails before generation/update
  writes on denied described kinds or resolvable later-JDK API signatures, but profile enforcement
  itself is not compiler-grade and does not reject unknown project/ambiguous type names.
- ADR 0020 fixes the current bootstrap boundary: explicit config hook class names are executable
  only when compiled on the run classloader/classpath, top-level hooks run before selected-suite
  hooks with duplicates preserved, and failures stop before examples/reports with ServiceLoader
  providers added later by ADR 0023, while still avoiding script engines, package scanning,
  dependency resolution, or runtime dependencies.
- ADR 0021 fixes the current stronger-doubles boundary: interface doubles may use explicit argument
  matchers, throwing stubs, and answer callbacks, but core doubles remain ordinary-interface JDK
  proxies.
- ADR 0022 fixes the CLI compilation boundary: source-only CLI runs can opt in to current-JDK
  `javax.tools.JavaCompiler` compilation before bootstrap/examples, while defaults, config, reports,
  dependency resolution, incremental caching, and source-level/release management remain unchanged.
- ADR 0023 fixes the known-limitations resolution boundary: implementation may resolve previously
  deferred items. Public publication is complete; artifacts are published on Maven Central and
  the Gradle Plugin Portal.
- ADR 0024 fixes the bytecode doubles boundary: non-final concrete-class doubles are optional and
  standalone, ByteBuddy is isolated to `javaspec-bytecode-doubles`, and final/static/constructor
  mocking remains unsupported.
