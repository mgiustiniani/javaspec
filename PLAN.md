# javaspec Implementation Plan

This plan defines the initial delivery path for javaspec, a Java 8-compatible, zero-runtime-dependency Java port inspired by phpspec.

## Current Implementation Status — Implemented and Verified

Phases 2 through 11 are implemented and verified. Phase 12 compatibility and quality verification is fully complete through the Distrobox multi-JDK matrix for Java 8, 11, 17, 21, and 25.

- Phase 2 implemented the Java 8 Maven project, zero-runtime-dependency guard, PHPSpec-style `describe`/`run` split, specification/support skeletons, and gated production type/method generation.
- Phase 3 implemented Java LTS target profiles `java8`, `java11`, `java17`, and `java21`, plus the forward-looking `java25` profile, the profile catalog, API-symbol metadata, target-profile compatibility checks, and reflection-only API availability probes.
- Phase 4 implemented the zero-runtime-dependency line-based configuration format, `--config <file>` and `--suite <name>` integration, suite-level spec/source directories, suite package prefixes, naming convention integration, and suite/class/example discovery filters.
- The Phase 5/6 MVP implemented `org.javaspec.runner`, keeps `javaspec run` discovery/generation/update behavior, and executes filtered discovered examples when compiled spec classes are available on the effective classloader.
- Runner behavior: existing `DiscoveredSpec`/`SpecExample` metadata remains the execution source, so suite/class/example filters remain effective; each example gets a fresh spec instance; optional public no-arg `let()` runs before each example and optional public no-arg `letGo()` runs after each example.
- Result states are `PASSED`, `FAILED` for `AssertionError`, `BROKEN` for non-assertion throwables/lifecycle/reflection errors, and `SKIPPED` for non-loadable spec classes or missing reflected example methods. The CLI exits `1` for failed or broken executable examples.
- Phase 7 expanded `Matchable` with negated equality aliases, type/instance aliases, `shouldImplement`, string negations, count/empty helpers for arrays/collections/maps/character sequences/iterables, and map key/value helpers.
- Phase 7 expanded `ObjectBehavior` direct convenience assertion methods that delegate through `match(actual)`, kept `MatcherRegistry` zero-dependency with a default negated-equality matcher, and updated `SpecDiscovery` so expanded chained matcher names participate in method-discovery/default-return inference where applicable.
- Phase 8 added zero-runtime-dependency interface doubles under `org.javaspec.doubles` using JDK dynamic proxies. The MVP supports ordinary interface doubles, return-value stubbing by method name or exact arguments, null and array-content argument matching, call history, called/not-called/exact-count verification, deterministic `toString`/`equals`/`hashCode`, Java default returns for unstubbed methods, and `ObjectBehavior` double convenience APIs.
- Phase 9 expanded `javaspec run` with run-only controls: `--dry-run`, `--stop-on-failure`, `--formatter <progress|pretty>`, `--profile <java8|java11|java17|java21|java25>`, and `--verbose`. Dry-run never writes or prompts and reports pending would-generate/would-update actions; stop-on-failure stops after the first FAILED or BROKEN executable example; progress output is concise and summary-oriented; pretty output prints per-example status lines plus details; CLI formatter/profile options override valid config/default selections; verbose output prints selected run settings.
- Phase 10 implemented the current advanced code-generation increment for interface-style methods. Missing ordinary interface skeletons render discovered non-static method declarations and skip static descriptors; missing annotation skeletons render compatible no-arg non-static elements and skip incompatible descriptors; missing sealed-interface skeletons render root declarations plus nested permitted implementation bodies with Java default returns; existing ordinary interfaces and annotations can receive missing declarations/elements source-preservingly and idempotently; existing sealed-interface source updates are intentionally deferred.
- Phase 11 implemented public zero-dependency run formatter contracts and deterministic built-in formatter registration, preserving compatible `progress` and `pretty` CLI output; added minimal programmatic extension contracts through `JavaspecExtension`/`Extension` and `ExtensionContext`; and added `javaspec run --report <file>` plus alias `--report-file <file>` for UTF-8 JSON runner reports with `schemaVersion` 1, summary counts, specs, examples, nullable failures, throwable class/message, and stack trace lines.
- Phase 11 reporting behavior: `--report` is run-only and rejected by `describe`; `--verbose` prints the report path when specified; no-spec runs write a valid empty report; normal passing/failing/broken/skipped runs write the report after summary rendering; failed or broken executable examples still exit `1` after the report write; dry-run pending generation/update exits before execution and does not write a report; report write failures print I/O diagnostics and exit `70`.
- Known limitations: the CLI runner does not compile source/spec files itself; source-only or otherwise unavailable spec classes are skipped/not executable. Selected profiles are validated and reported but not deeply enforced during execution yet. External extension discovery/loading is not implemented, so CLI formatter selection remains limited to built-in `progress` and `pretty` even though programmatic formatter registration APIs exist. JSON reporting is limited to schemaVersion 1 and has no config-level report destination or alternate machine-readable format. Count and emptiness checks on a generic `Iterable` consume the iterable and can hang on infinite iterables. Existing sealed-interface source updates are skipped until nested permitted implementations can be updated source-preservingly. Phase 8 doubles do not support concrete class/static/constructor/final-class doubles, primitives, arrays, annotations, enums, wildcard argument matchers, exception/callback stubbing, bytecode libraries, or default-interface-method invocation.
- Phase 12 Distrobox multi-JDK verification completed on 2026-06-03 with Distrobox `1.8.2.5` and Podman `5.8.2`: Java 8 (`1.8.0_492`), Java 11 (`11.0.31`), Java 17 (`17.0.19`), Java 21 (`21.0.11 LTS`), and Java 25 (`25.0.3 LTS`) containers each passed `mvn clean` and `mvn verify` with 364 tests, 0 failures, 0 errors, and 0 skipped.
- JDK 17+ matrix runs emitted only the expected `-source 8` / `-target 1.8` bootstrap/obsolete-option warnings.
- Runtime dependency verification completed in `javaspec-jdk25-matrix`: `mvn dependency:tree -Dscope=runtime` passed and showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.
- Java 25 Gatherer runtime verification completed in `javaspec-jdk25-matrix`: reflection probing passed for `java.util.stream.Gatherer`, `Gatherer$Downstream`, `Gatherer$Integrator`, `Gatherer$Integrator$Greedy`, and `java.util.stream.Gatherers`.

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
6. Method generation discovers typed proxy calls and can generate missing instance method skeletons with Java 8-compatible default returns for body-bearing production kinds. Phase 10 extends the same descriptor flow to ordinary interface declarations, compatible annotation elements, and missing sealed-interface skeleton root/nested implementation methods. Generated typed spec support skips static factory descriptors because construction methods are not instance subject proxies. `run --generate` writes non-interactively; without `--generate`, `run` prompts before adding supported missing methods to an existing source file.
7. The matcher subset now includes identity/equality aliases and negations (`shouldBe`, `shouldNotBe`, `shouldEqual`, `shouldNotEqual`, `shouldReturn`, `shouldNotReturn`, `shouldBeLike`, `shouldNotBeLike`, `shouldBeEqualTo`, `shouldNotBeEqualTo`), type/instance aliases (`shouldHaveType`, `shouldBeAnInstanceOf`, `shouldReturnAnInstanceOf`), `shouldImplement`, string containment/start/end/pattern checks and their negations, count/empty helpers for arrays/collections/maps/character sequences/iterables, map key/value helpers, and custom matchers that can evaluate null subjects.
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
- `src/main/java/org/javaspec/generation/ClassGenerationPlan.java`, `ClassSkeletonGenerator.java`, `ClassFileGenerator.java`, `TypeGenerationPlan.java`, `TypeSkeletonGenerator.java`, and `TypeFileGenerator.java` — production class-like skeleton planning and explicit file writing after prompt acceptance or `run --generate`, including inferred constructor, method-body, interface-declaration, annotation-element, sealed-interface nested implementation, and static factory method skeletons.
- `src/main/java/org/javaspec/generation/SpecGenerationPlan.java`, `SpecSkeletonGenerator.java`, `SpecFileGenerator.java`, and `SpecSupportFileGenerator.java` — PHPSpec-style specification and typed support skeleton planning/writing for `describe` and `run` support updates.
- `src/main/java/org/javaspec/generation/ConstructorPolicy.java`, `ClassConstructorUpdater.java`, and `ClassMethodUpdater.java` — constructor policy handling and source-preserving method-body/static-factory insertion plus ordinary-interface declaration and annotation-element insertion for existing production sources where supported.
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
- Phase 12 completed the Distrobox compatibility and quality matrix: Java 8, 11, 17, 21, and 25 containers all passed `mvn clean` and `mvn verify`; the Java 25 runtime Gatherer probe and runtime dependency audit also passed.

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
10. Bootstrap values are comma-separated metadata only and are not executed yet. Profile and formatter values were introduced as validated configuration values in Phase 4; Phase 9 now selects active profiles and built-in formatter output, while deep profile enforcement remains future work.
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
- Pending examples, bootstrap execution, deep profile-aware execution, and reporting beyond the Phase 11 JSON runner report remain later work. Stop-on-failure and built-in progress/pretty formatter behavior are implemented in Phase 9 and routed through formatter contracts in Phase 11.

