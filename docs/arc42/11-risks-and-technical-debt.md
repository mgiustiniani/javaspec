# 11. Risks and Technical Debt

Current risks and mitigations:

- **Source-only specs are skipped by default/adapters**
  - Current impact: Users may expect default `javaspec run`, programmatic invocation, build-tool
    adapters, or the optional JUnit Platform engine to compile and execute source files directly.
  - Mitigation / next action: Document that default/adapters execute only
    compiled/classloader-available specs; use `--classpath`, `--classpath-file`, optional
    Maven/Gradle plugin test classpath integration, optional JUnit Platform runtime classpath
    integration, a programmatic classloader after external compilation, or CLI `--compile` /
    `--compile-output <dir>` for source-only CLI runs. Phase 23 diagnostics explain
    discovered-but-not-executable specs/examples but do not repair classpaths.
- **Profile enforcement is conservative**
  - Current impact: Phase 26 rejects incompatible described type kinds and resolvable cataloged Java
    API signature owners before generation/update writes, but it is not a compiler and ignores
    unknown project types plus ambiguous or unresolvable type names.
  - Mitigation / next action: Keep the conservative boundary explicit; expand catalog coverage
    carefully and require a future ADR before compiler-integrated profile checks or broader source
    type resolution.
- **Bootstrap hooks depend on user classpath and side effects**
  - Current impact: Configured and ServiceLoader-discovered hook classes execute before examples,
    but hooks must already be compiled and visible to the run classloader/classpath or produced by
    successful opt-in compilation where supported. Hook side effects are user-controlled and are not
    reversible by javaspec.
  - Mitigation / next action: Document classloader/provider ordering and public no-arg constructor
    requirements, keep `BootstrapContext` immutable, fail before reports on bootstrap errors, and
    require future ADRs before adding scripts, package scanning, dependency resolution, or automatic
    classpath repair.
- **Extension discovery is classpath-scoped**
  - Current impact: ServiceLoader discovery and config-driven extension activation require provider
    classes on the effective run classloader; users may still expect package scanning, plugin
    repository lookup, or automatic classpath repair.
  - Mitigation / next action: Document provider classpath requirements and explicit activation keys;
    broader activation/control mechanisms require future ADRs.
- **Report scope is limited**
  - Current impact: JSON reports remain schemaVersion 1 with additive stable id/source, pending, and
    Phase 35 metadata/properties; JUnit XML-compatible reports remain intentionally minimal but
    include Phase 35 testsuite metadata/properties. Phase 24 adds config-level destinations for
    output paths only.
  - Mitigation / next action: Treat schema-version changes, suite-specific destinations, and new
    report formats as future decisions; keep current report behavior, schema docs, golden reports,
    and examples synchronized.
- **Java source parsing is heuristic**
  - Current impact: Complex Java source may not be updated safely by generators.
  - Mitigation / next action: Keep generation deterministic and conservative; skip unsupported
    updates instead of corrupting source; consider parser strategy only if compatible with
    zero-runtime-dependency policy.
- **Internal language seam could be mistaken for language support**
  - Current impact: Java discovery and production planning now pass through internal frontend/backend
    abstractions, but only Java is implemented and registered.
  - Mitigation / next action: Keep the seam classified `INTERNAL`, expose no language CLI/config or
    ServiceLoader SPI before 1.0, and defer Kotlin/other adapters until a post-1.0 vertical slice
    validates portable behavior modeling and fail-closed source ownership.
- **Generated post-Java-8 source requires newer JDKs**
  - Current impact: Records and sealed types can be emitted by a Java 8-compatible binary but cannot
    be compiled by Java 8 projects.
  - Mitigation / next action: Document minimum source levels and rely on target project/JDK profile
    choices.
- **Doubles are split between core and optional adapter**
  - Current impact: Core doubles remain interface-only JDK proxies. The optional bytecode adapter
    supports non-final concrete classes only and rejects
    final/static/constructor/enum/array/annotation/primitive/interface cases.
  - Mitigation / next action: Preserve the zero-dependency JDK-proxy core; keep ByteBuddy isolated
    in `javaspec-bytecode-doubles`; require a future ADR for final/static/constructor or broader
    bytecode behavior.
- **Dry-run planning synchronization**
  - Current impact: Every new generation/update feature must also be represented in dry-run output.
  - Mitigation / next action: Treat dry-run coverage as an acceptance criterion for future
    generation changes.
