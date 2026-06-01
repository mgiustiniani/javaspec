# javaspec Implementation Plan

This plan defines the initial delivery path for javaspec, a Java 8-compatible, zero-runtime-dependency Java port inspired by phpspec.

## Current Implementation Status — Implemented and Verified

Phases 2, 3, and 4 are complete, and the Phase 5/6 MVP reflection runner is implemented.

- Phase 2 implemented the Java 8 Maven project, zero-runtime-dependency guard, PHPSpec-style `describe`/`run` split, specification/support skeletons, and gated production type/method generation.
- Phase 3 implemented Java LTS target profiles `java8`, `java11`, `java17`, `java21`, and `java25`, the profile catalog, API-symbol metadata, target-profile compatibility checks, and reflection-only API availability probes.
- Phase 4 implemented the zero-runtime-dependency line-based configuration format, `--config <file>` and `--suite <name>` integration, suite-level spec/source directories, suite package prefixes, naming convention integration, and suite/class/example discovery filters.
- The Phase 5/6 MVP implemented `org.javaspec.runner`, keeps `javaspec run` discovery/generation/update behavior, and executes filtered discovered examples when compiled spec classes are available on the effective classloader.
- Runner behavior: existing `DiscoveredSpec`/`SpecExample` metadata remains the execution source, so suite/class/example filters remain effective; each example gets a fresh spec instance; optional public no-arg `let()` runs before each example and optional public no-arg `letGo()` runs after each example.
- Result states are `PASSED`, `FAILED` for `AssertionError`, `BROKEN` for non-assertion throwables/lifecycle/reflection errors, and `SKIPPED` for non-loadable spec classes or missing reflected example methods. The CLI prints a summary and exits `1` for failed or broken executable examples.
- Known limitation: the CLI runner does not compile source/spec files itself; source-only or otherwise unavailable spec classes are skipped/not executable.
- Verification completed on 2026-06-01: `mvn verify` passed with 307 tests, 0 failures, 0 errors, and 0 skipped.
- Runtime dependency verification completed on 2026-06-01: `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

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
8. Known limitations remain: the MVP reflection runner executes only compiled, classloader-available spec classes and does not compile source/spec files itself; lifecycle support is limited to optional public no-arg `let()`/`letGo()`; source parsing/generation uses Java 8-compatible heuristics rather than a full Java parser; generated post-Java-8 source forms still require an appropriate JDK to compile.
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

## Phase 3 Status

Phase 3 is completed and implemented as the core domain model and LTS profile catalog.

Implementation summary:

1. Added Java 8-compatible profile/catalog domain classes under `org.javaspec.profile`: `TargetProfile`, `FeatureFlag`, `ApiSymbol`, `ApiSymbolKey`, `ApiSymbolKind`, `ApiSymbolCategory`, `ProfileCatalog`, and `DefaultProfileCatalogSymbols`.
2. Added Java 8-compatible compatibility boundary classes under `org.javaspec.compatibility`: `CompatibilityCheck`, `ProfileCompatibilityCheck`, `CompatibilityResult`, and `ApiAvailabilityProbe`.
3. Encoded target profiles `java8`, `java11`, `java17`, `java21`, and `java25` with deterministic ordering, parsing, lookup, and feature-support behavior.
4. Implemented immutable API-symbol metadata and catalog lookup by introduced profile, availability profile, owner, and owner/member key.
5. Populated representative data-structure and modeling metadata from `docs/research/java-lts-data-structures.md`, including Java 8 collection/container/array/stream symbols, Java 11 collection factories and related stream/optional additions, Java 17 stream/record/sealed metadata, Java 21 sequenced collections, and Java 25 stream gatherer metadata.
6. Preserved the Java 8 binary strategy: Java 11+ APIs remain metadata strings or reflection-only probes; production source has no direct post-Java-8 imports.
7. Added tests for the profile domain objects, catalog behavior, compatibility checks, and reflective API availability probing.

Verification summary:

- Initial Phase 3 tester verification reported `mvn test` BUILD SUCCESS with 212 tests run, 0 failures, 0 errors, and 0 skipped.
- Stabilization verification after Phase 4 reported `mvn verify` BUILD SUCCESS with 301 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.
- Full multi-JDK runtime compatibility and bytecode/constant-pool audits remain Phase 12 quality-matrix work.

## Phase 4 Status

Phase 4 is completed and implemented as the configuration, naming, and discovery-filter integration. ADR 0005 records the restricted line-based configuration format decision.

Implementation summary:

1. Added the zero-dependency `org.javaspec.config` production package: `ConfigurationException`, `ConstructorPolicyParser`, `JavaspecSuiteConfiguration`, `JavaspecConfiguration`, and `JavaspecConfigurationParser`.
2. Added a restricted line-based config parser with no YAML/TOML/JSON runtime dependency. Blank lines and lines beginning with `#` are ignored; keys use `=` or `:` separators; duplicate, unknown, malformed, blank-required, invalid-profile, and invalid-constructor-policy input produces clear diagnostics.
3. Implemented inferred defaults through `JavaspecConfiguration.defaults()`: suite `default`, spec root `src/test/java`, source root `src/main/java`, spec package prefix `spec`, production package prefix empty, profile `java8`, formatter `progress`, constructor policy `comment`, and empty bootstrap hooks.
4. Modeled top-level config keys `profile`, `formatter`, `constructorPolicy`/`constructor-policy`, `defaultSuite`/`default-suite`, and `bootstrap`, plus suite keys for `specDir`/`spec-dir`, `sourceDir`/`source-dir`, `specPackagePrefix`/`spec-package-prefix`, `packagePrefix`/`package-prefix`, and `bootstrap`.
5. Integrated `--config <file>` and `--suite <name>` into `org.javaspec.cli.Main` for `describe` and `run`. The selected suite's paths and package prefixes drive naming unless paths are overridden by `--spec-dir`/`--spec-root` or `--source-dir`/`--source-root`.
6. Preserved `describe` as spec-only behavior: it still rejects command-line `--source-dir`, but a `sourceDir` in config is accepted because `describe` ignores source roots.
7. Integrated configuration with constructor handling: `run` uses the configured constructor policy unless command-line `--constructor-policy` overrides it; valid values remain `delete`, `preserve`, and `comment`.
8. Integrated suite package prefixes with `SpecNamingConvention`, `SpecDiscoveryRequest`, spec/support skeleton planning, and CLI describe/run flows so configured `specPackagePrefix` and `packagePrefix` map production classes to spec/support classes.
9. Added suite selection and run filters: `--suite <name>` selects the configured suite and spec root, repeatable `--class <name>` filters by described qualified/simple name or spec qualified/simple name, and repeatable `--example <name>` filters by example method name, display name, or source-order index.
10. Bootstrap values are comma-separated metadata only and are not executed yet. Profile and formatter values are parsed/validated metadata until the corresponding runner and formatter features are implemented.
11. Added tests: `src/test/java/org/javaspec/config/JavaspecConfigurationTest.java`, `src/test/java/org/javaspec/config/JavaspecConfigurationParserTest.java`, `src/test/java/org/javaspec/cli/MainConfigurationIntegrationTest.java`, `src/test/java/org/javaspec/discovery/SpecNamingConventionTest.java`, `src/test/java/org/javaspec/discovery/SpecDiscoveryNamingTest.java`, and `src/test/java/org/javaspec/generation/SpecSkeletonGeneratorNamingTest.java`.

