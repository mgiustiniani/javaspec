# javaspec User Manual

Wiki home for the current javaspec MVP.

javaspec is a Java 8-compatible, zero-runtime-dependency specification tool inspired by PHPSpec. The current MVP supports the first spec-first loop plus the ADR 0004 correction work, follow-up factory construction generation, the Phase 7 matcher/expectation expansion, and Phase 8 MVP collaborators/doubles: specification/support generation, production type discovery and generation, constructor and static factory construction generation, typed proxy matcher support, direct `ObjectBehavior` convenience assertions, method skeleton generation, Phase 3 Java LTS profiles/catalog/API-symbol metadata/compatibility probes, Phase 4 configuration, naming, suite selection, discovery filters, the Phase 5/6 MVP reflection runner, and JDK-proxy interface doubles.

> Current status: `describe` writes specification/support files only. `javaspec run` keeps discovery, generation, and source updates, then executes discovered examples when the compiled spec classes are available on the effective classloader. It reuses `DiscoveredSpec`/`SpecExample` metadata, so suite, class, and example filters remain effective. The matcher set includes expanded negation, type/instance, count/empty, string, and map key/value helpers while preserving zero runtime dependencies. Interface doubles are available for ordinary interfaces through JDK dynamic proxies, with method-name/exact-argument stubbing, call history, and verification helpers. Configured bootstrap hooks, profile, and formatter values are parsed/validated metadata for later runner and formatter features.

## Quick start

From the repository root:

```sh
mvn verify
mvn dependency:tree -Dscope=runtime
```

Run the CLI:

```sh
java -jar target/javaspec-0.1.0-SNAPSHOT.jar --help
```

Short form used in the examples below:

```sh
javaspec='java -jar target/javaspec-0.1.0-SNAPSHOT.jar'
```

## Commands

```sh
$javaspec describe <ClassName> [--config <file>] [--suite <name>] [--spec-dir <dir>]
$javaspec desc <ClassName> [--config <file>] [--suite <name>] [--spec-root <dir>]
$javaspec run [--config <file>] [--suite <name>] [--spec-dir <dir>] [--source-dir <dir>] [--generate] [--constructor-policy <delete|preserve|comment>] [--class <name>] [--example <name>]
```

Aliases and defaults:

| Long | Alias | Default | Command |
|---|---|---|---|
| `describe` | `desc` | n/a | n/a |
| `--config <file>` | n/a | inferred defaults | `describe`, `run` |
| `--suite <name>` | n/a | configuration default suite (`default` with inferred defaults) | `describe`, `run` |
| `--spec-dir` | `--spec-root` | selected suite `specDir` (`src/test/java` with inferred defaults) | `describe`, `run` |
| `--source-dir` | `--source-root` | selected suite `sourceDir` (`src/main/java` with inferred defaults) | `run` |
| `--generate` | n/a | `false` | `run` |
| `--constructor-policy <delete\|preserve\|comment>` | n/a | configuration `constructorPolicy` (`comment` with inferred defaults) | `run` |
| `--class <name>` | n/a | no class filter | `run` |
| `--example <name>` | n/a | no example filter | `run` |

`describe` writes specification files only. Production source generation and updates belong to `run`. After discovery/generation/update completes without declined prompts, `run` invokes the MVP reflection runner for discovered examples whose compiled spec classes are available on the effective classloader. `describe` rejects command-line `--source-dir`/`--source-root`; a `sourceDir` present in a selected config suite is accepted because `describe` ignores source roots.

## Configuration files

`describe` and `run` accept `--config <file>` and `--suite <name>`. When no config file is supplied, javaspec uses `JavaspecConfiguration.defaults()`.

### Syntax

The configuration format is intentionally restricted and line-based so the runtime stays dependency-free:

- Blank lines and lines whose first non-whitespace character is `#` are ignored.
- Key/value separator is either `=` or `:`.
- There is no YAML, TOML, or JSON parser dependency.
- Top-level keys are `profile`, `formatter`, `constructorPolicy`/`constructor-policy`, `defaultSuite`/`default-suite`, and `bootstrap`.
- Valid `profile` values are `java8`, `java11`, `java17`, `java21`, and `java25`; valid constructor policies are `delete`, `preserve`, and `comment`.
- Suite keys use `suite.<name>.<property>` with properties `specDir`/`spec-dir`, `sourceDir`/`source-dir`, `specPackagePrefix`/`spec-package-prefix`, `packagePrefix`/`package-prefix`, and `bootstrap`.
- `bootstrap` values are comma-separated strings. They are metadata only in the current MVP and are not executed.
- `specPackagePrefix` and `packagePrefix` drive naming conventions for `describe`, `run`, discovery, and spec/support generation. `packagePrefix` may be empty. Other configured values, including bootstrap entries when the key is present, must not be blank.

### Defaults

| Setting | Default |
|---|---|
| Default suite name | `default` |
| Spec root | `src/test/java` |
| Source root | `src/main/java` |
| Spec package prefix | `spec` |
| Production package prefix | empty |
| Profile | `java8` |
| Formatter | `progress` |
| Constructor policy | `comment` |
| Bootstrap hooks | empty |

