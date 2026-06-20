# Release notes — 0.1.1-SNAPSHOT / Unreleased

This development line keeps the core artifact Java 8-compatible and zero-runtime-dependency while
moving dependency-heavy behavior into optional artifacts.

## Core `io.github.jvmspec:javaspec`

- Generated support and Prophecy wrapper sources are written to `target/generated-sources/javaspec`
  by default, not `src/`.
- `javaspec prophesize <Class>` supports interfaces and concrete/final classes. Generated wrappers
  extend `ObjectProphecy<T>` and expose typed `MethodProphecy` methods.
- `javaspec run --generate` can generate missing Prophecy wrappers and update the generated
  `*SpecSupport` class with typed helpers such as `prophesizeMailer()` / `prophecyMailer()`.
- Prophecy authoring styles:
  - Java 8+: `MailerProphecy mailer = prophesizeMailer();`
  - Java 10+: `var mailer = prophesizeMailer();`
- The standalone `Prophecies` factory makes Prophecy runtime usage independent of `ObjectBehavior`;
  `ObjectBehavior` remains only the spec-author convenience base.
- Interface doubles now support default interface methods, sequential returns, callback sequences,
  captors, ordered verification, and return-then-throw stubbing.
- CLI compilation gained local-POM dependency resolution (`--resolve-pom`), `--release <N>`, and
  incremental cache hits for unchanged inputs.
- Extension discovery includes `list-extensions` and classpath repair hints.
- Parser/updater generation is backed by a parser SPI and ignores signatures inside comments and
  strings.
- `Doubles.controlFromHandler(...)` was added as a public adapter hook so optional adapters do not
  depend on package-private core implementation classes across build-tool classloaders.

## Optional subclass adapter `javaspec-bytecode-doubles`

- Remains the lightweight ByteBuddy subclass adapter for non-final concrete classes.
- Capability diagnostics clearly explain unsupported targets such as final classes, arrays,
  primitives, annotations, enums, and interfaces.

## Optional agent adapter `javaspec-bytecode-agent`

- Adds instrumentation-backed concrete doubles for final classes.
- Adds scoped static-method doubles via `BytecodeAgentDoubles.staticDouble(...)`.
- Adds construction-aware doubles via `BytecodeAgentDoubles.mockConstruction(...)`.
- Dynamic self-attach uses ByteBuddy Agent when the JVM allows it; `-javaagent` remains available for
  stricter environments.
- A Maven example is available at `examples/bytecode-agent-basic/`.

## Verification and release notes

- Version alignment checks cover core, Maven plugin, Gradle plugin, JUnit Platform engine,
  `javaspec-bytecode-doubles`, and `javaspec-bytecode-agent`.
- `scripts/verify-examples.sh` verifies Maven, Prophecy, bytecode-doubles, bytecode-agent,
  JUnit Platform, and Gradle examples. Use `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` when no Gradle
  executable is available locally.
- `scripts/verify-all.sh` remains the full aggregate check. This repository intentionally does not
  vendor a Gradle Wrapper in this development line; set `JAVASPEC_GRADLE_BIN` to a local Gradle
  executable or `JAVASPEC_SKIP_GRADLE=1` when verifying without Gradle.
