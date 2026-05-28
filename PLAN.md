# javaspec Implementation Plan

This plan defines the initial delivery path for javaspec, a Java 8-compatible, zero-runtime-dependency Java port inspired by phpspec.

## ADR 0004 Correction Status — Implemented and Verified

ADR 0004 was recorded before correction planning, as required by the course-correction protocol. The blocking correction track has now been implemented and the user manual has been updated to describe the actual behavior.

### Verified PHPSpec Inputs

Planner verification after ADR 0004 confirmed these source inputs:

- PHPSpec cookbook pages for construction and matchers were accessible with a browser User-Agent at `https://phpspec.net/en/stable/cookbook/construction.html` and `https://phpspec.net/en/stable/cookbook/matchers.html`.
- Raw documentation was also verified from `phpspec/phpspec` main branch files `docs/cookbook/construction.rst` and `docs/cookbook/matchers.rst`.
- PHPSpec 8.3.0 source was verified through GitHub API/raw files under `src/PhpSpec/Matcher` and `src/PhpSpec/Wrapper`.
- Construction semantics to preserve: PHPSpec treats the subject as `$this`, creates it lazily, supports `beConstructedWith(...)`, `beConstructedThrough(method, args)`, dynamic named construction forms such as `beConstructedThroughNamed(...)` and `beConstructedNamed(...)`, allows construction configuration before instantiation, allows a later construction call inside an example to override an earlier rule before instantiation, rejects construction-method changes after instantiation, and uses `duringInstantiation()` for constructor/factory exceptions.
- Matcher semantics to model: expectations use `should*` and `shouldNot*`; identity/equality keywords include `return`, `be`, `equal`, and `beEqualTo`; comparison uses `beLike`; throw matching uses `shouldThrow(...).duringMethod(...)`, `during(method, args)`, and `duringInstantiation()`; string containment/start/end/regex checks and custom matchers are part of the documented subset.

### Implemented Correction Summary

