# 2. Constraints

## 2.1 Technical Constraints

| Constraint | Description |
|---|---|
| Java 8 baseline | Production code must compile with Java 8 source/target compatibility and run on a Java 8 runtime. |
| No runtime dependencies | The core runtime artifact must depend only on the JDK; optional adapter artifacts must not leak third-party dependencies into core. |
| Core test-scope dependencies only | External dependencies are allowed in the core build only for tests or build-time verification and must not leak into the core runtime artifact; standalone optional adapters may have their own isolated dependencies. |
| Maven | The project uses Maven while preserving Java 8 bytecode compatibility; repository-root `mvn verify` is intentionally core-only, standalone optional Maven, Gradle, and JUnit Platform adapter artifacts plus Phase 21 example projects are intentionally not root Maven modules, and Maven `release-artifacts` profiles produce local sources/javadocs only. |
| Package base | Production code uses package base `org.javaspec`. |
| Post-Java 8 APIs | APIs introduced after Java 8 must be represented as metadata, strings, or reflected conditionally; production source must not import them directly. |
| LTS profiles | The system must model Java LTS profiles for 8, 11, 17, 21, and 25. |
| Restricted configuration parser | Configuration files must be parsed by an internal line-based parser; no YAML/TOML/JSON parser may be added to the runtime artifact. |
| Java 25 stream metadata | Java 25 stream gatherer metadata is implemented from verified API-documentation research, but runtime availability must remain metadata/reflection-only and be re-validated during quality-matrix work. |
| JDK proxy-only doubles | Core doubles must use Java 8 JDK dynamic proxies and therefore support ordinary interfaces only. |
| No-JUnit core execution | Programmatic invocation, CLI execution, ServiceLoader formatter/extension discovery, skipped/pending semantics, JSON reports, JUnit XML-compatible reports, and optional Maven/Gradle plugin execution must not add JUnit or third-party runtime dependencies to the core artifact or require JUnit in projects under test; the optional JUnit Platform engine must keep JUnit Platform dependencies isolated to its standalone artifact. |

## 2.2 Organizational Constraints

- Documentation phases and documenter-delegated tasks are documentation-only.
- Source code, build-file, scaffolding, and test changes must be delegated to implementation or testing agents.
- Architectural decisions must be documented as ADRs when made.
- Legal/product release metadata must not be invented. The MIT license and maintainer `Mario Giustiniani <mariogiustiniani@gmail.com>` are confirmed; public publication remains postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved.
- Generated documentation must be written in English.

## 2.3 Design Constraints

- The core runtime cannot depend on external assertion libraries, mocking libraries, YAML parsers, logging frameworks, dependency injection containers, or bytecode-generation libraries.
- Advanced features that normally require third-party dependencies must be implemented using JDK APIs, exposed as optional extensions/adapters outside the core runtime, or deferred.
- Core doubles are ordinary interface-only JDK dynamic proxies; concrete/final class doubles, static doubles, constructor doubles, wildcard matchers, exception/callback stubbing, and default-interface-method invocation are outside the Phase 8 core MVP.
- Reflection must be isolated behind compatibility boundaries to avoid accidental linkage to newer JDK APIs.
- Configuration bootstrap hooks remain metadata until bootstrap execution is implemented; profile and formatter settings are active `run` selections with CLI overrides, while selected profiles are not deeply enforced during execution yet. Formatter names may be built-in or ServiceLoader-discovered from the effective run classloader. Suite package prefixes are active naming-convention inputs for `describe`, `run`, discovery, spec/support generation, and MVP reflection execution.
- The MVP CLI runner, programmatic invocation API, and optional adapters do not compile source or specification files themselves; executable examples require compiled spec classes on the effective, selected explicit, build-tool, programmatic, or JUnit Platform launcher classloader, while source-only or unavailable spec classes are skipped with execution-availability diagnostics when applicable. The optional Maven plugin, Gradle plugin, and JUnit Platform engine supply host classpath integration as adapters over the same canonical runner.
- Dry-run mode must not write files or prompt, and run-only controls, including explicit classpath and report options, must be rejected by `describe`/`desc`.
- User-facing diagnostics should explain zero-dependency and external-compilation limitations clearly.
- Aggregate release verification must use explicit scripts/CI workflow configuration rather than changing the root Maven reactor unless a future ADR decides otherwise.
- Release-readiness scaffolding must stay local and non-publishing until explicit owner decisions provide GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval.
- Standalone examples and report schema/golden docs are adoption assets only; they must not be treated as root build modules, public publication, deployment, signing, or remote CI proof.
- Explicit skipped/pending semantics must preserve the zero-runtime-dependency core and Java 8 baseline; pending may be mapped to skipped only in JUnit-compatible formats that lack a distinct pending status.

## 2.4 Compatibility Constraints

- Java 8 is the binary compatibility floor.
- Java 11, 17, 21, and 25 are target profiles, not separate binaries unless a future ADR decides otherwise.
- The profile catalog must avoid assuming the presence of Java 9+ classes while running on Java 8.
- The build and test matrix should include Java 8 plus each supported LTS runtime where available.