- **Internal JSON/XML writer maintenance**
  - Current impact: No JSON, XML, JUnit, or reporting library means escaping and deterministic
    output are maintained internally.
  - Mitigation / next action: Keep focused tests for report writer behavior; do not expand report
    complexity without tests.
- **LTS API metadata drift**
  - Current impact: Java profile metadata can become stale as docs or JDK releases evolve.
  - Mitigation / next action: Maintain research notes and re-run runtime probes in compatibility
    matrix work, especially for Java 25 stream gatherers and later profiles.
- **Invocation API compatibility pressure**
  - Current impact: The optional Maven and Gradle plugins now depend on `io.github.jvmspec.invocation`
    and `RunResult` semantics; future adapters may also depend on them.
  - Mitigation / next action: Keep the API small, document exit-code mapping, preserve the
    `JavaspecLauncher` delegation guard, and add compatibility tests before adapter work expands it.
- **Standalone adapter verification can be missed**
  - Current impact: Because `javaspec-maven-plugin/`, `javaspec-gradle-plugin/`, and
    `javaspec-junit-platform-engine/` are intentionally standalone and not root Maven modules,
    repository-root `mvn verify` does not verify the adapters. Opt-in adapter compilation is
    explicit and current-JDK-only, so host build verification still matters.
  - Mitigation / next action: Document the local sequences and prefer `scripts/verify-all.sh` for
    aggregate release verification; keep standalone adapter verification in release/CI checklists
    and GitHub Actions full verification.
- **Gradle executable/JDK compatibility can block local verification**
  - Current impact: A Gradle executable compatible with the installed JDK is required for local
    Gradle plugin verification; `scripts/verify-all.sh` also requires a compatible Gradle
    executable unless `JAVASPEC_SKIP_GRADLE=1` is intentionally set.
  - Mitigation / next action: Use a JDK-compatible Gradle executable such as the verified system
    Gradle 9.6.1, set `JAVASPEC_GRADLE_BIN`, or explicitly choose `JAVASPEC_SKIP_GRADLE=1` only when
    Gradle verification is intentionally out of scope. The supported Fedora build image includes
    Gradle and runs the unmodified aggregate verification script.
- **Optional JUnit Platform engine can be mistaken for required execution**
  - Current impact: Users may assume javaspec specs require JUnit Platform once the optional engine
    exists, or that Phase 29 CLI compilation changes the engine.
  - Mitigation / next action: Keep no-JUnit CLI/programmatic/Maven/Gradle paths documented first;
    state that the engine is an optional IDE/CI adapter only, does not compile source/spec files
    itself, and that projects not opting in keep no JUnit dependency.
- **Standalone examples can be mistaken for root modules**
  - Current impact: Users may assume `examples/` are part of repository-root `mvn verify` or a Maven
    reactor because `scripts/verify-all.sh` now runs them by default.
  - Mitigation / next action: ADR 0014 and user docs state that examples are standalone consumer
    projects; verify them through `scripts/verify-examples.sh` or the explicit aggregate script, not
    through root Maven alone.
- **Remote CI status can be overstated**
  - Current impact: A configured workflow, local YAML parse, local aggregate verification,
    standalone examples verification, or Phase 22 local verification does not by itself prove that
    GitHub Actions has run remotely, and remote status can differ by phase.
  - Mitigation / next action: Documentation must state remote results by phase and evidence source.
    Phase 19 remote GitHub Actions success is user-/maintainer-confirmed for HEAD `4d30e63` on
    `develop`; after Phase 20/21/22 were pushed, remote GitHub Actions success for HEAD `5088e96` on
    `develop` is user-/maintainer-confirmed, with no independently queried run IDs, URLs, durations,
    or logs.
- **Future multi-module conversion may be assumed**
  - Current impact: Users may assume Phase 19 or Phase 20 converted the repository to Maven
    multi-module because they added aggregate verification and release-readiness scaffolding.
  - Mitigation / next action: ADR 0012, ADR 0013, and user docs state that no Maven multi-module
    conversion happened; root `mvn verify` remains core-only and adapters remain standalone.
- **Public publication readiness can be overstated**
  - Current impact: Local sources/javadocs, safe URL/SCM/issues metadata, confirmed MIT
    license/maintainer metadata, release checklists, schema docs, and standalone examples can be
    mistaken for a complete Maven Central or Gradle Plugin Portal publication flow.
  - Mitigation / next action: ADR 0013, ADR 0014, and `RELEASING.md` distinguish independently
    verified channels. RC4 Maven artifacts are published under `io.github.jvmspec`; the Gradle
    plugin id remains `io.github.jvmspec`, but Plugin Portal marker availability must be verified
    separately and is not implied by Maven publication or a successful local included build. Do not
    claim signing, staging, deployment, or public availability from local checks alone.

