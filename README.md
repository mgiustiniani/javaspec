# javaspec

[![CI](https://github.com/mgiustiniani/javaspec/actions/workflows/ci.yml/badge.svg)](https://github.com/mgiustiniani/javaspec/actions/workflows/ci.yml)

![javaspec demo](docs/assets/demo.gif)

javaspec is a spec-first BDD tool for Java, inspired by PHPSpec. You write subject-centric `it_*` examples with `let`, `beConstructedWith`, `subject()`, and `should*` expectations, run the specification, and let javaspec guide the next small production-code step.

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

- Spec-first disciplined TDD/BDD workflow inspired by PHPSpec.
- Java 8-compatible core.
- Zero-runtime-dependency core artifact.
- CLI, Maven plugin, Gradle plugin, and JUnit Platform adapter.
- Generation and update support for specs, support classes, production skeletons, constructors, and methods.
- JSON and JUnit XML-compatible reports.
- Recommended PHPSpec-like authoring with `ObjectBehavior<T>`, `it_*` examples, `let`, `beConstructedWith`, `subject()`, and generated typed proxy methods such as `method().shouldReturn(expected)`.
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

public class PriceCalculatorSpec extends PriceCalculatorSpecSupport {
    public void it_calculates_the_total_price() {
        total(10.0, 2.5).shouldReturn(12.5);
    }
}
```

The recommended concrete-spec style is PHPSpec-like: write one behavior method, call the generated typed proxy (`total(...).shouldReturn(...)`), and let the generated `*SpecSupport` class stay in the background. The explicit form `match(subject().total(10.0, 2.5)).shouldReturn(12.5)` is also supported when a proxy has not been generated yet or when an explicit subject call is clearer.

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

A javaspec spec is a Java class whose public no-argument methods are examples. `javaspec describe` creates a concrete `*Spec` plus a generated `*SpecSupport`; the support class extends `ObjectBehavior<T>`, while the concrete spec stays focused on behavior.

```java
public class CalculatorSpec extends CalculatorSpecSupport {
    public void it_adds_two_numbers() {
        add(2, 3).shouldReturn(5);
    }
}
```

That concise form is the recommended PHPSpec-like style. The generated support class provides typed proxy methods for each subject method — each returns `Matchable<T>` and can be chained directly. The explicit form `match(subject().add(2, 3)).shouldReturn(5)` is equivalent and useful when calling methods not yet reflected in the support class.

Common authoring concepts:

- `subject()` lazily creates the described object.
- Generated typed proxies (`add(2, 3).shouldReturn(5)`) are the preferred subject-call syntax.
- `match(value).shouldReturn(expected)` and related matchers express expectations when you need the explicit form.
- `beConstructedWith(...)` selects constructor arguments before `subject()` is used.
- `beConstructedThrough("factoryName", ...)` selects a static factory method.
- `@Skip`, `@Pending`, `skip(...)`, and `pending(...)` mark examples intentionally not executed.

## Running javaspec

### CLI

After building from the repository root:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main --help
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main describe com.example.Calculator
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main run --compile --generate
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
javaspec prophesize <Class>  # generate typed Prophecy wrapper for an interface or concrete class
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

Prefer generated typed proxy methods for PHPSpec-like fluent expectations:

```java
add(2, 3).shouldReturn(5);
name().shouldStartWith("calc");
items().shouldHaveCount(3);
```

The explicit `match(subject().method(...))` form is available too, and useful when a proxy method is not yet generated:

```java
match(subject().add(2, 3)).shouldReturn(5);
match(subject().name()).shouldStartWith("calc");
match(subject().items()).shouldHaveCount(3);
match(subject()).shouldBeAnInstanceOf(Calculator.class);
```

`ObjectBehavior` also has direct convenience assertions such as `shouldReturn(actual, expected)`. Treat those as ad-hoc helpers; they are not the recommended style for ordinary subject behavior examples.

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

    currency().shouldReturn("USD");
}

public void it_uses_a_factory() {
    beConstructedThrough("from", "42");

    value().shouldReturn(42);
}
```

Generation can preserve, comment, or delete constructor-related skeleton code according to the selected constructor policy.

## PHPSpec-style example data

Use example data when one behavior needs a few concrete examples but a Cucumber `Scenario Outline` or
JUnit parameterized test would add ceremony. The public `it_*` method remains the behavior example;
rows execute inside that example and failing rows include row context in the assertion message.

```java
public void it_normalizes_known_inputs() {
    examples(row("  Alice  ", "Alice"), row("Bob", "Bob"))
        .verify(new Example2<String, String>() {
            @Override
            public void run(String input, String expected) {
                match(subject().normalize(input)).shouldReturn(expected);
            }
        });
}
```

`Example1` and `Example2` callbacks are available in core and keep the API Java 8-compatible without
Jupiter dependencies.

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
import static io.github.jvmspec.doubles.Doubles.any;
import static io.github.jvmspec.doubles.Doubles.eq;

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
import io.github.jvmspec.doubles.Doubles;
import io.github.jvmspec.doubles.InterfaceDouble;

InterfaceDouble<DataStore> storeDouble = Doubles.concreteDouble(DataStore.class);
storeDouble.control().returns("save", true);
beConstructedWith(storeDouble.instance());
match(subject().save("item")).shouldReturn(true);
```

The adapter is ByteBuddy-based and lives outside the core artifact. It supports non-final concrete classes only and explicitly rejects final classes, enums, arrays, annotations, primitives, and interfaces. See [`examples/bytecode-doubles-basic/`](examples/bytecode-doubles-basic/).

### Optional bytecode agent doubles

Install and add the standalone agent adapter when you need final-class, static-method, or
construction-aware doubles:

```sh
mvn -q -f javaspec-bytecode-agent/pom.xml -DskipTests install
```

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec-bytecode-agent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

The module supports dynamic self-attach through ByteBuddy Agent when the JVM allows it. You can also
start tests with `-javaagent:javaspec-bytecode-agent.jar` to make instrumentation available before
execution.

```java
// Final concrete class instance-method double
InterfaceDouble<FinalGreeter> greeter = Doubles.concreteDouble(FinalGreeter.class);
greeter.when("greet", "Ada").thenReturn("stubbed Ada");
assertEquals("stubbed Ada", greeter.instance().greet("Ada"));

// Static method double; close() restores original behavior for later calls
try (StaticDouble<StaticUtility> statics = BytecodeAgentDoubles.staticDouble(StaticUtility.class)) {
    statics.when("message", "x").thenReturn("stubbed x");
    assertEquals("stubbed x", StaticUtility.message("x"));
}

// Construction-aware double; subsequently created instances are registered
try (ConstructionDouble<ConstructedGreeter> construction =
         BytecodeAgentDoubles.mockConstruction(ConstructedGreeter.class)) {
    construction.when("name").thenReturn("stubbed");
    assertEquals("stubbed", new ConstructedGreeter().name());
}
```

While a static double is active, unstubbed static calls return normal javaspec default values
(`null`, `0`, `false`, etc.) instead of executing the original method. Closing the handle removes the
static/construction registration; the class remains instrumented, but unregistered calls fall through
to original behavior. See [`examples/bytecode-agent-basic/`](examples/bytecode-agent-basic/) for a
Maven example covering final-class and static-method doubles.

## Prophecy-Style Doubles

Inspired by [phpspec/prophecy](https://github.com/phpspec/prophecy), javaspec provides a
declarative doubles API built around prophecies, promises, and predictions.

Doubles replace **dependencies** of the subject under test — interfaces that the subject
collaborates with — not the subject itself.

The recommended PHPSpec-like collaborator style is the generated typed `*Prophecy` wrapper:
write `MailerProphecy mailer = prophesizeMailer();`, configure promises with
`mailer.send(...).willReturn(...)`, inject `mailer.reveal()`, then add predictions such as
`mailer.send(...).shouldBeCalled()`. The lower-level reflective `mailer.method("send", ...)` form is
available as a bootstrap/fallback API, but should not be the primary style in user specs once the
wrapper has been generated.

### Typed wrapper API (recommended PHPSpec-like syntax)

For concise, method-name-safe syntax, generate typed `*Prophecy` wrapper classes using the
reflection-based generator:

```sh
javaspec prophesize com.example.Mailer
```

This produces `MailerProphecy extends ObjectProphecy<Mailer>` with typed delegation methods,
so you can call `mailer.send(...)` instead of `mailer.method("send", ...)`:

```java
import static io.github.jvmspec.doubles.prophecy.Argument.*;

public class UserServiceSpec extends UserServiceSpecSupport {
    public void it_sends_a_welcome_email() {
        MailerProphecy mailer = prophesizeMailer();

        mailer.send(any(String.class), any(String.class), any(String.class))
            .willReturn(true)
            .shouldBeCalled();

        setMailer(mailer.reveal());
        sendWelcomeEmail("user@example.com");

        checkPredictions();
    }
}
```

On Java 10+, the same generated helper also supports local-variable inference while keeping the same
typed PHPSpec-like method syntax:

```java
var mailer = prophesizeMailer();
mailer.send(any(String.class), any(String.class), any(String.class))
    .willReturn(true)
    .shouldBeCalled();
```

The typed wrapper and the support helper are generated under `target/generated-sources/javaspec`.
The wrapper is not written to `src/`; Java 8 specs name the wrapper type explicitly, while Java 10+
specs can hide it with `var`.

Predictions are checked by calling `checkPredictions()` at the end of an example, or automatically
when `--auto-check-predictions` is enabled.

You can also receive supported collaborators as PHPSpec-style parameters on `let`, examples, and
`letGo`. For one example run, the same typed prophecy is reused across lifecycle and example methods.
Declare each collaborator type at most once per method; duplicate same-type parameters are reported
as ambiguous:

```java
public void let(MailerProphecy mailer) {
    mailer.send("user@example.com").willReturn(true).shouldBeCalled();
    setMailer(mailer.reveal());
}

public void it_sends_a_welcome_email(MailerProphecy mailer) {
    sendWelcomeEmail("user@example.com");
}
```

### Reflective API (bootstrap/fallback)

The core prophecy types live in `io.github.jvmspec.doubles.prophecy`:

| Type | Purpose |
|---|---|
| `ObjectProphecy<T>` | Prophecy about an object of type `T` — wraps an `InterfaceDouble<T>` produced by core interface doubles or an optional concrete-double adapter |
| `MethodProphecy<R>` | Prophecy about a specific method call — stub setup and predictions |
| `Promise<R>` | A promised return value or side-effect (`willReturn`, `willThrow`, `will`) |
| `Prediction` | A verification that a method was called, not called, or called N times |
| `PredictionRegistry` | Collects predictions and checks them all at once |
| `Argument` / `Arg` | Static matcher DSL (`any()`, `eq()`, `same()`, `in()`, `notIn()`, `matching()`, `containingString()`, `isNull()`, `notNull()`) |

Use `ObjectBehavior.prophesize(Class<T>)` when the typed wrapper/helper has not been generated yet:

```java
ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);
mailer.method("send", any(String.class), any(String.class), any(String.class))
        .willReturn(true)
        .shouldBeCalled();
```

A common workflow is to start with the reflective call, run `javaspec run --generate`, then switch
the concrete spec to the generated `MailerProphecy` / `prophesizeMailer()` syntax.

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
`prophecy(...)` calls, detects missing wrapper classes, and generates them automatically. It also
updates the generated `*SpecSupport` class with typed helpers such as `prophesizeMailer()` and
`prophecyMailer()` under `target/generated-sources/javaspec` only:

```sh
javaspec run --generate --compile --formatter pretty
```

A common workflow is to write `prophesize(Mailer.class)` first, run generation once, then switch the
spec to the recommended typed helper (`MailerProphecy mailer = prophesizeMailer();`). Generated typed
wrappers include argument-token overloads, so calls such as `mailer.send(any(String.class))` keep the
PHPSpec-like wrapper syntax instead of requiring reflective `method("send", ...)` fallback.

### Argument matchers

Import from `Argument`:

```java
import static io.github.jvmspec.doubles.prophecy.Argument.*;
```

| Matcher | Description |
|---|---|
| `any()` | Matches any value, including null |
| `any(Class<?>)` | Matches null or any value assignable to the type |
| `eq(Object)` | Matches using javaspec's array-aware equality |
| `isNull()` | Matches only null |
| `notNull()` | Matches any non-null argument |
| `containingString(String)` | Matches strings containing a substring |
| `same(Object)` / `identicalTo(Object)` | Matches the same object reference |
| `in(Object...)` / `notIn(Object...)` | Matches membership using array-aware equality |
| `matching(Predicate<Object>, String)` | Matches with a custom callback and diagnostic description |
| `token(ArgumentToken)` / `custom(ArgumentToken)` | Uses a named custom Prophecy-style token |

Custom prediction callbacks are available when built-in predictions are not expressive enough:

```java
mailer.send(any(String.class)).should(context -> {
    if (context.callCount() < 2) {
        throw new AssertionError("expected at least two mail sends");
    }
});
```

The callback receives matching calls, all calls, the method name, and the argument pattern through
`PredictionContext`.

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

See [`examples/prophecy-basic/`](examples/prophecy-basic/) for a complete working example with an
interface to prophesize, the recommended typed-wrapper syntax, the reflective bootstrap/fallback
form, and a standalone verification test.

## Reports

The CLI and adapters can write both JSON and JUnit XML-compatible reports:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main run \
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
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main run --config javaspec.conf --suite domain
```

CLI options override matching config values where an override exists.

## Compilation

javaspec is classpath/reflection based by default. Source/spec compilation is explicit:

```sh
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main run --compile
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main run --compile-output target/javaspec-classes
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main run --compile --release 8
java -cp target/javaspec-0.1.0-SNAPSHOT.jar io.github.jvmspec.cli.Main run --compile --resolve-pom pom.xml
```

Compilation uses the current JDK `javax.tools.JavaCompiler` API. It can use `--release <N>` on
Java 9+ compilers, caches unchanged source compilation inputs, and can resolve simple Maven POM test
classpath entries from the local Maven repository via `--resolve-pom`. It still does not fork
`javac`; Maven and Gradle builds normally rely on their own Java compilation tasks unless their
javaspec adapter settings opt into javaspec compilation.

## Compatibility and boundaries

- The core artifact remains Java 8-compatible and zero-runtime-dependency.
- Artifacts are published on Maven Central under `io.github.jvmspec`. The Gradle plugin is published on the Gradle Plugin Portal.
- The Maven plugin, Gradle plugin, JUnit Platform engine, bytecode doubles adapter, and bytecode agent adapter are standalone optional artifacts outside the root Maven reactor.
- Repository-root `mvn verify` is intentionally core-only.
- `scripts/verify-all.sh` verifies the core, optional adapters, and standalone examples together.
- Non-final concrete-class doubles require the optional ByteBuddy subclass adapter; final-class, static-method, and construction-aware doubles require the optional bytecode agent adapter.
- Compilation is opt-in; it supports local-POM dependency resolution, incremental cache hits, and `--release <N>`, but does not fork `javac`.
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
- [`examples/bytecode-doubles-basic/`](examples/bytecode-doubles-basic/) — optional non-final concrete-class doubles.
- [`examples/bytecode-agent-basic/`](examples/bytecode-agent-basic/) — optional final-class and static-method doubles.
- [`docs/usermanual/Home.md`](docs/usermanual/Home.md) — user manual with more CLI details.
- [`docs/release-notes-0.1.1-SNAPSHOT.md`](docs/release-notes-0.1.1-SNAPSHOT.md) — unreleased development-line notes.
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
