# 0012 — Non-disruptive Aggregate Release and CI Verification

## Status

Accepted

## Context

The project now has a zero-runtime-dependency core artifact plus standalone optional Maven plugin, Gradle plugin, and JUnit Platform engine artifacts. The user asked whether converting the repository to a Maven multi-module build would help release and CI verification.

A mandatory root multi-module reactor would make root `mvn verify` cover more artifacts, but it would also disrupt the current architecture boundary where repository-root Maven verification builds and audits only the core artifact. The optional adapters intentionally remain standalone so their build-tool or JUnit Platform dependencies do not affect the core runtime dependency policy.

## Decision

Do not convert the repository to a Maven multi-module build in this increment.

Keep repository-root `mvn verify` as a core-only verification gate. Add non-disruptive aggregate verification instead:

- `scripts/verify-all.sh` runs the root Maven verify and runtime dependency audit, installs the current core snapshot locally, verifies the standalone Maven plugin and JUnit Platform engine with their own Maven POMs and runtime dependency audits, and verifies the standalone Gradle plugin build and runtimeClasspath audit.
- The script supports `MAVEN_BIN`, `JAVASPEC_GRADLE_BIN`, and explicit `JAVASPEC_SKIP_GRADLE=1` for local tool selection.
- `.github/workflows/ci.yml` adds a core Java matrix for Java 8, 11, 17, 21, and 25 plus a Java 21 full-verification job that runs the aggregate script with Gradle 8.8.
- No publishing, signing, release deployment, or secret-dependent behavior is added.

## Consequences

Positive consequences:

- The core artifact boundary remains unchanged and root Maven verification stays zero-runtime-dependency focused.
- Optional adapters are verified explicitly without making them mandatory root reactor modules.
- CI can exercise both the cross-JDK core matrix and an aggregate adapter verification path.
- Local release verification has one documented command while still allowing explicit Gradle executable selection or an explicit Gradle skip.

Negative consequences and limitations:

- Root `mvn verify` still does not verify standalone adapters by itself; release checks must run `scripts/verify-all.sh` or equivalent explicit commands.
- The aggregate script depends on a suitable local Gradle executable unless `JAVASPEC_SKIP_GRADLE=1` is intentionally set.
- The GitHub Actions workflow was initially locally parsed as valid YAML and the aggregate script passed locally before remote evidence was available; subsequent status reports record user-/maintainer-confirmed remote GitHub Actions success for HEAD `4d30e63` on `develop` and, after Phase 20/21/22 were pushed, for HEAD `5088e96` on `develop`, without independently queried run IDs, URLs, durations, or logs.
- A future multi-module or publishing/signing design remains possible, but it requires a separate decision because it would change release and dependency boundaries.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), [10. Quality Requirements](../arc42/10-quality-requirements.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