## 11.1 Resolved or Controlled Risks

- **Runtime dependency leakage**: controlled by dependency audits; Phase 12 and Phase 14 root
  runtime audits passed with only the project artifact, Phase 15 root/Maven-plugin runtime audits
  passed with only core for root and plugin plus compile-scope core for the standalone Maven plugin,
  Phase 16 root/Gradle-plugin runtime audits passed with only core for root and core-only Gradle
  runtimeClasspath, Phase 17 root/JUnit-engine runtime audits passed with only core for root and
  isolated JUnit Platform dependencies in the standalone engine, Phase 19 aggregate verification
  repeated root and adapter runtime audits with the same isolation, Phase 20 root/adapter runtime
  audits preserved the same isolation, and Phase 21 root/example runtime dependency checks stayed
  clean.
- **Java 8 runtime compatibility**: controlled by source/target settings, classfile audits, and the
  Phase 12 Java 8 matrix run.
- **Java 25 Gatherer availability assumptions**: controlled by metadata/reflection design and Phase
  12 Java 25 runtime probe.
- **Unsafe constructor deletion by default**: controlled by default `comment` policy and explicit
  `delete` opt-in.
- **No-JUnit foundation availability**: Phase 14 implemented programmatic no-`System.exit`
  invocation, explicit classpath input, and dependency-free JUnit XML-compatible reporting under ADR
  0011.
- **Optional Maven plugin availability**: Phase 15 implemented and verified standalone Maven
  `javaspec:run` integration over `JavaspecLauncher` without requiring JUnit in projects under test.
- **Optional Gradle plugin availability**: Phase 16 implemented and verified standalone Gradle
  `javaspecRun` integration over `JavaspecLauncher` without requiring JUnit in projects under test.
- **Optional JUnit Platform engine availability**: Phase 17 implemented and verified standalone
  JUnit Platform engine integration over `JavaspecLauncher` with isolated engine dependencies and no
  spec authoring style changes.
- **Stable id/source metadata availability**: Phase 18 implemented and verified stable id aliases,
  discovery/runner source metadata, additive JSON report fields, JUnit XML-compatible testcase
  file/line attributes, and optional JUnit Platform descriptor reporting aligned to stable ids.
- **Aggregate release/CI verification availability**: Phase 19 implemented and locally verified
  `scripts/verify-all.sh` plus `.github/workflows/ci.yml`, preserving root core-only Maven
  verification and standalone adapter boundaries.
- **Release-readiness scaffolding availability**: Phase 20 implemented and locally verified
  `scripts/check-version-alignment.sh`, `CHANGELOG.md`, `RELEASING.md`, the confirmed MIT `LICENSE`,
  MIT license and maintainer metadata, Maven `release-artifacts` source/javadoc packaging, Gradle
  source/javadoc jar readiness, and safe URL/SCM/issues metadata without adding
  publishing/signing/secrets/portal publication decisions.
- **Adoption assets availability**: Phase 21 implemented and locally verified standalone
  Maven/Gradle/JUnit Platform examples, `scripts/verify-examples.sh`, JSON report schema
  documentation, golden report examples, and default examples verification in
  `scripts/verify-all.sh` without core runtime changes or public publication.
- **Explicit skipped/pending semantics availability**: Phase 22 implemented and locally verified
  zero-dependency `@Skip`/`@Pending` annotations, unchecked skip/pending signals, `ObjectBehavior`
  helpers, distinct `PENDING` status/counts, pending-aware JSON/JUnit XML-compatible reports,
  Maven/Gradle inherited reporting behavior, and JUnit Platform skipped-event mapping without adding
  core runtime dependencies.
- **Execution availability diagnostics availability**: Phase 23 implemented deterministic
  `RunDiagnostics.executionAvailabilityLines(RunResult)`, enriched not-executable/skipped reasons
  for classloader availability problems, CLI `Execution diagnostics:` output, and Maven/Gradle
  `javaspec:` warning diagnostics with classpath element counts without adding core runtime
  dependencies or changing default/adapters compilation ownership.
