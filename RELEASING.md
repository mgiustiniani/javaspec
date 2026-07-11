# Releasing

Maven artifacts are published under `io.github.jvmspec`; the Gradle Plugin Portal id is
`io.github.jvmspec`. The active release line is `1.0.0-RC4`; use this guide for RC and final 1.0.0
publication, and verify each version directly before announcing availability.

## Branch and tag policy

This repository uses Git Flow with these permanent branches:

- `main`: production history; every release tag points to a commit on this branch.
- `develop`: integration history for the next release.
- `release/<version>`: temporary stabilization branch created from `develop`.

Never create a release tag directly from `develop`. Prepare and verify the release branch first, then
finish it with non-fast-forward merges:

```sh
git switch develop
git pull --ff-only origin develop
git switch -c release/<version>

# Apply only release fixes, documentation, and version alignment here.
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
JAVASPEC_RELEASE_TAG=v<version> scripts/check-release-preflight.sh
git push -u origin release/<version>

# After the release branch and CI are green:
git switch main
git pull --ff-only origin main
git merge --no-ff release/<version>
git tag -a v<version> -m "javaspec <version>"

git switch develop
git merge --no-ff release/<version>
git push origin main develop
git push origin v<version>
```

Pushing the tag starts `.github/workflows/release.yml`. Delete the release branch only after the tag
workflow and publication checks succeed. Preserve a failed tag while investigating only when no
commit correction is required; if the tagged commit must change and no artifact was published,
delete the failed tag and repeat the release finish from the corrected release branch. Never move a
tag after Maven Central or the Gradle Plugin Portal has accepted that version.

## Release checklist (for future releases)

1. Reconfirm publication prerequisites:
   - MIT license text is present in `LICENSE`; Maven and Gradle publication metadata declare the MIT License.
   - Maintainer metadata is recorded as `Mario Giustiniani <mariogiustiniani@gmail.com>`.
   - Confirm GPG signing key ownership and Central Portal / Gradle Plugin Portal credentials.
2. Choose the release version and update all aligned versions:
   - Root `pom.xml` project version.
   - `javaspec-maven-plugin/pom.xml` project version.
   - `javaspec-junit-platform-engine/pom.xml` project version and `javaspec.version`.
   - `javaspec-bytecode-doubles/pom.xml` project version and `javaspec.version`.
   - `javaspec-bytecode-agent/pom.xml` project version and `javaspec.version`.
   - `javaspec-gradle-plugin/build.gradle` `version` and `javaspecCoreVersion`.
   - Standalone consumer examples that pin the release line.
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
   - Configure repository secrets `SONATYPE_TOKEN_USER`, `SONATYPE_TOKEN_PASS`, `GPG_PRIVATE_KEY`,
     and `GPG_PASSPHRASE`.
   - The workflow imports `GPG_PRIVATE_KEY`, requires an imported secret key, and performs an
     isolated loopback signing probe with `GPG_PASSPHRASE` before Maven deployment.
   - It exposes the secret as `MAVEN_GPG_PASSPHRASE` and passes the verified value through Maven's
     `gpg.passphrase` user property; the plugin does not consume an arbitrarily named variable.
   - GPG signing is mandatory for every Maven artifact.
   - Configure and verify Central Portal publication steps.
8. For Gradle Plugin Portal publication:
   - Sign in or create an account at <https://plugins.gradle.org/user/login>.
   - Open the **API Keys** tab in the Plugin Portal profile and create/copy the key and secret.
   - In GitHub, open **Settings → Secrets and variables → Actions** for this repository and create
     repository secrets `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` with those values.
   - Confirm plugin id `io.github.jvmspec`, display metadata, tags, website, and VCS URL.
   - Reconfirm MIT license and maintainer metadata are acceptable for the publication target.
   - First publication may require Plugin Portal review; verify the published version at
     <https://plugins.gradle.org/plugin/io.github.jvmspec> before declaring the Gradle release complete.
   - The tag workflow requires Maven/GPG credentials before Maven publication. If Gradle credentials
     are absent, it publishes Maven artifacts and explicitly skips the Plugin Portal step with a
     warning; rerunning after configuring Gradle credentials is safe because an existing Maven
     release is detected and not deployed again.
9. Finish the Git Flow release only after local verification and release-branch CI are green:
   - Merge `release/<version>` into `main` with `--no-ff`.
   - Create annotated tag `v<version>` on the resulting `main` release commit.
   - Merge the release branch back into `develop` with `--no-ff`.
   - Push `main`, `develop`, and then the tag.
   - The tag workflow runs contract guards, tag/dependency preflight, the release dry-run, Maven
     Central publication for all Maven artifacts, and optional Gradle Plugin Portal publication.
10. Confirm publication before deleting the release branch:
    - Verify all five Maven artifact directories and their POM, main/source/Javadoc JARs, checksums,
      and `.asc` signatures directly on Maven Central.
    - Verify the signing public key is available from a public keyserver.
    - Verify the Gradle Plugin Portal version when Gradle credentials were available.
    - Record workflow links and publication URLs in `docs/release-1.0-rc-evidence.md`.
11. After the release:
    - Bump all aligned versions to the next snapshot.
    - Run `scripts/check-version-alignment.sh`.
    - Add the next `Unreleased` changelog section.
