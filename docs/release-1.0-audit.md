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

## Capabilities partial or not frozen for 1.0

- API/SPI/public/internal classification.
- Uniform generation plan/result semantics and atomic write audit.
- Release workflow coverage for every actual artifact and Gradle plugin marker.
- JUnit Platform selector/source/row contract audit.
- External consumer verification from staged artifacts.
- Phase 50 matcher parity scope.
- Event/extension model v2 scope.
- JSON schema evolution and compatibility policy.
- Example-data row execution semantics as a documented 1.0 contract.
- Prophecy generated wrapper token overload edge-case audit.

## Public contracts observed

Public-facing contracts exist in these areas and must be classified before API freeze:

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
- `RELEASING.md` is incomplete for the current artifact set and Gradle Plugin Portal concerns.
- API/SPI classification, generation safety guide, release dry-run evidence, and migration guide are missing or incomplete.

## Release gaps

- Version line has been normalized to `1.0.0-SNAPSHOT` after the initial audit.
- No release dry-run script verifies all artifacts as external consumers from a temporary repository.
- Release workflow must be audited for all actual modules.
- Gradle plugin marker publication path is not documented as a reproducible gate.
- API compatibility baseline/tooling has not been introduced.
- No formal 1.0 RC process has been encoded in docs/scripts.

## Risk classification

### P0

- P0-REL-001: version/release-note inconsistency blocks a credible 1.0 line.
- P0-DOC-001: active docs can contradict real capabilities and package names.
- P0-API-001: API/SPI not classified before 1.0 freeze.
- P0-GEN-001: generation lacks a uniform fail-closed structured plan/result contract across every mutating path.
- P0-REL-002: release workflow/dry-run does not yet prove all artifacts publish and work as external consumers.

### P1

- P1-ROW-001: example-data row selection semantics need a 1.0 contract.
- P1-PROP-001: generated Prophecy token overload edge cases need audit.
- P1-MATCH-001: Phase 50 matcher parity requires implementation or explicit deferral.
- P1-EXT-001: event/extension v2 scope must be decided before API freeze.
- P1-JUNIT-001: JUnit Platform IDE/source/selector parity needs contract audit.

### P2

- P2-PERF-001: large-suite performance baseline.
- P2-REPORT-001: optional TAP/TeamCity/HTML/Open Test Reporting adapters.
- P2-DOC-001: extended adoption case studies beyond minimal 1.0 evidence.

## Disposition of roadmap items 46-54

| Area | 1.0 disposition | Notes |
|---|---|---|
| Phase 46 PHPSpec compatibility | REQUIRED_FOR_1_0 | Implemented; keep docs current. |
| Phase 47 example data | REQUIRED_BEFORE_RC | Implemented; contract/documentation audit remains. |
| Phase 48 collaborator injection | REQUIRED_BEFORE_RC | Implemented; contract docs and edge cases still audited in M5. |
| Phase 49 Prophecy parity | REQUIRED_BEFORE_RC | Implemented; generated wrapper overload audit remains. |
| Phase 50 matcher parity | REQUIRED_BEFORE_RC scope decision | Implement or defer explicit parts. |
| Phase 51 event model v2 | DEFERRED_WITH_DOCUMENTED_LIMIT unless API audit upgrades it | Existing SPI classification still required. |
| Phase 52 JUnit Platform v2 | REQUIRED_BEFORE_RC for contract/selector/source audit | Full IDE parity improvements may be split. |
| Phase 53 generation/snippets parity | REQUIRED_BEFORE_RC for safe generation P0; P2 for broader snippets | Generation safety is P0. |
| Phase 54 reports/adoption | REQUIRED_BEFORE_RC for release evidence; P2 for optional reporters | Consumer smoke/release dry-run are P0. |

## Decisions required before API freeze

1. Stable vs preview status for each optional artifact.
2. Public/internal status of generation/model classes and example-data recorder.
3. Row selector semantics: inline descriptor/event filtering vs isolated row execution.
4. Phase 50 matcher scope for 1.0.
5. Event model v2 inclusion vs deferral.
6. API compatibility tooling and baseline storage.
7. JSON schema evolution strategy.
8. Release dry-run artifact matrix and external consumer coverage.

## Next P0 selected

P0-REL-001 and P0-DOC-001 were selected first because they block every subsequent 1.0 evidence trail. This slice created a single active roadmap, archived the historical plan, normalized the development version line, and added an automated current-doc check. Next P0: API/SPI classification and compatibility baseline.