Verification summary:

- `mvn verify` passed with 301 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

## Phase 5/6 MVP Reflection Runner Status — Implemented and Verified

ADR 0006 records the classpath reflection runner decision.

Implementation summary:

1. Added the zero-dependency `org.javaspec.runner` package with immutable example/spec/run result objects and failure details.
2. Integrated `SpecRunner` into `javaspec run` after discovery, related-spec handling, support updates, production type generation, constructor updates, and method updates.
3. Reused `DiscoveredSpec` and `SpecExample` metadata as the runner input, preserving suite selection, class filters, and example filters for execution.
4. Loaded compiled spec classes from the effective classloader. Non-loadable spec classes are not executable and their discovered examples are marked `SKIPPED`.
5. Executed each reflected example on a fresh spec instance.
6. Supported optional public no-argument `let()` before each example and optional public no-argument `letGo()` after each example, including after failures.
7. Mapped results to `PASSED`, `FAILED`, `BROKEN`, and `SKIPPED`: AssertionError from an example is `FAILED`; non-assertion throwables from examples, lifecycle hooks, instantiation, or reflection inspection are `BROKEN`; missing reflected example methods are `SKIPPED`.
8. Added CLI summary output with total, passed, failed, broken, and skipped counts plus failed/broken/skipped example details. `run` exits `1` when executable examples fail or break.
9. Preserved the zero-runtime-dependency policy and Java 8-compatible implementation style.

