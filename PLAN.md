# javaspec 1.0 Active Roadmap

This file is the active roadmap toward javaspec 1.0.0. Historical implementation notes were archived in [`docs/history/implementation-plan-pre-1.0.md`](docs/history/implementation-plan-pre-1.0.md). Current capability evidence lives in [`docs/CAPABILITIES.md`](docs/CAPABILITIES.md), and release gates live in [`docs/release-1.0-checklist.md`](docs/release-1.0-checklist.md).

## Product identity

PHPSpec-first is the main axis of the javaspec roadmap. javaspec is not a Java test framework with several PHPSpec features; it is a PHPSpec-first framework designed for the Java language. The canonical authoring model is public `it_*` / `its_*` examples, `let` / `letGo`, `subject()`, `beConstructedWith` / `beConstructedThrough`, `should*` matchers, Prophecy-style collaborators, and behavior-guided generation.

Non-goals for core: JUnit/Jupiter clone, Cucumber/Gherkin runner, general-purpose dependency injection, service container, mandatory template engine, or AI/coding-agent workflow runtime.

Core constraints:
- Java 8 runtime compatibility.
- Zero third-party runtime dependencies in `io.github.jvmspec:javaspec`.
- Dependency-heavy integrations remain optional artifacts.
- Generation must produce minimal mechanical scaffolding and must not hide meaningful RED feedback.

## Current HEAD and verification baseline

- Original 1.0 assignment baseline: `e5527b634154cc3156d8e81e6697fab60acaecc3`.
- Current restructuring baseline: `7fd7054ba02c0217d1ce3fe783ae5c95c5a5b00a`.
- The original baseline is an ancestor of the current restructuring baseline.
- Constructor hardening, deterministic generation reporting, and record/generic reconciliation are
  present on `develop`; quality-report and restructuring work may be staged above this baseline.
- Existing gates executed at the pre-normalization audited HEAD:
  - `git diff --check`: PASS
  - `scripts/check-version-alignment.sh`: PASS, baseline was the previous pre-1.0 snapshot
  - `mvn -q verify`: PASS for core
  - `scripts/verify-all.sh`: PASS including Gradle
- The active published Maven release candidate is `1.0.0-RC4`; post-RC4 constructor and generation
  hardening remains on `develop` until the next explicitly approved release candidate.

## Priority definitions

Priorities are ordered by product identity:

1. coherence of the PHPSpec semantic model;
2. safety and predictability of behavior-driven generation;
3. stability of APIs, CLI, lifecycle, results, and diagnostics;
4. faithful build and IDE adapters;
5. release engineering;
6. secondary integrations.

- **P0**: blocks the PHPSpec-first semantic core, safe generation, API contract, publication, or truthful documentation.
- **P1**: important for a credible 1.0 but releasable if explicitly documented in the compatibility matrix without contradicting core promises.
- **P2**: post-1.0 improvement; must not indefinitely hold 1.0 and must not be required for the promised PHPSpec workflow.

Disposition labels:
- `REQUIRED_FOR_1_0`
- `REQUIRED_BEFORE_RC`
- `DEFERRED_WITH_DOCUMENTED_LIMIT`
- `REJECTED_AS_NON_GOAL`

## Active milestones

The milestones are grouped by these macro-areas:

A. **PHPSpec semantic core** — subject, construction, examples, lifecycle, expectations, matchers, collaborators, predictions, pending, example data, generation semantics, and diagnostics.
B. **Safe behavior-driven generation** — minimal skeletons, fail-closed planning, dry-run parity, source preservation, and no accidental GREEN.
C. **Stable diagnostics, APIs and reports** — public contracts, CLI/config/report schemas, result taxonomy, and compatibility policy.
D. **Faithful build and IDE adapters** — Maven, Gradle, JUnit Platform, bytecode adapters, and external consumer evidence that translate javaspec semantics without changing the authoring model.
E. **Release readiness** — versioning, workflows, artifact publication, release candidates, and reproducible gates.

