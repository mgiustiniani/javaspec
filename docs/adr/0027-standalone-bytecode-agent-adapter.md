# ADR 0027 — Standalone bytecode-agent adapter for final, static, and construction doubles

## Status

Accepted

## Context

The zero-runtime-dependency core supports interface doubles through JDK proxies. ADR 0024 added the
standalone `javaspec-bytecode-doubles` adapter for non-final concrete classes through ByteBuddy
subclass generation, but subclassing cannot cover final classes, static methods, final methods, or
constructor interception.

Adding `java.lang.instrument`, ByteBuddy, or attach APIs to core would violate the Java 8
zero-runtime-dependency boundary and make ordinary javaspec execution depend on JVM instrumentation.
The advanced behavior must therefore remain opt-in and isolated.

## Decision

Provide `io.github.jvmspec:javaspec-bytecode-agent` as a standalone Java 8-compatible optional
adapter outside the root Maven reactor.

- Keep ByteBuddy and ByteBuddy Agent dependencies in this adapter only.
- Register `AgentConcreteDoubleProvider` through `ServiceLoader` as a
  `ConcreteDoubleProvider` for instrumentable concrete classes.
- Use `Doubles.concreteDouble(Class<T>)` for registered final or otherwise agent-backed concrete
  instances. Agent-backed construction requires a no-argument constructor.
- Expose explicit scoped APIs:
  - `BytecodeAgentDoubles.staticDouble(Class<T>)` for static methods;
  - `BytecodeAgentDoubles.mockConstruction(Class<T>)` for subsequently created instances.
- Require each `StaticDouble` or `ConstructionDouble` handle to be closed. Closing unregisters the
  scoped handler so later calls return to original behavior; already transformed bytecode may remain
  installed for the process lifetime.
- Obtain `Instrumentation` through dynamic ByteBuddy self-attach when permitted. Also package
  `Premain-Class` and `Agent-Class` manifest entries so environments that prohibit dynamic attach
  can start tests with `-javaagent`.
- Instrument non-private, non-abstract, non-native instance/static methods and constructors while
  excluding finalizers and type initializers.
- Reject null, primitive, array, annotation, enum, interface, and abstract-class targets.
- Keep this dependency test-scoped in consumers and preserve interface-oriented design as the
  default recommendation.

## Consequences

Positive:

- Final-class, final-method, static-method, and construction-aware doubles are available without
  changing core dependencies or ordinary execution.
- Existing `DoubleControl`, stubbing, verification, and Prophecy semantics remain reusable.
- `-javaagent` supports restricted environments where self-attach is disabled.
- Scoped registration limits interception to explicit handles or registered instances.

Trade-offs and limits:

- Instrumentation is process-global, JVM-dependent, and more invasive than interface or subclass
  doubles.
- Dynamic attach may be unavailable due to JVM or security policy; such environments must use
  `-javaagent` or avoid the adapter.
- Agent-backed final instance doubles require a no-argument constructor.
- Private, abstract, and native methods are not interception targets.
- While a static scope is active, unstubbed intercepted calls use javaspec default values rather
  than invoking original behavior; closing the scope restores fall-through behavior.
- Parallel tests must not create overlapping scopes for the same instrumented type without explicit
  coordination.

## Verification

- `javaspec-bytecode-agent` unit tests cover final-class, static, construction, provider, registry,
  instrumentation, cleanup, and manifest behavior.
- `examples/bytecode-agent-basic/` covers final-class and static-method behavior through the Maven
  adapter.
- `scripts/verify-all.sh`, `scripts/verify-examples.sh`, and
  `scripts/verify-release-dry-run.sh` include the adapter and consumer example.
- RC5 publishes signed main/source/Javadoc artifacts for this module under `io.github.jvmspec`.

Related decisions: [ADR 0002](0002-zero-runtime-dependency-policy.md),
[ADR 0007](0007-jdk-proxy-only-interface-doubles.md),
[ADR 0021](0021-stronger-interface-doubles.md), and
[ADR 0024](0024-standalone-optional-bytecode-doubles-adapter.md).
