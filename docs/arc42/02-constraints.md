# 2. Constraints

## 2.1 Technical Constraints

| Constraint | Description |
|---|---|
| Java 8 baseline | Production code must compile with Java 8 source/target compatibility and run on a Java 8 runtime. |
| No runtime dependencies | Runtime artifacts must depend only on the JDK. |
| Test-scope dependencies only | External dependencies are allowed only for tests or build-time verification and must not leak into the runtime artifact. |
| Maven | The project uses Maven while preserving Java 8 bytecode compatibility. |
| Package base | Production code uses package base `org.javaspec`. |
| Post-Java 8 APIs | APIs introduced after Java 8 must be represented as metadata, strings, or reflected conditionally; production source must not import them directly. |
| LTS profiles | The system must model Java LTS profiles for 8, 11, 17, 21, and 25. |
| Restricted configuration parser | Configuration files must be parsed by an internal line-based parser; no YAML/TOML/JSON parser may be added to the runtime artifact. |
| Java 25 stream metadata | Java 25 stream gatherer metadata is implemented from verified API-documentation research, but runtime availability must remain metadata/reflection-only and be re-validated during quality-matrix work. |
| JDK proxy-only doubles | Core doubles must use Java 8 JDK dynamic proxies and therefore support ordinary interfaces only. |

## 2.2 Organizational Constraints

- Documentation phases and documenter-delegated tasks are documentation-only.
- Source code, build-file, scaffolding, and test changes must be delegated to implementation or testing agents.
- Architectural decisions must be documented as ADRs when made.
- Generated documentation must be written in English.

## 2.3 Design Constraints

- The core runtime cannot depend on external assertion libraries, mocking libraries, YAML parsers, logging frameworks, dependency injection containers, or bytecode-generation libraries.
- Advanced features that normally require third-party dependencies must be implemented using JDK APIs, exposed as optional extensions, or deferred.
- Core doubles are ordinary interface-only JDK dynamic proxies; concrete/final class doubles, static doubles, constructor doubles, wildcard matchers, exception/callback stubbing, and default-interface-method invocation are outside the Phase 8 core MVP.
- Reflection must be isolated behind compatibility boundaries to avoid accidental linkage to newer JDK APIs.
- Configuration bootstrap hooks remain metadata until bootstrap execution is implemented; profile and formatter settings are active `run` selections with CLI overrides, while selected profiles are not deeply enforced during execution yet. Suite package prefixes are active naming-convention inputs for `describe`, `run`, discovery, spec/support generation, and MVP reflection execution.
- The MVP CLI runner does not compile source or specification files itself; executable examples require compiled spec classes on the effective classloader, while source-only or unavailable spec classes are skipped.
- Dry-run mode must not write files or prompt, and run-only controls, including report options, must be rejected by `describe`/`desc`.
- User-facing diagnostics should explain zero-dependency limitations clearly.

## 2.4 Compatibility Constraints

- Java 8 is the binary compatibility floor.
- Java 11, 17, 21, and 25 are target profiles, not separate binaries unless a future ADR decides otherwise.
- The profile catalog must avoid assuming the presence of Java 9+ classes while running on Java 8.
- The build and test matrix should include Java 8 plus each supported LTS runtime where available.