### M0 — Source of truth and release line cleanup

- Priority: P0
- Disposition: REQUIRED_FOR_1_0
- Status: COMPLETED
- Motivation: the previous `PLAN.md` mixed active roadmap, history, historical test reports, and pre-1.0 implementation notes. The repository now has an active roadmap, archived historical plan, capability audit, release checklist, and current-doc/version check.
- Dependencies: none.
- Acceptance criteria:
  - `docs/release-1.0-audit.md` exists and records the audited HEAD, modules, capabilities, risks, and API-freeze decisions.
  - Historical roadmap content is archived under `docs/history/implementation-plan-pre-1.0.md`.
  - `PLAN.md` is limited to active 1.0 roadmap, priorities, acceptance criteria, and evidence.
  - `docs/CAPABILITIES.md` describes verified current capabilities and limits.
  - `docs/release-1.0-checklist.md` defines release gates.
  - An automated current-doc/version check prevents stale release-note names and obsolete current package references outside archived areas.
- Verification:
  - `git diff --check`
  - `scripts/check-version-alignment.sh`
  - `scripts/check-current-docs.sh`
  - `mvn -q verify`
- Completion commit: pending.

### M1 — Version strategy and release metadata normalization

- Priority: P0
- Disposition: REQUIRED_FOR_1_0
- Status: COMPLETED
- Motivation: the project is preparing a 1.0 line; project versions are now on `1.0.0-RC1` and active release notes live in `docs/release-notes-1.0.0.md`.
- Dependencies: M0.
- Acceptance criteria:
  - Development line version is consistently `1.0.0-SNAPSHOT` before RC work.
  - Release notes use `docs/release-notes-1.0.0.md`.
  - README, user manual, examples, POMs, Gradle plugin, and release docs agree on version semantics.
  - Version alignment checks cover all real artifacts and examples.
- Verification:
  - `scripts/check-version-alignment.sh`
  - `scripts/check-current-docs.sh`
  - `mvn -q verify`
  - `scripts/verify-all.sh`
- Completion commit: pending.

### M2 — API/SPI contract classification and compatibility baseline

- Macro-area: C — Stable diagnostics, APIs and reports
- Priority: P0
- Disposition: REQUIRED_BEFORE_RC
- Status: COMPLETED
- Motivation: 1.0 requires an explicit contract for public API, public SPI, internal implementation, and pre-1.0 deprecated surfaces.
- Dependencies: M0, M1.
- Acceptance criteria:
  - Public API/SPI classification is documented for core, adapters, runner/result model, reporting, extension, parser, dependency resolver, doubles/Prophecy, generation, launcher, Maven/Gradle properties, and JUnit Platform unique IDs.
  - SemVer, deprecation, support-window, and breaking-change policies are documented.
  - API compatibility tooling is added as a build/test tool only and does not create a core runtime dependency.
  - A 1.0 API baseline generation procedure is documented for API freeze.
- Evidence:
  - `docs/api-surface-1.0.md` classifies shipped packages and external contracts as `PUBLIC_API`, `PUBLIC_SPI`, `ADAPTER_API`, `GENERATED_API`, or `INTERNAL`.
  - `scripts/check-api-surface.sh` fails when a shipped Java package is not classified.
  - `scripts/verify-all.sh` runs the API surface classification check.
- Verification:
  - `scripts/check-api-surface.sh`
  - `mvn -q verify`
  - `scripts/verify-all.sh`
- Completion commit: pending.

### M3 — CLI/config/report contract freeze

