# javaspec 1.0 result and diagnostics contract

This document freezes the 1.0 meaning of result states across the PHPSpec-first core and adapters.

## Core statuses

| Status | Meaning | Exit-code effect | JSON | JUnit XML | JUnit Platform |
|---|---|---:|---|---|---|
| `PASSED` | Behavior example completed successfully. | success unless other examples fail/break | `status: "PASSED"` | `<testcase>` without failure/error/skipped | `executionFinished(SUCCESSFUL)` |
| `FAILED` | Behavior expectation failed. This is meaningful RED. | non-zero | `status: "FAILED"` with `failureDetail` | `<failure>` | `executionFinished(FAILED)` |
| `BROKEN` | javaspec could not execute or trust the behavior result: construction failure, lifecycle failure, unsupported injection, classpath/load problem, adapter/internal error, or generated stubs still pending in a compiled run. | non-zero | `status: "BROKEN"` with `failureDetail` | `<error>` | `executionFinished(FAILED)` with broken detail |
| `SKIPPED` | User explicitly skipped an example. | success if only passed/skipped/pending | `status: "SKIPPED"` | `<skipped message="...">` | `executionSkipped` |
| `PENDING` | User explicitly declared behavior pending. | success if only passed/skipped/pending | `status: "PENDING"` and separate pending counts | `<skipped message="Pending: ...">` where needed | `executionSkipped` with pending reason |

Generated incomplete behavior is not the same as user-declared `PENDING`. A production body containing `// javaspec:stub` means domain logic has not been implemented. In compiled runs, remaining stub markers add a synthetic broken result:

```text
javaspec.generation.PendingStubs#generated_stubs_pending_implementation
```

This prevents accidental GREEN when a generated Java default return happens to satisfy an assertion.

## CLI exit codes

| Code | Meaning |
|---:|---|
| `0` | Successful command, help, no specs found, or only passed/skipped/pending examples. |
| `1` | Failed or broken executable result, refused/pending generation path that should fail CI, or compiled run with remaining generated stubs. |
| `64` | Usage/configuration/profile violation. |
| `70` | I/O, dependency resolution, compilation, report writing, or unexpected infrastructure failure. |

## Diagnostics taxonomy

A 1.0 diagnostic must let users distinguish:

- behavior expectation failure;
- subject construction failure;
- missing subject/member with generation available;
- generation refused or unsupported;
- generated stubs still pending implementation;
- prediction not satisfied;
- unexpected collaborator call;
- lifecycle setup/teardown failure;
- compile/classpath/bootstrap error;
- adapter failure;
- internal framework error.

## Report schema policy

- JSON report `schemaVersion: 1` evolves additively only.
- JUnit XML remains CI-compatible; pending maps to skipped because XML has no distinct pending status.
- JUnit Platform descriptors/events translate javaspec suite/spec/example/row semantics and do not require Jupiter annotations.
- Row data, source metadata, failure detail, and stable ids are part of the report contract once emitted.

## Verification

Primary automated evidence:

- `RunResult`, `SpecResult`, and `ExampleResult` tests in `SpecRunnerTest`.
- `RunReportWriterTest` JSON status/failure/pending tests.
- `JUnitXmlReportWriterTest` failure/error/skipped/pending tests.
- `MainPhase29CompileCliTest.runCompileWithPendingGeneratedStubCannotAccidentallyGreen`.
- Maven, Gradle, and JUnit Platform adapter tests exercised by `scripts/verify-all.sh`.
