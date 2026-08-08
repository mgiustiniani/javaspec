# Release 1.0 RC evidence

This page records local release-candidate evidence. Final RC/final publication evidence is appended
when `1.0.0-RC1` and `1.0.0` are cut.

## 2026-07-10 — local RC readiness dry-run

Commit verified: `7256969` (`docs: complete release documentation gates`).

Environment:

- Default JVM: Java 25.0.3.
- Additional local JVM checked: `/usr/lib/jvm/java-21-openjdk`.
- Gradle executable available on `PATH`.

Commands passed:

```sh
git diff --check
scripts/check-version-alignment.sh
scripts/check-current-docs.sh
scripts/check-api-surface.sh
mvn -q verify
scripts/check-core-java8-bytecode.sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH mvn -q verify
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

Additional checks:

- Core `mvn -q verify` runs Animal Sniffer against the Java 8 API signature to prevent direct
  linkage to post-Java-8 APIs, with only `com.sun.source.*` ignored for JDK 8 javac tree API use.

```sh
# Core classfiles are Java 8 bytecode-compatible.
find target/classes -name '*.class' -print0 | xargs -0 javap -verbose
# Maximum observed major version: 52
```

Release dry-run evidence:

- Packaged core, Maven plugin, JUnit Platform engine, bytecode doubles, and bytecode agent Maven
  artifacts with main/source/Javadoc jars.
- Packaged Gradle plugin main/source/Javadoc jars.
- Verified bytecode-agent manifest contains `Premain-Class` and `Agent-Class`.
- Generated and verified SHA-256 checksums for local dry-run artifacts in `target/release-dry-run-checksums.sha256`.
- Ran external consumer examples for Maven, Gradle, JUnit Platform, Prophecy, bytecode doubles, and
  bytecode agent from locally installed/staged artifacts.

## 2026-07-10 — CI runtime matrix

GitHub Actions [run 29115347259](https://github.com/mgiustiniani/javaspec/actions/runs/29115347259)
passed at commit `0f89906`:

- Core `mvn -q verify` on Java 8, 11, 17, 21, and 25.
- Root runtime dependency audit in every core matrix job.
- Full adapter and standalone consumer verification on Java 21.
- Release contract guards, including the portable API-surface classification check.

## 2026-07-10 — RC1 version cut

Commit `e212a39` (`chore: prepare 1.0.0-RC1`) aligned the repository at `1.0.0-RC1` and passed locally:

```sh
scripts/check-version-alignment.sh
scripts/check-current-docs.sh
JAVASPEC_RELEASE_TAG=v1.0.0-RC1 scripts/check-release-preflight.sh
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
scripts/generate-api-baseline.sh
```

Evidence:

- No `SNAPSHOT` references remain in Maven/Gradle build files.
- RC1 main/source/Javadoc artifacts, checksums, adapter builds, and consumer examples passed.
- `docs/history/api-baseline-1.0.0.md` contains the deterministic RC1 public/protected JVM signature
  inventory; a second generation produced no diff.
- GitHub Actions [run 29118310277](https://github.com/mgiustiniani/javaspec/actions/runs/29118310277)
  passed the Java 8/11/17/21/25 core matrix and full Java 21 adapter/consumer verification on the RC1
  version-cut commit.

## 2026-07-10 — release workflow hardening and Git Flow correction

Release workflow [run 29139156898](https://github.com/mgiustiniani/javaspec/actions/runs/29139156898)
on commit `8b87d99` established that:

- Required Maven/GPG repository secrets were present.
- Release contract guards, tag/dependency preflight, artifact packaging, checksums, and standalone
  consumer verification passed on the GitHub runner.
- GPG private-key import passed.
- The Maven Central deployment step failed before confirmed publication; direct Maven Central lookup
  still returned HTTP 404 for `javaspec:1.0.0-RC1`.
- Gradle Plugin Portal publication was skipped after the Maven failure.

The failed provisional tag was deleted because no RC1 artifact had been published. Repository branch
history was then normalized to Git Flow: `main` is the production branch and an ancestor of
`develop`; the former unrelated remote `main` commit is preserved as
`archive/origin-main-20260709`; stabilization continues on `release/1.0.0-RC1`. The final RC1 tag
must be created on the `main` merge commit, not directly on `develop`.

Release hardening after the failed attempts added clean-run core installation before standalone
artifact builds, pre-build-safe launcher alignment checks, portable release gates without a
`ripgrep` dependency, split preflight diagnostics, and redacted Maven deployment diagnostics.

After the first Git Flow merge, release workflow
[run 29139720896](https://github.com/mgiustiniani/javaspec/actions/runs/29139720896) again passed all
pre-deployment gates and isolated the core deployment failure to Maven GPG Plugin exit code 2. The
workflow had exposed the repository secret only as `GPG_PASSPHRASE`; Maven GPG Plugin expects
`MAVEN_GPG_PASSPHRASE`. Direct Central lookup remained HTTP 404, so the failed tag was safely removed
and the environment mapping was corrected on the release branch before repeating the release merge.

Release workflow [run 29139966108](https://github.com/mgiustiniani/javaspec/actions/runs/29139966108)
showed that the environment mapping alone was insufficient: Maven GPG Plugin still exited with code
2 while signing core. Central remained HTTP 404, so the tag was again removed safely. The next
hardening step validates that the imported material contains a secret key, performs an isolated
loopback signing probe with the configured passphrase, and passes the verified passphrase explicitly
to Maven GPG Plugin.

## 2026-07-11 — RC1 publication

Annotated tag `v1.0.0-RC1` points to production `main` commit
`7bd8ac4cf675a6faad56393a729d9274dc3308b7`; the release fixes were merged back into `develop`.
Release workflow [run 29146746362](https://github.com/mgiustiniani/javaspec/actions/runs/29146746362)
completed successfully:

- Contract guards, tag/dependency preflight, release dry-run, checksums, and consumer examples passed.
- Imported-key and loopback passphrase signing probes passed.
- Maven Central deployment completed for core, Maven plugin, JUnit Platform engine, bytecode doubles,
  and bytecode agent.
- Direct Maven Central checks returned HTTP 200 for every POM, main JAR, sources JAR, Javadoc JAR,
  and corresponding `.asc` signature across all five artifacts.
- The core JAR signature verified as `GOODSIG`/`VALIDSIG` with fingerprint
  `92EBAB37E11720596CD690CF212398D74CE93120`; the public key is retrievable from
  `keyserver.ubuntu.com`.
- The original Gradle submission was removed to replace an overly strong portal description.
- Gradle-only workflow
  [run 29148854181](https://github.com/mgiustiniani/javaspec/actions/runs/29148854181) verified the
  Maven Central core/tag/version boundary and submitted RC1 with description "Optional Gradle
  adapter for the javaspec runner." The plugin marker remains externally unavailable while
  first-publication approval completes.

Remaining stable-release evidence:

- The Gradle Plugin Portal page and plugin marker become publicly resolvable after external review.
- JLC-8 completes one coherent Java 21 real-project milestone from a clean environment against the
  published RC4 correction and the next approved remote release candidate.

## 2026-07-11 — JLC-8 remote-RC dogfooding blocker and RC2 preparation

A clean `magrathea-pki` Java 21 domain run used an empty Maven cache and resolved
`io.github.jvmspec:javaspec:1.0.0-RC1` directly from Maven Central; Maven Resolver recorded both the
POM and JAR as `central=` artifacts. With `target/generated-sources/javaspec` removed, the RC1 CLI
regenerated support for a constructor-bearing record but skipped a matcher-only enum specification
that still extended its generated `*SpecSupport` superclass. Compilation then failed because the
support class and inherited matchers were absent.

This was classified as a framework-origin clean-generation blocker, not worked around in the
consumer project. Commit `0bfd910` makes support regeneration consider the specification's explicit
generated-support superclass even when discovery infers no subject methods, constructors, or enum
constants. Its regression test starts from an empty generated-output directory, generates the
matcher-only enum support, compiles the subject/support/spec trio, and executes the example.

The strict language manifest remains `pass=50 planned=0 manifest-planned=0`; core verify and
`scripts/verify-all.sh` pass. The release line is advanced to `1.0.0-RC2`. JLC-8 remains open until
RC2 is published, but its Maven `run` goal still executes after `testCompile`; it cannot repair a
clean generated-source directory during the normal lifecycle.

## 2026-07-11 — RC3 source-first Maven generation candidate

RC3 adds the dedicated `javaspec:generate` goal with default phase `generate-test-sources`. The goal
parses specification source before specs compile, refines signatures from production source,
generates every required base typed support class, and registers
`target/generated-sources/javaspec` as a Maven test source root. It does not update production source
inside the Maven lifecycle.

The plugin regression starts with complete record and enriched-enum domain sources, two specs whose
support superclasses are absent, and an empty generated directory. It proves initial compilation
fails on both missing support types, generates both supports, compiles and executes all examples,
finds no `javaspec:stub`, repeats generation byte-idempotently, and confirms production/spec source
hashes are unchanged. `examples/maven-basic` now exercises `generate-test-sources -> testCompile ->
verify/run` from `mvn clean verify` without build-helper or tracked generated support.

Local RC3 CLI replay against `magrathea-pki` generated both support classes from an empty directory,
compiled four source files, passed all four examples with zero pending, emitted no stubs, and was
hash-idempotent. This result established the acceptance protocol that the following published-RC3
remote replay then executed from an isolated Maven repository.

## 2026-07-11 — RC3 remote replay and RC4 boolean-record correction

Published RC3 subsequently passed isolated Maven Central CLI and Maven lifecycle replays against
`magrathea-pki`: both support classes regenerated, all four baseline examples passed, pending was
zero, no stubs remained, and generated support was idempotent. The next domain slice then exposed a
separate generation defect: `beConstructedWith(false)` plus `ca().shouldReturn(false)` produced
invalid `record BasicConstraints(boolean false)`.

RC4 treats constructor expressions only as example values. Literal arguments receive no identifier
status; exact same-example `shouldReturn` accessor evidence in the same specification example maps
each constructor position to its component name. Every planned name must be legal, non-keyword,
unique, stable, and mapped once. Missing or conflicting evidence produces a detailed
`AMBIGUOUS_RECORD_COMPONENT_NAME` refusal before production or support writes. Focused RED/GREEN
coverage includes true/false, multiple booleans, integer/string/enum/negative/null-like literals,
missing evidence, keywords, existing record-prefix preservation, and idempotent skeleton rendering.

## 2026-07-18 — synchronized develop documentation audit

Audit base: `3790931` (`build: isolate sequential Gradle verification`). RC5 release work remains
intentionally paused while additional issues are collected; no RC5 version, tag, or publication is
claimed by this audit.

Local commands passed:

```sh
git diff --check
scripts/check-version-alignment.sh
scripts/check-current-docs.sh
scripts/check-api-surface.sh
mvn -q verify
mvn -q -Djavaspec.language.matrix.strict=true -Dtest=JavaLanguageCoverageManifestTest test
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

