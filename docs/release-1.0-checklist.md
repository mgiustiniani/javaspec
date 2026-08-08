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
- [x] RC2, RC3, and RC4 stabilization fixes published on the Git Flow release line; Maven Central
  resolves the immutable `1.0.0-RC4` artifacts.
- [x] RC5 aligned version cut and clean local qualification completed at `8f93a46`, including API
  baseline regeneration, reproducible artifacts, security, external consumers, and Tasks downstream
  conformance.
- [x] RC5 release-branch CI qualification completed at `4d72aca`: Java 8/11/17/21/25 and full
  Java 21 verification passed with zero annotations.
- [x] RC5 Git Flow merges completed; production commit `ae9291f` is tagged `v1.0.0-RC5`, all five
  signed Maven modules are published, and immutable Maven/CLI/JLC-8 replay passed.
- [ ] RC5 Gradle Plugin Portal first-publication approval and marker availability.
- [x] Java 8/11/17/21/25 language-coverage closure completed according to
  `docs/java-language-coverage-roadmap.md`, including remote-RC Java 21 dogfooding.
- [x] Post-publication Javadoc reproducibility correction completed on develop at `67db10c`; two
  independent clean worktrees reproduce all 18 archives.
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
| `git diff --check` | `8f93a46` | PASS | Clean aligned RC5 checkout |
| `scripts/check-version-alignment.sh` | `8f93a46` | PASS | Every Maven, Gradle, launcher, consumer, and current-document version is `1.0.0-RC5` |
| `scripts/check-current-docs.sh` | `8f93a46` | PASS | Contract docs and multilingual man-page guard green |
| `scripts/check-man-pages.sh` | `8f93a46` | PASS | English, Italian, Spanish, German, French, and Simplified Chinese pages render |
| `scripts/check-api-surface.sh` | `8f93a46` | PASS | API/SPI classification green; RC1-to-RC5 inventory has 172 additions and zero declaration removals |
| `mvn -q verify` | `8f93a46` | PASS | Core 884/884 on Java 25, including owner-return and incident-0007 regressions |
| strict Java-language manifest | `8f93a46` | PASS | 50 covered rows, zero planned rows |
| `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ... mvn -q verify` | `8f93a46` | PASS | Core 884/884 on Java 21 |
| `scripts/verify-all.sh` | `8f93a46` | PASS | Core, adapters, Gradle, standalone consumers, and examples |
| `scripts/verify-release-dry-run.sh` | `8f93a46` | PASS | All 18 RC5 main/source/Javadoc archives reproduced identical SHA-256 values; external consumers passed |
| `scripts/check-core-java8-bytecode.sh` | `8f93a46` | PASS | 323 core classfiles; max major 52 |
| `mvn clean verify -Psecurity` | `8f93a46` | PASS | OWASP Dependency-Check reported zero vulnerabilities; OSS Index remained credential-disabled |
| `JAVASPEC_RELEASE_TAG=v1.0.0-RC5 scripts/check-release-preflight.sh` | `8f93a46` | PASS | Version/tag aligned and no build-file `SNAPSHOT` references |
| Tasks source-backed downstream gate | `8f93a46` | PASS | QG-001, core 884/884, plugin 33/33, TaskSpec 9/9, direct probe 4/4, Cucumber 1/1, full reactor 66/65/1 |
| Local qualification archive | `8f93a46` | PASS | `.ide/agent-runs/javaspec-1.0.0-rc5-20260808/local-qualification/`; `SHA256SUMS` SHA-256 `bec6e44ca1c6ae77abe291b58f08263c8326c651f67d4d3d512706cb505f0566` |
| RC5 Java 8/11/17/21/25 and full Java 21 CI | `4d72aca` | PASS | [CI run 31261952121](https://github.com/mgiustiniani/javaspec/actions/runs/31261952121); six jobs passed with zero annotations |
| RC5 production/tag CI | `ae9291f` | PASS | [CI run 31262851868](https://github.com/mgiustiniani/javaspec/actions/runs/31262851868); six jobs passed on `v1.0.0-RC5` |
| RC5 release workflow | `ae9291f` | PASS | [Release run 31262851841](https://github.com/mgiustiniani/javaspec/actions/runs/31262851841); guards, dry-run, GPG, five Maven deployments, and Gradle submission passed |
| RC5 Maven Central availability/signatures | `ae9291f` | PASS | Five POM/main/source/Javadoc sets available; all 20 detached signatures validate with key `92EBAB37E11720596CD690CF212398D74CE93120` |
| Clean Maven Central consumer replay | `ae9291f` | PASS | Maven plugin, Prophecy, JUnit Platform, bytecode-doubles, and bytecode-agent consumers passed 5/5 from an empty repository |
| `magrathea-pki` remote JLC-8 replay | `7c6c41e` | PASS | CLI 4/4 twice, second generation `NO_CHANGES`; Java 21 Maven domain 4/4, zero pending, no source mutation |
| Clean publisher-equivalent Maven archive comparison | `ae9291f` | PARTIAL | 14/15 archives match Central; only core Javadoc `index-all.html` has nondeterministic implicit link labels; immutable RC5 is unchanged |
| Post-publication clean reproducibility fix | `67db10c` | PASS | Explicit Javadoc labels and clean-output rebuilds reproduce 18/18 archives across independent worktrees; Java 21 core 884/884 and API baseline unchanged |
| Reproducibility-fix evidence archive | `67db10c` | PASS | `.ide/agent-runs/javaspec-1.0.0-rc5-20260808/post-publication-repro-fix/`; `SHA256SUMS` SHA-256 `e24742aeeb220bf259ec0787a331ac851c4ec10b22909bf33570182fde96f2cb` |
| Remote publication evidence archive | `ae9291f` | PASS | `.ide/agent-runs/javaspec-1.0.0-rc5-20260808/remote-publication/`; `SHA256SUMS` SHA-256 `b0974ec38b44ce30040d9aea06729bc093b984cefd2824f8404f10b4a17eab94` |
| Gradle Plugin Portal RC5 submission | `ae9291f` | EXTERNAL PENDING | Release run submitted `io.github.jvmspec` for approval; marker POM remains unavailable |
| Tagged RC1 release workflow and Maven publication | `7bd8ac4` | PASS | [Release run 29146746362](https://github.com/mgiustiniani/javaspec/actions/runs/29146746362); Maven Central deployment succeeded |

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
- [x] RC5 API delta reviewed in `docs/release-1.0-rc5-api-review.md`: additive inventory only, no
  supported `PUBLIC_API`/`PUBLIC_SPI` removal; the committed baseline was regenerated twice from
  aligned RC5 artifacts and matched byte-for-byte.

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
- [x] No `SNAPSHOT` dependencies in the RC5 build (`scripts/check-release-preflight.sh` passed locally and in the tag workflow).
- [x] Source JARs present.
- [x] Javadoc JARs present.
- [x] POM metadata complete: SCM, license, developers, issues.
- [x] Maven plugin descriptor valid.
- [x] Gradle plugin marker publication path documented/tested.
- [x] Bytecode agent manifest has required `Premain-Class` and `Agent-Class`.
- [x] Checksums generated/verified by the release dry-run; a second clean build in the same
  environment reproduces every Maven and Gradle main/source/Javadoc archive hash.
- [x] Cross-worktree clean replay reproduces all 18 next-candidate archives at `67db10c`. Immutable
  RC5 remains 14/15 against its local replay; the corrected source and clean-build guard must be
  included in the next stable candidate.
- [x] Signing configured/documented; workflow requires an imported secret key, verifies the
  passphrase with an isolated loopback signing probe, and passes it explicitly to Maven GPG Plugin.
- [x] Gradle Plugin Portal API key/secret confirmed by successful RC5 `publishPlugins` execution.
- [ ] Public `io.github.jvmspec:1.0.0-RC5` marker visibility after first-publication review.
- [x] Release workflow requires Maven/GPG secrets, detects optional Gradle credentials, and safely skips an already published Maven version on rerun.
- [x] CI and publication workflows use Node 24-compatible `checkout`, `setup-java`, and Gradle setup
  actions; the final RC5 branch run completed with zero deprecation annotations.
- [x] Release workflow publishes or stages every declared artifact or fails clearly (Maven Central artifacts include core, Maven plugin, JUnit Platform engine, bytecode doubles, and bytecode agent; Gradle Plugin Portal publication uses `publishPlugins`).
- [x] Release dry-run script green locally and on the GitHub release runner.
- [x] Git Flow policy documented: release branches start from `develop`, merge with `--no-ff` into
  `main`, receive the annotated tag on the `main` merge commit, and merge back into `develop`.
- [x] Tag/version/workflow alignment verified on RC5 production commit `ae9291f` by release workflow
  run 31262851841.
- [x] RC5 Maven Central deployment succeeded for all five artifacts; direct checks verified POM,
  main/source/Javadoc JARs, and all 20 `.asc` signatures.
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

RC2 through RC5 use distinct RC versions for documented P0 compatibility and release-hardening
changes discovered by real consumers. For the current RC5 stabilization boundary:

- [x] No unreviewed feature addition remains; every additive change is classified in the API review,
  changelog, capability matrix, or generation/report contract.
- [x] Changes after the aligned RC5 cut are limited to reproducible-build, CI/publication hardening,
  and evidence.
- [x] The API inventory has no declaration removal or incompatible supported-surface change; RC5 is a
  new immutable version boundary.
- [x] Consumer smokes were rerun from clean staged artifacts, including the Tasks downstream gate.

## Final release decision

The release is ready only when all P0 gates above are green, every supported final Java construct has
a tested generated/updated/preserved/profile-gated/intentionally-unsupported disposition, P1
deferrals are explicitly documented, and no current documentation contradicts the 1.0 contract.
