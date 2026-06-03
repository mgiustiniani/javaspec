# 9. Architecture Decisions

Architecture decisions are recorded as ADRs in `docs/adr/`.

| ADR | Decision |
|---|---|
| [ADR 0001](../adr/0001-java-8-baseline-with-lts-target-profiles.md) | Java 8 baseline with metadata-driven Java LTS target profiles |
| [ADR 0002](../adr/0002-zero-runtime-dependency-policy.md) | Zero runtime dependency policy |
| [ADR 0003](../adr/0003-course-correction-move-class-creation-suggestion-into-first-mvp.md) | Course correction: move class-creation suggestion into the first MVP |
| [ADR 0004](../adr/0004-course-correction-construction-defaults-typed-matcher-proxies-and-method-generators.md) | Course correction: construction defaults, typed matcher proxies, and method generators |
| [ADR 0005](../adr/0005-restricted-line-based-configuration-format.md) | Restricted line-based configuration format |
| [ADR 0006](../adr/0006-classpath-reflection-runner.md) | Classpath reflection runner for executable examples |
| [ADR 0007](../adr/0007-jdk-proxy-only-interface-doubles.md) | JDK proxy-only interface doubles |
| [ADR 0008](../adr/0008-run-only-controls-and-non-mutating-dry-run-planning.md) | Run-only controls and non-mutating dry-run planning |
| [ADR 0009](../adr/0009-interface-style-method-generation-and-sealed-interface-update-deferral.md) | Interface-style method generation and sealed-interface update deferral |
| [ADR 0010](../adr/0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md) | Zero-dependency formatter, reporting, and programmatic extension boundary |
| [ADR 0011](../adr/0011-optional-junit-adapter-and-canonical-javaspec-runner.md) | Optional build-tool/JUnit adapters and canonical javaspec runner |

## 9.1 Decision Coverage by Architecture Area

| Architecture area | Relevant decisions |
|---|---|
| Compatibility and target profiles | ADR 0001 |
| Dependency policy | ADR 0002 |
| PHPSpec-style describe/run generation split | ADR 0003 |
| Construction semantics, typed proxies, and method generation foundation | ADR 0004 |
| Configuration and suite naming | ADR 0005 |
| Reflection execution and runner result semantics | ADR 0006 |
| Interface doubles | ADR 0007 |
| Phase 9 run controls and dry-run behavior | ADR 0008 |
| Phase 10 interface/annotation/sealed-interface generation | ADR 0009 |
| Phase 11 formatter, JSON reporting, and programmatic extension contracts | ADR 0010 |
| Phase 14 no-JUnit invocation, explicit classpath input, JUnit XML-compatible reports, Phase 15 standalone optional Maven plugin, Phase 16 standalone optional Gradle plugin, and Phase 17 standalone optional JUnit Platform engine | ADR 0011 |

No new ADR was needed for Phase 14 because the implemented no-`System.exit` invocation API, explicit classpath input, and dependency-free JUnit XML-compatible reports follow ADR 0011's accepted canonical-runner/no-JUnit boundary. No new ADR was needed for Phase 15 because the standalone optional Maven plugin follows the same adapter boundary: it stays outside the core runtime artifact, delegates to `JavaspecLauncher`, avoids `System.exit`, and does not require JUnit in projects under test. No new ADR was needed for Phase 16 because the standalone optional Gradle plugin follows the same adapter boundary: it stays outside the core runtime artifact and root Maven module list, delegates to `JavaspecLauncher`, avoids `System.exit`, and does not require JUnit in projects under test. No new ADR was needed for Phase 17 because the standalone optional JUnit Platform engine follows the same adapter boundary: it stays outside the core runtime artifact and root Maven module list, uses JUnit Platform APIs only in the optional engine artifact, delegates to `JavaspecLauncher`, avoids `System.exit`, and does not require changes to javaspec spec authoring style. External CLI extension discovery/loading, deeper profile enforcement, bootstrap execution, pending examples, Phase 18 IDE/CI polish, and advanced doubles remain future work and require new or updated ADRs before implementation if they change the current architecture boundaries. ADR 0011 fixes the current boundary that javaspec core remains canonical and no-JUnit execution stays first-class.
