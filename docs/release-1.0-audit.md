# javaspec 1.0 audit

## Audit identity

- Audit date: 2026-07-10
- Branch: `develop`
- Assignment baseline: `e5527b634154cc3156d8e81e6697fab60acaecc3`
- Audited HEAD: `a71297f1bb234b5faa70eca22abe3a5a3b3d6675`
- Baseline ancestry: baseline is an ancestor of audited HEAD.
- Working tree at audit start: clean.

## Commands executed

| Command | Result | Notes |
|---|---|---|
| `git rev-parse HEAD` | PASS | `a71297f1bb234b5faa70eca22abe3a5a3b3d6675` |
| `git merge-base --is-ancestor e5527b634154cc3156d8e81e6697fab60acaecc3 HEAD` | PASS | exit code 0 |
| `git status --short --branch` | PASS | `## develop...origin/develop` |
| `git diff --check` | PASS | no whitespace errors |
| `scripts/check-version-alignment.sh` | PASS | all checked modules/examples aligned at the previous pre-1.0 snapshot before release-line normalization; later normalized to `1.0.0-SNAPSHOT` |
| `mvn -q verify` | PASS | root core only |
| `scripts/verify-all.sh` | PASS | includes core, Maven plugin, JUnit Platform engine, bytecode adapters, Gradle plugin, and examples |

## Repository modules and artifacts

- Root core Maven project: `io.github.jvmspec:javaspec`.
- Maven plugin: `javaspec-maven-plugin`, coordinates `io.github.jvmspec:javaspec-maven-plugin`.
- JUnit Platform engine: `javaspec-junit-platform-engine`, coordinates `io.github.jvmspec:javaspec-junit-platform-engine`.
- Gradle plugin: `javaspec-gradle-plugin`, plugin id `io.github.jvmspec`.
- Bytecode doubles adapter: `javaspec-bytecode-doubles`, coordinates `io.github.jvmspec:javaspec-bytecode-doubles`.
- Bytecode agent adapter: `javaspec-bytecode-agent`, coordinates `io.github.jvmspec:javaspec-bytecode-agent`.
- Standalone examples: Maven, Gradle, JUnit Platform, bytecode-doubles, bytecode-agent, Prophecy.

## Capabilities really implemented at audited HEAD

See [`docs/CAPABILITIES.md`](CAPABILITIES.md) for the capability matrix. The important release-significant implemented areas are:

- PHPSpec-style authoring with `it_*` / `its_*`, `let`, `letGo`, subject lifecycle, construction helpers, skip/pending, and matchers.
- Zero-runtime-dependency core with Java 8 source/target.
- CLI run/generate/dry-run/compile/report/profile/config/classpath flows.
- JSON and JUnit XML reports with schema version 1 and row data support.
- Maven, Gradle, JUnit Platform, bytecode doubles, bytecode agent optional adapters.
- Example data row reporting across console/JSON/JUnit XML/JUnit Platform.
- Prophecy-style doubles, generated wrappers, argument tokens, custom tokens, predictions, custom prediction callbacks, and automatic prediction checks.
- Collaborator injection for supported lifecycle/example parameters.
- Generation/update support for classes, interfaces, records, sealed types, annotations, constructors, methods, support classes, and Prophecy wrappers.
- Record hardening including existing record evolution and explicit prefix construction compatibility.

## Capabilities frozen or explicitly bounded for 1.0

- API/SPI/public/internal classification is recorded in `docs/api-surface-1.0.md` and checked by `scripts/check-api-surface.sh`.
- Generation contract, pending generated-stub semantics, and atomic write guarantees are recorded in `docs/generation-contract-1.0.md`.
- Release dry-run coverage verifies every declared Maven/Gradle artifact and the external consumer examples through `scripts/verify-release-dry-run.sh`.
- JUnit Platform selector/source/row contract and adapter status mappings are recorded in `docs/result-contract-1.0.md` and `docs/release-1.0-acceptance-tests.md`.
- Phase 50 matcher scope is frozen for 1.0 as approximate numeric, iterator, and generated object-state Java-adapted semantics.
- Event/extension model v2 is deferred; existing extension surfaces are classified for 1.0.
- JSON schema evolution and compatibility policy are recorded in `docs/result-contract-1.0.md`.
- Example-data row execution semantics are frozen in `docs/example-data-contract-1.0.md` as inline execution with row descriptor/event/report projection.
- Prophecy generated wrappers, typed token overloads, and custom predictions are frozen in `docs/prophecy-contract-1.0.md`.

## Public contracts observed

Public-facing contracts are classified for 1.0 in `docs/api-surface-1.0.md`:

