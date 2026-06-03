# 10. Quality Requirements

## 10.1 Quality Tree

| Quality attribute | Scenario | Evidence / current status |
|---|---|---|
| Java 8 compatibility | The runtime artifact executes on Java 8 and does not link directly to Java 9+ APIs. | Phase 12 Java 8 Distrobox `mvn clean` and `mvn verify` passed; compiler source/target is 1.8; bytecode and constant-pool audits are summarized in [Test and Quality Report](../test-report.md). |
| Zero runtime dependencies | Runtime dependency scope contains only the javaspec artifact. | Phase 12 Java 25 runtime dependency audit passed with only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`. |
| Deterministic CLI behavior | Commands, options, prompts, output modes, and exit codes are stable for local and CI usage. | CLI behavior is documented in the user manual; Phase 12 ran 364 tests per JDK across the matrix. |
| Safe generation | Production source generation/update is gated by prompts, `--generate`, or `--dry-run` planning. | ADR 0003, ADR 0004, ADR 0008, and the user manual document generation ownership and policies. |
| Accurate implemented-feature documentation | Docs do not overstate unsupported behavior. | Limitations are recorded in the user manual, README, ARC42 section 11, and ADR consequences. |
| Extensibility without dependency cost | Formatter and extension contracts are public without adding runtime libraries. | ADR 0010 documents programmatic-only extension behavior and lack of external CLI loading. |
| LTS awareness | Java 8, 11, 17, 21, and 25 profiles are modeled and verified where runtime probing is relevant. | Phase 12 matrix passed; Java 25 Gatherer reflection probe passed. |

## 10.2 Quality Scenarios

### Compatibility

- When running on Java 8, javaspec must start and execute the implemented CLI without `NoClassDefFoundError` caused by newer JDK APIs.
- When profile metadata references Java 11+ APIs, those references must be strings or reflected conditionally.
- When generated records or sealed types are emitted, the generator may produce source text that requires a newer JDK, but the javaspec binary itself must remain Java 8-compatible.

### Dependency Integrity

- `mvn dependency:tree -Dscope=runtime` must show no third-party runtime dependencies.
- Features that normally need libraries, such as configuration parsing, JSON writing, doubles, or CLI parsing, must stay internal/JDK-based or move to future optional integrations.

### Generation Safety

- `describe` must not write production source.
- `run` must prompt before production generation/update unless `--generate` is provided.
- `run --dry-run` must not write files and must not prompt.
- Constructor deletion must require explicit `--constructor-policy delete` or an equivalent explicit config/CLI selection.
- Existing sealed-interface updates must remain skipped until safe source-preserving nested implementation updates are implemented.

### Execution Determinism

- Discovery order, filter behavior, example selection, and result status mapping must remain deterministic.
- `AssertionError` is FAILED; non-assertion throwables are BROKEN; non-loadable specs are SKIPPED.
- `--stop-on-failure` stops after the first FAILED or BROKEN executable example.
- Skipped-only runs remain successful; failed or broken executable examples exit `1`.

### Reporting and CI

- Built-in `progress` and `pretty` formatters must remain deterministic.
- JSON report schemaVersion 1 must remain stable unless a future schema decision is made.
- Report write failures must include the report path and exit `70`.
- Test and quality claims must cite produced tester/quality reports rather than invented results.

## 10.3 Phase 12 Verification Summary

Phase 12 is the current authoritative compatibility and quality evidence:

| JDK | Runtime | Result |
|---|---|---|
| Java 8 | `1.8.0_492` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 11 | `11.0.31` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 17 | `17.0.19` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 21 | `21.0.11 LTS` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 25 | `25.0.3 LTS` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |

Additional Phase 12 evidence:

- Distrobox `1.8.2.5` with Podman `5.8.2` was used.
- Maven `3.9.16` ran in every matrix container.
- JDK 17+ emitted only expected `-source 8` / `-target 1.8` warnings.
- Java 25 Gatherer runtime reflection passed for `java.util.stream.Gatherer`, required nested Gatherer types, and `java.util.stream.Gatherers`.
- Runtime dependency audit passed in the Java 25 container.

See [Test and Quality Report](../test-report.md) for the consolidated report.

## 10.4 Quality Gates for Future Work

Future implementation phases should preserve these gates:

1. `mvn verify` passes on the supported JDK matrix when tester resources are available.
2. Runtime dependency audit remains clean.
3. Java 8 bytecode/source compatibility remains enforced.
4. New architectural decisions are recorded as ADRs before implementation where they change core boundaries.
5. User manual, README, ARC42, ADR references, and test/quality reports remain synchronized with implemented behavior.