### Example config

```properties
# javaspec.conf
profile = java17
formatter = progress
constructorPolicy = preserve
defaultSuite = domain
bootstrap = org.example.SpecBootstrap

suite.domain.specDir = src/test/java
suite.domain.sourceDir = src/main/java
suite.domain.specPackagePrefix = spec
suite.domain.packagePrefix = org.example
suite.domain.bootstrap = org.example.DomainBootstrap, org.example.TestDataBootstrap

suite.integration.spec-dir: src/integrationSpec/java
suite.integration.source-dir: src/main/java
suite.integration.spec-package-prefix: spec
suite.integration.package-prefix:
```

### CLI precedence and examples

1. `--config <file>` loads explicit configuration; without it, defaults are inferred.
2. `--suite <name>` selects a configured suite; without it, `defaultSuite` is used.
3. The selected suite's `specDir`, `sourceDir`, `specPackagePrefix`, and `packagePrefix` are used. Command-line `--spec-dir`/`--spec-root` or `--source-dir`/`--source-root` override paths only; naming still comes from the selected suite.
4. `run` uses the configured `constructorPolicy` unless overridden by command-line `--constructor-policy`.
5. `run` applies repeatable `--class <name>` and `--example <name>` filters after suite selection.
6. `describe` accepts `--config` and `--suite` for spec-root and naming selection, rejects command-line `--source-dir`, and ignores any `sourceDir` value loaded from config.

Describe using a configured suite:

```sh
$javaspec describe org.example.Calculator --config javaspec.conf --suite domain
```

Run using a configured suite and generate non-interactively:

```sh
$javaspec run --config javaspec.conf --suite domain --generate
```

Override a selected-suite path from the command line:

```sh
$javaspec run --config javaspec.conf --suite domain --source-dir /tmp/demo/src/main/java --generate
```

### Diagnostics

Invalid configuration exits as command-line usage error (`64`) and starts with `Error: Invalid configuration:`. Diagnostics include line numbers for parser errors where available. Examples include duplicate keys, unknown keys, malformed lines without `=` or `:`, blank required values, invalid profiles, invalid constructor policies, and selecting an unconfigured suite.

Examples:

```text
Error: Invalid configuration: Line 3: Invalid constructor policy: keep. Valid values: delete, preserve, comment.
Error: Invalid configuration: Suite 'api' is not configured. Available suites: default, domain.
```

A missing or unreadable config file exits with I/O error (`70`) and prints the config path.

### Current configuration limitations

- Bootstrap hooks are parsed as strings but are not executed yet.
- `profile` and `formatter` are parsed and validated but active profile-aware execution and formatter selection are not implemented yet.
- Package-prefix naming is implemented for describe/run discovery, generation, and MVP reflection execution; future runner/reporting layers may add richer diagnostics and source locations.
- The runner lifecycle is intentionally minimal in the MVP: fresh spec instance per example plus optional public no-arg `let()` and `letGo()`. Pending examples, stop-on-failure, bootstrap execution, and active formatter behavior remain future work.

## Suite naming and filters

Suite package prefixes configure how production classes map to spec/support classes.

With inferred defaults, `org.example.Calculator` maps to:

```text
src/test/java/spec/org/example/CalculatorSpec.java
src/test/java/spec/org/example/CalculatorSpecSupport.java
```

With this suite configuration:

```properties
suite.domain.specDir = src/test/java
suite.domain.sourceDir = src/main/java
suite.domain.specPackagePrefix = spec.domain
suite.domain.packagePrefix = org.example
```

`org.example.Calculator` maps to:

```text
src/test/java/spec/domain/CalculatorSpec.java
src/test/java/spec/domain/CalculatorSpecSupport.java
```

`run` supports repeatable filters:

```sh
$javaspec run --config javaspec.conf --suite domain --class Calculator
$javaspec run --config javaspec.conf --suite domain --class org.example.Calculator
$javaspec run --config javaspec.conf --suite domain --class spec.domain.CalculatorSpec
$javaspec run --config javaspec.conf --suite domain --example it_is_initializable
$javaspec run --config javaspec.conf --suite domain --example "it is initializable"
$javaspec run --config javaspec.conf --suite domain --example 0
```

Class filters match described qualified names, described simple names, spec qualified names, or spec simple names exactly. Example filters match public `void` example methods named `it_*` or `its_*` by method name, display name (underscores replaced by spaces), or source-order index. Filters affect discovery/generation selection and MVP reflection execution because the runner uses the same `DiscoveredSpec`/`SpecExample` metadata.

## Example execution MVP

`javaspec run` has two stages:

1. Preserve the existing discovery, related-spec handling, support updates, production type generation, constructor updates, and method updates.
2. If no prompt was declined and no I/O/usage error occurred, execute the discovered examples whose compiled spec classes are available on the effective classloader.

