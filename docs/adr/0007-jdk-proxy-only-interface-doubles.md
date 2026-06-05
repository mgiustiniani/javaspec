# 0007 — JDK Proxy-Only Interface Doubles

## Status

Accepted; extended by [ADR 0021](0021-stronger-interface-doubles.md) for argument matchers, throwing stubs, and answer callbacks while preserving the JDK proxy-only interface boundary.

## Context

Phase 8 adds collaborator/double support while preserving the Java 8 baseline and the zero-runtime-dependency policy. Java testing and mocking tools often use bytecode generation to double concrete classes, final classes, constructors, or static methods, but those libraries would add runtime dependencies and complicate Java 8 compatibility.

The JDK already provides `java.lang.reflect.Proxy`, which can create runtime implementations of ordinary interfaces without third-party libraries. This makes interface doubles a suitable MVP boundary for the core runtime.

## Decision

The core doubles implementation uses JDK dynamic proxies only and lives under `org.javaspec.doubles`.

The supported target is an ordinary Java interface. The API rejects unsupported targets with clear `IllegalArgumentException` diagnostics: `null`, primitives, arrays, annotations, enums, concrete classes, and final classes.

The public API exposes factory, control, history, and verification objects through `Doubles`, `InterfaceDouble`, `DoubleControl`, `MethodStub`, `CallVerifier`, and `Call`. `ObjectBehavior` exposes convenience APIs for creating doubles, inspecting controls and calls, counting calls, and asserting called/not-called/exact-count expectations.

Doubles support return-value stubbing by method name with any arguments or by method name with exact arguments. Exact argument matching supports `null` values and array content matching. Calls are recorded as immutable call snapshots and can be inspected or verified. Verification supports called, not called, called once, and exact call count checks.

`toString`, `equals`, and `hashCode` are handled deterministically by the proxy invocation handler. Unstubbed interface methods return Java defaults: primitive defaults, `null` for reference types, and no action for `void`.

The MVP intentionally does not support concrete class doubles, final class doubles, static method doubles, constructor doubles, bytecode-library integrations in core, or invocation of default interface methods. ADR 0021 later extends the interface-only boundary with argument matchers plus throwing and answer stubs without adding concrete/static/constructor/bytecode mocking.

## Consequences

Positive consequences:

- Collaborator doubles work on Java 8 without third-party runtime dependencies.
- The doubles boundary is simple, deterministic, and auditable.
- Unsupported cases fail early with clear diagnostics instead of silently relying on unavailable bytecode features.
- The API provides both direct factory/control usage and `ObjectBehavior` conveniences for specs.

Negative consequences and limitations:

- Users can double only ordinary interfaces in the core runtime.
- Projects that need concrete class, static, constructor, or final-class doubles must use future optional extensions or external tools outside the core runtime.
- In the Phase 8 MVP, argument matching was exact only and richer matchers were future work; ADR 0021 later adds explicit argument matchers inside the interface-only boundary.
- In the Phase 8 MVP, stubbing was limited to return values; ADR 0021 later adds throwing stubs and answer callbacks while sequences and broader side-effect orchestration remain future work.
- Default interface methods are not invoked by the proxy handler.

Verification:

- `mvn verify` passed with 328 tests after the Phase 8 MVP collaborators/doubles implementation.
- `mvn dependency:tree -Dscope=runtime` remained limited to `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.