Verification summary:

- `mvn verify` passed with 307 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Known limitations:

- The CLI runner does not compile source/spec files itself. Source-only or otherwise unavailable spec classes are skipped/not executable until an external build, IDE, or launcher puts compiled classes on the effective classloader.
- Lifecycle support is intentionally minimal: public no-argument `let()` and `letGo()` only.
- Pending examples, stop-on-failure, active formatter behavior, bootstrap execution, profile-aware execution, and richer reporting remain later work.

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
| PHPSpec construction semantics | ADR 0004, verified PHPSpec construction docs/source, this plan, user manual | Runtime implemented in `ObjectBehavior`: lazy subject construction, `beConstructedWith`, factory/named construction forms, override-before-instantiation semantics, failure on change after instantiation, and `duringInstantiation()`. Generation implemented: `beConstructedWith(...)` remains constructor descriptor generation; factory/named forms with string-literal Java-identifier names generate static factory method skeletons returning the described type; non-string-literal factory names are ignored for generation. The MVP reflection lifecycle now runs compiled examples with fresh spec instances and optional public no-arg `let()`/`letGo()`; full PHPSpec parity remains future work. |
| Typed proxy matcher syntax | ADR 0004, verified PHPSpec matcher docs/source, this plan, user manual | Implemented: generated subject-specific support classes expose typed proxy methods and throw proxies while existing `match(value).should...` usage remains available. |
| Method generation | ADR 0004, this plan, user manual | Implemented and verified: discovery from typed proxy/throw calls, direct subject/setter calls, and static factory construction markers; Java 8-compatible instance method and static factory skeleton generation; static factory descriptors are skipped by generated support proxies; `--generate` writes non-interactively and interactive `run` prompts before updating existing source files. |
| Configuration model and inferred defaults | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `JavaspecConfiguration.defaults()` provides the default suite `default`, Maven-style spec/source roots, `spec` package prefix, empty production package prefix, `java8` profile, `progress` formatter, `comment` constructor policy, and empty bootstrap hooks when no config file is supplied. |
| Constructor-policy config default | ADR 0004, ADR 0005, user manual, this plan | Implemented in Phase 4: config key `constructorPolicy`/`constructor-policy` accepts only `delete`, `preserve`, and `comment`; `comment` remains the inferred and config default, and `run --constructor-policy` overrides config explicitly. |
| Explicit suites, paths, profile, and formatter config | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `--config <file>` and `--suite <name>` select suite configuration; selected-suite `specDir`/`sourceDir` drive `describe`/`run` unless CLI path options override them; selected-suite `specPackagePrefix`/`packagePrefix` drive naming; `profile` and `formatter` are parsed/validated metadata until profile-aware runner and formatter behavior is implemented. |
| Naming convention integration | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `SpecNamingConvention` maps production names to spec/support packages using configured suite package prefixes, validates naming metadata, and is used by describe, discovery, and support generation. |
| Suite, class, and example filters | README, user manual, ARC42 section 5, this plan | Implemented in Phase 4 and reused by the Phase 5/6 MVP runner: `--suite` selects the configured suite; repeatable `--class` filters by described or spec class names; repeatable `--example` filters by example method name, display name, or source-order index; filtered `DiscoveredSpec`/`SpecExample` metadata controls both generation/update and reflection execution. |
| Phase 5/6 MVP reflection runner | README, user manual, ARC42 section 5, ADR 0006, this plan | Implemented and verified: after discovery/generation/update, `javaspec run` executes examples when compiled spec classes are available on the effective classloader; each example uses a fresh spec instance with optional public no-arg `let()` and `letGo()`; results are `PASSED`, `FAILED`, `BROKEN`, or `SKIPPED`; CLI summary exits `1` for failed/broken executable examples; source-only/unavailable spec classes are skipped because the CLI does not compile them. |
| Missing-class flow with config | User manual, this plan | Implemented in Phase 4: `run` uses inferred defaults without a config file and selected-suite paths/naming with explicit config, preserving the existing missing-production prompt and `--generate` non-interactive generation behavior. |
| Maven implementation | This plan | Implemented in Phase 2. |
| Package base `org.javaspec` | README, this plan | Implemented in Phase 2 and retained for future work. |
| Target Java LTS profiles 8, 11, 17, 21, 25 | README, ARC42 section 5, ADR 0001, Java LTS research, this plan | Implemented in Phase 3: `TargetProfile` and `ProfileCatalog` encode `java8`, `java11`, `java17`, `java21`, and `java25`; full multi-JDK runtime matrix remains Phase 12. |
| Post-Java 8 APIs as metadata/reflection | README, ARC42 section 5, ADR 0001, Java LTS research, this plan | Implemented in Phase 3: Java 11+ API symbols are stored as metadata strings and probed only through `ApiAvailabilityProbe`; no post-Java-8 direct production imports are required. Constant-pool audit remains Phase 12. |
| Java 8 data-structure list | README, ARC42 section 5, Java LTS research, this plan | Implemented in Phase 3: representative Java 8 collection, container, array, optional, atomic/reference, and stream symbols are cataloged and tested. |
| Later LTS data-structure additions | README, ARC42 section 5, Java LTS research, this plan | Implemented in Phase 3: representative Java 11 collection factories/collectors/optional/stream additions, Java 17 stream/record/sealed metadata, Java 21 sequenced collections, and Java 25 stream-support metadata are cataloged. Ongoing synchronization with JDK docs remains future quality work. |
| Java 25 stream-support metadata | README, ARC42 section 5, Java LTS research, this plan | Implemented in Phase 3: `STREAM_GATHERERS` and metadata for `java.util.stream.Gatherer`, nested gatherer types, and `java.util.stream.Gatherers` are present; runtime availability must still be probed reflectively. |
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
- Keep configuration parsing restricted, line-based, and zero-dependency; do not add YAML/TOML/JSON parser dependencies to runtime.
- Treat missing config as `JavaspecConfiguration.defaults()` and apply command-line path/constructor-policy overrides over selected-suite values while keeping selected-suite package prefixes in the active naming convention.
- Treat bootstrap hooks and profile/formatter settings as parsed metadata until the corresponding runner and formatter features are implemented; suite package prefixes are active naming-convention inputs for describe/run discovery, generation, and MVP reflection execution.
- Use `DiscoveredSpec` and `SpecExample` as the execution selection source so suite, class, and example filters remain effective for the runner.
- Treat the CLI runner as a classpath reflection executor, not an in-process compiler; source-only or unavailable spec classes are skipped/not executable until compiled classes are present on the effective classloader.
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

