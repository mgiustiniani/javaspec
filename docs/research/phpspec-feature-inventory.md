# phpspec Feature Inventory for javaspec

## Purpose

This document records phpspec features that should inform the Java port. It is an inventory, not an implementation commitment. Each feature must be adapted to Java's static type system, Java 8 compatibility, Maven conventions, and the zero-runtime-dependency policy.

## Verification sources and version scope

Source verification date: 2026-05-27.

- Official phpspec documentation checked at <https://phpspec.net/en/stable/> and <https://phpspec.net/en/latest/>. Both currently describe phpspec 5.x documentation.
- Current package metadata checked through Packagist at <https://repo.packagist.org/p2/phpspec/phpspec.json>. The latest observed package version is `phpspec/phpspec` 8.3.0, dated 2026-04-13.
- Source verification used phpspec 8.3.0 from GitHub tag/source commit `3191d330af2474fdb7d2fe7bff6153426384d423`.
- Prophecy source/docs checked at <https://github.com/phpspec/prophecy>, especially the README sections on doubles, promises, predictions, and argument tokens.
- Current 8.3.0 Composer runtime dependencies include `phpspec/prophecy`, `phpspec/php-diff`, `sebastian/exporter`, `symfony/console`, `symfony/event-dispatcher`, `symfony/process`, `symfony/finder`, `symfony/yaml`, `doctrine/instantiator`, and `ext-tokenizer`. This strengthens the javaspec zero-runtime-dependency limitation: these libraries must not be copied into the Java runtime core.
- Current 8.3.0 `composer.json` constrains PHP to PHP 8.2.* through 8.5.*. Older description text may still mention PHP 7.1+ and should not be treated as the current runtime constraint.

Version interpretation:

- Documentation pages are useful for concepts and user-facing examples, but source-verified 8.3.0 details take precedence where command options, formatters, matchers, generators, or configuration differ.
- javaspec should use this inventory as a design input, not as a promise of byte-for-byte phpspec compatibility.

## Scope Notes

- phpspec is a PHP specification framework built around describing object behavior.
- Many phpspec features depend on Composer, PSR autoloading, Symfony components, and Prophecy. javaspec must not copy those runtime dependencies.
- Exact option names and formatter availability can vary by phpspec version and extension. Version-sensitive details are labeled where relevant.

## 1. Executable CLI

phpspec provides an executable command with a command model centered on running and describing specifications.

### Source-verified command model

- Command classes: `run` and `describe`.
- `desc` is a common shorthand/abbreviation documented and used by phpspec documentation, but the inspected source command class is `describe`.
- Invoking phpspec without an explicit command commonly runs specifications, depending on console configuration.

### Global options

- phpspec custom global option: `--config` / `-c`.
- Inherited Symfony console options include `--help`, `--quiet` / `-q`, verbosity flags such as `-v`, `-vv`, and `-vvv`, `--version`, `--ansi`, `--no-ansi`, and `--no-interaction`.
- `--no-interaction` disables interactive code generation.

### `run` command

- Argument: optional spec locator.
- Locator forms include file, directory, and `file:line` forms.
- Source-verified options:
  - `--format` / `-f`
  - `--stop-on-failure`
  - `--no-code-generation`
  - `--no-rerun`
  - `--fake`
  - `--bootstrap` / `-b`

### `describe` command

- Argument: optional class name.
- If the class name is omitted, current source prompts for the class to describe.

Java port implications:

- Provide `javaspec run`, `javaspec describe`, and possibly `javaspec desc` as user-facing commands.
- Implement option parsing without external CLI libraries.
- Keep exit codes stable and documented.
- Support non-interactive CI mode and make it clear when code generation is disabled.

## 2. Configuration

phpspec uses project and user configuration files to define suites, paths, formatters, extensions, and generation behavior.

### Source-verified configuration files

Project-level candidates:

- `phpspec.yml`
- `.phpspec.yml`
- `phpspec.yml.dist`
- `phpspec.yaml`
- `.phpspec.yaml`
- `phpspec.yaml.dist`

User-level file:

- `~/.phpspec.yml`

### Configuration keys and concepts

Source-verified configuration includes or supports:

- `composer_suite_detection`
- `suites`
  - `namespace`
  - `spec_prefix`
  - `src_path`
  - `spec_path`
  - `psr4_prefix`
- `formatter.name`
- `code_generation`
- `stop_on_failure`
- `verbose`
- `bootstrap`
- `extensions`
- `matchers`

The inspected source parameters also support rerun and fake behavior. These should be treated as runner behavior flags rather than general data-model concepts.

Java port implications:

- Maven defaults should be inferred when no configuration exists.
- Configuration should model suites, Java package roots, spec roots, source roots, target LTS profile, bootstrap hooks, formatters, and extensions.
- YAML compatibility is desirable conceptually, but the core must not add a YAML parser dependency. A restricted internal parser or a different zero-dependency format may be needed.

## 3. Spec Discovery and Naming Conventions

phpspec discovers specs by convention.

Core conventions:

- Specs usually live under a `spec/` directory.
- Spec files use a `*Spec.php` suffix.
- Spec classes map to described production classes by removing the `Spec` suffix and applying namespace/path rules.
- Examples are public methods whose names commonly begin with `it_`.
- `its_` examples express behavior of a property or returned value associated with the subject.
- The described class is inferred from the spec class name and namespace mapping.
- Resources represent discoverable spec/source pairs within suites.

Java port implications:

- Candidate spec suffix: `Spec` for Java classes.
- Candidate spec roots should align with Maven test-like directories while preserving framework independence.
- Example methods can use annotations, naming conventions, or both; Java's method naming rules should be respected.
- Mapping must account for packages, nested classes, and compiled classpath discovery.

## 4. Subject Lifecycle

phpspec specs are centered on a subject: the object whose behavior is described.

Subject lifecycle features:

- Spec classes commonly extend `ObjectBehavior`.
- `$this` in a spec represents a wrapped subject, not the spec object in the usual PHPUnit style.
- `let` is run before each example and can configure the subject or collaborators.
- `letGo` is run after each example for cleanup.
- Constructor arguments can be supplied, commonly through subject-construction helpers such as `beConstructedWith`.
- Alternate construction can be represented with helper methods such as construction through named constructors where supported.
- Subject creation may be delayed until needed.
- Collaborators can be injected into `let` or example methods through type-hinted parameters.
- Subject methods can be invoked through the wrapper and chained into expectations.
- Access to the wrapped object is possible when needed, but should be used sparingly.

Java port implications:

- Java needs an explicit subject model because `$this` cannot be redefined.
- Lifecycle hooks can be represented by conventions or annotations.
- Constructor arguments and collaborators must be modeled through reflection and type information.
- Lazy subject creation is possible but must produce clear errors for ambiguous constructors.

## 5. Example Lifecycle Hooks

phpspec examples execute in an isolated lifecycle.

Features:

- Per-example setup through `let`.
- Per-example teardown through `letGo`.
- Example method invocation.
- Collaborator/double creation before example execution.
- Prediction verification after example execution.
- Event dispatch around suite, spec, example, and result events.
- Extension listeners/subscribers can observe or influence lifecycle phases.
- Pending examples may be represented by empty examples, explicit pending markers, or pending exceptions depending on version and style.
- Broken examples represent unexpected errors distinct from expectation failures.

Java port implications:

- Execution must isolate state per example.
- Hook ordering must be deterministic.
- Doubles should verify predictions after the example body but before final teardown reporting.
- Statuses should include passed, failed, pending, skipped, and broken/error.

## 6. Matchers and Expectations

phpspec expectations are expressed through matcher methods, usually named with `should*` or `shouldNot*`.

### Source-verified built-in matcher inventory

Identity and equality:

- `shouldReturn`
- `shouldBe`
- `shouldEqual`
- `shouldBeEqualTo`

Comparison:

- `shouldBeLike`

Approximate numeric matching:

- `shouldBeApproximately`
- `shouldBeEqualToApproximately`
- `shouldEqualApproximately`
- `shouldReturnApproximately`

Exception matching:

- `shouldThrow`
- `during*` dynamic operation names
- `during(method, args)`
- `duringInstantiation`
- Named-constructor exception flows where applicable

Trigger matching:

- `shouldTrigger` for PHP errors, warnings, notices, or deprecations.

