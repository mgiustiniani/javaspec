# Releasing

Artifacts are published on Maven Central under `io.github.jvmspec`. The Gradle plugin is published
on the Gradle Plugin Portal with plugin id `io.github.jvmspec`. The release was performed for
version 1.0.0.

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
   - Optional Maven artifact check: `mvn -q -Prelease-artifacts -DskipTests package`
   - Optional standalone Maven plugin artifact check: `mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package`
   - Optional standalone JUnit Platform engine artifact check: `mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests package`
   - Optional Gradle plugin artifact check: run the standalone Gradle plugin `clean test build` with the supported Gradle executable.
5. Confirm GitHub Actions is green for the release commit.
6. Confirm release artifacts are present locally where expected:
   - Main jar.
   - Source jar.
   - Javadoc jar.
   - Generated POM metadata with URL, SCM, issue-management, license, and developer entries.
7. For Maven Central / Central Portal publication:
   - Reconfirm MIT license and maintainer metadata are still correct.
   - Configure an explicit signing profile or external signing process; GPG signing is required by Central publication workflows.
   - Configure Central Portal publication steps.
8. For Gradle Plugin Portal publication:
   - Confirm plugin id, display metadata, tags, website, and VCS URL.
   - Reconfirm MIT license and maintainer metadata are acceptable for the publication target.
   - Configure Gradle Plugin Portal publishing configuration and credentials.
9. Tag and create the release only after local verification and CI are green.
10. After the release:
    - Bump all aligned versions to the next snapshot.
    - Run `scripts/check-version-alignment.sh`.
    - Add the next `Unreleased` changelog section.
