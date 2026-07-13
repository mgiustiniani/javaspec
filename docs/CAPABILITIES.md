# javaspec capabilities audit snapshot

This document records capabilities verified while preparing the 1.0 roadmap. It is not a marketing page; it distinguishes implemented behavior from release-freeze work still required.

## Snapshot

- Audited HEAD: `a71297f1bb234b5faa70eca22abe3a5a3b3d6675`
- Branch: `develop`
- Baseline supplied for the assignment: `e5527b634154cc3156d8e81e6697fab60acaecc3` (ancestor of audited HEAD)
- Existing gate results at audited HEAD before release-line normalization:
  - `git diff --check`: PASS
  - `scripts/check-version-alignment.sh`: PASS (previous pre-1.0 snapshot aligned)
  - `mvn -q verify`: PASS for core
  - `scripts/verify-all.sh`: PASS including Gradle
- Active release candidate line: `1.0.0-RC4`.

## Artifacts and modules

| Artifact/module | Coordinates / plugin id | Runtime baseline | Runtime dependencies | Stability for 1.0 |
|---|---|---:|---|---|
| Core | `io.github.jvmspec:javaspec` | Java 8 | none in runtime dependency tree | REQUIRED_FOR_1_0; API/SPI frozen |
| Maven plugin | `io.github.jvmspec:javaspec-maven-plugin` | Java 8 | core + Maven APIs in plugin context | REQUIRED_FOR_1_0; verified and included in publication workflow |
| JUnit Platform engine | `io.github.jvmspec:javaspec-junit-platform-engine` | Java 8 | core + JUnit Platform engine APIs | REQUIRED_FOR_1_0 adapter; contract frozen |
| Gradle plugin | plugin id `io.github.jvmspec` | Java 8 | core in plugin runtimeClasspath | REQUIRED_FOR_1_0; verified, publication pending Portal confirmation |
| Bytecode doubles | `io.github.jvmspec:javaspec-bytecode-doubles` | Java 8 | core + ByteBuddy | Optional stable adapter for 1.0 |
| Bytecode agent | `io.github.jvmspec:javaspec-bytecode-agent` | Java 8 | core + ByteBuddy + ByteBuddy Agent | Optional stable adapter for 1.0 |

## Implemented core capabilities

- PHPSpec-style public `it_*` / `its_*` example discovery and execution.
- Subject-centric `ObjectBehavior<T>` with lazy construction, `subject()`, `beConstructedWith(...)`, `beConstructedThrough(...)`, named factories, and throw expectations.
- Explicit skipped and pending example signals.
- Core matcher/expectation API with equality, identity/type/count/string/pattern/exception-oriented helpers.
- Zero-dependency interface doubles with stubbing, matchers, captors, ordered verification, call history, sequential answers, default interface methods, and deterministic object methods.
- Prophecy-style collaborators with generated typed wrappers, `ObjectProphecy`, `MethodProphecy`, promises, predictions, custom prediction callbacks, argument tokens, custom `ArgumentToken`, and automatic prediction checks.
- Collaborator parameter injection for supported `let`, example, and `letGo` parameters: ordinary interfaces and generated typed `*Prophecy` wrappers.
- PHPSpec-style example data with `row(...)`, `examples(...)`, `Example1`/`Example2`, row recording, JSON/JUnit XML/JUnit Platform row diagnostics.
- CLI discovery, run, generation, dry-run, reports, compile, profile enforcement, local POM dependency resolution, class/example filters, generated support, and generated Prophecy wrappers.
- Java source generation/update support for classes, interfaces, records, sealed types, annotations, constructors, methods, support classes, and Prophecy wrappers.
- Record hardening: existing-record kind preservation, record header evolution, support default construction, compact-constructor preservation, explicit record construction prefix padding at runtime.
- Optional Maven, Gradle, JUnit Platform, bytecode doubles, and bytecode agent integrations.

## Frozen contracts and explicitly deferred capabilities

- API/SPI classification is frozen in `docs/api-surface-1.0.md`; `scripts/check-api-surface.sh` guards unclassified shipped Java packages.
- JSON schema is versioned (`schemaVersion: 1`) and documented in `docs/result-contract-1.0.md`.
- JUnit Platform selectors, unique IDs, sources, status mapping, and row descriptor/event filtering are documented in `docs/junit-platform-contract-1.0.md` and `docs/example-data-contract-1.0.md` as the 1.0 contract.
- Example data APIs, Prophecy/collaborator APIs, generation semantics, extension SPI, and report contracts have 1.0 contract documents.
- Generated typed Prophecy wrappers remain the canonical collaborator API; reflective `method("...")` is a bootstrap/fallback path.
- Approximate numeric, iterator, and generated object-state matchers are implemented as Java-adapted PHPSpec semantics; custom matcher 1.0 scope is frozen in `docs/matcher-contract-1.0.md`.
- Event/extension model v2 is deferred; existing extension/formatter/parser/resolver/bootstrap surfaces are classified for 1.0.
- Mutating generation paths use atomic source-file writes, and pending generated stubs produce a synthetic `BROKEN` result to prevent accidental GREEN.
- Release dry-run coverage verifies all declared modules/artifacts and external consumer examples.

