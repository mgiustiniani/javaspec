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
- Active release line after this audit slice: `1.0.0-SNAPSHOT`.

## Artifacts and modules

| Artifact/module | Coordinates / plugin id | Runtime baseline | Runtime dependencies | Stability for 1.0 |
|---|---|---:|---|---|
| Core | `io.github.jvmspec:javaspec` | Java 8 | none in runtime dependency tree | REQUIRED_FOR_1_0; API/SPI freeze pending |
| Maven plugin | `io.github.jvmspec:javaspec-maven-plugin` | Java 8 | core + Maven APIs in plugin context | REQUIRED_FOR_1_0 if published; release workflow audit pending |
| JUnit Platform engine | `io.github.jvmspec:javaspec-junit-platform-engine` | Java 8 | core + JUnit Platform engine APIs | REQUIRED_BEFORE_RC adapter contract audit |
| Gradle plugin | plugin id `io.github.jvmspec` | Java 8 | core in plugin runtimeClasspath | REQUIRED_FOR_1_0 if plugin marker is published; release workflow audit pending |
| Bytecode doubles | `io.github.jvmspec:javaspec-bytecode-doubles` | Java 8 | core + ByteBuddy | Optional adapter; preview/stable label pending |
| Bytecode agent | `io.github.jvmspec:javaspec-bytecode-agent` | Java 8 | core + ByteBuddy + ByteBuddy Agent | Optional adapter; preview/stable label pending |

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

## Partially verified or contract-pending capabilities

- Public API/SPI classification is not frozen for 1.0.
- JSON schema is versioned (`schemaVersion: 1`) but 1.0 evolution policy and compatibility contract need freeze.
- JUnit Platform row selectors currently filter descriptors/events while row execution remains inline in the owning example; this is intentional but must be documented as the 1.0 contract or changed before RC.
- `ExampleDataRowRecorder` is public under `api`; its 1.0 public/internal status and ThreadLocal/sequential execution limits require a decision.
- Generated typed Prophecy `Object` token overloads require an explicit edge-case audit for overload ambiguity, primitive/boxing, arrays, varargs, generics, bridge/synthetic methods, and mixed token/exact calls.
- Approximate numeric and iterator matchers are implemented; dynamic object-state and inline/configured matcher scope still require a 1.0 decision.
- Event/extension model v2 is not implemented; existing extension/formatter/parser/resolver/bootstrap surfaces require classification.
- Generation result semantics are not yet a uniform structured plan/result model across every mutating path.
- Atomic file-write guarantees and adversarial parser fixture coverage need audit/hardening.
- Release workflow coverage for all modules/artifacts is incomplete until audited.

## Public contracts already visible

Areas needing classification before API freeze:

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
- Current areas still contain some historical pre-migration package references; historical documents may keep them only with migration context.
- README and manual need a 1.0 pass after version strategy is applied.
- Release notes now use `docs/release-notes-1.0.0.md`; content still needs final RC evidence.
- Release engineering docs need all real artifacts and Gradle Plugin Portal marker details.
- Generation safety guide, API/SPI classification, compatibility policy, and migration guide are not yet 1.0-ready.

## Risk register

| ID | Priority | Risk | Required action |
|---|---|---|---|
| P0-REL-001 | P0 | Version line and release-note naming needed normalization for 1.0 preparation. | DONE in first 1.0 audit slice: normalized to `1.0.0-SNAPSHOT`, created `docs/release-notes-1.0.0.md`, and added automated current-doc check. |
| P0-API-001 | P0 | Public API/SPI not classified; accidental implementation classes may become 1.0 contract. | Classify packages/classes and add compatibility tooling. |
| P0-GEN-001 | P0 | Generation mutates user sources without a uniform structured plan/result model and atomic-write guarantee audit. | Introduce/freeze generation result semantics and fail-closed behavior. |
| P0-REL-002 | P0 | Release workflow may not publish/verify every real artifact. | Add release dry-run/staging verification and update workflows/docs. |
| P0-DOC-001 | P0 | Documentation can contradict current capabilities/version. | Add automated current-doc/version/package checks. |
| P1-ROW-001 | P1 | Example-data row selector semantics are subtle and could be misrepresented. | Document and test inline row execution vs descriptor/event filtering. |
| P1-PROP-001 | P1 | Generated Prophecy token overloads need edge-case audit. | Add regression matrix and document limits. |
| P0-MATCH-001 | P0 | Dynamic object-state matcher contract is not finalized. | Implement or explicitly defer/narrow scope before RC without contradicting the PHPSpec-first promise. |
| P1-EXT-001 | P1 | Event/extension v2 scope could affect API freeze. | Decide 1.0 scope and classify existing extension APIs. |
| P2-PERF-001 | P2 | No large-suite performance baseline yet. | Add reproducible benchmark before final 1.0 if feasible; otherwise document post-1.0 plan. |

## Decisions required before API freeze

1. Which modules are stable 1.0 artifacts and which are preview/optional.
2. Whether to keep example-data row execution inline for 1.0.
3. Public vs internal status of example-data recorder and generation/model classes.
4. Scope of Phase 50 matchers in 1.0.
5. Scope of event model v2 in 1.0.
6. API compatibility tool and baseline location.
7. JSON schema evolution policy.
8. Release dry-run artifact set and publication workflow responsibilities.