Evidence:

- The strict language manifest reported 50 covered rows and zero planned rows.
- Surefire reports contained 967 tests with zero failures, errors, or skips; Gradle plugin tests and
  consumer examples also passed through aggregate verification.
- The RC1-to-current API inventory still contained no removed public/protected declaration.
- Maven Central resolved the published `1.0.0-RC4` core POM, while Gradle Plugin Portal marker POMs
  for RC1 through RC4 remained externally unavailable. Portal approval therefore remains an
  external distribution gate.
- The local checkout retained version alignment at `1.0.0-RC4`, but its post-RC4 implementation is
  development input for RC5 and must not be treated as byte-identical to the immutable published RC4.

## 2026-07-18 — incident-0007 nested record component correction

Magrathea evidence commit `6f8e361` records incident-0007: Maven source-first regeneration and a
fresh JavaSpec compile rejected generated support that cast
`MagnonceAuthorityStateBasename.State` to nonexistent package-level
`com.magrathea.trustengine.kms.encryption.State`. The original acceptance RED remains at `c3c6974`;
runner removal `dbe308d` keeps the consumer reactor green, and `REQ-KMS-ENCRYPTION-005` remains
not implemented. No package-level domain enum or generated-support edit was accepted.

The JavaSpec correction on `develop`:

- derives implicit record canonical constructor and accessor signatures from javac record component
  fields when production source is the truth;
- renders a referenced nested type through its imported owner, for example
  `NestedStateRecord.State`, rather than importing or inventing a package-level `State`;
- uses one import plan for constructor defaults, typed proxies, generated state expectations, and
  throw-helper parameters;
- adds focused RED/GREEN tests for production refinement and nested import rendering;
- adds an isolated fresh-output CLI fixture and a Maven `generate-test-sources` fixture that both
  regenerate, compile, execute, and repeat idempotently;
- passes core verify, the strict 50-row language manifest, aggregate adapter/example verification,
  and the release dry-run; the resulting Surefire reports contain 972 tests with zero failures,
  errors, or skips.

This correction was committed as `114c832` and remains local release-candidate input only. The
blocked consumer slice must remain closed until a new immutable JavaSpec artifact and launcher
fingerprint are available, after which a new sealed RED-to-GREEN trajectory can start from the
approved clean consumer base.

## 2026-08-08 — post-issue pre-RC5 qualification

Qualification base: `34e692b` (`docs(cli): add multilingual manual pages`). The approved post-RC4
stabilization sequence now includes:

- `008d254`, which restores type-evidenced owner-return operation discovery while excluding exact
  top-level specification helpers, unknown argument types, and evidence inside nested types;