1. `describe` now writes both `<Subject>Spec.java` and `<Subject>SpecSupport.java`. Concrete specs extend the generated support class. The support class extends `ObjectBehavior<Subject>` and passes `Subject.class` for lazy subject construction.
2. `javaspec run` syntax now includes `--constructor-policy <delete|preserve|comment>`; default policy is `comment`. `delete` is the explicit destructive opt-in, `preserve` keeps non-empty unmatched constructors, and `comment` comments non-empty unmatched constructors. Empty generated/no-op unmatched constructors may be removed when safe.
3. Construction semantics are implemented in `ObjectBehavior`: lazy subject construction, `beConstructedWith(...)`, `beConstructedThrough(...)`, `beConstructedNamed(...)`, `beConstructedThroughNamed(...)`, override-before-instantiation, failure on change after instantiation, and `shouldThrow(...).duringInstantiation()`.
4. Generation distinguishes constructor and factory construction markers: `beConstructedWith(...)` remains constructor descriptor generation, while `beConstructedThrough("create", args...)`, `beConstructedNamed("named", args...)`, and `beConstructedThroughNamed("createNamed", args...)` discover/generate static factory method skeletons returning the described type. Factory names must be string literals and valid Java identifiers; non-string-literal names are ignored for generation instead of creating empty constructor markers.
5. Typed proxy matcher syntax is supported through generated support classes, including calls such as `getRating().shouldReturn(5)`, `getTitle().shouldContain("Wizard")`, and `shouldThrow(IllegalArgumentException.class).duringSetRating(-3)`. Existing `match(value).should...` usage remains available.
6. Method generation discovers typed proxy calls and can generate missing instance method skeletons with Java 8-compatible default returns. Generated typed spec support skips static factory descriptors because construction methods are not instance subject proxies. `run --generate` writes non-interactively; without `--generate`, `run` prompts before adding missing methods to an existing source file.
7. The matcher subset includes `shouldReturn`, `shouldNotReturn`, `shouldEqual`, `shouldBeLike`, `shouldContain`, `shouldNotContain`, `shouldStartWith`, `shouldEndWith`, and `shouldMatchPattern`; custom matchers can evaluate null subjects.
8. Known limitations remain: the example runner lifecycle is incomplete; `run` performs discovery/generation/update rather than executing full examples; source parsing/generation uses Java 8-compatible heuristics rather than a full Java parser; generated post-Java-8 source forms still require an appropriate JDK to compile.
9. Verification passed: `mvn test` completed with 174 tests, and `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.

## Phase 1 Status

Phase 1 creates documentation only:

- README and architecture principles.
- ARC42 sections 1-4.
- ADRs for Java 8 baseline/LTS profiles, zero runtime dependencies, and the first-MVP class-creation suggestion course correction.
- Research notes for Java LTS data structures and phpspec features.
- Internet/source verification was performed on 2026-05-27 and research notes were updated against Oracle API docs, phpspec 8.3.0 source/Packagist metadata, phpspec.net docs, and the Prophecy README.

No source code, build files, test files, or scaffolding are created in this phase.

## Phase 2 Status

Phase 2 is completed and implemented as the first MVP, with ADR 0004 correction behavior now reflected in the current status:

- Maven Java 8 project with `source`/`target` compatibility set to 1.8.
- Zero runtime dependencies; JUnit is used only in test scope.
- Runtime entry point: `org.javaspec.cli.Main`.
- CLI commands: `javaspec describe <ClassName>` / `javaspec desc <ClassName>` and minimal `javaspec run`.
- PHPSpec-style command split: `describe` creates only specification/support skeletons; `run` discovers specs and owns generation/update for missing production class-like types, constructors, methods, and static factories.
- Options: `--spec-dir <dir>` / `--spec-root <dir>`, defaulting to `src/test/java`; `--source-dir <dir>` / `--source-root <dir>`, defaulting to `src/main/java` for `run`; `--generate` for `run`; `--constructor-policy <delete|preserve|comment>` for `run`, defaulting to `comment`; `--help` / `-h`.
- `describe`: exit `0`, write the PHPSpec-style `spec.*.*Spec.java` and `spec.*.*SpecSupport.java` skeletons when absent, never write production code.
- `run` with existing source/classpath class: exit `0` and report that the class exists; when a source file exists and specs describe constructors, static factories, or missing instance methods, `run` may update the source according to constructor policy and generation confirmation rules.
- `run` with missing production type without `--generate`: print the target path and ask `Y/n`; generate on yes, or exit `1` and write no production files on no/unavailable input.
- `run` with missing production type and `--generate`: exit `0` and write the production type skeleton without prompting.
- Supported generated production type kinds: Java 8 class/interface/enum/annotation plus post-Java 8 record, sealed class, and sealed interface source forms; specs can declare `shouldExtend(...)`, `shouldImplement(...)`, and sealed `shouldPermit(...)` relationships.
- Invalid arguments exit `64`; I/O errors exit `70`.

Implemented files/classes at a high level:

- `pom.xml` — Maven build, Java 8 compiler settings, jar main class, and runtime dependency leakage guard.
- `src/main/java/org/javaspec/cli/Main.java` — first-MVP CLI parsing, dispatch, diagnostics, and exit codes.
- `src/main/java/org/javaspec/model/DescribedClass.java`, `DescribedType.java`, `JavaTypeKind.java`, `ConstructorDescriptor.java`, and `MethodDescriptor.java` — described-name validation, source-path mapping, constructor/method descriptors, and Java 8/post-Java 8 class-like type kind modeling without post-Java 8 binary linkage.
- `src/main/java/org/javaspec/api/ObjectBehavior.java` — PHPSpec-inspired generic base class (`ObjectBehavior<T>`) with lazy subject construction, construction configuration, matcher wrapping, and throw expectations.
- `src/main/java/org/javaspec/matcher/**` — zero-dependency matcher registry, `Matchable<T>` expectation wrapper, match results, and custom matcher support.
- `src/main/java/org/javaspec/discovery/ClassExistenceChecker.java` / `ClassCheckResult.java` and `TypeExistenceChecker.java` / `TypeCheckResult.java` — source-root and classpath existence checks, including classpath kind detection for class-like types.
- `src/main/java/org/javaspec/discovery/SpecDiscovery.java` and `DiscoveredSpec.java` — deterministic `*Spec.java` discovery, described-type inference from the `spec.` namespace, kind and relationship markers, constructor/factory construction markers, typed proxy matcher calls, throw-proxy calls, and method descriptor inference.
- `src/main/java/org/javaspec/generation/ClassGenerationPlan.java`, `ClassSkeletonGenerator.java`, `ClassFileGenerator.java`, `TypeGenerationPlan.java`, `TypeSkeletonGenerator.java`, and `TypeFileGenerator.java` — production class-like skeleton planning and explicit file writing after prompt acceptance or `run --generate`, including inferred constructor, instance method, and static factory method skeletons.
- `src/main/java/org/javaspec/generation/SpecGenerationPlan.java`, `SpecSkeletonGenerator.java`, `SpecFileGenerator.java`, and `SpecSupportFileGenerator.java` — PHPSpec-style specification and typed support skeleton planning/writing for `describe` and `run` support updates.
- `src/main/java/org/javaspec/generation/ConstructorPolicy.java`, `ClassConstructorUpdater.java`, and `ClassMethodUpdater.java` — constructor policy handling and source-preserving instance/static method insertion for existing production sources.
- `src/test/java/org/javaspec/**` — JUnit tests across model, discovery, generation, CLI, build, compatibility, matchers, and API behavior, including post-Java-8 source-form generation and ADR 0004 correction behavior.

Verification summary:

- `mvn test` passed with 174 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`, confirming no runtime third-party dependency leakage.

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
| PHPSpec-style describe/run generation split | ADR 0003, this plan, user manual | Implemented: `describe` writes only spec/support files; `run` owns production type, constructor, static factory, and instance method generation/update; prompts are used where required, and `run --generate` answers yes non-interactively. |
| Constructor-policy correction | ADR 0004, this plan, user manual | Implemented and verified: exact states `delete`, `preserve`, `comment`; default `comment`; destructive deletion only with `--constructor-policy delete`; empty no-op unmatched constructors may be removed safely. |
| PHPSpec construction semantics | ADR 0004, verified PHPSpec construction docs/source, this plan, user manual | Runtime implemented in `ObjectBehavior`: lazy subject construction, `beConstructedWith`, factory/named construction forms, override-before-instantiation semantics, failure on change after instantiation, and `duringInstantiation()`. Generation implemented: `beConstructedWith(...)` remains constructor descriptor generation; factory/named forms with string-literal Java-identifier names generate static factory method skeletons returning the described type; non-string-literal factory names are ignored for generation. Full CLI example lifecycle remains future work. |
| Typed proxy matcher syntax | ADR 0004, verified PHPSpec matcher docs/source, this plan, user manual | Implemented: generated subject-specific support classes expose typed proxy methods and throw proxies while existing `match(value).should...` usage remains available. |
| Method generation | ADR 0004, this plan, user manual | Implemented and verified: discovery from typed proxy/throw calls, direct subject/setter calls, and static factory construction markers; Java 8-compatible instance method and static factory skeleton generation; static factory descriptors are skipped by generated support proxies; `--generate` writes non-interactively and interactive `run` prompts before updating existing source files. |
| Maven implementation | This plan | Implemented in Phase 2. |
| Package base `org.javaspec` | README, this plan | Implemented in Phase 2 and retained for future work. |
| Target Java LTS profiles 8, 11, 17, 21, 25 | ADR 0001, Java LTS research | Future work in Phases 3 and 12. |
| Post-Java 8 APIs as metadata/reflection | ADR 0001, Java LTS research | Future work in Phases 3 and 12. |
| Java 8 data-structure list | Java LTS research | Future Phase 3 validation. |
| Later LTS data-structure additions | Java LTS research | Future Phase 3 validation. |
| Complete phpspec feature inventory | phpspec research and ADR 0004 follow-up verification | Phase 1 research completed; construction and matcher details were re-verified for ADR 0004; the ADR 0004 correction track is implemented, while broader PHPSpec feature parity remains future phased work. |

## Implementation Principles

- Compile production code with Java 8 `source` and `target` settings.
- Keep runtime code within package base `org.javaspec`.
- Use Maven for the project layout and lifecycle.
- Do not import or reference Java 9+ classes in production source.
- Store Java 9+ API names as strings in profile metadata.
- Use reflection only behind explicit compatibility boundaries.
- Keep optional integrations outside the core runtime.
- Keep production generation governed by the ADR 0003 `describe`/`run` split and by explicit confirmation or documented non-interactive generation policy.
- Treat `describe` as the explicit command that writes specification skeletons, interactive `run` confirmation as the normal PHPSpec-style production generation authorization, and `run --generate` as the explicit non-interactive yes.
- Do not write generated source files unless the user explicitly confirms the action or a documented non-interactive policy allows it.
- Treat ADR 0004 as implemented correction behavior that must be preserved before expanding unrelated functionality.
- Keep constructor policy states limited to `delete`, `preserve`, and `comment`; use `comment` as the default and require explicit opt-in for destructive deletion.
- Prefer generated subject-specific typed support/proxy classes while keeping explicit `match(value)` style APIs available.
- Insert generated methods source-preservingly, with confirmation or documented non-interactive behavior, and with Java 8-compatible default returns.
- Keep construction and matcher behavior aligned with the verified PHPSpec semantics for lazy construction, overrides before instantiation, negation, and exception matching.

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
7. PHPSpec-style specification and support skeleton generation from `describe`, using `ObjectBehavior<T>` through generated `<Subject>SpecSupport`, `it_is_initializable`, and `shouldHaveType(Subject.class)`.
8. Deterministic `spec.*.*Spec.java` discovery and described-class inference for `run`.
9. Missing production-class prompt output from `run`, including target path and `Y/n` confirmation.
10. Explicit `run --generate` skeleton writing for missing production class-like types without prompting.
11. Class-like kind markers in specs for interface, enum, and annotation generation.
12. Stable first-MVP exit codes: `0`, `1`, `64`, and `70`.

Implemented behavior:

- `describe`/`desc`: exit `0`, create Java 8-compatible PHPSpec-style `spec.*.*Spec.java` and `spec.*.*SpecSupport.java` skeletons when absent, and never generate production code.
- `describe`/`desc` when the spec already exists: exit `0`, report it, do not overwrite it, and generate the support file if it is missing.
- `run` with no specs: exit `0`, report that no specs were found.
- `run` with existing source/classpath class: exit `0` and report that the class exists; when a source file exists and specs describe constructors, static factories, or missing instance methods, `run` may update the source according to constructor policy and generation confirmation rules.
- `run` with missing production type without `--generate`: print the target path and ask whether to create it; yes generates and exits `0`, no/unavailable input exits `1` and writes no production files.
- `run` with missing production type and `--generate`: exit `0` and write the production type skeleton without prompting.
- `run --constructor-policy <delete|preserve|comment>` controls unmatched constructor handling; `comment` is the default and `delete` is the destructive opt-in.
- `run` discovers typed proxy matcher calls and factory construction markers, updates generated support classes, and can add missing Java 8-compatible instance method and static factory skeletons; without `--generate`, it prompts before updating an existing source file.
- `beConstructedWith(...)` remains constructor descriptor generation; `beConstructedThrough(...)`, `beConstructedNamed(...)`, and `beConstructedThroughNamed(...)` generate static factory methods only when the factory name is a string literal valid Java identifier. Generated support classes skip those static factory descriptors as non-instance construction methods.
- Specs can mark non-class production kinds with `shouldBeAFinalClass()`, `shouldBeAnInterface()`, `shouldBeAnEnum()`, `shouldBeAnAnnotation()`, `shouldBeARecord()`, `shouldBeASealedClass()`, or `shouldBeASealedInterface()`; generation writes the corresponding skeleton as source text.
- Specs can declare relationships with `shouldExtend(...)` and `shouldImplement(...)`; missing related production types get generated specs first, then production skeletons.
- Sealed class specs can declare explicit permitted subtypes with `shouldPermit(Circle.class, Rectangle.class)`; missing permitted subtypes get final-class specs extending the sealed root; otherwise a nested `Permitted` placeholder is generated so the sealed root has a syntactically valid permits clause.
- Sealed interface specs keep permitted implementations in the same production file; no separate specs or source files are generated for those local permitted implementations.
- Invalid arguments: exit `64`.
- I/O errors: exit `70`.

Verification:

- `mvn test` passed with 174 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` and no runtime third-party dependencies.

Still out of scope after the ADR 0004 correction:

- Complete example runner lifecycle execution.
- Interface generation beyond minimal skeletons.
- Private constructor source generation and broader named-constructor customization beyond the current static factory skeleton support.
- Template systems beyond the minimal class-like/spec/support skeleton need.
- Return constant generation from expectations beyond Java 8-compatible default returns.
- Full PHPSpec matcher parity beyond the documented matcher subset.
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
2. Model suites, source/spec paths, namespace/package mapping, bootstrap hooks, profile selection, formatter selection, and constructor policy.
3. Support sensible defaults for Maven-style layouts, the first-MVP describe flow, and ADR 0004's `comment` constructor-policy default.
4. Keep constructor policy states limited to `delete`, `preserve`, and `comment` in defaults, parsed configuration, and diagnostics.
5. Keep optional YAML/TOML/JSON support out of core unless implemented without dependencies.

Acceptance criteria:

- A default configuration can be inferred with no config file.
- Explicit config can select suite, paths, target profile, formatter, and constructor policy.
- Invalid config, including an unknown constructor policy, produces clear diagnostics.
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

**Status note:** ADR 0004 implemented the core `ObjectBehavior` construction APIs and throw expectation for instantiation. Complete suite/example lifecycle execution remains future work.

Tasks:

1. Implement suite, spec, and example execution models.
2. Define lifecycle hooks analogous to PHPSpec `let` and `letGo` using Java-friendly names and annotations or conventions.
3. Implement subject construction semantics verified for ADR 0004: lazy construction, `beConstructedWith(...)`, `beConstructedThrough(method, args)`, named factory-like construction forms, construction configuration in `let`, per-example override before instantiation, and rejection of construction-method changes after instantiation.
4. Integrate typed proxy matcher calls and method-generation diagnostics into the runner lifecycle without forcing eager subject construction.
5. Support `duringInstantiation()` for constructor/factory exception expectations and method-oriented throw checks for `duringMethod(...)` / `during(method, args)` equivalents.
6. Capture statuses: passed, failed, pending, skipped, broken/error.
7. Support stop-on-failure and verbosity settings.

Acceptance criteria:

- Examples run with isolated lifecycle state and fresh construction configuration.
- Construction is lazy and can be overridden inside an example only before instantiation.
- `let`, example execution, `letGo`, typed proxy matchers, and `duringInstantiation()` interact predictably.
- Failures include useful descriptions and source locations where available.
- Exit codes are stable for CI.

### Phase 7 — Expectations and Matchers

**Owner later:** Java implementation agent.

**Status note:** ADR 0004 implemented the documented matcher subset and generated typed proxy support. Full PHPSpec matcher parity remains future work.

Tasks:

1. Define matcher contracts with no external assertion library and expose them through generated subject-specific typed support/proxy classes.
2. Replace or refactor untyped wrapper-style matcher usage so user specs can call typed proxy methods such as `getRating().shouldReturn(5)` and `getTitle().shouldContain("Wizard")`.
3. Implement PHPSpec-inspired `should*` and `shouldNot*` expectation names, including identity/equality (`return`, `be`, `equal`, `beEqualTo`), comparison (`beLike`), approximate equality, type/assignability (`beAnInstanceOf`, `returnAnInstanceOf`, `haveType`, `implement`), exception throwing with `during...` forms, object-state `be*`/`have*`, scalar checks, count, containment, key/key-value checks, iteration/yield variants where Java-compatible, and string start/end/regex checks.
4. Support negation consistently across typed expectation objects.
5. Add extension points for inline `getMatchers()` custom matchers and later extension registration without runtime dependencies.

Acceptance criteria:

- Generated typed proxy/support classes compile and expose PHPSpec-like Java syntax.
- Matcher output explains expected and actual values.
- Negated expectations behave consistently.
- Throw matchers support method calls and `duringInstantiation()`.
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

**Status note:** ADR 0004 implemented constructor-policy help, parsing, defaults, and diagnostics. Broader CLI expansion remains future work.

Tasks:

1. Expand beyond the first-MVP `describe` or minimal `run`/`describe` flow into the full phpspec-inspired CLI.
2. Support commands such as `run`, `describe`, and `desc`.
3. Maintain `--constructor-policy <delete|preserve|comment>` in help text, parsing, defaults, and diagnostics, with `comment` as the default and destructive deletion available only through `delete`.
4. Support config selection, formatter selection, bootstrap, suite filters, stop-on-failure, profile selection, dry-run/confirmation behavior for generation, and verbosity.
5. Avoid external CLI parsing dependencies.
6. Define stable exit codes.

Acceptance criteria:

- CLI works on Java 8.
- Unknown commands and invalid options produce clear diagnostics.
- CLI help documents the exact constructor policy states and the `comment` default.
- `describe` preserves the first-MVP spec-only behavior, while `run` preserves missing-production-class and missing-method suggestion behavior.
- CLI generation-related actions remain gated by explicit confirmation, dry-run, or documented non-interactive policy.

### Phase 10 — Advanced Code Generation

**Owner later:** Java implementation agent.

**Status note:** ADR 0004 and the follow-up factory construction generation work implemented the current MVP subset of support/proxy generation, constructor policy handling, instance method skeleton generation, and static factory skeleton generation. Broader advanced generation remains future work.

Tasks:

1. Maintain the ADR 0004 correction behavior in the first-MVP spec and production class-like skeleton generators.
2. Maintain constructor handling with only `delete`, `preserve`, and `comment`; keep `comment` as the default for non-empty unmatched constructors; keep destructive deletion opt-in; and remove empty generated/no-op constructors only when safe and documented.
3. Maintain subject-specific typed support/proxy classes, for example `CalculatorSpecSupport extends ObjectBehavior<Calculator>`, that concrete specs can extend for typed PHPSpec-like calls.
4. Maintain and refine method discovery from generated typed proxy calls, static factory construction markers, and/or explicit missing-method diagnostics.
5. Maintain and refine missing method skeleton/snippet generation with Java 8-compatible default returns, inferred return types where available, parameters, overload safety, confirmation/non-interactive behavior, static factories returning the described type, and source-preserving insertion without rewriting whole classes.
6. Extend interface/enum/annotation generation beyond skeletons only after the production type skeleton flow is stable.
7. Refine static factory/named-constructor generation and add private constructor generation only after the current construction and factory semantics remain stable.
8. Provide templates for spec classes, support/proxy classes, examples, and generated production code where useful.
9. Add return constant generation only after matcher and example semantics can justify it.
10. Confirm before writing files unless a non-interactive flag or policy is provided.
11. Keep generated Java source compatible with the configured target profile and Java 8 binary rules.

Acceptance criteria:

- Generation is deterministic and reversible by review.
- Constructor updates follow the exact ADR 0004 policy states and default.
- Generated typed support/proxy classes compile and preserve PHPSpec-like Java syntax.
- Missing class/method prompts are actionable and overload-safe.
- Source-preserving insertion avoids whole-class rewrites.
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
7. Maintain and expand ADR 0004 correction tests for constructor default/comment policy, CLI help/parser behavior, class update safety, PHPSpec construction override/lazy semantics, factory construction skeleton generation, generated typed proxy compilation, matcher semantics and negation, throw/during/duringInstantiation behavior, method generation for missing methods and default returns, and user-manual examples when those examples become testable.

Acceptance criteria:

- Java 8 is a hard pass/fail gate.
- Runtime dependency audit passes.
- LTS profiles are tested or explicitly marked unavailable in the environment.
- First-MVP generator behavior is covered by automated tests.
- ADR 0004 correction behavior remains covered by automated tests before user-facing documentation is finalized or changed.

### Phase 13 — Documentation Completion

**Owner later:** documenter, with C4 delegated to c4model if requested.

Tasks:

1. Extend ARC42 sections 5-12 after implementation architecture stabilizes.
2. Add ADRs for major design choices made during implementation.
3. Integrate test and quality reports produced by tester agents.
4. Keep `docs/usermanual/Home.md` and `docs/usermanual/_Sidebar.md` synchronized after implementation begins.
5. Add user guide and migration notes from PHPSpec concepts to Java concepts.
6. Document construction, constructor policy, typed matcher syntax, method generation, limitations, commands, examples, the first-MVP generator flow, and later advanced generator behavior according to what is implemented.

Acceptance criteria:

- Documentation reflects implemented behavior.
- `docs/usermanual/Home.md` and `docs/usermanual/_Sidebar.md` expose the same navigation topics.
- ADRs link to relevant ARC42 sections.
- Test and quality claims are backed by produced reports.
