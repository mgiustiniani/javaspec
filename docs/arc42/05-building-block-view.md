# 5. Building Block View

## 5.1 Current Runtime Building Blocks

The implemented architecture now includes the Phase 2 first-MVP CLI/generation slice, the Phase 3 target-profile catalog and compatibility boundary, and the Phase 4 configuration, naming, and discovery-filter model.

| Building block | Package | Responsibility |
|---|---|---|
| CLI adapter | `org.javaspec.cli` | Parses first-MVP `describe`/`desc` and `run` commands, `--config`, `--suite`, path overrides, constructor-policy overrides, run `--class`/`--example` filters, diagnostics, and exit codes. |
| Configuration model | `org.javaspec.config` | Provides immutable default/configured suite settings and a restricted zero-runtime-dependency config parser. |
| Spec discovery, naming, and generation | `org.javaspec.discovery`, `org.javaspec.naming`, `org.javaspec.generation` | Applies default/configured naming conventions, discovers `*Spec.java` files, extracts example metadata, applies suite selection and class/example filters, and plans/writes gated spec, support, production type, constructor, factory, and method skeletons. |
| Object behavior and matchers | `org.javaspec.api`, `org.javaspec.matcher` | Provides the Java-facing specification base class, lazy construction support, expectation wrappers, and matcher contracts. |
| Profile catalog | `org.javaspec.profile` | Stores deterministic Java LTS profile, feature-flag, and API-symbol metadata for Java 8, 11, 17, 21, and 25. |
| Compatibility boundary | `org.javaspec.compatibility` | Checks profile compatibility and reflectively probes optional APIs without direct post-Java-8 linkage. |

## 5.2 Profile Catalog

`org.javaspec.profile` contains Java 8-compatible immutable metadata objects:

- `TargetProfile` defines ordered profile keys `java8`, `java11`, `java17`, `java21`, and `java25`.
- `FeatureFlag` models profile-gated language and library capabilities such as records, sealed types, collection factories, sequenced collections, and stream gatherers.
- `ApiSymbol`, `ApiSymbolKey`, `ApiSymbolKind`, and `ApiSymbolCategory` describe public JDK symbols by strings and categories.
- `ProfileCatalog` provides deterministic lookup by introduced profile, available profile, owner, owner/member key, and feature support.
- `DefaultProfileCatalogSymbols` populates representative metadata from the Java LTS data-structure research, including Java 25 stream gatherers.

The catalog has no runtime dependency outside the Java 8 standard library and does not import Java 9+ API types.

## 5.3 Compatibility Boundary

`org.javaspec.compatibility` separates profile decisions from runtime API probing:

- `ProfileCompatibilityCheck` checks whether a Java type kind, feature flag, or API symbol is allowed by a target profile.
- `CompatibilityResult` returns deterministic allowed/denied status, target profile, required profile, subject, and message.
- `ApiAvailabilityProbe` accepts class, method, and field names as strings and uses reflection to check availability on the current runtime. Missing classes, missing members, or linkage errors are treated as unavailable.

This boundary preserves the ADR 0001 rule that Java 11+ capabilities are metadata/reflection-only in production code while keeping later CLI and runner features able to make profile-aware decisions.

## 5.4 Configuration, Naming, and Discovery Filters

`org.javaspec.config` contains the Phase 4 configuration boundary:

- `JavaspecConfiguration` represents top-level settings: target profile, formatter, constructor policy, default suite, bootstrap metadata, and configured suites.
- `JavaspecSuiteConfiguration` represents one suite: suite name, spec root, source root, spec package prefix, production package prefix, and suite bootstrap metadata.
- `JavaspecConfigurationParser` reads a restricted line-based format with `=` or `:` separators, comment lines beginning with `#`, duplicate-key detection, unknown-key detection, required-value validation, and profile/constructor-policy validation.
- `ConstructorPolicyParser` keeps configuration-facing constructor-policy values limited to `delete`, `preserve`, and `comment`.
- `ConfigurationException` carries parse and validation diagnostics, including line numbers where available.

The naming/discovery boundary is implemented by `SpecNamingConvention`, `SpecDiscoveryRequest`, and `SpecExample`:

- `SpecNamingConvention` maps described production classes to spec/support classes and source paths using the selected suite's `specPackagePrefix` and `packagePrefix`.
- `SpecDiscoveryRequest` carries the spec root, suite name, naming convention, class filters, and example filters.
- `SpecExample` records public `void` `it_*`/`its_*` example methods with display names and source-order indexes.

The CLI adapter applies the selected suite's spec/source paths and package prefixes unless command-line path options override paths. `run` uses the configured constructor policy unless command-line `--constructor-policy` overrides it, filters classes with repeatable `--class <name>`, and filters examples with repeatable `--example <name>`. Bootstrap hooks and profile/formatter behavior are currently metadata for later runner and formatter features.
