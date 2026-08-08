# ADR 0026: Internal language seams before 1.0

- Status: Accepted
- Date: 2026-07-13

## Context

javaspec discovery and generation were implemented directly as Java source operations. Constructor
hardening exposed duplicated source/type normalization and a large orchestration boundary. Future
language-specific products may need to author specifications and generate production source in the
same language, while later allowing combinations such as Kotlin specifications generating Java
production or Java specifications generating Kotlin production.

The 1.0 release must remain Java 8-compatible, zero-runtime-dependency, PHPSpec-first, and stable.
No additional language implementation or public language SPI is required for 1.0.

## Decision

Before 1.0, introduce an internal, orthogonal seam:

```text
SpecLanguageFrontend -> BehaviorContract -> ProductionLanguageBackend
```

Only `JavaSpecLanguageFrontend` and `JavaProductionLanguageBackend` are registered. The existing
public `DescribedType` model remains unchanged and is retained as a Java compatibility bridge by the
internal `BehaviorContract`. The contract separately projects portable subject shape, relationships,
structured types, construction signatures, callable signatures, invocation kind, and unknown-type
evidence without changing the frozen public API. Language-specific values/bodies remain deferred
until a real post-1.0 adapter proves the required representation.

Production synchronization is planned entirely in memory by the selected backend and still passes
through the single CLI authorization and atomic-write boundary. Language selection is not exposed
through CLI/configuration, no ServiceLoader language SPI is published, and generated Java output
must remain behaviorally and byte-for-byte compatible where existing tests freeze it.

Actual Kotlin or other JVM-language support is deferred until after 1.0. The first post-1.0 language
slice should validate Kotlin-specification to Kotlin-production generation before stabilizing an
external SPI; cross-language combinations must exchange structured portable behavior rather than
pairwise source translators.

## Consequences

- Java discovery and generation gain explicit replaceable boundaries without changing user behavior.
- The core tool remains implemented in Java and retains its dependency policy.
- Additional languages can later be implemented with N frontend/backend adapters rather than N×N
  translators.
- Raw language-specific expressions and bodies are not automatically portable; future backends must
  fail closed when the behavior contract cannot represent them safely.
- The internal seam may change before it is validated by a second language and therefore carries no
  SemVer compatibility promise.

## Verification

- Existing Java unit, CLI, language-coverage, adapter, and consumer suites remain green.
- The supported public API classifications and runtime dependency checks remain unchanged; the exhaustive signature inventory may receive additive `INTERNAL` entries before the next RC.
- Unauthorized and dry-run source synchronization remain non-mutating.
- Existing generated-source hashes and idempotence expectations remain unchanged.