- `662f6f7`, which adds direct upstream unit coverage for the owner-return discovery boundary;
- `114c832`, which fixes incident-0007 nested record-component refinement and generated type
  rendering; and
- `34e692b`, which aligns CLI help and adds guarded section 1 manual pages in six languages.

Local commands passed sequentially:

```sh
git diff --check
scripts/check-version-alignment.sh
scripts/check-current-docs.sh
scripts/check-man-pages.sh
scripts/check-api-surface.sh
mvn -q verify
JAVA_HOME=/usr/lib/jvm/java-21-openjdk PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH mvn -q verify
mvn -q -Djavaspec.language.matrix.strict=true -Dtest=JavaLanguageCoverageManifestTest test
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
mvn -B -ntp clean verify -Psecurity
scripts/generate-api-baseline.sh /tmp/javaspec-pre-rc5-api-baseline.md
```

Evidence:

- Core verification passed 884/884 on both Java 21 and Java 25.
- Aggregate Surefire reports contained 979 tests with zero failures, errors, or skips.
- The strict language manifest retained 50 covered rows and zero planned rows.
- The Java 8 bytecode guard inspected 323 core classfiles with maximum major version 52.
- Aggregate adapter/example verification and the RC4-named release dry-run passed.
- OWASP Dependency-Check analyzed two dependencies and reported zero vulnerabilities; its OSS Index
  analyzer remained disabled because that service now requires credentials.
- The RC1-to-current API inventory retained 172 added public/protected declaration lines and zero
  removed declaration lines; additions remain classified through `docs/api-surface-1.0.md`.

This is pre-cut evidence only. The repository remains aligned at `1.0.0-RC4`; it does not claim an
RC5 version, CI run, tag, remote artifact, or publication. Those identities must be generated from
the clean aligned RC5 commit.

## 2026-08-08 — aligned RC5 clean local qualification

Version-cut commit `4509ebc` aligns every artifact and current consumer document at `1.0.0-RC5` and
regenerates the API baseline. Release-hardening commit `8f93a46` fixes archive timestamps, configures
deterministic Gradle archive ordering, and makes the release dry-run rebuild and compare every
published main/source/Javadoc artifact. The worktree was clean throughout the final qualification.

The complete preflight, Java 21/25, aggregate, reproducibility, security, API, and downstream command
set passed. Key results are:

- Java 21 and Java 25 core verification: 884/884 each, zero failures/errors/skips.
- Strict language manifest: 50 covered rows, zero planned rows.
- Aggregate Java 8 bytecode guard: 323 core classfiles, maximum major version 52.
- API baseline SHA-256 `7479ac621c1574df519c02b2f606e0d39c105c2d8b7c8385ead8071737a1df1a`;
  a second generation was byte-identical, with 172 RC1-to-RC5 declaration additions and zero
  declaration removals.
