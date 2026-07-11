# 0017 — Configuration-level report destinations

## Status

Accepted

## Context

javaspec already supports optional JSON and JUnit XML-compatible run reports from the CLI and optional Maven/Gradle adapters. Configuration files also already provide zero-dependency project defaults for suites, naming, constructor policy, profile, and formatter selection.

Users need reusable report destinations for `run --config <file>` and for build-tool adapters, without repeating command-line or adapter-specific report settings in every invocation. At the same time, report schemas, report contents, writer behavior, exit-code/build-failure semantics, and standalone adapter architecture must remain unchanged.

The command boundary from ADR 0008 still applies: `describe` may load configuration for spec naming, but it does not execute examples and must not write reports. Report destination configuration must also preserve the restricted line-based configuration format from ADR 0005 and the zero-dependency report writer boundary from ADR 0010.

## Decision

Add optional top-level report destinations to the javaspec configuration model. These settings configure only output paths; they do not change report schemas, report contents, report writer behavior, or runner result semantics.

Supported JSON report keys and aliases are:

- `report`
- `reportFile`
- `report-file`
- `jsonReport`
- `jsonReportFile`
- `json-report-file`

Supported JUnit XML-compatible report keys and aliases are:

- `junitXml`
- `junit-xml`
- `junitXmlFile`
- `junit-xml-file`
- `junitXmlReportFile`
- `junit-xml-report-file`

When present, values are trimmed and must be non-blank. When absent, the effective destination is absent/null and no report of that kind is written unless another entry point supplies a destination.

Precedence is explicit and adapter-local:

- `javaspec run --config <file>` writes configured JSON and/or JUnit XML-compatible reports when no corresponding CLI report option is supplied.
- CLI `run --report` / `--report-file` overrides any configured JSON destination.
- CLI `run --junit-xml` / `--junit-xml-file` overrides any configured JUnit XML destination.
- `describe --config <file>` accepts configuration files containing report destinations but does not write reports.
- Command-line report options remain run-only and are still rejected for `describe` / `desc`.
- Verbose run configuration shows the effective JSON and JUnit XML report paths, whether they came from config or CLI overrides.
- Dry-run pending generation/update still exits before execution and does not write reports.
- No-spec run report behavior remains unchanged; if a report destination is effective, the existing writers produce valid empty reports as before.
- The optional Maven plugin and Gradle plugin use config report destinations as defaults when explicit plugin/task/extension report settings are absent.
- Explicit Maven/Gradle adapter report settings override config destinations.

## Consequences

Positive consequences:

- Projects can keep common report destinations in one zero-dependency javaspec configuration file.
- CLI, Maven, and Gradle execution paths share the same destination-default semantics while preserving their explicit override mechanisms.
- `describe` can continue to accept full project configuration files without accidentally producing reports.
- Existing report consumers are unaffected because schemaVersion 1 JSON contents and dependency-free JUnit XML-compatible contents do not change.
- Dry-run, no-spec, failure exit-code, and adapter build-failure behavior remain stable.

Negative consequences and limitations:

- The configuration format has more top-level aliases to document and validate.
- Report destinations are global top-level defaults, not suite-specific settings.
- This decision does not add new report formats, schema fields, report merging, report streaming, or richer JUnit XML metadata.
- JUnit Platform engine users still rely on the JUnit Platform launcher/reporting ecosystem; this decision is about javaspec CLI and build-tool adapter report destinations.

Related decisions: [ADR 0005](0005-restricted-line-based-configuration-format.md), [ADR 0008](0008-run-only-controls-and-non-mutating-dry-run-planning.md), [ADR 0010](0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md), and [ADR 0011](0011-optional-junit-adapter-and-canonical-javaspec-runner.md).

Related ARC42 sections: [4. Solution Strategy](../arc42/04-solution-strategy.md), [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