- Complete PHPSpec-style runner lifecycle beyond the Phase 5/6 MVP reflection runner, including pending examples, stop-on-failure, active formatters, bootstrap execution, and profile-aware execution.
- Interface generation beyond minimal skeletons.
- Private constructor source generation and broader named-constructor customization beyond the current static factory skeleton support.
- Template systems beyond the minimal class-like/spec/support skeleton need.
- Return constant generation from expectations beyond Java 8-compatible default returns.
- Full PHPSpec matcher parity beyond the documented matcher subset.
- Broader Prophecy-inspired or double functionality.

### Phase 3 — Core Domain Model and LTS Profile Catalog (Completed)

**Status:** Completed and implemented.

**Implemented by:** Java implementation agent.

Implemented scope:

1. Extended the first-MVP domain model with immutable Java 8-compatible objects for target profiles, API symbols, feature flags, and compatibility checks.
2. Encoded profiles `java8`, `java11`, `java17`, `java21`, and `java25` in `TargetProfile` and exposed them through `ProfileCatalog`.
3. Added metadata for representative collection/data-structure APIs from `docs/research/java-lts-data-structures.md`.
4. Added reflection helpers that safely probe optional APIs by class, method, or field name without requiring those APIs on Java 8.
5. Included Java 25 stream gatherer metadata while keeping all Java 9+ APIs metadata/reflection-only.
6. Added deterministic catalog lookup by profile, owner, owner/member key, and feature flag.

