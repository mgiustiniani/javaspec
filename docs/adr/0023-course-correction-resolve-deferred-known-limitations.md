# ADR 0023 — Course correction: resolve intentionally deferred known limitations

## Context

Phases 2 through 29 are complete. The README documents a "Known limitations" list, and several of those limitations are intentional scope deferrals backed by accepted decisions rather than accidental gaps:

1. Doubles are interface-only JDK dynamic proxies; concrete-class doubles are out of scope ([ADR 0007](0007-jdk-proxy-only-interface-doubles.md), reaffirmed by [ADR 0021](0021-stronger-interface-doubles.md)).
2. Existing sealed-interface source updates are intentionally deferred; missing generated methods are skipped for existing sealed-interface sources ([ADR 0009](0009-interface-style-method-generation-and-sealed-interface-update-deferral.md)).
3. Phase 25 intentionally excluded configuration-driven extension activation, Maven plugin formatter output controls, and JUnit Platform formatter output controls ([ADR 0018](0018-serviceloader-external-formatter-extension-discovery.md)).
4. Phase 27 intentionally excluded ServiceLoader discovery of bootstrap hooks; only explicitly configured hook classes execute ([ADR 0020](0020-bootstrap-hook-execution.md)).
5. Phase 29 opt-in source/spec compilation is CLI-only; programmatic invocation and the Maven/Gradle adapters gained no compilation behavior ([ADR 0022](0022-opt-in-cli-source-spec-compilation.md)).
6. JUnit XML-compatible reporting remains intentionally minimal ([ADR 0010](0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md), extended by Phases 14/18/22).
7. Generic `Iterable` count/empty matcher checks consume the iterable fully and can hang on infinite iterables.

These deferrals were valid scope fences under the original phased direction. All of them sit inside the two non-negotiable project constraints: Java 8 source/target compatibility ([ADR 0001](0001-java-8-baseline-with-lts-target-profiles.md)) and zero runtime dependencies in the core artifact ([ADR 0002](0002-zero-runtime-dependency-policy.md)).

## Derailment

The maintainer direction has changed: the documented known limitations must now be resolved. Keeping them as permanent scope fences conflicts with this new requirement.

The symptom is an explicit maintainer instruction: "the known limitations must be resolved". The existing plan ends at Phase 29 and contains no work that resolves the deferred limitations, so continuing on the previous direction would leave the project permanently out of line with the maintainer requirement.

## Required Change

A phased resolution program (Phases 30 through 36, detailed in `PLAN.md`) must resolve every known limitation that can be resolved within the two non-negotiable constraints (Java 8 source/target compatibility; zero runtime dependencies in the core artifact):

1. Phase 30 — Bounded `Iterable` matcher checks: empty/non-empty checks on a generic `Iterable` use `iterator().hasNext()` instead of full consumption, and count checks iterate at most `expected + 1` elements with a fail-fast "more than `<expected>` elements" style message, so infinite iterables never hang.
2. Phase 31 — Sealed-interface source updates: source-preserving updates of existing sealed interfaces (generated method declarations in the sealed root plus updates of nested permitted implementations), superseding the deferral half of ADR 0009.
3. Phase 32 — Configuration-driven extension activation and adapter formatter controls: config keys for activating discovered extensions/formatters, Maven plugin formatter output controls, and JUnit Platform engine formatter configuration parameters.
4. Phase 33 — Optional ServiceLoader discovery of `io.github.jvmspec.bootstrap.BootstrapHook` providers in addition to explicitly configured hooks, with deterministic ordering and zero new dependencies.
5. Phase 34 — Extending the Phase 29 `javax.tools` current-JDK opt-in compilation to `JavaspecInvocation`/`JavaspecLauncher` programmatic runs and to the Maven and Gradle adapters as explicit opt-in settings.
6. Phase 35 — Additive report enrichment for the dependency-free JUnit XML-compatible writer and additive JSON report fields while remaining schemaVersion 1-compatible.
7. Phase 36 — Deeper target-profile enforcement with broader catalog coverage and enforcement signature resolution, while remaining source/generation-scoped.

Two limitations are explicitly EXCLUDED from the resolution program:

1. **Public publication is complete.** Artifacts are published on Maven Central under
   `io.github.jvmspec`. The Gradle plugin is published on the Gradle Plugin Portal with plugin id
   `io.github.jvmspec`.
2. **Concrete-class doubles remain deferred pending a maintainer architectural decision.** Adding a bytecode-manipulation library to the core would violate the zero-runtime-dependency policy ([ADR 0002](0002-zero-runtime-dependency-policy.md), [ADR 0007](0007-jdk-proxy-only-interface-doubles.md)). The candidate options are listed here without choosing one:
   - keep interface-only doubles (status quo);
   - zero-dependency runtime subclass generation through `javax.tools` for non-final classes;
   - a new standalone optional adapter artifact carrying a bytecode dependency outside the core.

## Consequences

- Phase 31 will supersede the deferral half of [ADR 0009](0009-interface-style-method-generation-and-sealed-interface-update-deferral.md); this is a forward reference recorded here, and ADR 0009 is not edited yet. The interface-style and annotation generation decisions in ADR 0009 remain in force.
- Phases 32 through 34 deliberately relax the Phase 25, Phase 27, and Phase 29 scope fences recorded in [ADR 0018](0018-serviceloader-external-formatter-extension-discovery.md), [ADR 0020](0020-bootstrap-hook-execution.md), and [ADR 0022](0022-opt-in-cli-source-spec-compilation.md).
- Phase 35 adds only additive report fields: JSON reports stay schemaVersion 1-compatible through additive fields, and the JUnit XML-compatible writer remains dependency-free.
- All phases preserve the Java 8 source/target compatibility gate, the zero-runtime-dependency gate for the core artifact, and the non-multi-module repository layout: root `mvn verify` remains core-only and standalone adapters stay outside the root Maven reactor.
- Previously documented "intentionally excluded" statements in earlier phases and ADRs become historical scope records rather than permanent constraints; affected README/ARC42 wording will be synchronized in the finalization documentation phase after implementation.
- The two excluded limitations remain documented known limitations until the maintainer supplies publication inputs or decides the concrete-class doubles architecture.

Related decisions: [ADR 0001](0001-java-8-baseline-with-lts-target-profiles.md), [ADR 0002](0002-zero-runtime-dependency-policy.md), [ADR 0007](0007-jdk-proxy-only-interface-doubles.md), [ADR 0009](0009-interface-style-method-generation-and-sealed-interface-update-deferral.md), [ADR 0010](0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md), [ADR 0013](0013-release-readiness-scaffolding-with-publication-blockers.md), [ADR 0018](0018-serviceloader-external-formatter-extension-discovery.md), [ADR 0019](0019-deep-target-profile-enforcement.md), [ADR 0020](0020-bootstrap-hook-execution.md), [ADR 0021](0021-stronger-interface-doubles.md), and [ADR 0022](0022-opt-in-cli-source-spec-compilation.md).
