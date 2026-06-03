# 12. Glossary

| Term | Meaning |
|---|---|
| ADR | Architecture Decision Record. javaspec ADRs live in `docs/adr/` and follow the Context, Decision, Consequences structure. |
| Annotation element | A no-argument method-like member of a Java annotation type. javaspec generates only compatible elements for annotation sources. |
| Bootstrap hook | A config value parsed as metadata for future runner lifecycle work. Bootstrap hooks are not executed in the current implementation. |
| Class filter | Repeatable `run --class <name>` filter matching described qualified/simple names or spec qualified/simple names. |
| Class-like type | A described production type kind: class, final class, interface, enum, annotation, record, sealed class, or sealed interface. |
| Constructor policy | `run` policy for unmatched constructors: `comment` (default), `preserve`, or explicit destructive `delete`. |
| Described type | The production Java type inferred from or targeted by a specification. |
| `describe` / `desc` | CLI command that creates specification/support skeletons only and never writes production source. |
| Direct matcher | An `ObjectBehavior` convenience assertion such as `shouldReturn(actual, expected)` that delegates through `match(actual)`. |
| Dry-run | `run --dry-run`; plans generation/update work without writes or prompts and exits `1` when pending work exists. |
| Effective classloader | The classloader visible to the CLI process. The runner can execute only spec classes available there. |
| Example | A public `void` Java spec method named `it_*` or `its_*`. |
| Example status | Runtime outcome: `PASSED`, `FAILED`, `BROKEN`, or `SKIPPED`. |
| Extension API | Programmatic contracts `JavaspecExtension`/`Extension` and `ExtensionContext`. External CLI extension discovery/loading is not implemented. |
| Formatter | A `RunFormatter` implementation. The CLI supports built-in `progress` and `pretty` names. |
| Interface double | A JDK dynamic proxy double for an ordinary Java interface under `org.javaspec.doubles`. |
| LTS profile | Target Java profile key: `java8`, `java11`, `java17`, `java21`, or `java25`. |
| Matchable | Fluent expectation wrapper returned by typed proxy methods and `match(actual)`. |
| Missing sealed-interface skeleton | A generated sealed-interface source file that can include root method declarations and nested permitted implementation bodies. |
| PHPSpec-inspired | Modeled after PHPSpec workflow concepts while adapted to Java packages, classes, static typing, compilation, and interfaces. |
| Profile catalog | Metadata model for Java LTS profiles, feature flags, and API symbols under `org.javaspec.profile`. |
| Reflection runner | Dependency-free runner that executes compiled spec examples by Java reflection after discovery/generation/update work. |
| Report | Optional UTF-8 JSON file written by `run --report` or `--report-file` with schemaVersion 1 runner results. |
| `run` | CLI command that discovers specs, owns production generation/update, can execute compiled examples, renders output, and can write reports. |
| Sealed-interface update deferral | Intentional limitation: existing sealed-interface source updates are skipped until nested permitted implementation updates can be done safely. |
| Source-only spec | A discovered spec source file whose compiled class is unavailable to the runner; examples are reported as skipped. |
| Spec package prefix | Suite naming prefix for generated/discovered spec classes, default `spec`. |
| Spec root | File-system root searched for specification sources, default `src/test/java`. |
| Suite | Configuration grouping for spec root, source root, spec package prefix, production package prefix, and bootstrap metadata. |
| Typed proxy | Generated support method that returns `Matchable<T>` for a subject method, enabling syntax like `getRating().shouldReturn(5)`. |
| Zero runtime dependency | Architectural policy that the runtime artifact depends only on the JDK and the project artifact itself. |
