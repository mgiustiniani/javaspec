# Compatibility policy — 1.0

This policy applies to the javaspec 1.0 line.

## Version line

The RC/final flow is:

```text
1.0.0-SNAPSHOT -> 1.0.0-RC1 -> 1.0.0
```

After final release, development moves to the next snapshot.

## Stable surfaces

Stable 1.0 surfaces are the packages/artifacts classified in `docs/api-surface-1.0.md` and the
contract documents referenced from that file:

- PHPSpec-first authoring API;
- runner/result/report contracts;
- matcher, Prophecy, example-data, generation, extension SPI, and JUnit Platform contracts;
- Maven/Gradle/JUnit Platform adapter user-facing parameters and behavior;
- generated source shapes documented as generated API.

Breaking changes to `PUBLIC_API`, `PUBLIC_SPI`, `ADAPTER_API`, or `GENERATED_API` require a new major
version unless this repository explicitly marks a narrower experimental/preview surface.

## Allowed compatible changes

Patch/minor releases may add:

- new matcher helpers;
- new report fields under an existing schema version when consumers can ignore them;
- new optional adapters or adapter options;
- new generated helper methods when they do not change existing generated signatures;
- diagnostics improvements that preserve documented key phrases where tests depend on them.

## Deprecation

When removal or semantic change is necessary, the preferred path is:

1. document the replacement;
2. keep the old behavior with a deprecation notice for at least one minor release when feasible;
3. remove or break only in a major release.

## Deferred 1.0 limits

Documented deferrals, such as typed event model v2, configuration-file custom matcher registration,
Cucumber/Gherkin support, optional extra reporters, and broader performance/adoption case studies, do
not reduce the 1.0 compatibility promise. They may be added later as additive features.

## Reports and result schema

JSON report schema version 1 evolves additively. Existing fields and status names remain stable for
1.x unless a new schema version is introduced. JUnit XML remains a CI projection of javaspec results,
not a new semantic source.

## Core dependency policy

The core artifact remains zero-runtime-dependency. New dependency-heavy features must live in optional
artifacts unless a future major version explicitly changes the policy.
