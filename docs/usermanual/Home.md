# javaspec User Manual

Wiki home for the current javaspec MVP.

javaspec is a Java 8-compatible, zero-runtime-dependency specification tool inspired by PHPSpec. The current MVP supports the first spec-first loop plus the ADR 0004 correction work, follow-up factory construction generation, the Phase 7 matcher/expectation expansion, Phase 8 MVP collaborators/doubles, the completed Phase 9 CLI expansion, the Phase 10 advanced code-generation increment, the Phase 11 formatter/reporting/extension increment, the Phase 14 no-JUnit integration foundation, the Phase 15 optional Maven plugin adapter, the Phase 16 optional Gradle plugin adapter, the Phase 17 optional JUnit Platform engine adapter, the Phase 18 stable identifier/source-location/report polish increment, Phase 19 post-roadmap release/CI hardening, Phase 20 release-readiness scaffolding, and Phase 21 standalone adoption assets: specification/support generation, production type discovery and generation, constructor and static factory construction generation, typed proxy matcher support, direct `ObjectBehavior` convenience assertions, method skeleton/declaration/element generation, Phase 3 Java LTS profiles/catalog/API-symbol metadata/compatibility probes, Phase 4 configuration, naming, suite selection, discovery filters, the Phase 5/6 MVP reflection runner, JDK-proxy interface doubles, run-only controls for dry-run planning, stop-on-failure, progress/pretty formatting, profile selection, verbose diagnostics, UTF-8 JSON run reports, dependency-free JUnit XML-compatible reports, explicit classpath input, public formatter contracts, minimal programmatic extension contracts, a no-`System.exit` programmatic invocation API, standalone optional Maven `javaspec:run` execution, standalone optional Gradle `javaspecRun` execution, standalone optional JUnit Platform engine execution, stable ids/source metadata for IDE/CI consumers, aggregate local verification through `scripts/verify-all.sh`, a GitHub Actions workflow, `CHANGELOG.md`, `RELEASING.md`, `scripts/check-version-alignment.sh`, Maven `release-artifacts` source/javadoc packaging checks, Gradle source/javadoc jar readiness, standalone examples under `examples/`, `scripts/verify-examples.sh`, `docs/schemas/run-report-v1.schema.json`, and golden reports under `docs/examples/reports/`. Phase 12 compatibility/quality verification is complete through the Distrobox multi-JDK matrix for Java 8, 11, 17, 21, and 25; Phase 19 local aggregate verification passed for the core and standalone optional adapters; Phase 20 local release-readiness verification passed without publishing or deployment; and Phase 21 local adoption-assets verification passed for schema/golden reports plus standalone Maven, Gradle, and JUnit Platform examples.

> Current status: `describe` writes specification/support files only. `javaspec run` keeps discovery, generation, and source updates, then executes discovered examples when the compiled spec classes are available on the effective classloader or the selected explicit classloader. It reuses `DiscoveredSpec`/`SpecExample` metadata, so suite, class, and example filters remain effective; these objects/results now expose stable ids and source metadata where available. The matcher set includes expanded negation, type/instance, count/empty, string, and map key/value helpers while preserving zero runtime dependencies. Interface doubles are available for ordinary interfaces through JDK dynamic proxies, with method-name/exact-argument stubbing, call history, and verification helpers. Run controls are active: `--dry-run` reports pending work without writes or prompts, `--stop-on-failure` stops after the first FAILED or BROKEN executable example, `--formatter` selects built-in `progress` or `pretty` through the public formatter contract/registry, `--profile` validates/selects an LTS profile without deep enforcement yet, `--verbose` prints selected run settings, `--classpath` / `--classpath-file` supplies explicit compiled-class classpath entries, `--report` / `--report-file` writes a UTF-8 JSON runner report, and `--junit-xml` / `--junit-xml-file` writes a UTF-8 JUnit XML-compatible report. JSON reports include stable spec/example ids and source file/line fields where available; JUnit XML-compatible testcase elements include file/line attributes when source data is available. The `org.javaspec.invocation` API exposes no-JUnit, no-`System.exit` programmatic invocation around canonical discovery and `SpecRunner`. The standalone optional Maven plugin at `javaspec-maven-plugin/` provides `javaspec:run`; the standalone optional Gradle plugin at `javaspec-gradle-plugin/` provides plugin id `org.javaspec`, extension `javaspec`, and task `javaspecRun`; both use build-tool classpaths and delegate to `JavaspecLauncher` without requiring JUnit in projects under test. The standalone optional JUnit Platform engine at `javaspec-junit-platform-engine/` provides engine id `javaspec`, ServiceLoader registration, canonical discovery and `JavaspecLauncher` execution, JUnit Platform selector/filter integration, stable unique-id shape with MethodSource behavior, and javaspec-to-JUnit listener event mapping; it is an optional IDE/CI adapter only and does not require changes to javaspec spec authoring style. These adapters are intentionally standalone and not root Maven modules. Repository-root `mvn verify` remains core-only; `scripts/verify-all.sh` is the aggregate local release check for core plus standalone adapters and now runs `scripts/check-version-alignment.sh` first and standalone examples verification by default at the end. `CHANGELOG.md` and `RELEASING.md` document release notes and the release checklist. Maven `release-artifacts` profiles and Gradle source/javadoc jar readiness support local artifact checks only; they do not sign, stage, deploy, or publish. Projects that do not opt into the JUnit Platform engine still have no JUnit dependency and can keep CLI/programmatic/Maven/Gradle no-JUnit execution paths. Phase 10 interface-style generation is active for missing ordinary interfaces, missing annotations, missing sealed interfaces, and source-preserving existing ordinary interface/annotation updates; existing sealed-interface source updates are intentionally deferred. Bootstrap hooks remain parsed metadata and are not executed yet. The Phase 11 extension API is programmatic only; external extension discovery/loading is not implemented yet. Phase 12 verification passed across the Distrobox Java 8, 11, 17, 21, and 25 matrix; Phase 19 local aggregate verification passed root Maven verify/install/dependency audit plus standalone Maven plugin, Gradle plugin, and JUnit Platform engine verification; Phase 19 remote GitHub Actions success was user-/maintainer-confirmed for HEAD `4d30e63` on `develop`; and Phase 20 local release-readiness verification passed. Phase 20 and Phase 21 have local verification only and have not been pushed/run remotely unless a future report says otherwise. The MIT `LICENSE` and confirmed maintainer metadata are resolved, but public publication remains postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval are resolved.

## Quick start

From the repository root, core verification remains:

```sh
mvn verify
mvn dependency:tree -Dscope=runtime
```

Repository-root `mvn verify` is intentionally core-only. For release-readiness version alignment, run:

```sh
scripts/check-version-alignment.sh
```

For local aggregate release verification of the core plus standalone optional adapters and examples, run:

```sh
scripts/verify-all.sh
```

`verify-all.sh` runs version alignment first and standalone examples verification last by default. To verify only the standalone examples, run:

```sh
scripts/verify-examples.sh
``` The optional Maven plugin is standalone and intentionally not a root module. To verify it locally, install the current core first, then run the plugin build:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
```

The optional Gradle plugin is also standalone and intentionally not a root Maven module. To verify it locally, install the current core first, then run the plugin build with a compatible Gradle executable:

```sh
mvn -q -DskipTests install
gradle -p javaspec-gradle-plugin build
```

The optional JUnit Platform engine is standalone and intentionally not a root Maven module. To verify it locally, install the current core first, then run the engine build:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
```

The Phase 16 Java 21 Gradle verification used Gradle 8.8 at `/tmp/gradle-8.8`; that download is not committed.

Run the CLI:

```sh
java -jar target/javaspec-0.1.0-SNAPSHOT.jar --help
```

Short form used in the examples below:

```sh
javaspec='java -jar target/javaspec-0.1.0-SNAPSHOT.jar'
```

## Release and CI verification

Phase 19 adds a non-disruptive aggregate verification path. Phase 20 adds release-readiness scaffolding. Phase 21 adds standalone examples and report schema/golden documentation. The repository was not converted to Maven multi-module: root `mvn verify` continues to verify and audit only the zero-runtime-dependency core artifact, while the optional Maven plugin, Gradle plugin, JUnit Platform engine, and examples remain standalone artifacts.

Run the version-alignment check from the repository root:

```sh
scripts/check-version-alignment.sh
```

The version-alignment script checks the root Maven version, standalone Maven plugin version, standalone JUnit Platform engine version, Gradle plugin `version`, and Gradle plugin `javaspecCoreVersion` against one baseline.

Run the aggregate local release check from the repository root, or invoke the script by its full/relative path from elsewhere:

```sh
scripts/verify-all.sh
# or: /path/to/javaspec/scripts/verify-all.sh
```

The script resolves the repository root from its own location and runs these checks in order:

1. version alignment through `scripts/check-version-alignment.sh`;
2. root `mvn -q verify`;
3. root `mvn dependency:tree -Dscope=runtime`;
4. root `mvn -q -DskipTests install` to refresh the local core snapshot;
5. standalone Maven plugin `verify` and runtime dependency audit;
6. standalone JUnit Platform engine `verify` and runtime dependency audit;
7. standalone Gradle plugin `clean test build` and `runtimeClasspath` audit unless explicitly skipped;
8. standalone examples verification through `scripts/verify-examples.sh` unless explicitly skipped.

Environment variables:

| Variable | Meaning |
|---|---|
| `MAVEN_BIN` | Maven executable to use; defaults to `mvn`. |
| `JAVASPEC_GRADLE_BIN` | Explicit Gradle executable to use for the standalone Gradle plugin checks. |
| `JAVASPEC_SKIP_GRADLE=1` | Explicitly skip Gradle adapter verification. Use only when that skip is intentional. |
| `JAVASPEC_SKIP_EXAMPLES=1` | Explicitly skip the standalone examples section in `scripts/verify-all.sh`. |
| `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` | Explicitly skip only the Gradle example inside `scripts/verify-examples.sh`. |

When `JAVASPEC_GRADLE_BIN` is not set and Gradle is not skipped, resolution order is repository `./gradlew` if executable, `/tmp/gradle-8.8/bin/gradle`, then `gradle` on `PATH`. If none is found, the script fails with a clear diagnostic.

Release-readiness documentation and local packaging checks:

- `CHANGELOG.md` records notable changes and the current unreleased release-readiness scaffold.
- `RELEASING.md` is a checklist, not a publishing script.
- Maven `release-artifacts` profiles on the root, Maven plugin, and JUnit Platform engine builds create local sources and javadocs only.
- The standalone Gradle plugin build is prepared to produce source and javadoc jars.
- Safe URL, SCM, GitHub Issues, MIT license, and maintainer/developer metadata are present; the confirmed maintainer is `Mario Giustiniani <mariogiustiniani@gmail.com>`.

Optional local artifact checks:

```sh
mvn -q -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests package
gradle -p javaspec-gradle-plugin clean test build
```

These checks do not sign, stage, deploy, or publish artifacts. The MIT license and maintainer metadata are resolved, but public publication remains postponed until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, the final release version/tag, and final publish approval are resolved outside normal local verification.

The GitHub Actions workflow at `.github/workflows/ci.yml` triggers on `push`, `pull_request`, and `workflow_dispatch`. It defines a core job matrix over Java 8, 11, 17, 21, and 25 using Temurin and Maven cache, running root `mvn -q verify` plus the root runtime dependency audit. It also defines a Java 21 `full-verification` job with Maven cache and Gradle 8.8 setup that runs `scripts/verify-all.sh` with `JAVASPEC_GRADLE_BIN=gradle`. The workflow performs no publishing and uses no secrets.

Remote CI status must be stated by phase: Phase 19 remote GitHub Actions success was user-/maintainer-confirmed for HEAD `4d30e63` on `develop`; no run IDs, URLs, durations, or logs were independently queried from this environment. Phase 20 and Phase 21 have local verification only in the recorded evidence and have not been pushed/run remotely unless a future report says otherwise.

## Standalone examples and report schema

Phase 21 adds standalone consumer examples. They are not root Maven modules and are not part of repository-root `mvn verify` by themselves.

| Path | Demonstrates | Report output |
|---|---|---|
| `examples/maven-basic/` | `javaspec-maven-plugin` in a consuming Maven project | `target/javaspec/run-report.json`, `target/javaspec/junit-report.xml` |
| `examples/gradle-basic/` | Gradle plugin id `org.javaspec` through an included plugin build | `build/reports/javaspec/run-report.json`, `build/reports/javaspec/junit-report.xml` |
| `examples/junit-platform-basic/` | Standalone JUnit Platform engine with Maven Surefire configured for `*Spec` | `target/surefire-reports/` |

Run all examples locally with:

```sh
scripts/verify-examples.sh
```

The script installs local snapshots for the core, Maven plugin, and JUnit Platform engine before running examples. Set `MAVEN_BIN` to choose Maven, `JAVASPEC_GRADLE_BIN` to choose Gradle, or `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` to skip only the Gradle example. `scripts/verify-all.sh` runs this examples check by default unless `JAVASPEC_SKIP_EXAMPLES=1` is set.

Report documentation assets:

- JSON schema: [`../schemas/run-report-v1.schema.json`](../schemas/run-report-v1.schema.json)
- Golden JSON report: [`../examples/reports/passing-run-report-v1.json`](../examples/reports/passing-run-report-v1.json)
- Golden JUnit XML-compatible report: [`../examples/reports/passing-junit-report.xml`](../examples/reports/passing-junit-report.xml)

The golden examples show schemaVersion 1, stable id `spec.com.example.CalculatorSpec#it_adds_two_numbers`, `PASSED` status, and source line `11`.

## Commands

```sh
$javaspec describe <ClassName> [--config <file>] [--suite <name>] [--spec-dir <dir>]
$javaspec desc <ClassName> [--config <file>] [--suite <name>] [--spec-root <dir>]
$javaspec run [--config <file>] [--suite <name>] [--spec-dir <dir>] [--source-dir <dir>] [--classpath <path-list>] [--classpath-file <file>] [--generate] [--dry-run] [--stop-on-failure] [--formatter <progress|pretty>] [--profile <java8|java11|java17|java21|java25>] [--verbose] [--report <file>] [--report-file <file>] [--junit-xml <file>] [--junit-xml-file <file>] [--constructor-policy <delete|preserve|comment>] [--class <name>] [--example <name>]
```

Aliases and defaults:

| Long | Alias | Default | Command |
|---|---|---|---|
| `describe` | `desc` | n/a | n/a |
| `--config <file>` | n/a | inferred defaults | `describe`, `run` |
| `--suite <name>` | n/a | configuration default suite (`default` with inferred defaults) | `describe`, `run` |
| `--spec-dir` | `--spec-root` | selected suite `specDir` (`src/test/java` with inferred defaults) | `describe`, `run` |
| `--source-dir` | `--source-root` | selected suite `sourceDir` (`src/main/java` with inferred defaults) | `run` |
| `--classpath <path-list>` | n/a | no explicit entries | `run` |
| `--classpath-file <file>` | n/a | no explicit entries | `run` |
| `--generate` | n/a | `false` | `run` |
| `--dry-run` | n/a | `false` | `run` |
| `--stop-on-failure` | n/a | `false` | `run` |
| `--formatter <progress\|pretty>` | n/a | configuration `formatter` (`progress` with inferred defaults) | `run` |
| `--profile <java8\|java11\|java17\|java21\|java25>` | n/a | configuration `profile` (`java8` with inferred defaults) | `run` |
| `--verbose` | n/a | `false` | `run` |
| `--report <file>` | `--report-file <file>` | no JSON report | `run` |
| `--junit-xml <file>` | `--junit-xml-file <file>` | no JUnit XML-compatible report | `run` |
| `--constructor-policy <delete\|preserve\|comment>` | n/a | configuration `constructorPolicy` (`comment` with inferred defaults) | `run` |
| `--class <name>` | n/a | no class filter | `run` |
| `--example <name>` | n/a | no example filter | `run` |

`describe` writes specification files only. Production source generation, updates, execution, formatting, classpath selection, and reporting belong to `run`. After discovery/generation/update completes without declined prompts, `run` invokes the MVP reflection runner for discovered examples whose compiled spec classes are available on the effective or selected explicit classloader. `describe` rejects command-line `--source-dir`/`--source-root` and all run-only controls (`--classpath`, `--classpath-file`, `--generate`, `--dry-run`, `--stop-on-failure`, `--formatter`, `--profile`, `--verbose`, `--report`, `--report-file`, `--junit-xml`, `--junit-xml-file`, `--constructor-policy`, `--class`, and `--example`); a `sourceDir` present in a selected config suite is accepted because `describe` ignores source roots.

## Configuration files

`describe` and `run` accept `--config <file>` and `--suite <name>`. When no config file is supplied, javaspec uses `JavaspecConfiguration.defaults()`.

### Syntax

The configuration format is intentionally restricted and line-based so the runtime stays dependency-free:

- Blank lines and lines whose first non-whitespace character is `#` are ignored.
- Key/value separator is either `=` or `:`.
- There is no YAML, TOML, or JSON parser dependency.
- Top-level keys are `profile`, `formatter`, `constructorPolicy`/`constructor-policy`, `defaultSuite`/`default-suite`, and `bootstrap`.
- Valid `profile` values are `java8`, `java11`, `java17`, `java21`, and `java25`; valid `formatter` values are `progress` and `pretty`; valid constructor policies are `delete`, `preserve`, and `comment`.
- Suite keys use `suite.<name>.<property>` with properties `specDir`/`spec-dir`, `sourceDir`/`source-dir`, `specPackagePrefix`/`spec-package-prefix`, `packagePrefix`/`package-prefix`, and `bootstrap`.
- `bootstrap` values are comma-separated strings. They are metadata only in the current MVP and are not executed.
- `specPackagePrefix` and `packagePrefix` drive naming conventions for `describe`, `run`, discovery, and spec/support generation. `packagePrefix` may be empty. Other configured values, including bootstrap entries when the key is present, must not be blank.

### Defaults

| Setting | Default |
|---|---|
| Default suite name | `default` |
| Spec root | `src/test/java` |
| Source root | `src/main/java` |
| Spec package prefix | `spec` |
| Production package prefix | empty |
| Profile | `java8` |
| Formatter | `progress` |
| Constructor policy | `comment` |
| Bootstrap hooks | empty |

### Example config

```properties
# javaspec.conf
profile = java17
formatter = progress
constructorPolicy = preserve
defaultSuite = domain
bootstrap = org.example.SpecBootstrap

suite.domain.specDir = src/test/java
suite.domain.sourceDir = src/main/java
suite.domain.specPackagePrefix = spec
suite.domain.packagePrefix = org.example
suite.domain.bootstrap = org.example.DomainBootstrap, org.example.TestDataBootstrap

suite.integration.spec-dir: src/integrationSpec/java
suite.integration.source-dir: src/main/java
suite.integration.spec-package-prefix: spec
suite.integration.package-prefix:
```

### CLI precedence and examples

1. `--config <file>` loads explicit configuration; without it, defaults are inferred.
2. `--suite <name>` selects a configured suite; without it, `defaultSuite` is used.
3. The selected suite's `specDir`, `sourceDir`, `specPackagePrefix`, and `packagePrefix` are used. Command-line `--spec-dir`/`--spec-root` or `--source-dir`/`--source-root` override paths only; naming still comes from the selected suite.
4. `run` uses the configured `constructorPolicy`, `profile`, and `formatter` unless overridden by command-line `--constructor-policy`, `--profile`, or `--formatter`.
5. `run` applies repeatable `--class <name>` and `--example <name>` filters after suite selection.
6. `run --dry-run`, `--stop-on-failure`, and `--verbose` are command-line controls only.
7. `describe` accepts `--config` and `--suite` for spec-root and naming selection, rejects command-line `--source-dir`, and ignores any `sourceDir` value loaded from config.

Describe using a configured suite:

```sh
$javaspec describe org.example.Calculator --config javaspec.conf --suite domain
```

Run using a configured suite and generate non-interactively:

```sh
$javaspec run --config javaspec.conf --suite domain --generate
```

Override a selected-suite path from the command line:

```sh
$javaspec run --config javaspec.conf --suite domain --source-dir /tmp/demo/src/main/java --generate
```

### Diagnostics

Invalid configuration exits as command-line usage error (`64`) and starts with `Error: Invalid configuration:`. Diagnostics include line numbers for parser errors where available. Examples include duplicate keys, unknown keys, malformed lines without `=` or `:`, blank required values, invalid profiles, invalid formatters, invalid constructor policies, and selecting an unconfigured suite.

