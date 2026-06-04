# 12. Glossary

| Term | Meaning |
|---|---|
| ADR | Architecture Decision Record. javaspec ADRs live in `docs/adr/` and follow the Context, Decision, Consequences structure. |
| Aggregate verification script | `scripts/verify-all.sh`; Phase 19 local release verification script, extended in Phase 20 to run version alignment first and in Phase 21 to run standalone examples by default, that keeps root Maven verification core-only, installs the current core snapshot, verifies/audits standalone Maven plugin, Gradle plugin, and JUnit Platform engine artifacts explicitly, and then verifies adoption examples unless explicitly skipped. |
| Changelog | `CHANGELOG.md`; Phase 20 release-readiness documentation for notable changes. |
| Version alignment check | `scripts/check-version-alignment.sh`; Phase 20 script that checks root Maven, standalone Maven plugin, standalone JUnit Platform engine, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` alignment. |
| Annotation element | A no-argument method-like member of a Java annotation type. javaspec generates only compatible elements for annotation sources. |
| Bootstrap hook | A config value parsed as metadata for future runner lifecycle work. Bootstrap hooks are not executed in the current implementation. |
| Class filter | Repeatable `run --class <name>` filter matching described qualified/simple names or spec qualified/simple names. |
| Class-like type | A described production type kind: class, final class, interface, enum, annotation, record, sealed class, or sealed interface. |
| Constructor policy | `run` policy for unmatched constructors: `comment` (default), `preserve`, or explicit destructive `delete`. |
| Core-only Maven verification | Repository-root `mvn verify`; intentionally verifies only the zero-runtime-dependency core artifact, not standalone optional adapters. |
| Described type | The production Java type inferred from or targeted by a specification. |
| `describe` / `desc` | CLI command that creates specification/support skeletons only and never writes production source. |
| Direct matcher | An `ObjectBehavior` convenience assertion such as `shouldReturn(actual, expected)` that delegates through `match(actual)`. |
| Dry-run | `run --dry-run`; plans generation/update work without writes or prompts and exits `1` when pending work exists. |
| Effective classloader | The classloader visible to the CLI process. The runner can execute only spec classes available there unless an explicit classloader is selected. |
| Explicit classpath | `run --classpath` or `--classpath-file` entries used to create the selected classloader for type existence checks and spec execution. Entries must point to already compiled classes or archives. |
| Example | A public `void` Java spec method named `it_*` or `its_*`. |
| Example status | Runtime outcome: `PASSED`, `FAILED`, `BROKEN`, or `SKIPPED`. |
| GitHub Actions workflow | `.github/workflows/ci.yml`; Phase 19 CI configuration with a Java 8/11/17/21/25 core matrix and Java 21 full-verification job. Phase 19 remote success is user-/maintainer-confirmed for HEAD `4d30e63` on `develop`; Phase 20 and Phase 21 have no remote CI success claim in the current evidence. |
| Stable id | Identifier exposed by discovery/result objects and reports. Spec ids derive from the spec qualified name; example ids use `<specQualifiedName>#<methodName>` and match `ExampleResult.fullName()`. |
| Extension API | Programmatic contracts `JavaspecExtension`/`Extension` and `ExtensionContext`. External CLI extension discovery/loading is not implemented. |
| Formatter | A `RunFormatter` implementation. The CLI supports built-in `progress` and `pretty` names. |
| Gradle plugin adapter | Standalone optional artifact `javaspec-gradle-plugin/` with plugin id `org.javaspec`, extension `javaspec`, and task `javaspecRun`. It is not a root Maven module and does not require JUnit in projects under test. |
| Gradle test source set runtime classpath | The compiled `test` source set runtime classpath used by the optional Gradle plugin by default when the Gradle Java plugin/source sets are present. |
| Interface double | A JDK dynamic proxy double for an ordinary Java interface under `org.javaspec.doubles`. |
| Invocation API | `org.javaspec.invocation` no-`System.exit` programmatic API over canonical discovery, `SpecRunner`, and `RunResult`. |
| `JavaspecRunMojo` | Phase 15 Maven plugin goal implementation for `javaspec:run`; it uses Maven test dependency resolution/classpath and delegates to `JavaspecLauncher` without `System.exit`. |
| `JavaspecRunTask` | Phase 16 Gradle plugin task implementation for `javaspecRun`; it uses the Gradle classpath, manages a `URLClassLoader` and thread context classloader, writes reports through core writers, and delegates to `JavaspecLauncher` without `System.exit`. |
| `JavaspecTestEngine` | Phase 17 optional JUnit Platform `TestEngine` implementation with engine id `javaspec`; it is registered through ServiceLoader, filters canonical discovery by JUnit Platform selectors/configuration parameters, and delegates execution to `JavaspecLauncher` without `System.exit`. |
| JUnit Platform engine adapter | Standalone optional artifact `javaspec-junit-platform-engine/` packaging `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`; it is not a root Maven module and does not add JUnit Platform dependencies to the core runtime artifact. |
| JUnit Platform selector | Class, package, method, or unique-id selector supplied by JUnit Platform and applied by the optional engine as a filter over canonical javaspec discovery results. |
| JUnit XML-compatible report | Dependency-free UTF-8 XML report written by `run --junit-xml` / `--junit-xml-file`, Maven/Gradle plugin report settings, or core report writers from `RunResult`; it does not require JUnit. Phase 21 adds a golden passing XML report under `docs/examples/reports/`. |
| LTS profile | Target Java profile key: `java8`, `java11`, `java17`, `java21`, or `java25`. |
| Matchable | Fluent expectation wrapper returned by typed proxy methods and `match(actual)`. |
| Maven plugin adapter | Standalone optional artifact `javaspec-maven-plugin/` packaging `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as a Maven plugin with goal prefix `javaspec`. It is not a root module and does not require JUnit in projects under test. |
| Maven test classpath | The compiled test-scope classpath supplied by Maven to the optional plugin and used as input to the canonical javaspec runner. |
| Maven `release-artifacts` profile | Phase 20 Maven profile on root, Maven plugin, and JUnit Platform engine builds that creates local sources and javadocs only; it does not sign, stage, deploy, or publish. |
| Missing sealed-interface skeleton | A generated sealed-interface source file that can include root method declarations and nested permitted implementation bodies. |
| PHPSpec-inspired | Modeled after PHPSpec workflow concepts while adapted to Java packages, classes, static typing, compilation, and interfaces. |
| Profile catalog | Metadata model for Java LTS profiles, feature flags, and API symbols under `org.javaspec.profile`. |
| Reflection runner | Dependency-free runner that executes compiled spec examples by Java reflection after discovery/generation/update work. |
| Release checklist | `RELEASING.md`; Phase 20 local release-readiness checklist that documents verification steps and explicit blockers before public publication. |
| Publication blockers | Required decisions, credentials, or approvals that remain intentionally unresolved before public release: GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval. The MIT `LICENSE` and maintainer metadata are already confirmed. |
| Report | Optional machine-readable output written by `run`, currently JSON via `--report` / `--report-file` and JUnit XML-compatible XML via `--junit-xml` / `--junit-xml-file`. |
| Report schema | `docs/schemas/run-report-v1.schema.json`; Phase 21 JSON Schema documentation for `schemaVersion` 1 run reports. |
| Standalone examples | Consumer projects under `examples/` for Maven plugin, Gradle plugin, and JUnit Platform engine adoption paths. They are not root modules and are verified by `scripts/verify-examples.sh` or by the default examples section in `scripts/verify-all.sh`. |
| `run` | CLI command that discovers specs, owns production generation/update, can execute compiled examples, renders output, and can write reports. |
| Sealed-interface update deferral | Intentional limitation: existing sealed-interface source updates are skipped until nested permitted implementation updates can be done safely. |
| Source location metadata | Source file path and 1-based source line information captured from discovered specs/examples and propagated to runner results and reports where available. |
| Source-only spec | A discovered spec source file whose compiled class is unavailable to the runner; examples are reported as skipped. |
| Spec package prefix | Suite naming prefix for generated/discovered spec classes, default `spec`. |
| Spec root | File-system root searched for specification sources, default `src/test/java`. |
| Suite | Configuration grouping for spec root, source root, spec package prefix, production package prefix, and bootstrap metadata. |
| Typed proxy | Generated support method that returns `Matchable<T>` for a subject method, enabling syntax like `getRating().shouldReturn(5)`. |
| UniqueId segment | Optional JUnit Platform engine identifier segment. The Phase 17 engine uses `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and `[example:<methodName>]`. |
| Zero runtime dependency | Architectural policy that the runtime artifact depends only on the JDK and the project artifact itself. |