## Phase 7 Matcher/Expectation Expansion Status — Implemented and Verified

Implementation summary:

1. Expanded `org.javaspec.matcher.Matchable` with identity/equality aliases and negations: `shouldBe`, `shouldNotBe`, `shouldEqual`, `shouldNotEqual`, `shouldReturn`, `shouldNotReturn`, `shouldBeLike`, `shouldNotBeLike`, `shouldBeEqualTo`, and `shouldNotBeEqualTo`.
2. Added type and assignability matcher aliases: `shouldHaveType`, `shouldBeAnInstanceOf`, `shouldReturnAnInstanceOf`, and `shouldImplement`.
3. Expanded string and containment helpers: `shouldContain`, `shouldNotContain`, `shouldStartWith`, `shouldNotStartWith`, `shouldEndWith`, `shouldNotEndWith`, `shouldMatchPattern`, and `shouldNotMatchPattern`.
4. Added count and emptiness helpers for arrays, collections, maps, character sequences, and generic iterables: `shouldHaveCount`, `shouldBeEmpty`, and `shouldNotBeEmpty`.
5. Added map-specific helpers: `shouldHaveKey`, `shouldNotHaveKey`, `shouldHaveValue`, and `shouldNotHaveValue`.
6. Expanded `ObjectBehavior` direct convenience assertion methods for equality/negation aliases, type/instance/implementation checks, containment, count/empty checks, map key/value checks, and string negations. These methods delegate through `match(actual)` so direct assertions and fluent `Matchable` assertions share the same implementation.
7. Kept `MatcherRegistry` zero-runtime-dependency and added a default `negated-equality` matcher available through the registry fallback while retaining the existing identity, equality, and negated-identity defaults.
8. Updated `SpecDiscovery` so expanded chained matcher names are recognized for method-discovery/default-return inference where applicable.
9. Preserved custom matcher support through `shouldMatch(...)` and registry registration without adding runtime dependencies.

Verification summary:

- `mvn verify` passed after the Phase 7 matcher/expectation expansion.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Known limitation:

- Count and emptiness checks on a generic `Iterable` iterate the iterable to compute a size. This consumes one-shot iterables and can hang on infinite iterables.