Examples:

```text
Error: Invalid configuration: Line 3: Invalid constructor policy: keep. Valid values: delete, preserve, comment.
Error: Invalid configuration: Suite 'api' is not configured. Available suites: default, domain.
```

A missing or unreadable config file exits with I/O error (`70`) and prints the config path.

### Current configuration limitations

- Bootstrap hooks are parsed as strings but are not executed yet.
- `profile` is selected and validated from config or `--profile`, but deep profile-aware execution/enforcement is not implemented yet.
- `formatter` is selected from config or `--formatter` and controls the built-in `progress` or `pretty` CLI output through the public formatter contract/registry; config cannot select extension-provided names because external extension loading is not implemented yet.
- Package-prefix naming is implemented for describe/run discovery, generation, MVP reflection execution, JSON report contents, and JUnit XML-compatible report test case class names; Phase 18 adds stable ids and source file/line metadata where available.
- The runner lifecycle is intentionally minimal in the MVP: fresh spec instance per example plus optional public no-arg `let()` and `letGo()`. Pending examples, bootstrap execution, deeper profile enforcement, and broader reporting beyond the implemented JSON/JUnit XML-compatible outputs remain future work.

## Suite naming and filters

Suite package prefixes configure how production classes map to spec/support classes.

With inferred defaults, `org.example.Calculator` maps to:

```text
src/test/java/spec/org/example/CalculatorSpec.java
src/test/java/spec/org/example/CalculatorSpecSupport.java
```

With this suite configuration:

```properties
suite.domain.specDir = src/test/java
suite.domain.sourceDir = src/main/java
suite.domain.specPackagePrefix = spec.domain
suite.domain.packagePrefix = org.example
```

`org.example.Calculator` maps to:

```text
src/test/java/spec/domain/CalculatorSpec.java
src/test/java/spec/domain/CalculatorSpecSupport.java
```

`run` supports repeatable filters:

```sh
$javaspec run --config javaspec.conf --suite domain --class Calculator
$javaspec run --config javaspec.conf --suite domain --class org.example.Calculator
$javaspec run --config javaspec.conf --suite domain --class spec.domain.CalculatorSpec
$javaspec run --config javaspec.conf --suite domain --example it_is_initializable
$javaspec run --config javaspec.conf --suite domain --example "it is initializable"
$javaspec run --config javaspec.conf --suite domain --example 0
```

Class filters match described qualified names, described simple names, spec qualified names, or spec simple names exactly. Example filters match public `void` example methods named `it_*` or `its_*` by method name, display name (underscores replaced by spaces), or source-order index. Filters affect discovery/generation selection and MVP reflection execution because the runner uses the same `DiscoveredSpec`/`SpecExample` metadata.

## Run controls

### Dry-run planning

`javaspec run --dry-run` performs discovery and planning without writing files and without prompting. It reports actions that would be generated or updated, including related specs/support, support updates, constructor changes, method bodies/declarations/elements, and missing production type generation.

Dry-run exit behavior:

| Condition | Exit code |
|---|---:|
| Pending generation/update work exists | `1` |
| No pending generation/update work and examples pass or are skipped-only | `0` |
| No pending generation/update work but executable examples fail or break | `1` |

`--dry-run` is useful in CI when pending generated work should fail the build without modifying the workspace.

### Stop on failure

By default, `javaspec run` processes all discovered example metadata. With `--stop-on-failure`, the reflection runner stops after the first FAILED or BROKEN executable example. Skipped examples before that point are still reported; examples after the first failure/break are not executed.

### Explicit classpath runs

`javaspec run` can execute specs from an explicit compiled-class classpath without requiring JUnit:

```sh
$javaspec run --classpath target/classes:target/test-classes
$javaspec run --classpath-file target/javaspec-classpath.txt
```

`--classpath <path-list>` reads entries separated by Java's `File.pathSeparator`: `:` on Unix-like systems and `;` on Windows. Empty entries are ignored after trimming.

`--classpath-file <file>` reads UTF-8 entries from a file, one entry per non-empty line. Lines whose trimmed form begins with `#` are comments and are ignored:

```text
# target/javaspec-classpath.txt
target/classes
target/test-classes
/path/to/dependency.jar
```

When explicit entries are supplied, javaspec builds a selected classloader over those entries and uses it for both production type existence checks and spec execution. `--verbose` lists the explicit entries under `Explicit classpath entries:`. The entries must point to already compiled classes or archives; javaspec still does not compile source or spec files itself.

Both classpath options belong to `run` only and are rejected by `describe`/`desc`.

### Formatters and extension contracts

`--formatter progress` is concise and summary-oriented. It is the default when neither config nor CLI selects a formatter.

`--formatter pretty` prints per-example status lines plus details for failed, broken, or skipped examples.

Built-in output is rendered through the public zero-dependency `org.javaspec.formatter.RunFormatter` contract and deterministic `RunFormatterRegistry`. Built-in names are `progress` and `pretty`; invalid formatter diagnostics list those names. A config file can set `formatter = progress` or `formatter = pretty`, and a valid CLI `--formatter` overrides the configured formatter.

The minimal Phase 11 extension API exposes `org.javaspec.extension.JavaspecExtension`, its short-name alias `Extension`, and `ExtensionContext`. An extension can receive an `ExtensionContext` and register run formatters programmatically through `context.runFormatterRegistry()` / `context.runFormatters()`. External extension discovery/loading is not implemented yet, so the CLI does not load extension classes from configuration, classpath scanning, service files, or plugins in this increment.

### JSON run reports

`--report <file>` writes a UTF-8 JSON runner report after normal no-spec output or runner summary rendering. `--report-file <file>` is an alias. JSON reports are available only for `run`; `describe`/`desc` rejects both options because it does not execute examples.

```sh
$javaspec run --report target/javaspec-report.json
$javaspec run --report-file target/javaspec-report.json --verbose
```

The report schema is versioned with `"schemaVersion": 1`. Phase 18 adds identifier/source fields additively while preserving the existing fields. The schema is documented at [`../schemas/run-report-v1.schema.json`](../schemas/run-report-v1.schema.json), and golden passing JSON/JUnit XML-compatible reports are available under [`../examples/reports/`](../examples/reports/). The top-level object contains:

- `summary`: total, passed, failed, broken, skipped, and successful counts for the whole run.
- `specs`: one entry per discovered spec result, with the spec name, `id`, `stableId`, optional `sourceFile`, executable flag, not-executable reason, per-spec summary, and examples.
- `examples`: spec name, method, display name, source-order index, status, detail, `id`, `stableId`, `fullName`, optional `source { file, line }`, and `failure`.
- `failure`: `null` when no throwable was captured; otherwise throwable class name, message, and stack trace lines.

Stable example ids use `<specQualifiedName>#<methodName>`, matching `ExampleResult.fullName()`.

A no-spec run with `--report` writes a valid empty report with zero summary counts and an empty `specs` array. Passing, failing, broken, and skipped-only runs write the report after summary rendering. Failed or broken executable examples still exit `1` after the report is written. Dry-run with pending generation/update work exits before execution and does not write a report. Report write failures print I/O diagnostics, include the report path, and exit `70`. JSON and JUnit XML-compatible reports can be requested together.

Minimal empty report example:

```json
{
  "schemaVersion": 1,
  "summary": {
    "total": 0,
    "passed": 0,
    "failed": 0,
    "broken": 0,
    "skipped": 0,
    "successful": true
  },
  "specs": []
}
```

### JUnit XML reports

`--junit-xml <file>` writes a UTF-8 JUnit XML-compatible runner report after normal no-spec output or runner summary rendering. `--junit-xml-file <file>` is an alias. The writer is dependency-free and does not require JUnit or JUnit Platform. The options are available only for `run`; `describe`/`desc` rejects both options.

```sh
$javaspec run --junit-xml target/javaspec-report.xml
$javaspec run --report target/javaspec-report.json --junit-xml target/javaspec-report.xml
```

The report uses a single `<testsuite name="javaspec">` element with `tests`, `failures`, `errors`, `skipped`, and `time="0"` attributes. Each example becomes a `<testcase>` whose `classname` is the spec qualified name and whose `name` is the example method name; `file` and `line` attributes are included when source data is available. `FAILED` examples become `<failure>`, `BROKEN` examples become `<error>`, and `SKIPPED` examples become `<skipped>`. A no-spec run writes a valid empty test suite.

Minimal empty JUnit XML report example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="javaspec" tests="0" failures="0" errors="0" skipped="0" time="0">
</testsuite>
```

Passing, failing, broken, skipped-only, and no-spec runs write requested reports after normal output. Failed or broken executable examples still exit `1` after the report is written. Dry-run with pending generation/update work exits before execution and does not write JUnit XML. Report write failures print I/O diagnostics, include the JUnit XML path, and exit `70`.

### Profile selection

`--profile <java8|java11|java17|java21|java25>` validates and selects the active target profile for the run. A valid CLI profile overrides the configured profile. The selected profile is visible in verbose output, but deep profile enforcement during execution is not implemented yet.

### Verbose diagnostics

`--verbose` prints the selected suite, spec root, source root, spec package prefix, production package prefix, constructor policy, profile, formatter, JSON report path when specified, JUnit XML path when specified, explicit classpath entries when supplied, dry-run setting, and stop-on-failure setting before run work proceeds.

These controls belong to `run` only and are rejected for `describe`/`desc`.

## No-JUnit programmatic invocation

The `org.javaspec.invocation` package provides a no-`System.exit` API for launchers, build tools, and CI adapters that want to invoke javaspec inside the current JVM without JUnit:

| Class | Responsibility |
|---|---|
| `JavaspecInvocation` | Immutable invocation input: either a `SpecDiscoveryRequest` or already discovered `DiscoveredSpec` list, a selected `ClassLoader`, and stop-on-failure behavior. |
| `JavaspecLauncher` | Delegates to canonical `SpecDiscovery`, `SpecRunner`, and `RunResult` semantics and returns a structured result. |
| `JavaspecInvocationResult` | Exposes discovered specs, the `RunResult`, exit code, success helpers, and failure helpers. |
| `JavaspecExitCode` | Maps passing, skipped-only, and no-spec runs to `0`; failed or broken runs to `1`. |

Example:

```java
SpecDiscoveryRequest request = SpecDiscoveryRequest.of(new File("src/test/java"));
JavaspecInvocation invocation = JavaspecInvocation.discovering(request, classLoader)
        .withStopOnFailure(true);