Type matching:

- `shouldHaveType`
- `shouldReturnAnInstanceOf`
- `shouldBeAnInstanceOf`
- `shouldImplement`

Object-state matching:

- Dynamic `shouldBe*` and `shouldHave*` forms map to subject methods such as `is*` and `has*`.

Scalar matching:

- Dynamic forms such as `shouldBeString`, `shouldBeArray`, `shouldBeBool`, `shouldBeBoolean`, `shouldBeInteger`, `shouldBeFloat`, `shouldBeCallable`, `shouldBeObject`, and related PHP `is_*` checks.

Count matching:

- `shouldHaveCount` for arrays and traversables.

Containment, key, and value matching:

- `shouldContain`
- `shouldHaveKey`
- `shouldHaveKeyWithValue`
- These apply to arrays, traversables, and `ArrayAccess` where supported by the matcher.

Iteration matching:

- `shouldIterateAs`
- `shouldYield`
- `shouldIterateLike`
- `shouldYieldLike`
- `shouldStartIteratingAs`
- `shouldStartYielding`

String matching:

- `shouldContain`
- `shouldStartWith`
- `shouldEndWith`
- `shouldMatch`

Negation:

- `shouldNot*` variants are supported for matcher forms where negation is meaningful.

Custom and inline matchers:

- Inline matchers can be supplied through `getMatchers()`.
- Configuration-based custom matcher classes implement `PhpSpec\Matcher\Matcher`.

### Expectation behavior

- Matchers produce failure messages explaining expected and actual values.
- Some matchers compare PHP identity, equality, or loose structural equivalence; Java must define equivalent semantics explicitly.
- Exception expectations wrap the operation under test rather than requiring manual try/catch in examples.

Java port implications:

- Provide Java-friendly expectation syntax while preserving recognizable names where possible.
- Define equality using Java `equals`, identity using `==`, and type checks using `Class`/`isAssignableFrom`.
- Avoid external assertion libraries in the runtime.
- Custom matcher APIs should be simple Java interfaces.

## 7. Prophecy Object Doubles

phpspec integrates with `phpspec/prophecy` for object doubles.

### Core Prophecy concepts

- `ObjectProphecy` represents a configured double before it is revealed.
- `MethodProphecy` represents configured behavior or predictions for a method call.
- `reveal()` produces the object that can be passed to the subject.

### Dummies, stubs, mocks, and spies

- Dummies are placeholders; methods generally return `null` or do nothing when behavior is irrelevant.
- Stubs use promises for configured behavior:
  - `willReturn`
  - `willReturnArgument`
  - `willThrow`
  - `will(callback)`
  - Multiple `willReturn` values for sequential responses.
- Mocks use predictions for expected interactions:
  - `shouldBeCalled`
  - `shouldNotBeCalled`
  - `shouldBeCalledTimes`
  - `should(callback)`
- Spies/post-verification support:
  - `shouldHaveBeenCalled`
  - `shouldHave(PredictionInterface)`

### Argument tokens

Prophecy argument matching includes:

- Exact value tokens.
- `is` / identical matching.
- `type` matching.
- `which` method/property matching.
- `that` / callback matching.
- `any`.
- `cetera`.
- `containingString`.
- `in`.
- `notIn`.
- Custom tokens implementing `TokenInterface`.

### Prediction verification

- Predictions are verified after the example body.
- Prophecy exposes prediction checking through the prophecy/prophet lifecycle, including `checkPredictions` behavior.

Java port implications:

- The zero-dependency MVP can support interface doubles with JDK dynamic proxies.
- Concrete class doubles, final classes, static methods, constructors, and private methods are limited without bytecode generation libraries.
- Argument matchers and call recording can be implemented in core for interface proxies.
- Advanced doubles may be extension-only if they require external libraries.

## 8. Code Generation

phpspec supports a strong generation workflow.

### Source-verified generators

The inspected source contains generation support for:

- Specification generation.
- Class generation.
- Interface generation.
- Method generation.
- Method signature generation.
- Return constant generation.
- Named constructor generation.
- Private constructor generation.

### Source-verified templates and overrides

