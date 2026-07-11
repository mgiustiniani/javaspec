# PHPSpec compatibility charter

This charter defines the product direction for javaspec as a PHPSpec-first framework for Java. PHPSpec semantics are the product core, not an optional compatibility layer. The verifiable release matrix is maintained in [`docs/phpspec-compatibility-matrix.md`](phpspec-compatibility-matrix.md).

## Positioning

javaspec is a Java implementation of a spec-first, subject-centric, behavior-driven experience inspired by PHPSpec. It is intended for precise behavior slices close to code: domain objects, value objects, collaborators, edge cases, and design feedback loops where Cucumber/Gherkin adds ceremony instead of clarity.

javaspec complements, rather than replaces, Cucumber:

- Use Cucumber for cross-role executable requirements, business-readable workflows, and system-level acceptance language.
- Use javaspec for object/domain behavior, collaborator interactions, and disciplined RED/GREEN/REFACTOR design feedback.

javaspec also complements, rather than clones, JUnit:

- Use the optional JUnit Platform engine for IDE/build-tool interoperability.
- Keep PHPSpec-style syntax and workflow as the canonical authoring model.

## Design constraints

1. **Subject-centric examples** — specs describe one subject's behavior. `subject()`, generated support proxies, and `match(...).should*` remain central.
2. **PHPSpec naming** — `it_*`, `its_*`, `let`, `letGo`, `beConstructedWith`, `beConstructedThrough`, `prophesize*`, `reveal`, `will*`, and `should*` are the preferred vocabulary.
3. **Disciplined TDD/BDD workflow** — the tool should encourage one behavior, meaningful RED, minimal GREEN, refactor, repeat.
4. **Generation as guidance** — generation may create boring skeletons and snippets, but must not hide the need for user-written behavior.
5. **No Jupiter clone in core** — core must not adopt `@Test`, `@BeforeEach`, `@ParameterizedTest`, or Jupiter extension APIs as canonical syntax.
6. **No Gherkin runner in core** — core must not add feature files, step definitions, or Given/When/Then DSLs.
7. **Zero runtime dependencies in core** — dependency-heavy capabilities remain optional adapters.

## Canonical Java adaptation

Because Java cannot redefine `$this`, javaspec uses explicit subject APIs:

```java
public class PriceCalculatorSpec extends PriceCalculatorSpecSupport {
    public void let() {
        beConstructedWith("USD");
    }

    public void it_calculates_a_total() {
        total(10.0, 2.5).shouldReturn(12.5);
    }

    public void letGo() {
        // optional cleanup
    }
}
```

The explicit equivalent remains valid and should always be supported:

```java
match(subject().total(10.0, 2.5)).shouldReturn(12.5);
```

## JUnit parity by translation

JUnit-level tooling support should be achieved by translating the PHPSpec model into external tooling concepts:

| External need | javaspec direction |
|---|---|
| IDE run/debug | JUnit Platform adapter maps suite/spec/example/row descriptors. |
| Parameterized tests | PHPSpec-style example data under one behavior example, not Jupiter annotations. |
| Extensions | PHPSpec-style events/listeners and registries, not Jupiter callbacks. |
| Reports | Formatters/report sinks over javaspec result taxonomy. |
| Tags/filters | Suite/spec/example metadata mapped to CLI and adapter filters. |

## Cucumber boundary

Do not turn object behavior into feature-file ceremony. A useful rule of thumb:

- If a non-technical stakeholder must read or approve the behavior, consider Cucumber.
- If the behavior is a small object rule, invariant, collaborator contract, or edge case, prefer javaspec.

Low-value `Scenario Outline` usage may be translated into PHPSpec-style example data, but the public method remains an `it_*` / `its_*` behavior example:

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

## 1.0 semantic core

For 1.0, the PHPSpec-first semantic core is the primary release criterion. CLI, generation, reports, Maven, Gradle, JUnit Platform, doubles, and extension SPI must serve this subject-centric model rather than redefine it. The release can defer non-essential parity only when the compatibility matrix records the difference, the main spec -> RED -> skeleton -> GREEN loop remains coherent, and public documentation does not imply unsupported behavior.

## Workflow guardrails

Future features should be evaluated against these guardrails:

- Does the feature preserve a meaningful RED before GREEN?
- Does it keep one behavior visible as one example?
- Does it avoid batching unrelated production changes?
- Does it make accidental GREEN behavior obvious?
- Does it improve design feedback rather than only assertion volume?

Features that primarily support after-the-fact assertion dumping, large behavior batches, or hidden generation side effects are out of scope for the core authoring model.
