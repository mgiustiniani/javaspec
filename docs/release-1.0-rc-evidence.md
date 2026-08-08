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
