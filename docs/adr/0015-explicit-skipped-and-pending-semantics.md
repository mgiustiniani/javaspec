# 0015 — Explicit skipped and pending semantics

## Status

Accepted

## Context

javaspec already reported `SKIPPED` when a specification class could not be loaded or a reflected example method was unavailable. Users also need intentional non-execution semantics: skipping examples for known external constraints and marking examples pending while behavior is still being clarified or implemented.

The feature must preserve the Java 8 baseline and zero-runtime-dependency policy. It must not require JUnit, assertion libraries, or reporting libraries in the core runtime. It must also remain compatible with existing CLI/programmatic/Maven/Gradle/JUnit Platform execution paths and existing stable identifiers/descriptors.

## Decision

Add explicit skip and pending semantics in the zero-dependency API:

- Add runtime method annotations `org.javaspec.api.Skip` and `org.javaspec.api.Pending`, each with `value()` and `reason()` reason aliases.
- Add unchecked signals `SkipExampleException` and `PendingExampleException` so specs that do not extend `ObjectBehavior` can mark examples directly.
- Add `ObjectBehavior.skip()`, `skip(String reason)`, `pending()`, and `pending(String reason)` convenience helpers that throw the corresponding unchecked signal.
- Treat `@Skip` as taking precedence over `@Pending` when both annotations are present.
- For annotation-based skip/pending, do not instantiate the spec and do not run `let()`, the example body, or `letGo()`.
- For runtime skip/pending signals thrown from `let()` or an example method, run `letGo()` and mark the example `SKIPPED` or `PENDING` only if teardown succeeds; a `letGo()` failure after a skip/pending signal is `BROKEN`.
- Keep `ExampleStatus.PENDING` distinct from `SKIPPED`. `skippedCount()` remains SKIPPED-only; `pendingCount()` is separate, with combined skipped-plus-pending helpers for JUnit XML-compatible report use.
- Keep runs successful when only passed, skipped, and pending examples exist.
- Add pending counts to formatter summaries, Maven summary logging, and JSON run/spec summaries. JSON examples use `status: "PENDING"`.
- Keep JSON schemaVersion 1 additive-compatible: the writer emits `pending`, but the schema does not require it so older schemaVersion 1 reports remain valid.
- Map both `SKIPPED` and `PENDING` to JUnit XML-compatible `<skipped>` elements; the testsuite `skipped` attribute includes skipped plus pending. Pending messages use `Pending: <reason>` or `Pending by javaspec.`.
- Map pending examples in the optional JUnit Platform engine to `executionSkipped` with a `Pending:` reason while preserving unique IDs and descriptors.

## Consequences

Positive consequences:

- Users can distinguish intentionally skipped examples from pending behavior in core results and JSON reports without adding dependencies.
- Annotation-based skip/pending is cheap and side-effect-free because lifecycle and example code are not run.
- Runtime skip/pending helpers support conditional decisions in `let()` or example methods while preserving teardown semantics.
- CLI, Maven, Gradle, JSON, JUnit XML-compatible reports, and the optional JUnit Platform engine share one canonical result model.
- Existing JUnit XML consumers remain compatible because both skipped and pending examples are represented as `<skipped>`.

Negative consequences and limitations:

- JUnit XML cannot express `PENDING` as a distinct standard status, so pending is represented as skipped in that format while JSON and core results remain distinct.
- Consumers of schemaVersion 1 JSON should tolerate `pending` as an additive field and `PENDING` as an additional example status.
- Formatters, schema docs, golden reports, adapters, and tests must stay synchronized whenever skip/pending semantics change.
- Skip/pending is explicit example semantics only; bootstrap execution and deep profile enforcement remain future work.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), [10. Quality Requirements](../arc42/10-quality-requirements.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
