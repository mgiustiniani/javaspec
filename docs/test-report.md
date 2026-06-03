# Test and Quality Report

## Phase 12 consolidated quality matrix

Date: 2026-06-03

This report records the completed Phase 12 verification results provided by the Java tester. The earlier host-only unavailable runtime matrix is superseded by the completed Distrobox multi-JDK matrix below. The tester reported no production or test code changes.

## Executive summary

| Area | Result | Notes |
|---|---|---|
| Container toolchain | PASS | Distrobox `1.8.2.5` with Podman `5.8.2` was used for the multi-JDK matrix. |
| Multi-JDK Maven verification | PASS | Java 8, 11, 17, 21, and 25 containers each passed `mvn clean` and `mvn verify`. |
| Test totals | PASS | Each container ran 364 tests with 0 failures, 0 errors, and 0 skipped. |
| Runtime dependency audit | PASS | Java 25 container runtime scope contained only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`. |
| Java 25 Gatherer runtime probe | PASS | Java 25 runtime reflection found `Gatherer`, nested Gatherer types, and `Gatherers`. |
| Warning review | PASS | JDK 17+ emitted only expected `-source 8` / `-target 1.8` bootstrap/obsolete-option warnings. |
| Blockers | PASS | None reported. |

## Container environment

| Item | Value |
|---|---|
| Distrobox | `1.8.2.5` |
| Podman | `5.8.2` |
| Java 8 image | `docker.io/library/maven:3.9-eclipse-temurin-8` |
| Java 11 image | `docker.io/library/maven:3.9-eclipse-temurin-11` |
| Java 17 image | `docker.io/library/maven:3.9-eclipse-temurin-17` |
| Java 21 image | `docker.io/library/maven:3.9-eclipse-temurin-21` |
| Java 25 image | `docker.io/library/maven:3.9-eclipse-temurin-25` |

Per-container command:

```bash
distrobox enter --name <container> --no-tty -- bash -lc \
  'cd /home/paperboy/workspace/javaspec &&
   java -version &&
   javac -version &&
   mvn -version &&
   mvn clean &&
   mvn verify'
```

The matrix containers were created and stopped, not removed: `javaspec-jdk8-matrix`, `javaspec-jdk11-matrix`, `javaspec-jdk17-matrix`, `javaspec-jdk21-matrix`, and `javaspec-jdk25-matrix`.

## Distrobox multi-JDK matrix

| JDK | Container | Java runtime | Maven | Result | Test totals |
|---|---|---|---|---|---|
| Java 8 | `javaspec-jdk8-matrix` | `1.8.0_492` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 11 | `javaspec-jdk11-matrix` | `11.0.31` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 17 | `javaspec-jdk17-matrix` | `17.0.19` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 21 | `javaspec-jdk21-matrix` | `21.0.11 LTS` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 25 | `javaspec-jdk25-matrix` | `25.0.3 LTS` | `3.9.16` | PASS | 364 tests, 0 failures, 0 errors, 0 skipped |

JDK 17, 21, and 25 emitted only the expected Maven compiler bootstrap/obsolete-option warnings for the Java 8 source/target settings.

## Supplemental compatibility and regression audits

The Distrobox matrix is the authoritative runtime matrix. Earlier Phase 12 compatibility and regression audits remain part of the consolidated evidence:

- Targeted regression tests passed for `TypeSkeletonGeneratorTest`, `ClassMethodUpdaterTest`, `MainPhase11ReportCliTest`, `RunFormatterRegistryTest`, `ExtensionContextTest`, and `RunReportWriterTest` with 43 tests, 0 failures, 0 errors, and 0 skipped.
- Maven compiler `source` and `target` remain `1.8`.
- Production sources compiled with `javac --release 8` on JDK 21.
- 103 production class files were inspected and all used Java 8 classfile major version `52`.
- The constant-pool audit found 82 unique direct `java.*` class references, 70 intentional post-Java-8 metadata string hits, and 0 direct post-Java-8 API references.

## Runtime dependency audit

The runtime dependency audit was run in `javaspec-jdk25-matrix` with:

```bash
mvn dependency:tree -Dscope=runtime
```

Result: PASS. Runtime scope contained only the root artifact:

```text
org.javaspec:javaspec:jar:0.1.0-SNAPSHOT
```

No third-party runtime dependency leakage was found.

## Java 25 Gatherer runtime validation

The Java 25 runtime reflection probe passed in `javaspec-jdk25-matrix` for all expected stream gatherer types:

- `java.util.stream.Gatherer`
- `java.util.stream.Gatherer$Downstream`
- `java.util.stream.Gatherer$Integrator`
- `java.util.stream.Gatherer$Integrator$Greedy`
- `java.util.stream.Gatherers`

## Related documentation

- [ARC42 quality requirements](arc42/10-quality-requirements.md) summarize the quality scenarios backed by this report.
- [ARC42 risks and technical debt](arc42/11-risks-and-technical-debt.md) records remaining limitations that are not test failures.
- [README](../README.md) and the [user manual](usermanual/Home.md) reference this Phase 12 matrix for user-facing verification claims.

## Conclusion

Phase 12 is fully completed through the Distrobox multi-JDK matrix. Java 8, 11, 17, 21, and 25 containers all passed `mvn clean` and `mvn verify` with identical clean test totals. The Java 25 runtime Gatherer probe and Java 25 runtime dependency audit also passed. No blockers remain.
