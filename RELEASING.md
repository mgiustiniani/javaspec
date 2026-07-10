# Releasing

Artifacts are published on Maven Central under `io.github.jvmspec`. The Gradle plugin is published
on the Gradle Plugin Portal with plugin id `io.github.jvmspec`. The active release line is
`1.0.0-RC1`; use this guide for RC and final 1.0.0 publication.

## Release checklist (for future releases)

1. Reconfirm publication prerequisites:
   - MIT license text is present in `LICENSE`; Maven and Gradle publication metadata declare the MIT License.
   - Maintainer metadata is recorded as `Mario Giustiniani <mariogiustiniani@gmail.com>`.
   - Confirm GPG signing key ownership and Central Portal / Gradle Plugin Portal credentials.
2. Choose the release version and update all aligned versions:
   - Root `pom.xml` project version.
   - `javaspec-maven-plugin/pom.xml` project version.
   - `javaspec-junit-platform-engine/pom.xml` project version.
   - `javaspec-gradle-plugin/build.gradle` `version` and `javaspecCoreVersion`.
3. Update `CHANGELOG.md`:
   - Move relevant `Unreleased` entries under the release version.
   - Add the next empty `Unreleased` section.
4. Run local verification from the repository root:
   - `scripts/verify-all.sh`
   - `scripts/verify-release-dry-run.sh`
   - `scripts/generate-api-baseline.sh` at RC1; a second run must produce no diff.
   - At RC/final tag time, `JAVASPEC_RELEASE_TAG=v<version> scripts/check-release-preflight.sh`.
   - `mvn verify` includes Animal Sniffer Java 8 API linkage verification for core.
   - The release dry-run packages core, Maven plugin, JUnit Platform engine, bytecode doubles, bytecode agent, and Gradle plugin artifacts; verifies source/javadoc jars; verifies the bytecode-agent manifest; generates/verifies SHA-256 checksums; and runs standalone consumer examples.
5. Confirm GitHub Actions is green for the release commit.
6. Confirm release artifacts are present locally where expected:
   - Main, source, and Javadoc jars for `javaspec`.
   - Main, source, and Javadoc jars for `javaspec-maven-plugin`.
   - Main, source, and Javadoc jars for `javaspec-junit-platform-engine`.
   - Main, source, and Javadoc jars for `javaspec-bytecode-doubles`.
   - Main, source, and Javadoc jars for `javaspec-bytecode-agent`.
   - Gradle plugin main, source, and Javadoc jars.
   - Generated POM metadata with URL, SCM, issue-management, license, and developer entries.
   - Bytecode agent manifest entries `Premain-Class` and `Agent-Class`.
   - Local dry-run SHA-256 checksums in `target/release-dry-run-checksums.sha256`.
7. For Maven Central / Central Portal publication:
   - Reconfirm MIT license and maintainer metadata are still correct.
   - Configure an explicit signing profile or external signing process; GPG signing is required by Central publication workflows.
   - Configure Central Portal publication steps.
8. For Gradle Plugin Portal publication:
   - Confirm plugin id, display metadata, tags, website, and VCS URL.
   - Reconfirm MIT license and maintainer metadata are acceptable for the publication target.
   - Configure Gradle Plugin Portal publishing credentials (`GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET`).
9. Tag and create the release only after local verification and CI are green. The release workflow runs
   the release preflight, release dry-run, Maven Central publication for all Maven artifacts, and
   Gradle Plugin Portal publication.
10. After the release:
    - Bump all aligned versions to the next snapshot.
    - Run `scripts/check-version-alignment.sh`.
    - Add the next `Unreleased` changelog section.
