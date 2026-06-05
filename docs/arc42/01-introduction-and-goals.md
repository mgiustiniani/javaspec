# 1. Introduction and Goals

## 1.1 Requirements Overview

javaspec is a Java 8-compatible, zero-runtime-dependency specification framework inspired by phpspec. It supports an implemented first-MVP specification/generation slice, reflection runner, matcher expansion, MVP interface doubles, Phase 9 run controls, Phase 10 interface-style method generation, Phase 11 formatter/reporting/programmatic extension contracts, the Phase 14 no-JUnit integration foundation for programmatic invocation, explicit classpath input, and JUnit XML-compatible reports, Phase 15 Maven, Phase 16 Gradle, and Phase 17 JUnit Platform standalone optional adapters, Phase 18 stable identifier/source-location/report polish, Phase 19 aggregate release/CI verification, Phase 20 release-readiness scaffolding, Phase 21 standalone adoption examples plus report schema/golden documentation, Phase 22 explicit skipped/pending semantics, Phase 23 classpath/execution availability diagnostics, Phase 24 configuration-level report destinations, Phase 25 ServiceLoader external formatter/extension discovery, Phase 26 target-profile enforcement before generation/update writes, and Phase 27 bootstrap hook execution before examples. Future backlog work can grow the specification-first workflow further while preserving the conservative compatibility baseline.

Core requirements:

- Compile and run on Java 8.
- Use Maven and package base `org.javaspec`.
- Keep the core runtime artifact free of third-party runtime dependencies beyond the JDK.
- Allow dependencies only in test scope.
- Model Java LTS target profiles for Java 8, 11, 17, 21, and 25 through a profile catalog, API-symbol metadata, compatibility checks, reflection-only probes, and conservative pre-write profile enforcement.
- Avoid direct production-code references to APIs unavailable on Java 8.
- Support zero-runtime-dependency line-based configuration with suite-level spec/source directories, package-prefix naming, selected-suite discovery, executable top-level and selected-suite bootstrap hooks, and optional top-level JSON/JUnit XML-compatible report destinations.
- Execute discovered examples through a Java 8-compatible reflection runner when compiled spec classes are available on the effective or selected explicit classloader, with optional stop-on-failure and explicit skipped/pending semantics.
- Support zero-runtime-dependency interface doubles using JDK dynamic proxies, with explicit limitations for unsupported concrete/static/constructor scenarios.
- Support run-only CLI controls for dry-run planning, stop-on-failure, progress/pretty output, profile selection/enforcement, verbose diagnostics, explicit classpath input, execution-availability diagnostics, bootstrap execution immediately before examples, optional JSON reports, optional JUnit XML-compatible reports, stable identifiers, separate pending counts, and source metadata where available.
- Expose no-`System.exit` invocation, formatter/reporting, minimal extension contracts, and zero-dependency ServiceLoader discovery for external run formatter/extension providers on CLI and Gradle run classloaders.
- Provide Maven, Gradle, and JUnit Platform integrations only as standalone optional adapters over the canonical runner, not as core runtime dependencies.
- Keep repository-root Maven verification core-only while providing explicit aggregate release/CI verification, release-readiness checks, and standalone adoption examples verification for standalone adapters.
- Use phpspec as the functional inspiration for CLI, discovery, lifecycle, expectations, doubles, generation, reporting, and extension concepts.

## 1.2 Quality Goals

| Priority | Quality goal | Rationale |
|---|---|---|
| 1 | Java 8 compatibility | The binary must run in older Java environments and must not link against newer APIs. |
| 2 | Zero runtime dependencies | Users should be able to add javaspec without dependency conflicts or transitive runtime cost. |
| 3 | Clear specification workflow | The project should preserve the productive phpspec style while adapting to Java's static type system. |
| 4 | Deterministic execution | Discovery, ordering, results, and exit codes must be stable for CI and repeatable local runs. |
| 5 | Extensibility | Users should be able to add matchers, generators, formatters, and integrations without bloating the core runtime. |
| 6 | LTS-awareness | The framework should understand modern JDK capabilities without breaking Java 8 runtime compatibility. |

## 1.3 Stakeholders

| Stakeholder | Interest |
|---|---|
| Java developers | Specification-first design and readable behavioral tests. |
| Library maintainers | Minimal dependency footprint and compatibility with older runtime environments. |
| Build/CI maintainers | Stable CLI behavior, exit codes, and compatibility across JDK versions. |
| Extension authors | Clear contracts for matchers, formatters, generators, and integrations. |
| Project maintainers | Strict compatibility rules, documented decisions, and traceable implementation phases. |

## 1.4 Current Non-Goals and Limits

- No third-party runtime dependencies in the core artifact.
- No in-process source/spec compilation by the CLI runner, invocation API, or optional adapters; source-only specs are skipped until compiled classes are available, with Phase 23 diagnostics to explain availability problems. Optional Maven, Gradle, and JUnit Platform adapters supply host classpath integration but do not change core compilation ownership.
- Profile enforcement is conservative and generation-scoped: `run` checks described type kinds and resolvable cataloged Java API signature owners before writes, but it is not an integrated compiler and ignores unknown project types plus ambiguous or unresolvable type names.
- No configuration-driven extension activation, package scanning, plugin repository lookup, Maven plugin formatter controls, or JUnit Platform formatter controls; Phase 25 ServiceLoader discovery is limited to formatter/extension providers available on the effective CLI or Gradle run classloader. Bootstrap hooks are explicit compiled class names only; no ServiceLoader hook discovery, script engine, package scanning, integrated compilation, or dependency resolution is added.
- No existing sealed-interface source updates until nested permitted implementations can be updated source-preservingly.
- No Maven multi-module conversion, runtime dependency additions, publishing, signing, secrets, portal credentials, final release tag/version, or final publish approval in the Phase 20 release-readiness or Phase 21 adoption-assets increments; the MIT license and confirmed maintainer metadata are resolved.
- Phase 21 examples are standalone consumer projects, not root modules or public-publication evidence.
- Phase 20, Phase 21, and Phase 22 have local verification evidence plus user-/maintainer-confirmed remote GitHub Actions success for HEAD `5088e96` on `develop` after push; no GitHub run IDs, URLs, durations, or logs were independently queried.
- Phase 19 remote GitHub Actions success remains historical user-/maintainer-confirmed evidence for HEAD `4d30e63` on `develop`.
- No C4 diagrams are currently generated.