## Delegation Rule for Later Work

Further implementation work must be delegated to the appropriate workflow agents. The documenter must not create application source code, Maven build files, scaffolding, or tests. Future work should be delegated as follows by the parent workflow:

- Additional Java/Maven build changes: Java scaffolding/build agent.
- Production code under `org.javaspec`: Java implementation agent.
- Test code, compatibility matrix execution, and coverage reports: Java tester/quality agents.
- C4 diagrams, if requested: c4model via the documenter child delegation path.

## Requirement Traceability

| Requirement | Documentation artifact | Implementation status |
|---|---|---|
| Java 8-compatible binary | README, ARC42 constraints, ADR 0001, test report | Baseline implemented in Phase 2; Phase 12 Java 8 Distrobox verification passed on Temurin `1.8.0_492` with `mvn clean` and `mvn verify` running 364 tests, 0 failures, 0 errors, and 0 skipped. Supplemental compiler, bytecode, and constant-pool audits also preserved the Java 8 compatibility gate. |
| Zero runtime dependencies | README, ARC42 constraints, ADR 0002 | Implemented in Phase 2 and must be preserved through later phases. |
| Dependencies allowed only in test scope | README, ADR 0002 | Implemented in Phase 2 with JUnit in test scope only. |
| PHPSpec-style describe/run generation split | ADR 0003, this plan, user manual | Implemented: `describe` writes only spec/support files; `run` owns production type, constructor, static factory, and instance method generation/update; prompts are used where required, and `run --generate` answers yes non-interactively. |
| Constructor-policy correction | ADR 0004, this plan, user manual | Implemented and verified: exact states `delete`, `preserve`, `comment`; default `comment`; destructive deletion only with `--constructor-policy delete`; empty no-op unmatched constructors may be removed safely. |
| PHPSpec construction semantics | ADR 0004, verified PHPSpec construction docs/source, this plan, user manual | Runtime implemented in `ObjectBehavior`: lazy subject construction, `beConstructedWith`, factory/named construction forms, override-before-instantiation semantics, failure on change after instantiation, and `duringInstantiation()`. Generation implemented: `beConstructedWith(...)` remains constructor descriptor generation; factory/named forms with string-literal Java-identifier names generate static factory method skeletons returning the described type; non-string-literal factory names are ignored for generation. The MVP reflection lifecycle now runs compiled examples with fresh spec instances and optional public no-arg `let()`/`letGo()`; full PHPSpec parity remains future work. |
| Typed proxy matcher syntax | ADR 0004, verified PHPSpec matcher docs/source, this plan, user manual | Implemented: generated subject-specific support classes expose typed proxy methods and throw proxies while existing `match(value).should...` usage remains available. |
| Phase 7 matcher/expectation expansion | README, user manual, ARC42 section 5, this plan | Implemented and verified: `Matchable` includes expanded equality/negation, type/instance, implementation, string, count/empty, and map key/value helpers; `ObjectBehavior` direct convenience methods delegate through `match(actual)`; `MatcherRegistry` keeps a zero-dependency default negated-equality matcher; `SpecDiscovery` recognizes expanded chained matcher names for method-discovery/default-return inference where applicable. Generic `Iterable` count/empty checks consume the iterable and can hang on infinite iterables. |
| Phase 8 MVP collaborators/doubles | README, user manual, ARC42 section 5, ADR 0007, this plan | Implemented and verified: `org.javaspec.doubles` provides JDK-proxy ordinary-interface doubles, method-name and exact-argument return stubs, null and array-content argument matching, immutable call history, called/not-called/exact-count verification, deterministic object methods, Java default returns for unstubbed methods, and `ObjectBehavior` double conveniences. Limitations are explicit: no concrete class/static/constructor/final-class doubles, primitives, arrays, annotations, enums, wildcard matchers, exception/callback stubbing, bytecode libraries, or default-interface-method invocation. |
| Method generation | ADR 0004, this plan, user manual | Implemented and verified: discovery from typed proxy/throw calls, direct subject/setter calls, static factory construction markers, and the expanded chained matcher names where applicable; Java 8-compatible method-body and static factory skeleton generation for class/final/sealed class/enum/record sources remains unchanged; static factory descriptors are skipped by generated support proxies; `--generate` writes non-interactively and interactive `run` prompts before updating existing source files. |
| Phase 10 interface-style method generation | README, user manual, ARC42 section 5, this plan | Implemented and verified: missing ordinary interface skeletons render non-static method declarations and skip static descriptors; missing annotations render compatible no-arg non-static elements and skip incompatible descriptors; missing sealed interfaces render root method declarations and nested permitted implementations with Java default-return bodies; existing ordinary interfaces and annotations receive missing declarations/elements source-preservingly and idempotently; existing sealed-interface source updates remain deferred. |
| Configuration model and inferred defaults | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `JavaspecConfiguration.defaults()` provides the default suite `default`, Maven-style spec/source roots, `spec` package prefix, empty production package prefix, `java8` profile, `progress` formatter, `comment` constructor policy, and empty bootstrap hooks when no config file is supplied. |
| Constructor-policy config default | ADR 0004, ADR 0005, user manual, this plan | Implemented in Phase 4: config key `constructorPolicy`/`constructor-policy` accepts only `delete`, `preserve`, and `comment`; `comment` remains the inferred and config default, and `run --constructor-policy` overrides config explicitly. |
| Explicit suites, paths, profile, and formatter config | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4 and expanded in Phase 9: `--config <file>` and `--suite <name>` select suite configuration; selected-suite `specDir`/`sourceDir` drive `describe`/`run` unless CLI path options override them; selected-suite `specPackagePrefix`/`packagePrefix` drive naming; config `profile` and `formatter` provide run defaults that valid CLI `--profile` and `--formatter` override. Profile selection is validated but not deeply enforced yet. |
| Naming convention integration | README, user manual, ARC42 section 5, ADR 0005, this plan | Implemented in Phase 4: `SpecNamingConvention` maps production names to spec/support packages using configured suite package prefixes, validates naming metadata, and is used by describe, discovery, and support generation. |
| Suite, class, and example filters | README, user manual, ARC42 section 5, this plan | Implemented in Phase 4 and reused by the Phase 5/6 MVP runner: `--suite` selects the configured suite; repeatable `--class` filters by described or spec class names; repeatable `--example` filters by example method name, display name, or source-order index; filtered `DiscoveredSpec`/`SpecExample` metadata controls both generation/update and reflection execution. |
| Phase 5/6 MVP reflection runner | README, user manual, ARC42 section 5, ADR 0006, this plan | Implemented and verified: after discovery/generation/update, `javaspec run` executes examples when compiled spec classes are available on the effective classloader; each example uses a fresh spec instance with optional public no-arg `let()` and `letGo()`; results are `PASSED`, `FAILED`, `BROKEN`, or `SKIPPED`; CLI summary exits `1` for failed/broken executable examples; source-only/unavailable spec classes are skipped because the CLI does not compile them. Phase 9 adds `--stop-on-failure` to stop after the first FAILED or BROKEN executable example while the default remains executing all discovered metadata. |
| Phase 9 CLI expansion | README, user manual, ARC42 section 5, this plan | Implemented and verified: `run --dry-run` performs no writes and no prompts, reports would-generate/would-update actions for related specs/support, support updates, constructors, method bodies/declarations/elements, and missing production type generation, exits `1` when pending work exists, and exits `0` when no pending changes exist and examples pass or skip; `run --stop-on-failure` stops after the first FAILED or BROKEN executable example; `run --formatter <progress|pretty>` selects concise or per-example output and overrides config/default; `run --profile <java8|java11|java17|java21|java25>` validates/selects the profile and overrides config without deep enforcement yet; `run --verbose` prints selected run settings; all Phase 9 controls are rejected for `describe`. |
| Phase 11 formatter/reporting/extension increment | README, user manual, ARC42 section 5, this plan | Implemented and verified: built-in output uses the public zero-dependency `RunFormatter` contract and deterministic `RunFormatterRegistry`; `progress` and `pretty` behavior remains compatible; `JavaspecExtension`/`Extension` and `ExtensionContext` allow programmatic run formatter registration; `javaspec run --report <file>` and alias `--report-file <file>` write UTF-8 JSON reports with `schemaVersion` 1, summary counts, specs, examples, nullable failure details, throwable class/message, and stack trace lines. Reports are run-only, rejected by `describe`, written for no-spec/passing/failing/broken/skipped runs after normal output, skipped for dry-run pending generation/update, and report write failures exit `70`. External extension discovery/loading is not implemented. |
| Missing-class flow with config | User manual, this plan | Implemented in Phase 4: `run` uses inferred defaults without a config file and selected-suite paths/naming with explicit config, preserving the existing missing-production prompt and `--generate` non-interactive generation behavior. |
| Maven implementation | This plan | Implemented in Phase 2. |
| Package base `org.javaspec` | README, this plan | Implemented in Phase 2 and retained for future work. |
| Target Java LTS profiles 8, 11, 17, 21, 25 | README, ARC42 section 5, ADR 0001, Java LTS research, this plan, test report | Implemented in Phase 3: `TargetProfile` and `ProfileCatalog` encode `java8`, `java11`, `java17`, `java21`, and `java25`; Phase 12 executed the full Distrobox runtime matrix for Java 8, 11, 17, 21, and 25, and every profile container passed `mvn clean` and `mvn verify`. |
| Post-Java 8 APIs as metadata/reflection | README, ARC42 section 5, ADR 0001, Java LTS research, this plan, test report | Implemented in Phase 3: Java 11+ API symbols are stored as metadata strings and probed only through `ApiAvailabilityProbe`; no post-Java-8 direct production imports are required. Phase 12 constant-pool audit passed with 0 direct post-Java-8 API references and 70 intentional metadata string hits. |
| Java 8 data-structure list | README, ARC42 section 5, Java LTS research, this plan | Implemented in Phase 3: representative Java 8 collection, container, array, optional, atomic/reference, and stream symbols are cataloged and tested. |
| Later LTS data-structure additions | README, ARC42 section 5, Java LTS research, this plan, test report | Implemented in Phase 3: representative Java 11 collection factories/collectors/optional/stream additions, Java 17 stream/record/sealed metadata, Java 21 sequenced collections, and Java 25 stream-support metadata are cataloged. Phase 12 verified the Java 25 Gatherer symbols with a Java 25 runtime reflection probe; ongoing synchronization remains future maintenance work. |
| Java 25 stream-support metadata | README, ARC42 section 5, Java LTS research, this plan, test report | Implemented in Phase 3: `STREAM_GATHERERS` and metadata for `java.util.stream.Gatherer`, nested gatherer types, and `java.util.stream.Gatherers` are present; Phase 12 Java 25 runtime reflection probing passed for `java.util.stream.Gatherer`, `Gatherer$Downstream`, `Gatherer$Integrator`, `Gatherer$Integrator$Greedy`, and `java.util.stream.Gatherers`. |
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
- Treat bootstrap hooks as parsed metadata until bootstrap execution is implemented; treat profile and formatter settings as active run selections, with CLI overrides over config/default values, while profile selection remains validation/reporting only until deep enforcement is implemented.
- Keep built-in formatter rendering behind the zero-dependency `RunFormatter` contract and deterministic `RunFormatterRegistry`; keep CLI formatter selection limited to built-in names until external extension loading is explicitly implemented.
- Keep run reports dependency-free and based on the immutable runner result model; write `--report` output only for run paths that reach no-spec handling or runner summary rendering, skip reports when dry-run exits before execution because pending work exists, and treat report write failures as exit `70` I/O failures.
- Treat the Phase 11 extension API as programmatic registration only; do not imply config-driven loading, classpath scanning, `ServiceLoader`, or plugin activation until implemented and documented.
- Use `DiscoveredSpec` and `SpecExample` as the execution selection source so suite, class, and example filters remain effective for the runner.
- Treat the CLI runner as a classpath reflection executor, not an in-process compiler; source-only or unavailable spec classes are skipped/not executable until compiled classes are present on the effective classloader.
- Prefer generated subject-specific typed support/proxy classes while keeping explicit `match(value)` style APIs available.
- Keep `Matchable`, `ObjectBehavior` direct convenience assertions, and `SpecDiscovery` matcher-name recognition synchronized when matcher names are added.
- Keep core doubles interface-only and JDK-proxy based unless a future ADR authorizes optional advanced integrations; reject unsupported double targets with clear diagnostics.
- Preserve exact-argument double semantics for `null` values and array contents, and keep `ObjectBehavior` double convenience APIs synchronized with `org.javaspec.doubles` control behavior.
- Document that count/empty checks on generic `Iterable` values consume the iterable and are unsafe for infinite iterables.
- Insert generated class/final-class/sealed-class/enum/record method bodies, interface method declarations, and annotation elements source-preservingly where supported, with confirmation or documented non-interactive behavior; use Java default returns where generated method bodies are required, and keep existing sealed-interface source updates deferred until nested permitted implementations can also be updated safely.
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

