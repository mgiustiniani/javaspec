# Example data contract — 1.0

Example data is the javaspec Java adaptation of PHPSpec example data/tables. It keeps multiple
concrete cases inside one subject-centric behavior example without adopting Jupiter parameterized
annotations or a separate test-template lifecycle.

## Authoring API

The stable 1.0 authoring surface is in `io.github.jvmspec.api`:

- `row(A)` and `row(A, B)` create one- and two-column rows.
- `examples(row(...), ...)` creates an ordered row set.
- `Example1<A>` and `Example2<A, B>` are Java 8 functional interfaces for row bodies.

Rows are verified from inside a normal `it_*` / `its_*` behavior method:

```java
public void it_normalizes_names() {
    examples(row("Alice", "Alice"), row(" Bob ", "Bob"))
        .verify(new Example2<String, String>() {
            public void run(String input, String expected) {
                match(subject().normalize(input)).shouldReturn(expected);
            }
        });
}
```

## Execution semantics

Rows execute inline in the owning example method, in declaration order. The owning example lifecycle
(`let`, construction, collaborator injection, automatic prophecy predictions, and `letGo`) remains
one javaspec example lifecycle. Rows do not create separate subject instances or separate lifecycle
runs.

A failing or broken row records that row result and stops the row set by throwing from the owning
example body. Later rows are not executed after the first row failure. This fail-fast behavior keeps
normal Java control flow and avoids pretending that rows are isolated test-template invocations.

## Diagnostics

Failing row messages include row index and row values:

```text
Example data row 2 [ Bob , Robert] failed: Expected equality(Robert) but got Bob
```

Row indexes are one-based and stable within the owning example's row declaration order. Row
descriptions use the row values' string representation.

## JSON report contract

JSON reports keep the owning example as the canonical result and include row details in
`exampleDataRows` when rows were recorded.

Each row entry has:

- `index`: one-based row index;
- `description`: rendered row values;
- `status`: `PASSED`, `FAILED`, or `BROKEN`;
- `detail`: failure/broken detail when present.

The example stable id remains `<specQualifiedName>#<methodName>`. Row identity is represented by the
owning example id plus the row index.

## JUnit XML report contract

JUnit XML reports expose rows as testcase entries so CI systems can display row-level diagnostics.
The owning behavior method remains the semantic source of the rows; XML row testcases are reporting
projections and must not be interpreted as separate javaspec lifecycles.

## JUnit Platform adapter contract

The optional JUnit Platform engine keeps javaspec semantics and translates row diagnostics for IDEs:

- Row unique id shape: `[engine:javaspec]/[spec:<specQualifiedName>]/[example:<methodName>]/[row:<index>]`.
- Dynamic row descriptors are registered during execution from recorded row results.
- Row unique-id selectors select the owning example for execution.
- Row unique-id selectors filter row descriptor/event publication to the selected row indexes.
- Row unique-id selectors do **not** isolate execution to that row; all rows still execute inline in
  the owning example until normal row fail-fast behavior stops the example.

This means selecting row 2 can still fail because row 1 failed first. In that case row 2 may not be
published because the owning example never reached row 2. This is intentional: the adapter may filter
JUnit descriptors/events, but it must not redefine javaspec example-data execution as Jupiter
parameterized execution.

## Boundaries

Out of scope for 1.0:

- Jupiter `@ParameterizedTest`, `@MethodSource`, or test-template semantics in core.
- Per-row subject construction/lifecycle isolation.
- Per-row collaborator prediction registries.
- Cucumber/Gherkin Scenario Outline support in core.
- Row selectors for the CLI/Maven/Gradle runner; row selectors are an optional JUnit Platform adapter
  projection only.