JavaspecInvocationResult result = JavaspecLauncher.run(invocation);

int exitCode = result.exitCode();
RunResult runResult = result.runResult();
```

The launcher does not call `System.exit`, so the host process can inspect the structured result and decide how to continue. `DiscoveredSpec`, `SpecResult`, and `ExampleResult` expose stable id aliases (`id`/`stableId` getter styles), and runner results carry source metadata where discovery supplied it. It is still a classpath-based executor: callers must provide a classloader that can load compiled production/spec classes, or skipped results will be produced for non-loadable specs.

## Optional Maven plugin

Phase 15 provides a standalone optional Maven plugin artifact at `javaspec-maven-plugin/`. It is intentionally not registered as a root module, so repository-root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact.

Local plugin verification sequence:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
```

The plugin packages `org.javaspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin`, uses Java source/target `1.8`, goal prefix `javaspec`, Maven API baseline `3.6.3`, Maven API and plugin annotations in `provided` scope, JUnit in `test` scope, and a compile-scope dependency on core `org.javaspec:javaspec`.

A consuming Maven build can declare the plugin as optional project tooling:

```xml
<plugin>
  <groupId>org.javaspec</groupId>
  <artifactId>javaspec-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</plugin>
```

The `JavaspecRunMojo` goal `javaspec:run` is bound to Maven's `verify` phase by default, requires test dependency resolution, and uses Maven's test classpath. It supports config, suite, `specDir`/`specRoot` selection, class/example filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible reports, and Maven logging. It delegates to canonical no-JUnit `org.javaspec.invocation.JavaspecLauncher` without `System.exit`, so projects under test do not need JUnit.

## Optional Gradle plugin

Phase 16 provides a standalone optional Gradle plugin artifact at `javaspec-gradle-plugin/`. It is intentionally not registered as a root Maven module, so repository-root `mvn verify` continues to build and audit only the zero-runtime-dependency core artifact.

Local plugin verification sequence:

```sh
mvn -q -DskipTests install
gradle -p javaspec-gradle-plugin build
```

The Phase 16 Java 21 verification used `/tmp/gradle-8.8/bin/gradle`; the downloaded Gradle 8.8 distribution was not committed. A cached Gradle 7.4.2 command was blocked by the installed Java 21 runtime with `Unsupported class file major version 65`; do not treat that cached-executable blocker as a javaspec feature failure or as proof that Gradle 7.4.2 verification passed.

In a consuming Gradle build where the standalone plugin artifact is available to Gradle, apply plugin id `org.javaspec` and configure the optional extension/task:

```groovy
plugins {
    id 'java'
    id 'org.javaspec' version '0.1.0-SNAPSHOT'
}

javaspec {
    suite = 'default'
    formatter = 'progress'
    jsonReportFile = file("$buildDir/reports/javaspec/report.json")
    junitXmlReportFile = file("$buildDir/test-results/javaspec.xml")
}

tasks.named('javaspecRun') {
    stopOnFailure = true
    failOnFailure = true
}
```

The plugin registers extension `javaspec` and task `javaspecRun` in Gradle's `verification` group. When the Gradle Java plugin/source sets are present, `javaspecRun` defaults to the `test` source set runtime classpath and depends on `testClasses`. The task supports `skip`, `failOnFailure` (default `true`), `stopOnFailure`, `configFile`, `suite`, `specDir`/`specRoot`, class filters, example filters, formatter `progress` or `pretty`, JSON report aliases (`reportFile`, `jsonReportFile`), and JUnit XML-compatible report aliases (`junitXmlReportFile`, `junitXmlFile`). It loads javaspec configuration when configured, selects suites, builds `SpecDiscoveryRequest` with `SpecNamingConvention`, uses a `URLClassLoader` over the Gradle classpath, sets/restores the thread context classloader, closes the loader, writes reports through core writers, logs through Gradle, throws `GradleException` on failed/broken examples when `failOnFailure=true`, and delegates to canonical no-JUnit `JavaspecLauncher` without `System.exit`.

No JUnit is required in projects under test; JUnit is only a plugin test dependency.

## Optional JUnit Platform engine

Phase 17 provides a standalone optional JUnit Platform engine artifact at `javaspec-junit-platform-engine/`. It is intentionally not registered as a root Maven module and remains outside the zero-runtime-dependency core artifact.

Local engine verification sequence:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
```

The engine artifact is `org.javaspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging `jar`, Java source/target `1.8`, and uses Java 8-compatible JUnit Platform `1.10.2` rather than JUnit Platform 6/JUnit 6. Runtime dependencies are isolated to the optional engine artifact: core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, and transitives `opentest4j`, `junit-platform-commons`, and `apiguardian-api`. JUnit Platform Launcher, JUnit Platform TestKit, and JUnit Jupiter are engine test-only dependencies.

The engine implementation is `org.javaspec.junit.platform.JavaspecTestEngine`, registered through `META-INF/services/org.junit.platform.engine.TestEngine`, and its engine id is `javaspec`. To opt in, place the engine artifact on the JUnit Platform test runtime classpath used by the selected IDE/CI/build launcher. Projects that do not opt into it still have no JUnit dependency and can keep CLI, programmatic, Maven plugin, or Gradle plugin no-JUnit execution paths.

Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`. Supported configuration parameters are:

| Parameter | Purpose |
|---|---|
| `javaspec.configFile` | Select a javaspec configuration file. |
| `javaspec.suite` | Select a configured suite. |
| `javaspec.specDir` / `javaspec.specRoot` | Select the spec root. |
| `javaspec.classFilters` / `javaspec.classFilter` / `javaspec.class` | Apply class filters over canonical discovery results. |
| `javaspec.exampleFilters` / `javaspec.exampleFilter` / `javaspec.example` | Apply example filters over canonical discovery results. |
| `javaspec.stopOnFailure` | Stop after the first failed or broken executable example. |

Class, package, method, and unique-id selectors are supported as filters over canonical discovery results. UniqueId segments use `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and `[example:<methodName>]`; Phase 18 retains this stable shape and MethodSource behavior while aligning descriptor reporting to stable ids.

Execution delegates to canonical no-JUnit `JavaspecLauncher` using discovered specs. Result mapping to JUnit Platform listener events is: passed -> successful, failed assertion results -> failed assertion-style throwable, broken results -> failed/error-style throwable, and skipped or non-loadable results -> skipped. The engine avoids `System.exit` and does not require changes to javaspec spec authoring style.

## Example execution MVP

`javaspec run` has two stages:

1. Preserve the existing discovery, related-spec handling, support updates, production type generation, constructor updates, and method updates.
2. If no prompt was declined and no I/O/usage error occurred, execute the discovered examples whose compiled spec classes are available on the effective classloader or the selected explicit classloader.

The runner is reflection-based and dependency-free. It does not compile source or spec files. If a spec exists only as source, or if the compiled spec class is otherwise unavailable to the CLI process or explicit classpath, the discovered examples are marked `SKIPPED` rather than executed. Compile first and put spec/production classes on the process classpath or pass already compiled entries with `--classpath` / `--classpath-file` before expecting execution.

Execution uses the existing discovery metadata:

- `DiscoveredSpec` selects the spec class and described type and exposes stable id aliases derived from the spec qualified name.
- `SpecExample` selects public `void` `it_*`/`its_*` examples, display names, source-order indexes, and 1-based source lines.
- `ExampleResult` stable ids use `<specQualifiedName>#<methodName>` and match `fullName()`; `SpecResult` stable ids derive from the spec qualified name.
- Suite, class, and example filters affect execution because they filter that metadata before the runner sees it.

Lifecycle behavior:

- A fresh spec instance is created for each example.
- Optional public no-argument `let()` runs before each example.
- Optional public no-argument `letGo()` runs after each example, including after failures.

Result states:

| State | Meaning |
|---|---|
| `PASSED` | The example completed normally. |
| `FAILED` | The example threw `AssertionError`. |
| `BROKEN` | A non-assertion throwable occurred in the example, `let()`, `letGo()`, spec instantiation, or reflection inspection. |
| `SKIPPED` | The spec class was not loadable, or the reflected example method was missing or not public no-arg. |

CLI summary example:

```text
Examples: 3 total, 1 passed, 1 failed, 0 broken, 1 skipped.
Failed examples:
  FAILED spec.org.example.CalculatorSpec#it_adds_numbers (it adds numbers): Assertion failed - java.lang.AssertionError: expected 4
Skipped examples:
  SKIPPED spec.org.example.CalculatorSpec#it_subtracts_numbers (it subtracts numbers): Example method not found or not public no-arg: it_subtracts_numbers
```

Exit code `1` is returned when executable examples fail or break. Skipped-only runs remain successful. With `--report` and/or `--junit-xml`, passing, failing, broken, and skipped-only runs write requested reports before the final run exit code is returned. Missing production generation or method-update prompts that are declined or unavailable also return exit code `1` before execution.

## BDD workflow

### 1. Describe a class

```sh
$javaspec describe org.example.Calculator
```

or:

```sh
$javaspec desc org.example.Calculator
```

This creates two test-source files:

```text
src/test/java/spec/org/example/CalculatorSpec.java
src/test/java/spec/org/example/CalculatorSpecSupport.java
```

Generated concrete spec:

```java
package spec.org.example;

import org.example.Calculator;

public class CalculatorSpec extends CalculatorSpecSupport {
    public void it_is_initializable() {
        shouldHaveType(Calculator.class);
    }
}
```

Generated support class:

```java
package spec.org.example;

import org.example.Calculator;

public class CalculatorSpecSupport extends org.javaspec.api.ObjectBehavior<Calculator> {
    public CalculatorSpecSupport() {
        super(Calculator.class);
    }
}
```

The concrete spec extends the generated support class. The support class extends `ObjectBehavior<Calculator>` and passes `Calculator.class` to enable lazy subject construction.

The import of `org.example.Calculator` is intentional. In a BDD/spec-first flow the production class may not exist yet, so the project can be red until `run` generates or the user writes the class.

### 2. Run discovery, generation, and execution

```sh
$javaspec run
```