- Complete PHPSpec-style runner lifecycle beyond the Phase 5/6 MVP reflection runner, including pending examples, bootstrap execution, deep profile-aware execution, and reporting beyond the Phase 11 JSON runner report. Stop-on-failure and built-in progress/pretty formatters are implemented in Phase 9 and routed through formatter contracts in Phase 11.
- Broader interface and annotation generation beyond the supported Phase 10 method declarations/elements, plus enum generation beyond minimal skeletons.
- Private constructor source generation and broader named-constructor customization beyond the current static factory skeleton support.
- Template systems beyond the minimal class-like/spec/support skeleton need.
- Return constant generation from expectations beyond Java 8-compatible default returns.
- Full PHPSpec matcher parity beyond the implemented Phase 7 matcher subset.
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
12. Kept bootstrap values as comma-separated metadata only. Profile and formatter values were parsed/validated configuration values in Phase 4 and are now consumed by Phase 9 run profile/formatter selection.

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
- Explicit config can select suite, paths, target profile, formatter, constructor policy, spec package prefix, and production package prefix. Current `describe`/`run` behavior uses selected suite paths, package prefixes, and constructor policy; Phase 9 `run` also consumes configured profile and formatter defaults, while bootstrap remains parsed metadata until later runner features.
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

1. Extend discovery diagnostics and source locations as needed by future reporting layers.
2. Preserve the source-discovery metadata contract when future bootstrap and deeper profile behavior is added.
3. Keep Phase 9 stop-on-failure and formatter behavior aligned with the existing discovery metadata contract.

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