The runner is reflection-based and dependency-free. It does not compile source or spec files. If a spec exists only as source, or if the compiled spec class is otherwise unavailable to the CLI process, the discovered examples are marked `SKIPPED` rather than executed. Compile or otherwise put spec classes on the classpath before expecting execution.

Execution uses the existing discovery metadata:

- `DiscoveredSpec` selects the spec class and described type.
- `SpecExample` selects public `void` `it_*`/`its_*` examples, display names, and source-order indexes.
- Suite, class, and example filters affect execution because they filter that metadata before the runner sees it.

Lifecycle behavior:

- A fresh spec instance is created for each example.
- Optional public no-argument `let()` runs before each example.
- Optional public no-argument `letGo()` runs after each example, including after failures.

Result states:

| State | Meaning |
|---|---|
| `PASSED` | The example completed normally. |
| `FAILED` | The example threw `AssertionError`. |
| `BROKEN` | A non-assertion throwable occurred in the example, `let()`, `letGo()`, spec instantiation, or reflection inspection. |
| `SKIPPED` | The spec class was not loadable, or the reflected example method was missing or not public no-arg. |

CLI summary example:

```text
Examples: 3 total, 1 passed, 1 failed, 0 broken, 1 skipped.
Failed examples:
  FAILED spec.org.example.CalculatorSpec#it_adds_numbers (it adds numbers): Assertion failed - java.lang.AssertionError: expected 4
Skipped examples:
  SKIPPED spec.org.example.CalculatorSpec#it_subtracts_numbers (it subtracts numbers): Example method not found or not public no-arg: it_subtracts_numbers
```

Exit code `1` is returned when executable examples fail or break. Skipped-only runs remain successful. Missing production generation or method-update prompts that are declined or unavailable also return exit code `1` before execution.

## BDD workflow

### 1. Describe a class

```sh
$javaspec describe org.example.Calculator
```

or:

```sh
$javaspec desc org.example.Calculator
```

This creates two test-source files:

```text
src/test/java/spec/org/example/CalculatorSpec.java
src/test/java/spec/org/example/CalculatorSpecSupport.java
```

Generated concrete spec:

```java
package spec.org.example;

import org.example.Calculator;

public class CalculatorSpec extends CalculatorSpecSupport {
    public void it_is_initializable() {
        shouldHaveType(Calculator.class);
    }
}
```

Generated support class:

```java
package spec.org.example;

import org.example.Calculator;

public class CalculatorSpecSupport extends org.javaspec.api.ObjectBehavior<Calculator> {
    public CalculatorSpecSupport() {
        super(Calculator.class);
    }
}
```

The concrete spec extends the generated support class. The support class extends `ObjectBehavior<Calculator>` and passes `Calculator.class` to enable lazy subject construction.

The import of `org.example.Calculator` is intentional. In a BDD/spec-first flow the production class may not exist yet, so the project can be red until `run` generates or the user writes the class.

### 2. Run discovery, generation, and execution

```sh
$javaspec run
```

`run` first performs discovery and any gated generation/update work. If execution can proceed, it then runs discovered examples whose compiled spec classes are available on the effective classloader.

If `org.example.Calculator` is missing, javaspec asks:

```text
spec.org.example.CalculatorSpec describes missing class org.example.Calculator.
Target path: src/main/java/org/example/Calculator.java
Do you want me to create org.example.Calculator for you? [Y/n]
```

Answers:

| Answer | Meaning |
|---|---|
| `Y`, `y`, `yes`, or empty Enter | generate the production class skeleton |
| `N`, `n`, `no` | do not generate |
| any other value | javaspec asks again |

### 3. Accept generation interactively

```text
Do you want me to create org.example.Calculator for you? [Y/n]
y
Generated class skeleton: src/main/java/org/example/Calculator.java
```

Generated production class:

```java
package org.example;

public class Calculator { }
```

### 4. Decline generation

```text
Do you want me to create org.example.Calculator for you? [Y/n]
n
No production files were written.
```

Exit code: `1`.

### 5. Generate non-interactively

For CI or scripted usage, use `--generate` to answer yes without prompting:

```sh
$javaspec run --generate
```

This writes missing production type skeletons, generated specification support updates, constructor updates, static factory construction method skeletons, and missing instance method skeletons inferred from specs without interactive confirmation. After those updates, executable examples run only if the corresponding compiled spec classes are already on the effective classloader; otherwise they are reported as skipped.

## Construction semantics

Generated support classes configure `ObjectBehavior<Subject>` with `Subject.class`, so the subject is constructed lazily on first access. Construction can be configured before that first access.

Constructor arguments use `beConstructedWith(...)`. For generation, this remains constructor descriptor generation: `run` can create or update matching constructor skeletons, not factory methods.

```java
public class BookSpec extends BookSpecSupport {
    public void it_can_be_constructed_with_values() {
        beConstructedWith("Wizard", 5);

        getTitle().shouldReturn("Wizard");
        getRating().shouldReturn(5);
    }
}
```

Static factory construction uses `beConstructedThrough("create", args...)`. For generation, the factory marker now discovers/generates a static factory method skeleton returning the described type instead of an empty constructor marker.

