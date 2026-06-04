# 0014 — Standalone adoption assets and default examples verification

## Status

Accepted

## Context

Phase 21 follows the Phase 20 release-readiness scaffolding increment. Public publication is still postponed: no signing, portal publication, deploy automation, final release version/tag, or final publish approval has been completed. Until artifacts are publicly available, consumers and maintainers need local adoption assets that demonstrate the supported Maven plugin, Gradle plugin, and JUnit Platform engine paths without changing the core runtime or making the examples part of the root build reactor.

The Phase 18 report format already exposes schemaVersion 1 with additive stable id and source-location fields, and Phase 14 added JUnit XML-compatible reports. External consumers need a documented JSON schema and golden report examples for tooling integration. Maintainers also need local verification that standalone examples generate the expected reports and stable identifiers.

Phase 21 added only adoption assets: `docs/schemas/run-report-v1.schema.json`, golden report examples under `docs/examples/reports/`, standalone consumer projects under `examples/`, `scripts/verify-examples.sh`, and an update to `scripts/verify-all.sh` so examples run by default after core and adapter verification. No core production/test Java files changed, no runtime dependencies were added, and no public publish/deploy/signing commands were run.

## Decision

Keep examples, schema docs, and golden reports as standalone adoption assets:

- Place report schema documentation under `docs/schemas/` and golden report examples under `docs/examples/reports/`.
- Keep `examples/maven-basic/`, `examples/gradle-basic/`, and `examples/junit-platform-basic/` as standalone consumer projects, not root Maven modules and not part of a mandatory root reactor.
- Use local snapshot installation for examples while public artifacts remain unpublished. The Gradle example may use an included build for `javaspec-gradle-plugin`, while still resolving the core snapshot locally.
- Verify examples through `scripts/verify-examples.sh`, which installs local snapshots, runs the Maven plugin example, JUnit Platform example, and Gradle plugin example, and asserts report markers such as `schemaVersion`, stable id `spec.com.example.CalculatorSpec#it_adds_two_numbers`, `PASSED`, and source `line=11`.
- Run standalone examples by default from `scripts/verify-all.sh` after core and adapter checks. Use `JAVASPEC_SKIP_EXAMPLES=1` only as an explicit all-examples opt-out, and `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` only as an explicit Gradle-example opt-out inside `scripts/verify-examples.sh`.
- Continue to support `MAVEN_BIN` and `JAVASPEC_GRADLE_BIN` for tool selection.
- Do not treat examples verification as public publication, deployment, signing, or remote CI success.
- Do not change the zero-runtime-dependency core, the standalone optional adapter boundaries, or the postponed-publication boundary.

## Consequences

Positive consequences:

- New users have concrete standalone Maven, Gradle, and JUnit Platform adoption examples without the repository pretending those examples are root modules.
- Tooling authors have a versioned JSON schema and golden JSON/JUnit XML-compatible report examples to validate against.
- Aggregate local verification now covers the documented adoption path by default, making drift between examples, reports, and adapter behavior more visible.
- The core runtime dependency and standalone adapter boundaries remain unchanged.

Negative consequences and limitations:

- `scripts/verify-all.sh` now takes longer and requires the example prerequisites unless `JAVASPEC_SKIP_EXAMPLES=1` is explicitly selected.
- Example verification depends on local Maven snapshot installation until public artifacts are available.
- Gradle example verification still depends on a compatible Gradle executable; `JAVASPEC_GRADLE_BIN` or the explicit `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` opt-out may be needed on local machines.
- Phase 21 has local verification only until the changes are pushed and CI-run; no remote CI success is claimed by this decision.
- Future report schema changes must update the schema, golden reports, examples, user docs, and verification assertions together.
- Public publication remains postponed and still requires a future ADR or ADR update before signing, staging, deploying, portal publication, or final publishing is implemented.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), [10. Quality Requirements](../arc42/10-quality-requirements.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