- Core authoring API: `io.github.jvmspec.api`.
- Doubles and Prophecy API: `io.github.jvmspec.doubles`, `io.github.jvmspec.doubles.prophecy`.
- Runner/result model: `io.github.jvmspec.runner`.
- Invocation/launcher: `io.github.jvmspec.invocation`.
- Formatter and reporting APIs: `io.github.jvmspec.formatter`, `io.github.jvmspec.reporting`.
- Extension, bootstrap, parser, dependency resolver SPIs.
- Generation/model packages, many of which may need `INTERNAL` or `PUBLIC_SPI` classification.
- CLI options, config keys, exit codes, JSON schema, JUnit XML shape.
- JUnit Platform engine id and unique-id segments.
- Maven plugin parameters and Gradle plugin extension/task properties.

## Documentation gaps

- Active roadmap was too large and mixed historical implementation notes with future work.
- `docs/test-report.md` is a historical accumulator and should not be the current source of truth.
- Historical docs retain previous `0.1.0` release references; current docs are now guarded by an automated check.
- Some non-archived docs still mention historical pre-migration package names.
- Release notes file is now named `docs/release-notes-1.0.0.md`; it still requires final RC evidence.
- `RELEASING.md` covers the current artifact set, Gradle Plugin Portal path, RC flow, signing, and post-release steps.
- API/SPI classification, generation safety guide, result contract, Prophecy contract, and release dry-run evidence are present; migration/troubleshooting polish remains documentation work.

## Release gaps

- Version line has been normalized to `1.0.0-SNAPSHOT` after the initial audit.
- `scripts/verify-release-dry-run.sh` verifies all declared artifacts and consumer examples.
- Release workflow responsibilities are documented for the actual Maven modules and Gradle plugin marker.
- Gradle plugin marker publication path is documented as a release gate in `RELEASING.md`.
- API classification tooling is introduced via `scripts/check-api-surface.sh`.
- Formal 1.0 RC process is encoded in `RELEASING.md`; no-SNAPSHOT dependency and tag alignment checks happen when RC/final versions are cut.

## Risk classification

### P0

- P0-REL-001: DONE — version/release-note line normalized for 1.0.
- P0-DOC-001: DONE — current docs/version/package checks are automated.
- P0-API-001: DONE — API/SPI classification is documented and checked.
- P0-GEN-001: DONE — generation safety, atomic writes, and pending-stub fail-closed behavior are documented/tested.
- P0-REL-002: DONE — release dry-run verifies all artifacts and consumer examples.

### P1

- P1-ROW-001: DONE — example-data row execution/selector/report semantics are frozen in `docs/example-data-contract-1.0.md`.
- P1-PROP-001: generated Prophecy token overload edge cases need audit.
- P1-MATCH-001: Phase 50 matcher parity requires implementation or explicit deferral.
- P1-EXT-001: DONE — existing SPI semantics are frozen in `docs/extension-spi-1.0.md`; typed event model v2 is deferred.
- P1-JUNIT-001: JUnit Platform IDE/source/selector parity needs contract audit.

### P2

- P2-PERF-001: large-suite performance baseline.
- P2-REPORT-001: optional TAP/TeamCity/HTML/Open Test Reporting adapters.
- P2-DOC-001: extended adoption case studies beyond minimal 1.0 evidence.

## Disposition of roadmap items 46-54

| Area | 1.0 disposition | Notes |
|---|---|---|
| Phase 46 PHPSpec compatibility | REQUIRED_FOR_1_0 | Implemented; keep docs current. |
| Phase 47 example data | REQUIRED_BEFORE_RC | Implemented; 1.0 contract documented in `docs/example-data-contract-1.0.md`. |
| Phase 48 collaborator injection | REQUIRED_BEFORE_RC | Implemented; contract docs and edge cases still audited in M5. |
| Phase 49 Prophecy parity | REQUIRED_BEFORE_RC | Implemented; generated wrapper overload audit remains. |
| Phase 50 matcher parity | REQUIRED_BEFORE_RC scope decision | Implement or defer explicit parts. |
| Phase 51 event model v2 | DEFERRED_WITH_DOCUMENTED_LIMIT unless API audit upgrades it | Existing SPI classification still required. |
| Phase 52 JUnit Platform v2 | REQUIRED_BEFORE_RC for contract/selector/source audit | Full IDE parity improvements may be split. |
| Phase 53 generation/snippets parity | REQUIRED_BEFORE_RC for safe generation P0; P2 for broader snippets | Generation safety is P0. |
| Phase 54 reports/adoption | REQUIRED_BEFORE_RC for release evidence; P2 for optional reporters | Consumer smoke/release dry-run are P0. |

## Decisions required before API freeze

The P0 API-freeze decisions have been made and recorded in the contract documents: optional artifact status, generation/model/internal classification, example-data row execution, Phase 50 matcher scope, event model v2 deferral, API classification tooling, JSON schema evolution, and release dry-run artifact coverage.

## P0 status

The P0 release-readiness slices are complete for RC preparation. Remaining release-candidate checks are evidence gates tied to the actual RC/final version cut: no-SNAPSHOT dependency checks, tag/version/workflow alignment, checksums, and publication signing execution.
