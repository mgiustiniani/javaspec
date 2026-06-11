# javaspec

[![CI](https://github.com/mgiustiniani/javaspec/actions/workflows/ci.yml/badge.svg)](https://github.com/mgiustiniani/javaspec/actions/workflows/ci.yml)

![javaspec demo](docs/assets/demo.gif)

javaspec is a specification-first testing and design tool for Java, inspired by phpspec. You describe the behavior you want, run the specification, and let javaspec guide the next small production-code step.

The core is Java 8-compatible and has no third-party runtime dependencies. It can be used directly from the CLI, embedded through a no-`System.exit` launcher, or adopted through optional Maven, Gradle, and JUnit Platform adapters.

Public artifact publication is not available yet. Until Central Portal and Gradle Plugin Portal publication are completed, the examples and snippets below use locally installed `0.1.0-SNAPSHOT` artifacts.

## Highlights

- Specification-first Java workflow inspired by phpspec.
- Java 8-compatible core.
- Zero-runtime-dependency core artifact.
- CLI, Maven plugin, Gradle plugin, and JUnit Platform adapter.
- Generation and update support for specs, support classes, production skeletons, constructors, and methods.
- JSON and JUnit XML-compatible reports.
- Interface doubles in core; optional ByteBuddy-based concrete-class doubles adapter.

## Quick Start

From a checkout of this repository, install the local snapshot:

```sh
mvn -q -DskipTests install
```

Describe a class:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main describe com.example.Calculator
```

This creates a spec skeleton under the configured spec root. Public coordinates and packaged distributions are not published yet, so local use currently relies on the built jar or Maven-local snapshots.

Write the first behavior:

```java
package spec.com.example;

import com.example.Calculator;
import org.javaspec.api.ObjectBehavior;

public class CalculatorSpec extends ObjectBehavior<Calculator> {
    public CalculatorSpec() {
        super(Calculator.class);
    }

    public void it_adds_two_numbers() {
        match(subject().add(2, 3)).shouldReturn(5);
    }
}
```

Run specs and opt into source/spec compilation:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main run --compile
```

If production code is missing, `run` can prompt for generation. Use `--generate` for non-interactive generation.

## Why use javaspec?

Use javaspec when you want to:

- start with executable behavior examples before production implementation;
- keep test/runtime infrastructure small and dependency-light;
- generate boring spec/support/production skeletons while you focus on behavior;
- run the same specs from the CLI, Maven, Gradle, or JUnit Platform-based tools;
- produce machine-readable JSON and CI-friendly JUnit XML-compatible reports;
- use built-in expectations and interface doubles without requiring JUnit or a mocking framework.

## Writing a first spec

A javaspec spec is a Java class whose public no-argument methods are examples. Generated specs usually extend `ObjectBehavior<T>`.

```java
public class CalculatorSpec extends ObjectBehavior<Calculator> {
    public CalculatorSpec() {
        super(Calculator.class);
    }

    public void it_adds_two_numbers() {
        match(subject().add(2, 3)).shouldReturn(5);
    }
}
```

Common authoring concepts:

- `subject()` lazily creates the described object.
- `match(value).shouldReturn(expected)` and related matchers express expectations.
- `beConstructedWith(...)` selects constructor arguments before `subject()` is used.
- `beConstructedThrough("factoryName", ...)` selects a static factory method.
- `@Skip`, `@Pending`, `skip(...)`, and `pending(...)` mark examples intentionally not executed.

## Running javaspec

### CLI

After building from the repository root:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main --help
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main describe com.example.Calculator
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main run --compile --generate
```

Useful `run` options:

```sh
--config <file>              # load javaspec configuration
--suite <name>               # select a configured suite
--spec-dir <dir>             # spec source root
--source-dir <dir>           # production source root
--classpath <path-list>      # explicit runtime/dependency classpath
--classpath-file <file>      # one classpath entry per line
--compile                    # compile source/spec trees before execution
--compile-output <dir>       # compile output directory; implies --compile
--generate                   # apply generation/update prompts non-interactively
--dry-run                    # plan generation/update work without writes
--stop-on-failure            # stop after first failed or broken example
--formatter progress|pretty  # select built-in output format
--profile java8|java11|java17|java21|java25
--report <file>              # JSON report
--junit-xml <file>           # JUnit XML-compatible report
--class <name>               # filter described/spec class
--example <name>             # filter example method/display name/order index
```

Exit codes are stable: `0` for success, `1` for failed/broken examples or declined/pending generation work, `64` for usage/profile/compiler/bootstrap errors, and `70` for I/O failures.

### Maven

Until artifacts are public, install the core and standalone Maven plugin snapshots locally:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml -DskipTests install
```

Consumer `pom.xml` example:

```xml
<properties>
  <javaspec.version>0.1.0-SNAPSHOT</javaspec.version>
</properties>

<dependencies>
  <dependency>
    <groupId>org.javaspec</groupId>
    <artifactId>javaspec</artifactId>
    <version>${javaspec.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.javaspec</groupId>
      <artifactId>javaspec-maven-plugin</artifactId>
      <version>${javaspec.version}</version>
      <executions>
        <execution>
          <phase>verify</phase>
          <goals>
            <goal>run</goal>
          </goals>
          <configuration>
            <jsonReportFile>${project.build.directory}/javaspec/run-report.json</jsonReportFile>
            <junitXmlReportFile>${project.build.directory}/javaspec/junit-report.xml</junitXmlReportFile>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

See [`examples/maven-basic/`](examples/maven-basic/) for a complete consumer project.

### Gradle

Install the core snapshot first:

```sh
mvn -q -DskipTests install
```

Until the Gradle plugin is published, use the standalone plugin from this checkout, as shown in [`examples/gradle-basic/settings.gradle`](examples/gradle-basic/settings.gradle):

```groovy
pluginManagement {
    includeBuild('../../javaspec-gradle-plugin')
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
    }
}
```

Consumer `build.gradle` example:

```groovy
plugins {
    id 'java'
    id 'org.javaspec'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation 'org.javaspec:javaspec:0.1.0-SNAPSHOT'
}

javaspec {
    jsonReportFile = file("$buildDir/reports/javaspec/run-report.json")
    junitXmlReportFile = file("$buildDir/reports/javaspec/junit-report.xml")
}

tasks.named('javaspecRun') {
    stopOnFailure = true
    failOnFailure = true
}
```

Run it with:

```sh
gradle -p examples/gradle-basic clean javaspecRun
```

### JUnit Platform

The JUnit Platform engine is optional. Install it locally before use:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml -DskipTests install
```

Consumer Maven example:

```xml
<dependency>
  <groupId>org.javaspec</groupId>
  <artifactId>javaspec</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.javaspec</groupId>
  <artifactId>javaspec-junit-platform-engine</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.junit.platform</groupId>
  <artifactId>junit-platform-launcher</artifactId>
  <version>1.10.2</version>
  <scope>test</scope>
</dependency>
```

Configure your JUnit Platform launcher, IDE, or Surefire setup to include `*Spec.java`. See [`examples/junit-platform-basic/`](examples/junit-platform-basic/) and [`javaspec-junit-platform-engine/README.md`](javaspec-junit-platform-engine/README.md).

### Programmatic invocation

Build tools and custom launchers can call javaspec without `System.exit`:

```java
SpecDiscoveryRequest request = SpecDiscoveryRequest.of(new File("src/test/java"));
JavaspecInvocation invocation = JavaspecInvocation.discovering(request, classLoader)
        .withStopOnFailure(true);
JavaspecInvocationResult result = JavaspecLauncher.run(invocation);
int exitCode = result.exitCode();
```

## Matchers and expectations

Use `match(value)` for fluent expectations:

```java
match(subject().add(2, 3)).shouldReturn(5);
match(subject().name()).shouldStartWith("calc");
match(subject().items()).shouldHaveCount(3);
match(subject()).shouldBeAnInstanceOf(Calculator.class);
```

Available expectation families include:

- equality/identity: `shouldReturn`, `shouldEqual`, `shouldBe`, and negated aliases;
- type checks: `shouldHaveType`, `shouldBeAnInstanceOf`, `shouldImplement`;
- strings, collections, maps, arrays, and iterables: contain/count/empty/key/value checks;
- string helpers: starts-with, ends-with, and regular-expression checks;
- exception expectations through `shouldThrow(...).during...` support methods;
- structural markers used by generation, such as `shouldBeAnInterface()` and `shouldImplement(...)`.

## Construction

Configure construction before calling `subject()`:

```java
public void it_uses_constructor_arguments() {
    beConstructedWith("USD", 2);

    match(subject().currency()).shouldReturn("USD");
}

public void it_uses_a_factory() {
    beConstructedThrough("from", "42");

    match(subject().value()).shouldReturn(42);
}
```

Generation can preserve, comment, or delete constructor-related skeleton code according to the selected constructor policy.

## Doubles

### Interface doubles in core

Core doubles use JDK dynamic proxies and require no extra dependencies:

```java
InterfaceDouble<Notifier> notifier = interfaceDouble(Notifier.class);
notifier.control().returns("send", true);

beConstructedWith(notifier.instance());
match(subject().notify("hello")).shouldReturn(true);
notifier.control().verifyCalled("send", "hello");
```

Argument matchers, throwing stubs, and answer callbacks are supported:

```java
import static org.javaspec.doubles.Doubles.any;
import static org.javaspec.doubles.Doubles.eq;

notifier.control().returnsFor("send", true, eq("alerts"), any(String.class));
notifier.control().verifyCalled("send", eq("alerts"), any(String.class));
```

Core doubles support ordinary interfaces only. Concrete classes, final classes, static methods, and constructors are not mocked by the core runtime.

### Optional bytecode concrete-class doubles

Install and add the standalone adapter only when you need non-final concrete-class doubles:

```sh
mvn -q -f javaspec-bytecode-doubles/pom.xml -DskipTests install
```

```xml
<dependency>
  <groupId>org.javaspec</groupId>
  <artifactId>javaspec-bytecode-doubles</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

Example:

```java
import org.javaspec.doubles.Doubles;
import org.javaspec.doubles.InterfaceDouble;

InterfaceDouble<DataStore> storeDouble = Doubles.concreteDouble(DataStore.class);
storeDouble.control().returns("save", true);
beConstructedWith(storeDouble.instance());
match(subject().save("item")).shouldReturn(true);
```

The adapter is ByteBuddy-based and lives outside the core artifact. It supports non-final concrete classes only and explicitly rejects final classes, enums, arrays, annotations, primitives, and interfaces. Static method mocking, constructor mocking, and final-class mocking remain unsupported. See [`examples/bytecode-doubles-basic/`](examples/bytecode-doubles-basic/).

## Reports

The CLI and adapters can write both JSON and JUnit XML-compatible reports:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main run \
  --compile \
  --report target/javaspec-report.json \
  --junit-xml target/javaspec-report.xml
```

Notes:

- JSON reports use `schemaVersion: 1` and include stable ids, status counts, pending counts, source metadata where available, and optional run metadata/properties.
- JUnit XML-compatible reports map skipped and pending examples to `<skipped>` elements.
- Report write failures exit with code `70`.
- Schema and golden examples: [`docs/schemas/run-report-v1.schema.json`](docs/schemas/run-report-v1.schema.json), [`docs/examples/reports/`](docs/examples/reports/).

## Configuration

Without a config file, javaspec uses conventional defaults:

- suite: `default`
- spec root: `src/test/java`
- source root: `src/main/java`
- spec package prefix: `spec`
- production package prefix: empty
- profile: `java8`
- formatter: `progress`
- constructor policy: `comment`

A small line-based config avoids adding parser dependencies:

```properties
profile=java17
formatter=pretty
report=target/javaspec/run-report.json
junit-xml=target/javaspec/junit-report.xml
constructor-policy=comment

suite.domain.spec-dir=src/spec/java
suite.domain.source-dir=src/main/java
suite.domain.spec-package-prefix=spec
suite.domain.package-prefix=com.example
suite.domain.bootstrap=com.example.SpecBootstrap
```

Use it with:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main run --config javaspec.conf --suite domain
```

CLI options override matching config values where an override exists.

## Compilation

javaspec is classpath/reflection based by default. Source/spec compilation is explicit:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main run --compile
java -cp target/javaspec-0.1.0-SNAPSHOT.jar org.javaspec.cli.Main run --compile-output target/javaspec-classes
```

Compilation uses the current JDK `javax.tools.JavaCompiler` API. It does not add dependency resolution, fork `javac`, maintain an incremental cache, or manage source/release levels for you. Maven and Gradle builds normally rely on their own Java compilation tasks unless their javaspec adapter settings opt into javaspec compilation.

## Compatibility and boundaries

- The core artifact remains Java 8-compatible and zero-runtime-dependency.
- Public publication is postponed until signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final version/tag, and publish approval are complete.
- The Maven plugin, Gradle plugin, JUnit Platform engine, and bytecode doubles adapter are standalone optional artifacts outside the root Maven reactor.
- Repository-root `mvn verify` is intentionally core-only.
- `scripts/verify-all.sh` verifies the core, optional adapters, and standalone examples together.
- Concrete-class doubles require the optional ByteBuddy adapter and support non-final classes only.
- Compilation is opt-in and does not provide dependency resolution, forked `javac`, incremental caching, or source-release management.
- Target profiles (`java8`, `java11`, `java17`, `java21`, `java25`) are enforced conservatively before generation/update writes where metadata is resolvable.
- Extension, formatter, and bootstrap discovery are classpath/ServiceLoader based; package scanning, plugin lookup, script engines, and automatic classpath repair are out of scope.

## Development and verification

For day-to-day core verification:

```sh
mvn verify
mvn dependency:tree -Dscope=runtime
```

For aggregate local verification of core, standalone adapters, and examples:

```sh
scripts/verify-all.sh
```

For examples only:

```sh
scripts/verify-examples.sh
```

Version alignment across the root project and standalone adapters:

```sh
scripts/check-version-alignment.sh
```

Detailed verification evidence is maintained in [`docs/test-report.md`](docs/test-report.md). The implementation plan and status history live in [`PLAN.md`](PLAN.md), not at the top of this README.

## Examples and deeper documentation

Start here:

- [`examples/README.md`](examples/README.md) — standalone consumer examples.
- [`examples/maven-basic/`](examples/maven-basic/) — Maven plugin adoption.
- [`examples/gradle-basic/`](examples/gradle-basic/) — Gradle plugin adoption.
- [`examples/junit-platform-basic/`](examples/junit-platform-basic/) — JUnit Platform adoption.
- [`examples/bytecode-doubles-basic/`](examples/bytecode-doubles-basic/) — optional concrete-class doubles.
- [`docs/usermanual/Home.md`](docs/usermanual/Home.md) — user manual with more CLI and migration notes.
- [`javaspec-gradle-plugin/README.md`](javaspec-gradle-plugin/README.md) — Gradle plugin details.
- [`javaspec-junit-platform-engine/README.md`](javaspec-junit-platform-engine/README.md) — JUnit Platform engine details.
- [`CHANGELOG.md`](CHANGELOG.md) — release-change log scaffold.
- [`RELEASING.md`](RELEASING.md) — release-readiness checklist and publication blockers.

Architecture and decisions:

- [`PLAN.md`](PLAN.md) — implementation plan and requirement traceability.
- [`docs/arc42/`](docs/arc42/) — architecture documentation.
- [`docs/arc42/01-introduction-and-goals.md`](docs/arc42/01-introduction-and-goals.md)
- [`docs/arc42/02-constraints.md`](docs/arc42/02-constraints.md)
- [`docs/arc42/03-context-and-scope.md`](docs/arc42/03-context-and-scope.md)
- [`docs/arc42/04-solution-strategy.md`](docs/arc42/04-solution-strategy.md)
- [`docs/arc42/05-building-block-view.md`](docs/arc42/05-building-block-view.md)
- [`docs/arc42/06-runtime-view.md`](docs/arc42/06-runtime-view.md)
- [`docs/arc42/07-deployment-view.md`](docs/arc42/07-deployment-view.md)
- [`docs/arc42/08-concepts.md`](docs/arc42/08-concepts.md)
- [`docs/arc42/09-architecture-decisions.md`](docs/arc42/09-architecture-decisions.md)
- [`docs/arc42/10-quality-requirements.md`](docs/arc42/10-quality-requirements.md)
- [`docs/arc42/11-risks-and-technical-debt.md`](docs/arc42/11-risks-and-technical-debt.md)
- [`docs/arc42/12-glossary.md`](docs/arc42/12-glossary.md)
- [`docs/adr/`](docs/adr/) — architectural decision records.
- [`docs/research/phpspec-feature-inventory.md`](docs/research/phpspec-feature-inventory.md) — phpspec feature inventory.
- [`docs/research/java-lts-data-structures.md`](docs/research/java-lts-data-structures.md) — Java LTS profile research.
