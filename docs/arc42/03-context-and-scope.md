# 3. Context and Scope

## 3.1 System Context

javaspec is a developer tool used from local development environments and CI pipelines. The implemented MVP discovers specifications with configured suite paths/package prefixes and class/example filters, can generate gated spec or source skeletons, runs discovered examples through a reflection runner when the compiled spec classes are available on the effective classloader, offers Phase 9 run controls for dry-run planning, stop-on-failure, progress/pretty output, profile selection, and verbose diagnostics, and provides zero-dependency interface doubles for collaborators. Later phases will expand reporting, bootstrap execution, deeper profile enforcement, pending examples, and optional advanced double integrations.

External actors and systems:

| Actor/System | Relationship |
|---|---|
| Spec author | Writes specs, runs the CLI, reviews generated snippets, and configures suites. |
| Java project under test | Provides production classes described by javaspec specifications. |
| JVM/JDK | Executes javaspec and supplies Java 8+ runtime APIs. |
| Maven | Build lifecycle and dependency-scope enforcement. |
| CI system | Runs specs, consumes exit codes, and may collect reports. |
| Optional extensions | May provide custom matchers, formatters, generators, or integrations without being part of the core runtime. |
| Test dependencies | Used only by the javaspec project test suite and never required by runtime users. |

## 3.2 Scope

In scope for the planned product:

- CLI command model inspired by phpspec.
- Configuration and suite selection.
- Suite-level spec/source directories and package-prefix naming.
- Spec discovery, described-class mapping, and class/example filters.
- Example lifecycle and subject construction.
- Expectations and matchers.
- Zero-dependency interface doubles where feasible, currently through JDK dynamic proxies.
- Code generation prompts and templates.
- Built-in progress/pretty reporting and stable exit codes.
- Extension contracts.
- Java LTS target profile metadata and run-time profile selection.

Out of scope for the core runtime:

- Runtime dependency on external mocking, assertion, CLI, YAML, logging, or bytecode libraries.
- Direct compile-time dependency on Java 9+ APIs.
- Full emulation of PHP dynamic language behavior where it conflicts with Java type safety.
- Coverage collection in core; coverage should be provided by external tools or test-scope integrations.

## 3.3 Business Context

javaspec should help Java teams practice specification-first design with a low-friction dependency footprint. It is especially suited for libraries that must support older Java runtimes or avoid dependency conflicts.

## 3.4 Technical Context

The implementation is a Java 8-compatible Maven project. JDK-version-specific knowledge above Java 8 is stored as profile metadata under `org.javaspec.profile` and resolved through the `org.javaspec.compatibility` boundary using reflection only when the runtime JDK supports it. Example execution uses Java reflection against compiled spec classes on the effective classloader; the CLI does not compile source/spec files itself, so source-only or otherwise unavailable spec classes are skipped. Phase 9 dry-run mode plans without writes/prompts, stop-on-failure controls runner continuation, formatter selection controls built-in CLI output, and profile selection is validated/reported without deep enforcement yet. Collaborator doubles use Java 8 JDK dynamic proxies for ordinary interfaces and do not require bytecode libraries.

No C4 diagrams are currently generated. If diagrams are requested later, the documenter should delegate diagram generation to the `c4model` child agent and integrate the resulting diagrams into ARC42 section 3 or section 5 as appropriate.