```java
public void it_can_be_constructed_through_a_factory() {
    beConstructedThrough("create", "Wizard");

    getTitle().shouldReturn("Wizard");
}
```

For a described `Book`, that construction marker can generate a factory skeleton such as:

```java
public class Book {
    public static Book create(String arg0) {
        return new Book();
    }
}
```

Named factory forms behave the same way:

```java
beConstructedNamed("named");
beConstructedNamed("named", "Wizard");
beConstructedThroughNamed("createNamed", "Wizard");
```

For a described `Book`, these correspond to static factory skeletons such as `named()`, `named(String arg0)`, and `createNamed(String arg0)`, all returning `Book`.

Factory marker names must be string literals and valid Java identifiers to generate methods. Calls with non-string-literal names, such as `beConstructedThrough(factoryName, "Wizard")`, are ignored by generation because the method name is not statically known; they do not create empty constructor markers.

Construction can be overridden before instantiation. The last construction rule before the first subject access wins:

```java
public void it_overrides_construction_before_instantiation() {
    beConstructedWith("first");
    beConstructedWith("second");

    getTitle().shouldReturn("second");
}
```

After the subject has been instantiated, changing construction is an error:

```java
public void it_rejects_late_construction_changes() {
    getTitle().shouldReturn("Wizard"); // instantiates the subject

    beConstructedWith("late"); // throws IllegalStateException
}
```

Constructor or factory failures can be specified with `duringInstantiation()`:

```java
public void it_rejects_invalid_constructor_arguments() {
    beConstructedWith(-1);

    shouldThrow(IllegalArgumentException.class).duringInstantiation();
}
```

## Typed proxy matcher syntax

Generated support classes can expose subject-specific typed proxy methods. This allows PHPSpec-like Java syntax in the concrete spec:

```java
public class BookSpec extends BookSpecSupport {
    public void it_has_a_rating() {
        getRating().shouldReturn(5);
    }

    public void it_has_a_title() {
        getTitle().shouldContain("Wizard");
    }

    public void it_rejects_negative_ratings() {
        shouldThrow(IllegalArgumentException.class).duringSetRating(-3);
    }
}
```

For discovered methods, the support class contains methods similar to:

```java
protected org.javaspec.matcher.Matchable<Integer> getRating() {
    return match(subject().getRating());
}

protected org.javaspec.matcher.Matchable<String> getTitle() {
    return match(subject().getTitle());
}

protected void setRating(int rating) {
    subject().setRating(rating);
}
```

It also generates typed throw proxies such as `duringSetRating(...)`.

Generated typed spec support intentionally skips static factory descriptors discovered from construction markers such as `beConstructedThrough("create", ...)`. Static factories are construction methods on the described type, not instance subject proxies, so support classes do not generate `create().should...`, `duringCreate(...)`, or `subject().create(...)` wrappers for them.

The existing explicit wrapper style remains available:

```java
match(subject().getRating()).shouldReturn(5);
```

## Method generation

`run` discovers typed proxy calls and construction factory markers, then can generate missing subject method skeletons. Discovery currently covers the supported expanded chained matcher calls, typed throw calls such as `shouldThrow(...).duringSetRating(-3)`, direct `subject().method(...)` calls, simple setter-style calls, and static factory construction markers.

`beConstructedWith(...)` remains constructor descriptor generation. The factory construction forms `beConstructedThrough("create", args...)`, `beConstructedNamed("named", args...)`, and `beConstructedThroughNamed("createNamed", args...)` are method-generation inputs when the factory name is a string literal and a valid Java identifier; they generate static factory methods returning the described type instead of empty constructor markers.

Example spec:

```java
public class BookSpec extends BookSpecSupport {
    public void it_has_a_rating() {
        getRating().shouldReturn(5);
    }

    public void it_has_a_title() {
        getTitle().shouldContain("Wizard");
    }

    public void it_rejects_negative_ratings() {
        shouldThrow(IllegalArgumentException.class).duringSetRating(-3);
    }
}
```

With `--generate`, javaspec writes updates non-interactively:

```sh
$javaspec run --generate
```

Possible generated production class:

```java
package org.example;

public class Book {
    public int getRating() {
        return 0;
    }

    public String getTitle() {
        return null;
    }

    public void setRating(int rating) {
    }
}
```

Factory construction markers add static factory skeletons returning the described type. For example, `beConstructedThrough("create", "Wizard")` can add:

```java
public static Book create(String arg0) {
    return new Book();
}
```

Static factory descriptors are skipped when generated typed support is updated, because they are construction methods rather than instance subject proxy methods.

When the production source file already exists and `--generate` is not used, javaspec prompts before adding missing method skeletons:

```text
Do you want me to add missing method skeletons to org.example.Book in src/main/java/org/example/Book.java? [Y/n]
```

Default returns are Java 8-compatible: `false` for `boolean`, zero values for numeric primitives, `'\0'` for `char`, and `null` for reference types.

## Constructor policy

`run` accepts constructor handling policy explicitly:

```sh
$javaspec run --constructor-policy comment
$javaspec run --constructor-policy preserve
$javaspec run --constructor-policy delete
```

| Policy | Meaning |
|---|---|
| `comment` | Default. Non-empty unmatched constructors are commented out. |
| `preserve` | Non-empty unmatched constructors are kept. |
| `delete` | Non-empty unmatched constructors are deleted. This is the explicit destructive opt-in. |

Empty generated/no-op unmatched constructors may be removed when safe, regardless of policy. Constructor policy applies to `run`; `describe` never updates production source. A config file can set `constructorPolicy`/`constructor-policy`; command-line `--constructor-policy` overrides the configured value.

## Matchers

Typed proxy methods return `Matchable<T>` for non-void subject methods. The explicit wrapper style also returns `Matchable<T>`:

```java
getRating().shouldReturn(5);
match(subject().getRating()).shouldReturn(5);
```

The implemented matcher set is dependency-free and includes these groups.

### Equality, identity, and negation

| Method | Meaning |
|---|---|
| `shouldBe(expected)` | identity comparison using Java `==` semantics |
| `shouldNotBe(unexpected)` | negated identity comparison |
| `shouldEqual(expected)` | equality comparison using `equals` semantics |
| `shouldNotEqual(unexpected)` | negated equality comparison |
| `shouldReturn(expected)` | alias for equality/return terminology |
| `shouldNotReturn(unexpected)` | negated return/equality alias |
| `shouldBeLike(expected)` | equality alias for PHPSpec-like terminology |
| `shouldNotBeLike(unexpected)` | negated `beLike` alias |
| `shouldBeEqualTo(expected)` | equality alias |
| `shouldNotBeEqualTo(unexpected)` | negated equality alias |

`MatcherRegistry` keeps the runtime dependency-free. It provides the built-in identity/equality/negated-identity matchers and a default negated-equality matcher for `shouldNotEqual` and its aliases.

### Type, implementation, and containment

| Method | Meaning |
|---|---|
| `shouldHaveType(Class<?>)` | requires a non-null value assignable to the expected type |
| `shouldBeAnInstanceOf(Class<?>)` | alias for `shouldHaveType` |
| `shouldReturnAnInstanceOf(Class<?>)` | return-terminology alias for `shouldHaveType` |
| `shouldImplement(Class<?>)` | requires the wrapped value, or wrapped `Class<?>`, to implement or extend the expected type |
| `shouldContain(value)` | checks character sequences, collections, maps, arrays, or iterables for a contained value |
| `shouldNotContain(value)` | negated containment check |

For maps, `shouldContain(value)` succeeds if the value is present as either a key or a value.

### Count, emptiness, and maps

| Method | Supported values |
|---|---|
| `shouldHaveCount(int)` | arrays, collections, maps, character sequences, and iterables |
| `shouldBeEmpty()` | arrays, collections, maps, character sequences, and iterables |
| `shouldNotBeEmpty()` | arrays, collections, maps, character sequences, and iterables |
| `shouldHaveKey(key)` / `shouldNotHaveKey(key)` | maps |
| `shouldHaveValue(value)` / `shouldNotHaveValue(value)` | maps |

Known limitation: count and emptiness checks on a generic `Iterable` iterate the iterable to compute the count. This consumes one-shot iterables and can hang on infinite iterables.

### String helpers

| Method | Meaning |
|---|---|
| `shouldStartWith(prefix)` / `shouldNotStartWith(prefix)` | character sequence prefix check |
| `shouldEndWith(suffix)` / `shouldNotEndWith(suffix)` | character sequence suffix check |
| `shouldMatchPattern(pattern)` / `shouldNotMatchPattern(pattern)` | Java regular expression check using `Pattern` |

Examples:

```java
getRating().shouldReturn(5);
getRating().shouldNotReturn(0);
getTitle().shouldContain("Wizard");
getTitle().shouldNotContain("Draft");
getTitle().shouldStartWith("The");
getTitle().shouldNotStartWith("Draft");
getTitle().shouldEndWith("Oz");
getTitle().shouldNotEndWith("Draft");
getTitle().shouldMatchPattern("Wiz.*");
getTitle().shouldNotMatchPattern("Draft.*");
getTags().shouldHaveCount(2);
getTags().shouldNotBeEmpty();
getMetadata().shouldHaveKey("isbn");
getMetadata().shouldHaveValue("Wizard");
getBookClass().shouldImplement(Readable.class);
```

### Direct `ObjectBehavior` convenience assertions

`ObjectBehavior` also exposes direct assertion methods for ad-hoc checks. These methods delegate through `match(actual)`, so they share behavior with typed proxy and explicit wrapper assertions:

```java
shouldReturn(subject().getRating(), 5);
shouldNotEqual(subject().getRating(), 0);
shouldHaveType(subject().getTitle(), String.class);
shouldImplement(subject(), Readable.class);
shouldContain(subject().getTitle(), "Wizard");
shouldHaveCount(subject().getTags(), 2);
shouldBeEmpty(subject().getNotes());
shouldHaveKey(subject().getMetadata(), "isbn");
shouldNotStartWith(subject().getTitle(), "Draft");
```

