# javaspec Implementation Plan

This plan defines the initial delivery path for javaspec, a Java 8-compatible, zero-runtime-dependency Java port inspired by phpspec.

## Phase 1 Status

Phase 1 creates documentation only:

- README and architecture principles.
- ARC42 sections 1-4.
- ADRs for Java 8 baseline/LTS profiles, zero runtime dependencies, and the first-MVP class-creation suggestion course correction.
- Research notes for Java LTS data structures and phpspec features.
- Internet/source verification was performed on 2026-05-27 and research notes were updated against Oracle API docs, phpspec 8.3.0 source/Packagist metadata, phpspec.net docs, and the Prophecy README.

No source code, build files, test files, or scaffolding are created in this phase.

## Phase 2 Status

Phase 2 is completed and implemented as the first MVP:

- Maven Java 8 project with `source`/`target` compatibility set to 1.8.
- Zero runtime dependencies; JUnit is used only in test scope.
- Runtime entry point: `org.javaspec.cli.Main`.
- CLI commands: `javaspec describe <ClassName>` / `javaspec desc <ClassName>` and minimal `javaspec run`.
- PHPSpec-style command split: `describe` creates only a specification skeleton; `run` discovers specs and asks whether to generate missing production class-like types.
- Options: `--spec-dir <dir>` / `--spec-root <dir>`, defaulting to `src/test/java`; `--source-dir <dir>` / `--source-root <dir>`, defaulting to `src/main/java` for `run`; `--generate` for `run`; `--help` / `-h`.
- `describe`: exit `0`, write the PHPSpec-style `spec.*.*Spec.java` skeleton when absent, never write production code.
- `run` with existing source/classpath class: exit `0`, report that the class exists, and write no files.
- `run` with missing production type without `--generate`: print the target path and ask `Y/n`; generate on yes, or exit `1` and write no production files on no/unavailable input.
- `run` with missing production type and `--generate`: exit `0` and write the production type skeleton without prompting.
- Supported generated production type kinds: Java 8 class/interface/enum/annotation plus post-Java 8 record, sealed class, and sealed interface source forms; specs can declare `shouldExtend(...)`, `shouldImplement(...)`, and sealed `shouldPermit(...)` relationships.
- Invalid arguments exit `64`; I/O errors exit `70`.

Implemented files/classes at a high level:

- `pom.xml` — Maven build, Java 8 compiler settings, jar main class, and runtime dependency leakage guard.
- `src/main/java/org/javaspec/cli/Main.java` — first-MVP CLI parsing, dispatch, diagnostics, and exit codes.
- `src/main/java/org/javaspec/model/DescribedClass.java`, `DescribedType.java`, and `JavaTypeKind.java` — described-name validation, source-path mapping, and Java 8/post-Java 8 class-like type kind modeling without post-Java 8 binary linkage.
- `src/main/java/org/javaspec/api/ObjectBehavior.java` — minimal PHPSpec-inspired generic base class (`ObjectBehavior<T>`) for generated specs.
- `src/main/java/org/javaspec/discovery/ClassExistenceChecker.java` / `ClassCheckResult.java` and `TypeExistenceChecker.java` / `TypeCheckResult.java` — source-root and classpath existence checks, including classpath kind detection for class-like types.
- `src/main/java/org/javaspec/discovery/SpecDiscovery.java` and `DiscoveredSpec.java` — deterministic `*Spec.java` discovery, described-type inference from the `spec.` namespace, kind markers such as `shouldBeAnInterface()`, and relationship markers such as `shouldExtend(...)`, `shouldImplement(...)`, and sealed `shouldPermit(...)` parsing.
- `src/main/java/org/javaspec/generation/ClassGenerationPlan.java`, `ClassSkeletonGenerator.java`, `ClassFileGenerator.java`, `TypeGenerationPlan.java`, `TypeSkeletonGenerator.java`, and `TypeFileGenerator.java` — production class-like skeleton planning and explicit file writing after prompt acceptance or `run --generate`.
- `src/main/java/org/javaspec/generation/SpecGenerationPlan.java`, `SpecSkeletonGenerator.java`, and `SpecFileGenerator.java` — PHPSpec-style specification skeleton planning and explicit file writing for `describe`.
- `src/test/java/org/javaspec/**` — JUnit tests across model, discovery, generation, CLI, build, and Java 8 compatibility checks, including post-Java 8 source-form generation.

Verification summary:

- `mvn test` passed after class-like type expansion.
- `mvn dependency:tree -Dscope=runtime` showed only the project artifact, confirming no runtime third-party dependency leakage.

## Delegation Rule for Later Work

