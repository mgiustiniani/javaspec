# 0018 — ServiceLoader external formatter and extension discovery

## Status

Accepted

## Context

ADR 0010 exposed the public `RunFormatter` contract, `RunFormatterRegistry`, `JavaspecExtension`/`Extension`, and `ExtensionContext`, but kept extension usage programmatic. Phase 25 needs externally packaged run formatters to be usable by the CLI and the Gradle adapter while preserving the Java 8 baseline, zero-runtime-dependency core, deterministic built-in defaults, and existing report/runner semantics.

The core must not add a plugin framework, classpath scanner, dependency-injection container, YAML/TOML/JSON configuration mechanism, or publishing-time dependency. Discovery must happen from the classloader that will run specs so users can place formatter or extension jars beside compiled specs and dependencies. Invalid providers must fail with clear diagnostics instead of being ignored silently.

## Decision

Use the JDK `ServiceLoader` API for external run formatter and extension discovery.

The canonical programmatic entry point is `org.javaspec.extension.JavaspecExtensionLoader.loadRunFormatterRegistry()` with the overload `loadRunFormatterRegistry(ClassLoader)`. The default overload uses the thread context classloader when available, otherwise the loader's own classloader. The classloader overload uses the supplied classloader, falling back to the same default when `null` is supplied. Compatibility aliases such as `loadRunFormatters()` may exist, but `loadRunFormatterRegistry(...)` is the documented helper.

The returned `RunFormatterRegistry` starts with built-in formatters (`progress`, `pretty`) and then loads ServiceLoader providers from the effective classloader:

- `META-INF/services/org.javaspec.formatter.RunFormatter` registers each provider by `RunFormatter.name()`.
- `META-INF/services/org.javaspec.extension.JavaspecExtension` configures each extension with an `ExtensionContext` that exposes the same registry.
- `META-INF/services/org.javaspec.extension.Extension` is accepted as a short-name alias service type for extension providers.

If the same extension implementation class is listed under both extension service types, it is configured once per registry load. Invalid service declarations, unloadable provider classes, invalid formatter names, or extension configuration failures raise `ExtensionLoadingException` with service type and implementation diagnostics.

`javaspec run` loads the formatter registry after effective classloader selection. External formatter/extension jars can therefore be available either on the process classpath or through `--classpath` / `--classpath-file`. CLI `--formatter <name>` overrides configuration `formatter=<name>`; configuration values are otherwise used, and the default remains `progress`. Invalid formatter diagnostics list the discovered formatter names.

The Gradle `javaspecRun` task loads the formatter registry from its run classloader. External formatter/extension jars can be on the configured task classpath, the extension classpath, or the default Java test runtime classpath when that default is active. Formatter selection precedence remains adapter-local: task setting, extension setting, project property `javaspec.formatter`, configuration file formatter, then default. Invalid formatter diagnostics list the discovered formatter names.

This phase does not add Maven plugin formatter output controls, report schema/content changes, integrated source/spec compilation, new runtime dependencies, publishing changes, plugin lookup, or configuration-driven extension activation beyond selecting discovered formatter names through existing formatter settings.

## Consequences

Positive consequences:

- External run formatters can be packaged as ordinary Java jars with standard `META-INF/services` files.
- Built-in formatter defaults remain available and deterministic before any external providers are added.
- The core keeps zero third-party runtime dependencies by using only JDK `ServiceLoader`.
- CLI and Gradle users can select custom formatter names through existing formatter settings once the provider jar is on the effective run classloader.
- Programmatic hosts can build the same registry explicitly with `JavaspecExtensionLoader`.
- Provider loading failures surface early with actionable diagnostics.

Negative consequences and limitations:

- ServiceLoader provider classes must follow Java ServiceLoader requirements, including being loadable from the selected classloader and constructible by the JDK loader.
- Users are responsible for putting formatter/extension jars on the correct process, explicit CLI, or Gradle run classpath.
- Maven plugin formatter selection remains out of scope for this phase.
- ServiceLoader discovery is classpath-based; there is still no package scanning, plugin repository lookup, bootstrap execution, or automatic classpath repair.
- Report writers, report schemaVersion 1, runner result semantics, exit-code/build-failure behavior, and publication readiness remain unchanged.

Related decisions: [ADR 0002](0002-zero-runtime-dependency-policy.md), [ADR 0010](0010-zero-dependency-formatter-reporting-and-programmatic-extension-boundary.md), [ADR 0011](0011-optional-junit-adapter-and-canonical-javaspec-runner.md), and [ADR 0017](0017-configuration-level-report-destinations.md).

Related ARC42 sections: [4. Solution Strategy](../arc42/04-solution-strategy.md), [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), [9. Architecture Decisions](../arc42/09-architecture-decisions.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