1. Add pending examples, bootstrap execution, deep profile-aware execution, and reporting beyond the Phase 11 JSON runner report; stop-on-failure, verbosity, and built-in progress/pretty formatter behavior are implemented in Phase 9 and routed through formatter contracts in Phase 11.
2. Expand source-location diagnostics for failed/broken examples where available.
3. Continue to refine typed proxy matcher diagnostics and method-generation reporting without forcing eager subject construction.
4. Keep ADR 0004 construction semantics stable as the runner grows beyond the MVP lifecycle.

Acceptance criteria status:

- Examples run with isolated spec instances.
- `let()`, example execution, and `letGo()` interact predictably for the MVP public no-arg lifecycle.
- Failed and broken examples include throwable summary details in CLI output.
- Exit code `1` is stable for failed/broken executable examples; skipped-only runs remain successful.
- Source-only or unavailable spec classes are skipped until compiled classes are present on the effective classloader.

### Phase 7 — Expectations and Matchers (Completed)

**Status:** Implemented and verified for the current zero-dependency matcher/expectation expansion. Full PHPSpec matcher parity, approximate equality, richer object-state matchers, iteration/yield variants, and extension registration beyond the current registry remain future work.

Implemented scope:

1. `Matchable<T>` exposes PHPSpec-inspired equality/identity aliases and negations: `shouldBe`, `shouldNotBe`, `shouldEqual`, `shouldNotEqual`, `shouldReturn`, `shouldNotReturn`, `shouldBeLike`, `shouldNotBeLike`, `shouldBeEqualTo`, and `shouldNotBeEqualTo`.
2. Type and assignability aliases are implemented: `shouldHaveType`, `shouldBeAnInstanceOf`, `shouldReturnAnInstanceOf`, and `shouldImplement`.
3. String and containment matchers are expanded: `shouldContain`, `shouldNotContain`, `shouldStartWith`, `shouldNotStartWith`, `shouldEndWith`, `shouldNotEndWith`, `shouldMatchPattern`, and `shouldNotMatchPattern`.
4. Count and emptiness helpers are implemented for arrays, collections, maps, character sequences, and iterables: `shouldHaveCount`, `shouldBeEmpty`, and `shouldNotBeEmpty`.
5. Map key/value helpers are implemented: `shouldHaveKey`, `shouldNotHaveKey`, `shouldHaveValue`, and `shouldNotHaveValue`.
6. `ObjectBehavior` direct convenience assertions were expanded and delegate through `match(actual)` so direct assertions share the same matcher behavior as fluent typed proxy or explicit wrapper usage.
7. `MatcherRegistry` keeps zero runtime dependencies and exposes a default negated-equality matcher while preserving custom matcher registration.
8. `SpecDiscovery` recognizes the expanded chained matcher names for method-discovery/default-return inference where applicable.

