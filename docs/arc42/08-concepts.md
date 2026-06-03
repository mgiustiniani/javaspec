# 8. Concepts

## 8.1 Java 8-Compatible Core

All production code is written for Java 8 source/target compatibility. Newer JDK capabilities are modeled as profile metadata, generated source text, or reflection-only probes. This keeps the runtime binary usable on Java 8 while allowing javaspec to understand Java 11, 17, 21, and 25 concepts.

## 8.2 Zero Runtime Dependencies

The core runtime depends only on the JDK. This affects every feature:

- Configuration uses a restricted internal line-based parser instead of YAML/TOML/JSON libraries.
- Doubles use JDK dynamic proxies instead of bytecode libraries.
- JSON reports are written by an internal UTF-8 writer instead of a JSON library.
- CLI parsing, formatting, matchers, and extension contracts are implemented with JDK APIs.

## 8.3 PHPSpec-Inspired Java Workflow

javaspec keeps the PHPSpec workflow shape but adapts it to Java:

| PHPSpec concept | javaspec Java concept |
|---|---|
| `describe` command | `javaspec describe` / `desc` creates Java spec/support skeletons only. |
| Subject as `$this` | `ObjectBehavior<T>` lazy subject plus generated typed support methods and explicit `subject()`. |
| PHP namespaces | Java packages plus configurable spec and production package prefixes. |
| Examples | Public `void` Java methods named `it_*` or `its_*`. |
| Construction customization | `beConstructedWith(...)`, `beConstructedThrough(...)`, `beConstructedNamed(...)`, and `beConstructedThroughNamed(...)` before subject instantiation. |
| Matcher syntax | `getValue().shouldReturn(...)`, `match(value).should...`, and direct `ObjectBehavior` convenience assertions. |
| Collaborator doubles | Interface-only JDK-proxy doubles in the zero-dependency core. |
| Generation prompts | Production generation/update belongs to `run`, gated by confirmation or `--generate`; `--dry-run` reports planned work without writing. |

The user manual contains practical migration notes for PHPSpec users.

## 8.4 Describe/Run Ownership Split

The architecture preserves a strict command split:

- `describe` creates or finds specification/support files and never writes production source.
- `run` discovers specs, handles generation/update planning, prompts or uses `--generate`, executes compiled examples when available, renders output, writes optional reports, and returns stable exit codes.

This split is central to ADR 0003 and ADR 0008.

## 8.5 Configuration and Suite Naming

Configuration is suite-oriented. Each suite provides spec/source roots and package-prefix naming metadata. The active `SpecNamingConvention` maps production classes to spec/support classes and maps discovered spec classes back to described production types.

Path options can override selected-suite roots, but naming still comes from the selected suite. Constructor policy, profile, and formatter defaults are loaded from config and can be overridden by run CLI options. Bootstrap hooks are parsed metadata only and are not executed yet.

## 8.6 Construction Semantics

Generated support classes pass the described subject `Class<T>` to `ObjectBehavior<T>`. The subject is constructed lazily on first access, so construction rules can be configured before instantiation.

Rules:

- `beConstructedWith(...)` describes constructor arguments.
- `beConstructedThrough(...)`, `beConstructedNamed(...)`, and `beConstructedThroughNamed(...)` describe static factories at runtime and generate factory skeletons only when the factory name is a string-literal valid Java identifier.
- The last construction rule before instantiation wins.
- Changing construction after instantiation is an error.
- `shouldThrow(...).duringInstantiation()` specifies constructor or factory exceptions.

## 8.7 Matcher and Assertion Semantics

`Matchable<T>` is the fluent expectation wrapper for typed proxies and explicit `match(actual)` calls. It includes the implemented subset of equality/identity aliases, negations, type/instance/implementation checks, containment, string helpers, count/empty helpers, and map key/value helpers.

`ObjectBehavior` direct convenience assertions delegate through `match(actual)`, which keeps direct and fluent matcher semantics synchronized. Custom matchers can be registered without adding runtime dependencies and may evaluate null subjects.

Known matcher limitation: count and emptiness checks on a generic `Iterable` consume the iterable and can hang on infinite iterables.

## 8.8 Generation Semantics

Generation is deterministic and reviewable:

- Missing production class-like type skeletons are generated only by `run` after confirmation or `--generate`.
- Constructor handling follows the selected policy: `comment` (default), `preserve`, or `delete`; destructive deletion requires explicit `delete`.
- Empty generated/no-op unmatched constructors may be removed when safe.
- Method generation uses Java default returns for generated method bodies.
- Interface-style generation emits declarations or annotation elements where valid.
- Missing sealed-interface skeletons include nested permitted implementation bodies; existing sealed-interface updates are deferred.

Source parsing/generation uses Java 8-compatible heuristics rather than a full Java parser.

## 8.9 Runner, Results, Formatters, and Reports

The runner result model separates discovery from output:

- `SpecRunner` produces immutable `RunResult`, `SpecResult`, and `ExampleResult` data.
- Built-in `progress` and `pretty` output render those results through `RunFormatter` implementations.
- JSON reports with `schemaVersion` 1 are written from the same results.
- Report failures are I/O failures and exit `70`.

Source-only or non-loadable compiled spec classes produce skipped examples because javaspec is not an in-process compiler.

## 8.10 Interface Doubles Concept

Core doubles intentionally support ordinary Java interfaces only. The design favors explicit limits over hidden dependencies:

- JDK dynamic proxies implement interface doubles.
- Stubbing is by method name or exact arguments.
- Calls are recorded as immutable snapshots.
- Verification supports called/not-called/called-once/exact-count checks.
- Unstubbed methods return Java defaults.

Concrete class, final class, static method, constructor, primitive, array, annotation, and enum doubles are outside the core runtime.

## 8.11 Extension Boundary

The current extension API is programmatic. `JavaspecExtension`/`Extension` can configure an `ExtensionContext`, and the context exposes the run formatter registry.

Not implemented in the current architecture:

- Configuration-driven extension activation.
- External CLI extension discovery/loading.
- Classpath scanning.
- `ServiceLoader` integration.
- Plugin lookup.
- CLI formatter selection for extension-provided names.