`run` first performs discovery and any gated generation/update work. If execution can proceed, it then runs discovered examples whose compiled spec classes are available on the effective classloader or selected explicit classloader.

If `org.example.Calculator` is missing, javaspec asks:

```text
spec.org.example.CalculatorSpec describes missing class org.example.Calculator.
Target path: src/main/java/org/example/Calculator.java
Do you want me to create org.example.Calculator for you? [Y/n]
```

Answers:

| Answer | Meaning |
|---|---|
| `Y`, `y`, `yes`, or empty Enter | generate the production class skeleton |
| `N`, `n`, `no` | do not generate |
| any other value | javaspec asks again |

### 3. Accept generation interactively

```text
Do you want me to create org.example.Calculator for you? [Y/n]
y
Generated class skeleton: src/main/java/org/example/Calculator.java
```

Generated production class:

```java
package org.example;

public class Calculator { }
```

### 4. Decline generation

```text
Do you want me to create org.example.Calculator for you? [Y/n]
n
No production files were written.
```

Exit code: `1`.

### 5. Generate non-interactively

For CI or scripted usage, use `--generate` to answer yes without prompting:

```sh
$javaspec run --generate
```

This writes missing production type skeletons, generated specification support updates, constructor updates, static factory construction method skeletons, and supported missing method bodies/declarations/elements inferred from specs without interactive confirmation. After those updates, executable examples run only if the corresponding compiled spec classes are already on the effective classloader or selected explicit classloader; otherwise they are reported as skipped.

Use `--dry-run` when CI should report pending generated work without modifying the workspace:

```sh
$javaspec run --dry-run
```

Dry-run never writes files and never prompts. It exits `1` if any generation/update work is pending.

## PHPSpec-to-Java migration notes

javaspec is inspired by PHPSpec, but Java's packages, static typing, compilation model, and interfaces change how the concepts are expressed.

| PHPSpec concept | javaspec Java equivalent |
|---|---|
| `phpspec desc App\\Book` | `javaspec describe org.example.Book` creates Java `BookSpec` and `BookSpecSupport`. |
| PHP namespace convention | Java packages plus suite `specPackagePrefix` and `packagePrefix`. |
| Subject available as `$this` | Lazy `ObjectBehavior<T>` subject accessed through generated typed support methods or explicit `subject()`. |
| Spec examples | Public `void` methods named `it_*` or `its_*`; display names replace underscores with spaces. |
| `let()` / `letGo()` lifecycle | Optional public no-argument `let()` and `letGo()` on the Java spec class. |
| `beConstructedWith(...)` | Same method name for constructor arguments; configure before first subject access. |
| `beConstructedThrough(...)` / named constructors | Static factory construction with string-literal Java method names for generation. |
| `should*` / `shouldNot*` expectations | `Matchable<T>` methods such as `shouldReturn`, `shouldNotReturn`, `shouldContain`, `shouldHaveCount`, and direct `ObjectBehavior` convenience assertions. |
| PHPSpec generated method suggestions | `javaspec run` owns production generation/update after confirmation, `--generate`, or `--dry-run` planning. |
| Prophecy-style collaborators | Core javaspec doubles ordinary Java interfaces only through JDK dynamic proxies. |
| Formatters/extensions | Built-in `progress`/`pretty` formatters and programmatic extension contracts; no external CLI extension loading yet. |
| No-JUnit CI execution | CLI `--classpath` / `--classpath-file`, programmatic `org.javaspec.invocation`, optional Maven/Gradle plugin adapters, JSON reports, and JUnit XML-compatible reports without requiring JUnit. |

Practical migration guidance:

1. Start with `describe`, but expect the generated Java spec to import a production type that may not exist yet. The Java project may be temporarily red until `run --generate` or manual production code creation catches up.
2. Keep examples as public `void it_*`/`its_*` methods. The MVP runner ignores unrelated methods and can execute only compiled spec classes on the effective classloader or explicit classpath.
3. Prefer generated typed proxy methods for PHPSpec-like syntax, for example `getRating().shouldReturn(5)`. Use `match(subject().getRating()).shouldReturn(5)` when an explicit wrapper is clearer.
4. Configure construction before touching the subject. The last construction rule before first subject access wins; changes after instantiation are errors.
5. Use string-literal Java identifiers for factory construction markers when you want generation, for example `beConstructedThrough("create", value)`. Non-literal factory names can still describe runtime construction, but generation cannot infer a method name from them.
6. Treat generated method bodies as skeletons with Java default returns, not as inferred business behavior. javaspec does not synthesize return constants from expectations.
7. Prefer interface collaborators if you need core doubles. Concrete class, final class, static, constructor, primitive, array, annotation, and enum doubles are not supported in the zero-dependency runtime.
8. Use the restricted line-based config format instead of PHPSpec YAML-style configuration. Bootstrap entries are parsed metadata only and are not executed yet.
9. Use `--dry-run` in CI to detect pending generated work without modifying the workspace.
10. Use `--classpath` or `--classpath-file` after an external build has compiled production/spec classes.
11. Use `--report` for the implemented JSON runner report, `--junit-xml` for dependency-free JUnit XML-compatible output, or both after execution/no-spec handling. Both report paths include stable identifiers and source metadata where available after the Phase 18 polish increment.

## Construction semantics

Generated support classes configure `ObjectBehavior<Subject>` with `Subject.class`, so the subject is constructed lazily on first access. Construction can be configured before that first access.

Constructor arguments use `beConstructedWith(...)`. For generation, this remains constructor descriptor generation: `run` can create or update matching constructor skeletons, not factory methods.

```java
public class BookSpec extends BookSpecSupport {
    public void it_can_be_constructed_with_values() {
        beConstructedWith("Wizard", 5);

        getTitle().shouldReturn("Wizard");
        getRating().shouldReturn(5);
    }
}
```

Static factory construction uses `beConstructedThrough("create", args...)`. For generation, the factory marker now discovers/generates a static factory method skeleton returning the described type instead of an empty constructor marker.

```java
public void it_can_be_constructed_through_a_factory() {
    beConstructedThrough("create", "Wizard");

    getTitle().shouldReturn("Wizard");
}
```

For a described `Book`, that construction marker can generate a factory skeleton such as:

```java
public class Book {
    public static Book create(String arg0) {
        return new Book();
    }
}
```

Named factory forms behave the same way:

```java
beConstructedNamed("named");
beConstructedNamed("named", "Wizard");
beConstructedThroughNamed("createNamed", "Wizard");
```

For a described `Book`, these correspond to static factory skeletons such as `named()`, `named(String arg0)`, and `createNamed(String arg0)`, all returning `Book`.

Factory marker names must be string literals and valid Java identifiers to generate methods. Calls with non-string-literal names, such as `beConstructedThrough(factoryName, "Wizard")`, are ignored by generation because the method name is not statically known; they do not create empty constructor markers.

Construction can be overridden before instantiation. The last construction rule before the first subject access wins:

```java
public void it_overrides_construction_before_instantiation() {
    beConstructedWith("first");
    beConstructedWith("second");

    getTitle().shouldReturn("second");
}
```

After the subject has been instantiated, changing construction is an error:

```java
public void it_rejects_late_construction_changes() {
    getTitle().shouldReturn("Wizard"); // instantiates the subject

    beConstructedWith("late"); // throws IllegalStateException
}
```

Constructor or factory failures can be specified with `duringInstantiation()`:

```java
public void it_rejects_invalid_constructor_arguments() {
    beConstructedWith(-1);

    shouldThrow(IllegalArgumentException.class).duringInstantiation();
}
```

## Typed proxy matcher syntax

Generated support classes can expose subject-specific typed proxy methods. This allows PHPSpec-like Java syntax in the concrete spec:

```java
public class BookSpec extends BookSpecSupport {
    public void it_has_a_rating() {
        getRating().shouldReturn(5);
    }

    public void it_has_a_title() {
        getTitle().shouldContain("Wizard");
    }

    public void it_rejects_negative_ratings() {
        shouldThrow(IllegalArgumentException.class).duringSetRating(-3);
    }
}
```

For discovered methods, the support class contains methods similar to:

```java
protected org.javaspec.matcher.Matchable<Integer> getRating() {
    return match(subject().getRating());
}

protected org.javaspec.matcher.Matchable<String> getTitle() {
    return match(subject().getTitle());
}

protected void setRating(int rating) {
    subject().setRating(rating);
}
```

It also generates typed throw proxies such as `duringSetRating(...)`.

Generated typed spec support intentionally skips static factory descriptors discovered from construction markers such as `beConstructedThrough("create", ...)`. Static factories are construction methods on the described type, not instance subject proxies, so support classes do not generate `create().should...`, `duringCreate(...)`, or `subject().create(...)` wrappers for them.

The existing explicit wrapper style remains available:

```java
match(subject().getRating()).shouldReturn(5);
```

## Method generation

`run` discovers typed proxy calls and construction factory markers, then can generate supported missing subject method bodies, interface declarations, or annotation elements depending on the described production kind. Discovery currently covers the supported expanded chained matcher calls, typed throw calls such as `shouldThrow(...).duringSetRating(-3)`, direct `subject().method(...)` calls, simple setter-style calls, and static factory construction markers.

`beConstructedWith(...)` remains constructor descriptor generation. The factory construction forms `beConstructedThrough("create", args...)`, `beConstructedNamed("named", args...)`, and `beConstructedThroughNamed("createNamed", args...)` are method-generation inputs when the factory name is a string literal and a valid Java identifier; they generate static factory methods returning the described type instead of empty constructor markers.

Example spec:

```java
public class BookSpec extends BookSpecSupport {
    public void it_has_a_rating() {
        getRating().shouldReturn(5);
    }

    public void it_has_a_title() {
        getTitle().shouldContain("Wizard");
    }

    public void it_rejects_negative_ratings() {
        shouldThrow(IllegalArgumentException.class).duringSetRating(-3);
    }
}
```

With `--generate`, javaspec writes updates non-interactively:

```sh
$javaspec run --generate
```

Possible generated production class:

```java
package org.example;

public class Book {
    public int getRating() {
        return 0;
    }

    public String getTitle() {
        return null;
    }

    public void setRating(int rating) {
    }
}
```

Factory construction markers add static factory skeletons returning the described type. For example, `beConstructedThrough("create", "Wizard")` can add:

```java
public static Book create(String arg0) {
    return new Book();
}
```

