# ADR 0028: Project-specific Native Image preview

- Status: Accepted — experimental post-RC5 increment
- Date: 2026-08-09

## Context

JavaSpec is distributed as Java 8-compatible JVM bytecode. Its normal runner discovers source,
loads compiled consumer classes through class loaders, optionally invokes the JDK compiler, and can
activate ServiceLoader/bytecode integrations. GraalVM Native Image instead applies a closed-world
assumption: code must be available while the machine executable is built, and reflection requires
reachability metadata.

A generic native JavaSpec executable built at framework release time cannot retain the JVM CLI's
ability to load arbitrary future consumer `.class` files. A native front-end that merely launches
`java` would not satisfy the requested JVM-free execution path. Embedding a JVM/interpreter would be
a materially different product and deployment model.

A GraalVM 25.0.3 probe confirmed the boundary: a JVM `URLClassLoader` loaded a class compiled after
the launcher, while the native launcher returned `ClassNotFoundException` for the same post-build
class.

## Decision

Provide a **project-specific executable** prepared after consumer source generation and
`testCompile`:

1. Add zero-dependency core API `io.github.jvmspec.nativeimage.NativeImageLauncher`.
2. Add Maven goal `javaspec:native-prepare` to discover spec source during
   `generate-test-sources`.
3. Generate `io.github.jvmspec.generated.JavaspecNativeMain` with class literals for every selected
   spec and described subject plus deterministic example/source metadata.
4. Generate `META-INF/native-image/.../reflect-config.json` for spec construction/public methods and
   subject constructors/methods.
5. Let official GraalVM Native Build Tools consume the project test classpath and create the native
   executable. JavaSpec does not shell out to or reimplement `native-image` inside its Maven plugin.
6. Keep the first executable option set closed and explicit: built-in console formatters,
   stop-on-failure, prediction checking toggle, help, and process exit mapping.
7. Reject default-package types and preparation without examples before generating a misleading
   build.
8. Treat native support as a post-RC5 preview until its capability slices are separately qualified.

The core remains Java 8-compatible and retains zero third-party runtime dependencies. No sixth
published JavaSpec artifact is introduced; the consumer's executable is an output of its own build.

## Initial capability boundary

Qualified initially:

- build-linked named-package specs and subjects;
- built-in matchers and subject lifecycle, including constructor arguments;
- `let` / `letGo`;
- passed, failed, broken, skipped, and pending outcomes;
- pretty/progress/JSON console output and deterministic exit codes;
- Linux x86-64 GraalVM Native Image 25 build/replay.

Not yet qualified:

- runtime discovery, compiler/generation/update operations, or new classpath entries;
- configuration suites/filters, report files, hooks, resolver, or ServiceLoader extensions;
- dynamic proxies/Prophecy and both ByteBuddy adapters;
- Gradle/JUnit Platform native paths;
- other operating systems/architectures or cross-build binary reproducibility.

Unsupported native options fail with usage exit `64`; they are not accepted and ignored.

## Consequences

### Positive

- The produced test executable runs without a JVM process.
- The build includes the actual consumer classes rather than pretending a release-time binary can
  discover future bytecode.
- Generated inputs are inspectable, deterministic, and covered by JVM unit tests.
- Native runtime behavior reuses the existing runner/result/formatter semantics.
- Native-specific dependencies remain outside the core artifact.

### Negative

- Every project/suite needs its own native-image build.
- Native compilation adds toolchain, time, memory, and platform requirements.
- Broad reflection metadata favors correctness over minimum first-preview image size.
- Runtime source/classpath flexibility is intentionally absent.
- A feature being supported on the JVM does not imply native support; each dynamic capability needs
  reachability design and native evidence.

## Alternatives considered

### Publish one universal native JavaSpec CLI

Rejected for the standard Native Image architecture because future consumer bytecode is not part of
the release-time closed world.

### Spawn an external JVM

Rejected as the native execution definition. It remains a possible launcher optimization but is not
JVM-free testing.

### Embed Espresso or another JVM/interpreter

Deferred. It could restore dynamic bytecode loading but changes size, startup, distribution,
security, and operational semantics substantially.

### Generate direct method calls instead of reflection metadata

Deferred. Direct adapters could reduce metadata and image size, but would duplicate lifecycle,
parameter injection, annotation, and result semantics. The first increment reuses the qualified
runner and makes reflection inputs explicit.

## Verification

- `NativeImageLauncherTest`
- `JavaspecNativePrepareMojoTest`
- `scripts/verify-native-example.sh`
- `examples/native-basic/`
- GitHub Actions job `Native executable / GraalVM 25`

## Follow-up decisions

Report destinations/filters, proxy metadata, Gradle parity, platform expansion, and instrumentation
must be implemented and evidenced as independent increments. The documentation must continue to
distinguish the immutable RC5 feature set from this `develop` preview until a later release is
published.