Further implementation work must be delegated to the appropriate workflow agents. The documenter must not create application source code, Maven build files, scaffolding, or tests. Future work should be delegated as follows by the parent workflow:

- Additional Java/Maven build changes: Java scaffolding/build agent.
- Production code under `org.javaspec`: Java implementation agent.
- Test code, compatibility matrix execution, and coverage reports: Java tester/quality agents.
- C4 diagrams, if requested: c4model via the documenter child delegation path.

## Requirement Traceability

| Requirement | Documentation artifact | Implementation status |
|---|---|---|
| Java 8-compatible binary | README, ARC42 constraints, ADR 0001 | Baseline implemented in Phase 2; compatibility matrix remains Phase 12 future work. |
| Zero runtime dependencies | README, ARC42 constraints, ADR 0002 | Implemented in Phase 2 and must be preserved through later phases. |
| Dependencies allowed only in test scope | README, ADR 0002 | Implemented in Phase 2 with JUnit in test scope only. |
| PHPSpec-style describe/run generation split | ADR 0003, this plan | Implemented in Phase 2: `describe` writes only specs; `run` prompts for missing production type skeletons; `run --generate` answers yes non-interactively. |
| Maven implementation | This plan | Implemented in Phase 2. |
| Package base `org.javaspec` | README, this plan | Implemented in Phase 2 and retained for future work. |
| Target Java LTS profiles 8, 11, 17, 21, 25 | ADR 0001, Java LTS research | Future work in Phases 3 and 12. |
| Post-Java 8 APIs as metadata/reflection | ADR 0001, Java LTS research | Future work in Phases 3 and 12. |
| Java 8 data-structure list | Java LTS research | Future Phase 3 validation. |
| Later LTS data-structure additions | Java LTS research | Future Phase 3 validation. |
| Complete phpspec feature inventory | phpspec research | Phase 1 research completed; implementation remains Phases 4-11 future work. |

## Implementation Principles

- Compile production code with Java 8 `source` and `target` settings.
- Keep runtime code within package base `org.javaspec`.
- Use Maven for the project layout and lifecycle.
- Do not import or reference Java 9+ classes in production source.
- Store Java 9+ API names as strings in profile metadata.
- Use reflection only behind explicit compatibility boundaries.
- Keep optional integrations outside the core runtime.
- Keep the first-MVP production generator limited to the missing-class class-skeleton flow, as required by ADR 0003.
- Treat `describe` as the explicit command that writes specification skeletons, interactive `run` confirmation as the normal PHPSpec-style production generation authorization, and `run --generate` as the explicit non-interactive yes.
- Do not write generated source files unless the user explicitly confirms the action or a documented non-interactive policy allows it.

## Phased Plan

### Phase 2 — First MVP: Java 8 Scaffolding and Missing Class Creation Suggestion (Completed)

**Status:** Completed and implemented.

**Relevant ADRs:** ADR 0001, ADR 0002, ADR 0003.

ADR 0003 moved the class-creation suggestion into the first MVP; the implemented CLI now follows PHPSpec's split where `describe` creates specs and `run` owns production-code generation. The implemented Phase 2 scope is:

1. Maven project layout with Java 8 `source`/`target` settings.
2. Runtime dependency leakage guard and no runtime third-party dependencies.
3. Production package base `org.javaspec`.
4. Minimal described-class model and validation.
5. Source-root and classpath existence checks.
6. CLI flow for `describe`/`desc` and minimal `run`.
7. PHPSpec-style specification skeleton generation from `describe`, using `ObjectBehavior<T>`, `it_is_initializable`, and `shouldHaveType(Subject.class)`.
8. Deterministic `spec.*.*Spec.java` discovery and described-class inference for `run`.
9. Missing production-class prompt output from `run`, including target path and `Y/n` confirmation.
10. Explicit `run --generate` skeleton writing for missing production class-like types without prompting.
11. Class-like kind markers in specs for interface, enum, and annotation generation.
12. Stable first-MVP exit codes: `0`, `1`, `64`, and `70`.

Implemented behavior:

- `describe`/`desc`: exit `0`, create a Java 8-compatible PHPSpec-style `spec.*.*Spec.java` skeleton when absent, and never generate production code.
- `describe`/`desc` when the spec already exists: exit `0`, report it, and do not overwrite.
- `run` with no specs: exit `0`, report that no specs were found.
- `run` with existing source/classpath class: exit `0`, report that the class exists and no generation is needed.
- `run` with missing production type without `--generate`: print the target path and ask whether to create it; yes generates and exits `0`, no/unavailable input exits `1` and writes no production files.
- `run` with missing production type and `--generate`: exit `0` and write the production type skeleton without prompting.
- Specs can mark non-class production kinds with `shouldBeAFinalClass()`, `shouldBeAnInterface()`, `shouldBeAnEnum()`, `shouldBeAnAnnotation()`, `shouldBeARecord()`, `shouldBeASealedClass()`, or `shouldBeASealedInterface()`; generation writes the corresponding skeleton as source text.
- Specs can declare relationships with `shouldExtend(...)` and `shouldImplement(...)`; missing related production types get generated specs first, then production skeletons.
- Sealed class specs can declare explicit permitted subtypes with `shouldPermit(Circle.class, Rectangle.class)`; missing permitted subtypes get final-class specs extending the sealed root; otherwise a nested `Permitted` placeholder is generated so the sealed root has a syntactically valid permits clause.
- Sealed interface specs keep permitted implementations in the same production file; no separate specs or source files are generated for those local permitted implementations.
- Invalid arguments: exit `64`.
- I/O errors: exit `70`.

Verification:

- `mvn test` passed across model, discovery, generation, CLI, build, and compatibility areas.
- `mvn dependency:tree -Dscope=runtime` showed only the project artifact and no runtime third-party dependencies.

Out of scope remains:

- Method generation.
- Interface generation beyond minimal skeletons.
- Named constructor generation.
- Private constructor generation.
- Template systems beyond the minimal class-like skeleton need.
- Return constant generation.
- Matcher generation.
- Broader Prophecy-inspired or double functionality.

### Phase 3 — Core Domain Model and LTS Profile Catalog

**Owner later:** Java implementation agent.

Tasks:

1. Extend the first-MVP domain model into immutable domain objects for target profiles, API symbols, feature flags, and compatibility checks using Java 8 constructs.
2. Encode profiles `java8`, `java11`, `java17`, `java21`, and `java25`.
3. Add metadata for collection/data-structure APIs from `docs/research/java-lts-data-structures.md`.
4. Add reflection helpers that safely probe optional APIs by class and method name.
5. Include the source-verified Java 25 stream-support metadata while keeping all Java 9+ APIs metadata/reflection-only.

Acceptance criteria:

- The profile catalog is usable on Java 8.
- Java 11+ APIs are represented by metadata strings or reflective probes only.
- Profile behavior is deterministic and testable.
- The first-MVP described-class model remains compatible with the expanded domain model.

### Phase 4 — Configuration Model

**Owner later:** Java implementation agent.

Tasks:

1. Design a zero-dependency configuration format or a restricted parser for a phpspec-inspired file.
2. Model suites, source/spec paths, namespace/package mapping, bootstrap hooks, profile selection, and formatter selection.
3. Support sensible defaults for Maven-style layouts and for the first-MVP describe flow.
4. Keep optional YAML/TOML/JSON support out of core unless implemented without dependencies.

Acceptance criteria:

- A default configuration can be inferred with no config file.
- Explicit config can select suite, paths, target profile, and formatter.
- Invalid config produces clear diagnostics.
- The missing-class suggestion flow works with both inferred defaults and explicit config.

### Phase 5 — Spec Discovery and Naming Conventions

**Owner later:** Java implementation agent.

Tasks:

1. Define Java naming conventions inspired by phpspec, adapted to packages and class names.
2. Discover spec classes under configured spec roots.
3. Map described Java classes to spec classes and back.
4. Define example method naming conventions and ordering.
5. Support suite filters and class/example filters.

Acceptance criteria:

- Spec discovery is deterministic.
- A described class can be mapped to its spec class.
- Filters work without loading unrelated examples where possible.
- Existing first-MVP described-class checks remain stable under full spec discovery.

### Phase 6 — Runner and Example Lifecycle

**Owner later:** Java implementation agent.

Tasks:

1. Implement suite, spec, and example execution models.
2. Define lifecycle hooks analogous to phpspec `let` and `letGo` using Java-friendly names and annotations or conventions.
3. Create subject construction support using reflection and constructor arguments.
4. Capture statuses: passed, failed, pending, skipped, broken/error.
5. Support stop-on-failure and verbosity settings.

Acceptance criteria:

- Examples run with isolated lifecycle state.
- Failures include useful descriptions and source locations where available.
- Exit codes are stable for CI.

### Phase 7 — Expectations and Matchers

**Owner later:** Java implementation agent.

Tasks:

1. Define matcher contracts with no external assertion library.
2. Implement MVP matchers: identity, equality, type, assignability/interface, nullability, exception throwing, collection/content checks, string checks, and negation.
3. Provide phpspec-inspired names where idiomatic in Java.
4. Add extension points for custom matchers.

