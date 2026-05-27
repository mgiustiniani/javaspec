# ADR 0003 — Course correction: move class-creation suggestion into the first MVP

## Context

The current plan deferred code generation to a later phase and treated generator work as post-MVP or later work. The initial MVP direction focused on non-generative specification checks before enabling any generation path.

## Derailment

The plan no longer matches the user's MVP priority. The user wants generation enabled from the beginning, initially only for the missing-class creation suggestion use case, while preserving PHPSpec's command responsibilities where possible.

## Required Change

The MVP must include a minimal PHPSpec-style generator flow:

1. `describe` / `desc` creates or locates only the specification skeleton for a target class-like type.
2. `describe` / `desc` must not generate the production type.
3. `run` discovers specifications and infers the class-like types they describe.
4. `run` detects when a described production type is absent.
5. `run` asks whether to create the missing type with a `Y/n` confirmation.
6. `run` can also enable class creation deliberately through a defined non-interactive policy such as `--generate`, which answers yes to the prompt.

The production-type write operation must still require explicit interactive confirmation or a defined non-interactive policy. The zero-runtime-dependency constraint and Java 8 compatibility constraint remain unchanged.

## Consequences

Implementation phases and traceability must be adjusted after this ADR. Spec skeleton generation and the production-type generation prompt move into the first MVP, while broader generator capabilities remain later work.

Tests should cover PHPSpec-style spec skeleton creation, existing spec non-overwrite behavior, class-exists behavior during `run`, and class-missing prompt behavior during `run`, including yes/no/default answers and the boundary where production files are not written unless confirmation or policy allows it.
