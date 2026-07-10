# javaspec 1.0 release checklist

This checklist is the release gate source for 1.0. It must be updated with command evidence, commit references, and CI links as the release candidate progresses.

## Phase progression

- [x] Development line normalized to `1.0.0-SNAPSHOT`.
- [x] API/SPI classification complete; binary baseline archive remains RC task.
- [ ] `1.0.0-RC1` prepared from a clean commit.
- [ ] RC consumer verification complete.
- [ ] Final `1.0.0` prepared from verified RC or documented RC fix commit.
- [ ] Post-release snapshot bump complete.

## Mandatory local gates

Run from a clean checkout:

```sh
git diff --check
scripts/check-version-alignment.sh
scripts/check-current-docs.sh
mvn -q verify
scripts/verify-all.sh
```

Evidence:

| Gate | Last verified commit | Result | Notes |
|---|---|---|---|
| `git diff --check` | `a71297f1bb234b5faa70eca22abe3a5a3b3d6675` | PASS | Audit baseline |
| `scripts/check-version-alignment.sh` | working 1.0 audit slice | PASS | `1.0.0-SNAPSHOT` aligned after normalization |
| `scripts/check-current-docs.sh` | working 1.0 audit slice | PASS | Added in first 1.0 audit slice |
| `mvn -q verify` | `a71297f1bb234b5faa70eca22abe3a5a3b3d6675` | PASS | Core only |
| `scripts/verify-all.sh` | `a71297f1bb234b5faa70eca22abe3a5a3b3d6675` | PASS | Includes Gradle |

## Core gates

- [ ] Java 8 runtime compatibility verified.
- [ ] Java 11 verified.
- [ ] Java 17 verified.
- [ ] Java 21 verified.
- [ ] Java 25 verified.
- [ ] Root runtime dependency tree has no third-party runtime dependencies.
- [ ] No direct linkage to post-Java-8 APIs in core.
- [ ] CLI contract tests green.
- [ ] JSON schema/golden fixtures green.
- [ ] JUnit XML golden fixtures green.

## API/SPI gates

- [x] Public API classified.
- [x] Public SPI classified.
- [x] Internal implementation packages documented.
- [x] Pre-1.0 deprecations documented or removed/not identified.
- [x] SemVer policy documented.
- [x] Deprecation policy documented.
- [x] Support window documented.
- [x] API compatibility tool added as test/build tooling.
- [x] 1.0 API baseline generation procedure documented.

## Semantic gates

- [x] PHPSpec compatibility matrix has no `UNSPECIFIED` entries.
- [x] All `PARTIAL_BLOCKING_1_0` entries have been implemented or intentionally reclassified with tests/docs.
- [x] Seven mandatory PHPSpec semantic acceptance scenarios from `docs/phpspec-compatibility-matrix.md` are mapped to automated tests (`docs/release-1.0-acceptance-tests.md`).
- [x] PASSED/FAILED/BROKEN/SKIPPED/PENDING contract documented (`docs/result-contract-1.0.md`).
- [x] State mapping verified across CLI, launcher, JSON, JUnit XML, JUnit Platform, Maven, Gradle.
- [x] Pending generation cannot be reported as final GREEN (`// javaspec:stub` creates a synthetic BROKEN result in compiled runs).
- [x] Lifecycle and prediction ordering documented/tested.
- [x] Example-data row semantics documented/tested.
- [x] Collaborator injection contract documented/tested.
- [x] Prophecy contract documented/tested.
- [x] Matcher 1.0 scope implemented or explicitly deferred.

## Generation gates

- [x] Every mutating path has structured plan/result semantics documented in `docs/generation-contract-1.0.md`.
- [x] Dry-run parity for every mutating path included in 1.0.
- [x] Atomic writes audited/implemented where possible.
- [x] No silent refusal path.
- [x] Record updates use semantic record components for existing records.
- [x] Constructor and type identity edge cases covered.
- [x] Adversarial parser fixtures covered.
- [x] `run --generate --compile` regressions green for records, sealed types, interfaces, annotations, collaborators.

## Adapter gates

- [x] Maven plugin verification green.
- [x] Gradle plugin verification green.
- [x] JUnit Platform engine verification green.
- [x] Bytecode doubles verification green.
- [x] Bytecode agent verification green.
- [x] External Maven consumer smoke green from staged artifacts/local release dry-run.
- [x] External Gradle consumer smoke green from staged artifacts/local release dry-run.
- [x] External JUnit Platform consumer smoke green from staged artifacts/local release dry-run.
- [x] Bytecode adapter consumer smokes green from staged artifacts/local release dry-run.

## Release engineering gates

- [x] Version alignment green.
- [ ] No `SNAPSHOT` dependencies in release build (final-release-only check; current line is `1.0.0-SNAPSHOT`).
- [x] Source JARs present.
- [x] Javadoc JARs present.
- [x] POM metadata complete: SCM, license, developers, issues.
- [x] Maven plugin descriptor valid.
- [x] Gradle plugin marker publication path documented/tested.
- [x] Bytecode agent manifest has required `Premain-Class` and `Agent-Class`.
- [ ] Checksums generated/verified by publication workflow.
- [x] Signing configured/documented.
- [x] Release workflow publishes or stages every declared artifact or fails clearly.
- [x] Release dry-run script green.
- [ ] Tag/version/workflow alignment verified at RC/final tag time.
- [x] Post-release checklist documented.

## Documentation gates

- [ ] README matches 1.0 capabilities and version.
- [ ] User manual matches README and capabilities.
- [ ] CLI reference current.
- [ ] Configuration reference current.
- [ ] Matcher reference current.
- [ ] Prophecy/doubles reference current.
- [ ] Generation safety guide present.
- [ ] Maven guide current.
- [ ] Gradle guide current.
- [ ] JUnit Platform/IDE guide current.
- [x] Extension SPI guide current.
- [ ] Java compatibility matrix current.
- [ ] Migration guide from 0.1.0/snapshots present.
- [ ] JUnit-to-javaspec guide present or explicitly deferred.
- [ ] Cucumber boundary guide present or explicitly deferred.
- [ ] Troubleshooting current.
- [ ] Release notes 1.0.0 complete.
- [ ] Compatibility policy complete.
- [ ] No current pre-migration package references outside archived/historical docs.
- [ ] No contradictory current version references.

## Release candidate rules

After RC1:

- [ ] No new features.
- [ ] Only bug fixes, compatibility fixes, release hardening, and documentation.
- [ ] Every breaking change has explicit motivation and triggers a new RC.
- [ ] Consumer smoke tests rerun from clean staged artifacts.

## Final release decision

The release is ready only when all P0 gates above are green, P1 deferrals are explicitly documented, and no current documentation contradicts the 1.0 contract.
