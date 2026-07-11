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

Remaining RC evidence:

- Gradle Plugin Portal page and RC1 marker become publicly resolvable after external review.
- External consumers validate the published RC1 from remote repositories rather than local staging.