- Macro-area: C — Stable diagnostics, APIs and reports
- Priority: P0
- Disposition: REQUIRED_BEFORE_RC
- Status: COMPLETED
- Motivation: 1.0 must freeze command semantics, configuration precedence, exit codes, and report schemas.
- Dependencies: M2.
- Acceptance criteria:
  - CLI options, aliases, config precedence, exit codes, dry-run, generation, filters, classpath, profiles, and programmatic launcher are documented and covered by contract tests.
  - PASSED/FAILED/BROKEN/SKIPPED/PENDING mapping is specified across CLI, launcher, JSON, JUnit XML, JUnit Platform, Maven, and Gradle.
  - JSON schema evolution policy and golden fixtures are documented.
  - Pending generation/stubs cannot look like final GREEN in formatter summaries or machine reports.
- Verification:
  - Core CLI/report tests
  - Maven/Gradle/JUnit Platform adapter tests
  - `scripts/verify-all.sh`
- Evidence:
  - `docs/result-contract-1.0.md` documents state semantics, exit codes, adapter mappings, report schema policy, and diagnostics taxonomy.
  - `docs/release-1.0-acceptance-tests.md` maps mandatory semantic scenarios to automated tests.
  - `MainPhase29CompileCliTest.runCompileWithPendingGeneratedStubCannotAccidentallyGreen` protects pending generated stubs from accidental GREEN.
- Completion commit: pending.

### M4 — Example data contract freeze

- Macro-area: A — PHPSpec semantic core
- Compatibility matrix status: `JAVA_ADAPTED`
- Priority: P0
- Disposition: REQUIRED_BEFORE_RC
- Status: COMPLETED
- Motivation: Phase 47 features are implemented, but the 1.0 contract needs explicit decisions around row selectors, row result structure, lifecycle, stop-on-failure, and public/internal API boundaries.
- Dependencies: M3.
- Acceptance criteria:
  - Decision recorded: rows remain inline with descriptor/event filtering, or true isolated row execution is implemented.
  - Selection semantics document the case where an earlier inline row fails before a selected row.
  - Row results use stable structured status/failure semantics and tests cover JSON, JUnit XML, JUnit Platform, and stop-on-failure.
  - `ExampleDataRowRecorder` public/internal contract and ThreadLocal/sequential execution limits are documented.
- Verification:
  - Phase 47 CLI/report tests
  - JUnit Platform row descriptor/selector tests
  - `scripts/verify-all.sh`
- Evidence:
  - `docs/phpspec-compatibility-matrix.md` records inline row execution and adapter translation as the 1.0 Java adaptation.
  - `docs/release-1.0-acceptance-tests.md` maps row lifecycle/failure/report/JUnit Platform evidence.
- Completion commit: pending.

### M5 — Collaborator injection and Prophecy 1.0 contract freeze

- Macro-area: A — PHPSpec semantic core
- Compatibility matrix status: `JAVA_ADAPTED`; Prophecy is part of the promised PHPSpec experience, not a generic optional mock layer
- Priority: P0
- Disposition: REQUIRED_BEFORE_RC
- Status: COMPLETED
- Motivation: Phase 48 and 49 implementation is complete enough for 1.0; the 1.0 collaborator/Prophecy contract is documented and linked to tests.
- Dependencies: M3.
- Acceptance criteria:
  - Collaborator injection contract covers interface doubles, typed `*Prophecy` wrappers, reuse, duplicate type rejection, unsupported types, inherited lifecycle methods, deterministic parameter order, prediction verification, teardown precedence, and suppressed failures.
  - Prophecy wrapper `Object` token overload audit covers same-name/same-arity overloads, primitive/boxing, arrays, varargs, generic methods, wildcards, generic return types, existing `Object` parameters, zero-arg methods, default interface methods, static/bridge/synthetic methods, null literal, token/exact mixes, duplicate generation, and ambiguous Java calls.
  - Any deferred edge case is explicitly documented with a test or non-goal statement.
- Verification:
  - `mvn -q -Dtest=SpecRunnerTest,ProphecySkeletonGeneratorTest,MethodProphecyTest,DoublesTest test`
  - `scripts/verify-all.sh`
- Evidence:
  - `docs/prophecy-contract-1.0.md` documents collaborator injection, prediction lifecycle, typed wrappers, argument tokens, callbacks, Java overload adaptation, and diagnostics.
  - Phase 48/49 tests and `examples/prophecy-basic` are included in `scripts/verify-all.sh`.