- Template override lookup checks project/home `.phpspec` template locations.
- Source templates include:
  - `specification`
  - `class`
  - `interface`
  - `method`
  - `interface_method_signature`
  - `named_constructor_create_object`
  - `named_constructor_exception`
  - `private-constructor`
  - `returnconstant`

### Generation behavior

- `describe` creates or prepares a spec for a class.
- Missing class prompts can generate production classes.
- Missing method prompts can generate methods on described classes.
- Interactive prompts ask for confirmation unless configured otherwise.
- Non-interactive modes can disable generation or fail with snippets.
- Generated snippets are shown in formatter output.

Java port implications:

- Generation should support spec skeletons and missing class/method snippets.
- Writing files should require confirmation unless a non-interactive policy says otherwise.
- Generated source must be Java 8-compatible unless explicitly targeting metadata-only concepts.
- Java compilation and package directories make generation more constrained than PHP file generation.

## 9. Runner, Suite, Locator, and Resource Model

phpspec separates execution concerns into runner, suite, locating, and resource concepts.

Features:

- Suites group specs by configuration.
- Locators discover spec files/resources.
- Resources connect spec classes/files to described source classes/files.
- Runners execute suites, specs, and examples.
- Loaders/bootstrap mechanisms prepare autoloading and runtime setup.
- Event objects represent lifecycle phases and results.
- Filters restrict suites, specs, or examples.

### Source-verified statuses and exit codes

Result statuses include:

- `PASSED`
- `PENDING`
- `SKIPPED`
- `FAILED`
- `BROKEN`

Exit-code mapping observed from the current runner/result behavior:

- Passed suite result: exit code `0`.
- Skipped suite result: exit code `0`.
- Any other suite result: exit code `1`.

Java port implications:

- Model suite, spec, example, described type, and resource as explicit domain objects.
- Discovery should work from classpath and/or filesystem roots.
- Resource mapping must handle Java package names, source roots, and compiled classes.
- Event/listener APIs should not require an external service container.
- Exit codes must remain stable for CI.

## 10. Formatters and Reporting

phpspec reports execution through formatters.

### Documentation-listed formatters

The documentation lists:

- `progress` as the default formatter.
- `html` / `h`.
- `pretty`.
- `junit`.
- `dot`.
- `tap`.

### Source-verified default formatter registrations in 8.3.0

The inspected 8.3.0 source registers:

- `progress`
- `pretty`
- `junit`
- `json`
- `dot`
- `tap`
- `html` / `h`

A TeamCity formatter class/changelog entry exists, but it was not registered by default in the inspected `ContainerAssembler`. Treat TeamCity output as version-sensitive or extension-sensitive rather than a guaranteed built-in default.

Common reporting capabilities:

- Progress/dot formatter.
- Pretty/spec formatter with readable example descriptions.
- Failure summaries with matcher explanations.
- Snippets for missing classes or methods.
- Pending/skipped/broken markers.
- Verbose output with traces and diagnostics.
- Machine-readable formats such as JUnit or JSON where available.
- Formatter configuration through CLI and config files.

Java port implications:

- MVP should include progress and pretty formatters.
- Machine-readable reports should be implemented without runtime dependencies or left to extensions.
- Formatter APIs should receive structured events/results, not parse console text.

## 11. Extension System

phpspec exposes extension points through its service container and event system.

Source-verified extension capabilities:

- Extension classes implement an `Extension` interface with a `load(ServiceContainer, config)`-style entry point.
- Extensions can register or modify services in the service container.
- Tagged services/listeners can participate in lifecycle behavior.
- Event subscribers can observe runner, suite, spec, example, and result events.
- Custom matchers can be registered through configuration.
- Generators, templates, formatters, and other services can be customized through service-container hooks.

Java port implications:

- javaspec cannot depend on a service container library.
- Extension points should be plain Java interfaces.
- `ServiceLoader` can be considered because it is part of the JDK, but explicit configuration may also be needed.
- Extension loading must be deterministic and safe on Java 8.

## 12. Integrations and Tooling

phpspec integrates naturally with the PHP ecosystem.

Feature areas:

- Composer installation and scripts.
- PSR-0/PSR-4 autoloading.
- CI execution.
- Coverage through external tools such as PHP coverage tooling rather than phpspec core alone.
- IDE and editor integration through standard CLI behavior.
- Extensions for framework-specific behavior.

