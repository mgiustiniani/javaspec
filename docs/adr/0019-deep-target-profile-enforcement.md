# 0019 — Deep target-profile enforcement before generation/update writes

## Status

Accepted

## Context

ADR 0001 established a Java 8-compatible binary with metadata-driven Java LTS target profiles. Earlier phases validated and reported selected profiles, but `javaspec run` could still generate or update source that contradicted the selected target profile.

Phase 26 moves profile handling beyond selection and reporting. The project needs enforcement that protects generated or updated source before writes happen, while preserving the Java 8 baseline, zero-runtime-dependency core, no integrated source/spec compilation, existing report schemas, and existing optional adapter boundaries.

The enforcement must also avoid false positives. javaspec sees descriptors inferred from source heuristics, not a full compiler type model. Unknown project types, imports outside the profile catalog, and simple names that resolve ambiguously in the catalog cannot be rejected safely.

## Decision

Add a conservative profile enforcement boundary under `io.github.jvmspec.compatibility`:

- `ProfileEnforcement` enforces one `TargetProfile` against a discovered `DescribedType`.
- `ProfileEnforcementReport` represents the immutable result for one described type.
- `ProfileViolation` represents one denied location and compatibility result.

`javaspec run` enforces the effective profile after discovery and before related-spec generation, support updates, production type skeleton generation, constructor updates, method updates, prompts, dry-run planning output, runner execution, or report writing. The effective profile comes from config unless CLI `--profile` is supplied; CLI `--profile` overrides config. `describe` remains unaffected by config profile metadata and continues to reject command-line `--profile` as a run-only option.

The enforcement checks described type kinds against their minimum Java source/profile requirements. Classes, final classes, interfaces, enums, and annotations remain allowed under `java8`; `record`, `sealed class`, and `sealed interface` require a Java 17-compatible target profile.

The enforcement also checks generated method return and parameter types when they can be resolved to known Java API owners in the profile catalog. This catches cataloged Java API types introduced in later profiles, such as Java 21 or Java 25 data-structure/stream APIs, when they appear in generated signatures for a lower target profile.

The signature checks are intentionally conservative:

- Known qualified Java API owners in the catalog are checked.
- Simple type names are checked only when they resolve uniquely to one catalog owner.
- Generic type arguments, arrays, varargs, and wildcard bounds are inspected where their owners can be resolved.
- Primitive types, `void`, unknown project types, unknown catalog owners, malformed/unresolvable type strings, and ambiguous simple names are ignored to avoid false positives.

Profile violations are CLI usage failures. The CLI exits `64`, prints `Profile compatibility error`, shows the selected profile, the spec/type source, and violation reasons, and stops before generation/update writes. Dry-run uses the same enforcement; when a violation exists, it writes no files and no JSON/JUnit XML-compatible reports.

Phase 26 does not add integrated compilation, does not change report schemas, does not add runtime dependencies, and does not change the optional Maven/Gradle/JUnit Platform adapter architecture.

## Consequences

Positive consequences:

- Selected target profiles now protect generation/update paths rather than only appearing in validation or verbose output.
- Java 8-targeted projects are protected from generated records, sealed types, and resolvable later-JDK API signatures.
- Enforcement remains Java 8-compatible and zero-dependency by reusing profile catalog metadata and compatibility results.
- Programmatic hosts can reuse the enforcement report model without invoking the CLI.
- Failing early avoids partially generated or updated source when profile compatibility is denied.

Negative consequences and limitations:

- Enforcement is not a compiler. It does not type-check source, compile specs, validate arbitrary project types, or prove that all generated code compiles under the target profile.
- Unknown, unresolvable, or ambiguous simple types are ignored, so some incompatible signatures may remain undetected until the target project compiles.
- Only cataloged Java API owners can be rejected by signature checks; catalog drift or missing symbols limit enforcement coverage.
- CLI profile violations share exit code `64` with other usage/configuration errors.
- Reports are not written on enforcement failures because enforcement occurs before runner/reportable execution.

Related decisions: [ADR 0001](0001-java-8-baseline-with-lts-target-profiles.md), [ADR 0002](0002-zero-runtime-dependency-policy.md), [ADR 0008](0008-run-only-controls-and-non-mutating-dry-run-planning.md), [ADR 0009](0009-interface-style-method-generation-and-sealed-interface-update-deferral.md), [ADR 0010](0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md), [ADR 0011](0011-optional-junit-adapter-and-canonical-javaspec-runner.md), and [ADR 0017](0017-configuration-level-report-destinations.md).

Related ARC42 sections: [2. Constraints](../arc42/02-constraints.md), [4. Solution Strategy](../arc42/04-solution-strategy.md), [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