- Completion commit: pending.

### M6 — Matcher parity 1.0 scope

- Macro-area: A — PHPSpec semantic core
- Compatibility matrix status: approximate numeric, iterator, and generated object-state matchers are `JAVA_ADAPTED`
- Priority: P0
- Disposition: REQUIRED_BEFORE_RC
- Status: COMPLETED
- User behavior: users express behavior through readable `should*` expectations that produce meaningful RED feedback, including Java-adapted object-state, iterable, and numeric expectations.
- Java intentional difference: object-state expectations use deterministic generated support methods and accessor inference instead of PHP runtime dynamic method lookup.
- Risk if deferred: inline/configured custom matcher APIs can be deferred only if public docs keep the 1.0 matcher promise narrowed to the implemented PHPSpec-first core.
- Motivation: Phase 50 is implemented for approximate numeric, iterator, and generated object-state matchers. Inline/configured custom matcher expansion is deferred as P1/post-1.0 scope because `MatcherRegistry`/`shouldMatch` already provide a documented minimal custom matcher surface.
- Dependencies: M3.
- Acceptance criteria:
  - Approximate numeric matchers are implemented and documented with Java decimal comparison semantics.
  - Iterable/Iterator matchers are implemented and documented with explicit iterator-consumption semantics.
  - Dynamic object-state expectations are implemented with generated Java support helpers, accessor inference, and ambiguity skip rules.
  - Inline/configured custom matcher expansion is deferred with the existing `MatcherRegistry`/`shouldMatch` surface documented as the 1.0 minimum.
- Verification:
  - Matcher tests added for any implemented scope.
  - Documentation updated for any deferred scope.
  - Contract tests required before RC: custom matcher registration or documented deferral.
- Evidence:
  - Approximate numeric: `MatchableTest`, `ObjectBehaviorTest`.
  - Iterator semantics: `MatchableBoundedIterableTest`, `MatchableTest` iterator cases.
  - Generated object-state: `SpecSkeletonGeneratorTest`, `SpecDiscoveryTest`.
  - Custom matcher 1.0 scope: `docs/matcher-contract-1.0.md`, `MatcherRegistryTest`, `MatchableTest.shouldMatchUsesCustomMatcher`.
- Completion commit: pending.

### M7 — Safe generation P0 hardening

- Macro-area: B — Safe behavior-driven generation
- Priority: P0
- Disposition: REQUIRED_BEFORE_RC
- Status: COMPLETED
- Motivation: generation mutates user sources and is the riskiest 1.0 surface.
- Dependencies: M0, M3.
- Acceptance criteria:
  - Mutating generation paths produce structured plan/result semantics distinguishing planned, applied, already satisfied, no change, refused ambiguous, unsupported source, incompatible type, and failed.
  - Dry-run parity exists for every mutating path included in 1.0.
  - File writes are atomic where the filesystem supports it.
  - Record updates use record components as semantic source for existing records and do not choose canonical constructors by largest arity.
  - Type identity does not collapse unknown, real Object, raw, parameterized, wildcard, type variable, array, varargs, primitive, boxed, and fully qualified types incorrectly.
  - Adversarial parser/source fixtures are added for comments, strings, nested types, records, sealed types, CRLF, Unicode, no-final-newline, and modern Java syntax.
- Evidence:
  - `docs/generation-contract-1.0.md` records structured generation outcomes, dry-run/mutation rules, pending stub semantics, and atomic-write policy.
  - `AtomicFileWriter` routes generated/source writes through same-directory temporary files and atomic move where supported.
  - `MainPhase29CompileCliTest.runCompileWithPendingGeneratedStubCannotAccidentallyGreen` prevents generated default accidental GREEN.
- Verification:
  - Generation test matrix
  - `run --generate --compile` CLI regressions
  - `AtomicFileWriterTest`
  - `scripts/verify-all.sh`