Implemented files/classes at a high level:

- `src/main/java/org/javaspec/profile/TargetProfile.java` — ordered LTS profile keys, labels, major versions, parsing, and comparison helpers.
- `src/main/java/org/javaspec/profile/FeatureFlag.java` — profile-gated feature flags for Java type kinds, collection factories, streams, sequenced collections, and stream gatherers.
- `src/main/java/org/javaspec/profile/ApiSymbol.java`, `ApiSymbolKey.java`, `ApiSymbolKind.java`, and `ApiSymbolCategory.java` — immutable API-symbol metadata and lookup keys.
- `src/main/java/org/javaspec/profile/ProfileCatalog.java` and `DefaultProfileCatalogSymbols.java` — deterministic default catalog for Java 8, 11, 17, 21, and 25 metadata.
- `src/main/java/org/javaspec/compatibility/CompatibilityCheck.java`, `ProfileCompatibilityCheck.java`, and `CompatibilityResult.java` — target-profile compatibility checks for type kinds, feature flags, and API symbols.
- `src/main/java/org/javaspec/compatibility/ApiAvailabilityProbe.java` — reflection-only availability probe for optional APIs.
- `src/test/java/org/javaspec/profile/**` and `src/test/java/org/javaspec/compatibility/**` — tests for profiles, feature flags, symbols, catalog behavior, compatibility checks, and API probing.

Verification:

- Initial Phase 3 tester verification reported `mvn test` BUILD SUCCESS with 212 tests run, 0 failures, 0 errors, and 0 skipped.
- Stabilization verification after Phase 4 reported `mvn verify` BUILD SUCCESS with 301 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Acceptance criteria status:

- The profile catalog is usable on Java 8 by design and by Java 8-compatible production source.
- Java 11+ APIs are represented by metadata strings or reflective probes only.
- Profile behavior is deterministic and covered by tests.
- The first-MVP described-class model remains compatible with the expanded domain model.

### Phase 4 — Configuration, Naming, and Discovery Filters (Completed)

**Status:** Completed and implemented.

**Relevant ADRs:** ADR 0002, ADR 0005.

**Implemented by:** Java implementation agent.

Implemented scope:

1. Added the `org.javaspec.config` package with immutable top-level and suite configuration objects, configuration exceptions, a constructor-policy parser, and a restricted config parser.
2. Implemented `JavaspecConfiguration.defaults()` for the inferred no-config case: default suite `default`, spec root `src/test/java`, source root `src/main/java`, spec package prefix `spec`, empty production package prefix, profile `java8`, formatter `progress`, constructor policy `comment`, and no bootstrap hooks.
3. Implemented a zero-runtime-dependency line-based config format: blank lines and `#` comments are ignored, `=` and `:` are accepted separators, duplicate/unknown/malformed keys are rejected, and no YAML/TOML/JSON runtime parser is required.
4. Implemented top-level keys for `profile`, `formatter`, constructor policy, default suite, and bootstrap metadata; implemented suite keys for spec/source roots, `specPackagePrefix`/`spec-package-prefix`, `packagePrefix`/`package-prefix`, and bootstrap metadata.
5. Integrated `--config <file>` and `--suite <name>` with `describe` and `run` in `org.javaspec.cli.Main`.
6. Applied selected-suite paths unless overridden by command-line spec/source path options. `describe` still rejects command-line source-root options but accepts and ignores `sourceDir` loaded from config.
7. Applied configured package prefixes through `SpecNamingConvention` so `describe`, `run`, discovery, and spec/support generation map between production packages and spec packages consistently.
8. Added naming/discovery metadata for example methods: public `void` methods named `it_*` or `its_*` are extracted with source-order indexes and display names derived from underscores.
9. Added suite selection and run filters: repeatable `--class <name>` filters by described qualified name, described simple name, spec qualified name, or spec simple name; repeatable `--example <name>` filters by example method name, display name, or source-order index. Suite selection through `--suite <name>` selects the configured suite, spec root, source root, and naming convention.
10. Applied the configured constructor policy to `run` unless command-line `--constructor-policy` overrides it.
11. Preserved the missing-production prompt and `--generate` flow for inferred defaults and explicit config.
12. Kept bootstrap values as comma-separated metadata only. Profile and formatter values are parsed/validated metadata until the corresponding runner and formatter features are implemented.

