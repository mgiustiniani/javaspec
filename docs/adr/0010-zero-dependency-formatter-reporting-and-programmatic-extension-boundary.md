# 0010 — Zero-Dependency Formatter, Reporting, and Programmatic Extension Boundary

## Status

Accepted

## Context

Phase 11 stabilized human-readable run output behind public formatter contracts, added optional JSON run reports, and introduced a minimal extension lifecycle API. These features must preserve the zero-runtime-dependency policy from ADR 0002 and reuse the immutable runner result model from ADR 0006.

The implementation also needs to avoid overstating plugin behavior. Public extension contracts exist, but Phase 11 did not discover or load external extensions; Phase 25 / ADR 0018 later adds ServiceLoader-based formatter/extension discovery without changing this ADR's zero-dependency formatter/reporting boundary.

## Decision

javaspec exposes `org.javaspec.formatter.RunFormatter` and a deterministic `RunFormatterRegistry` as zero-dependency public contracts. Built-in CLI formatter names are `progress` and `pretty`; they preserve the Phase 9 output behavior while moving rendering out of the CLI adapter.

javaspec writes optional run reports through `org.javaspec.reporting.RunReportWriter`. Reports are UTF-8 JSON generated without a JSON runtime dependency. The current schema is `schemaVersion` 1 and contains summary counts, specs, examples, nullable failure details, throwable class/message, and stack trace lines. Phase 18 adds stable spec/example ids and source file/line metadata additively while preserving the existing fields; Phase 22 adds pending counts and `PENDING` statuses additively; Phase 35 default writers add optional run-level metadata/properties while preserving schemaVersion 1 compatibility.

`javaspec run --report <file>` and `--report-file <file>` are run-only. No-spec, passing, failing, broken, skipped-only, and pending-only runs write reports after normal output. Failed or broken executable examples still exit `1` after the report is written. Dry-run pending generation/update exits before execution and does not write a report. Report write failures are I/O failures and exit `70`.

javaspec exposes minimal programmatic extension contracts through `JavaspecExtension`, the short-name alias `Extension`, and `ExtensionContext`. Extensions can register run formatters programmatically through the context registry. Phase 11 did not implement external CLI extension discovery/loading. Phase 25 / ADR 0018 later adds JDK ServiceLoader discovery for `RunFormatter`, `JavaspecExtension`, and alias `Extension` providers; configuration-driven activation, package scanning, plugin lookup, Maven plugin formatter controls, and JUnit Platform formatter controls remain out of scope.

## Consequences

Positive consequences:

- Human-readable output and JSON reports share the same immutable runner result model.
- CI can collect a stable machine-readable report with stable identifiers and source metadata where available without adding runtime JSON dependencies.
- Formatter rendering is decoupled from the CLI adapter and can be extended programmatically in tests or embedding scenarios.
- Documentation can state a precise extension boundary without promising external plugin loading.

Negative consequences and limitations:

- Before ADR 0018, CLI formatter selection was limited to built-in `progress` and `pretty`; after ADR 0018 it can also select ServiceLoader-discovered formatter names available on the effective run classloader.
- Reports remain schemaVersion 1 with additive Phase 18 identifier/source fields, Phase 22 pending fields/statuses, and Phase 35 optional run-level metadata/properties; Phase 24 adds config-level report destinations only, with no alternate report format or streaming report mode.
- Extension APIs are useful programmatically and, after ADR 0018, through classpath-scoped ServiceLoader providers. End-user plugin lookup, package scanning, and configuration-driven activation remain future work and need their own design decision before implementation.
- The JSON writer must maintain correct escaping and deterministic output internally because no JSON library is used at runtime.

Related decisions: [ADR 0017](0017-configuration-level-report-destinations.md) extends report destination configuration without changing report schemas or writers, and [ADR 0018](0018-serviceloader-external-formatter-extension-discovery.md) extends formatter/extension discovery without adding runtime dependencies.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [7. Deployment View](../arc42/07-deployment-view.md), [8. Concepts](../arc42/08-concepts.md), and [10. Quality Requirements](../arc42/10-quality-requirements.md).