- Completion commit: pending.

### M8 — Event/extension SPI freeze

- Macro-area: C — Stable diagnostics, APIs and reports
- Priority: P1
- Disposition: REQUIRED_BEFORE_RC for API decision; full v2 is DEFERRED_WITH_DOCUMENTED_LIMIT.
- Status: COMPLETED
- Motivation: event/extension changes after 1.0 are expensive, but current extension surfaces are sufficient when clearly scoped.
- Dependencies: M2.
- Acceptance criteria:
  - Decide whether typed suite/spec/example/row/generation/report events are in 1.0 or deferred.
  - Existing formatter, parser, dependency resolver, bootstrap hook, and extension APIs are classified and documented.
  - Listener ordering, classloader, failure semantics, and cleanup behavior are documented for any 1.0 event API.
- Evidence:
  - `docs/extension-spi-1.0.md` freezes existing extension, formatter, bootstrap, parser, and resolver SPI semantics.
  - Typed event model v2 is explicitly deferred from 1.0.
  - Extension activation now temporarily sets the thread context classloader to the effective run classloader and restores it after success or failure.
- Verification:
  - Extension tests and ServiceLoader tests
  - `scripts/verify-all.sh`
- Completion commit: pending.

### M9 — Adapter parity and consumer verification

- Macro-area: D — Faithful build and IDE adapters
- Priority: P0
- Disposition: REQUIRED_BEFORE_RC
- Status: COMPLETED
- Motivation: optional artifacts must translate javaspec semantics faithfully and be verified like external consumers.
- Dependencies: M3, M4, M5.
- Acceptance criteria:
  - JUnit Platform class/method/package/unique-id/row selectors, file/line sources, display names, discovery-only behavior, and status mapping are contract-tested.
  - Maven and Gradle plugins have smoke consumer tests using installed/staged artifacts, including generated sources, reports, failure propagation, filters, and classpath.
  - Bytecode doubles and bytecode agent consumer examples verify final/static/constructor capabilities and restore behavior.
  - IDE workflow docs are updated for IntelliJ, Maven Surefire, Gradle Test, and JUnit Platform console launcher.
- Evidence:
  - `scripts/verify-all.sh` verifies Maven, Gradle, JUnit Platform, bytecode doubles/agent modules, and standalone examples.
  - `scripts/verify-release-dry-run.sh` packages release artifacts and runs external consumer examples from locally staged artifacts.
  - `docs/release-1.0-acceptance-tests.md` maps adapter semantic evidence.
- Verification:
  - `scripts/verify-all.sh`
  - `scripts/verify-release-dry-run.sh`
- Completion commit: pending.

### M10 — Release engineering and RC pipeline

- Macro-area: E — Release readiness
- Priority: P0
- Disposition: REQUIRED_FOR_1_0
- Status: COMPLETED
- Motivation: 1.0 must be publishable without hidden manual steps or partial artifact publication.
- Dependencies: M1 through M9 P0 items.
- Acceptance criteria:
  - `RELEASING.md` covers core, Maven plugin, JUnit Platform engine, bytecode-doubles, bytecode-agent, Gradle plugin, and Gradle plugin marker publication.
  - Release dry-run builds artifacts from a clean checkout, inspects POMs/manifests/source/javadoc jars, and runs external Maven/Gradle/JUnit Platform/bytecode consumers.
  - Final-release no-SNAPSHOT dependency and tag alignment checks are performed when the RC/final version is cut.
  - CI/release workflows publish every declared artifact or fail clearly.
  - RC flow is documented: `1.0.0-SNAPSHOT -> 1.0.0-RC1 -> 1.0.0`.
- Evidence:
  - `scripts/verify-release-dry-run.sh` packages and verifies all declared Maven/Gradle artifacts and consumer examples.
  - `RELEASING.md` documents RC/final flow, artifact set, signing/Central/Gradle publication expectations, and post-release steps.
  - `javaspec-bytecode-agent/pom.xml` now has release metadata, Central publication config, and source/Javadoc release artifacts like the other Maven artifacts.
