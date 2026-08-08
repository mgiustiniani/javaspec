# javaspec documentation

This index separates current user and compatibility contracts from architecture history and
append-only qualification evidence.

## Current release baseline

- Maven release: `1.0.0-RC5`, available under `io.github.jvmspec`.
- Production tag: `v1.0.0-RC5` on `ae9291f`.
- Post-RC5 qualification baseline: `9e4a317`; reproducibility fix: `67db10c`. Current documentation
  follows `develop`.
- Maven Central: five modules, signed main/source/Javadoc artifacts, verified consumers.
- Gradle plugin id: `io.github.jvmspec`; RC5 was submitted successfully but first-publication
  approval and public marker resolution remain pending.
- Stable `1.0.0` is not yet published.
- `develop` now contains a post-RC5 project-specific Native Image preview; immutable Maven Central
  RC5 artifacts do not contain it.

Use [`release-1.0-checklist.md`](release-1.0-checklist.md) and
[`release-1.0-rc-evidence.md`](release-1.0-rc-evidence.md) for the source-bound status. Historical
phase statements elsewhere must not override those current records.

## Start here

- [Project README](../README.md) — installation, examples, adapters, and common APIs.
- [Multilingual user manual](usermanual/README.md) — English, Italian, Spanish, German, French, and
  Simplified Chinese.
- [Multilingual section 1 manual pages](man/README.md).
- [Troubleshooting](troubleshooting.md).
- [Migration guide](migration-guide-1.0.md).
- [Release notes](release-notes-1.0.0.md).
- [Project-specific Native Image preview](native-image.md) — build-linked Maven executable, current
  scope, and closed-world limits.

## Guided development

- [javaspec Spec-Driven Development Agent](agent/javaspec-guided-development-assistant.md) — a
  copyable project-agnostic example inspired by the `bdd-java` workflow's `spec-driven` agent.
- [Generation contract](generation-contract-1.0.md).
- [Result contract](result-contract-1.0.md).
- [Test matrix and generation coverage](test-matrix-generation.md).

## Stable 1.0 contracts

- [Capabilities and support policy](CAPABILITIES.md)
- [API surface classification](api-surface-1.0.md)
- [Compatibility policy](compatibility-policy-1.0.md)
- [Java compatibility](java-compatibility-1.0.md)
- [PHPSpec compatibility charter](phpspec-compatibility-charter.md)
- [PHPSpec compatibility matrix](phpspec-compatibility-matrix.md)
- [Matcher contract](matcher-contract-1.0.md)
- [Prophecy contract](prophecy-contract-1.0.md)
- [Example-data contract](example-data-contract-1.0.md)
- [Extension SPI](extension-spi-1.0.md)
- [JUnit Platform contract](junit-platform-contract-1.0.md)
- [JUnit-to-javaspec guide](junit-to-javaspec-guide.md)
- [Cucumber boundary](cucumber-boundary.md)
- [Bytecode doubles guide](bytecode-doubles.md)

## Architecture

- [ARC42 overview](arc42/README.md)
- [Architecture decision records](adr/)
- [Java LTS research](research/java-lts-data-structures.md)
- [PHPSpec feature inventory](research/phpspec-feature-inventory.md)

ADRs record decisions at the time they were accepted. Later status corrections are additive; current
artifact availability is governed by the release checklist, not by old publication assumptions.

## Release qualification and history

- [Release acceptance tests](release-1.0-acceptance-tests.md)
- [Release audit](release-1.0-audit.md)
- [Release checklist](release-1.0-checklist.md)
- [RC5 API review](release-1.0-rc5-api-review.md)
- [RC evidence](release-1.0-rc-evidence.md)
- [Test and quality report](test-report.md) — append-only historical evidence; read its current-status
  preface before older phase sections.
- [Historical API baseline](history/api-baseline-1.0.0.md)
- [Pre-1.0 implementation-plan archive](history/implementation-plan-pre-1.0.md)

## Documentation verification

From the repository root:

```sh
scripts/check-usermanuals.sh
scripts/check-man-pages.sh
scripts/check-current-docs.sh
scripts/check-api-surface.sh
```

The English long-form manual is the semantic source for detailed behavior. Command names, options,
coordinates, release availability, safety rules, and exit codes must remain aligned across all
localized user manuals and manual pages.
