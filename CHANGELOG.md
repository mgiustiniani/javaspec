# Changelog

All notable changes to this project will be documented in this file.

## Unreleased

- Added the MIT `LICENSE` file and release-readiness scaffolding: version-alignment verification, release-artifact profiles, MIT license and maintainer publication metadata, and a release checklist; public publishing/deployment/signing/portal credentials remain postponed.
- Kept the repository layout non-reactor/non-multi-module: the core, Maven plugin, Gradle plugin, and JUnit Platform engine remain standalone builds.
- Added explicit skipped/pending semantics: zero-dependency `@Skip`/`@Pending` annotations, runtime skip/pending signals and `ObjectBehavior` helpers, distinct `PENDING` counts/status, pending-aware formatter summaries, JSON reports, JUnit XML-compatible `<skipped>` mapping, Maven/Gradle report behavior, and JUnit Platform skipped-event mapping.
- Added Phase 23 classpath/execution availability diagnostics: enriched not-executable reasons, deterministic `RunDiagnostics.executionAvailabilityLines(RunResult)` helper lines, CLI `Execution diagnostics:` output, and Maven/Gradle `javaspec:` warning diagnostics with classpath element counts, without adding source compilation or changing exit/build-failure semantics.
- Added Phase 24 configuration-level report destinations: top-level JSON/JUnit XML-compatible report path aliases in config, `run --config` default report writing, CLI and explicit Maven/Gradle adapter override precedence, verbose effective report path display, and unchanged report schemas/writers/exit/build semantics.
- Added Phase 25 ServiceLoader external formatter/extension discovery: zero-dependency `JavaspecExtensionLoader` helpers, `RunFormatter`/`JavaspecExtension`/alias `Extension` service types, CLI and Gradle custom formatter selection from effective run classloaders, duplicate extension de-duplication, clear `ExtensionLoadingException` diagnostics, and no report schema/content, compilation, dependency, publishing, or Maven plugin formatter-control changes.

## 0.1.0-SNAPSHOT

- Implemented the Java 8-compatible, zero-runtime-dependency javaspec core with CLI describe/run workflows, configuration, discovery, generation support, matchers, interface doubles, run controls, reports, and programmatic no-JUnit invocation.
- Added standalone optional adapters for Maven, Gradle, and JUnit Platform, all outside the core runtime artifact.
- Added aggregate local/CI verification for the core and standalone adapters while preserving the zero-runtime-dependency core policy.