The direct convenience set covers equality/negation aliases, type/instance/implementation checks, containment, count/empty checks, map key/value checks, and string negations. Positive string helpers are available through typed proxy or explicit `match(actual)` usage.

### Custom matchers

Custom matchers can be registered in the matcher registry and may evaluate null subjects; javaspec passes the actual subject value, including `null`, to the matcher predicate.

```java
matcherRegistry().register("beAbsent", new org.javaspec.matcher.CustomMatcher<Object>(
    "beAbsent",
    new org.javaspec.matcher.CustomMatcher.MatchPredicate<Object>() {
        @Override
        public boolean test(Object subject, Object... expected) {
            return subject == null;
        }
    }
));

match(null).shouldMatch("beAbsent");
```

`SpecDiscovery` recognizes the expanded chained matcher names on typed proxy calls for method-discovery/default-return inference where applicable.

## Interface doubles

The Phase 8 MVP adds zero-runtime-dependency collaborator doubles under `org.javaspec.doubles`. Doubles are implemented with Java 8 JDK dynamic proxies, so the core runtime can double ordinary interfaces without bytecode libraries.

### Creating interface doubles

Use `Doubles` directly when working outside an `ObjectBehavior` subclass. The examples below assume `Notifier` is an ordinary interface.

```java
import org.javaspec.doubles.Doubles;
import org.javaspec.doubles.InterfaceDouble;

InterfaceDouble<Notifier> notifierDouble = Doubles.interfaceDouble(Notifier.class);
Notifier notifier = notifierDouble.instance();

// Shortcuts when only the proxy is needed:
Notifier proxy = Doubles.create(Notifier.class);
Notifier sameStyle = Doubles.of(Notifier.class);
Notifier alsoProxy = Doubles.proxy(Notifier.class);
```

Inside specs that extend `ObjectBehavior`, use the convenience APIs:

```java
Notifier notifier = doubleFor(Notifier.class);
InterfaceDouble<Notifier> notifierDouble = interfaceDouble(Notifier.class);
```

`Doubles.isDouble(value)` returns whether a value is a javaspec double. `Doubles.control(proxy)`, `Doubles.inspect(proxy)`, `doubleControl(proxy)`, and `inspectDouble(proxy)` return the control API for an existing proxy.

### Stubbing return values

Stubs match either by method name with any arguments or by method name with exact arguments:

```java
notifierDouble.when("channel").thenReturn("alerts");
notifierDouble.when("send", "alerts", new String[] {"ops", "oncall"}).thenReturn(Boolean.TRUE);

notifierDouble.returns("fallbackChannel", "general");
notifierDouble.returnsFor("send", Boolean.TRUE, "alerts", new String[] {"ops", "oncall"});
```

Exact argument matching supports `null` values and compares array contents rather than array identity. Unstubbed methods return Java defaults: `false` for `boolean`, zero values for numeric primitives, `'\0'` for `char`, `null` for reference types, and no action for `void` methods.

### Call history and verification

Interface method calls are recorded as immutable `Call` snapshots. The control APIs can inspect all calls, calls by method name, or calls by method name and exact arguments:

```java
notifier.send("alerts", new String[] {"ops", "oncall"});

notifierDouble.calls();
notifierDouble.calls("send");
notifierDouble.calls("send", "alerts", new String[] {"ops", "oncall"});
notifierDouble.callCount("send");
notifierDouble.callCount("send", "alerts", new String[] {"ops", "oncall"});
```

Verification supports called, not-called, called-once, and exact-count checks:

```java
notifierDouble.verify("send").called();
notifierDouble.verify("send", "alerts", new String[] {"ops", "oncall"}).calledOnce();
notifierDouble.verify("send").times(1);
notifierDouble.verify("missing").notCalled();

notifierDouble.verifyCalled("send");
notifierDouble.verifyCalledWith("send", "alerts", new String[] {"ops", "oncall"});
notifierDouble.verifyNotCalled("missing");
notifierDouble.verifyCallCount("send", 1);
```

`ObjectBehavior` adds spec-style convenience assertions:

```java
shouldHaveBeenCalled(notifier, "send");
shouldHaveBeenCalledWith(notifier, "send", "alerts", new String[] {"ops", "oncall"});
shouldNotHaveBeenCalled(notifier, "missing");
shouldHaveBeenCalledTimes(notifier, "send", 1);

doubleCalls(notifier);
doubleCalls(notifier, "send");
doubleCallCount(notifier, "send");
```

### Object methods and resets

`toString()`, `equals(Object)`, and `hashCode()` are handled deterministically by the proxy invocation handler. `toString()` identifies the doubled interface and internal id, `equals` uses proxy identity, and `hashCode` uses the stable internal id. These object methods are not user stubs and do not represent collaborator calls.

Call and stub state can be cleared separately or together:

```java
notifierDouble.clearCalls();
notifierDouble.clearStubs();
notifierDouble.reset();
```

