# 0005 — Restricted Line-Based Configuration Format

## Status

Accepted

## Context

Phase 4 adds project configuration for defaults, suites, spec/source paths, target profile, formatter name, bootstrap hook metadata, and constructor policy.

The runtime artifact must remain Java 8-compatible and must not gain third-party runtime dependencies. Common configuration formats such as YAML, TOML, or JSON would normally require parser libraries or a substantially larger internal parser. Phase 4 also needs suite-level spec/source directories, package prefixes for naming conventions, selected suites, and discovery filters. Later phases make configured formatter/profile/bootstrap/report values active while preserving this restricted parser boundary.

## Decision

javaspec uses a restricted internal, line-based configuration format implemented in `org.javaspec.config`.

The parser accepts blank lines, comment lines beginning with `#`, and key/value lines separated by `=` or `:`. It recognizes a small fixed set of top-level keys and `suite.<name>.<property>` keys, validates target profiles and constructor-policy values, rejects duplicate/unknown/malformed keys, and reports line-number diagnostics where available.

No YAML, TOML, or JSON parser is part of the runtime artifact. Suite package-prefix settings are active inputs to `SpecNamingConvention` for `describe`, `run`, discovery, and spec/support generation. Bootstrap hooks are parsed as explicit class-name values and Phase 27 / ADR 0020 later makes them executable during `run` when compiled hook classes are on the effective run classloader.

## Consequences

Positive consequences:

- Runtime dependency policy and Java 8 compatibility are preserved.
- Configuration diagnostics remain deterministic and easy to test.
- The format is sufficient for Phase 4 defaults, suites, paths, package-prefix naming, profile metadata/enforcement defaults, formatter defaults, bootstrap hook class names, constructor-policy defaults, report destinations, and discovery filters driven by selected suites.

Negative consequences:

- Users do not get a general-purpose configuration language.
- Nested structures, includes, quoting rules, environment interpolation, and inline comments are intentionally not supported.
- Future richer configuration needs may require either compatible extensions to this format or optional integrations outside the core runtime.

Implementation follow-up:

- Phase 9 made configured `profile` and `formatter` active run selections with command-line overrides; Phase 26 / ADR 0019 later enforces selected profiles before generation/update writes.
- Phase 11 kept CLI formatter selection limited to built-in `progress` and `pretty` before external loading existed; Phase 25 / ADR 0018 later adds ServiceLoader-discovered formatter names on CLI/Gradle run classloaders.
- Phase 24 extends the fixed top-level key set with optional JSON and JUnit XML-compatible report destinations while preserving the same restricted line-based parser and zero-runtime-dependency configuration boundary.
- Phase 25 / ADR 0018 allows `formatter` to name a ServiceLoader-discovered formatter when the provider is on the effective run classloader; the line-based parser still stores the value without adding parser dependencies or configuration-driven extension activation.
- Phase 27 / ADR 0020 executes configured bootstrap hook class names before examples without adding ServiceLoader hook discovery, scripts, package scanning, dependency resolution, or runtime dependencies.
- Phase 29 / ADR 0022 intentionally adds no configuration keys for CLI compilation; `--compile` and `--compile-output` remain run-only CLI options.
