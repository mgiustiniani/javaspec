# javaspec ARC42 architecture documentation

These documents describe the 1.0 architecture and preserve historical phase context where it helps
explain a decision. The current runtime baseline is Java 8-compatible, zero-runtime-dependency core
plus standalone optional Maven, Gradle, JUnit Platform, bytecode-doubles, and bytecode-agent
adapters.

## Current release note

`1.0.0-RC5` Maven artifacts are available and verified. The Gradle plugin id is
`io.github.jvmspec`, but RC5 first-publication approval and public marker resolution are still
pending. Post-RC5 develop commit `67db10c` resolves the observed Javadoc reproducibility mismatch.
Consult the [release checklist](../release-1.0-checklist.md) and
[RC evidence](../release-1.0-rc-evidence.md) for authoritative current status; older phase-specific
publication statements are historical rather than release evidence.

## Sections

1. [Introduction and goals](01-introduction-and-goals.md)
2. [Constraints](02-constraints.md)
3. [Context and scope](03-context-and-scope.md)
4. [Solution strategy](04-solution-strategy.md)
5. [Building block view](05-building-block-view.md)
6. [Runtime view](06-runtime-view.md)
7. [Deployment view](07-deployment-view.md)
8. [Concepts](08-concepts.md)
9. [Architecture decisions](09-architecture-decisions.md)
10. [Quality requirements](10-quality-requirements.md)
11. [Risks and technical debt](11-risks-and-technical-debt.md)
12. [Glossary](12-glossary.md)

Accepted decisions are stored under [`../adr/`](../adr/). The current optional instrumentation
boundary is recorded in [ADR 0027](../adr/0027-standalone-bytecode-agent-adapter.md). Stable public
contracts are indexed by the main [documentation index](../README.md).
