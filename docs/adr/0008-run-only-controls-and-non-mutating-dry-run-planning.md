# 0008 — Run-Only Controls and Non-Mutating Dry-Run Planning

## Status

Accepted

## Context

Phase 9 expanded `javaspec run` with operational controls for local and CI use: dry-run planning, stop-on-failure, formatter selection, profile selection, and verbose diagnostics. These controls affect discovery, generation/update planning, execution, output, and diagnostics, but they do not belong to `describe`, which intentionally remains a specification/support skeleton command.

The implementation must preserve the PHPSpec-style split from ADR 0003, the Java 8 and zero-runtime-dependency constraints from ADR 0001 and ADR 0002, the configuration defaults from ADR 0005, and the reflection runner boundary from ADR 0006.

## Decision

Run controls are accepted only by `javaspec run`. `describe` and `desc` reject `--generate`, `--dry-run`, `--stop-on-failure`, `--formatter`, `--profile`, `--verbose`, `--report`, `--report-file`, `--constructor-policy`, `--class`, and `--example` because those options either mutate production code, control execution, or describe run output.

`run --dry-run` performs discovery and planning without writes and without prompts. It reports pending related-spec/support generation, support updates, constructor changes, method bodies, ordinary-interface declarations, annotation elements, and missing production type generation. Dry-run exits `1` when pending generation/update work exists. If no pending generation/update work exists, execution proceeds according to the normal runner result semantics, so passing or skipped/pending-only runs exit `0` and failed or broken executable examples exit `1`.

`run --stop-on-failure` stops reflection execution after the first FAILED or BROKEN executable example. Without the flag, the runner processes all discovered example metadata.

`run --formatter <progress|pretty|custom>` and `run --profile <java8|java11|java17|java21|java25>` override valid configured/default selections. Formatter selection originally covered built-in names; Phase 25 / ADR 0018 extends it to ServiceLoader-discovered names available on the effective run classloader. Phase 26 / ADR 0019 extends profile selection with conservative target-profile enforcement before generation/update writes; this remains source/generation-scoped enforcement rather than compiler-grade integrated execution.

`run --verbose` prints the selected run settings before run work proceeds.

## Consequences

Positive consequences:

- The command boundary remains clear: `describe` is spec-only, while `run` owns production generation/update, execution, output controls, and reports.
- CI can detect pending generated work without mutating the workspace.
- Local users can choose concise or detailed output and stop early on the first executable failure.
- Configuration defaults remain useful while command-line options retain explicit precedence.

Negative consequences and limitations:

- Dry-run planning must stay synchronized with every generation/update capability, including later method-generation increments.
- Profile enforcement can be misunderstood as compiler-grade validation of all project source and dependencies; documentation must keep the conservative source/generation-scoped boundary explicit.
- CLI formatter selection can use ServiceLoader-discovered names after ADR 0018, but only when providers are available on the effective run classloader.
- Dry-run behavior has multiple exit paths and therefore requires regression tests whenever run planning changes.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), and [10. Quality Requirements](../arc42/10-quality-requirements.md).
