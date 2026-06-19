# javaspec

[![CI](https://github.com/mgiustiniani/javaspec/actions/workflows/ci.yml/badge.svg)](https://github.com/mgiustiniani/javaspec/actions/workflows/ci.yml)

![javaspec demo](docs/assets/demo.gif)

javaspec is a spec-first BDD tool for Java, inspired by phpspec. You describe the behavior you want, run the specification, and let javaspec guide the next small production-code step.

The core is Java 8-compatible and has no third-party runtime dependencies. It can be used directly from the CLI, embedded through a no-`System.exit` launcher, or adopted through optional Maven, Gradle, and JUnit Platform adapters.

Artifacts are published on Maven Central under `io.github.jvmspec`. Add the dependency:

```xml
<dependency>
    <groupId>io.github.jvmspec</groupId>
    <artifactId>javaspec</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

For snapshots, use the Central Portal Snapshots repository.

The Gradle plugin id is `io.github.jvmspec`.

## Highlights

- Spec-first BDD workflow inspired by phpspec.
- Java 8-compatible core.
- Zero-runtime-dependency core artifact.
- CLI, Maven plugin, Gradle plugin, and JUnit Platform adapter.
- Generation and update support for specs, support classes, production skeletons, constructors, and methods.
- JSON and JUnit XML-compatible reports.
- Generated typed proxy methods for phpspec-style `method().shouldReturn(expected)` syntax.
- Interface doubles in core; optional ByteBuddy-based concrete-class doubles adapter.

## Quick Start

### Setup

Build the local snapshot and make the launcher available:

```sh
# Build and install the local snapshot
mvn -q -DskipTests install

# Add bin/ to your PATH for this session
export PATH="$PWD/bin:$PATH"

# Or invoke directly
./bin/javaspec --help
```

After adding `bin/` to your `PATH`, the `javaspec` command is available. You can also run `./bin/javaspec` directly from the repository root without modifying `PATH`.

### The red-green cycle

**Step 1 — Describe a class:** creates spec and support skeletons.

```sh
javaspec describe com.example.PriceCalculator
```

**Step 2 — View and edit the generated spec:** open `src/test/java/spec/com/example/PriceCalculatorSpec.java` and add one example.

```java
package spec.com.example;

import com.example.PriceCalculator;
import com.example.PriceCalculatorSpecSupport;

public class PriceCalculatorSpec extends PriceCalculatorSpecSupport {
    public void it_calculates_the_total_price() {
        total(10.0, 2.5).shouldReturn(12.5);
    }
}
```

The `subject().method()` style is the standard way to invoke the subject under test; generated support classes provide sugar on top so you can call `method()` directly. The explicit form `match(subject().total(10.0, 2.5)).shouldReturn(12.5)` is also supported.

**Step 3 — Run specs:** the generation prompt fires because the production class is missing. `--generate` accepts automatically; `--compile` recompiles before execution; `--formatter pretty` shows descriptive output.

```sh
javaspec run --generate --compile --formatter pretty
```

Sample output:

```
PriceCalculator
  ✗ it calculates the total price  [BROKEN: class not found]

describes missing class: com.example.PriceCalculator
Generated class skeleton: src/main/java/com/example/PriceCalculator.java
Compilation output: target/javaspec-classes

  ✓ it calculates the total price  [PASSED]

1 spec, 1 example — 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending
```

**Step 4 — View the generated production class:**

```sh
cat src/main/java/com/example/PriceCalculator.java
```

javaspec wrote a skeleton with a stub return value (`return 0.0d;`).

**Step 5 — Implement the body (minimal fix):**

```sh
sed -i 's/return 0.0d;/return arg0 + arg1;/' src/main/java/com/example/PriceCalculator.java
```

The fixed method now looks like:

```java
public double total(double arg0, double arg1) {
    return arg0 + arg1;
}
```

**Step 6 — Verify everything passes:**

```sh
javaspec run --compile --formatter pretty
```

If you have a Maven project with the javaspec Maven plugin configured, `mvn javaspec:run` works too.

That's the full red-green cycle with javaspec.

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
        add(2, 3).shouldReturn(5);
    }
}
```

The concise form works because `javaspec describe` generates a support class with typed
proxy methods for each subject method — each returns `Matchable<T>` and can be chained
directly. The explicit form `match(subject().add(2, 3)).shouldReturn(5)` is equivalent
and useful when calling methods not yet reflected in the support class.

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
--resolve-pom <pom.xml>      # resolve runtime deps from POM (offline, local repo)
--compile                    # compile source/spec trees before execution
--compile-output <dir>       # compile output directory; implies --compile
--release <N>                # Java release target for --compile (e.g. 8, 11, 17)
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

Other commands:

```sh
javaspec list-extensions     # list discovered formatters and extensions + classpath hints
javaspec prophesize <Class>  # generate typed Prophecy wrapper for an interface
```

Exit codes are stable: `0` for success, `1` for failed/broken examples or declined/pending generation work, `64` for usage/profile/compiler/bootstrap errors, and `70` for I/O failures.

### Maven

Add the dependency and plugin to your `pom.xml`:

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

Add the plugin id `io.github.jvmspec` to your `build.gradle`. The Gradle plugin is published on the Gradle Plugin Portal. See [`examples/gradle-basic/settings.gradle`](examples/gradle-basic/settings.gradle) for a complete example:

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
    id 'io.github.jvmspec'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation 'io.github.jvmspec:javaspec:0.1.0-SNAPSHOT'
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
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.github.jvmspec</groupId>
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

Use proxy methods (phpspec style) or `match(value)` for fluent expectations:

```java
add(2, 3).shouldReturn(5);
name().shouldStartWith("calc");
items().shouldHaveCount(3);
shouldBeAnInstanceOf(Calculator.class);
```

The explicit form is available too, and useful when a proxy method is not yet generated:

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

Advanced stubbing APIs (all zero-dependency, all in core):

```java
// Sequential returns — each call returns the next value; last value repeats
notifier.control().when("send").thenReturn(true, false, true);

// Exhaustion policy — return values then throw
notifier.control().when("fetch").thenReturnThenThrow(new NoSuchElementException(), "a", "b");

// Sequential answer callbacks
notifier.control().when("transform").thenAnswerSequence(
    inv -> "first:"  + inv.argument(0),
    inv -> "second:" + inv.argument(0)
);

// Argument captor
ArgumentCaptor<String> captor = ArgumentCaptor.create();
notifier.control().when("send", captor).thenReturn(true);
notifier.instance().send("hello");
assertEquals("hello", captor.value());

// Ordered verification
notifier.control().verifyInOrder("prepare", "send", "cleanup");
notifier.control().verifyCalledBefore("prepare", "send");
```

### Optional bytecode concrete-class doubles

Install and add the standalone adapter only when you need non-final concrete-class doubles:

```sh
mvn -q -f javaspec-bytecode-doubles/pom.xml -DskipTests install
```

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
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

## Prophecy-Style Doubles

Inspired by [phpspec/prophecy](https://github.com/phpspec/prophecy), javaspec provides a
declarative doubles API built around prophecies, promises, and predictions.

Doubles replace **dependencies** of the subject under test — interfaces that the subject
collaborates with — not the subject itself.

### Reflective API

The core prophecy types live in `org.javaspec.doubles.prophecy`:

| Type | Purpose |
|---|---|
| `ObjectProphecy<T>` | Prophecy about an object of type `T` — wraps an `InterfaceDouble<T>` |
| `MethodProphecy<R>` | Prophecy about a specific method call — stub setup and predictions |
| `Promise<R>` | A promised return value or side-effect (`willReturn`, `willThrow`, `will`) |
| `Prediction` | A verification that a method was called, not called, or called N times |
| `PredictionRegistry` | Collects predictions and checks them all at once |
| `Argument` / `Arg` | Static matcher DSL (`any()`, `eq()`, `containingString()`, `isNull()`, `notNull()`) |

Use the reflective API in specs via `ObjectBehavior.prophesize(Class<T>)`. The subject
under test accesses dependencies via `subject()`:

```java
import static org.javaspec.doubles.prophecy.Argument.*;

public class UserServiceSpec extends ObjectBehavior<UserService> {
    public UserServiceSpec() {
        super(UserService.class);
    }

    public void it_sends_a_welcome_email() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        // Promise: stub the dependency method
        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .willReturn(true);

        // Inject the double into the subject
        subject().setMailer(mailer.reveal());

        // Execute — subject uses the double
        subject().sendWelcomeEmail("user@example.com");

        // Prediction: verify the dependency was called as expected
        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .shouldBeCalled();
        checkPredictions();
    }
}
```

Predictions are checked by calling `checkPredictions()` at the end of an example, or
automatically when `--auto-check-predictions` is enabled.

### Typed wrapper API

For concise, method-name-safe syntax, generate typed `*Prophecy` wrapper classes using the
reflection-based generator:

```sh
javaspec prophesize com.example.Mailer
```

This produces `MailerProphecy extends BaseObjectProphecy<Mailer>` with typed delegation methods,
so you can call `mailer.send(...)` instead of `mailer.method("send", ...)`:

```java
MailerProphecy mailer = new MailerProphecy(
    Doubles.interfaceDouble(Mailer.class),
    new PredictionRegistry()
);

mailer.send(any(String.class), any(String.class), any(String.class))
    .willReturn(true)
    .shouldBeCalled();
```

### Generating wrappers via CLI

```sh
javaspec prophesize <fqcn>                  # generate wrapper to generated-sources/
javaspec prophesize <fqcn> --output <dir>   # custom output directory
javaspec prophesize <fqcn> --package <pkg>  # custom target package
javaspec prophesize <fqcn> --overwrite      # replace existing file
javaspec prophesize <fqcn> --dry-run        # preview without writing
```

### Auto-generation during `javaspec run`

When `javaspec run --generate` is used, javaspec scans spec files for `prophesize(...)` and
`prophecy(...)` calls, detects missing wrapper classes, and generates them automatically:

```sh
javaspec run --generate --compile --formatter pretty
```

### Argument matchers

Import from `Argument`:

```java
import static org.javaspec.doubles.prophecy.Argument.*;
```

| Matcher | Description |
|---|---|
| `any()` | Matches any value, including null |
| `any(Class<?>)` | Matches null or any value assignable to the type |
| `eq(Object)` | Matches using javaspec's array-aware equality |
| `isNull()` | Matches only null |
| `notNull()` | Matches any non-null argument |
| `containingString(String)` | Matches strings containing a substring |

### Auto-check predictions

Enable automatic prediction verification after each example via CLI flag:

```sh
javaspec run --auto-check-predictions
```

Or programmatically in a spec:

```java
public UserServiceSpec() {
    super(UserService.class);
    setAutoCheckPredictions(true);
}
```

When enabled, the runner calls `checkPredictions()` after each example. If any prediction fails,
the example is marked FAILED with a descriptive message.

### Full example

See [`examples/prophecy-basic/`](examples/prophecy-basic/) for a complete working example with
an interface to prophesize, a spec using both reflective and typed-wrapper syntax, and a
standalone verification test.

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
- Artifacts are published on Maven Central under `io.github.jvmspec`. The Gradle plugin is published on the Gradle Plugin Portal.
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
- [`docs/usermanual/Home.md`](docs/usermanual/Home.md) — user manual with more CLI details.
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
