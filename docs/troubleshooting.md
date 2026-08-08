# Troubleshooting — 1.0

## The launcher selects an unexpected JAR

Run `bin/javaspec --launcher-fingerprint`. The repository launcher prefers the POM-aligned artifact
in `~/.m2/repository` and falls back to `target/`. Reinstall the intended checkout with
`mvn -q -DskipTests install`, then verify the version, selected path, and SHA-256 again. Do not infer
the executed binary from a source commit alone.

## Specs are discovered but skipped as non-loadable

The CLI/JUnit Platform adapter can discover source files that are not present as compiled classes on
the run classpath. Compile test/spec sources first or add the compile output to the javaspec
classpath. For CLI runs, use `--compile` when appropriate.

## Generated skeletons report BROKEN even though assertions pass

A generated production body still contains `// javaspec:stub`. Replace the generated default with real
domain behavior. Compiled runs intentionally add a synthetic `BROKEN` generation result while stub
markers remain.

## `No matching constructor found`

Check `beConstructedWith(...)` arguments against subject constructors. For records, explicit prefix
construction can be padded for newly added trailing canonical components, but exact constructors still
win and compact constructor validation still applies.

## Collaborator injection reports BROKEN

Only ordinary interfaces and generated typed `*Prophecy` wrappers are supported collaborator
parameters. Duplicate same-type collaborator parameters in one lifecycle/example method are
ambiguous. Collaborator injection is not a DI container.

## Prophecy prediction fails after the example

Automatic predictions run after the example body and before `letGo`. Verify that the revealed double
was passed to the subject and that the expected method/arguments match the actual calls. Diagnostics
include matching and recorded call context where available.

## Extension activation fails

Configured extensions must be loadable on the run classpath, implement `JavaspecExtension` or
`Extension`, have a public no-argument constructor, and register valid formatter names. Use
`javaspec list-extensions` for discovered provider and classpath hints.

## Formatter name is invalid

Use a built-in formatter (`progress`, `pretty`, `json`) or an external formatter registered through
ServiceLoader/configured extension activation on the effective run classpath.

## JUnit Platform IDE run finds no tests

Ensure the optional `javaspec-junit-platform-engine` artifact and compiled spec classes are on the
JUnit Platform test runtime classpath. Configure `*Spec.java` includes in Surefire/IDE patterns and
set `javaspec.specRoot` when source discovery uses a non-default directory.

## Row selector behaves like the whole example still ran

That is expected. Example-data rows execute inline in the owning example. JUnit Platform row selectors
filter descriptors/events but do not isolate per-row execution.

## Report writing fails with `No such file or directory`

Create the destination's parent directory before requesting a JSON or JUnit XML report:

```sh
mkdir -p target/javaspec
bin/javaspec run --report target/javaspec/run-report.json \
  --junit-xml target/javaspec/junit.xml
```

Report writers fail with exit `70` and include the destination path when a parent directory is
missing or unwritable. `--generation-report` uses the atomic generation-report path and may create
its parent independently; do not rely on its later write to prepare an earlier run-report path.

## Gradle plugin marker returns 404

RC5 was submitted under plugin id `io.github.jvmspec`, but first publication requires Portal review.
Until the marker resolves, use the included plugin build in `examples/gradle-basic/settings.gradle`
or a local publication. A successful `publishPlugins` task proves submission, not public marker
availability.

## Maven reports unknown goal `native-prepare`

Published `1.0.0-RC5` predates the Native Image preview. Build/install current `develop` core and
Maven plugin, then confirm `mvn help:describe -Dplugin=io.github.jvmspec:javaspec-maven-plugin -Dgoal=native-prepare -Ddetail` before using the native
example. Do not overwrite source-bound evidence with an unverified artifact of the same version.

## Native verification stops with `GRAALVM_NATIVE_IMAGE_REQUIRED`

Install GraalVM Native Image 25 or set `NATIVE_IMAGE_BIN`/`GRAALVM_HOME` to that distribution. A
Temurin/OpenJDK runtime alone can run JavaSpec but cannot create the machine executable. The
dedicated CI job is the remote native gate.

## Native preparation rejects a default-package type

The generated launcher lives in a named package and Java cannot reference default-package consumer
types from it. Move both spec and described production type into named packages. This is an explicit
first-preview boundary, not a silent skip.

## The native executable rejects a normal CLI option

Only the options listed by the generated executable's `--help` are supported. Runtime discovery,
`--compile`, `--generate`, classpath mutation, report files, extensions, and dynamic doubles are not
yet native-qualified and return usage exit `64`. Run the JVM CLI for those operations, rebuild the
native image after `testCompile`, and consult [`native-image.md`](native-image.md).

## Core dependency audit shows third-party runtime dependencies

The root core artifact should have none. Dependency-heavy capabilities belong in optional artifacts
such as the JUnit Platform engine, bytecode doubles, bytecode agent, Maven plugin, or Gradle plugin.
Run `mvn dependency:tree -Dscope=runtime` from the repository root to audit core.