Verification:

- `mvn verify` passed.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Known limitation:

- Count and emptiness on a generic `Iterable` consume the iterable and can hang on infinite iterables.

Acceptance criteria status:

- Generated typed proxy/support classes continue to expose PHPSpec-like Java syntax through `Matchable<T>`.
- Negated equality, string, count/empty, and map key/value expectations are implemented for the documented subset.
- Throw matchers still support method calls and `duringInstantiation()` from the earlier runner/construction work.
- Custom matcher registration still requires no runtime dependencies.

### Phase 8 — Collaborators and Doubles (Completed)

**Status:** Implemented and verified for the current zero-dependency interface-doubles MVP.

**Relevant ADRs:** ADR 0002, ADR 0007.

Implementation summary:

1. Added `org.javaspec.doubles` with a JDK dynamic proxy implementation for ordinary interface doubles.
2. Added public factory/control/history/verification APIs through `Doubles`, `InterfaceDouble`, `DoubleControl`, `MethodStub`, `CallVerifier`, and `Call`.
3. Added `ObjectBehavior` convenience APIs for `doubleFor`, `interfaceDouble`, `doubleControl`/`inspectDouble`, call history, call counts, called/not-called assertions, and exact-count assertions.
4. Implemented stubbing by method name with any arguments and by method name with exact arguments; exact matching supports `null` values and array-content comparison.
5. Implemented call recording and immutable call-history snapshots, including method-name and exact-argument filtering.
6. Implemented verification for called, not called, called once, and exact call count checks through fluent and direct APIs.
7. Handled `toString`, `equals`, and `hashCode` deterministically in the proxy invocation handler.
8. Returned Java defaults for unstubbed methods: primitive defaults, `null` for reference types, and no-op behavior for `void` methods.
9. Added explicit unsupported-target diagnostics for `null`, primitives, arrays, annotations, enums, concrete classes, and final classes.
10. Preserved the zero-runtime-dependency policy; no bytecode, mocking, assertion, or callback libraries were added.

Verification:

- `mvn verify` passed with 328 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Known limitations:

- The core runtime supports ordinary interface doubles only.
- Concrete class, final class, static method, and constructor doubles are not implemented.
- Wildcard argument matchers, predicate matchers, exception stubbing, callback stubbing, sequences, and side-effect stubbing are not implemented.
- Default interface methods are not invoked by the proxy handler.
- Advanced doubles that require bytecode libraries must remain future optional integrations outside the core runtime unless a future ADR changes the policy.

Acceptance criteria status:

- Interface doubles work on Java 8 without third-party libraries.
- Limitations are explicit and tested.
- The API does not require non-JDK runtime artifacts.

### Phase 9 — CLI Expansion (Completed)

**Status:** Implemented and verified for the current run-control increment.

Implementation summary:

1. Preserved the existing `describe`/`desc` spec-only behavior and the `run` discovery/generation/update/execution flow.
2. Added `javaspec run --dry-run`: no files are written, no prompts are shown, and the CLI reports would-generate/would-update actions for related specs/support, support updates, constructor changes, supported method bodies/declarations/elements, and missing production type generation.
3. Defined dry-run exits: `1` when pending generation/update work exists; `0` when no pending changes exist and executable examples pass or are skipped-only; failed or broken executable examples still produce exit `1`.
4. Added `javaspec run --stop-on-failure`: the runner stops after the first FAILED or BROKEN executable example. Without this flag, the default remains to process all discovered example metadata.
5. Added `javaspec run --formatter <progress|pretty>`: `progress` is concise and summary-oriented; `pretty` prints per-example status lines plus failed/broken/skipped details. A valid CLI formatter overrides config; otherwise the configured formatter or default `progress` is used.
6. Added `javaspec run --profile <java8|java11|java17|java21|java25>`: the profile is validated and selected, and a valid CLI profile overrides config. Deep profile enforcement is not implemented yet.
7. Added `javaspec run --verbose`: prints selected suite, spec root, source root, spec package prefix, production package prefix, constructor policy, profile, formatter, dry-run, and stop-on-failure.
8. Rejected the new run-only flags for `describe`/`desc`, along with existing run-only controls such as `--generate`, `--constructor-policy`, `--class`, and `--example`.
9. Preserved zero-runtime-dependency CLI parsing; no external CLI/formatter dependency was added.

Verification:

- `mvn verify` passed with 338 tests.
- `mvn dependency:tree -Dscope=runtime` showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT`.

Acceptance criteria status:

- CLI works on Java 8-compatible production code.
- Unknown commands and invalid options produce clear diagnostics.
- CLI help documents constructor policy, dry-run, stop-on-failure, formatter, profile, verbose, filters, and run/describe ownership.
- `describe` preserves first-MVP spec-only behavior, while `run` preserves missing-production-class and missing-method suggestion behavior.
- CLI generation-related actions remain gated by explicit confirmation, dry-run reporting, or documented non-interactive `--generate` behavior.

### Phase 10 — Advanced Code Generation (Completed Increment)

**Owner:** Java implementation agent.

**Status:** Implemented and verified for the current advanced code-generation increment focused on interface-style methods. Broader advanced generation remains future work.

Implementation summary:

1. Preserved the existing class, final-class, sealed-class, enum, and record method-body generation behavior.
2. Updated missing production `INTERFACE` skeleton rendering so discovered non-static methods are emitted as declarations ending in `;`; static descriptors are skipped.
3. Updated missing production `ANNOTATION` skeleton rendering so compatible no-argument non-static descriptors are emitted as annotation elements; incompatible descriptors are skipped.
4. Updated missing production `SEALED_INTERFACE` skeleton rendering so root method declarations are emitted and generated nested permitted classes implement those methods with Java default-return bodies, keeping generated Java 17 source forms valid.
5. Updated existing ordinary interface sources so missing method declarations can be inserted source-preservingly and idempotently.
6. Updated existing annotation sources so compatible missing elements can be inserted source-preservingly and idempotently.
7. Intentionally skipped existing sealed-interface source updates for now because source-preserving updates must also update nested permitted implementations safely.
8. Production files changed: `src/main/java/org/javaspec/generation/TypeSkeletonGenerator.java` and `src/main/java/org/javaspec/generation/ClassMethodUpdater.java`.
9. Test files changed: `src/test/java/org/javaspec/generation/TypeSkeletonGeneratorTest.java` and `src/test/java/org/javaspec/generation/ClassMethodUpdaterTest.java`.

Verification:

- `mvn -Dtest='org.javaspec.generation.*Test' test` passed with 84 generation tests.
- `mvn verify` passed with 345 tests.
- `mvn dependency:tree -Dscope=runtime` passed and showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.

Known limitations:

- Existing sealed-interface source updates are deferred until the updater can insert both root declarations and matching nested permitted implementation bodies source-preservingly.
- Annotation element generation only emits descriptors that are valid Java annotation elements; descriptors with parameters, static descriptors, `void`, `Object`, or otherwise incompatible return types are ignored for annotation sources.
- Source parsing and generation remain Java 8-compatible heuristics rather than a full Java parser.

Remaining advanced-generation work:

1. Keep the ADR 0004 correction behavior stable in spec/support/proxy generation, constructor policy handling, instance method skeleton generation, and static factory skeleton generation.
2. Refine static factory/named-constructor generation and add private constructor generation only after current construction and factory semantics remain stable.
3. Provide templates for spec classes, support/proxy classes, examples, and generated production code where useful.
4. Add return constant generation only after matcher and example semantics can justify it.
5. Extend enum generation and broader interface/annotation generation only when the production type skeleton flow remains stable and documented.
6. Confirm before writing files unless a non-interactive flag or documented policy is provided.
7. Keep generated Java source compatible with the configured target profile and Java 8 binary rules.

Acceptance criteria status:

- Generation remains deterministic and reviewable.
- Existing constructor updates still follow the ADR 0004 policy states and defaults.
- Generated typed support/proxy classes continue to preserve PHPSpec-like Java syntax.
- Missing method prompts remain actionable and reuse the existing `--generate` non-interactive behavior.
- Existing ordinary interface and annotation updates are source-preserving and idempotent.
- Advanced generation features do not expand the zero-runtime-dependency surface.

### Phase 11 — Formatters, Reporting, and Extensions (Completed Increment)

**Owner:** Java implementation agent.

**Status:** Implemented and verified for the current formatter/reporting/extension increment. External extension loading and broader plugin behavior remain future work.

Implementation summary:

1. Added the public zero-dependency `org.javaspec.formatter.RunFormatter` contract and deterministic `RunFormatterRegistry`.
2. Moved built-in run output behind `ProgressRunFormatter`, `PrettyRunFormatter`, and `RunFormatterSupport`, preserving Phase 9-compatible `progress` and `pretty` CLI output.
3. Added minimal extension lifecycle contracts under `org.javaspec.extension`: `JavaspecExtension`, short-name alias `Extension`, and `ExtensionContext`.
4. `ExtensionContext` exposes the run formatter registry through `runFormatterRegistry()` and `runFormatters()` so extensions can register formatters programmatically.
5. Added `org.javaspec.reporting.RunReportWriter`, a dependency-free UTF-8 JSON writer for immutable runner results.
6. Added `javaspec run --report <file>` and alias `--report-file <file>`.
7. JSON reports use `schemaVersion` 1 and include whole-run summary counts, specs, examples, nullable failure details, throwable class/message, and stack trace lines.
8. `--report` is run-only and rejected by `describe`/`desc`; `--verbose` prints the report path when specified.
9. No-spec runs with `--report` write a valid empty report. Passing, failing, broken, and skipped-only runs write reports after normal summary rendering; failed or broken executable examples still exit `1` after the report write.
10. Dry-run pending generation/update exits before execution and does not write a report. Report write failures produce I/O diagnostics, include the report path, and exit `70`.
11. No external extension loading is implemented yet: no config-driven activation, classpath scanning, `ServiceLoader`, plugin lookup, or CLI use of extension-provided formatter names.

Production files changed/added:

- `src/main/java/org/javaspec/cli/Main.java`
- `src/main/java/org/javaspec/formatter/RunFormatter.java`
- `src/main/java/org/javaspec/formatter/RunFormatterRegistry.java`
- `src/main/java/org/javaspec/formatter/ProgressRunFormatter.java`
- `src/main/java/org/javaspec/formatter/PrettyRunFormatter.java`
- `src/main/java/org/javaspec/formatter/RunFormatterSupport.java`
- `src/main/java/org/javaspec/extension/JavaspecExtension.java`
- `src/main/java/org/javaspec/extension/Extension.java`
- `src/main/java/org/javaspec/extension/ExtensionContext.java`
- `src/main/java/org/javaspec/reporting/RunReportWriter.java`

Test files added:

- `src/test/java/org/javaspec/formatter/RunFormatterRegistryTest.java`
- `src/test/java/org/javaspec/extension/ExtensionContextTest.java`
- `src/test/java/org/javaspec/reporting/RunReportWriterTest.java`
- `src/test/java/org/javaspec/cli/MainPhase11ReportCliTest.java`

Verification:

- `mvn -q -Dtest=RunFormatterRegistryTest,ExtensionContextTest,RunReportWriterTest,MainPhase11ReportCliTest test` passed.
- `mvn verify` passed with 364 tests, 0 failures, and 0 errors.
- `mvn dependency:tree -Dscope=runtime` passed and showed only `org.javaspec:javaspec` in runtime scope.

Known limitations:

- Formatter extension contracts are public and programmatic, but the CLI currently exposes only built-in `progress` and `pretty` because no external extension loading mechanism is implemented.
- JSON reporting is limited to schemaVersion 1 runner results and has no config-level destination, alternate schemas, streaming mode, or additional report formats.
- Reports are not written when dry-run exits before execution because pending generation/update work exists.

Acceptance criteria status:

- Human-readable output remains clear and compatible for local use.
- CI receives stable exit codes and optional zero-dependency machine-readable JSON output.
- Extensions can register run formatters programmatically without modifying core internals; broader extension capabilities remain future work.

### Phase 12 — Compatibility and Quality Matrix (Completed via Distrobox Multi-JDK Matrix)

**Owner:** Java tester/quality agents.

**Status:** Completed and verified on 2026-06-03 through Distrobox `1.8.2.5` with Podman `5.8.2`. See [`docs/test-report.md`](docs/test-report.md) for the consolidated quality matrix.

Verification summary:

1. Distrobox/Podman container tooling was used to run the full Java 8, 11, 17, 21, and 25 matrix with Maven Temurin images.
2. Each container executed `java -version`, `javac -version`, `mvn -version`, `mvn clean`, and `mvn verify` from `/home/paperboy/workspace/javaspec`.
3. Java 8 (`1.8.0_492`), Java 11 (`11.0.31`), Java 17 (`17.0.19`), Java 21 (`21.0.11 LTS`), and Java 25 (`25.0.3 LTS`) all passed with 364 tests, 0 failures, 0 errors, and 0 skipped.
4. Maven `3.9.16` was used in every matrix container.
5. JDK 17+ emitted only expected `-source 8` / `-target 1.8` bootstrap/obsolete-option warnings.
6. Java 25 runtime reflection probing passed for `java.util.stream.Gatherer`, `java.util.stream.Gatherer$Downstream`, `java.util.stream.Gatherer$Integrator`, `java.util.stream.Gatherer$Integrator$Greedy`, and `java.util.stream.Gatherers`.
7. Runtime dependency auditing in `javaspec-jdk25-matrix` with `mvn dependency:tree -Dscope=runtime` passed and showed only `org.javaspec:javaspec:jar:0.1.0-SNAPSHOT` in runtime scope.
8. Matrix containers were created and stopped, not removed: `javaspec-jdk8-matrix`, `javaspec-jdk11-matrix`, `javaspec-jdk17-matrix`, `javaspec-jdk21-matrix`, and `javaspec-jdk25-matrix`.
9. Blockers: none.

Distrobox JDK matrix:

| JDK | Image | Container | Runtime | Maven | Phase 12 result |
|---|---|---|---|---|---|
| Java 8 | `docker.io/library/maven:3.9-eclipse-temurin-8` | `javaspec-jdk8-matrix` | `1.8.0_492` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 11 | `docker.io/library/maven:3.9-eclipse-temurin-11` | `javaspec-jdk11-matrix` | `11.0.31` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 17 | `docker.io/library/maven:3.9-eclipse-temurin-17` | `javaspec-jdk17-matrix` | `17.0.19` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 21 | `docker.io/library/maven:3.9-eclipse-temurin-21` | `javaspec-jdk21-matrix` | `21.0.11 LTS` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |
| Java 25 | `docker.io/library/maven:3.9-eclipse-temurin-25` | `javaspec-jdk25-matrix` | `25.0.3 LTS` | `3.9.16` | PASS — 364 tests, 0 failures, 0 errors, 0 skipped |

Acceptance criteria status:

- Java 8 remains a hard compatibility gate and now has a passing native Java 8 Distrobox run in addition to the compiler, bytecode, and constant-pool compatibility evidence.
- Runtime dependency audit passed with only the root project artifact in runtime scope.
- LTS profiles were exercised across Java 8, 11, 17, 21, and 25 containers, and every matrix entry passed.
- Java 25 stream Gatherer metadata was validated by runtime reflection on Java 25.
- First-MVP, ADR 0004 correction, Phase 4 configuration/filter, Phase 8 doubles, Phase 9 CLI, Phase 10 generation, and Phase 11 formatter/reporting/extension behavior remain covered by the automated test suite summarized above.

### Phase 13 — Documentation Completion

**Owner:** documenter, with C4 delegated to c4model if requested.

**Status:** Completed documentation pass for ARC42 sections 5-12, ADR coverage, user manual navigation, PHPSpec-to-Java migration notes, and Phase 12 report references. No C4 diagrams were required.

Tasks:

1. Extend ARC42 sections 5-12 after implementation architecture stabilizes.
2. Add ADRs for major design choices made during implementation.
3. Maintain and extend integrated test and quality reports produced by tester agents; Phase 12 is captured in [`docs/test-report.md`](docs/test-report.md).
4. Keep `docs/usermanual/Home.md` and `docs/usermanual/_Sidebar.md` synchronized after implementation begins.
5. Add user guide and migration notes from PHPSpec concepts to Java concepts.
6. Document configuration files, construction, constructor policy, typed matcher syntax, method generation including Phase 10 interface declarations and annotation elements, interface doubles, Phase 9 run controls, Phase 11 formatter/reporting/extension behavior, limitations, commands, examples, the first-MVP generator flow, and later advanced generator behavior according to what is implemented.

Acceptance criteria:

- Documentation reflects implemented behavior.
- `docs/usermanual/Home.md` and `docs/usermanual/_Sidebar.md` expose the same navigation topics.
- ADRs link to relevant ARC42 sections.
- Test and quality claims are backed by produced reports.