Implemented files/classes at a high level:

- `src/main/java/org/javaspec/config/ConfigurationException.java`
- `src/main/java/org/javaspec/config/ConstructorPolicyParser.java`
- `src/main/java/org/javaspec/config/JavaspecSuiteConfiguration.java`
- `src/main/java/org/javaspec/config/JavaspecConfiguration.java`
- `src/main/java/org/javaspec/config/JavaspecConfigurationParser.java`
- `src/main/java/org/javaspec/discovery/SpecNamingConvention.java` and `src/main/java/org/javaspec/naming/SpecNamingConvention.java`
- `src/main/java/org/javaspec/discovery/SpecDiscoveryRequest.java`
- `src/main/java/org/javaspec/discovery/SpecExample.java`
- `src/main/java/org/javaspec/discovery/SpecDiscovery.java` updates for configured naming, class filters, example extraction, and example filters.
- `src/main/java/org/javaspec/generation/SpecSkeletonGenerator.java` updates for configured spec/support naming.
- `src/main/java/org/javaspec/cli/Main.java` updates for config loading, suite selection, path precedence, naming metadata, constructor-policy precedence, and `--class`/`--example` filters.
- `src/test/java/org/javaspec/config/JavaspecConfigurationTest.java`
- `src/test/java/org/javaspec/config/JavaspecConfigurationParserTest.java`
- `src/test/java/org/javaspec/cli/MainConfigurationIntegrationTest.java`
- `src/test/java/org/javaspec/discovery/SpecNamingConventionTest.java`
- `src/test/java/org/javaspec/discovery/SpecDiscoveryNamingTest.java`
- `src/test/java/org/javaspec/generation/SpecSkeletonGeneratorNamingTest.java`

Verification:

- `mvn verify` BUILD SUCCESS with 301 tests run, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Acceptance criteria status:

- A default configuration can be inferred with no config file.
- Explicit config can select suite, paths, target profile, formatter, constructor policy, spec package prefix, and production package prefix. Current `describe`/`run` behavior uses selected suite paths, package prefixes, and constructor policy; profile, formatter, and bootstrap remain parsed/validated metadata until later runner features.
- Naming convention mapping works for default and configured package prefixes, including spec/support skeleton paths and discovery mapping back to described production classes.
- Suite selection, class filters, and example filters are implemented for the current discovery/generation flow and are reused by the MVP reflection runner.
- Invalid config, including an unknown constructor policy or invalid naming metadata, produces clear diagnostics.
- The missing-class suggestion flow works with both inferred defaults and explicit config.

### Phase 5 — Full Runner Discovery Expansion

**Status:** MVP implemented and verified; richer runner discovery diagnostics remain future work.

**Relevant ADRs:** ADR 0006.

Implemented scope:

1. The naming-convention and discovery-filter subset originally planned here was implemented during Phase 4 stabilization: default/configured package-prefix mapping, configured spec roots, described/spec class mapping, example metadata extraction, suite selection, class filters, and example filters.
2. The Phase 5/6 MVP runner now consumes the existing `DiscoveredSpec` and `SpecExample` metadata, so suite, class, and example filters remain effective for reflection execution.
3. Only discovered/filtered example metadata is executed; unrelated methods in a compiled spec class are not invoked by the runner.
4. Existing first-MVP described-type checks, generation prompts, related-spec handling, support updates, constructor updates, and method updates remain ahead of execution in `javaspec run`.

Remaining tasks:

1. Extend discovery diagnostics and source locations as needed by future formatter and reporting layers.
2. Add richer runner controls such as stop-on-failure only after the MVP reflection runner remains stable.
3. Preserve the source-discovery metadata contract when future bootstrap/profile/formatter behavior is added.

Acceptance criteria status:

- Deterministic discovery and configured naming behavior remains stable.
- A described class can be mapped to its spec class with default or configured package prefixes.
- Suite, class, and example filters work for generation/update and for MVP reflection execution.
- Existing first-MVP described-class checks remain stable before execution.

