# Extension SPI contract — 1.0

javaspec 1.0 keeps the extension surface deliberately small. Extensions may add run formatters,
bootstrap hooks, Java source parsers, and dependency resolvers. A typed suite/spec/example/row event
model is **not** part of 1.0; it is deferred so adapters cannot redefine javaspec semantics before
those events are stable.

## Stable 1.0 SPI packages

| Package | Classification | 1.0 contract |
|---|---|---|
| `io.github.jvmspec.extension` | `PUBLIC_SPI` | ServiceLoader and configured extension activation for registering formatter contributions. |
| `io.github.jvmspec.formatter` | `PUBLIC_SPI` | `RunFormatter` and `RunFormatterRegistry` for rendering final `RunResult` values. |
| `io.github.jvmspec.bootstrap` | `PUBLIC_SPI` | Per-run `BootstrapHook` execution before examples run. |
| `io.github.jvmspec.generation.parser` | `PUBLIC_SPI` | Optional source parser replacement/fallback for generation structural queries. |
| `io.github.jvmspec.resolver` | `PUBLIC_SPI` | Optional dependency descriptor resolver used by CLI classpath repair/resolution. |

`docs/api-surface-1.0.md` is the authoritative classification table. APIs not classified there are
not stable extension contracts.

## Extension and formatter activation

Built-in formatters are registered first: `progress`, `pretty`, then `json`.

After built-ins:

1. `ServiceLoader<RunFormatter>` providers are registered in ServiceLoader iteration order.
2. `ServiceLoader<JavaspecExtension>` providers are activated in ServiceLoader iteration order.
3. `ServiceLoader<Extension>` providers are activated in ServiceLoader iteration order.
4. Configured extension class names from the configuration/launcher are activated after discovery,
   in declaration order, with duplicate configured entries preserved.

When the same implementation class is listed under both `JavaspecExtension` and its `Extension`
alias through ServiceLoader, it is configured once. Configured duplicate class names are preserved
and activated once per occurrence.

A formatter name is trimmed and lower-cased with `Locale.ROOT`; blank names fail. Registering an
existing formatter name replaces the previous formatter for that name while preserving registry order.
Formatter output receives the final `RunResult`; formatters do not receive per-example callbacks in
1.0.

## Classloader and cleanup semantics

Extension discovery and configured activation use the effective run classloader. During discovery and
activation, javaspec temporarily sets the thread context classloader to that effective classloader and
restores the previous thread context classloader in a `finally` block after success or failure.

Configured extension classes are loaded with `Class.forName(name, true, effectiveRunClassLoader)` and
must expose a public no-argument constructor.

## Extension failure semantics

Extension discovery or activation failures fail the run before examples execute and before JSON/JUnit
reports are written for that run. CLI users receive usage exit code `64` for extension activation
errors.

Failure diagnostics name the offending provider or configured class when possible:

- invalid ServiceLoader configuration or missing provider class;
- formatter provider with blank/invalid formatter name;
- configured class not found on the run classpath;
- configured class that does not implement `JavaspecExtension` or `Extension`;
- missing public no-argument constructor;
- constructor or `configure(ExtensionContext)` failure.

`list-extensions` remains the diagnostic command for discovered providers and classpath-repair hints.

## Bootstrap hooks

Explicit bootstrap hooks run before ServiceLoader-discovered hooks and before examples. Explicit hook
order is declaration order and duplicates are preserved.

ServiceLoader bootstrap discovery is opt-in (`bootstrapDiscovery=true`). When enabled,
`ServiceLoader<BootstrapHook>` providers run after explicit hooks in deterministic provider
implementation class-name order; discovery index is only a tie-breaker.

Bootstrap execution temporarily sets the thread context classloader to the run classloader and
restores the previous value in a `finally` block after success or failure. Bootstrap failures fail the
run before examples execute and are reported as bootstrap errors.

## Java source parser SPI

`JavaSourceParserLoader` checks ServiceLoader parser providers before the built-in
`CommentStrippingSourceParser`. The first provider whose `isAvailable()` returns `true` is selected;
broken providers encountered during provider instantiation are skipped. If no external provider is
available, the built-in parser is used.

Parser implementations receive the original source text and must preserve javaspec generation safety:
comments, strings, nested types, records, sealed types, no-final-newline files, CRLF, Unicode, and
modern Java syntax must not lead to destructive source edits. A parser that cannot guarantee safe
answers should report `isAvailable() == false`.

## Dependency resolver SPI

`DependencyResolverLoader` tries the built-in `LocalMavenRepoResolver` first for supported Maven
`pom.xml` descriptors. External `ServiceLoader<DependencyResolver>` providers are consulted only when
the built-in resolver does not support the descriptor, in ServiceLoader order. The first provider
whose `supports(File)` returns `true` is used.

Resolver failures are not swallowed: dependency-resolution errors surface as run/classpath failures
because running with a partial or silently repaired classpath would be misleading.

## Deferred event model v2

The following are explicitly deferred from 1.0 and must not be promised by adapters or extensions as
stable contracts:

- typed suite/spec/example/row lifecycle events;
- per-row execution callbacks separate from the owning example body;
- generation-plan/reporting events;
- listener priority/ordering APIs beyond the SPI ordering documented above;
- extension-managed cleanup callbacks beyond normal Java `try/finally` in extension code.

Adapters must continue to translate the stable javaspec `RunResult`/report semantics instead of
inventing Jupiter-like lifecycle semantics.
