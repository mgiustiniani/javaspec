# JUnit Platform adapter contract — 1.0

`javaspec-junit-platform-engine` is an optional adapter. It preserves javaspec subject-centric
semantics for IDEs, Maven Surefire, Gradle Test, and the JUnit Platform Launcher. It does not turn
javaspec into Jupiter and does not require Jupiter annotations or lifecycle callbacks.

## Engine identity and dependency boundary

- Engine id: `javaspec`.
- Artifact: `io.github.jvmspec:javaspec-junit-platform-engine`.
- The core `io.github.jvmspec:javaspec` artifact has no JUnit Platform dependency.
- The engine delegates execution to the canonical `JavaspecLauncher` and never calls `System.exit` or
  the CLI entrypoint.

## Configuration parameters

The engine reads javaspec configuration from Launcher configuration parameters and optional config
files. Important public parameters include:

- `javaspec.specRoot`: root directory for source-based spec discovery;
- `javaspec.suite`: selected configured suite;
- `javaspec.config`: config file path;
- `javaspec.formatter`: formatter name;
- `javaspec.classpath` / classpath-file related settings where supported by the engine settings;
- `javaspec.bootstrapDiscovery`: opt-in ServiceLoader bootstrap discovery;
- configured extension names through the javaspec config model.

Invalid configuration fails discovery/execution with a JUnit Platform exception that names the
invalid javaspec setting.

## Discovery and selectors

Selectors are filters over canonical javaspec discovery results:

- class selectors select matching spec classes;
- method selectors select matching `it_*` / `its_*` example methods;
- package selectors select specs in matching packages;
- unique-id selectors select engine/spec/example/row descriptors by stable javaspec ids.

Selectors do not invent Jupiter test classes, test templates, or dynamic containers. Source-only
specs that are discovered but not loadable on the run classpath are reported as skipped with a
classpath/compilation diagnostic rather than as silent absence.

## Descriptor hierarchy and unique IDs

The stable 1.0 descriptor hierarchy is:

```text
[engine:javaspec]
  [spec:<specQualifiedName>]
    [example:<exampleMethodName>]
      [row:<oneBasedRowIndex>]   # dynamic row descriptors only when row results are recorded
```

Example display names use the stable javaspec example id:

```text
<specQualifiedName>#<exampleMethodName>
```

Spec descriptors use `ClassSource`; example descriptors use `MethodSource`; row descriptors use the
owning example `MethodSource`.

## Example data rows

Rows are adapter projections of javaspec example-data results, not isolated Jupiter parameterized
invocations. See `docs/example-data-contract-1.0.md` for the authoritative row contract.

In short:

- row descriptors are registered dynamically during execution from recorded row results;
- row unique-id selectors select the owning example for execution;
- row unique-id selectors filter row descriptor/event publication;
- row selectors do not isolate execution to a single row.

## Status mapping

javaspec results map to JUnit Platform events as follows:

| javaspec status | JUnit Platform event |
|---|---|
| `PASSED` | `executionFinished(..., successful())` |
| `FAILED` | `executionFinished(..., failed(JavaspecAssertionFailure))` |
| `BROKEN` | `executionFinished(..., failed(JavaspecBrokenExampleException))` |
| `SKIPPED` | `executionSkipped(...)` |
| `PENDING` | `executionSkipped(...)` with pending reason |

Stop-on-failure skip propagation is reported as skipped events with stop-on-failure diagnostics.
Generated incomplete stubs remain javaspec `BROKEN` results, not JUnit assumptions.

## Discovery-only behavior

Discovery must be side-effect-light and must not execute examples. Dynamic row descriptors are not
known until execution because row results are produced by running the owning example body. Consumers
that perform discovery only should expect suite/spec/example descriptors, not row descriptors.

## IDE workflow

For IntelliJ, Maven Surefire, Gradle Test, and the JUnit Platform Console Launcher:

1. Put compiled spec/test classes and `javaspec-junit-platform-engine` on the test runtime classpath.
2. Configure `javaspec.specRoot` when source discovery should use a non-default root.
3. Use class/method/package/unique-id filters normally; they remain filters over javaspec specs.
4. Do not add Jupiter annotations to javaspec specs. Public `it_*` / `its_*` methods and
   `ObjectBehavior` lifecycle remain the authoring model.

## Boundaries

Out of scope for 1.0:

- Jupiter lifecycle or annotation semantics in javaspec specs;
- Jupiter parameterized/test-template row isolation;
- per-row discovery-only descriptors;
- JUnit-specific result states that do not map to javaspec `PASSED`/`FAILED`/`BROKEN`/`SKIPPED`/`PENDING`;
- core runtime dependency on JUnit Platform APIs.
