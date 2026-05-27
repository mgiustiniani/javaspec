# 3. Context and Scope

## 3.1 System Context

javaspec is a developer tool used from local development environments and CI pipelines. It discovers and executes Java specification examples, reports results, and may generate spec or source skeletons in future phases.

External actors and systems:

| Actor/System | Relationship |
|---|---|
| Spec author | Writes specs, runs the CLI, reviews generated snippets, and configures suites. |
| Java project under test | Provides production classes described by javaspec specifications. |
| JVM/JDK | Executes javaspec and supplies Java 8+ runtime APIs. |
| Maven | Future build lifecycle and dependency-scope enforcement. |
| CI system | Runs specs, consumes exit codes, and may collect reports. |
| Optional extensions | May provide custom matchers, formatters, generators, or integrations without being part of the core runtime. |
| Test dependencies | Used only by the javaspec project test suite and never required by runtime users. |

## 3.2 Scope

In scope for the planned product:

- CLI command model inspired by phpspec.
- Configuration and suite selection.
- Spec discovery and described-class mapping.
- Example lifecycle and subject construction.
- Expectations and matchers.
- Zero-dependency object doubles where feasible.
- Code generation prompts and templates.
- Progress/pretty reporting and stable exit codes.
- Extension contracts.
- Java LTS target profile metadata.

Out of scope for the core runtime:

- Runtime dependency on external mocking, assertion, CLI, YAML, logging, or bytecode libraries.
- Direct compile-time dependency on Java 9+ APIs.
- Full emulation of PHP dynamic language behavior where it conflicts with Java type safety.
- Coverage collection in core; coverage should be provided by external tools or test-scope integrations.

## 3.3 Business Context

javaspec should help Java teams practice specification-first design with a low-friction dependency footprint. It is especially suited for libraries that must support older Java runtimes or avoid dependency conflicts.

## 3.4 Technical Context

The initial implementation should be a Java 8-compatible Maven project. All JDK-version-specific knowledge above Java 8 should be stored as profile metadata and resolved through reflection only when the runtime JDK supports it.

No C4 diagrams are generated in Phase 1. If diagrams are requested later, the documenter should delegate diagram generation to the `c4model` child agent and integrate the resulting diagrams into ARC42 section 3 or section 5 as appropriate.
