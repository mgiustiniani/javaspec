# javaspec capabilities audit snapshot

This document records capabilities verified while preparing the 1.0 roadmap. It is not a marketing page; it distinguishes implemented behavior from release-freeze work still required.

## Snapshot

- Original capability-audit HEAD: `a71297f1bb234b5faa70eca22abe3a5a3b3d6675`.
- Current clean RC5 local-qualification commit: `8f93a46`.
- Baseline supplied for the assignment: `e5527b634154cc3156d8e81e6697fab60acaecc3` (ancestor of both audit points).
- Current local evidence passes `git diff --check`, version/document/API/manual-page guards, Java 21
  and Java 25 core verification (884/884 each), the security profile with zero reported
  vulnerabilities, `scripts/verify-all.sh`, a byte-reproducible `scripts/verify-release-dry-run.sh`,
  strict language-manifest mode with 50 covered rows and zero planned rows, and Tasks downstream
  conformance.
- Remote RC5 branch CI run
  [`31261952121`](https://github.com/mgiustiniani/javaspec/actions/runs/31261952121) passed the Java
  8/11/17/21/25 matrix and full Java 21 verification at `4d72aca` with zero annotations.
- Published production candidate: `v1.0.0-RC5` at `ae9291f`. Release workflow
  [`31262851841`](https://github.com/mgiustiniani/javaspec/actions/runs/31262851841) deployed all five
  signed Maven modules and submitted the Gradle plugin for first-publication approval.
- Five clean Maven consumers and the `magrathea-pki` Java 21 domain replay passed against immutable
  RC5 artifacts; the latter ran CLI generation twice and Maven verification at 4/4 with zero pending
  and no source mutation.
- Post-RC4 hardening is part of immutable RC5. Develop commit `67db10c` resolves its one observed
  cross-environment core Javadoc mismatch and passes 18/18 across independent clean worktrees.
  Gradle Portal approval and inclusion of this fix in the next candidate remain stable-1.0 gates.
- User documentation is maintained in six languages with matching section 1 pages and automated
  token/link guards. The copyable spec-driven agent documents semantic admission, launcher
  provenance, safe generation, typed stops, and structured handoff.
- Post-RC5 `develop` adds a project-specific Native Image preview. Local GraalVM 25 verification
  builds and replays `examples/native-basic/`; CI run
  [`31283024044`](https://github.com/mgiustiniani/javaspec/actions/runs/31283024044) repeated it with
  GraalVM 25.0.4 and zero annotations. Published RC5 does not include the new launcher/goal.

## Artifacts and modules

| Artifact/module | Coordinates / plugin id | Runtime baseline | Runtime dependencies | Stability for 1.0 |
|---|---|---:|---|---|
| Core | `io.github.jvmspec:javaspec` | Java 8 | none in runtime dependency tree | REQUIRED_FOR_1_0; API/SPI frozen |
| Maven plugin | `io.github.jvmspec:javaspec-maven-plugin` | Java 8 | core + Maven APIs in plugin context | REQUIRED_FOR_1_0; verified and included in publication workflow |
| JUnit Platform engine | `io.github.jvmspec:javaspec-junit-platform-engine` | Java 8 | core + JUnit Platform engine APIs | REQUIRED_FOR_1_0 adapter; contract frozen |
| Gradle plugin | plugin id `io.github.jvmspec` | Java 8 | core in plugin runtimeClasspath | REQUIRED_FOR_1_0; verified, publication pending Portal confirmation |
| Bytecode doubles | `io.github.jvmspec:javaspec-bytecode-doubles` | Java 8 | core + ByteBuddy | Optional stable adapter for 1.0 |
| Bytecode agent | `io.github.jvmspec:javaspec-bytecode-agent` | Java 8 | core + ByteBuddy + ByteBuddy Agent | Optional stable adapter for 1.0 |
| Project native executable | Consumer build output (no new JavaSpec coordinate) | GraalVM Native Image 25 build tool; no JVM process at execution | Build-linked core + consumer production/test classes | Post-RC5 experimental preview; Linux x86-64 qualified locally and in CI |

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
- Java source generation/update support for classes, interfaces, records, sealed types, annotations,
  constructors, methods, support classes, and Prophecy wrappers; implicit record constructors and
  accessors retain structured owner-qualified nested component types during support regeneration.
- Record hardening: existing-record kind preservation, record header evolution, support default construction, compact-constructor preservation, explicit record construction prefix padding at runtime.
- Optional Maven, Gradle, JUnit Platform, bytecode doubles, and bytecode agent integrations.
- Project-specific Native Image preparation through `javaspec:native-prepare`: deterministic linked
  launcher/reflection metadata plus pretty/progress/JSON execution for the qualified static subset.

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
- Native Image dynamic capabilities remain slice-qualified: runtime compile/generate/classpath,
  extensions, reports, proxies/Prophecy, ByteBuddy adapters, Gradle/JUnit paths, and wider platforms
  are explicitly deferred in `docs/native-image.md`.

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
- `io.github.jvmspec.nativeimage`: project-linked no-exit launcher and native exit-code subset.
- Maven plugin parameters, Gradle extension/task properties, JUnit Platform engine id and unique IDs.

## Documentation gaps

- Active roadmap was mixed with historical implementation notes before this audit.
- Historical docs still mention previous `0.1.0` releases; current docs are guarded by `scripts/check-current-docs.sh`.
- Historical documents may keep pre-migration package/version references only with migration context; current docs are checked automatically.
- README, user manual, CLI/configuration/matcher/Prophecy/generation/adapter docs, migration, troubleshooting, and compatibility policy have 1.0 contract coverage.
- Release notes use `docs/release-notes-1.0.0.md`; RC5 Maven publication and remote replay evidence
  are complete, while Gradle Portal approval remains external.
- Release engineering docs cover the real artifact set, Git Flow through `main`/`develop` and
  `release/*`, Maven Central signing/deployment, and the Gradle Plugin Portal marker path.

## Risk register

| ID | Priority | Risk | Required action |
|---|---|---|---|
| P0-REL-001 | P0 | Version line and release-note naming needed normalization for 1.0 preparation. | DONE in first 1.0 audit slice: normalized to `1.0.0-SNAPSHOT`, created `docs/release-notes-1.0.0.md`, and added automated current-doc check. |
| P0-API-001 | P0 | Public API/SPI classification was missing. | DONE: `docs/api-surface-1.0.md` classifies packages/contracts and `scripts/check-api-surface.sh` gates unclassified packages. |
| P0-GEN-001 | P0 | Generation needed frozen structured outcomes and atomic-write audit. | DONE: `docs/generation-contract-1.0.md`, `AtomicFileWriter`, and pending-stub synthetic BROKEN result. |
| P0-REL-002 | P0 | Release workflow may not publish/verify every real artifact. | DONE: `scripts/verify-release-dry-run.sh`, `RELEASING.md`, and bytecode-agent release metadata/artifacts verify the declared artifact set and consumer examples. |
| P0-REL-003 | P0 | Clean publisher-equivalent replay found nondeterministic implicit link labels in the core Javadoc index. | DONE ON DEVELOP at `67db10c`: explicit labels plus clean-output rebuilds match 18/18 archives across two worktrees; include in the next candidate. |
| P0-DOC-001 | P0 | Documentation can contradict current capabilities/version. | DONE: `scripts/check-current-docs.sh` and `scripts/check-version-alignment.sh` gate current docs/version/package consistency. |
| P1-ROW-001 | P1 | Example-data row selector semantics are subtle and could be misrepresented. | DONE: `docs/example-data-contract-1.0.md` documents inline execution vs descriptor/event filtering, with JUnit Platform regression coverage. |
| P1-PROP-001 | P1 | Generated Prophecy token overloads need edge-case audit. | DONE: `ProphecySkeletonGeneratorTest` covers primitive, array, varargs, bounded generic, bridge/synthetic, duplicate, and mixed exact/token call cases; `docs/prophecy-contract-1.0.md` documents limits. |
| P1-MATCH-001 | P1 | Inline/configured custom matcher scope is not finalized. | DONE: `docs/matcher-contract-1.0.md` freezes programmatic `MatcherRegistry`/`shouldMatch(...)` support and defers config/inline dynamic custom matcher conveniences. |
| P1-EXT-001 | P1 | Event/extension v2 scope could affect API freeze. | DONE: `docs/extension-spi-1.0.md` freezes existing SPI semantics and defers typed event model v2. |
| P1-JUNIT-001 | P1 | JUnit Platform IDE/source/selector parity needed a contract audit. | DONE: `docs/junit-platform-contract-1.0.md` freezes engine id, selector, unique-id, source, row, status, and IDE boundaries with engine regression coverage. |
| P1-NATIVE-001 | P1 | JVM feature parity could be inferred from the first closed-world native executable. | Keep a strict supported/unsupported matrix, fail unknown native options, and admit reports/proxies/platforms only with native evidence. |
| P2-PERF-001 | P2 | No large-suite performance baseline yet. | Add reproducible benchmark before final 1.0 if feasible; otherwise document post-1.0 plan. |

## Decisions required before API freeze

The P0/P1 decisions for API/SPI classification, example-data row execution, PHPSpec matcher scope, custom matcher scope, event model v2 deferral, JSON schema evolution, generation safety, and release dry-run artifact responsibilities are recorded in the 1.0 contract documents. Remaining decisions are P2 polish or RC evidence gates unless release-candidate evidence exposes a contradiction.