- OWASP Dependency-Check: two dependencies analyzed, zero vulnerabilities; retained JSON SHA-256
  `4631cd6fcb6a95abe3236a90cbd1374789a17bce74e410ba39b7865339594ab4`.
- `JAVASPEC_RELEASE_TAG=v1.0.0-RC5` preflight: version/tag aligned and no build-file `SNAPSHOT`
  references.

The reproducible release manifest has SHA-256
`a584462ac44fb3453e093083755fc29cc5ef7b5baad3fb3c278c1a54ea43a7f0` and binds all 18 archives.
Main artifact hashes are:

| Artifact | SHA-256 |
|---|---|
| Core | `b650e56744ab53979cd93e75d39c8787abe5741d1e7eb6f7a9ab3ab5c1f2bf79` |
| Maven plugin | `1922c6d38e01683034aa981cc4bb3f3c855d76dcc00c9dc346cb6579e6dba34f` |
| JUnit Platform engine | `9fdee79f7261794cb060b6eab9e969b4c82c3ac1ffb0e7496c5fd88d8812bcb1` |
| Bytecode doubles | `c894846e5fa7a7d3208db425cd57b451958d8004704b4f41252f17c0c2e37ff8` |
| Bytecode agent | `0f5104f0fb5bf68deb42be1d8ff6cb60fad36b1dbd78c15613bff3f54c2782c8` |
| Gradle plugin | `7ceea4bb466313a9175e86458c846ed0b852d11ad7f399710a1f291a08e3c87b` |

Tasks source-backed qualification consumed clean JavaSpec commit `8f93a46` under Java 21. Both the
full and explicit isolated builds selected identical core/plugin SHA-256 values
`d56102c56f825275c8d033e266c03b1ce0ee025ba2c2a08010712c55c84d7b37` and
`e53685403446fcf8cc8580220d412a6cf172442fcfbc92767ee325c1f1dadf6e`, proving same-environment
reproducibility for source-manifest SHA-256
`a19a6a2583d76993651270287c6bb3e039f99f25e6304dfc8bef0f63ffdb941c`. QG-001, core 884/884,
plugin 33/33, unchanged `TaskSpec` 9/9, direct probe 4/4, explicit Cucumber 1/1, and the full Tasks
reactor at 66 total / 65 passed / one native-only skip all passed.

The retained local archive is
`.ide/agent-runs/javaspec-1.0.0-rc5-20260808/local-qualification/`; its `SHA256SUMS` file has SHA-256
`bec6e44ca1c6ae77abe291b58f08263c8326c651f67d4d3d512706cb505f0566`, and every listed entry
validates.