Static factory descriptors are skipped when generated typed support is updated, because they are construction methods rather than instance subject proxy methods.

When the production source file already exists and `--generate` is not used, javaspec prompts before adding supported missing method skeletons, declarations, or elements:

```text
Do you want me to add missing method skeletons to org.example.Book in src/main/java/org/example/Book.java? [Y/n]
```

Default returns are Java 8-compatible: `false` for `boolean`, zero values for numeric primitives, `'\0'` for `char`, and `null` for reference types.

### Interface-style method declarations and annotation elements

For a described ordinary interface, missing production skeletons and existing ordinary interface sources use Java declarations without method bodies. Static descriptors are skipped because interface-style generation only adds discovered instance methods.

```java
package org.example;

public interface PaymentGateway {
    String status();

    boolean charge(String accountId, int cents);
}
```

For a described annotation, missing skeletons and existing annotation sources emit only Java-compatible no-argument non-static annotation elements. Descriptors with parameters, static descriptors, `void`, `Object`, or otherwise incompatible return types are ignored for annotation generation/update.

```java
package org.example;

public @interface GeneratedTag {
    String value();

    int priority();

    String[] tags();
}
```

For a missing described sealed interface, the generated root interface receives method declarations and each generated nested permitted implementation receives matching method bodies with Java default returns so the Java 17 source form remains valid.

```java
package org.example;

public sealed interface Shape permits Shape.Circle {
    int sides();

    final class Circle implements Shape {
        public int sides() {
            return 0;
        }
    }
}
```

Existing sealed-interface source updates are intentionally skipped for now. Updating such a source safely requires source-preserving insertion into both the sealed root and its nested permitted implementations, so this remains deferred.

## Constructor policy

`run` accepts constructor handling policy explicitly:

```sh
$javaspec run --constructor-policy comment
$javaspec run --constructor-policy preserve
$javaspec run --constructor-policy delete
```

| Policy | Meaning |
|---|---|
| `comment` | Default. Non-empty unmatched constructors are commented out. |
| `preserve` | Non-empty unmatched constructors are kept. |
| `delete` | Non-empty unmatched constructors are deleted. This is the explicit destructive opt-in. |

Empty generated/no-op unmatched constructors may be removed when safe, regardless of policy. Constructor policy applies to `run`; `describe` never updates production source. A config file can set `constructorPolicy`/`constructor-policy`; command-line `--constructor-policy` overrides the configured value.

## Matchers

Typed proxy methods return `Matchable<T>` for non-void subject methods. The explicit wrapper style also returns `Matchable<T>`:

```java
getRating().shouldReturn(5);
match(subject().getRating()).shouldReturn(5);
```

The implemented matcher set is dependency-free and includes these groups.

### Equality, identity, and negation

| Method | Meaning |
|---|---|
| `shouldBe(expected)` | identity comparison using Java `==` semantics |
| `shouldNotBe(unexpected)` | negated identity comparison |
| `shouldEqual(expected)` | equality comparison using `equals` semantics |
| `shouldNotEqual(unexpected)` | negated equality comparison |
| `shouldReturn(expected)` | alias for equality/return terminology |
| `shouldNotReturn(unexpected)` | negated return/equality alias |
| `shouldBeLike(expected)` | equality alias for PHPSpec-like terminology |
| `shouldNotBeLike(unexpected)` | negated `beLike` alias |
| `shouldBeEqualTo(expected)` | equality alias |
| `shouldNotBeEqualTo(unexpected)` | negated equality alias |

`MatcherRegistry` keeps the runtime dependency-free. It provides the built-in identity/equality/negated-identity matchers and a default negated-equality matcher for `shouldNotEqual` and its aliases.

### Type, implementation, and containment

| Method | Meaning |
|---|---|
| `shouldHaveType(Class<?>)` | requires a non-null value assignable to the expected type |
| `shouldBeAnInstanceOf(Class<?>)` | alias for `shouldHaveType` |
| `shouldReturnAnInstanceOf(Class<?>)` | return-terminology alias for `shouldHaveType` |
| `shouldImplement(Class<?>)` | requires the wrapped value, or wrapped `Class<?>`, to implement or extend the expected type |
| `shouldContain(value)` | checks character sequences, collections, maps, arrays, or iterables for a contained value |
| `shouldNotContain(value)` | negated containment check |

For maps, `shouldContain(value)` succeeds if the value is present as either a key or a value.

### Count, emptiness, and maps

| Method | Supported values |
|---|---|
| `shouldHaveCount(int)` | arrays, collections, maps, character sequences, and iterables |
| `shouldBeEmpty()` | arrays, collections, maps, character sequences, and iterables |
| `shouldNotBeEmpty()` | arrays, collections, maps, character sequences, and iterables |
| `shouldHaveKey(key)` / `shouldNotHaveKey(key)` | maps |
| `shouldHaveValue(value)` / `shouldNotHaveValue(value)` | maps |

Known limitation: count and emptiness checks on a generic `Iterable` iterate the iterable to compute the count. This consumes one-shot iterables and can hang on infinite iterables.

### String helpers

| Method | Meaning |
|---|---|
| `shouldStartWith(prefix)` / `shouldNotStartWith(prefix)` | character sequence prefix check |
| `shouldEndWith(suffix)` / `shouldNotEndWith(suffix)` | character sequence suffix check |
| `shouldMatchPattern(pattern)` / `shouldNotMatchPattern(pattern)` | Java regular expression check using `Pattern` |

Examples:

```java
getRating().shouldReturn(5);
getRating().shouldNotReturn(0);
getTitle().shouldContain("Wizard");
getTitle().shouldNotContain("Draft");
getTitle().shouldStartWith("The");
getTitle().shouldNotStartWith("Draft");
getTitle().shouldEndWith("Oz");
getTitle().shouldNotEndWith("Draft");
getTitle().shouldMatchPattern("Wiz.*");
getTitle().shouldNotMatchPattern("Draft.*");
getTags().shouldHaveCount(2);
getTags().shouldNotBeEmpty();
getMetadata().shouldHaveKey("isbn");
getMetadata().shouldHaveValue("Wizard");
getBookClass().shouldImplement(Readable.class);
```

### Direct `ObjectBehavior` convenience assertions

`ObjectBehavior` also exposes direct assertion methods for ad-hoc checks. These methods delegate through `match(actual)`, so they share behavior with typed proxy and explicit wrapper assertions:

```java
shouldReturn(subject().getRating(), 5);
shouldNotEqual(subject().getRating(), 0);
shouldHaveType(subject().getTitle(), String.class);
shouldImplement(subject(), Readable.class);
shouldContain(subject().getTitle(), "Wizard");
shouldHaveCount(subject().getTags(), 2);
shouldBeEmpty(subject().getNotes());
shouldHaveKey(subject().getMetadata(), "isbn");
shouldNotStartWith(subject().getTitle(), "Draft");
```

The direct convenience set covers equality/negation aliases, type/instance/implementation checks, containment, count/empty checks, map key/value checks, and string negations. Positive string helpers are available through typed proxy or explicit `match(actual)` usage.

### Custom matchers

Custom matchers can be registered in the matcher registry and may evaluate null subjects; javaspec passes the actual subject value, including `null`, to the matcher predicate.

```java
matcherRegistry().register("beAbsent", new org.javaspec.matcher.CustomMatcher<Object>(
    "beAbsent",
    new org.javaspec.matcher.CustomMatcher.MatchPredicate<Object>() {
        @Override
        public boolean test(Object subject, Object... expected) {
            return subject == null;
        }
    }
));

match(null).shouldMatch("beAbsent");
```

`SpecDiscovery` recognizes the expanded chained matcher names on typed proxy calls for method-discovery/default-return inference where applicable.

## Interface doubles

The Phase 8 MVP adds zero-runtime-dependency collaborator doubles under `org.javaspec.doubles`. Doubles are implemented with Java 8 JDK dynamic proxies, so the core runtime can double ordinary interfaces without bytecode libraries.

### Creating interface doubles

Use `Doubles` directly when working outside an `ObjectBehavior` subclass. The examples below assume `Notifier` is an ordinary interface.

```java
import org.javaspec.doubles.Doubles;
import org.javaspec.doubles.InterfaceDouble;

InterfaceDouble<Notifier> notifierDouble = Doubles.interfaceDouble(Notifier.class);
Notifier notifier = notifierDouble.instance();

// Shortcuts when only the proxy is needed:
Notifier proxy = Doubles.create(Notifier.class);
Notifier sameStyle = Doubles.of(Notifier.class);
Notifier alsoProxy = Doubles.proxy(Notifier.class);
```

Inside specs that extend `ObjectBehavior`, use the convenience APIs:

```java
Notifier notifier = doubleFor(Notifier.class);
InterfaceDouble<Notifier> notifierDouble = interfaceDouble(Notifier.class);
```

`Doubles.isDouble(value)` returns whether a value is a javaspec double. `Doubles.control(proxy)`, `Doubles.inspect(proxy)`, `doubleControl(proxy)`, and `inspectDouble(proxy)` return the control API for an existing proxy.

### Stubbing return values

Stubs match either by method name with any arguments or by method name with exact arguments:

```java
notifierDouble.when("channel").thenReturn("alerts");
notifierDouble.when("send", "alerts", new String[] {"ops", "oncall"}).thenReturn(Boolean.TRUE);

notifierDouble.returns("fallbackChannel", "general");
notifierDouble.returnsFor("send", Boolean.TRUE, "alerts", new String[] {"ops", "oncall"});
```

Exact argument matching supports `null` values and compares array contents rather than array identity. Unstubbed methods return Java defaults: `false` for `boolean`, zero values for numeric primitives, `'\0'` for `char`, `null` for reference types, and no action for `void` methods.

### Call history and verification

Interface method calls are recorded as immutable `Call` snapshots. The control APIs can inspect all calls, calls by method name, or calls by method name and exact arguments:

```java
notifier.send("alerts", new String[] {"ops", "oncall"});

notifierDouble.calls();
notifierDouble.calls("send");
notifierDouble.calls("send", "alerts", new String[] {"ops", "oncall"});
notifierDouble.callCount("send");
notifierDouble.callCount("send", "alerts", new String[] {"ops", "oncall"});
```

Verification supports called, not-called, called-once, and exact-count checks:

```java
notifierDouble.verify("send").called();
notifierDouble.verify("send", "alerts", new String[] {"ops", "oncall"}).calledOnce();
notifierDouble.verify("send").times(1);
notifierDouble.verify("missing").notCalled();

notifierDouble.verifyCalled("send");
notifierDouble.verifyCalledWith("send", "alerts", new String[] {"ops", "oncall"});
notifierDouble.verifyNotCalled("missing");
notifierDouble.verifyCallCount("send", 1);
```

`ObjectBehavior` adds spec-style convenience assertions:

```java
shouldHaveBeenCalled(notifier, "send");
shouldHaveBeenCalledWith(notifier, "send", "alerts", new String[] {"ops", "oncall"});
shouldNotHaveBeenCalled(notifier, "missing");
shouldHaveBeenCalledTimes(notifier, "send", 1);

doubleCalls(notifier);
doubleCalls(notifier, "send");
doubleCallCount(notifier, "send");
```

### Object methods and resets

`toString()`, `equals(Object)`, and `hashCode()` are handled deterministically by the proxy invocation handler. `toString()` identifies the doubled interface and internal id, `equals` uses proxy identity, and `hashCode` uses the stable internal id. These object methods are not user stubs and do not represent collaborator calls.

Call and stub state can be cleared separately or together:

```java
notifierDouble.clearCalls();
notifierDouble.clearStubs();
notifierDouble.reset();
```

### Supported targets and limitations

Only ordinary interfaces are supported. The double factory rejects `null`, primitive types, arrays, annotations, enums, concrete classes, and final classes with clear `IllegalArgumentException` messages.

Current MVP limitations:

- No concrete class, final class, static method, or constructor doubles.
- No wildcard or predicate argument matchers; matching is by method name or exact arguments only.
- No exception stubbing, callback stubbing, sequential returns, or side-effect stubbing.
- No bytecode-library integration in the core runtime.
- Default interface methods are not invoked by the proxy handler.

## Class-like type generation

javaspec supports these class-like production types. The javaspec binary remains Java 8-compatible; post-Java-8 forms are generated as source text and represented as metadata/strings.

| Production kind | Spec marker | Generated skeleton | Minimum Java source |
|---|---|---|---:|
| class | default, or `shouldBeAClass();` | `public class Foo { }` | 8 |
| final class | `shouldBeAFinalClass();` | `public final class Foo { }` | 8 |
| interface | `shouldBeAnInterface();` | `public interface Foo { }` | 8 |
| enum | `shouldBeAnEnum();` | `public enum Foo { }` | 8 |
| annotation | `shouldBeAnAnnotation();` | `public @interface Foo { }` | 8 |
| record | `shouldBeARecord();` | `public record Foo() { }` | 16 |
| sealed class | `shouldBeASealedClass();` | `public sealed class Foo permits Foo.Permitted { ... }` | 17 |
| sealed interface | `shouldBeASealedInterface();` | `public sealed interface Foo permits Foo.Permitted { ... }` | 17 |

The command stays PHPSpec-like: `describe` does not take a type flag. To describe a non-class type, edit the generated spec and add a marker example before running generation.

When method descriptors are discovered before a missing class-like type is generated, Phase 10 enriches interface-style skeletons where valid: ordinary interfaces receive non-static method declarations, annotations receive compatible no-argument elements, and sealed interfaces receive root declarations plus generated nested permitted implementation bodies with Java default returns. Existing class, final-class, sealed-class, enum, and record method-body generation remains unchanged.

## Extends and implements

Use spec markers to describe inheritance and implemented interfaces:

```java
public class ServiceSpec extends ServiceSpecSupport {
    public void it_extends_base_service() {
        shouldExtend(BaseService.class);
    }

    public void it_implements_payment_gateway() {
        shouldImplement(PaymentGateway.class);
    }
}
```

When related types are missing, `run` handles them before generating the owner type: it suggests or creates their specs, then writes their production skeletons. For sealed classes, `shouldPermit(...)` can create final permitted subtype specs that extend the sealed root. For sealed interfaces, permitted implementations remain nested in the sealed interface source file in this MVP; missing sealed-interface skeletons generate those nested implementations with any required default-return method bodies, while existing sealed-interface source updates are skipped for now.

## Custom directories

### Custom spec directory

```sh
$javaspec describe org.example.Calculator --spec-dir /tmp/demo/src/test/java
```

Creates:

```text
/tmp/demo/src/test/java/spec/org/example/CalculatorSpec.java
/tmp/demo/src/test/java/spec/org/example/CalculatorSpecSupport.java
```

Equivalent alias:

```sh
$javaspec desc org.example.Calculator --spec-root /tmp/demo/src/test/java
```

### Custom source directory during run

```sh
$javaspec run \
  --spec-dir /tmp/demo/src/test/java \
  --source-dir /tmp/demo/src/main/java
```

Equivalent alias:

```sh
$javaspec run \
  --spec-root /tmp/demo/src/test/java \
  --source-root /tmp/demo/src/main/java
```

### Custom directories with non-interactive generation

```sh
$javaspec run \
  --spec-dir /tmp/demo/src/test/java \
  --source-dir /tmp/demo/src/main/java \
  --generate
```

## Existing specification

If the spec already exists:

```sh
$javaspec describe org.example.Calculator
```

javaspec reports it and does not overwrite the spec:

```text
Specification spec.org.example.CalculatorSpec exists; no generation needed.
Spec file: src/test/java/spec/org/example/CalculatorSpec.java
No production class was generated.
```

If the support file is missing, `describe` creates `CalculatorSpecSupport.java`.

## Existing production class

If a spec exists and the production class already exists in the source tree:

```text
src/test/java/spec/org/example/CalculatorSpec.java
src/test/java/spec/org/example/CalculatorSpecSupport.java
src/main/java/org/example/Calculator.java
```

then:

```sh
$javaspec run
```

prints:

```text
spec.org.example.CalculatorSpec describes org.example.Calculator; class exists.
Source file: src/main/java/org/example/Calculator.java
```

No production type skeleton is generated. If the spec describes constructors, static factories, or supported missing methods, `run` may update the existing source according to the constructor policy and method-generation confirmation rules. Existing ordinary interfaces can receive missing declarations and existing annotations can receive compatible missing elements source-preservingly and idempotently. Existing sealed-interface source updates are skipped for now because nested permitted implementations would also need safe source-preserving updates.

If the class is available on the effective classloader or selected explicit classloader instead of the source tree, javaspec reports:

```text
Classpath: present
```

After these existence and update checks, `run` executes the filtered examples when the compiled spec class itself is also available on the effective or selected explicit classloader. If only the source file is present, the examples are listed as `SKIPPED` because the CLI does not compile them.

## No specs found

```sh
$javaspec run --spec-dir /tmp/empty-spec-root
```

Output:

```text
No specifications found in /tmp/empty-spec-root.
```

Exit code: `0`.

## Spec-to-class mapping

javaspec follows a PHPSpec-inspired namespace convention. The default convention uses spec package prefix `spec` and an empty production package prefix; configured suites can replace both prefixes.

| Spec file | Spec class | Support class | Described production type |
|---|---|---|---|
| `src/test/java/spec/org/example/CalculatorSpec.java` | `spec.org.example.CalculatorSpec` | `spec.org.example.CalculatorSpecSupport` | `org.example.Calculator` |
| `src/test/java/spec/com/acme/UserSpec.java` | `spec.com.acme.UserSpec` | `spec.com.acme.UserSpecSupport` | `com.acme.User` |
| `src/test/java/spec/domain/CalculatorSpec.java` with `specPackagePrefix=spec.domain`, `packagePrefix=org.example` | `spec.domain.CalculatorSpec` | `spec.domain.CalculatorSpecSupport` | `org.example.Calculator` |

Rules:

1. The spec class name ends with `Spec`.
2. The generated support class name ends with `SpecSupport`.
3. The spec package starts with the active `specPackagePrefix`.
4. The described production package is the active `packagePrefix` plus the spec package suffix after `specPackagePrefix`. With the default empty production package prefix this is the spec package without the leading `spec.`.
5. The described production type name is the spec class name without the trailing `Spec`.
6. The described production kind defaults to class unless the spec contains a marker such as `shouldBeAFinalClass();`, `shouldBeAnInterface();`, `shouldBeAnEnum();`, `shouldBeAnAnnotation();`, `shouldBeARecord();`, `shouldBeASealedClass();`, or `shouldBeASealedInterface();`.
7. `shouldExtend(...)`, `shouldImplement(...)`, and `shouldPermit(...)` class literals are resolved through imports or the described production package.
8. Constructor and method descriptors are discovered heuristically from supported construction and typed proxy syntax: `beConstructedWith(...)` describes constructors; factory construction markers with string-literal Java-identifier names describe static factory methods; typed proxy calls using the expanded chained matcher names, throw-proxy calls, direct `subject().method(...)`, and simple setter calls describe instance methods where applicable. For interface/annotation described kinds, supported descriptors produce declarations or elements instead of method bodies; static descriptors are not inserted into ordinary interfaces, incompatible annotation descriptors are ignored, and existing sealed-interface source updates remain deferred.

Legacy same-package specs are also discovered by convention when the default production package prefix is empty, but new specs generated by `describe` use the active suite naming convention.

## Invalid usage examples

### Run-only flags do not belong to `describe`

```sh
$javaspec describe org.example.Calculator --generate
$javaspec describe org.example.Calculator --dry-run
$javaspec describe org.example.Calculator --stop-on-failure
$javaspec describe org.example.Calculator --formatter pretty
$javaspec describe org.example.Calculator --profile java17
$javaspec describe org.example.Calculator --verbose
$javaspec describe org.example.Calculator --classpath target/classes:target/test-classes
$javaspec describe org.example.Calculator --classpath-file target/javaspec-classpath.txt
$javaspec describe org.example.Calculator --report target/javaspec-report.json
$javaspec describe org.example.Calculator --junit-xml target/javaspec-report.xml
```

Result examples:

```text
Error: The --generate option belongs to run; describe creates only a specification skeleton.
Error: The --dry-run option belongs to run; describe creates only a specification skeleton.
Error: The --formatter option belongs to run; describe does not execute examples.
Error: The --classpath option belongs to run; describe does not execute examples.
Error: The --report option belongs to run; describe does not execute examples.
Error: The --junit-xml option belongs to run; describe does not execute examples.
```

### `--source-dir` does not belong to `describe`

