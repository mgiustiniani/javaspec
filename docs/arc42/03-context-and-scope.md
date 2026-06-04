# 3. Context and Scope

## 3.1 System Context

javaspec is a developer tool used from local development environments and CI pipelines. The implemented MVP discovers specifications with configured suite paths/package prefixes and class/example filters, can generate gated spec or source skeletons, runs discovered examples through a reflection runner when the compiled spec classes are available on the effective or selected explicit classloader, offers Phase 9 run controls for dry-run planning, stop-on-failure, progress/pretty output, profile selection, and verbose diagnostics, writes optional JSON and JUnit XML-compatible runner reports with stable ids/source metadata where available, exposes no-`System.exit` programmatic invocation plus formatter/extension contracts, provides zero-dependency interface doubles for collaborators, includes standalone optional Maven, Gradle, and JUnit Platform adapters, and now provides aggregate release/CI verification assets. Future backlog work may expand bootstrap execution, deeper profile enforcement, pending examples, external extension loading, broader IDE/CI polish, publishing/signing, optional advanced double integrations, or a separate multi-module decision.

External actors and systems:

| Actor/System | Relationship |
|---|---|
| Spec author | Writes specs, runs the CLI, reviews generated snippets, and configures suites. |
| Java project under test | Provides production classes described by javaspec specifications. |
| JVM/JDK | Executes javaspec and supplies Java 8+ runtime APIs. |
| Maven | Build lifecycle, dependency-scope enforcement, and optional `javaspec:run` plugin execution. |
| Gradle | Optional `javaspecRun` task execution and test source set runtime classpath integration. |
| JUnit Platform launcher | Optional engine discovery/execution path for tools that opt into `javaspec-junit-platform-engine/`. |
| CI system | Runs specs, supplies compiled classpaths when needed, consumes exit codes or optional JUnit Platform events, may collect JSON and/or JUnit XML-compatible runner reports with stable ids/source metadata where available, and can run the configured Java 8/11/17/21/25 core matrix plus Java 21 aggregate verification workflow. |
| Host launcher or build-tool adapter | Can call `org.javaspec.invocation` without `System.exit`, provide a classloader, and inspect structured invocation results. |
| Optional extensions | May register behavior programmatically in the current implementation; external CLI discovery/loading is future work. |
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
- Built-in progress/pretty reporting, optional JSON and JUnit XML-compatible runner reports, stable identifiers/source metadata where available, and stable exit codes.
- Programmatic no-`System.exit` invocation and programmatic extension contracts.
- Standalone optional Maven, Gradle, and JUnit Platform adapters over the canonical runner.
- Non-disruptive aggregate release/CI verification through `scripts/verify-all.sh` and `.github/workflows/ci.yml`.
- Java LTS target profile metadata and run-time profile selection.

Out of scope for the core runtime:

- Runtime dependency on external mocking, assertion, CLI, YAML, logging, or bytecode libraries.
- Direct compile-time dependency on Java 9+ APIs.
- Full emulation of PHP dynamic language behavior where it conflicts with Java type safety.
- Coverage collection in core; coverage should be provided by external tools or test-scope integrations.

## 3.3 Business Context

javaspec should help Java teams practice specification-first design with a low-friction dependency footprint. It is especially suited for libraries that must support older Java runtimes or avoid dependency conflicts.

## 3.4 Technical Context

The implementation is a Java 8-compatible Maven project. JDK-version-specific knowledge above Java 8 is stored as profile metadata under `org.javaspec.profile` and resolved through the `org.javaspec.compatibility` boundary using reflection only when the runtime JDK supports it. Example execution uses Java reflection against compiled spec classes on the effective or selected explicit classloader; the CLI, invocation API, and optional adapters do not compile source/spec files themselves, so source-only or otherwise unavailable spec classes are skipped. The optional Maven plugin uses Maven test dependency resolution/test classpath, the optional Gradle plugin uses Gradle classpath integration, and the optional JUnit Platform engine uses JUnit Platform launcher classpath/selectors as adapters over the canonical launcher. Phase 9 dry-run mode plans without writes/prompts, stop-on-failure controls runner continuation, formatter selection controls built-in CLI output, profile selection is validated/reported without deep enforcement yet, and reports are written from immutable runner results with stable ids/source metadata where available when requested. Collaborator doubles use Java 8 JDK dynamic proxies for ordinary interfaces and do not require bytecode libraries. Phase 19 release/CI verification keeps root `mvn verify` core-only and verifies standalone adapters explicitly through `scripts/verify-all.sh`; the GitHub Actions workflow is configured but only local YAML parse and local script execution are currently claimed.

No C4 diagrams are currently generated. If diagrams are requested later, the documenter should delegate diagram generation to the `c4model` child agent and integrate the resulting diagrams into ARC42 section 3 or section 5 as appropriate.
