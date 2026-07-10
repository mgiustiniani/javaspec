# Cucumber/Gherkin boundary — 1.0

javaspec 1.0 deliberately does not include a Cucumber/Gherkin runner, feature files, step definitions,
or Given/When/Then DSL in core.

## Why

javaspec's product identity is PHPSpec-first and subject-centric. A spec describes one subject type and
its behavior through executable Java methods. Gherkin describes cross-role scenarios and acceptance
criteria. Both can be valuable, but they are different models.

## What javaspec supports instead

For small data tables inside one behavior example, use PHPSpec-style example data:

```java
public void it_normalizes_known_inputs() {
    examples(row(" Alice ", "Alice"), row("Bob", "Bob"))
        .verify(new Example2<String, String>() {
            public void run(String input, String expected) {
                match(subject().normalize(input)).shouldReturn(expected);
            }
        });
}
```

For collaborators, use interface doubles or generated typed Prophecy wrappers. For build/IDE
integration, use the optional Maven, Gradle, or JUnit Platform adapters.

## Migration guidance

Low-value `Scenario Outline` cases that only vary inputs/outputs of one subject method can usually be
converted to `examples(row(...))`. Higher-level acceptance scenarios should remain in a dedicated BDD
acceptance tool outside javaspec.

## 1.0 boundary

Out of scope:

- `.feature` file parsing;
- step-definition discovery/execution;
- Given/When/Then annotations or DSL in core;
- Cucumber report adapters;
- mapping javaspec examples to Gherkin scenario semantics.

This boundary keeps the core zero-runtime-dependency and avoids redefining javaspec as a general BDD
acceptance runner.
