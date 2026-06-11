# ADR 0024 — Standalone optional bytecode doubles adapter for concrete-class doubles

## Status

Accepted

## Context

javaspec doubles are interface-only JDK dynamic proxies. [ADR 0007](0007-jdk-proxy-only-interface-doubles.md) established this boundary and [ADR 0021](0021-stronger-interface-doubles.md) (Phase 28) reaffirmed it while strengthening the interface doubles feature set. Both decisions sit inside the zero-runtime-dependency policy for the core artifact ([ADR 0002](0002-zero-runtime-dependency-policy.md)): concrete-class doubles require bytecode generation, and a bytecode-manipulation library cannot enter the core runtime artifact.

[ADR 0023](0023-course-correction-resolve-deferred-known-limitations.md) launched the known-limitations resolution program (Phases 30-36) but explicitly excluded concrete-class doubles pending a maintainer architectural decision among three candidate options:

- (a) keep interface-only doubles (status quo);
- (b) zero-dependency runtime subclass generation through `javax.tools` for non-final classes;
- (c) a new standalone optional adapter artifact carrying a bytecode dependency outside the core.

The repository already has an established pattern for optional capability that cannot live inside the zero-runtime-dependency core: the standalone Maven plugin, Gradle plugin, and JUnit Platform engine adapters ([ADR 0011](0011-optional-junit-adapter-and-canonical-javaspec-runner.md), [ADR 0012](0012-non-disruptive-aggregate-release-ci-verification.md)) are intentionally kept outside the root Maven reactor, carry their own isolated dependencies, and are verified through `scripts/verify-all.sh` and the aggregate CI verification.

## Decision

The maintainer selected option (c): concrete-class doubles are delivered through a new standalone optional adapter artifact, not through the core.

A new standalone optional artifact, `javaspec-bytecode-doubles` (working name, directory `javaspec-bytecode-doubles/`), provides concrete-class doubles for non-final classes. Static mocking, final mocking, and constructor mocking are explicitly out of scope initially. The adapter uses a bytecode library (candidate: ByteBuddy) as an isolated dependency of the adapter artifact only.

The artifact follows the existing standalone adapter pattern exactly (ADR 0011/0012):

- It lives outside the root Maven reactor, so repository-root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact.
- The bytecode dependency never enters the core runtime artifact.
- It is verified through `scripts/verify-all.sh` and the aggregate CI verification alongside the other standalone adapters.
- The core remains zero-runtime-dependency and interface-only by default.

The adapter plugs in through a public core extension point: a doubles provider SPI added to the core as interface-only contracts with no new dependencies. The core discovers an installed provider and delegates concrete-class double creation to it; without a provider, core doubles behavior is unchanged.

## Consequences

Positive consequences:

- Concrete-class doubles become available without violating the zero-runtime-dependency policy of [ADR 0002](0002-zero-runtime-dependency-policy.md).
- The core API stays unchanged for interface doubles; existing specs and double usage are unaffected.
- Adoption is opt-in: only projects that add the adapter artifact gain concrete-class doubles, mirroring the Maven/Gradle/JUnit Platform adapter model.
- This resolves exclusion (b) of [ADR 0023](0023-course-correction-resolve-deferred-known-limitations.md); concrete-class doubles are no longer deferred pending a maintainer decision.

Negative consequences and limitations:

- The adapter requires a third-party bytecode dependency (candidate: ByteBuddy) that must be maintained, audited, and version-managed separately from the core.
- Final classes remain unmockable without instrumentation; static, final, and constructor mocking stay out of scope initially.
- The doubles provider SPI becomes a public core extension surface that must stay stable for the adapter, increasing compatibility pressure on the core doubles contracts.

Related decisions: [ADR 0002](0002-zero-runtime-dependency-policy.md), [ADR 0007](0007-jdk-proxy-only-interface-doubles.md), [ADR 0011](0011-optional-junit-adapter-and-canonical-javaspec-runner.md), [ADR 0012](0012-non-disruptive-aggregate-release-ci-verification.md), [ADR 0021](0021-stronger-interface-doubles.md), and [ADR 0023](0023-course-correction-resolve-deferred-known-limitations.md).
