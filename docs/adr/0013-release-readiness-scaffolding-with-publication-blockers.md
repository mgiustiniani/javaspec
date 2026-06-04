# 0013 — Release-readiness scaffolding with resolved metadata and postponed publication

## Status

Accepted

## Context

Phase 20 prepares the project for eventual public release checks after the original roadmap was completed through Phase 18 and after Phase 19 added non-disruptive aggregate release/CI verification. The repository now has a core artifact plus standalone optional Maven plugin, Gradle plugin, and JUnit Platform engine artifacts.

The project owner confirmed the license as MIT and confirmed the maintainer as `Mario Giustiniani <mariogiustiniani@gmail.com>`. The MIT `LICENSE` already existed on `main`; Phase 20 copied it exactly from `origin/main`, and tester verification confirmed the identical blob `b990d5492f3ef404ffc145890b83e51914351bb5`. Release metadata now includes the MIT license and the confirmed maintainer/developer metadata in the root POM, Maven plugin POM, JUnit engine POM, and Gradle generated POM metadata.

Public publication still requires decisions and credentials that remain outside this implementation increment: GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, the final release version/tag, and final publish approval. Adding publishing/signing automation before those are resolved would create misleading release behavior.

## Decision

Add release-readiness scaffolding only, with confirmed license and maintainer metadata but without publishing or deployment automation:

- Keep the repository non-reactor/non-multi-module; do not convert the root build to a mandatory Maven multi-module reactor.
- Add and run `scripts/check-version-alignment.sh` before aggregate verification so the root Maven version, standalone Maven plugin version, standalone JUnit Platform engine version, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` stay aligned.
- Add `CHANGELOG.md` and `RELEASING.md` as release-process documentation.
- Preserve the confirmed MIT `LICENSE` from `origin/main` and declare MIT license metadata.
- Add confirmed maintainer/developer metadata for `Mario Giustiniani <mariogiustiniani@gmail.com>`.
- Add safe URL, SCM, and GitHub Issues metadata to Maven and Gradle publication metadata.
- Add Maven `release-artifacts` profiles for sources and javadocs only.
- Add Gradle source and javadoc jar readiness through the standalone Gradle plugin build.
- Do not add GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, secret usage, final release tagging, final publish approval, or deploy/publish commands until explicit owner decisions are made.

## Consequences

Positive consequences:

- Maintainers can locally verify version alignment, confirmed MIT/maintainer metadata, and source/javadoc artifact readiness before any public publication design.
- Release documentation now states the exact remaining publication blockers instead of implying publication is ready.
- Maven and Gradle metadata can include confirmed license/maintainer metadata plus safe project URL, SCM, and issue-management information without inventing legal or personal metadata.
- The existing zero-runtime-dependency core and standalone adapter boundaries remain unchanged.

Negative consequences and limitations:

- Public publication remains intentionally postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved outside normal local verification.
- `release-artifacts` packaging checks create local source/javadoc artifacts only; they do not sign, stage, deploy, or publish anything.
- Phase 20 local verification is supplemented by later user-/maintainer-confirmed remote GitHub Actions success for HEAD `5088e96` on `develop` after Phase 20/21/22 were pushed; no GitHub run IDs, URLs, durations, or logs were independently queried.
- Future actual publishing/signing automation requires a new or updated ADR because it will change release, credential, and final publication boundaries.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), [10. Quality Requirements](../arc42/10-quality-requirements.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
