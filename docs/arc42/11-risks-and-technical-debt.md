# 11. Risks and Technical Debt

| Risk / debt | Current impact | Mitigation / next action |
|---|---|---|
| Source-only specs are skipped | Users may expect `javaspec run`, programmatic invocation, build-tool adapters, or the optional JUnit Platform engine to compile and execute source files directly. | Document that javaspec executes only compiled/classloader-available specs; use `--classpath`, `--classpath-file`, optional Maven/Gradle plugin test classpath integration, optional JUnit Platform runtime classpath integration, or a programmatic classloader after external compilation. |
| Profile selection is not deep enforcement | `--profile` validates and reports a target profile but does not yet enforce all runtime/generation decisions. | Keep limitation explicit; implement deeper enforcement behind the compatibility boundary in a future phase. |
| Bootstrap hooks are metadata only | Configured bootstrap entries do not execute. | Preserve parsed metadata and add execution only with a future runner lifecycle design and tests. |
| External extension loading is not implemented | Programmatic extension contracts exist, but the CLI cannot load extension-provided formatter names or plugins. | Do not document config/classpath/ServiceLoader loading as supported; create a future ADR before implementing external loading. |
| Report scope is limited | JSON reports are schemaVersion 1, JUnit XML-compatible reports are intentionally minimal, and reports have no config-level destination. | Treat schema changes, richer JUnit XML metadata, and new report destinations as future decisions; keep current report behavior stable. |
| Existing sealed-interface source updates are deferred | Missing sealed-interface skeletons work, but existing sealed interfaces do not receive generated declarations/bodies. | Implement only when root declarations and nested permitted implementation bodies can be updated source-preservingly. |
| Java source parsing is heuristic | Complex Java source may not be updated safely by generators. | Keep generation deterministic and conservative; skip unsupported updates instead of corrupting source; consider parser strategy only if compatible with zero-runtime-dependency policy. |
| Generated post-Java-8 source requires newer JDKs | Records and sealed types can be emitted by a Java 8-compatible binary but cannot be compiled by Java 8 projects. | Document minimum source levels and rely on target project/JDK profile choices. |
| Generic `Iterable` count/empty checks consume the iterable | One-shot iterables are consumed and infinite iterables can hang. | Keep limitation explicit; future matcher variants may require bounded/predicate-aware semantics. |
| Interface doubles are limited | Core doubles cannot cover concrete classes, final classes, static methods, constructors, arrays, primitives, annotations, enums, wildcard matchers, exception/callback stubbing, or default-interface-method invocation. | Preserve zero-dependency JDK-proxy core; optional advanced doubles require a future ADR and likely external integration boundary. |
| Dry-run planning synchronization | Every new generation/update feature must also be represented in dry-run output. | Treat dry-run coverage as an acceptance criterion for future generation changes. |
| Internal JSON/XML writer maintenance | No JSON, XML, JUnit, or reporting library means escaping and deterministic output are maintained internally. | Keep focused tests for report writer behavior; do not expand report complexity without tests. |
| LTS API metadata drift | Java profile metadata can become stale as docs or JDK releases evolve. | Maintain research notes and re-run runtime probes in compatibility matrix work, especially for Java 25 stream gatherers and later profiles. |
| Invocation API compatibility pressure | The optional Maven and Gradle plugins now depend on `org.javaspec.invocation` and `RunResult` semantics; future adapters may also depend on them. | Keep the API small, document exit-code mapping, preserve the `JavaspecLauncher` delegation guard, and add compatibility tests before adapter work expands it. |
| Standalone adapter verification can be missed | Because `javaspec-maven-plugin/`, `javaspec-gradle-plugin/`, and `javaspec-junit-platform-engine/` are intentionally standalone and not root Maven modules, repository-root `mvn verify` does not verify the adapters. | Document the local sequences `mvn -q -DskipTests install` followed by Maven plugin verification, compatible-Gradle plugin verification, and JUnit Platform engine verification, and keep standalone adapter verification in release/CI checklists. |
| Gradle executable/JDK compatibility can block local verification | The cached Gradle 7.4.2 executable was blocked on the installed Java 21 runtime with `Unsupported class file major version 65`. | Document this as an environment/tooling compatibility blocker for that cached executable, not as a javaspec feature failure; use a Java 21-compatible Gradle executable such as the verified `/tmp/gradle-8.8` download when reproducing Phase 16 locally. |
| Optional JUnit Platform engine can be mistaken for required execution | Users may assume javaspec specs require JUnit Platform once the optional engine exists. | Keep no-JUnit CLI/programmatic/Maven/Gradle paths documented first; state that the engine is an optional IDE/CI adapter only and that projects not opting in keep no JUnit dependency. |

## 11.1 Resolved or Controlled Risks

- **Runtime dependency leakage**: controlled by dependency audits; Phase 12 and Phase 14 root runtime audits passed with only the project artifact, Phase 15 root/Maven-plugin runtime audits passed with only core for root and plugin plus compile-scope core for the standalone Maven plugin, Phase 16 root/Gradle-plugin runtime audits passed with only core for root and core-only Gradle runtimeClasspath, and Phase 17 root/JUnit-engine runtime audits passed with only core for root and isolated JUnit Platform dependencies in the standalone engine.
- **Java 8 runtime compatibility**: controlled by source/target settings, classfile audits, and the Phase 12 Java 8 matrix run.
- **Java 25 Gatherer availability assumptions**: controlled by metadata/reflection design and Phase 12 Java 25 runtime probe.
- **Unsafe constructor deletion by default**: controlled by default `comment` policy and explicit `delete` opt-in.
- **No-JUnit foundation availability**: Phase 14 implemented programmatic no-`System.exit` invocation, explicit classpath input, and dependency-free JUnit XML-compatible reporting under ADR 0011.
- **Optional Maven plugin availability**: Phase 15 implemented and verified standalone Maven `javaspec:run` integration over `JavaspecLauncher` without requiring JUnit in projects under test.
- **Optional Gradle plugin availability**: Phase 16 implemented and verified standalone Gradle `javaspecRun` integration over `JavaspecLauncher` without requiring JUnit in projects under test.
- **Optional JUnit Platform engine availability**: Phase 17 implemented and verified standalone JUnit Platform engine integration over `JavaspecLauncher` with isolated engine dependencies and no spec authoring style changes.

## 11.2 Highest-Priority Future Corrections

1. Keep documentation synchronized whenever implementation changes run controls, generation behavior, reports, or extension behavior.
2. Add deeper profile enforcement only after the compatibility boundary defines precise generation/runtime rules.
3. Design external extension loading separately from the current programmatic extension API.
4. Extend sealed-interface existing-source updates only when nested permitted implementation updates are safe.
5. Preserve Phase 14 invocation/classpath/report behavior plus Phase 15 Maven, Phase 16 Gradle, and Phase 17 JUnit Platform adapter behavior as Phase 18 IDE/CI polish work is designed.