Java port implications:

- Maven replaces Composer as the initial build ecosystem.
- Java packages and classpaths replace PSR autoloading.
- Coverage should remain external or test-scope only.
- CI integration depends on stable CLI, exit codes, and reports.

## 13. Error Handling and Example States

phpspec distinguishes multiple result types.

States and behaviors:

- Passed examples.
- Failed examples from matcher expectation failures.
- Broken examples from unexpected exceptions/errors.
- Pending examples from explicit pending markers, unimplemented behavior, or generated placeholders.
- Skipped examples where supported by extensions or framework conventions.
- Deprecation notices or version-specific warnings depending on PHP/phpspec versions.
- Configuration and discovery errors.
- Generation prompts for missing artifacts.

Java port implications:

- Define a stable result taxonomy.
- Distinguish assertion/matcher failure from unexpected exception.
- Represent pending and skipped states explicitly.
- Keep diagnostics concise by default and detailed in verbose mode.

## 14. Deprecations and Version Differences

phpspec behavior has changed across versions, including command options, Symfony component versions, Prophecy integration details, matcher availability, formatter/extension packaging, PHP runtime requirements, and generator behavior.

Java port implications:

- Treat this inventory as a feature map, not a compatibility promise with one exact phpspec release.
- Verify exact phpspec behavior before intentionally cloning a detail.
- Prefer Java-idiomatic behavior when PHP-specific behavior would be confusing or impossible without dependencies.

## 15. Limitations for a Java Zero-Dependency Port

The Java port must account for limitations that do not exist, or are less severe, in PHP/phpspec:

- Java cannot redefine `$this`; the subject wrapper must be explicit.
- Java has static typing, constructors, method overloads, checked exceptions, packages, and classloaders.
- Interface doubles can use JDK proxies; concrete class doubles generally require bytecode generation and are therefore not suitable for the zero-dependency core.
- Final classes, final methods, static methods, constructors, and private methods are hard to double without external tools.
- YAML parsing cannot rely on Symfony/YAML or similar libraries in the runtime.
- Composer and PSR autoloading concepts must be mapped to Maven, Java package names, source roots, and classpaths.
- Rich HTML/XML/JSON reporting must be implemented manually, kept minimal, or delegated to extensions.
- Runtime extension discovery must avoid dependency injection containers.
- Current phpspec 8.3.0 depends on multiple runtime packages, including Symfony components and Prophecy. javaspec core must not inherit that dependency model.

## Traceability Table

