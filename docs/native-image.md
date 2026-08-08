# Project-specific Native Image executable (preview)

JavaSpec can prepare a **project-specific GraalVM Native Image test executable** from the compiled
production and specification classes of a Maven consumer. The resulting program runs without
starting a JVM process.

> **Release boundary:** this preview was added on `develop` after immutable tag `v1.0.0-RC5`.
> Maven Central `1.0.0-RC5` does not contain `javaspec:native-prepare` or
> `NativeImageLauncher`. Build/install the current source checkout until the next release includes
> the capability.

This is not a universal native JavaSpec CLI. GraalVM uses a closed-world build: each executable
contains the exact spec and subject classes known when `native-image` runs.

## Verified first increment

The first increment supports:

- Maven source/spec support generation before `testCompile`;
- named-package `it_*` / `its_*` specifications linked into the native image;
- `ObjectBehavior<T>`, built-in matchers, lazy subject construction, constructor arguments,
  `let`, and `letGo`;
- `PASSED`, `FAILED`, `BROKEN`, `SKIPPED`, and `PENDING` outcomes;
- built-in `pretty`, `progress`, and JSON console formatters;
- stop-on-failure and automatic-prediction toggle semantics in the native launcher;
- process exit `0` for a run without failed/broken examples, `1` for failed/broken examples, and
  `64` for unsupported/invalid native options;
- Linux x86-64 build and JVM-free replay on GraalVM Native Image 25 locally and in dedicated CI run
  [`31283024044`](https://github.com/mgiustiniani/javaspec/actions/runs/31283024044), where GraalVM
  25.0.4 completed with zero annotations and uploaded the example executable.

The executable is generated for the consumer project. It is not a sixth JavaSpec publication
artifact and is not included in the Maven release checksum set.

## Deliberately unavailable in the first increment

The native executable does not currently perform:

- runtime source discovery, `--compile`, `--generate`, or source updates;
- runtime classpath mutation, POM dependency resolution, or loading new `.class` files;
- suite configuration, class/example filters, bootstrap hooks, or ServiceLoader extension discovery;
- JSON/JUnit report-file writing (JSON console formatting is supported);
- interface-double/proxy, generated Prophecy-wrapper, ByteBuddy concrete-double, or bytecode-agent
  qualification;
- JUnit Platform or Gradle native integration;
- default-package spec/subject linking;
- cross-platform qualification beyond the Linux x86-64 CI probe.

Unsupported options fail explicitly. They are not silently delegated to the JVM CLI. Executable
help keeps the boundary visible: **No runtime source discovery, compilation, generation, classpath
mutation, ServiceLoader extension discovery, or bytecode-agent self-attach.**

## Build from this checkout

Requirements:

- Maven;
- GraalVM Native Image 25 (`native-image` on `PATH`);
- production and specification types in named Java packages.

Install the current core and Maven plugin, then build the verified example:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml -DskipTests install
mvn -f examples/native-basic/pom.xml -Pnative clean package

examples/native-basic/target/javaspec-native-basic
examples/native-basic/target/javaspec-native-basic --formatter json
```

Or run the complete build/replay gate:

```sh
scripts/verify-native-example.sh
```

The script requires a real `native-image`, validates generated closed-world inputs, builds the
binary, replays passing/pending/skipped outcomes, checks JSON/help output, and verifies that an
unsupported JVM-only option exits `64`.

## Consumer Maven configuration

The JavaSpec Maven goal prepares inputs; GraalVM Native Build Tools produces the machine executable.
The two phases are intentionally separate.

```xml
<properties>
  <javaspec.version>VERSION_CONTAINING_NATIVE_PREVIEW</javaspec.version>
  <graalvm.buildtools.version>1.1.8</graalvm.buildtools.version>
</properties>

<dependencies>
  <dependency>
    <groupId>io.github.jvmspec</groupId>
    <artifactId>javaspec</artifactId>
    <version>${javaspec.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>io.github.jvmspec</groupId>
      <artifactId>javaspec-maven-plugin</artifactId>
      <version>${javaspec.version}</version>
      <executions>
        <execution>
          <id>prepare-javaspec-native-image</id>
          <phase>generate-test-sources</phase>
          <goals>
            <goal>generate</goal>
            <goal>native-prepare</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>

<profiles>
  <profile>
    <id>native</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.graalvm.buildtools</groupId>
          <artifactId>native-maven-plugin</artifactId>
          <version>${graalvm.buildtools.version}</version>
          <executions>
            <execution>
              <id>build-javaspec-native-executable</id>
              <phase>package</phase>
              <goals>
                <goal>compile-no-fork</goal>
              </goals>
            </execution>
          </executions>
          <configuration>
            <imageName>project-specs</imageName>
            <mainClass>io.github.jvmspec.generated.JavaspecNativeMain</mainClass>
            <fallback>false</fallback>
            <classpath>
              <param>${project.build.testOutputDirectory}</param>
              <param>${project.build.outputDirectory}</param>
              <param>${settings.localRepository}/io/github/jvmspec/javaspec/${javaspec.version}/javaspec-${javaspec.version}.jar</param>
            </classpath>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

Build after generation and `testCompile`:

```sh
mvn -Pnative clean package
./target/project-specs
```

Use [`../examples/native-basic/pom.xml`](../examples/native-basic/pom.xml) as the executable reference
configuration.

## Generated closed-world inputs

`javaspec:native-prepare` writes two deterministic inputs:

```text
target/generated-test-sources/javaspec-native/
  io/github/jvmspec/generated/JavaspecNativeMain.java

target/test-classes/META-INF/native-image/io.github.jvmspec/javaspec-native/
  reflect-config.json
```

The generated main class:

1. references every discovered spec and described subject with class literals;
2. embeds example names, source order, and source lines;
3. invokes the no-exit `NativeImageLauncher`;
4. maps its result to a process exit code.

The reflection metadata registers spec construction/public methods and subject constructors/methods
needed by the existing JavaSpec reflection runner and subject lifecycle. Re-running preparation with
unchanged source leaves both files byte-identical.

## Native executable options

```text
--formatter <progress|pretty|json>
--stop-on-failure
--auto-check-predictions
--no-auto-check-predictions
-h, --help
```

`NativeImageLauncher.run(...)` is also available as a no-`System.exit` programmatic entrypoint for
generated/build integrations.

## Why the executable is project-specific

A Java JAR contains JVM bytecode. The JVM can combine a previously published JavaSpec JAR with new
consumer classes at runtime. Native Image instead requires all executable code during its build;
reflection metadata exposes already-linked code but cannot admit arbitrary future bytecode.

Therefore the supported sequence is:

```text
javaspec:generate
    → testCompile
    → javaspec:native-prepare inputs
    → native-image(JavaSpec + target/classes + target/test-classes)
    → project-specific executable
```

See [ADR 0028](adr/0028-project-specific-native-image-preview.md) for the architecture decision.

## Extension sequence

Future increments should be admitted independently:

1. report files and deterministic runtime filters;
2. interface proxy and Prophecy reachability metadata;
3. broader construction/factory/record coverage;
4. Gradle preparation parity;
5. platform matrix expansion and executable reproducibility/security qualification;
6. an explicit decision for instrumentation-based doubles—never an implicit claim that JVM attach
   works inside Native Image.
