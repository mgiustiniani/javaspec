# 7. Deployment View

## 7.1 Runtime Artifact

javaspec is packaged as a Maven-built Java artifact with CLI main class `org.javaspec.cli.Main`. The current repository build produces `target/javaspec-0.1.0-SNAPSHOT.jar`.

The runtime artifact is intentionally small:

- Java 8 source/target compatibility.
- No third-party runtime dependencies.
- Package base `org.javaspec`.
- JDK APIs only at runtime.
- Post-Java-8 capabilities represented as metadata, source text, or reflection-only probes.

## 7.2 Deployment Environments

| Environment | Usage |
|---|---|
| Developer workstation | Run `describe` and `run`, review generated source, execute already-compiled specs, write reports when requested. |
| Build tool or IDE classpath | Supplies compiled production/spec classes to the reflection runner. The CLI does not compile source/spec files itself. |
| CI pipeline | Runs `mvn verify`, dependency audits, optional `javaspec run --dry-run`, optional `javaspec run --report <file>`, and consumes exit codes. |
| Java 8 runtime | Compatibility floor for the production binary. |
| Java 11/17/21/25 runtimes | Supported target/runtime matrix entries; newer APIs remain metadata/reflection-only unless generated source is compiled by a suitable JDK. |

## 7.3 Build and Verification Commands

Repository verification uses Maven:

```sh
mvn verify
mvn dependency:tree -Dscope=runtime
```

Phase 12 used Distrobox/Podman containers for Java 8, 11, 17, 21, and 25. Each container ran `mvn clean` and `mvn verify` with 364 tests, 0 failures, 0 errors, and 0 skipped. The Java 25 container also passed the runtime dependency audit and Java 25 Gatherer reflection probe. See [Test and Quality Report](../test-report.md).

## 7.4 Classpath Requirements for Execution

`javaspec run` discovers source files but executes examples only when compiled spec classes are available on the effective classloader.

Consequences for deployment:

- A source-only spec tree can be discovered but its examples are marked `SKIPPED`.
- Users should compile the project or run javaspec from a launcher/classpath that includes compiled spec and production classes when they expect execution.
- Build systems may run javaspec after compilation or use `--dry-run` earlier to detect pending generated work without requiring compiled specs.

## 7.5 Generated Source Deployment

The javaspec binary remains Java 8-compatible even when it generates source forms that require newer Java language levels:

| Generated source form | Minimum Java source level to compile generated source |
|---|---:|
| Class, final class, interface, enum, annotation | 8 |
| Record | 16 |
| Sealed class or sealed interface | 17 |

Generated post-Java-8 source forms are text output only from the Java 8-compatible generator. Compiling those generated files is the responsibility of the target project and requires an appropriate JDK/source level.

## 7.6 Runtime Dependency Deployment Constraint

The runtime dependency audit must continue to show only the project artifact in runtime scope:

```text
org.javaspec:javaspec:jar:0.1.0-SNAPSHOT
```

Optional integrations that need third-party libraries must remain outside the core runtime or be scoped to test/build tooling until a future ADR changes the deployment model.
