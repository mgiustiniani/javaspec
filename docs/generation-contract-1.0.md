# javaspec 1.0 generation contract

Generation supports the PHPSpec workflow by creating mechanical scaffolding only. It must not invent domain logic or hide RED feedback.

## Deterministic generation report

`javaspec run --generation-report <file>` atomically writes generation report schema version 1
for successful, dry-run, stopped, and failed pipelines. The document contains no timestamp;
`pendingStubs` uses source-root-relative `/`-separated paths sorted by path and line. Consumers
must use `outcome`, `exitCode`, `proceed`, `actions`, `appliedWrites`,
`pendingGenerationWork`, and `pendingStubs` rather than parsing human diagnostics. Each action is
reported as `PROPOSED` or `APPLIED`; `appliedWrites` counts only actual changed-file writes. See
[`docs/schemas/generation-report-v1.schema.json`](schemas/generation-report-v1.schema.json).

With `--formatter json`, stdout is reserved for exactly one run-result JSON document. Human
configuration, discovery, generation, compilation, pending-stub, and execution diagnostics are
written to stderr, including early generation stops and compilation failures.

## Generation outcomes

| Outcome | Meaning | CLI/report behavior |
|---|---|---|
| planned | `--dry-run` found work that would be generated or updated. | Print `Would ...`, exit non-zero before execution; write `--generation-report` when requested. |
| applied | `--generate` or accepted prompt wrote/updated files. | Print generated/updated paths, proceed to compile/run when requested. |
| already satisfied | Target source/support/wrapper already matches discovered behavior. | No mutation; proceed. |
| refused | User declined generation/update. | Exit non-zero before execution. |
| unsupported | Requested generation target cannot be represented safely in Java/source profile. | Refuse with diagnostic; do not write partial source. |
| incompatible | Profile enforcement rejects generated source for selected Java profile. | Exit usage/profile error before writing. |
| failed | I/O, compile, classpath, report, or internal generation error. | Exit error; do not present GREEN. |
| pending implementation | Generated production body still contains `// javaspec:stub`. | In compiled runs, add synthetic `BROKEN` result and non-zero exit. |

## Mutation rules

- Mutating generation is opt-in through prompt acceptance or `--generate`.
- `--dry-run` must not write files.
- Generation writes only mechanical source shapes: specs, support classes, subject skeletons, constructors, factories, methods, record components/accessors, prophecy wrappers, and support helpers.
- Generated production method bodies carry `// javaspec:stub` until the user implements domain logic.
- Existing records are preserved as records; record headers/components are the semantic source for record evolution.
- Ambiguous or unsupported source updates fail closed instead of guessing.
- Generated-source artifacts such as `*SpecSupport` and `*Prophecy` are derived artifacts and may be fully regenerated.

## Atomic write policy

Where javaspec writes source content directly, it uses a same-directory temporary file and then moves it into place with `ATOMIC_MOVE` when supported by the filesystem, falling back to replace when atomic move is unavailable. This applies to generated source files, support files, prophecy wrappers, and source updates routed through generation helpers.

## Verification

Primary automated evidence:

- `AtomicFileWriterTest` for atomic-write helper behavior.
- `StubMarkerTest` for generated stub markers and scanning.
- `MainPhase29CompileCliTest.runCompileWithPendingGeneratedStubCannotAccidentallyGreen` for no accidental GREEN.
- Generation/update tests under `src/test/java/io/github/jvmspec/generation`.
- CLI generation/compile tests under `src/test/java/io/github/jvmspec/cli`.
- `scripts/verify-all.sh` for full adapter/example verification.
