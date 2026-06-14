# 0025 — Course correction: Gradle plugin test dependency version mismatch after release

## Context

The javaspec project was released to Maven Central under version `0.1.0` (commit 26a11e4). As part of the release process:

- The root `pom.xml` version changed from `0.1.0-SNAPSHOT` to `0.1.0`.
- The Gradle plugin `build.gradle` was correctly updated: `javaspecCoreVersion = '0.1.0'` and `version = '0.1.0'`.
- However, the test file `javaspec-gradle-plugin/src/test/java/org/javaspec/gradle/JavaspecGradlePluginTest.java` still has 5 occurrences of `0.1.0-SNAPSHOT` in helper methods that create temporary Gradle projects for testing (lines 569, 613, 950, 1043, 1110).

## Derailment

- The CI build fails with 9 failing tests in `JavaspecGradlePluginTest`.
- Failures include `UnexpectedBuildFailure` at line 1316 (gradleRunner failing because it can't resolve `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT` from mavenLocal) and `NullPointerException` at lines 635, 784, 740 (result.task(":javaspecRun") returning null).
- In CI, `mvn -q -DskipTests install` installs version `0.1.0` (not `0.1.0-SNAPSHOT`) to `~/.m2/repository`. The test helper projects request `0.1.0-SNAPSHOT`, which doesn't exist in CI.
- Locally the tests pass because both `0.1.0` and `0.1.0-SNAPSHOT` exist in the local Maven repo from earlier development builds.

## Required Change

Change all 5 occurrences of `0.1.0-SNAPSHOT` to `0.1.0` in `JavaspecGradlePluginTest.java` to match the actual installed version.

## Consequences

- Fixing this will make CI pass for the Gradle plugin tests.
- No behavior change for local development because both versions exist locally.
- No API, schema, or runtime dependency changes.
- Future releases will automatically stay in sync because the version is now stable at `0.1.0`.
