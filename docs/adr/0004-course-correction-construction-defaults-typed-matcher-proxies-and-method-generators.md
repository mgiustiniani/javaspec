# ADR 0004 — Course correction: construction defaults, typed matcher proxies, and method generators

## Context

Construction and matcher support were already implemented as part of the current generator and specification workflow. The implemented work covers class/type skeleton generation and constructor generation, and it introduced matcher support that can be used by specifications.

A review found that parts of this implementation do not match the desired PHPSpec-like behavior for JavaSpec. The user has now clarified the required behavior for constructor defaults, matcher syntax, and generator coverage.

## Derailment

The implementation diverged from the clarified direction in these areas:

1. Constructor handling does not use the desired default policy. The desired default is the `comment` policy.
2. Matcher syntax currently relies on a registry/wrapper API instead of typed generated proxies that can preserve syntax as close as possible to PHPSpec.
3. Generation currently covers class/type skeletons and constructors, but it does not cover method generators.
4. Documentation, especially the user manual, must remain synchronized with the supported construction, matcher, and generator behavior.

## Required Change

The correction must make these changes:

1. Constructor policy states must be exactly `delete`, `preserve`, and `comment`.
2. The default constructor policy must be `comment`.
3. Matcher handling must be designed around generated typed proxies so specifications can use syntax as close as possible to PHPSpec while remaining Java-compatible.
4. Method generators must be added in addition to the existing class/type skeleton and constructor generators.
5. `docs/usermanual` must be updated as part of the change so user-facing behavior stays current.
6. Source and web verification of PHPSpec construction and matcher APIs must be retried before final correction planning.

## Consequences

The correction requires a follow-up `PLAN.md` update after this ADR is recorded. Implementation must be delegated to the appropriate Java agents rather than performed in this documentation step.

Tests must be added or updated for:

- the constructor policy state set and the `comment` default;
- typed generated matcher proxies and the intended PHPSpec-like matcher syntax;
- method generation behavior alongside class/type skeleton and constructor generation;
- user-manual examples that describe the corrected behavior.

Existing construction and matcher implementation may need to be refactored or replaced where it conflicts with the typed-proxy and constructor-policy requirements. Documentation updates must remain part of the correction, not a deferred cleanup task.