### Supported targets and limitations

Only ordinary interfaces are supported. The double factory rejects `null`, primitive types, arrays, annotations, enums, concrete classes, and final classes with clear `IllegalArgumentException` messages.

Current MVP limitations:

- No concrete class, final class, static method, or constructor doubles.
- No wildcard or predicate argument matchers; matching is by method name or exact arguments only.
- No exception stubbing, callback stubbing, sequential returns, or side-effect stubbing.
- No bytecode-library integration in the core runtime.
- Default interface methods are not invoked by the proxy handler.

## Class-like type generation

javaspec supports these class-like production types. The javaspec binary remains Java 8-compatible; post-Java-8 forms are generated as source text and represented as metadata/strings.

| Production kind | Spec marker | Generated skeleton | Minimum Java source |
|---|---|---|---:|
| class | default, or `shouldBeAClass();` | `public class Foo { }` | 8 |
| final class | `shouldBeAFinalClass();` | `public final class Foo { }` | 8 |
| interface | `shouldBeAnInterface();` | `public interface Foo { }` | 8 |
| enum | `shouldBeAnEnum();` | `public enum Foo { }` | 8 |
| annotation | `shouldBeAnAnnotation();` | `public @interface Foo { }` | 8 |
| record | `shouldBeARecord();` | `public record Foo() { }` | 16 |
| sealed class | `shouldBeASealedClass();` | `public sealed class Foo permits Foo.Permitted { ... }` | 17 |
| sealed interface | `shouldBeASealedInterface();` | `public sealed interface Foo permits Foo.Permitted { ... }` | 17 |

The command stays PHPSpec-like: `describe` does not take a type flag. To describe a non-class type, edit the generated spec and add a marker example before running generation.

## Extends and implements

Use spec markers to describe inheritance and implemented interfaces:

```java
public class ServiceSpec extends ServiceSpecSupport {
    public void it_extends_base_service() {
        shouldExtend(BaseService.class);
    }

    public void it_implements_payment_gateway() {
        shouldImplement(PaymentGateway.class);
    }
}
```

When related types are missing, `run` handles them before generating the owner type: it suggests or creates their specs, then writes their production skeletons. For sealed classes, `shouldPermit(...)` can create final permitted subtype specs that extend the sealed root. For sealed interfaces, permitted implementations remain nested in the sealed interface source file in this MVP.

## Custom directories

### Custom spec directory

```sh
$javaspec describe org.example.Calculator --spec-dir /tmp/demo/src/test/java
```

Creates:

```text
/tmp/demo/src/test/java/spec/org/example/CalculatorSpec.java
/tmp/demo/src/test/java/spec/org/example/CalculatorSpecSupport.java
```

Equivalent alias:

```sh
$javaspec desc org.example.Calculator --spec-root /tmp/demo/src/test/java
```

### Custom source directory during run

```sh
$javaspec run \
  --spec-dir /tmp/demo/src/test/java \
  --source-dir /tmp/demo/src/main/java
```

Equivalent alias:

```sh
$javaspec run \
  --spec-root /tmp/demo/src/test/java \
  --source-root /tmp/demo/src/main/java
```

### Custom directories with non-interactive generation

```sh
$javaspec run \
  --spec-dir /tmp/demo/src/test/java \
  --source-dir /tmp/demo/src/main/java \
  --generate
```

## Existing specification

If the spec already exists:

```sh
$javaspec describe org.example.Calculator
```

javaspec reports it and does not overwrite the spec:

```text
Specification spec.org.example.CalculatorSpec exists; no generation needed.
Spec file: src/test/java/spec/org/example/CalculatorSpec.java
No production class was generated.
```

If the support file is missing, `describe` creates `CalculatorSpecSupport.java`.

## Existing production class

If a spec exists and the production class already exists in the source tree:

```text
src/test/java/spec/org/example/CalculatorSpec.java
src/test/java/spec/org/example/CalculatorSpecSupport.java
src/main/java/org/example/Calculator.java
```

then:

```sh
$javaspec run
```

prints:

```text
spec.org.example.CalculatorSpec describes org.example.Calculator; class exists.
Source file: src/main/java/org/example/Calculator.java
```

No production type skeleton is generated. If the spec describes constructors, static factories, or missing instance methods, `run` may update the existing source according to the constructor policy and method-generation confirmation rules.

If the class is available on the classpath instead of the source tree, javaspec reports:

```text
Classpath: present
```

After these existence and update checks, `run` executes the filtered examples when the compiled spec class itself is also available on the effective classloader. If only the source file is present, the examples are listed as `SKIPPED` because the CLI does not compile them.

## No specs found

```sh
$javaspec run --spec-dir /tmp/empty-spec-root
```

Output:

```text
No specifications found in /tmp/empty-spec-root.
```

Exit code: `0`.

## Spec-to-class mapping

javaspec follows a PHPSpec-inspired namespace convention. The default convention uses spec package prefix `spec` and an empty production package prefix; configured suites can replace both prefixes.