Acceptance criteria:

- Matcher output explains expected and actual values.
- Negated expectations behave consistently.
- Custom matcher registration does not require runtime dependencies.

### Phase 8 — Collaborators and Doubles

**Owner later:** Java implementation agent.

Tasks:

1. Define a zero-dependency double model.
2. Support interface doubles through JDK dynamic proxies for MVP.
3. Support stubbing, simple mocking expectations, argument matching, and call recording.
4. Document limitations for concrete classes, final classes, static methods, and constructors without bytecode libraries.
5. Consider extension-only integration for advanced doubles if users accept external dependencies outside core.

Acceptance criteria:

- Interface doubles work on Java 8 without third-party libraries.
- Limitations are explicit and tested.
- The API does not require non-JDK runtime artifacts.

### Phase 9 — CLI Expansion

**Owner later:** Java implementation agent.

Tasks:

1. Expand beyond the first-MVP `describe` or minimal `run`/`describe` flow into the full phpspec-inspired CLI.
2. Support commands such as `run`, `describe`, and `desc`.
3. Support config selection, formatter selection, bootstrap, suite filters, stop-on-failure, profile selection, and verbosity.
4. Avoid external CLI parsing dependencies.
5. Define stable exit codes.

Acceptance criteria:

- CLI works on Java 8.
- Unknown commands and invalid options produce clear diagnostics.
- `describe` preserves the first-MVP spec-only behavior, while `run` preserves missing-production-class suggestion behavior.
- CLI generation-related actions remain gated by explicit confirmation or non-interactive policy.

### Phase 10 — Advanced Code Generation

**Owner later:** Java implementation agent.

Tasks:

1. Extend the first-MVP spec and production class-like skeleton generators with broader generation capabilities.
2. Generate richer spec skeletons and missing method snippets.
3. Extend interface/enum/annotation generation beyond skeletons only after the production type skeleton flow is stable.
4. Add named constructor and private constructor generation only after the basic class path is stable.
5. Provide templates for spec classes, examples, and generated production code where useful.
6. Add return constant generation only after matcher and example semantics can justify it.
7. Confirm before writing files unless a non-interactive flag or policy is provided.
8. Keep generated Java source compatible with the configured target profile and Java 8 binary rules.

Acceptance criteria:

- Generation is deterministic and reversible by review.
- Missing class/method prompts are actionable.
- Non-interactive CI mode does not block when an explicit policy is supplied.
- Advanced generation features do not expand the zero-runtime-dependency surface.

### Phase 11 — Formatters, Reporting, and Extensions

**Owner later:** Java implementation agent.

Tasks:

1. Implement progress and pretty formatters.
2. Define formatter extension contracts.
3. Add machine-readable reports only if they can be produced without runtime dependencies.
4. Define extension registration and lifecycle hooks.

Acceptance criteria:

- Human-readable output is clear for local use.
- CI receives stable exit codes and optional machine-readable output.
- Extensions can add matchers, formatters, generators, or lifecycle behavior without modifying core internals.

### Phase 12 — Compatibility and Quality Matrix

**Owner later:** Java tester/quality agents.

Tasks:

1. Run compilation and tests on Java 8.
2. Run runtime compatibility tests on Java 8, 11, 17, 21, and 25 where available.
3. Verify no runtime dependencies are packaged.
4. Verify no Java 9+ symbols appear in Java 8 production bytecode constant-pool references except as intentional metadata strings.
5. Re-validate Java 25 metadata against the target JDK 25 API documentation during implementation, including stream gatherer support, before relying on environment-specific behavior.
6. Include first-MVP tests for existing type detection, missing type detection, prompt output for a missing type, class-like kind generation, and write gating.

Acceptance criteria:

- Java 8 is a hard pass/fail gate.
- Runtime dependency audit passes.
- LTS profiles are tested or explicitly marked unavailable in the environment.
- First-MVP generator behavior is covered by automated tests.

### Phase 13 — Documentation Completion

**Owner later:** documenter, with C4 delegated to c4model if requested.

Tasks:

1. Extend ARC42 sections 5-12 after implementation architecture stabilizes.
2. Add ADRs for major design choices made during implementation.
3. Integrate test and quality reports produced by tester agents.
4. Add user guide and migration notes from phpspec concepts to Java concepts.
5. Document the first-MVP generator flow and later advanced generator behavior according to what is implemented.

Acceptance criteria:

- Documentation reflects implemented behavior.
- ADRs link to relevant ARC42 sections.
- Test and quality claims are backed by produced reports.