- Verification:
  - `scripts/verify-release-dry-run.sh`
  - `scripts/verify-all.sh`
  - CI matrix evidence when available
- Completion commit: pending.

### M11 — Java language coverage closure

- Macro-area: B/E — Safe behavior-driven generation and release readiness
- Priority: P0
- Disposition: REQUIRED_FOR_1_0
- Status: IN PROGRESS — JLC-0 through JLC-7 and the strict zero-PLANNED manifest gate are implemented; classic-PHPSpec real-project dogfooding remains.
- Motivation: the Java 8/11/17/21/25 runtime and profile matrix is green, but stable source-compatibility claims also require systematic parser, preservation, generation, compilation, execution, and idempotence evidence for final language constructs relevant to each profile.
- Product constraint: modern-Java support must serve the classic PHPSpec-inspired subject-centric workflow. It must not invent domain behavior, hide RED, add a second authoring model, or require javaspec to generate every Java grammar production.
- Dependencies: M3, M7, M9, M10.
- Plan and evidence contract:
  - [`docs/java-language-coverage-roadmap.md`](docs/java-language-coverage-roadmap.md) classifies each construct as generated, updated, preserved, profile-gated, or intentionally unsupported.
  - JLC-0 provides a test-only construct inventory, TSV manifest, `COVERED`/`PLANNED` evidence states, deterministic report, strict stable switch, and first Java 8 discovery/update/compile/idempotence fixture.
  - JLC-1 adds text-block-aware offset masking, direct-member scope isolation, and fixtures for comments/literals, lambdas, method references, local/anonymous types, and multiple top-level types.
  - JLC-2 adds release-8 update/compile/idempotence fixtures for final classes, interfaces, enums, annotations, interface default/static methods, type-use/repeatable annotations, complex generic/array/varargs signatures, and fail-closed spec-lambda SAM target inference from explicit typing or one unambiguous production signature.
  - JLC-3 covers local `var`, `var` lambda parameters, private interface helpers, anonymous diamond classes, effectively-final try-with-resources, module descriptor preservation, and representative Java 11 API compile/run evidence.
  - JLC-4 covers records, multi-file sealed hierarchies, top-level non-sealed and abstract subjects, text blocks, switch expressions, `instanceof` patterns, and representative Java 17 API execution.
  - JLC-5 covers nested record patterns, pattern switches including guarded `when`, multi-file sealed exhaustiveness, Sequenced Collections, and joined virtual-thread execution on Java 21.
  - JLC-6 covers unnamed variables/patterns, flexible constructor bodies, module imports, fail-closed compact-source refusal, Markdown documentation comments, and Stream Gatherers on Java 25.
  - JLC-7 covers CRLF, no-final-newline, BOM, Unicode, local indentation, dry-run parity, atomic failure cleanup, idempotence, and actionable source diagnostics.
  - The shared fixture harness expands across later JLC milestones to cover dry-run, one mechanical update, untouched-region preservation, `javac --release`, selected behavior execution, second-pass idempotence, and older-profile refusal where applicable.
  - CI partitions fixtures across Java 8, 11, 17, 21, and 25.
  - Java 21 real-project dogfooding uses the remotely published RC and preserves typed proxy plus explicit `subject()` equivalence.
- Stable blockers:
  - source corruption or ambiguous mutation;
  - accidental GREEN from generated bodies;
  - a promised final construct with no tested disposition;
  - Java 21/25 source accepted without matching compile/update/idempotence evidence.
- Verification:
  - Java language fixture suite on each matching JDK
  - `mvn -q verify`
  - `scripts/verify-all.sh`
  - `scripts/verify-release-dry-run.sh`
- Completion commit: pending.

### M12 — Constructor-safe restructuring and internal language seams

