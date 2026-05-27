# javaspec User Manual

Wiki home for the current javaspec MVP.

javaspec is a Java 8-compatible, zero-runtime-dependency specification tool inspired by PHPSpec. The current MVP focuses on the first BDD loop:

1. describe a type;
2. generate a specification skeleton only;
3. run specs discovery;
4. when a described production type is missing, ask whether to generate it.

> Current status: this MVP does not execute examples yet. `run` discovers spec files, infers the described production type, and handles missing production type generation.

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
$javaspec run [--spec-dir <dir>] [--source-dir <dir>] [--generate]
```

Aliases:

| Long | Alias | Default |
|---|---|---|
| `describe` | `desc` | n/a |
| `--spec-dir` | `--spec-root` | `src/test/java` |
| `--source-dir` | `--source-root` | `src/main/java` |

## BDD workflow

### 1. Describe a class

```sh
$javaspec describe org.example.Calculator
```

or:

```sh
$javaspec desc org.example.Calculator
```

This creates only the spec skeleton:

```text
src/test/java/spec/org/example/CalculatorSpec.java
```

Generated spec:

```java
package spec.org.example;

import org.example.Calculator;
import org.javaspec.api.ObjectBehavior;

public class CalculatorSpec extends ObjectBehavior<Calculator> {
    public void it_is_initializable() {
        shouldHaveType(Calculator.class);
    }
}
```

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

This writes missing production type skeletons inferred from specs.

## Class-like type generation

javaspec currently supports these class-like production types. The javaspec binary remains Java 8-compatible; post-Java 8 forms are generated as source text and represented as metadata/strings.

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
package spec.org.example;

import org.example.BaseService;
import org.example.PaymentGateway;
import org.example.Service;
import org.javaspec.api.ObjectBehavior;

public class ServiceSpec extends ObjectBehavior<Service> {
    public void it_is_initializable() {
        shouldHaveType(Service.class);
    }

    public void it_extends_base_service() {
        shouldExtend(BaseService.class);
    }

    public void it_implements_payment_gateway() {
        shouldImplement(PaymentGateway.class);
    }
}
```

When `BaseService` and `PaymentGateway` do not exist, `run` handles them first:

1. suggest/create `BaseServiceSpec` and `PaymentGatewaySpec`;
2. then suggest/create the production skeletons;
3. finally generate `Service` with the declared relationships.

With `--generate`, the flow is non-interactive:

```sh
$javaspec run --generate
```

Possible generated files:

```java
package org.example;

public class BaseService { }
```

```java
package org.example;

public interface PaymentGateway { }
```

```java
package org.example;

public class Service extends BaseService implements PaymentGateway { }
```

`shouldExtend(...)` may contain multiple interfaces when the described type is an interface. For classes, only the first extended type is used because Java classes support a single superclass.

### Interface example

Start with the same command:

```sh
$javaspec desc org.example.PaymentGateway
```

Then edit the spec:

```java
package spec.org.example;

import org.example.PaymentGateway;
import org.javaspec.api.ObjectBehavior;

public class PaymentGatewaySpec extends ObjectBehavior<PaymentGateway> {
    public void it_is_initializable() {
        shouldHaveType(PaymentGateway.class);
    }

    public void it_is_an_interface() {
        shouldBeAnInterface();
    }
}
```

Run generation:

```sh
$javaspec run --generate
```

Output includes:

```text
spec.org.example.PaymentGatewaySpec describes missing interface org.example.PaymentGateway.
Generated interface skeleton: src/main/java/org/example/PaymentGateway.java
```

Generated production type:

```java
package org.example;

public interface PaymentGateway { }
```

### Enum example

Spec marker:

```java
public void it_is_an_enum() {
    shouldBeAnEnum();
}
```

Generated production type:

```java
package org.example;

public enum OrderStatus { }
```

### Annotation example

Spec marker:

```java
public void it_is_an_annotation() {
    shouldBeAnAnnotation();
}
```

Generated production type:

```java
package org.example;

public @interface Experimental { }
```

### Record example

Spec marker:

```java
public void it_is_a_record() {
    shouldBeARecord();
}
```

Generated production type:

```java
package org.example;

public record User() { }
```

This source requires a Java version that supports records.

### Sealed class example

Sealed types require a `permits` list. Prefer making the permitted subtypes explicit in the spec. Missing permitted subtypes get their own specs first and are then generated as `final` classes extending or implementing the sealed root:

```java
package spec.org.example;

import org.example.Circle;
import org.example.Rectangle;
import org.example.Shape;
import org.javaspec.api.ObjectBehavior;

public class ShapeSpec extends ObjectBehavior<Shape> {
    public void it_is_initializable() {
        shouldHaveType(Shape.class);
    }

    public void it_is_sealed() {
        shouldBeASealedClass();
        shouldPermit(Circle.class, Rectangle.class);
    }
}
```

Generated production type:

```java
package org.example;

public sealed class Shape permits Circle, Rectangle { }
```

If `Circle` or `Rectangle` are missing, `run` suggests/creates their specs before generating production classes. For a sealed class, generated permitted subtype specs include:

```java
shouldBeAFinalClass();
shouldExtend(Shape.class);
```

and the generated subtype is valid as a direct sealed subclass:

```java
package org.example;

public final class Circle extends Shape { }
```

If `shouldPermit(...)` is omitted, javaspec creates a nested `Permitted` placeholder so the generated sealed root has a syntactically valid permits clause:

```java
package org.example;

public sealed class Shape permits Shape.Permitted {
    static final class Permitted extends Shape { }
}
```

This source requires a Java version that supports sealed classes. Permitted subclasses must still be implemented as `final`, `sealed`, or `non-sealed` types.

### Sealed interface example

For sealed interfaces, javaspec keeps permitted implementations in the same production file. This is the only sealed case where javaspec intentionally keeps the permitted types local to the root type.

Spec marker with explicit permits:

```java
public void it_is_a_sealed_interface() {
    shouldBeASealedInterface();
    shouldPermit(EmailMessage.class, SmsMessage.class);
}
```

Generated production type:

```java
package org.example;

public sealed interface Message permits Message.EmailMessage, Message.SmsMessage {
    final class EmailMessage implements Message { }
    final class SmsMessage implements Message { }
}
```

If `shouldPermit(...)` is omitted, javaspec creates a nested `Permitted` placeholder:

```java
package org.example;

public sealed interface Message permits Message.Permitted {
    final class Permitted implements Message { }
}
```

This source requires a Java version that supports sealed interfaces. javaspec does not create separate specs or separate production files for the permitted implementations of a sealed interface in this MVP.

## Custom directories

### Custom spec directory

```sh
$javaspec describe org.example.Calculator --spec-dir /tmp/demo/src/test/java
```

Creates:

```text
/tmp/demo/src/test/java/spec/org/example/CalculatorSpec.java
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

javaspec reports it and does not overwrite it:

```text
Specification spec.org.example.CalculatorSpec exists; no generation needed.
Spec file: src/test/java/spec/org/example/CalculatorSpec.java
No production class was generated.
```

## Existing production class

If a spec exists and the production class already exists in the source tree:

```text
src/test/java/spec/org/example/CalculatorSpec.java
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

No production files are generated.

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

| Spec file | Spec class | Described production type |
|---|---|---|
| `src/test/java/spec/org/example/CalculatorSpec.java` | `spec.org.example.CalculatorSpec` | `org.example.Calculator` |
| `src/test/java/spec/com/acme/UserSpec.java` | `spec.com.acme.UserSpec` | `com.acme.User` |

Rules:

1. The spec class name ends with `Spec`.
2. The spec package starts with `spec.`.
3. The described production package is the spec package without the leading `spec.`.
4. The described production type name is the spec class name without the trailing `Spec`.
5. The described production kind defaults to class unless the spec contains a marker such as `shouldBeAFinalClass();`, `shouldBeAnInterface();`, `shouldBeAnEnum();`, `shouldBeAnAnnotation();`, `shouldBeARecord();`, `shouldBeASealedClass();`, or `shouldBeASealedInterface();`.
6. `shouldExtend(...)`, `shouldImplement(...)`, and `shouldPermit(...)` class literals are resolved through imports or the described production package.

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
| `0` | success, help, no specs found, existing targets, or generated targets |
| `1` | missing production type was not generated because the prompt was declined or input was unavailable |
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

## Current MVP limitations

- `run` does not execute example methods yet.
- No full matcher engine yet; generic `ObjectBehavior<T>`, `shouldHaveType(Class<?>)`, and the class-like type/relationship marker methods are placeholders for generated specs and discovery.
- No doubles/collaborators yet.
- No method or constructor generation yet.
- Record and sealed skeletons are generated as source text for newer Java targets; javaspec itself still compiles and runs as a Java 8-compatible binary.
- Production generation is limited to minimal class, interface, enum, annotation, record, sealed class, and sealed interface skeletons.