The release branch was pushed through evidence commit `424ea11`. Initial remote run
[`31261275000`](https://github.com/mgiustiniani/javaspec/actions/runs/31261275000) passed all six jobs
but reported Node 20 deprecations from the workflow actions. Commits `b9ce403` and `4d72aca` upgraded
checkout, Java, and Gradle setup actions and aligned the manual Gradle publication default. Final run
[`31261952121`](https://github.com/mgiustiniani/javaspec/actions/runs/31261952121) passed the Java
8/11/17/21/25 core jobs and full Java 21 verification with zero annotations.

This closed local qualification and release-branch CI before the publication boundary below.

## 2026-08-08 — RC5 tag, Maven publication, and immutable replay

The release branch was merged with `--no-ff` into `main` as production commit `ae9291f` and into
`develop` as `3b9a781`. Both branch CI runs passed before annotated tag `v1.0.0-RC5` was pushed.
Tag CI run [`31262851868`](https://github.com/mgiustiniani/javaspec/actions/runs/31262851868)
passed the Java 8/11/17/21/25 matrix and full Java 21 verification.

Release run [`31262851841`](https://github.com/mgiustiniani/javaspec/actions/runs/31262851841)
passed every guard, release dry-run, signing probe, five Maven deployments, and Gradle submission.
Direct Maven Central verification established:

- all POM, main, source, and Javadoc files for core, Maven plugin, JUnit Platform engine, bytecode
  doubles, and bytecode agent are available;
- all 20 detached signatures validate with EDDSA key
  `92EBAB37E11720596CD690CF212398D74CE93120`, which is retrievable from the Ubuntu keyserver;
- published main JAR SHA-256 values are core
  `d56102c56f825275c8d033e266c03b1ce0ee025ba2c2a08010712c55c84d7b37`, Maven plugin
  `a70d4f32beb5bbcac13f73b679d5510f418d193f2929722720117ae1c064c7f5`, JUnit engine
  `fff5e8706bcac4db1d72a4a83c48286658e55684c517cf77f89721bab541ddc1`, bytecode doubles
  `4ad2852f6e8f6f4c279dde9660e90fea01a41e1c0dea238c502ae5ab44401f0e`, and bytecode agent
  `9dc429e64dea782f2397f53fc43af44daee5b9f27883689e4b45d2e1a4509131`.

Five standalone Maven consumers resolved an empty local repository from Central and passed: Maven
plugin, Prophecy, JUnit Platform, bytecode doubles, and bytecode agent. `magrathea-pki` branch
`dogfood/javaspec-1.0.0-rc5` at `7c6c41e` then provided the JLC-8 Java 21 replay. Its launcher bound
the published core hash above; the first CLI run generated two support files and passed 4/4, the
second reported `NO_CHANGES`, identical generated hashes, and 4/4 again. An isolated Maven domain
build resolved the published core/plugin/engine and passed both Surefire and JavaSpec at 4/4 with
zero pending. Source manifests were unchanged. A broader consumer reactor stopped later at the
pre-existing `@not-implemented` Cucumber scenario in `trust-engine-api-adapter`; the JavaSpec domain
and preceding modules were green, so this is retained as a consumer-owned boundary rather than an RC5
defect.

A clean tag checkout rebuilt all 18 archives twice under the publisher's Temurin 21.0.11+10 and was
internally byte-reproducible. Comparing the 15 Maven archives with Central matched 14 exactly. The
only mismatch is `javaspec-1.0.0-RC5-javadoc.jar`: extracted trees differ only in ten implicit link
labels inside `index-all.html`; runtime, source, signatures, URLs, and all other Javadoc archives are
identical. Cross-environment core Javadoc determinism therefore remains open for stable 1.0.

The Gradle `publishPlugins` task succeeded but reported that `io.github.jvmspec` was submitted for
first-publication approval; the public marker is not yet resolvable. This is an external pending gate.

The retained remote archive is
`.ide/agent-runs/javaspec-1.0.0-rc5-20260808/remote-publication/`; its 64-file `SHA256SUMS` manifest
has SHA-256 `b0974ec38b44ce30040d9aea06729bc093b984cefd2824f8404f10b4a17eab94`, and every entry validates.

## 2026-08-08 — post-publication reproducibility correction

Develop commit `67db10c` resolves the one RC5 replay mismatch without changing a supported JVM
signature. `AssertionDispatcher` alias Javadocs now use explicit link labels, eliminating the
filesystem-order-dependent qualification in `index-all.html`. The release dry-run now deletes every
Maven and Gradle output before both checksum passes rather than repackaging an existing Javadoc tree.

The corrected tree passed Java 21 core verification at 884/884 and regenerated an unchanged API
baseline. The complete release dry-run passed in the original worktree and a fresh detached worktree
under Temurin 21.0.11+10. Both clean builds reproduced all 18 archives internally and produced the
same cross-worktree checksum manifest, SHA-256
`02d9c734fa9712b16222dff9a943e8a84c042dfe55b8cad20f3533cae629e4c7`.

This commit does not and cannot change immutable RC5. It is qualified input for the next stable
candidate. Evidence is retained at
`.ide/agent-runs/javaspec-1.0.0-rc5-20260808/post-publication-repro-fix/`; its `SHA256SUMS` manifest
has SHA-256 `e24742aeeb220bf259ec0787a331ac851c4ec10b22909bf33570182fde96f2cb`.
