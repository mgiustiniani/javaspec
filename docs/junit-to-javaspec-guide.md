# JUnit-to-javaspec guide — 1.0

javaspec is PHPSpec-first. It is not a Jupiter replacement with different annotations. The migration
path is to describe subject behavior through `ObjectBehavior<T>`, `subject()`, generated support
helpers, and javaspec matchers.

## Basic mapping

| JUnit/Jupiter habit | javaspec style |
|---|---|
| `@Test void adds()` | `public void it_adds_numbers()` |
| Test class constructs subject manually | Spec extends `ObjectBehavior<Subject>` and uses `subject()` |
| `@BeforeEach` | `public void let()` |
| `@AfterEach` | `public void letGo()` |
| `assertEquals(expected, actual)` | `match(actual).shouldReturn(expected)` or generated `method().shouldReturn(expected)` |
| `assertThrows` | generated `shouldThrow(...).during...` helpers |
| `@ParameterizedTest` | PHPSpec-style `examples(row(...)).verify(...)` |
| Mocks | Interface doubles or generated typed Prophecy wrappers |

## Example

JUnit-style:

```java
@Test
void addsNumbers() {
    Calculator calculator = new Calculator();
    assertEquals(5, calculator.add(2, 3));
}
```

javaspec-style:

```java
public final class CalculatorSpec extends ObjectBehavior<Calculator> {
    public CalculatorSpec() { super(Calculator.class); }

    public void it_adds_numbers() {
        match(subject().add(2, 3)).shouldReturn(5);
    }
}
```

## What not to migrate mechanically

- Do not add Jupiter annotations to javaspec specs.
- Do not treat JUnit Platform row descriptors as parameterized-test invocations.
- Do not replace subject-centric behavior names with generic test utility names.
- Do not move collaborator setup into an application DI container for spec execution.

## Running through JUnit Platform

The optional JUnit Platform engine lets IDEs and build tools discover/run javaspec specs. It remains
an adapter; see `docs/junit-platform-contract-1.0.md` for selector, unique-id, source, and status
mapping rules.
