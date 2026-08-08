# 0013 — Release-readiness scaffolding and publication gates

## Status

Accepted — Maven publication is complete; Gradle first-publication approval remains external.

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

Maven artifacts, including `1.0.0-RC5`, are published under `io.github.jvmspec`. RC5
`publishPlugins` submission succeeded for Gradle plugin id `io.github.jvmspec`, but the first
publication still awaits Portal approval and its public marker is unavailable. Submission must not
be described as publication. The stable `1.0.0` release remains gated by direct Portal verification
and final-candidate qualification.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime
View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8.
Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md),
[10. Quality Requirements](../arc42/10-quality-requirements.md), and [11. Risks and Technical
Debt](../arc42/11-risks-and-technical-debt.md).
