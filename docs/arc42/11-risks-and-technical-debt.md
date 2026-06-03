# 11. Risks and Technical Debt

| Risk / debt | Current impact | Mitigation / next action |
|---|---|---|
| Source-only specs are skipped | Users may expect `javaspec run` to compile and execute source files directly. | Document that the CLI runner executes only compiled/classloader-available specs; future build-tool integrations may supply compilation or classpath setup. |
| Profile selection is not deep enforcement | `--profile` validates and reports a target profile but does not yet enforce all runtime/generation decisions. | Keep limitation explicit; implement deeper enforcement behind the compatibility boundary in a future phase. |
| Bootstrap hooks are metadata only | Configured bootstrap entries do not execute. | Preserve parsed metadata and add execution only with a future runner lifecycle design and tests. |
| External extension loading is not implemented | Programmatic extension contracts exist, but the CLI cannot load extension-provided formatter names or plugins. | Do not document config/classpath/ServiceLoader loading as supported; create a future ADR before implementing external loading. |
| JSON report scope is limited | Reports are schemaVersion 1 only and have no config-level destination or alternate format. | Treat schema changes and new report destinations as future decisions; keep current report behavior stable. |
| Existing sealed-interface source updates are deferred | Missing sealed-interface skeletons work, but existing sealed interfaces do not receive generated declarations/bodies. | Implement only when root declarations and nested permitted implementation bodies can be updated source-preservingly. |
| Java source parsing is heuristic | Complex Java source may not be updated safely by generators. | Keep generation deterministic and conservative; skip unsupported updates instead of corrupting source; consider parser strategy only if compatible with zero-runtime-dependency policy. |
| Generated post-Java-8 source requires newer JDKs | Records and sealed types can be emitted by a Java 8-compatible binary but cannot be compiled by Java 8 projects. | Document minimum source levels and rely on target project/JDK profile choices. |
| Generic `Iterable` count/empty checks consume the iterable | One-shot iterables are consumed and infinite iterables can hang. | Keep limitation explicit; future matcher variants may require bounded/predicate-aware semantics. |
| Interface doubles are limited | Core doubles cannot cover concrete classes, final classes, static methods, constructors, arrays, primitives, annotations, enums, wildcard matchers, exception/callback stubbing, or default-interface-method invocation. | Preserve zero-dependency JDK-proxy core; optional advanced doubles require a future ADR and likely external integration boundary. |
| Dry-run planning synchronization | Every new generation/update feature must also be represented in dry-run output. | Treat dry-run coverage as an acceptance criterion for future generation changes. |
| Internal JSON writer maintenance | No JSON library means escaping and deterministic output are maintained internally. | Keep focused tests for report writer behavior; do not expand report complexity without tests. |
| LTS API metadata drift | Java profile metadata can become stale as docs or JDK releases evolve. | Maintain research notes and re-run runtime probes in compatibility matrix work, especially for Java 25 stream gatherers and later profiles. |

## 11.1 Resolved or Controlled Risks

- **Runtime dependency leakage**: controlled by dependency audits; Phase 12 runtime audit passed with only the project artifact.
- **Java 8 runtime compatibility**: controlled by source/target settings, classfile audits, and the Phase 12 Java 8 matrix run.
- **Java 25 Gatherer availability assumptions**: controlled by metadata/reflection design and Phase 12 Java 25 runtime probe.
- **Unsafe constructor deletion by default**: controlled by default `comment` policy and explicit `delete` opt-in.

## 11.2 Highest-Priority Future Corrections

1. Keep documentation synchronized whenever implementation changes run controls, generation behavior, reports, or extension behavior.
2. Add deeper profile enforcement only after the compatibility boundary defines precise generation/runtime rules.
3. Design external extension loading separately from the current programmatic extension API.
4. Extend sealed-interface existing-source updates only when nested permitted implementation updates are safe.
