# Releasing

This repository has release-readiness scaffolding only. The MIT license and maintainer metadata are resolved, but public publishing/deployment/signing/portal credential automation remains postponed and unimplemented here. This checklist does not publish artifacts, and no secrets are required for local verification.

## Release checklist

1. Reconfirm publication prerequisites before any public publication:
   - MIT license text is present in `LICENSE`; Maven and Gradle publication metadata declare the MIT License.
   - Maintainer metadata is recorded as `Mario Giustiniani <mariogiustiniani@gmail.com>`.
   - Confirm GPG signing key ownership and Central Portal / Gradle Plugin Portal credentials outside this repository.
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
7. Before Maven Central / Central Portal publication:
   - Reconfirm MIT license and maintainer metadata are still correct.
   - Configure an explicit signing profile or external signing process; GPG signing is required by Central publication workflows.
   - Configure Central Portal publication steps outside normal local verification.
8. Before Gradle Plugin Portal publication:
   - Confirm plugin id, display metadata, tags, website, and VCS URL.
   - Reconfirm MIT license and maintainer metadata are acceptable for the publication target.
   - Configure Gradle Plugin Portal publishing configuration and credentials outside normal local verification.
9. Tag and create the release only after local verification and CI are green.
10. After the release:
    - Bump all aligned versions to the next snapshot.
    - Run `scripts/check-version-alignment.sh`.
    - Add the next `Unreleased` changelog section.

Do not run publishing, deployment, or signing commands until publication is approved and signing / portal credentials are explicitly resolved.
