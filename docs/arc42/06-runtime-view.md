# 6. Runtime View

This section describes the implemented runtime scenarios without C4 diagrams. The runtime is a Java 8-compatible CLI and library surface with no third-party runtime dependencies.

## 6.1 `describe` Scenario

1. The user runs `javaspec describe <ClassName>` or `javaspec desc <ClassName>`.
2. The CLI parses command-line arguments, loads an optional restricted configuration file, selects the active suite, and applies the selected suite's spec root and naming convention. Command-line spec-root options override paths only.
3. The described Java name is validated through the model boundary.
4. The naming convention maps the production type to `*Spec.java` and `*SpecSupport.java` under the active spec package prefix.
5. Missing spec/support skeletons are written. Existing spec files are not overwritten; missing support files may still be created.
6. No production source is generated or updated. Run-only controls are rejected.

Important runtime invariant: `describe` is specification/support generation only. Production type, constructor, factory, method, execution, formatter, report, and dry-run behavior belongs to `run`.

## 6.2 `run` Discovery, Generation, and Execution Scenario

1. The user runs `javaspec run` with optional config, suite, path overrides, generation flags, filters, run controls, or report path.
2. The CLI loads inferred defaults or the configured suite. `--constructor-policy`, `--profile`, and `--formatter` override valid configured/default values when supplied.
3. `SpecDiscovery` scans the selected spec root using the active naming convention and filters by suite, class filters, and example filters.
4. Discovery extracts described production type metadata, kind markers, relationship markers, construction markers, factory construction markers, typed proxy/throw proxy calls, direct subject/setter calls, and public `void` `it_*`/`its_*` example metadata.
5. Existence checks inspect the source root and effective classloader for described production types and related types.
6. Generation planning determines missing or updatable work: related specs/support, production type skeletons, support updates, constructors, static factory skeletons, class-like method bodies, ordinary-interface declarations, annotation elements, and missing sealed-interface skeleton declarations plus nested implementation bodies.
7. If `--dry-run` is active, the CLI reports pending work without writing files or prompting. Pending work exits `1`; if no pending work exists, execution may proceed.
8. If `--generate` is active, supported missing generation/update work is written non-interactively. Otherwise `run` prompts before production generation/update where required.
9. After generation/update work completes without a declined or unavailable prompt, the reflection runner attempts to load compiled spec classes from the effective classloader.
10. Loadable specs execute filtered examples. Source-only or otherwise unavailable specs are marked `SKIPPED` because the CLI does not compile source/spec files itself.
11. Built-in output is rendered through the selected run formatter. Optional JSON reports are written after no-spec output or runner summary rendering when the run reaches reportable execution/no-spec handling.
12. The process exits with the documented code: `0`, `1`, `64`, or `70`.

## 6.3 Example Execution Scenario

The reflection runner uses source-discovered metadata as the execution source of truth:

1. For each filtered `DiscoveredSpec`, the runner attempts to load the compiled spec class.
2. A non-loadable spec yields skipped examples.
3. For each executable example, a fresh spec instance is created through the spec class no-argument constructor.
4. Optional public no-argument `let()` runs before the example.
5. The public no-argument example method runs.
6. Optional public no-argument `letGo()` runs after the example, including after failures.
7. `AssertionError` is reported as `FAILED`.
8. Non-assertion throwables from lifecycle, example body, instantiation, or reflection are reported as `BROKEN`.
9. Missing reflected example methods are reported as `SKIPPED`.
10. With `--stop-on-failure`, execution stops after the first FAILED or BROKEN executable example.

## 6.4 Construction and Typed Matcher Scenario

1. Generated support classes extend `ObjectBehavior<Subject>` and pass `Subject.class` to the base class.
2. The subject is constructed lazily on first access.
3. `beConstructedWith(...)`, `beConstructedThrough(...)`, `beConstructedNamed(...)`, and `beConstructedThroughNamed(...)` configure construction before instantiation.
4. A later construction rule before first subject access overrides an earlier rule. Changing construction after instantiation is an error.
5. `shouldThrow(...).duringInstantiation()` captures constructor or factory failures.
6. Generated support methods expose typed subject proxies such as `getRating().shouldReturn(5)` and typed throw proxies such as `shouldThrow(...).duringSetRating(-3)`.
7. Explicit `match(actual).should...` usage remains available and shares matcher behavior with typed proxies.

## 6.5 Method Generation Scenario

Method generation is driven by discovered construction and typed subject syntax:

- `beConstructedWith(...)` describes constructors.
- `beConstructedThrough("name", ...)`, `beConstructedNamed("name", ...)`, and `beConstructedThroughNamed("name", ...)` describe static factory methods when the name is a string literal and valid Java identifier.
- Typed proxy, throw proxy, direct `subject().method(...)`, and simple setter calls can describe missing instance methods.

Generation output depends on the described production kind:

| Production kind | Generation behavior |
|---|---|
| Class, final class, sealed class, enum, record | Java method bodies with Java default returns where supported. |
| Ordinary interface | Non-static method declarations ending in `;`; static descriptors are skipped. |
| Annotation | Compatible no-argument non-static elements; incompatible descriptors are ignored. |
| Missing sealed interface | Root declarations plus nested permitted implementation bodies with Java default returns. |
| Existing sealed interface | Source updates intentionally skipped until nested permitted implementations can also be updated safely. |

## 6.6 Interface Double Scenario

1. User code creates a double for an ordinary interface through `Doubles` or `ObjectBehavior` convenience APIs.
2. The doubles engine validates that the target is an ordinary interface.
3. A JDK dynamic proxy records calls through the invocation handler.
4. Stubs are resolved by method name with any arguments or by method name with exact arguments. Exact matching supports `null` values and array-content comparison.
5. Unstubbed methods return Java defaults.
6. Call snapshots can be inspected and verified for called, not called, called once, or exact count.
7. `toString`, `equals`, and `hashCode` are handled deterministically and are not treated as user collaborator calls.

Unsupported target kinds fail fast with diagnostics rather than using bytecode libraries.

## 6.7 Reporting and Extension Runtime Scenario

- Built-in CLI output is selected from `progress` or `pretty` and rendered through `RunFormatter` implementations registered in `RunFormatterRegistry`.
- `--report` and `--report-file` write UTF-8 JSON reports with `schemaVersion` 1 from the immutable runner result model.
- No-spec, passing, failing, broken, and skipped-only runs write reports after normal output. Dry-run pending generation/update exits before execution and does not write a report.
- Report write failures are I/O failures and exit `70`.
- `JavaspecExtension`/`Extension` and `ExtensionContext` support programmatic formatter registration. External CLI extension discovery/loading is not implemented.

## 6.8 Profile and Compatibility Probe Scenario

1. The active profile is loaded from defaults, config, or `--profile`.
2. Profile values are validated against `java8`, `java11`, `java17`, `java21`, and `java25`.
3. Profile metadata and API-symbol catalog lookups use strings and Java 8-compatible domain objects.
4. Optional runtime availability checks use `ApiAvailabilityProbe` with class, method, or field names.
5. Post-Java-8 APIs are never imported directly by production code.

Current limitation: profile selection is visible and validated but not deeply enforced during example execution.