### Phase 6 — Runner and Example Lifecycle

**Status:** MVP implemented and verified; full PHPSpec-style lifecycle parity remains future work.

**Relevant ADRs:** ADR 0004, ADR 0006.

Implemented scope:

1. Added the `org.javaspec.runner` execution/result model: `SpecRunner`, `ExampleStatus`, `ExampleResult`, `SpecResult`, `RunResult`, and `FailureDetail`.
2. `javaspec run` executes examples after discovery/generation/update work has completed and only when compiled spec classes are available on the effective classloader.
3. Each example runs on a fresh spec instance constructed through the spec class no-argument constructor.
4. Optional public no-argument `let()` runs before each example; optional public no-argument `letGo()` runs after each example, including when `let()` or the example fails.
5. Result states are implemented: `PASSED` for normal completion, `FAILED` for `AssertionError`, `BROKEN` for non-assertion throwables/lifecycle/instantiation/reflection errors, and `SKIPPED` for non-loadable spec classes or missing reflected example methods.
6. The CLI prints total, passed, failed, broken, and skipped counts and exits `1` when executable examples fail or break.
7. The runner preserves the zero-runtime-dependency policy and does not compile source/spec files itself.

Remaining tasks:

1. Add pending examples, stop-on-failure, verbosity, active formatter behavior, bootstrap execution, profile-aware execution, and richer reporting.
2. Expand source-location diagnostics for failed/broken examples where available.
3. Deepen integration with typed proxy matchers and method-generation diagnostics without forcing eager subject construction.
4. Keep ADR 0004 construction semantics stable as the runner grows beyond the MVP lifecycle.

Acceptance criteria status:

- Examples run with isolated spec instances.
- `let()`, example execution, and `letGo()` interact predictably for the MVP public no-arg lifecycle.
- Failed and broken examples include throwable summary details in CLI output.
- Exit code `1` is stable for failed/broken executable examples; skipped-only runs remain successful.
- Source-only or unavailable spec classes are skipped until compiled classes are present on the effective classloader.

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

**Status note:** ADR 0004 implemented constructor-policy help, parsing, defaults, and diagnostics. Phase 4 implemented `--config`/`--suite`, selected-suite path and package-prefix precedence, config-backed constructor-policy defaults, and repeatable `--class`/`--example` filters for `run`. Broader CLI expansion remains future work.

Tasks:

1. Expand beyond the first-MVP `describe` or minimal `run`/`describe` flow into the full phpspec-inspired CLI.
2. Support commands such as `run`, `describe`, and `desc`.
3. Maintain `--constructor-policy <delete|preserve|comment>` in help text, parsing, defaults, and diagnostics, with `comment` as the default and destructive deletion available only through `delete`.
4. Extend Phase 4 config selection into active formatter behavior, bootstrap execution, stop-on-failure, profile-aware execution, dry-run/confirmation behavior for generation, and verbosity.
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
5. Re-validate Java 25 stream gatherer metadata against the target JDK 25 API documentation during quality-matrix work before relying on environment-specific behavior.
6. Include first-MVP tests for existing type detection, missing type detection, prompt output for a missing type, class-like kind generation, and write gating.
7. Maintain and expand ADR 0004 correction tests for constructor default/comment policy, CLI help/parser behavior, class update safety, PHPSpec construction override/lazy semantics, factory construction skeleton generation, generated typed proxy compilation, matcher semantics and negation, throw/during/duringInstantiation behavior, method generation for missing methods and default returns, and user-manual examples when those examples become testable.
8. Maintain and expand Phase 4 configuration, naming, and filter tests for defaults, parser diagnostics, suite/path/package-prefix precedence, constructor-policy config defaults and overrides, class/example filters, and missing-class flow with inferred and explicit config.

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
6. Document configuration files, construction, constructor policy, typed matcher syntax, method generation, limitations, commands, examples, the first-MVP generator flow, and later advanced generator behavior according to what is implemented.

Acceptance criteria:

- Documentation reflects implemented behavior.
- `docs/usermanual/Home.md` and `docs/usermanual/_Sidebar.md` expose the same navigation topics.
- ADRs link to relevant ARC42 sections.
- Test and quality claims are backed by produced reports.