| Spec file | Spec class | Support class | Described production type |
|---|---|---|---|
| `src/test/java/spec/org/example/CalculatorSpec.java` | `spec.org.example.CalculatorSpec` | `spec.org.example.CalculatorSpecSupport` | `org.example.Calculator` |
| `src/test/java/spec/com/acme/UserSpec.java` | `spec.com.acme.UserSpec` | `spec.com.acme.UserSpecSupport` | `com.acme.User` |
| `src/test/java/spec/domain/CalculatorSpec.java` with `specPackagePrefix=spec.domain`, `packagePrefix=org.example` | `spec.domain.CalculatorSpec` | `spec.domain.CalculatorSpecSupport` | `org.example.Calculator` |

Rules:

1. The spec class name ends with `Spec`.
2. The generated support class name ends with `SpecSupport`.
3. The spec package starts with the active `specPackagePrefix`.
4. The described production package is the active `packagePrefix` plus the spec package suffix after `specPackagePrefix`. With the default empty production package prefix this is the spec package without the leading `spec.`.
5. The described production type name is the spec class name without the trailing `Spec`.
6. The described production kind defaults to class unless the spec contains a marker such as `shouldBeAFinalClass();`, `shouldBeAnInterface();`, `shouldBeAnEnum();`, `shouldBeAnAnnotation();`, `shouldBeARecord();`, `shouldBeASealedClass();`, or `shouldBeASealedInterface();`.
7. `shouldExtend(...)`, `shouldImplement(...)`, and `shouldPermit(...)` class literals are resolved through imports or the described production package.
8. Constructor and method descriptors are discovered heuristically from supported construction and typed proxy syntax: `beConstructedWith(...)` describes constructors; factory construction markers with string-literal Java-identifier names describe static factory methods; typed proxy calls using the expanded chained matcher names, throw-proxy calls, direct `subject().method(...)`, and simple setter calls describe instance methods where applicable.

Legacy same-package specs are also discovered by convention when the default production package prefix is empty, but new specs generated by `describe` use the active suite naming convention.

## Invalid usage examples

### `--generate` does not belong to `describe`

```sh
$javaspec describe org.example.Calculator --generate
```

Result:

```text
Error: The --generate option belongs to run; describe creates only a specification skeleton.
```

### `--source-dir` does not belong to `describe`

```sh
$javaspec describe org.example.Calculator --source-dir src/main/java
```

Result:

```text
Error: The source directory is used by run; describe writes only to the spec directory.
```

### Unknown constructor policy

```sh
$javaspec run --constructor-policy keep
```

Result:

```text
Error: Invalid constructor policy: keep. Valid values: delete, preserve, comment.
```

### Unknown command

```sh
$javaspec generate org.example.Calculator
```

Result:

```text
Error: Unknown command: generate
```

### Invalid class name

```sh
$javaspec describe class
```

Result:

```text
Error: Invalid class name: Class name segment is a reserved Java word: class
```

## Exit codes

| Code | Meaning |
|---:|---|
| `0` | success, help, no specs found, existing/generated/updated targets, passed examples, or skipped-only example runs |
| `1` | missing production type or missing method update was not generated because the prompt was declined or input was unavailable; or executable examples failed/broke |
| `64` | invalid command line usage |
| `70` | I/O or security error while reading config, checking, or writing files |

## Dependency policy

Runtime dependencies are not allowed.

Check runtime dependencies:

```sh
mvn dependency:tree -Dscope=runtime
```

Expected output contains only the project artifact:

```text
org.javaspec:javaspec:jar:0.1.0-SNAPSHOT
```

## Verification

Current verification after completing the Phase 8 MVP collaborators/doubles implementation:

- `mvn verify` passed with 328 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

## Current MVP limitations

- The CLI runner does not compile source or spec files itself; source-only or otherwise unavailable spec classes are skipped/not executable until compiled classes are present on the effective classloader.
- The runner lifecycle is an MVP: fresh spec instance per example plus optional public no-arg `let()` and `letGo()`. Pending examples, stop-on-failure, active formatters, bootstrap execution, profile-aware execution, and richer reporting remain future work.
- Configuration files currently drive selected suite paths, package-prefix naming, constructor-policy defaults, and run class/example filters; bootstrap hooks and profile/formatter behavior remain metadata until later runner and formatter features are implemented.
- Source parsing and generation use Java 8-compatible heuristics, not a full Java parser.
- Generated post-Java-8 source forms, such as records and sealed types, require an appropriate JDK to compile.
- Method generation covers the supported typed proxy, throw-proxy, direct subject/setter, and static factory construction marker syntax; it is not a general Java source synthesis engine.
- Count and emptiness checks on generic `Iterable` values consume the iterable and can hang on infinite iterables.
- Doubles/collaborators are interface-only in the core runtime. Concrete class, final class, static method, constructor, primitive, array, annotation, and enum doubles are not supported.
- Double argument matching has no wildcard or predicate matchers; stubbing is return-value-only and does not support exceptions, callbacks, sequences, or side effects.
- Default interface methods are not invoked by interface doubles.
