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

Remaining RC/final cut-time evidence:

- Java 8, Java 11, and Java 17 matrix evidence where those runtimes are available in CI/release
  infrastructure.
- `scripts/check-release-preflight.sh` on the actual version-cut commit. A disposable-copy simulation
  with all aligned build files set to `1.0.0-RC1` and `JAVASPEC_RELEASE_TAG=v1.0.0-RC1` passed; the
  current snapshot branch correctly fails the same gate.
- Tag/version/workflow alignment at tag time.
- Publication-generated signatures and Central/Gradle Plugin Portal staging evidence.
