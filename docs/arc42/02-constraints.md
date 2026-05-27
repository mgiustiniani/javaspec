# 2. Constraints

## 2.1 Technical Constraints

| Constraint | Description |
|---|---|
| Java 8 baseline | Production code must compile with Java 8 source/target compatibility and run on a Java 8 runtime. |
| No runtime dependencies | Runtime artifacts must depend only on the JDK. |
| Test-scope dependencies only | External dependencies are allowed only for tests or build-time verification and must not leak into the runtime artifact. |
| Maven | Future implementation should use Maven. |
| Package base | Production code should use package base `org.javaspec`. |
| Post-Java 8 APIs | APIs introduced after Java 8 must be represented as metadata, strings, or reflected conditionally; production source must not import them directly. |
| LTS profiles | The system must model Java LTS profiles for 8, 11, 17, 21, and 25. |
| Java 25 caution | Java 25-specific public data-structure additions are not assumed until maintainers verify the target JDK 25 API documentation. |

## 2.2 Organizational Constraints

- Phase 1 is documentation-only.
- Source code, build files, scaffolding, and tests must be delegated to implementation or testing agents later.
- Architectural decisions must be documented as ADRs when made.
- Generated documentation must be written in English.

## 2.3 Design Constraints

- The core runtime cannot depend on external assertion libraries, mocking libraries, YAML parsers, logging frameworks, dependency injection containers, or bytecode-generation libraries.
- Advanced features that normally require third-party dependencies must be implemented using JDK APIs, exposed as optional extensions, or deferred.
- Reflection must be isolated behind compatibility boundaries to avoid accidental linkage to newer JDK APIs.
- User-facing diagnostics should explain zero-dependency limitations clearly.

## 2.4 Compatibility Constraints

- Java 8 is the binary compatibility floor.
- Java 11, 17, 21, and 25 are target profiles, not separate binaries unless a future ADR decides otherwise.
- The profile catalog must avoid assuming the presence of Java 9+ classes while running on Java 8.
- The build and test matrix should include Java 8 plus each supported LTS runtime where available.