```sh
$javaspec describe org.example.Calculator --source-dir src/main/java
```

Result:

```text
Error: The source directory is used by run; describe writes only to the spec directory.
```

### Unknown constructor policy, formatter, or profile

```sh
$javaspec run --constructor-policy keep
$javaspec run --formatter dots
$javaspec run --profile java99
```

Result examples:

```text
Error: Invalid constructor policy: keep. Valid values: delete, preserve, comment.
Error: Invalid formatter: dots. Valid values: progress, pretty.
Error: Invalid profile: java99. Valid values: java8, java11, java17, java21, java25.
```

### Unknown command

```sh
$javaspec generate org.example.Calculator
```

Result:

```text
Error: Unknown command: generate
```

### Invalid class name

```sh
$javaspec describe class
```

Result:

```text
Error: Invalid class name: Class name segment is a reserved Java word: class
```

## Exit codes

| Code | Meaning |
|---:|---|
| `0` | success, help, no specs found, existing/generated/updated targets, dry-run with no pending generation/update work, passed examples, or skipped-only example runs |
| `1` | missing production type or missing method update was not generated because the prompt was declined or input was unavailable; dry-run found pending generation/update work; or executable examples failed/broke |
| `64` | invalid command line usage |
| `70` | I/O or security error while reading config, reading a classpath file, checking, writing files, or writing a JSON/JUnit XML report |

## Dependency policy

Runtime dependencies are not allowed for the core artifact. The repository test suite uses JUnit only as a test-scope dependency; using javaspec specs, the CLI runner, programmatic invocation, the optional Maven plugin, the optional Gradle plugin, and JUnit XML-compatible reports do not require JUnit in projects under test. The Phase 15 Maven plugin is a separate optional artifact with Maven API/plugin annotations in `provided` scope, JUnit only in plugin `test` scope, and a runtime tree containing the plugin plus compile-scope core `org.javaspec:javaspec` only. The Phase 16 Gradle plugin is a separate optional artifact with JUnit/TestKit only as plugin test dependencies; its verified runtimeClasspath contains only core `org.javaspec:javaspec:0.1.0-SNAPSHOT`. The Phase 17 JUnit Platform engine is a separate optional artifact over the canonical javaspec runner; its runtime dependencies are isolated to the engine artifact and do not enter the core runtime dependency tree. Projects that do not opt into the engine keep the no-JUnit CLI/programmatic/Maven/Gradle execution paths.

Check core runtime dependencies:

```sh
mvn dependency:tree -Dscope=runtime
```

Expected root output contains only the project artifact:

```text
org.javaspec:javaspec:jar:0.1.0-SNAPSHOT
```

Check the standalone optional adapters through the aggregate script or separately when needed:

```sh
scripts/verify-all.sh
mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
```

Expected Maven plugin runtime scope contains the plugin plus compile-scope core `org.javaspec:javaspec` only. Expected Gradle plugin runtimeClasspath contains only core `org.javaspec:javaspec:0.1.0-SNAPSHOT`. Expected JUnit Platform engine runtime scope contains core `org.javaspec:javaspec`, `org.junit.platform:junit-platform-engine`, `opentest4j`, `junit-platform-commons`, and `apiguardian-api`, with no runtime `junit-jupiter`, `junit-platform-launcher`, or `junit-platform-testkit`.

## Verification

Current verification after Phase 21:

- `LICENSE` is identical to `origin/main:LICENSE` with blob `b990d5492f3ef404ffc145890b83e51914351bb5`.
- `bash -n scripts/check-version-alignment.sh`, `bash -n scripts/verify-all.sh`, and `bash -n scripts/verify-examples.sh` passed; all three scripts are executable.
- `bash scripts/check-version-alignment.sh` passed with all checked versions aligned at `0.1.0-SNAPSHOT`.
- `git diff --check`, `git diff --cached --check`, and untracked whitespace checks passed.
- Effective POM generation passed for root, Maven plugin, and JUnit engine.
- Maven POM metadata checks for root, Maven plugin, and JUnit engine passed: MIT License, URL `https://opensource.org/licenses/MIT`, distribution `repo`, Mario Giustiniani email, and maintainer role.
- Gradle generated POMs `pluginMaven` and `javaspecPluginMarkerMaven` include MIT license and maintainer metadata.
- `mvn -q verify` passed with 386 tests, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` passed with root runtime containing no dependencies beyond `org.javaspec:javaspec`.
- `mvn -q -Prelease-artifacts -DskipTests package` passed and found non-empty root main, sources, and javadoc jars.
- `mvn -q -DskipTests install` passed.
- Maven plugin and JUnit Platform engine `-Prelease-artifacts -DskipTests package` checks passed and found non-empty main, sources, and javadoc jars.
- Standalone Maven plugin `mvn -q verify` passed with 12 tests; standalone JUnit Platform engine `mvn -q verify` passed with 12 tests.
- Gradle plugin publication POM generation passed; Gradle plugin `clean test build` passed with 11 tests and produced non-empty main/sources/javadoc jars; Gradle runtime dependencies contained only `org.javaspec:javaspec:0.1.0-SNAPSHOT`.
- Full aggregate `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed, covering version alignment, core verify, root audit, local install, Maven plugin verify/audit, JUnit engine verify/audit, Gradle plugin build/audit, and standalone examples verification.
- Phase 21 checks confirmed no core production/test Java changes, parsed the schema and golden reports, validated the golden JSON against `docs/schemas/run-report-v1.schema.json`, verified ignored generated example output directories, and passed `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh`.
- No tester files were modified and no publish/deploy/signing commands were run.
- Phase 20 and Phase 21 have no remote GitHub Actions success claim. Phase 19 remote GitHub Actions success remains user-/maintainer-confirmed for HEAD `4d30e63` on `develop`.
- Phase 18 tests asserted stable ids/source metadata in discovery, runner results, JSON reports, JUnit XML reports, CLI reports, Maven plugin reports, and Gradle plugin reports.
- Phase 12 remains the current cross-JDK evidence: Distrobox `1.8.2.5` with Podman `5.8.2` ran Maven `3.9.16` Temurin containers for Java 8, 11, 17, 21, and 25; every container executed `mvn clean` and `mvn verify` and passed with 364 tests, 0 failures, 0 errors, and 0 skipped.
- Matrix runtimes: Java `1.8.0_492`, `11.0.31`, `17.0.19`, `21.0.11 LTS`, and `25.0.3 LTS`.
- The Java 25 runtime reflection probe and Java 25 runtime dependency audit passed.
- Verification blockers: none for Phase 21 adoption-assets verification. Publication remains intentionally postponed because GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final release version/tag, and final publish approval remain unresolved.

See [`../test-report.md`](../test-report.md) for the consolidated test and quality report.

## Future backlog

The original numbered roadmap is complete through Phase 18. Phase 19 adds post-roadmap release/CI hardening without Maven multi-module conversion, Phase 20 adds release-readiness scaffolding without public publishing, and Phase 21 adds standalone adoption examples plus report schema/golden documentation without core runtime changes. Future feature work should be tracked as new roadmap/backlog items and should distinguish implemented Phase 18 stable identifier/source-location/report polish, Phase 19 aggregate verification/CI workflow setup, Phase 20 release-readiness scaffolding, and Phase 21 adoption assets from broader IDE/CI, adoption, or release ideas that are not implemented.

Potential backlog items include pending examples, bootstrap execution, deeper profile-aware enforcement, external extension loading, advanced double integrations, richer failure-location diagnostics, broader classpath diagnostics, actual publishing/signing automation after signing/portal/final-approval decisions, and any future multi-module conversion decision. No-JUnit CLI/programmatic/Maven/Gradle paths remain first-class; the JUnit Platform engine remains an optional adapter.

## Current MVP limitations

- javaspec does not compile source or spec files itself; source-only or otherwise unavailable spec classes are skipped/not executable until compiled classes are present on the effective classloader, selected explicit classloader, build-tool adapter classpath, or JUnit Platform engine runtime classpath. `--classpath`, `--classpath-file`, and adapter-supplied entries must point to already compiled classes or archives. The optional Maven plugin, Gradle plugin, and JUnit Platform engine supply integration paths but remain standalone artifacts that are not covered by repository-root `mvn verify`; verify them through `scripts/verify-all.sh` or separately after installing the current core. Phase 21 examples are standalone consumer projects that currently use local snapshots rather than public artifact resolution.
- The runner lifecycle is an MVP: fresh spec instance per example plus optional public no-arg `let()` and `letGo()`. Pending examples, bootstrap execution, and deep profile-aware execution/enforcement remain future work.
- JSON reporting remains the Phase 11 `schemaVersion` 1 runner report with Phase 18 additive stable id/source fields, and Phase 14 JUnit XML-compatible reporting remains intentionally minimal with Phase 18 additive testcase `file`/`line` attributes when source data is available. There is no config-level report destination, and dry-run pending generation/update exits before execution without writing reports.
- Configuration files currently drive selected suite paths, package-prefix naming, constructor-policy defaults, profile and formatter defaults, and run class/example filters; bootstrap hooks remain metadata until later runner features are implemented.
- The extension API is minimal and programmatic: extensions can receive `ExtensionContext` and register run formatters, but external extension discovery/loading is not implemented, so CLI formatter selection remains limited to built-in `progress` and `pretty`.
- Source parsing and generation use Java 8-compatible heuristics, not a full Java parser.
- Generated post-Java-8 source forms, such as records and sealed types, require an appropriate JDK to compile.
- Method generation covers the supported typed proxy, throw-proxy, direct subject/setter, and static factory construction marker syntax; it can emit method bodies for class-like body-bearing types, declarations for ordinary interfaces, compatible elements for annotations, and missing sealed-interface skeleton declarations plus nested permitted implementation bodies. It is not a general Java source synthesis engine.
- Existing sealed-interface source updates are intentionally skipped until nested permitted implementations can also be updated source-preservingly.
- Count and emptiness checks on generic `Iterable` values consume the iterable and can hang on infinite iterables.
- Doubles/collaborators are interface-only in the core runtime. Concrete class, final class, static method, constructor, primitive, array, annotation, and enum doubles are not supported.
- Double argument matching has no wildcard or predicate matchers; stubbing is return-value-only and does not support exceptions, callbacks, sequences, or side effects.
- Default interface methods are not invoked by interface doubles.
