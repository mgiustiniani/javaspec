# javaspec 1.0 release checklist

This checklist is the release gate source for 1.0. It must be updated with command evidence, commit references, and CI links as the release candidate progresses.

## Phase progression

- [x] Development line normalized to `1.0.0-SNAPSHOT`.
- [x] API/SPI classification complete and RC1 binary signature inventory archived.
- [x] `1.0.0-RC1` prepared from an aligned, locally verified version-cut commit.
- [x] RC consumer verification complete against locally staged `1.0.0-RC1` artifacts.
- [x] Git Flow production history normalized: `main` is an ancestor of `develop`, and the former
  unrelated remote `main` commit is archived.
- [x] `release/1.0.0-RC1` created from the verified `develop` release line.
- [x] RC1 release branch merged with `--no-ff` into `main`, tagged on commit `7bd8ac4`, and merged
  back into `develop`.
- [ ] Java 8/11/17/21/25 language-coverage closure completed according to
  `docs/java-language-coverage-roadmap.md`, including remote-RC Java 21 dogfooding.
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
| `git diff --check` | `7256969` | PASS | See `docs/release-1.0-rc-evidence.md` |
| `scripts/check-version-alignment.sh` | `7256969` | PASS | `1.0.0-SNAPSHOT` aligned |
| `scripts/check-current-docs.sh` | `7256969` | PASS | Contract docs present and current-doc guard green |
| `scripts/check-api-surface.sh` | `7256969` | PASS | API/SPI classification guard green |
| `mvn -q verify` | local RC hardening slice | PASS | Core on default Java 25 JVM; includes Java 8 API linkage check |
| `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ... mvn -q verify` | `7256969` | PASS | Core on local Java 21 runtime |
| `scripts/verify-all.sh` | `e212a39` | PASS | Aligned RC1 adapters and examples |
| `scripts/verify-release-dry-run.sh` | `e212a39` | PASS | RC1 artifacts, checksums, and consumer examples |
| `scripts/check-core-java8-bytecode.sh` | `e212a39` | PASS | Core classfiles max major 52 |
| `scripts/check-release-preflight.sh` | `e212a39` | PASS | `JAVASPEC_RELEASE_TAG=v1.0.0-RC1`; actual tag publication remains pending |
| GitHub Actions Java 8/11/17/21/25 + full Java 21 | `8b87d99` | PASS | [CI run 29139032096](https://github.com/mgiustiniani/javaspec/actions/runs/29139032096) |
| Tagged RC1 release workflow and Maven publication | `7bd8ac4` | PASS | [Release run 29146746362](https://github.com/mgiustiniani/javaspec/actions/runs/29146746362); Maven Central deployment succeeded |
| Corrected Gradle Plugin Portal submission | `e797ca0` | PASS | [Gradle publish run 29148854181](https://github.com/mgiustiniani/javaspec/actions/runs/29148854181); awaiting first-publication approval |

## Core gates

- [x] Java 8 runtime compatibility verified ([CI run 29115347259](https://github.com/mgiustiniani/javaspec/actions/runs/29115347259)).
- [x] Java 11 verified ([CI run 29115347259](https://github.com/mgiustiniani/javaspec/actions/runs/29115347259)).
- [x] Java 17 verified ([CI run 29115347259](https://github.com/mgiustiniani/javaspec/actions/runs/29115347259)).
- [x] Java 21 verified locally and in CI.
- [x] Java 25 verified locally and in CI.
- [x] Root runtime dependency tree has no third-party runtime dependencies.
- [x] No direct linkage to post-Java-8 APIs in core (`mvn -q verify` runs Animal Sniffer against Java 8 API signature; `com.sun.source.*` javac tree API is explicitly allowed for JDK 8 `tools.jar` compatibility).
- [x] Core classfiles are Java 8 bytecode-compatible (max major 52 checked by `scripts/check-core-java8-bytecode.sh`; Java 8 runtime matrix job passed).
- [x] CLI contract tests green.
- [x] JSON schema/golden fixtures green.
- [x] JUnit XML golden fixtures green.

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
- [x] RC1 public/protected JVM signature inventory archived in `docs/history/api-baseline-1.0.0.md`.
- [x] RC5 pre-cut API delta reviewed in `docs/release-1.0-rc5-api-review.md`: additive inventory only,
  no supported `PUBLIC_API`/`PUBLIC_SPI` removal; committed baseline regeneration remains part of the
  aligned RC5 version cut.

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
- [x] No `SNAPSHOT` dependencies in the RC1 build (`scripts/check-release-preflight.sh` passed locally).
- [x] Source JARs present.
- [x] Javadoc JARs present.
- [x] POM metadata complete: SCM, license, developers, issues.
- [x] Maven plugin descriptor valid.
- [x] Gradle plugin marker publication path documented/tested.
- [x] Bytecode agent manifest has required `Premain-Class` and `Agent-Class`.
- [x] Checksums generated/verified locally by release dry-run; publication workflow checksums still recorded at RC/final publication time.
- [x] Signing configured/documented; workflow requires an imported secret key, verifies the
  passphrase with an isolated loopback signing probe, and passes it explicitly to Maven GPG Plugin.
- [x] Gradle Plugin Portal API key/secret confirmed by successful `publishPlugins` execution; public
  RC1 marker visibility remains pending first-publication review.
- [x] Release workflow requires Maven/GPG secrets, detects optional Gradle credentials, and safely skips an already published Maven version on rerun.
- [x] Release workflow publishes or stages every declared artifact or fails clearly (Maven Central artifacts include core, Maven plugin, JUnit Platform engine, bytecode doubles, and bytecode agent; Gradle Plugin Portal publication uses `publishPlugins`).
- [x] Release dry-run script green locally and on the GitHub release runner.
- [x] Git Flow policy documented: release branches start from `develop`, merge with `--no-ff` into
  `main`, receive the annotated tag on the `main` merge commit, and merge back into `develop`.
- [x] Tag/version/workflow alignment verified on RC1 production commit `7bd8ac4` by release workflow
  run 29146746362.
- [x] Maven Central deployment succeeded for all five artifacts; direct checks verified POM,
  main/source/Javadoc JARs, and `.asc` signatures.
- [x] Post-release checklist documented.

## Documentation gates

- [x] README matches 1.0 capabilities and version.
- [x] User manual matches README and capabilities.
- [x] CLI reference current.
- [x] Configuration reference current.
- [x] Matcher reference current.
- [x] Prophecy/doubles reference current.
- [x] Generation safety guide present.
- [x] Maven guide current.
- [x] Gradle guide current.
- [x] JUnit Platform/IDE guide current.
- [x] Extension SPI guide current.
- [x] Example data contract current.
- [x] Java compatibility matrix current.
- [x] Migration guide from 0.1.0/snapshots present.
- [x] JUnit-to-javaspec guide present.
- [x] Cucumber boundary guide present.
- [x] Troubleshooting current.
- [x] Release notes 1.0.0 include RC1 Git Flow, workflow, Maven publication, and signing evidence;
  final 1.0.0 evidence will be appended at final cut time.
- [x] Compatibility policy complete.
- [x] No current pre-migration package references outside archived/historical docs.
- [x] No contradictory current version references.

## Release candidate rules

After RC1:

- [ ] No new features.
- [ ] Only bug fixes, compatibility fixes, release hardening, and documentation.
- [ ] Every breaking change has explicit motivation and triggers a new RC.
- [ ] Consumer smoke tests rerun from clean staged artifacts.

## Final release decision

The release is ready only when all P0 gates above are green, every supported final Java construct has
a tested generated/updated/preserved/profile-gated/intentionally-unsupported disposition, P1
deferrals are explicitly documented, and no current documentation contradicts the 1.0 contract.