- Macro-area: B/C — Safe generation and internal architecture
- Priority: P0 for constructor correctness and documentation; P1 for behavior-preserving extraction
- Disposition: REQUIRED_FOR_1_0, without adding another language
- Status: IN PROGRESS
- Motivation: post-RC4 constructor fixes exposed unrecognized package-private/generic constructors,
  qualified-type identity collisions, duplicated Java lexical/type normalization, and concentrated
  orchestration. The same restructuring can establish a private frontend/backend seam for future
  language-specific products without expanding the 1.0 feature promise.
- Scope before 1.0:
  - preserve package-private and generic constructors and distinct qualified overloads;
  - canonicalize source names before JVM constructor-signature comparison;
  - route Java discovery through `JavaSpecLanguageFrontend`;
  - keep the public `SpecDiscovery` facade while extracting focused Java construction,
    construction-argument, callable, subject-declaration, and example-discovery components;
  - separate Java inference orchestration from literal/factory inference, expression-argument
    splitting, and source method/import context parsing;
  - reduce `ClassMethodUpdater` by extracting Java type-kind eligibility, method/factory rendering,
    and shared identifier validation without changing source insertion behavior;
  - project subject shape, relationships, structured types, construction/callable signatures,
    invocation kind, and unknown-type evidence through the internal `BehaviorContract` while
    retaining `DescribedType` as the Java compatibility bridge;
  - route in-memory production synchronization through `JavaProductionLanguageBackend` and
    `BehaviorContract`;
  - keep the language seam internal, Java-only, dependency-free, and absent from CLI/config/SPI;
  - synchronize README, user manual, architecture decisions, release evidence, and documentation
    guards with the actual RC4/develop boundary.
- Explicitly deferred until after 1.0:
  - Kotlin or other language parsers/generators;
  - public language registration or ServiceLoader SPI;
  - `--spec-language` / `--production-language` options;
  - cross-language generation claims.
- Acceptance criteria:
  - supported public API contracts and the runtime dependency tree are unchanged; additive
    `INTERNAL` inventory entries are allowed before the next RC;
  - existing Java generated outputs, authorization, dry-run, reports, and idempotence remain stable;
  - constructor regressions cover package-private, generic-bound, and qualified-name overloads;
  - all core, adapter, language-manifest, example, and release dry-run gates pass.
- Decision: [`ADR 0026`](docs/adr/0026-internal-language-seams-before-1.0.md).
- Completion commit: pending.

## Deferred or rejected for 1.0

- AI/coding-agent integrations: REJECTED_AS_NON_GOAL for javaspec 1.0 core.
- Cucumber/Gherkin runner, feature files, step definitions: REJECTED_AS_NON_GOAL.
- General-purpose DI/container model: REJECTED_AS_NON_GOAL.
- Mandatory template engine in core: REJECTED_AS_NON_GOAL.
- Full Phase 51 event model v2: candidate for DEFERRED_WITH_DOCUMENTED_LIMIT unless M8 decides it is required for API freeze.
- Optional TAP/TeamCity/HTML reporters and Open Test Reporting adapter: P2 unless needed by release consumers.
- Kotlin or other JVM-language frontends/backends: post-1.0. The internal Java-only seam in M12 must
  be validated before any public language SPI or language-specific artifact is named or frozen.

## Definition of done for 1.0.0

1. No open P0s.
2. API/SPI/CLI/report/generation contracts frozen and documented.
3. Core remains Java 8 runtime-compatible and zero-runtime-dependency.
4. `git diff --check`, `scripts/check-version-alignment.sh`, `scripts/check-current-docs.sh`, `mvn -q verify`, and `scripts/verify-all.sh` pass from a clean checkout.
5. Release dry-run passes and verifies every declared artifact as an external consumer.
6. README, user manual, release notes, checklist, changelog, POMs, Gradle plugin, and examples agree on version and capabilities.
7. Known P1/P2 limitations are explicit and do not contradict 1.0 promises.
