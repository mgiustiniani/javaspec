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
JAVA_HOME=/usr/lib/jvm/java-21-openjdk PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH mvn -q verify
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

Additional checks:

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
- Ran external consumer examples for Maven, Gradle, JUnit Platform, Prophecy, bytecode doubles, and
  bytecode agent from locally installed/staged artifacts.

Remaining RC/final cut-time evidence:

- Java 8, Java 11, and Java 17 matrix evidence where those runtimes are available in CI/release
  infrastructure.
- No-SNAPSHOT dependency check after replacing `1.0.0-SNAPSHOT` with `1.0.0-RC1` / `1.0.0`.
- Tag/version/workflow alignment at tag time.
- Publication-generated checksums/signatures and Central/Gradle Plugin Portal staging evidence.