- **Configuration-level report destinations availability**: Phase 24 implemented optional top-level
  JSON/JUnit XML-compatible report destinations with CLI and explicit Maven/Gradle adapter override
  precedence, while leaving schemas, writers, dry-run/no-spec behavior, exit codes, and
  build-failure semantics unchanged.
- **ServiceLoader formatter/extension discovery availability**: Phase 25 implemented zero-dependency
  discovery of `RunFormatter`, `JavaspecExtension`, and alias `Extension` providers from CLI and
  Gradle run classloaders through JDK `ServiceLoader`, with built-ins first, duplicate extension
  implementation de-duplication, and `ExtensionLoadingException` diagnostics, while leaving reports,
  dependencies, and publishing unchanged; Phase 32 later adds config-driven activation and adapter
  formatter controls where implemented.
- **Target-profile enforcement availability**: Phase 26 implemented `ProfileEnforcement`,
  `ProfileEnforcementReport`, and `ProfileViolation`, and made `javaspec run` fail with exit `64`
  before generation/update writes when described type kinds or resolvable cataloged generated method
  signature owners exceed the effective target profile, while leaving compilation ownership, report
  schemas, dependencies, and optional adapter architecture unchanged.
- **Bootstrap hook execution availability**: Phase 27 implemented explicit configured bootstrap hook
  class execution before examples, with top-level hooks before selected-suite hooks, order and
  duplicates preserved, immutable `BootstrapContext`, CLI exit `64` plus no reports on bootstrap
  failures, and Maven/Gradle build/task failure diagnostics, while leaving scripts, package
  scanning, dependency resolution, runtime dependencies, Java 8 compatibility, and optional adapter
  architecture unchanged; Phase 33 later adds ServiceLoader-discovered hooks after explicit hooks.
- **Stronger interface doubles availability**: Phase 28 implemented `ArgumentMatcher`,
  `ArgumentMatchers`, `Doubles` matcher aliases, matcher-aware stubs/verifications/call queries,
  argument-constrained stub priority, throwing stubs, and answer callbacks while preserving the
  zero-dependency interface-only core.
- **Opt-in compilation availability**: Phase 29 implemented CLI `--compile` / `--compile-output
  <dir>` source/spec compilation, and Phase 34 extended explicit current-JDK compilation to
  programmatic, Maven, and Gradle paths while leaving config keys, JUnit Platform behavior, report
  schemas/content, dependency resolution, incremental caches, forked `javac`, source-level/release
  management, and core runtime dependencies unchanged.
- **Bytecode doubles availability**: Phase 37 implemented `ConcreteDoubleProvider`,
  `Doubles.concreteDouble` / `classDouble`, the standalone `javaspec-bytecode-doubles` ByteBuddy
  adapter, and `examples/bytecode-doubles-basic/`, while preserving the zero-runtime-dependency core
  and rejecting final/static/constructor/unsupported target cases.

## 11.2 Highest-Priority Future Corrections

1. Keep documentation synchronized whenever implementation changes run controls, generation
   behavior, reports, or extension behavior.
2. Expand profile coverage only with conservative catalog updates or a future ADR for
   compiler-integrated profile checks; do not turn heuristic enforcement into broad type rejection
   without clear false-positive controls.
3. Design extension activation beyond configured class names and ServiceLoader separately from the
   current minimal extension API, including any package scanning, plugin lookup, or automatic
   classpath repair.
4. Preserve Phase 14 invocation/classpath/report behavior plus Phase 15 Maven, Phase 16 Gradle,
   Phase 17 JUnit Platform adapter behavior, Phase 18 stable id/source/report polish, Phase 19
   aggregate release/CI verification, Phase 20 release-readiness scaffolding, Phase 21 adoption
   assets, Phase 22 explicit skipped/pending semantics, Phase 23 execution-availability diagnostics,
   Phase 24 configuration-level report destination precedence, Phase 25 ServiceLoader
   formatter/extension discovery, Phase 26 profile-enforcement boundaries, Phase 27 bootstrap hook
   execution boundaries, Phase 28 stronger interface doubles boundaries, Phase 29/34 opt-in
   compilation boundaries, Phases 30-36 known-limitations resolution (completed), and Phase 37 bytecode adapter
   boundaries (documented) — subsequent work can build on these resolved boundaries.
5. Keep report schema/golden examples, standalone examples, pending/skipped semantics, and
   verification assertions synchronized with future report or adapter behavior changes.
6. Resolve GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials,
   final release version/tag, and final publish approval before designing or claiming public
   publication; keep the confirmed MIT license and maintainer metadata consistent.
