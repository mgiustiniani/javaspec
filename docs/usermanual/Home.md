# javaspec User Manual

Wiki home for the current javaspec MVP.

javaspec is a Java 8-compatible, zero-runtime-dependency specification tool inspired by PHPSpec. The current MVP supports the first spec-first loop plus the ADR 0004 correction work and follow-up factory construction generation: specification/support generation, production type discovery and generation, constructor and static factory construction generation, typed proxy matcher support, and method skeleton generation.

> Current status: `javaspec run` still performs discovery, generation, and source updates. It does **not** execute full examples or a complete runner lifecycle yet.

## Quick start

From the repository root:

```sh
mvn test
mvn package
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
$javaspec describe <ClassName> [--spec-dir <dir>]
$javaspec desc <ClassName> [--spec-root <dir>]
$javaspec run [--spec-dir <dir>] [--source-dir <dir>] [--generate] [--constructor-policy <delete|preserve|comment>]
```

Aliases and defaults:

| Long | Alias | Default | Command |
|---|---|---|---|
| `describe` | `desc` | n/a | n/a |
| `--spec-dir` | `--spec-root` | `src/test/java` | `describe`, `run` |
| `--source-dir` | `--source-root` | `src/main/java` | `run` |
| `--generate` | n/a | `false` | `run` |
| `--constructor-policy <delete\|preserve\|comment>` | n/a | `comment` | `run` |

`describe` writes specification files only. Production source generation and updates belong to `run`.

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

### 2. Run discovery

```sh
$javaspec run
```

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

This writes missing production type skeletons, generated specification support updates, constructor updates, static factory construction method skeletons, and missing instance method skeletons inferred from specs without interactive confirmation.

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

`run` discovers typed proxy calls and construction factory markers, then can generate missing subject method skeletons. Discovery currently covers the supported matcher calls, typed throw calls such as `shouldThrow(...).duringSetRating(-3)`, direct `subject().method(...)` calls, simple setter-style calls, and static factory construction markers.

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

Empty generated/no-op unmatched constructors may be removed when safe, regardless of policy. Constructor policy applies to `run`; `describe` never updates production source.

## Matchers

Typed proxy methods return `Matchable<T>` for non-void subject methods. The documented matcher set includes:

- `shouldReturn`
- `shouldNotReturn`
- `shouldEqual`
- `shouldBeLike`
- `shouldContain`
- `shouldNotContain`
- `shouldStartWith`
- `shouldEndWith`
- `shouldMatchPattern`

Examples:

```java
getRating().shouldReturn(5);
getRating().shouldNotReturn(0);
getTitle().shouldContain("Wizard");
getTitle().shouldStartWith("The");
getTitle().shouldEndWith("Oz");
getTitle().shouldMatchPattern("Wiz.*");
```

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

javaspec follows a PHPSpec-inspired namespace convention.

| Spec file | Spec class | Support class | Described production type |
|---|---|---|---|
| `src/test/java/spec/org/example/CalculatorSpec.java` | `spec.org.example.CalculatorSpec` | `spec.org.example.CalculatorSpecSupport` | `org.example.Calculator` |
| `src/test/java/spec/com/acme/UserSpec.java` | `spec.com.acme.UserSpec` | `spec.com.acme.UserSpecSupport` | `com.acme.User` |

Rules:

1. The spec class name ends with `Spec`.
2. The generated support class name ends with `SpecSupport`.
3. The spec package starts with `spec.`.
4. The described production package is the spec package without the leading `spec.`.
5. The described production type name is the spec class name without the trailing `Spec`.
6. The described production kind defaults to class unless the spec contains a marker such as `shouldBeAFinalClass();`, `shouldBeAnInterface();`, `shouldBeAnEnum();`, `shouldBeAnAnnotation();`, `shouldBeARecord();`, `shouldBeASealedClass();`, or `shouldBeASealedInterface();`.
7. `shouldExtend(...)`, `shouldImplement(...)`, and `shouldPermit(...)` class literals are resolved through imports or the described production package.
8. Constructor and method descriptors are discovered heuristically from supported construction and typed proxy syntax: `beConstructedWith(...)` describes constructors; factory construction markers with string-literal Java-identifier names describe static factory methods; typed proxy, throw-proxy, direct `subject().method(...)`, and simple setter calls describe instance methods.

Legacy same-package specs are also discovered by convention, but new specs generated by `describe` use the `spec.` package prefix.

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
| `0` | success, help, no specs found, existing targets, or generated/updated targets |
| `1` | missing production type or missing method update was not generated because the prompt was declined or input was unavailable |
| `64` | invalid command line usage |
| `70` | I/O or security error while checking or writing files |

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

Current tester-reported verification after the factory construction generation follow-up:

- `mvn test` passed with 174 tests.
- `mvn dependency:tree -Dscope=runtime` passed with only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.

## Current MVP limitations

- The example runner lifecycle is still incomplete; `run` performs discovery, generation, and update work rather than executing full examples.
- Source parsing and generation use Java 8-compatible heuristics, not a full Java parser.
- Generated post-Java-8 source forms, such as records and sealed types, require an appropriate JDK to compile.
- Method generation covers the supported typed proxy, throw-proxy, direct subject/setter, and static factory construction marker syntax; it is not a general Java source synthesis engine.
- Doubles/collaborators are not implemented yet.
