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

Remaining RC/final cut-time evidence:

- Tag/version/workflow alignment at actual tag time.
- Confirmation that `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` are configured as GitHub
  repository secrets.
- Publication-generated signatures and Central/Gradle Plugin Portal staging evidence.