| phpspec feature | Java port candidate | Initial priority | Constraints |
|---|---|---|---|
| `phpspec run` | `javaspec run` | MVP | Implement CLI parsing without external libraries. |
| `describe` / `desc` | `javaspec describe` / `javaspec desc` | MVP | Generation can start with spec skeletons and snippets. |
| Config selection `--config` / `-c` | `--config` or equivalent | MVP | Parser must be zero-dependency. |
| `run` spec locator file/directory/file:line | Java spec locator | MVP | Must map source files to class names deterministically. |
| `--format` / `-f` | Formatter selection | MVP | Built-in formatters only at first. |
| `--bootstrap` / `-b` | Bootstrap hooks/classpath setup | Later | Must adapt Composer autoloading to Java classpath/Maven. |
| `--stop-on-failure` | Runner stop policy | MVP | Stable exit codes required. |
| `--no-code-generation` | Disable generation prompts/snippets that write files | MVP | Required for non-interactive CI. |
| `--no-rerun` | Disable failed-example rerun behavior | Later | Requires persisted or session runner state if implemented. |
| `--fake` | Dry-run/fake execution behavior | Later | Useful diagnostically, but not essential for zero-dependency MVP. |
| Inherited verbosity flags | Output detail levels | MVP | No logging dependency. |
| `--no-interaction` disabling generation | Non-interactive CI mode | MVP | Must not block on prompts. |
| `phpspec.yml` and YAML variants | javaspec config file | MVP | Restricted format or internal parser; no YAML dependency. |
| `~/.phpspec.yml` | Optional user defaults | Later | Must not make builds non-reproducible by default. |
| `composer_suite_detection` | Maven/default suite detection | Later | Composer concept must be translated, not copied. |
| Suite namespace/path mapping | Package/source/spec root mapping | MVP | Maven defaults should work without config. |
| `formatter.name` | Formatter config property | MVP | Built-ins first. |
| `extensions` in config | Extension registration | Later | Plain interfaces or JDK `ServiceLoader`; no DI container. |
| `matchers` in config | Custom matcher registration | Later | Plain Java matcher interfaces. |
| `spec/` directory | Java spec root convention | MVP | Align with Maven layout decisions. |
| `*Spec.php` files | `*Spec` Java classes | MVP | Java package/classpath mapping required. |
| `it_*` examples | Example method convention or annotation | MVP | Must fit Java method naming and reflection. |
| `its_*` examples | Subject property/result examples | Later | Java accessor conventions differ from PHP properties. |
| Described class mapping | Spec-to-subject mapping | MVP | Handle packages, nested types, and missing classes. |
| `let` | Before-example lifecycle hook | MVP | Convention or annotation to be decided. |
| `letGo` | After-example lifecycle hook | MVP | Deterministic cleanup order. |
| Constructor arguments | Subject construction API | MVP | Reflection errors must be clear. |
| Collaborator injection | Hook/example parameter injection | Later | Needs type resolution and double integration. |
| Lazy subject creation | Lazy reflected subject wrapper | Later | Ambiguous constructors must be diagnosed. |
| `shouldBe` | Identity matcher | MVP | Java `==` semantics. |
| `shouldReturn` | Method result matcher | MVP | Needs invocation wrapper or expectation DSL. |
| `shouldEqual` / `shouldBeEqualTo` | Equality matcher | MVP | Java `equals` semantics. |
| `shouldBeLike` | Structural/lenient equality candidate | Later | Java equivalent must be defined carefully. |
| Approximate matchers | Numeric tolerance matcher | Later | Define tolerance API for Java primitive/wrapper numeric types. |
| `shouldHaveType` | Exact or assignable type matcher | MVP | Use `Class` metadata. |
| `shouldReturnAnInstanceOf` / `shouldBeAnInstanceOf` | Instance type matcher | MVP | Use `Class#isInstance`. |
| `shouldImplement` | Interface assignability matcher | MVP | Use `Class#isAssignableFrom`. |
| `shouldThrow` and `during*` | Exception expectation | MVP | Java checked exceptions and Java 8 functional interfaces. |
| `shouldTrigger` | PHP error/deprecation trigger matcher | Extension/Not core | PHP-specific; Java analogue might be warnings/log events. |
| Object-state dynamic matchers | `is*`/`has*` JavaBean-style state matchers | Later | Reflection conventions must be explicit. |
| Scalar dynamic matchers | Java primitive/wrapper/string type matchers | MVP subset | PHP `is_*` semantics do not map exactly to Java. |
| Array/traversable matchers | Array, `Iterable`, `Iterator`, `Collection`, and `Map` expectation support | MVP subset | Java arrays, iterables, and maps require separate code paths; PHP traversable semantics differ. |
| Count matcher | Array/collection/iterable count matcher | MVP | Must support arrays without dependencies. |
| Containment/key/value matchers | Collection, map, array, iterable, string content matchers | MVP | Define behavior for `Iterable`, `Map`, arrays, and strings. |
| Iteration/yield matchers | Iterable/iterator sequence matchers | Later | Generators/yield do not map directly to Java 8. |
| String matchers | Contains, starts-with, ends-with, regex matchers | MVP | Use JDK string and regex APIs. |
| `shouldNot*` | Negated expectations | MVP | Negation messages must be clear. |
| Inline `getMatchers()` matchers | Per-spec matcher registration | Later | Java equivalent needs lifecycle and type-safety design. |
| Custom matcher classes | Matcher extension API | Later | Plain Java interfaces; no service container. |
| Prophecy `ObjectProphecy` / `reveal()` | Interface double definition and proxy reveal | MVP | JDK proxies only for interfaces. |
| Prophecy dummies | Interface dummy proxies | MVP | Default return values must be explicit. |
| Prophecy promises `willReturn` / `willThrow` / callbacks | Stubbed return/throw/callback behavior | MVP | Java functional interfaces can represent callbacks. |
| Sequential `willReturn` values | Sequential stub responses | Later | Requires deterministic call-order handling. |
| Prophecy predictions `shouldBeCalled` / `shouldNotBeCalled` / `shouldBeCalledTimes` | Interface interaction predictions | Later | Call recording and verification needed. |
| Prophecy `should(callback)` predictions | Custom prediction callbacks | Later | Define callback API and diagnostics. |
| Spies `shouldHaveBeenCalled` | Recorded calls with post-verification | Later | Define memory, ordering, and thread-safety semantics. |
| Prophecy argument tokens | Argument matcher API | MVP subset | Implement exact, any, type, callback first; advanced tokens later. |
| Custom `TokenInterface` | Custom argument token API | Later | Plain Java interface; no dependency on Prophecy. |
| Prediction verification after examples | Double verification lifecycle | MVP for supported doubles | Verify after example body and before final result. |
| Specification generator | Java spec skeleton generator | Later | Must respect packages/source roots. |
| Class generator | Missing class snippet/generator | Later | Must respect Java source layout and package names. |
| Interface generator | Missing interface snippet/generator | Later | Useful but less critical than spec skeletons. |
| Method generator | Missing method snippet/generator | Later | Overloads and return types complicate generation. |
| Method signature generator | Suggested method signatures | Later | Needs type inference and source parsing strategy. |
| Return constant generator | Suggested constant returns | Later | Java type safety and defaults must be explicit. |
| Named constructor generation | Static factory/named-constructor snippets | Later | PHP named-constructor patterns map imperfectly to Java. |
| Private constructor generation | Private constructor snippet support | Later | Must avoid unsafe changes and respect class intent. |
| Template overrides in `.phpspec` | Template override directory | Later | No template engine dependency; security-sensitive. |
| Runner/suite/resource model | Explicit Java domain model | MVP | Must work without external container. |
| Locators | Filesystem/classpath discovery | MVP | Deterministic ordering required. |
| Result statuses | Passed, failed, pending, skipped, broken | MVP | Distinguish matcher failures from unexpected exceptions. |
| Exit code 0 for passed/skipped, 1 otherwise | CI contract | MVP | Documented and tested later by tester agents. |
| Progress formatter | Dot/progress output | MVP | Console-only, no dependencies. |
| Pretty formatter | Readable spec output | MVP | Stable descriptions from method names/annotations. |
| JUnit report | Optional machine-readable report | Later | Must avoid dependencies or live outside core. |
| HTML report | Optional human-readable report file | Later/Extension | HTML generation can remain minimal or extension-based. |
| JSON formatter | Optional machine-readable report | Later | Implement manually if included; no JSON dependency. |
| TAP formatter | Optional compatibility report | Later/Extension | Useful for tooling, not MVP. |
| TeamCity formatter | CI-specific extension | Extension | Not registered by default in inspected 8.3.0 source. |
| Snippets | Missing artifact suggestions | Later | Should not write files without policy. |
| Event subscribers/listeners | Event/listener interfaces | Later | No Symfony event dispatcher dependency. |
| Service container hooks | Minimal extension registry | Extension | No DI container in core. |
| `Extension::load(ServiceContainer, config)` | Java extension loading contract | Later | Adapt to plain interfaces or `ServiceLoader`. |
| Composer integration | Maven/classpath integration | MVP | Java ecosystem replacement. |
| PSR-0/PSR-4 autoloading | Java package and classpath mapping | MVP | Different naming model. |
| Coverage tooling | External coverage integration | Extension | Not core runtime. |
| Pending examples | Pending status/exception/annotation | MVP | Explicit state in result model. |
| Skipped examples | Skip status/annotation/convention | Later | Define conditions without dependencies. |
| Broken examples | Unexpected exception status | MVP | Separate from matcher failure. |
| Deprecation handling | Version warnings | Later | Avoid PHP-specific semantics unless useful. |
| phpspec runtime dependencies | Zero-runtime-dependency Java core | MVP constraint | Do not copy Symfony, Prophecy, YAML, DI, or exporter dependencies. |