## Public 1.0 contract areas

The API freeze classifies these shipped areas in `docs/api-surface-1.0.md`:

- `io.github.jvmspec.api`: `ObjectBehavior`, example data APIs, lifecycle helpers, skip/pending, Prophecy convenience hooks.
- `io.github.jvmspec.runner`: `SpecRunner`, result model, statuses, failure detail.
- `io.github.jvmspec.reporting`: JSON and JUnit XML report writers and schema.
- `io.github.jvmspec.formatter`: formatter API and registry.
- `io.github.jvmspec.extension`: extension activation and context.
- `io.github.jvmspec.parser`: parser SPI.
- `io.github.jvmspec.dependency`: dependency resolver SPI.
- `io.github.jvmspec.doubles` and `io.github.jvmspec.doubles.prophecy`: doubles/Prophecy API.
- `io.github.jvmspec.generation` and `io.github.jvmspec.model`: generation/model surfaces; many may need INTERNAL classification.
- `io.github.jvmspec.invocation`: programmatic launcher and result mapping.
- Maven plugin parameters, Gradle extension/task properties, JUnit Platform engine id and unique IDs.

## Documentation gaps

- Active roadmap was mixed with historical implementation notes before this audit.
- Historical docs still mention previous `0.1.0` releases; current docs are guarded by `scripts/check-current-docs.sh`.
- Historical documents may keep pre-migration package/version references only with migration context; current docs are checked automatically.
- README, user manual, CLI/configuration/matcher/Prophecy/generation/adapter docs, migration, troubleshooting, and compatibility policy have 1.0 contract coverage.
- Release notes use `docs/release-notes-1.0.0.md`; successful publication evidence is still pending.
- Release engineering docs cover the real artifact set, Git Flow through `main`/`develop` and
  `release/*`, Maven Central signing/deployment, and the Gradle Plugin Portal marker path.

## Risk register

| ID | Priority | Risk | Required action |
|---|---|---|---|
| P0-REL-001 | P0 | Version line and release-note naming needed normalization for 1.0 preparation. | DONE in first 1.0 audit slice: normalized to `1.0.0-SNAPSHOT`, created `docs/release-notes-1.0.0.md`, and added automated current-doc check. |
| P0-API-001 | P0 | Public API/SPI classification was missing. | DONE: `docs/api-surface-1.0.md` classifies packages/contracts and `scripts/check-api-surface.sh` gates unclassified packages. |
| P0-GEN-001 | P0 | Generation needed frozen structured outcomes and atomic-write audit. | DONE: `docs/generation-contract-1.0.md`, `AtomicFileWriter`, and pending-stub synthetic BROKEN result. |
| P0-REL-002 | P0 | Release workflow may not publish/verify every real artifact. | DONE: `scripts/verify-release-dry-run.sh`, `RELEASING.md`, and bytecode-agent release metadata/artifacts verify the declared artifact set and consumer examples. |
| P0-DOC-001 | P0 | Documentation can contradict current capabilities/version. | DONE: `scripts/check-current-docs.sh` and `scripts/check-version-alignment.sh` gate current docs/version/package consistency. |
| P1-ROW-001 | P1 | Example-data row selector semantics are subtle and could be misrepresented. | DONE: `docs/example-data-contract-1.0.md` documents inline execution vs descriptor/event filtering, with JUnit Platform regression coverage. |
| P1-PROP-001 | P1 | Generated Prophecy token overloads need edge-case audit. | DONE: `ProphecySkeletonGeneratorTest` covers primitive, array, varargs, bounded generic, bridge/synthetic, duplicate, and mixed exact/token call cases; `docs/prophecy-contract-1.0.md` documents limits. |
| P1-MATCH-001 | P1 | Inline/configured custom matcher scope is not finalized. | DONE: `docs/matcher-contract-1.0.md` freezes programmatic `MatcherRegistry`/`shouldMatch(...)` support and defers config/inline dynamic custom matcher conveniences. |
| P1-EXT-001 | P1 | Event/extension v2 scope could affect API freeze. | DONE: `docs/extension-spi-1.0.md` freezes existing SPI semantics and defers typed event model v2. |
| P1-JUNIT-001 | P1 | JUnit Platform IDE/source/selector parity needed a contract audit. | DONE: `docs/junit-platform-contract-1.0.md` freezes engine id, selector, unique-id, source, row, status, and IDE boundaries with engine regression coverage. |
| P2-PERF-001 | P2 | No large-suite performance baseline yet. | Add reproducible benchmark before final 1.0 if feasible; otherwise document post-1.0 plan. |

## Decisions required before API freeze

The P0/P1 decisions for API/SPI classification, example-data row execution, PHPSpec matcher scope, custom matcher scope, event model v2 deferral, JSON schema evolution, generation safety, and release dry-run artifact responsibilities are recorded in the 1.0 contract documents. Remaining decisions are P2 polish or RC evidence gates unless release-candidate evidence exposes a contradiction.
