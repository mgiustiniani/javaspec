# 0013 — Release-readiness scaffolding with resolved metadata (publication completed)

## Status

Superseded — publication is now complete.

## Context

Phase 20 prepared the project for eventual public release checks. The repository has a core
artifact plus standalone optional Maven plugin, Gradle plugin, and JUnit Platform engine artifacts.

The project owner confirmed the license as MIT and confirmed the maintainer as `Mario Giustiniani
<mariogiustiniani@gmail.com>`. The MIT `LICENSE` already existed on `main`; Phase 20 copied it
exactly from `origin/main`, and tester verification confirmed the identical blob
`b990d5492f3ef404ffc145890b83e51914351bb5`. Release metadata now includes the MIT license and the
confirmed maintainer/developer metadata in the root POM, Maven plugin POM, JUnit engine POM, and
Gradle generated POM metadata.

Public publication required decisions and credentials that remained outside this implementation
increment: GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials,
the final release version/tag, and final publish approval.

## Decision (historical)

Add release-readiness scaffolding only, with confirmed license and maintainer metadata but without
publishing or deployment automation:

- Keep the repository non-reactor/non-multi-module; do not convert the root build to a mandatory
  Maven multi-module reactor.
- Add and run `scripts/check-version-alignment.sh` before aggregate verification so the root Maven
  version, standalone Maven plugin version, standalone JUnit Platform engine version, Gradle plugin
  `version`, and Gradle plugin `javaspecCoreVersion` stay aligned.
- Add `CHANGELOG.md` and `RELEASING.md` as release-process documentation.
- Preserve the confirmed MIT `LICENSE` from `origin/main` and declare MIT license metadata.
- Add confirmed maintainer/developer metadata for `Mario Giustiniani <mariogiustiniani@gmail.com>`.
- Add safe URL, SCM, and GitHub Issues metadata to Maven and Gradle publication metadata.
- Add Maven `release-artifacts` profiles for sources and javadocs only.
- Add Gradle source and javadoc jar readiness through the standalone Gradle plugin build.
- Do not add GPG signing, Central Portal publication, Gradle Plugin Portal
  publication/credentials, secret usage, final release tagging, final publish approval, or
  deploy/publish commands until explicit owner decisions are made.

## Consequences

Positive consequences (historical):

- Maintainers could locally verify version alignment, confirmed MIT/maintainer metadata, and
  source/javadoc artifact readiness before any public publication design.
- Release documentation stated the exact remaining publication blockers instead of implying
  publication was ready.
- Maven and Gradle metadata could include confirmed license/maintainer metadata plus safe project
  URL, SCM, and issue-management information without inventing legal or personal metadata.
- The existing zero-runtime-dependency core and standalone adapter boundaries remained unchanged.

### Current status

All publication blockers have been resolved. Artifacts are published on Maven Central under
`io.github.jvmspec`. The Gradle plugin is published on the Gradle Plugin Portal with plugin id
`io.github.jvmspec`. The 0.1.0 release is complete.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime
View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8.
Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md),
[10. Quality Requirements](../arc42/10-quality-requirements.md), and [11. Risks and Technical
Debt](../arc42/11-risks-and-technical-debt.md).
