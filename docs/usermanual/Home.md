# javaspec User Manual

User manual for current javaspec development.

javaspec is a Java 8-compatible, zero-runtime-dependency specification tool inspired by PHPSpec. The recommended authoring style is subject-centric and PHPSpec-like: one `it_*` / `its_*` behavior method, optional `let()` / `letGo()`, `beConstructedWith(...)` or `beConstructedThrough(...)` before first subject access, and fluent `should*` expectations.

Preferred spec syntax:

1. **Generated typed proxy style (recommended):** Generated support classes expose typed wrapper methods so concrete specs read fluently: `method().shouldReturn(expected)`. This is the primary PHPSpec-like style for ordinary subject behavior.
2. **Explicit subject matcher style:** `match(subject().method(...)).shouldReturn(expected)` is equally supported and useful when a proxy method has not been generated yet or when an explicit subject call is clearer.
3. **Direct `ObjectBehavior` convenience assertions:** helpers such as `shouldReturn(actual, expected)` are available for ad-hoc checks and compatibility, but should not be presented as the main style in examples.

Implemented capabilities include:

- PHPSpec-style specification/support generation, production type discovery and generation,
  constructor and static factory construction generation, recommended typed proxy matcher support,
  explicit `match(subject()...)` expectations, direct `ObjectBehavior` convenience assertions for
  ad-hoc checks, and source-preserving method/declaration/element
  generation.
- Java LTS profile metadata and conservative target-profile enforcement before generation/update
  writes.
- Suite-oriented configuration, naming, filters, run controls, explicit classpath input, optional
  current-JDK compilation, bootstrap hooks, and execution-availability diagnostics.
- Expanded matchers, JDK-proxy interface doubles, stronger double APIs for argument
  matchers/throwing stubs/answer callbacks, and optional ByteBuddy-backed non-final concrete-class
  doubles through the standalone `javaspec-bytecode-doubles` adapter.
- UTF-8 JSON reports, dependency-free JUnit XML-compatible reports, stable ids/source metadata,
  additive Phase 35 report metadata/properties, formatter contracts, minimal extension contracts,
  and JDK `ServiceLoader` discovery for formatter/extension/bootstrap providers.
- No-`System.exit` programmatic invocation plus standalone optional Maven, Gradle, and JUnit
  Platform adapters that delegate to the canonical runner without making JUnit required for
  CLI/programmatic/Maven/Gradle no-JUnit paths.
- Release/verification assets such as `scripts/verify-all.sh`, `.github/workflows/ci.yml`,
  `CHANGELOG.md`, `RELEASING.md`, `scripts/check-version-alignment.sh`, standalone examples under
  `examples/`, `scripts/verify-examples.sh`, the report schema, and golden reports.

Verification status:

- The canonical local aggregate check is `./scripts/verify-all.sh`; it verifies the zero-dependency
  core, standalone Maven/Gradle/JUnit Platform adapters, runtime dependency audits, and standalone
  examples.
- Recent PHPSpec-compatibility work covers CLI, Maven, Gradle, and JUnit Platform smoke paths,
  PHPSpec-style example data, row-aware JSON/JUnit XML/JUnit Platform reporting, and parser/generator
  hardening for records, overloads, unknown `null`, `var`, class literals, arrays, and common
  value-object argument expressions.
- Remote CI status should be confirmed from GitHub Actions for the commit being released; older
  phase-specific commit references in historical notes are not release evidence for current HEAD.

## Current status

- `describe` writes specification/support files only.
- `javaspec run` owns discovery, target-profile enforcement, generation/update planning or writes,
  optional compilation, bootstrap hooks, report writing, and example execution when compiled
  hook/spec classes are available on the effective classloader, selected explicit classloader,
  adapter classpath, or compile-output-first classloader.
- Suite, class, and example filters remain effective because execution reuses `DiscoveredSpec` /
  `SpecExample` metadata; these objects/results expose stable ids and source metadata where
  available.
- Run controls are active: dry-run planning, stop-on-failure, built-in or ServiceLoader-discovered
  formatters, profile selection/enforcement, verbose diagnostics, explicit classpath input, optional
  CLI compilation, JSON reports, and JUnit XML-compatible reports.
- JSON reports remain `schemaVersion` 1 and include stable ids, source file/line fields where
  available, separate pending counts, and optional Phase 35 metadata. JUnit XML-compatible reports
  include Phase 35 testsuite metadata/properties, testcase file/line attributes when available, and
  skipped mappings for both skipped and pending examples.
- `io.github.jvmspec.invocation` exposes no-JUnit, no-`System.exit` programmatic invocation around
  canonical discovery, bootstrap hooks, and `SpecRunner`.
- The optional Maven plugin, Gradle plugin, and JUnit Platform engine are standalone adapters
  outside the root Maven reactor. Maven and Gradle use build-tool classpaths and can log `javaspec:`
  execution-availability warnings; the JUnit Platform engine remains an optional IDE/CI adapter.
- Bootstrap hooks are executable compiled Java classes or ServiceLoader providers. They receive
  immutable `BootstrapContext`, run immediately before examples, preserve configured order and
  duplicates, and do not add script engines, package scanning, dependency resolution, or runtime
  dependencies.
- Extension activation is classpath-based through configured extension names and ServiceLoader
  providers. Plugin lookup and automatic classpath repair remain unimplemented.
- Core doubles remain ordinary-interface-only JDK proxies. Optional non-final concrete-class doubles
  require `javaspec-bytecode-doubles`. Optional final-class, static-method, and construction-aware
  doubles require the separate `javaspec-bytecode-agent` adapter and JVM instrumentation support;
  `examples/bytecode-agent-basic/` demonstrates final-class and static-method doubles.
- Repository-root `mvn verify` remains core-only. `scripts/verify-all.sh` is the aggregate local
  check for core, standalone adapters, and examples.
- Artifacts are published on Maven Central under `io.github.jvmspec`. The Gradle plugin is
  published on the Gradle Plugin Portal with plugin id `io.github.jvmspec`. Add the dependency
  `io.github.jvmspec:javaspec:0.1.0` (test scope).

## Quick start

From the repository root, core verification remains:

```sh
mvn verify
mvn dependency:tree -Dscope=runtime
```

Repository-root `mvn verify` is intentionally core-only. For release-readiness version alignment,
run:

```sh
scripts/check-version-alignment.sh
```

For local aggregate release verification of the core plus standalone optional adapters and examples,
run:

```sh
scripts/verify-all.sh
```

`verify-all.sh` runs version alignment first and standalone examples verification last by default.
To verify only the standalone examples, run:

```sh
scripts/verify-examples.sh
```

The optional Maven plugin is standalone and intentionally not a root module. To verify it locally,
install the current core first, then run the plugin build:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
```

The optional Gradle plugin is also standalone and intentionally not a root Maven module. To verify
it locally, install the current core first, then run the plugin build with a compatible Gradle
executable:

```sh
mvn -q -DskipTests install
gradle -p javaspec-gradle-plugin build
```

The optional JUnit Platform engine is standalone and intentionally not a root Maven module. To
verify it locally, install the current core first, then run the engine build:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
```

The Phase 16 Java 21 Gradle verification used Gradle 8.8 at `/tmp/gradle-8.8`; that download is not
committed.

Run the CLI:

```sh
java -jar target/javaspec-0.1.0-SNAPSHOT.jar --help
```

Short form used in the examples below:

```sh
javaspec='java -jar target/javaspec-0.1.0-SNAPSHOT.jar'
```

## Release and CI verification

Use `scripts/verify-all.sh` as the local aggregate verification path. It covers the zero-runtime-dependency core, version alignment, standalone Maven/Gradle/JUnit Platform adapters, runtime dependency audits, and standalone examples. The repository is intentionally not a Maven multi-module build: root `mvn verify` continues to verify and audit only the core artifact, while optional adapters and examples remain standalone artifacts.

Run the version-alignment check from the repository root:

```sh
scripts/check-version-alignment.sh
```

The version-alignment script checks the root Maven version, standalone Maven plugin version,
standalone JUnit Platform engine version, Gradle plugin `version`, and Gradle plugin
`javaspecCoreVersion` against one baseline.

Run the aggregate local release check from the repository root, or invoke the script by its
full/relative path from elsewhere:

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
7. standalone Gradle plugin `clean test build` and `runtimeClasspath` audit unless explicitly
   skipped;
8. standalone examples verification through `scripts/verify-examples.sh` unless explicitly skipped.

Environment variables:

| Variable | Meaning |
|---|---|
| `MAVEN_BIN` | Maven executable to use; defaults to `mvn`. |
| `JAVASPEC_GRADLE_BIN` | Explicit Gradle executable to use for the standalone Gradle plugin checks. |
| `JAVASPEC_SKIP_GRADLE=1` | Explicitly skip Gradle adapter verification. Use only when that skip is intentional. |
| `JAVASPEC_SKIP_EXAMPLES=1` | Explicitly skip the standalone examples section in `scripts/verify-all.sh`. |
| `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` | Explicitly skip only the Gradle example inside `scripts/verify-examples.sh`. |

When `JAVASPEC_GRADLE_BIN` is not set and Gradle is not skipped, resolution order is repository
`./gradlew` if executable, `/tmp/gradle-8.8/bin/gradle`, then `gradle` on `PATH`. If none is found,
the script fails with a clear diagnostic.

Release-readiness documentation and local packaging checks:

- `CHANGELOG.md` records notable changes and the current unreleased release-readiness scaffold.
- `RELEASING.md` is a checklist, not a publishing script.
- Maven `release-artifacts` profiles on the root, Maven plugin, and JUnit Platform engine builds
  create local sources and javadocs only.
- The standalone Gradle plugin build is prepared to produce source and javadoc jars.
- Safe URL, SCM, GitHub Issues, MIT license, and maintainer/developer metadata are present; the
  confirmed maintainer is `Mario Giustiniani <mariogiustiniani@gmail.com>`.

Optional local artifact checks:

```sh
mvn -q -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-maven-plugin/pom.xml -Prelease-artifacts -DskipTests package
mvn -q -f javaspec-junit-platform-engine/pom.xml -Prelease-artifacts -DskipTests package
gradle -p javaspec-gradle-plugin clean test build
```

These checks do not sign, stage, deploy, or publish artifacts. The MIT license and maintainer
metadata are resolved. Artifacts are published on Maven Central under `io.github.jvmspec`. The
Gradle plugin is published on the Gradle Plugin Portal with plugin id `io.github.jvmspec`. Add the
dependency `io.github.jvmspec:javaspec:0.1.0` (test scope).

The GitHub Actions workflow at `.github/workflows/ci.yml` triggers on `push`, `pull_request`, and
`workflow_dispatch`. It defines a core job matrix over Java 8, 11, 17, 21, and 25 using Temurin and
Maven cache, running root `mvn -q verify` plus the root runtime dependency audit. It also defines a
Java 21 `full-verification` job with Maven cache and Gradle 8.8 setup that runs
`scripts/verify-all.sh` with `JAVASPEC_GRADLE_BIN=gradle`. The workflow performs no publishing and
uses no secrets.

Remote CI status should be checked against the current branch/commit in GitHub Actions. This manual does not treat older phase-specific commit confirmations as current release evidence. The workflow scope remains the configured Java 8/11/17/21/25 core matrix and Java 21 full-verification job through `scripts/verify-all.sh`, which includes standalone examples by default unless explicitly skipped.

## Standalone examples and report schema

Standalone consumer examples are not root Maven modules and are not part of repository-root
`mvn verify` by themselves.

- **`examples/maven-basic/`**
  - Demonstrates: `javaspec-maven-plugin` in a consuming Maven project
  - Report output: `target/javaspec/run-report.json`, `target/javaspec/junit-report.xml`
- **`examples/gradle-basic/`**
  - Demonstrates: Gradle plugin id `io.github.jvmspec` through an included plugin build
  - Report output: `build/reports/javaspec/run-report.json`,
    `build/reports/javaspec/junit-report.xml`
- **`examples/junit-platform-basic/`**
  - Demonstrates: Standalone JUnit Platform engine with Maven Surefire configured for `*Spec`
  - Report output: `target/surefire-reports/`
- **`examples/bytecode-doubles-basic/`**
  - Demonstrates: Optional `javaspec-bytecode-doubles` adapter for non-final concrete-class doubles
  - Report output: `target/javaspec/run-report.json`, `target/javaspec/junit-report.xml`

Run all examples locally with:

```sh
scripts/verify-examples.sh
```

The script installs local snapshots for the core, Maven plugin, JUnit Platform engine, and bytecode
doubles adapter before running examples. Set `MAVEN_BIN` to choose Maven, `JAVASPEC_GRADLE_BIN` to
choose Gradle, `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` to skip only the Gradle example, or
`JAVASPEC_SKIP_BYTECODE_DOUBLES_EXAMPLE=1` to skip only the bytecode doubles example.
`scripts/verify-all.sh` runs this examples check by default unless `JAVASPEC_SKIP_EXAMPLES=1` is
set.

Report documentation assets:

- JSON schema: [`../schemas/run-report-v1.schema.json`](../schemas/run-report-v1.schema.json)
- Golden JSON report:
  [`../examples/reports/passing-run-report-v1.json`](../examples/reports/passing-run-report-v1.json)
- Golden JUnit XML-compatible report:
  [`../examples/reports/passing-junit-report.xml`](../examples/reports/passing-junit-report.xml)
- Pending JSON report:
  [`../examples/reports/pending-run-report-v1.json`](../examples/reports/pending-run-report-v1.json)
- Pending JUnit XML-compatible report:
  [`../examples/reports/pending-junit-report.xml`](../examples/reports/pending-junit-report.xml)

The golden examples show schemaVersion 1, additive Phase 35 run metadata/properties with
deterministic timestamp `2026-01-01T00:00:00Z` and hostname `ci.example.local`, stable id
`spec.com.example.CalculatorSpec#it_adds_two_numbers`, `PASSED` status, pending counts, a concise
PENDING JSON example, a JUnit XML-compatible pending `<skipped message="Pending: ...">` example, and
source line metadata.

## Commands

```sh
$javaspec describe <ClassName> [--config <file>] [--suite <name>] [--spec-dir <dir>]
$javaspec desc <ClassName> [--config <file>] [--suite <name>] [--spec-root <dir>]
$javaspec run \
  [--config <file>] [--suite <name>] [--spec-dir <dir>] [--source-dir <dir>] \
  [--classpath <path-list>] [--classpath-file <file>] \
  [--resolve-pom <pom.xml>] \
  [--compile] [--compile-output <dir>] [--release <N>] [--generate] [--dry-run] \
  [--stop-on-failure] [--formatter <progress|pretty|custom>] \
  [--profile <java8|java11|java17|java21|java25>] [--verbose] \
  [--report <file>] [--report-file <file>] \
  [--junit-xml <file>] [--junit-xml-file <file>] \
  [--constructor-policy <delete|preserve|comment>] [--class <name>] [--example <name>]
$javaspec list-extensions
$javaspec prophesize <ClassName> [--package <pkg>] [--output <dir>] [--overwrite]
```

`prophesize` generates a typed `*Prophecy` wrapper for an interface or concrete class under
`target/generated-sources/javaspec` by default. This generated wrapper is the recommended
PHPSpec-like collaborator syntax: Java 8 specs can declare
`MailerProphecy mailer = prophesizeMailer();`, and Java 10+ specs can write
`var mailer = prophesizeMailer();`; both keep typed calls such as
`mailer.send(...).willReturn(...).shouldBeCalled()`. During `run --generate`, javaspec can also
update the generated `*SpecSupport` class with helper methods such as `prophesizeMailer()` /
`prophecyMailer()`.


Aliases and defaults:

- **`describe`**
  - Alias: `desc`
  - Default: n/a
  - Command: n/a
- **`--config <file>`**
  - Alias: n/a
  - Default: inferred defaults
  - Command: `describe`, `run`
- **`--suite <name>`**
  - Alias: n/a
  - Default: configuration default suite (`default` with inferred defaults)
  - Command: `describe`, `run`
- **`--spec-dir`**
  - Alias: `--spec-root`
  - Default: selected suite `specDir` (`src/test/java` with inferred defaults)
  - Command: `describe`, `run`
- **`--source-dir`**
  - Alias: `--source-root`
  - Default: selected suite `sourceDir` (`src/main/java` with inferred defaults)
  - Command: `run`
- **`--classpath <path-list>`**
  - Alias: n/a
  - Default: no explicit entries
  - Command: `run`
- **`--classpath-file <file>`**
  - Alias: n/a
  - Default: no explicit entries
  - Command: `run`
- **`--compile`**
  - Alias: n/a
  - Default: `false`
  - Command: `run`
- **`--compile-output <dir>`**
  - Alias: n/a
  - Default: `target/javaspec-classes`; implies `--compile`
  - Command: `run`
- **`--generate`**
  - Alias: n/a
  - Default: `false`
  - Command: `run`
- **`--dry-run`**
  - Alias: n/a
  - Default: `false`
  - Command: `run`
- **`--stop-on-failure`**
  - Alias: n/a
  - Default: `false`
  - Command: `run`
- **`--formatter <progress\|pretty\|custom>`**
  - Alias: n/a
  - Default: configuration `formatter` (`progress` with inferred defaults)
  - Command: `run`
- **`--profile <java8\|java11\|java17\|java21\|java25>`**
  - Alias: n/a
  - Default: configuration `profile` (`java8` with inferred defaults)
  - Command: `run`
- **`--verbose`**
  - Alias: n/a
  - Default: `false`
  - Command: `run`
- **`--report <file>`**
  - Alias: `--report-file <file>`
  - Default: config JSON report destination, otherwise no JSON report
  - Command: `run`
- **`--junit-xml <file>`**
  - Alias: `--junit-xml-file <file>`
  - Default: config JUnit XML-compatible destination, otherwise no JUnit XML-compatible report
  - Command: `run`
- **`--constructor-policy <delete\|preserve\|comment>`**
  - Alias: n/a
  - Default: configuration `constructorPolicy` (`comment` with inferred defaults)
  - Command: `run`
- **`--resolve-pom <pom.xml>`**
  - Alias: n/a
  - Default: no POM resolution
  - Command: `run`
  - Notes: resolves runtime-scope dependencies from the given POM via the built-in
    `LocalMavenRepoResolver` (offline, `~/.m2/repository`) or any `DependencyResolver` provider
    registered via `ServiceLoader`.  Resolved JARs are prepended to the run classpath.  Missing
    artifacts are skipped gracefully.  Test, provided, system, and optional dependencies are excluded.
- **`--compile`**
  - Alias: n/a
  - Default: `false`
  - Command: `run`
- **`--compile-output <dir>`**
  - Alias: n/a
  - Default: `target/javaspec-classes`; implies `--compile`
  - Command: `run`
- **`--release <N>`**
  - Alias: n/a
  - Default: no release option passed to javac
  - Command: `run` (requires `--compile` or `--compile-output`)
  - Notes: passes `--release N` on Java 9+ or `-source N -target N` on Java 8.
    Compilation results are cached keyed by source timestamps, classpath, and options;
    unchanged inputs skip recompilation.
- **`--class <name>`**
  - Alias: n/a
  - Default: no class filter
  - Command: `run`
- **`--example <name>`**
  - Alias: n/a
  - Default: no example filter
  - Command: `run`
- **`list-extensions` command**
  - Alias: n/a
  - Default: n/a
  - Notes: prints all `RunFormatter` and `JavaspecExtension` providers visible on the current
    classpath (built-in formatters plus `ServiceLoader`-discovered providers), then prints
    classpath repair hints (`--classpath`, `--classpath-file`, `--resolve-pom`) for adding
    missing extensions.  Always exits 0.

`describe` writes specification files only. Production source generation, updates, bootstrap
execution, example execution, formatting, classpath selection, profile enforcement, and report
writing belong to `run`. After discovery/profile enforcement/generation/update completes without
declined prompts, `run` performs optional CLI compilation when requested, executes configured
bootstrap hooks, and then invokes the reflection runner for discovered examples whose compiled
hook/spec classes are available on the effective, selected explicit, or compile-output-first
classloader. `describe` rejects command-line `--source-dir`/`--source-root` and all run-only
controls (`--classpath`, `--classpath-file`, `--compile`, `--compile-output`, `--generate`,
`--dry-run`, `--stop-on-failure`, `--formatter`, `--profile`, `--verbose`, `--report`,
`--report-file`, `--junit-xml`, `--junit-xml-file`, `--constructor-policy`, `--class`, and
`--example`); a `sourceDir`, `profile`, bootstrap hook, or report destination present in a selected
config file is accepted because `describe` ignores source roots, does not enforce profiles, does not
execute hooks, and does not write reports.

## Configuration files

`describe` and `run` accept `--config <file>` and `--suite <name>`. When no config file is supplied,
javaspec uses `JavaspecConfiguration.defaults()`.

### Syntax

The configuration format is intentionally restricted and line-based so the runtime stays
dependency-free:

- Blank lines and lines whose first non-whitespace character is `#` are ignored.
- Key/value separator is either `=` or `:`.
- There is no YAML, TOML, or JSON parser dependency.
- Top-level keys are `profile`, `formatter`, `constructorPolicy`/`constructor-policy`,
  `defaultSuite`/`default-suite`, `bootstrap`, extension activation aliases
  `extensions`/`extension`, JSON report destination aliases (`report`, `reportFile`, `report-file`,
  `jsonReport`, `jsonReportFile`, `json-report-file`), and JUnit XML-compatible report destination
  aliases (`junitXml`, `junit-xml`, `junitXmlFile`, `junit-xml-file`, `junitXmlReportFile`,
  `junit-xml-report-file`). There are no configuration keys for CLI compilation; use `run --compile`
  or `--compile-output <dir>` per run.
- Valid `profile` values are `java8`, `java11`, `java17`, `java21`, and `java25`; `run` enforces the
  effective profile before generation/update writes. Built-in `formatter` values are `progress` and
  `pretty`; ServiceLoader-discovered formatter names are also valid at run time when their providers
  are on the effective run classloader; valid constructor policies are `delete`, `preserve`, and
  `comment`.
- Suite keys use `suite.<name>.<property>` with properties `specDir`/`spec-dir`,
  `sourceDir`/`source-dir`, `specPackagePrefix`/`spec-package-prefix`,
  `packagePrefix`/`package-prefix`, `bootstrap`, and extension activation aliases
  `extensions`/`extension`.
- `bootstrap` and extension activation values are comma-separated fully qualified class names.
  Explicit bootstrap hooks execute during `run` when compiled hook classes are available on the
  effective run classloader/classpath; configured extensions activate discovered `JavaspecExtension`
  implementations from the effective classloader.
- Report destination values are trimmed, must be non-blank when present, and default to absent/null
  when no alias is configured.
- `specPackagePrefix` and `packagePrefix` drive naming conventions for `describe`, `run`, discovery,
  and spec/support generation. `packagePrefix` may be empty. Other configured values, including
  bootstrap entries, extension entries, and report destination values when their keys are present,
  must not be blank.

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
| JSON report destination | absent/null |
| JUnit XML-compatible report destination | absent/null |
| Bootstrap hooks | empty |
| Extensions | empty |

### Example config

```properties
# javaspec.conf
profile = java17
formatter = progress
constructorPolicy = preserve
defaultSuite = domain
jsonReportFile = target/javaspec-report.json
junitXmlReportFile = target/javaspec-report.xml
bootstrap = org.example.SpecBootstrap
extensions = org.example.SpecExtension

suite.domain.specDir = src/test/java
suite.domain.sourceDir = src/main/java
suite.domain.specPackagePrefix = spec
suite.domain.packagePrefix = org.example
suite.domain.bootstrap = org.example.DomainBootstrap, org.example.TestDataBootstrap
suite.domain.extensions = org.example.DomainSpecExtension

suite.integration.spec-dir: src/integrationSpec/java
suite.integration.source-dir: src/main/java
suite.integration.spec-package-prefix: spec
suite.integration.package-prefix:
```

### CLI precedence and examples

1. `--config <file>` loads explicit configuration; without it, defaults are inferred.
2. `--suite <name>` selects a configured suite; without it, `defaultSuite` is used.
3. The selected suite's `specDir`, `sourceDir`, `specPackagePrefix`, and `packagePrefix` are used.
   Command-line `--spec-dir`/`--spec-root` or `--source-dir`/`--source-root` override paths only;
   naming still comes from the selected suite.
4. `run` uses the configured `constructorPolicy`, `profile`, and `formatter` unless overridden by
   command-line `--constructor-policy`, `--profile`, or `--formatter`; CLI `--profile` takes
   precedence over config profile before enforcement, and formatter validity is checked against
   built-in plus ServiceLoader-discovered names from the effective run classloader.
5. `run` combines top-level `bootstrap` hooks before selected-suite `bootstrap` hooks, then
   ServiceLoader-discovered `BootstrapHook` providers, preserving explicit declaration order and
   duplicates and using deterministic discovery order. There is no CLI override for bootstrap hooks.
6. `run` uses configured JSON/JUnit XML-compatible report destinations when no corresponding CLI
   report option is supplied; `--report`/`--report-file` and `--junit-xml`/`--junit-xml-file`
   override config values.
7. `run` applies repeatable `--class <name>` and `--example <name>` filters after suite selection;
   the filtered discovered specs are passed to bootstrap hooks and to the runner.
8. `run --compile`, `--compile-output <dir>`, `--dry-run`, `--stop-on-failure`, and `--verbose` are
   command-line controls only. CLI compilation still has no config keys; programmatic/Maven/Gradle
   compilation opt-in uses their own APIs or build-tool settings.
9. `describe` accepts `--config` and `--suite` for spec-root and naming selection, rejects
   command-line `--source-dir`, command-line `--profile`, and command-line report options, ignores
   any `sourceDir` or `profile` value loaded from config, and accepts but ignores configured
   bootstrap hooks and report destinations because it does not enforce profiles, execute hooks, or
   write reports.

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

Invalid configuration exits as command-line usage error (`64`) and starts with `Error: Invalid
configuration:`. Diagnostics include line numbers for parser errors where available. Examples
include duplicate keys, unknown keys, malformed lines without `=` or `:`, blank required values,
invalid profiles, invalid constructor policies, and selecting an unconfigured suite. Formatter names
are validated during `run` after built-in and ServiceLoader-provided formatters are known.

Examples:

```text
Error: Invalid configuration: Line 3: Invalid constructor policy: keep. Valid values: delete, preserve, comment.
Error: Invalid configuration: Suite 'api' is not configured. Available suites: default, domain.
```

A missing or unreadable config file exits with I/O error (`70`) and prints the config path.

### Current configuration limitations

- Bootstrap hooks can be explicit compiled Java class names from configuration and can also be
  discovered as `BootstrapHook` ServiceLoader providers after explicit hooks. javaspec still does
  not run scripts, scan packages, resolve dependencies, or compile hooks/specs/sources from
  configuration. CLI source/spec compilation is available through run-time `--compile` /
  `--compile-output <dir>` options; programmatic/Maven/Gradle opt-in compilation uses separate
  APIs/settings.
- Profile enforcement is conservative and source/generation-scoped: it covers described type kinds,
  generated method return/parameter API owners, and relationship type references such as
  super/extends/implements/permits where they resolve to the profile catalog, but it ignores unknown
  project types and ambiguous or unresolvable names to avoid false positives.
- `formatter` is selected from config, CLI/build-tool/JUnit Platform settings, or project properties
  where supported, and controls the built-in `progress`/`pretty` output or a
  ServiceLoader-discovered formatter name when the provider is on the effective run classloader.
  Configured `extensions` / `extension` values activate discovered extension classes before
  formatter lookup. A formatter value fails with a diagnostic if no provider with that name is
  discovered.
- Package-prefix naming is implemented for describe/run discovery, generation, bootstrap context,
  reflection execution, JSON report contents, and JUnit XML-compatible report test case class
  names; Phase 18 adds stable ids and source file/line metadata where available.
- The runner lifecycle is intentionally small: optional current-JDK compilation where that entry
  point supports it, then explicit and discovered bootstrap hooks before examples, then a fresh spec
  instance per executable example plus optional public no-arg `let()` and `letGo()`. Explicit
  skipped/pending examples are implemented through annotations or runtime signals.

## Bootstrap hooks

Configured `bootstrap` entries are executable hook class names for `run`; Phase 33 also discovers
`io.github.jvmspec.bootstrap.BootstrapHook` providers with JDK `ServiceLoader` after explicit hooks.

Requirements for each hook class:

- It must implement `io.github.jvmspec.bootstrap.BootstrapHook`.
- It must declare a public no-argument constructor.
- It must be compiled and loadable from the effective run classloader/classpath: the CLI process
  classloader or explicit `--classpath` / `--classpath-file` classloader, Maven's test classpath,
  Gradle's run classpath, or a programmatic invocation classloader.

Hook order is deterministic:

1. Top-level `bootstrap` entries run first.
2. The selected suite's `suite.<name>.bootstrap` entries run next.
3. Declaration order is preserved for explicit hooks.
4. Duplicate explicit hook class names are preserved and execute each time they appear.
5. ServiceLoader-discovered `BootstrapHook` providers execute afterward in deterministic provider
   order.

Hooks receive immutable `io.github.jvmspec.bootstrap.BootstrapContext`. The context exposes the run
classloader and the discovered specs selected by suite/class/example filters.

```java
import io.github.jvmspec.bootstrap.BootstrapContext;
import io.github.jvmspec.bootstrap.BootstrapHook;

public final class SpecBootstrap implements BootstrapHook {
    public SpecBootstrap() {
    }

    @Override
    public void bootstrap(BootstrapContext context) throws Exception {
        ClassLoader runClassLoader = context.classLoader();
        int selectedSpecCount = context.discoveredSpecs().size();
        // Prepare test data or environment state here.
    }
}
```

CLI `javaspec run --config <file>` executes hooks after discovery, profile enforcement,
generation/update decisions, any successful generation/update work, and any requested successful CLI
compilation, but immediately before examples and before JSON/JUnit XML-compatible reports. If no
specs are discovered, CLI `run` does not execute hooks. A bootstrap failure starts with `Error:
Bootstrap execution failed`, exits `64`, and writes no reports.

Maven `javaspec:run` and Gradle `javaspecRun` pass top-level plus selected-suite hooks into the
canonical `JavaspecInvocation` / `JavaspecLauncher` path. Bootstrap failures fail the Maven build or
Gradle task with clear `javaspec bootstrap execution failed` diagnostics.

Bootstrap execution does not add script engines, package scanning, dependency resolution, or runtime
dependencies; ServiceLoader discovery is classloader-scoped and runs after explicit configured
hooks. The core remains Java 8-compatible and zero-runtime-dependency.

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

Class filters match described qualified names, described simple names, spec qualified names, or spec
simple names exactly. Example filters match public `void` example methods named `it_*` or `its_*` by
method name, display name (underscores replaced by spaces), or source-order index. Filters affect
discovery/generation selection and reflection execution because the runner uses the same
`DiscoveredSpec`/`SpecExample` metadata.

## Run controls

### Dry-run planning

`javaspec run --dry-run` performs discovery and profile enforcement before planning without writing
files, prompting, or compiling. It reports actions that would be generated or updated, including
related specs/support, support updates, constructor changes, method bodies/declarations/elements,
and missing production type generation. If `--compile` or `--compile-output` is also supplied,
dry-run keeps the workspace non-mutating and skips compilation/output-directory creation.

Dry-run exit behavior:

| Condition | Exit code |
|---|---:|
| Profile compatibility violation exists | `64` |
| Bootstrap execution fails before examples | `64` |
| Pending generation/update work exists | `1` |
| No pending generation/update work and examples pass or are skipped/pending-only | `0` |
| No pending generation/update work but executable examples fail or break | `1` |

`--dry-run` is useful in CI when pending generated work should fail the build without modifying the
workspace. If profile enforcement fails, dry-run exits before any file or report writes. If dry-run
reaches example execution and a configured bootstrap hook fails, it exits before reports.

### Stop on failure

By default, `javaspec run` processes all discovered example metadata. With `--stop-on-failure`, the
reflection runner stops after the first FAILED or BROKEN executable example. Skipped examples before
that point are still reported; examples after the first failure/break are not executed.

### Explicit classpath runs

`javaspec run` can execute specs from an explicit compiled-class classpath without requiring JUnit:

```sh
$javaspec run --classpath target/classes:target/test-classes
$javaspec run --classpath-file target/javaspec-classpath.txt
```

`--classpath <path-list>` reads entries separated by Java's `File.pathSeparator`: `:` on Unix-like
systems and `;` on Windows. Empty entries are ignored after trimming.

`--classpath-file <file>` reads UTF-8 entries from a file, one entry per non-empty line. Lines whose
trimmed form begins with `#` are comments and are ignored:

```text
# target/javaspec-classpath.txt
target/classes
target/test-classes
/path/to/dependency.jar
```

When explicit entries are supplied, javaspec builds a selected classloader over those entries and
uses it for production type existence checks, bootstrap hook loading/execution, and spec execution.
`--verbose` lists the explicit entries under `Explicit classpath entries:`. Without `--compile`, the
entries must point to already compiled classes or archives. With `--compile`, explicit entries are
dependency classpath entries for the compiler and execution classloader; the compile output
directory is placed before them after successful compilation.

If source discovery finds specs/examples but the runner classloader cannot load the compiled spec
class, a dependency, or the expected public no-argument example method, the CLI prints an `Execution
diagnostics:` block only when those availability issues exist. With explicit classpath input it
reports the explicit entry count; without explicit entries it reports that the current process
classloader was used and suggests `--classpath`/`--classpath-file` or opt-in `--compile` for CLI
source-only runs. These diagnostics exclude explicit `@Skip` and `PENDING` results and do not change
exit-code semantics.

Both classpath options belong to `run` only and are rejected by `describe`/`desc`.

### Opt-in CLI source/spec compilation

`javaspec run --compile` enables a CLI-only compilation step before executable examples:

```sh
$javaspec run --compile
$javaspec run --compile-output target/javaspec-classes
$javaspec run --compile --release 8
$javaspec run --compile --resolve-pom pom.xml
$javaspec run --classpath lib/dependency.jar --compile --compile-output target/javaspec-classes
```

`--compile-output <dir>` selects the output directory and implies `--compile`; the default output
directory is `target/javaspec-classes`. `--release <N>` requests a javac release target when the
current compiler supports it. `--resolve-pom <pom.xml>` adds dependencies resolvable from the local
Maven repository to the run/compile classpath. These options are run-only and are rejected by
`describe`/`desc`.

The compiler is the current JDK `javax.tools.JavaCompiler` obtained from
`ToolProvider.getSystemJavaCompiler()`. javaspec does not fork `javac` or add compiler dependencies.
It does keep an incremental cache for unchanged source inputs. If the compiler API is unavailable,
the run prints a compiler-unavailable diagnostic and exits `64`.

When specs are discovered and the run is not dry-run, compilation happens after discovery, profile
enforcement, related-spec generation, support/source generation or updates, and prompts/`--generate`
decisions, but before bootstrap hooks and example execution. The input set is all `.java` files
under the effective source root and effective spec root, de-duplicated by normalized path. No-spec
runs skip compilation. Dry-run remains non-mutating and skips compilation even when `--compile` or
`--compile-output` is present.

Compiler classpath order is deterministic: compile output directory first, explicit CLI classpath
entries from `--classpath` / `--classpath-file` second, and the current process `java.class.path`
last. After successful compilation of at least one source file, execution uses a classloader whose
URL entries place the compile output directory before the explicit CLI entries; the current process
classloader remains the parent.

A compilation failure exits `1`, prints `Compilation failed:` followed by compiler diagnostics,
skips bootstrap/example execution, and writes no JSON or JUnit XML-compatible reports. There are no
config keys, Maven/Gradle/JUnit adapter changes, report schema changes, dependency resolver,
incremental cache, or source-level/release-management changes.

### Formatters and extension contracts

`--formatter progress` is concise and summary-oriented. It is the default when neither config nor
CLI selects a formatter.

`--formatter pretty` prints per-example status lines plus details for failed, broken, skipped, or
pending examples.

Built-in and external output is rendered through the public zero-dependency
`io.github.jvmspec.formatter.RunFormatter` contract and deterministic `RunFormatterRegistry`. Built-in
names are `progress` and `pretty`. Phase 25 adds JDK `ServiceLoader` discovery through
`io.github.jvmspec.extension.JavaspecExtensionLoader.loadRunFormatterRegistry()` and
`loadRunFormatterRegistry(ClassLoader)`; compatibility aliases such as `loadRunFormatters(...)` may
also exist. The registry contains built-ins first, then providers discovered from the effective
classloader.

Supported service types are:

- `io.github.jvmspec.formatter.RunFormatter` providers, registered by `RunFormatter.name()`.
- `io.github.jvmspec.extension.JavaspecExtension` providers, configured with an `ExtensionContext` that
  exposes `context.runFormatterRegistry()` / `context.runFormatters()`.
- `io.github.jvmspec.extension.Extension`, a short-name alias service type for extension providers.

Service files live under `META-INF/services/` in the provider jar:

```text
# META-INF/services/io.github.jvmspec.formatter.RunFormatter
com.example.javaspec.MarkdownRunFormatter

# META-INF/services/io.github.jvmspec.extension.JavaspecExtension
com.example.javaspec.MarkdownExtension

# alias service type also supported:
# META-INF/services/io.github.jvmspec.extension.Extension
com.example.javaspec.MarkdownExtension
```

Minimal provider examples:

```java
package com.example.javaspec;

import java.io.PrintStream;
import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.runner.RunResult;

public final class MarkdownRunFormatter implements RunFormatter {
    @Override
    public String name() {
        return "markdown";
    }

    @Override
    public void format(RunResult result, PrintStream out) {
        out.println("# javaspec");
        out.println("Total examples: " + result.totalCount());
    }
}
```

```java
package com.example.javaspec;

import io.github.jvmspec.extension.ExtensionContext;
import io.github.jvmspec.extension.JavaspecExtension;

public final class MarkdownExtension implements JavaspecExtension {
    @Override
    public void configure(ExtensionContext context) {
        context.runFormatters().register(new MarkdownRunFormatter());
    }
}
```

If the same extension implementation is listed under both extension service types, javaspec
configures it once per registry load. Invalid service declarations, unloadable providers, invalid
formatter names, and extension configuration failures raise `ExtensionLoadingException` with
service/provider diagnostics.

CLI behavior:

1. `javaspec run` selects the effective classloader first. Without explicit classpath entries, that
   is the current process classloader; with `--classpath` / `--classpath-file`, it is the selected
   URL classloader over those entries.
2. javaspec loads built-in formatters and ServiceLoader providers from that classloader.
3. CLI `--formatter <name>` overrides config `formatter=<name>`; otherwise config is used, then
   default `progress`.
4. Invalid formatter diagnostics list all discovered names, including external names.

Examples:

```sh
$javaspec run --classpath target/classes:target/test-classes:lib/javaspec-markdown-formatter.jar --formatter markdown
$javaspec run --classpath-file target/javaspec-classpath.txt --config javaspec.conf # may use formatter = markdown
```

This phase does not add report schema/content changes, integrated source/spec compilation, new
runtime dependencies, publishing changes, package scanning, plugin repository lookup, or Maven/JUnit
Platform formatter output controls.

### JSON run reports

`--report <file>` writes a UTF-8 JSON runner report after normal no-spec output or runner summary
rendering. `--report-file <file>` is an alias. A config file can provide the same destination
default with `report`, `reportFile`, `report-file`, `jsonReport`, `jsonReportFile`, or
`json-report-file`. Values are trimmed, must be non-blank when present, and default to absent/null.
JSON report writing is available only for `run`; `describe`/`desc` rejects command-line report
options because it does not execute examples, while `describe --config <file>` accepts config files
containing report destinations and ignores them.

```sh
$javaspec run --report target/javaspec-report.json
$javaspec run --report-file target/javaspec-report.json --verbose
$javaspec run --config javaspec.conf # writes the configured JSON report if jsonReportFile/report aliases are present
```

Programmatic hosts can use `io.github.jvmspec.reporting.ReportMetadata` with the Phase 35
`RunReportWriter` overloads when they need deterministic metadata; existing writer methods use
`ReportMetadata.current()`.

The report schema is versioned with `"schemaVersion": 1`. Phase 18 adds identifier/source fields
additively while preserving the existing fields, Phase 22 adds `pending` summary counts plus
`PENDING` example statuses, and Phase 35 default writers emit additive run-level `metadata`. JSON
remains schemaVersion 1; the metadata object is optional so older schemaVersion 1 reports without
metadata still validate. The schema is documented at
[`../schemas/run-report-v1.schema.json`](../schemas/run-report-v1.schema.json), and golden passing
and pending JSON/JUnit XML-compatible reports are available under
[`../examples/reports/`](../examples/reports/). The top-level object contains:

- `metadata` (optional): Phase 35 run-level metadata with required `timestamp`, `hostname`, `time`,
  and `properties` fields when present. `timestamp` and `hostname` are non-empty strings, `time` is
  a non-negative number, and `properties` is an object with non-blank names and string values such
  as `javaspec.report.schemaVersion=1` and `javaspec.report.tool=javaspec`.
- `summary`: total, passed, failed, broken, skipped, pending, and successful counts for the whole
  run. The Phase 22 writer emits `pending` for all summaries; the schema keeps it optional for
  additive compatibility with older schemaVersion 1 reports.
- `specs`: one entry per discovered spec result, with the spec name, `id`, `stableId`, optional
  `sourceFile`, executable flag, not-executable reason, per-spec summary, and examples.
- `examples`: spec name, method, display name, source-order index, status, detail, `id`, `stableId`,
  `fullName`, optional `source { file, line }`, and `failure`.
- `failure`: `null` when no throwable was captured; otherwise throwable class name, message, and
  stack trace lines.

Stable example ids use `<specQualifiedName>#<methodName>`, matching `ExampleResult.fullName()`.

A no-spec run with an effective JSON report destination writes a valid empty report with zero
summary counts and an empty `specs` array; configured bootstrap hooks are not executed on CLI
no-spec runs. Passing, failing, broken, skipped-only, and pending-only runs write the report after
summary rendering. Failed or broken executable examples still exit `1` after the report is written.
Dry-run with pending generation/update work exits before execution and does not write a report;
profile compatibility violations, opt-in CLI compilation failures, and bootstrap execution failures
also exit before report writing. Report write failures print I/O diagnostics, include the report
path, and exit `70`. JSON and JUnit XML-compatible reports can be requested together from CLI
options, config destinations, or a mix of both; CLI options override config values.

Minimal empty report example:

```json
{
  "schemaVersion": 1,
  "metadata": {
    "timestamp": "2026-01-01T00:00:00Z",
    "hostname": "ci.example.local",
    "time": 0,
    "properties": {
      "javaspec.report.schemaVersion": "1",
      "javaspec.report.tool": "javaspec"
    }
  },
  "summary": {
    "total": 0,
    "passed": 0,
    "failed": 0,
    "broken": 0,
    "skipped": 0,
    "pending": 0,
    "successful": true
  },
  "specs": []
}
```

### JUnit XML reports

`--junit-xml <file>` writes a UTF-8 JUnit XML-compatible runner report after normal no-spec output
or runner summary rendering. `--junit-xml-file <file>` is an alias. A config file can provide the
same destination default with `junitXml`, `junit-xml`, `junitXmlFile`, `junit-xml-file`,
`junitXmlReportFile`, or `junit-xml-report-file`. Values are trimmed, must be non-blank when
present, and default to absent/null. The writer is dependency-free and does not require JUnit or
JUnit Platform. JUnit XML-compatible report writing is available only for `run`; `describe`/`desc`
rejects command-line report options while accepting and ignoring configured report destinations.

```sh
$javaspec run --junit-xml target/javaspec-report.xml
$javaspec run --report target/javaspec-report.json --junit-xml target/javaspec-report.xml
$javaspec run --config javaspec.conf # writes the configured JUnit XML report if junitXmlReportFile aliases are present
```

Programmatic hosts can use `io.github.jvmspec.reporting.ReportMetadata` with the Phase 35
`JUnitXmlReportWriter` overloads when they need deterministic metadata; existing writer methods use
`ReportMetadata.current()`.

The report uses a single `<testsuite name="javaspec">` element with `tests`, `failures`, `errors`,
`skipped`, Phase 35 `timestamp` and `hostname` metadata, and run-level `time` attributes. The Phase
35 default writer places a `<properties>` block immediately under the testsuite with string
properties such as `javaspec.report.schemaVersion=1` and `javaspec.report.tool=javaspec`. Each
example becomes a `<testcase>` whose `classname` is the spec qualified name and whose `name` is the
example method name; testcase `time="0"`, `file`, and `line` attributes are included as before when
source data is available. `FAILED` examples become `<failure>`, `BROKEN` examples become `<error>`,
and both `SKIPPED` and `PENDING` examples become `<skipped>`. The testsuite `skipped` attribute
includes skipped plus pending; pending messages use `Pending: <reason>` or `Pending by javaspec.`. A
no-spec run writes a valid empty test suite.

Minimal empty JUnit XML report example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="javaspec" tests="0" failures="0" errors="0" skipped="0" timestamp="2026-01-01T00:00:00Z" hostname="ci.example.local" time="0">
  <properties>
    <property name="javaspec.report.schemaVersion" value="1"/>
    <property name="javaspec.report.tool" value="javaspec"/>
  </properties>
</testsuite>
```

Passing, failing, broken, skipped-only, pending-only, and no-spec runs write requested reports after
normal output; configured bootstrap hooks are not executed on CLI no-spec runs. Failed or broken
executable examples still exit `1` after the report is written. Dry-run with pending
generation/update work exits before execution and does not write JUnit XML; profile compatibility
violations, opt-in CLI compilation failures, and bootstrap execution failures also exit before
report writing. Report write failures print I/O diagnostics, include the JUnit XML path, and exit
`70`. CLI `--junit-xml` / `--junit-xml-file` overrides any configured JUnit XML-compatible
destination.

### Profile selection and enforcement

`--profile <java8|java11|java17|java21|java25>` selects the active target profile for `run`. A valid
CLI profile overrides the configured `profile` value; without either, the default is `java8`. The
selected profile is visible in verbose output.

After discovery and before any related-spec generation, support updates, production type skeleton
writes, constructor/method updates, prompts, execution, or report writing, `run` enforces the
effective profile. Enforcement rejects described kinds that require a newer source level: `record`,
`sealed class`, and `sealed interface` require a Java 17-compatible target profile, while Java
8-compatible kinds pass under `java8`.

Generated method signatures are checked conservatively against the profile catalog. Return and
parameter types that resolve to known Java API owners introduced in Java 21 or Java 25, for example,
are rejected for lower profiles. Unknown project types, unknown catalog owners, malformed or
unresolvable type strings, and simple names that are ambiguous in the catalog are ignored to avoid
false positives.

Examples:

```sh
# Uses profile = java17 from config.
$javaspec run --config javaspec.conf --generate

# Overrides config profile for this run and enforces java21 before writes.
$javaspec run --config javaspec.conf --profile java21 --generate

# Dry-run also enforces; on violations it writes no files and no reports.
$javaspec run --profile java8 --dry-run --report target/javaspec-report.json
```

Violation output starts with `Profile compatibility error`, includes the selected profile, the
spec/type, and the reason list, then exits `64` before generation/update writes:

```text
Profile compatibility error: record requires Java 17 but target profile is Java 8
Selected profile: java8 (Java 8)
Spec/type: spec.org.example.CustomerSpec -> record org.example.Customer
```

### Verbose diagnostics

`--verbose` prints the selected suite, spec root, source root, spec package prefix, production
package prefix, constructor policy, profile, formatter, compile flag/output when requested,
configured bootstrap hooks when present, effective JSON report path when specified by config or CLI,
effective JUnit XML path when specified by config or CLI, explicit classpath entries when supplied,
dry-run setting, and stop-on-failure setting before run work proceeds.

These controls belong to `run` only and are rejected for `describe`/`desc`.

## No-JUnit programmatic invocation

The `io.github.jvmspec.invocation` package provides a no-`System.exit` API for launchers, build tools,
and CI adapters that want to invoke javaspec inside the current JVM without JUnit:

- **`JavaspecInvocation`**: Immutable invocation input: either a `SpecDiscoveryRequest` or already
  discovered `DiscoveredSpec` list, a selected `ClassLoader`, optional bootstrap hook class names,
  activated extension class names, optional compilation settings, and stop-on-failure behavior.
- **`JavaspecLauncher`**: Delegates to canonical `SpecDiscovery`, bootstrap hook execution,
  `SpecRunner`, and `RunResult` semantics and returns a structured result.
- **`JavaspecInvocationResult`**: Exposes discovered specs, the `RunResult`, exit code, success
  helpers, and failure helpers.
- **`JavaspecExitCode`**: Maps passing, skipped/pending-only, and no-spec runs to `0`; failed or
  broken runs to `1`.

Example:

```java
SpecDiscoveryRequest request = SpecDiscoveryRequest.of(new File("src/test/java"));
JavaspecInvocation invocation = JavaspecInvocation.discovering(request, classLoader)
        .withBootstrapHook("org.example.SpecBootstrap")
        .withStopOnFailure(true);
JavaspecInvocationResult result = JavaspecLauncher.run(invocation);

int exitCode = result.exitCode();
RunResult runResult = result.runResult();
```

The launcher does not call `System.exit`, so the host process can inspect the structured result and
decide how to continue. `DiscoveredSpec`, `SpecResult`, and `ExampleResult` expose stable id aliases
(`id`/`stableId` getter styles), and runner results carry source metadata where discovery supplied
it. By default it is still a classpath-based executor: callers must provide a classloader that can
load compiled bootstrap hooks, production classes, and spec classes, or bootstrap failures/skipped
results will occur. Programmatic hosts can opt into current-JDK compilation with
`JavaspecInvocation.withCompilation(...)`; this uses `javax.tools`, has no dependency
resolution/forked `javac`/incremental cache, and exposes compilation results through
`JavaspecInvocationResult`. Programmatic hosts can call
`io.github.jvmspec.diagnostics.RunDiagnostics.executionAvailabilityLines(RunResult)` to obtain
deterministic human-readable availability diagnostics that exclude explicit `@Skip` and `PENDING`
semantics.

Programmatic tools that need the same source/generation compatibility boundary as the CLI can use
`io.github.jvmspec.compatibility.ProfileEnforcement.defaultEnforcement()` and inspect
`ProfileEnforcementReport` / `ProfileViolation` before writing generated or updated source. This API
is additive and does not require invoking the CLI.

## Optional Maven plugin

Phase 15 provides a standalone optional Maven plugin artifact at `javaspec-maven-plugin/`. It is
intentionally not registered as a root module, so repository-root `mvn verify` continues to build
and audit only the zero-runtime-dependency core artifact.

Local plugin verification sequence:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml verify
```

The plugin packages `io.github.jvmspec:javaspec-maven-plugin:0.1.0-SNAPSHOT` as `maven-plugin`, uses Java
source/target `1.8`, goal prefix `javaspec`, Maven API baseline `3.6.3`, Maven API and plugin
annotations in `provided` scope, JUnit in `test` scope, and a compile-scope dependency on core
`io.github.jvmspec:javaspec`.

A consuming Maven build can declare the plugin as optional project tooling:

```xml
<plugin>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</plugin>
```

The `JavaspecRunMojo` goal `javaspec:run` is bound to Maven's `verify` phase by default, requires
test dependency resolution, and uses Maven's test classpath. It supports config, suite,
`specDir`/`specRoot` selection, top-level plus selected-suite bootstrap hooks, class/example
filters, `stopOnFailure`, `skip`, `failOnFailure`, JSON reports, JUnit XML-compatible reports, and
Maven logging with pending counts. Configured report destinations are used as defaults when explicit
Maven plugin report settings are absent; explicit plugin report settings override config values.
Maven formatter selection is available through config or `javaspec.formatter`, and extension
activation through config or `javaspec.extensions`, where implemented. When execution-availability
issues exist, it logs `javaspec:` warnings plus the Maven test classpath element count. Bootstrap
failures fail the build with clear `javaspec bootstrap execution failed` diagnostics. It delegates
to canonical no-JUnit `io.github.jvmspec.invocation.JavaspecLauncher` without `System.exit`, so projects
under test do not need JUnit. Maven source/spec compilation remains Maven's lifecycle responsibility
by default. Phase 34 adds opt-in `javaspec.compile`, `javaspec.compileOutput`, and
`javaspec.compileOutputDirectory` settings for current-JDK `javax.tools` compilation before
bootstrap/examples, without dependency resolution, forked `javac`, or incremental caching.

## Optional Gradle plugin

Phase 16 provides a standalone optional Gradle plugin artifact at `javaspec-gradle-plugin/`. It is
intentionally not registered as a root Maven module, so repository-root `mvn verify` continues to
build and audit only the zero-runtime-dependency core artifact.

Local plugin verification sequence:

```sh
mvn -q -DskipTests install
gradle -p javaspec-gradle-plugin build
```

In a consuming Gradle build where the standalone plugin artifact is available to Gradle, apply
plugin id `io.github.jvmspec` and configure the optional extension/task:

```groovy
plugins {
    id 'java'
    id 'io.github.jvmspec' version '0.1.0-SNAPSHOT'
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

The plugin registers extension `javaspec` and task `javaspecRun` in Gradle's `verification` group.
When the Gradle Java plugin/source sets are present, `javaspecRun` defaults to the `test` source set
runtime classpath and depends on `testClasses`. The task supports `skip`, `failOnFailure` (default
`true`), `stopOnFailure`, `configFile`, `suite`, `specDir`/`specRoot`, top-level plus selected-suite
bootstrap hooks, class filters, example filters, formatter selection, JSON report aliases
(`reportFile`, `jsonReportFile`), and JUnit XML-compatible report aliases (`junitXmlReportFile`,
`junitXmlFile`), inheriting pending-aware summaries/reports from core. It loads built-in formatters
and ServiceLoader formatter/extension providers from the Gradle run classloader; provider jars can
be on the configured task classpath, extension classpath, or default Java test runtime classpath.
Bootstrap hook classes must also be on the Gradle run classloader. Formatter precedence is task
setting, extension setting, project property `javaspec.formatter`, config `formatter`, then default
`progress`, and invalid formatter diagnostics list discovered names. Configured report destinations
are used as defaults when explicit Gradle extension/task report settings are absent; explicit Gradle
adapter settings override config values. It loads javaspec configuration when configured, selects
suites, builds `SpecDiscoveryRequest` with `SpecNamingConvention`, uses a `URLClassLoader` over the
Gradle classpath, sets/restores the thread context classloader, closes the loader, writes reports
through core writers, logs through Gradle including `javaspec:` execution-availability warnings plus
the Gradle classpath element count when needed, throws `GradleException` for bootstrap failures with
clear `javaspec bootstrap execution failed` diagnostics, throws `GradleException` on failed/broken
examples when `failOnFailure=true`, and delegates to canonical no-JUnit `JavaspecLauncher` without
`System.exit`. Gradle source/spec compilation remains Gradle's task/source-set responsibility by
default. Phase 34 adds opt-in `compile` / `javaspec.compile` and `compileOutput` /
`javaspec.compileOutput` settings for current-JDK `javax.tools` compilation before
bootstrap/examples, without dependency resolution, forked `javac`, or incremental caching.

No JUnit is required in projects under test; JUnit is only a plugin test dependency.

## Optional JUnit Platform engine

Phase 17 provides a standalone optional JUnit Platform engine artifact at
`javaspec-junit-platform-engine/`. It is intentionally not registered as a root Maven module and
remains outside the zero-runtime-dependency core artifact.

Local engine verification sequence:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml verify
```

The engine artifact is `io.github.jvmspec:javaspec-junit-platform-engine:0.1.0-SNAPSHOT`, packaging
`jar`, Java source/target `1.8`, and uses Java 8-compatible JUnit Platform `1.10.2` rather than
JUnit Platform 6/JUnit 6. Runtime dependencies are isolated to the optional engine artifact: core
`io.github.jvmspec:javaspec`, `org.junit.platform:junit-platform-engine`, and transitives `opentest4j`,
`junit-platform-commons`, and `apiguardian-api`. JUnit Platform Launcher, JUnit Platform TestKit,
and JUnit Jupiter are engine test-only dependencies.

The engine implementation is `io.github.jvmspec.junit.platform.JavaspecTestEngine`, registered through
`META-INF/services/org.junit.platform.engine.TestEngine`, and its engine id is `javaspec`. To opt
in, place the engine artifact on the JUnit Platform test runtime classpath used by the selected
IDE/CI/build launcher. Projects that do not opt into it still have no JUnit dependency and can keep
CLI, programmatic, Maven plugin, or Gradle plugin no-JUnit execution paths.

Discovery uses canonical `SpecDiscovery` / `SpecDiscoveryRequest`. Supported configuration
parameters are:

| Parameter | Purpose |
|---|---|
| `javaspec.configFile` | Select a javaspec configuration file. |
| `javaspec.suite` | Select a configured suite. |
| `javaspec.specDir` / `javaspec.specRoot` | Select the spec root. |
| `javaspec.classFilters` / `javaspec.classFilter` / `javaspec.class` | Apply class filters over canonical discovery results. |
| `javaspec.exampleFilters` / `javaspec.exampleFilter` / `javaspec.example` | Apply example filters over canonical discovery results. |
| `javaspec.stopOnFailure` | Stop after the first failed or broken executable example. |

Class, package, method, and unique-id selectors are supported as filters over canonical discovery
results. UniqueId segments use `[engine:javaspec]`, `[spec:<specQualifiedName>]`, and
`[example:<methodName>]`; Phase 18 retains this stable shape and MethodSource behavior while
aligning descriptor reporting to stable ids.

Execution delegates to canonical no-JUnit `JavaspecLauncher` using discovered specs. Result mapping
to JUnit Platform listener events is: passed -> successful, failed assertion results -> failed
assertion-style throwable, broken results -> failed/error-style throwable, and skipped, pending, or
non-loadable results -> `executionSkipped`. Pending skip reasons are prefixed with `Pending:`. The
engine avoids `System.exit` and does not require changes to javaspec spec authoring style. It relies
on the JUnit Platform test runtime classpath to contain compiled spec classes, production classes,
and dependencies; it does not compile source/spec files itself, and Phase 29/34 compilation support
does not change the engine.

## Example execution

`javaspec run` has two stages:

1. Preserve the existing discovery, related-spec handling, support updates, production type
   generation, constructor updates, and method updates.
2. If no prompt was declined and no I/O/usage error occurred, optionally compile source/spec trees
   for CLI `--compile` runs, then execute the discovered examples whose compiled spec classes are
   available on the effective classloader, the selected explicit classloader, or the
   compile-output-first execution classloader.

The runner is reflection-based and dependency-free. By default it does not compile source or spec
files. If a spec exists only as source, or if the compiled spec class or one of its dependencies is
otherwise unavailable to the CLI process or explicit classpath, the discovered examples are marked
`SKIPPED` rather than executed with enriched execution-availability reasons. Missing or stale
compiled public no-argument example methods are also skipped with availability details. Compile
first and put spec/production classes on the process classpath, pass already compiled entries with
`--classpath` / `--classpath-file`, or for CLI runs opt into source/spec compilation with
`--compile` / `--compile-output <dir>` before expecting execution.

Execution uses the existing discovery metadata:

- `DiscoveredSpec` selects the spec class and described type and exposes stable id aliases derived
  from the spec qualified name.
- `SpecExample` selects public `void` `it_*`/`its_*` examples, display names, source-order indexes,
  and 1-based source lines.
- `ExampleResult` stable ids use `<specQualifiedName>#<methodName>` and match `fullName()`;
  `SpecResult` stable ids derive from the spec qualified name.
- Suite, class, and example filters affect execution because they filter that metadata before the
  runner sees it.

Lifecycle behavior:

- A fresh spec instance is created for each executable example.
- Optional public no-argument `let()` runs before each executable example.
- Optional public no-argument `letGo()` runs after each executable example, including after
  failures.
- Annotation-based skip/pending is decided before spec construction, so the spec is not instantiated
  and `let()`/example/`letGo()` do not run.
- Runtime `SkipExampleException` or `PendingExampleException` thrown from `let()` or an example
  method marks the example after successful `letGo()`; a `letGo()` failure after such a signal is
  `BROKEN`.

Explicit skip/pending examples:

```java
import io.github.jvmspec.api.Pending;
import io.github.jvmspec.api.Skip;

public class PaymentSpec extends PaymentSpecSupport {
    @Skip(reason = "external gateway unavailable")
    public void it_refunds_a_charge() {
    }

    @Pending("waiting for billing provider")
    public void it_charges_a_card() {
    }

    public void it_can_stop_at_runtime() {
        pending("replace stub with contract test");
        // or: skip("requires test data")
    }
}
```

`@Skip` takes precedence over `@Pending` when both are present. `value()` and `reason()` both
provide reasons; `reason()` wins when both are set. Specs that do not extend `ObjectBehavior` can
throw `SkipExampleException` or `PendingExampleException` directly.

Result states:

- **`PASSED`**: The example completed normally.
- **`FAILED`**: The example threw `AssertionError`.
- **`BROKEN`**: A non-assertion throwable occurred in the example, `let()`, `letGo()`, spec
  instantiation, or reflection inspection.
- **`SKIPPED`**: The spec class was not loadable, the reflected example method was missing or not
  public no-arg, or the example was explicitly skipped by annotation/signal.
- **`PENDING`**: The example was explicitly marked pending by annotation/signal. Pending is distinct
  from skipped and has a separate `pendingCount()`.

CLI summary example:

```text
Examples: 4 total, 1 passed, 1 failed, 0 broken, 1 skipped, 1 pending.
Failed examples:
  FAILED spec.org.example.CalculatorSpec#it_adds_numbers (it adds numbers): Assertion failed - java.lang.AssertionError: expected 4
Skipped examples:
  SKIPPED spec.org.example.CalculatorSpec#it_subtracts_numbers (it subtracts numbers): Example method not found or not public no-arg: it_subtracts_numbers
Pending examples:
  PENDING spec.org.example.CalculatorSpec#it_multiplies_numbers (it multiplies numbers): waiting on multiplication rules
```

Exit code `1` is returned when executable examples fail or break. Skipped-only, pending-only, and
skipped-plus-pending runs remain successful. With `--report` and/or `--junit-xml`, passing, failing,
broken, skipped-only, and pending-only runs write requested reports before the final run exit code
is returned. Missing production generation or method-update prompts that are declined or unavailable
also return exit code `1` before execution.

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

The generated smoke example is intentionally small. In normal work, replace or extend it with behavior-focused PHPSpec-like examples using typed proxies, for example `add(2, 3).shouldReturn(5)`, or the explicit form `match(subject().add(2, 3)).shouldReturn(5)` until the proxy exists.

Generated support class:

```java
package spec.org.example;

import org.example.Calculator;

public class CalculatorSpecSupport extends io.github.jvmspec.api.ObjectBehavior<Calculator> {
    public CalculatorSpecSupport() {
        super(Calculator.class);
    }
}
```

The concrete spec extends the generated support class. The support class extends
`ObjectBehavior<Calculator>` and passes `Calculator.class` to enable lazy subject construction.

The import of `org.example.Calculator` is intentional. In a BDD/spec-first flow the production class
may not exist yet, so the project can be red until `run` generates or the user writes the class.

### 2. Run discovery, generation, and execution

```sh
$javaspec run
```

`run` first performs discovery, enforces the effective target profile, and then performs any gated
generation/update work. If execution can proceed, CLI `--compile` runs compile source/spec files
before configured bootstrap hooks; then `run` executes hooks immediately before examples and runs
discovered examples whose compiled spec classes are available on the effective classloader, selected
explicit classloader, or compile-output-first execution classloader.

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

After profile enforcement succeeds, this writes missing production type skeletons, generated
specification support updates, constructor updates, static factory construction method skeletons,
and supported missing method bodies/declarations/elements inferred from specs without interactive
confirmation. After those updates, CLI `--compile` runs compile source/spec files if requested,
configured bootstrap hooks execute before examples, and executable examples run only if the
corresponding compiled spec classes are already on the effective classloader, selected explicit
classloader, or compile-output-first execution classloader; otherwise they are reported as skipped.
If enforcement fails, no source files are written.

Use `--dry-run` when CI should report pending generated work without modifying the workspace:

```sh
$javaspec run --dry-run
```

Dry-run never writes files, never prompts, and never compiles. It exits `64` on profile violations
before reports, exits `64` on bootstrap failures if it reaches example execution, or exits `1` if
compatible generation/update work is pending.

## PHPSpec-to-Java migration notes

javaspec is inspired by PHPSpec, but Java's packages, static typing, compilation model, and
interfaces change how the concepts are expressed.

- **`phpspec desc App\\Book`**: `javaspec describe org.example.Book` creates Java `BookSpec` and
  `BookSpecSupport`.
- **PHP namespace convention**: Java packages plus suite `specPackagePrefix` and `packagePrefix`.
- **Subject available as `$this`**: Lazy `ObjectBehavior<T>` subject accessed through generated
  typed support methods or explicit `subject()`.
- **Spec examples**: Public `void` methods named `it_*` or `its_*`; display names replace
  underscores with spaces.
- **`let()` / `letGo()` lifecycle**: Optional public no-argument `let()` and `letGo()` on the Java
  spec class.
- **`beConstructedWith(...)`**: Same method name for constructor arguments; configure before first
  subject access.
- **`beConstructedThrough(...)` / named constructors**: Static factory construction with
  string-literal Java method names for generation.
- **`should*` / `shouldNot*` expectations**: prefer `Matchable<T>` methods through generated typed
  proxies such as `getTitle().shouldReturn("Wizard")`, or through explicit wrappers such as
  `match(subject().getTitle()).shouldReturn("Wizard")`. Direct `ObjectBehavior` convenience
  assertions exist for ad-hoc checks but are not the main documentation style.
- **PHPSpec generated method suggestions**: `javaspec run` owns production generation/update after
  confirmation, `--generate`, or `--dry-run` planning.
- **Source/spec compilation**: CLI `javaspec run --compile`, programmatic `withCompilation(...)`,
  and Maven/Gradle opt-in settings can use current-JDK compilation before bootstrap/examples;
  defaults remain classpath-based.
- **Prophecy-style collaborators**: Core javaspec doubles ordinary Java interfaces through JDK
  dynamic proxies. Prefer generated typed `*Prophecy` wrappers for PHPSpec-like syntax such as
  `mailer.send(...).willReturn(...).shouldBeCalled()`; Java 8 specs name the wrapper type
  explicitly, while Java 10+ specs can use `var`. The reflective `mailer.method("send", ...)` form
  is a bootstrap/fallback. Optional non-final concrete-class doubles require the standalone bytecode
  adapter, and final-class collaborators require the optional bytecode agent adapter.
- **Formatters/extensions**: Built-in `progress`/`pretty` formatters, programmatic extension
  contracts, config-driven extension activation, and ServiceLoader-discovered run
  formatter/extension providers where implemented.
- **Bootstrap hooks**: Explicit configured Java hook classes plus ServiceLoader-discovered
  `BootstrapHook` providers, executed before examples from the run classloader/classpath with
  explicit hooks first.
- **No-JUnit CI execution**: CLI `--classpath` / `--classpath-file`, programmatic
  `io.github.jvmspec.invocation`, optional Maven/Gradle plugin adapters, JSON reports, and JUnit
  XML-compatible reports without requiring JUnit.

Practical migration guidance:

1. Start with `describe`, but expect the generated Java spec to import a production type that may
   not exist yet. The Java project may be temporarily red until `run --generate` or manual
   production code creation catches up.
2. Keep examples as public `void it_*`/`its_*` methods. The reflection runner ignores unrelated methods and
   can execute only compiled spec classes on the effective classloader, explicit classpath, or CLI
   compile-output-first classloader.
3. Prefer generated typed proxy methods for PHPSpec-like syntax, for example
   `getRating().shouldReturn(5)`. Use `match(subject().getRating()).shouldReturn(5)` when the
   proxy has not been generated yet or when an explicit subject call is clearer. Avoid making direct
   convenience assertions like `shouldReturn(actual, expected)` the default style in new docs/specs.
4. Configure construction before touching the subject. The last construction rule before first
   subject access wins; changes after instantiation are errors.
5. Use string-literal Java identifiers for factory construction markers when you want generation,
   for example `beConstructedThrough("create", value)`. Non-literal factory names can still describe
   runtime construction, but generation cannot infer a method name from them.
6. Treat generated method bodies as skeletons with Java default returns, not as inferred business
   behavior. javaspec does not synthesize return constants from expectations.
7. Prefer interface collaborators when you need zero-dependency core doubles. Optional non-final
   concrete-class doubles are available through `javaspec-bytecode-doubles`. Optional final-class,
   static-method, and construction-aware doubles are available through `javaspec-bytecode-agent`,
   which requires ByteBuddy Agent / JVM instrumentation support. Primitive, array, annotation, enum,
   and interface targets are still unsupported by bytecode concrete-class adapters.
8. Use the restricted line-based config format instead of PHPSpec YAML-style configuration.
   Bootstrap entries are explicit compiled Java hook class names that execute before examples during
   `run`.
9. Use `--dry-run` in CI to detect pending generated work without modifying the workspace.
10. Use `--classpath` or `--classpath-file` after an external build has compiled production/spec
    classes, or use CLI `--compile` / `--compile-output <dir>` when a source-only CLI run should
    compile with the current JDK before examples.
11. Use `--report` for the implemented JSON runner report, `--junit-xml` for dependency-free JUnit
    XML-compatible output, or configure top-level report destinations for reusable `run --config
    <file>` defaults. Both report paths include stable identifiers and source metadata where
    available after the Phase 18 polish increment, Phase 35 run-level metadata/properties from the
    default writers, and CLI report options override config destinations.

## Construction semantics

Generated support classes configure `ObjectBehavior<Subject>` with `Subject.class`, so the subject
is constructed lazily on first access. Construction can be configured before that first access.

Constructor arguments use `beConstructedWith(...)`. For generation, this remains constructor
descriptor generation: `run` can create or update matching constructor skeletons, not factory
methods.

```java
public class BookSpec extends BookSpecSupport {
    public void it_can_be_constructed_with_values() {
        beConstructedWith("Wizard", 5);

        getTitle().shouldReturn("Wizard");
        getRating().shouldReturn(5);
    }
}
```

Static factory construction uses `beConstructedThrough("create", args...)`. For generation, the
factory marker now discovers/generates a static factory method skeleton returning the described type
instead of an empty constructor marker.

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

For a described `Book`, these correspond to static factory skeletons such as `named()`,
`named(String arg0)`, and `createNamed(String arg0)`, all returning `Book`.

Factory marker names must be string literals and valid Java identifiers to generate methods. Calls
with non-string-literal names, such as `beConstructedThrough(factoryName, "Wizard")`, are ignored by
generation because the method name is not statically known; they do not create empty constructor
markers.

Construction can be overridden before instantiation. The last construction rule before the first
subject access wins:

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

## PHPSpec-style example data

Example data keeps a small set of concrete cases inside one behavior example. It is intended for
places where JUnit parameterized tests or Cucumber `Scenario Outline` tables would add more ceremony
than design feedback.

```java
public class NameNormalizerSpec extends ObjectBehavior<NameNormalizer> {
    public NameNormalizerSpec() {
        super(NameNormalizer.class);
    }

    public void it_normalizes_known_inputs() {
        examples(row("  Alice  ", "Alice"), row("Bob", "Bob"))
            .verify(new Example2<String, String>() {
                @Override
                public void run(String input, String expected) {
                    match(subject().normalize(input)).shouldReturn(expected);
                }
            });
    }
}
```

`Example1` and `Example2` callbacks are available in core for Java 8-compatible one- and two-column
rows. Rows currently execute inside the containing `it_*` example. If a row fails, the assertion
message includes the row number and values, for example `Example data row 2 [ Bob , Robert] failed`.

## Typed proxy matcher syntax

Generated support classes can expose subject-specific typed proxy methods. This allows PHPSpec-like
Java syntax in the concrete spec:

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
protected io.github.jvmspec.matcher.Matchable<Integer> getRating() {
    return match(subject().getRating());
}

protected io.github.jvmspec.matcher.Matchable<String> getTitle() {
    return match(subject().getTitle());
}

protected void setRating(int rating) {
    subject().setRating(rating);
}
```

It also generates typed throw proxies such as `duringSetRating(...)`.

Generated typed spec support intentionally skips static factory descriptors discovered from
construction markers such as `beConstructedThrough("create", ...)`. Static factories are
construction methods on the described type, not instance subject proxies, so support classes do not
generate `create().should...`, `duringCreate(...)`, or `subject().create(...)` wrappers for them.

The existing explicit wrapper style remains available:

```java
match(subject().getRating()).shouldReturn(5);
```

## Method generation

`run` discovers typed proxy calls and construction factory markers, then can generate supported
missing subject method bodies, interface declarations, or annotation elements depending on the
described production kind. Discovery currently covers the supported expanded chained matcher calls,
typed throw calls such as `shouldThrow(...).duringSetRating(-3)`, direct `subject().method(...)`
calls, simple setter-style calls, and static factory construction markers.

`beConstructedWith(...)` remains constructor descriptor generation. The factory construction forms
`beConstructedThrough("create", args...)`, `beConstructedNamed("named", args...)`, and
`beConstructedThroughNamed("createNamed", args...)` are method-generation inputs when the factory
name is a string literal and a valid Java identifier; they generate static factory methods returning
the described type instead of empty constructor markers.

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

Factory construction markers add static factory skeletons returning the described type. For example,
`beConstructedThrough("create", "Wizard")` can add:

```java
public static Book create(String arg0) {
    return new Book();
}
```

Static factory descriptors are skipped when generated typed support is updated, because they are
construction methods rather than instance subject proxy methods.

When the production source file already exists and `--generate` is not used, javaspec prompts before
adding supported missing method skeletons, declarations, or elements:

```text
Do you want me to add missing method skeletons to org.example.Book in src/main/java/org/example/Book.java? [Y/n]
```

Default returns are Java 8-compatible: `false` for `boolean`, zero values for numeric primitives,
`'\0'` for `char`, and `null` for reference types. Before any generated method bodies, declarations,
or elements are written, profile enforcement checks resolvable return and parameter type owners
against the selected profile catalog. Unknown project types and ambiguous simple names are left
alone so generation does not fail on incomplete source-type information.

### Interface-style method declarations and annotation elements

For a described ordinary interface, missing production skeletons and existing ordinary interface
sources use Java declarations without method bodies. Static descriptors are skipped because
interface-style generation only adds discovered instance methods.

```java
package org.example;

public interface PaymentGateway {
    String status();

    boolean charge(String accountId, int cents);
}
```

For a described annotation, missing skeletons and existing annotation sources emit only
Java-compatible no-argument non-static annotation elements. Descriptors with parameters, static
descriptors, `void`, `Object`, or otherwise incompatible return types are ignored for annotation
generation/update.

```java
package org.example;

public @interface GeneratedTag {
    String value();

    int priority();

    String[] tags();
}
```

For a missing described sealed interface, the generated root interface receives method declarations
and each generated nested permitted implementation receives matching method bodies with Java default
returns so the Java 17 source form remains valid.

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

Existing sealed-interface source updates are now supported source-preservingly: generated
declarations are inserted into the sealed root and matching default-return bodies are inserted into
nested permitted implementations.

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

Empty generated/no-op unmatched constructors may be removed when safe, regardless of policy.
Constructor policy applies to `run`; `describe` never updates production source. A config file can
set `constructorPolicy`/`constructor-policy`; command-line `--constructor-policy` overrides the
configured value.

## Matchers

Typed proxy methods return `Matchable<T>` for non-void subject methods. The explicit wrapper style
also returns `Matchable<T>`:

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

`MatcherRegistry` keeps the runtime dependency-free. It provides the built-in
identity/equality/negated-identity matchers and a default negated-equality matcher for
`shouldNotEqual` and its aliases.

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

Generic `Iterable` empty checks use a single `iterator().hasNext()` peek.
`shouldHaveCount(expected)` iterates at most `expected + 1` elements, so infinite iterables fail
fast when more elements than expected are observed; one-shot iterables may still be advanced by the
bounded check.

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

`ObjectBehavior` also exposes direct assertion methods for ad-hoc checks. They delegate through
`match(actual)`, so behavior is the same, but they are secondary to the PHPSpec-like typed-proxy and
explicit `match(subject()...)` styles:

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

The direct convenience set covers equality/negation aliases, type/instance/implementation checks,
containment, count/empty checks, map key/value checks, and string negations. Prefer typed proxy or
explicit `match(actual)` usage in examples unless the direct helper makes an ad-hoc assertion much
clearer.

### Custom matchers

Custom matchers can be registered in the matcher registry and may evaluate null subjects; javaspec
passes the actual subject value, including `null`, to the matcher predicate.

```java
matcherRegistry().register("beAbsent", new io.github.jvmspec.matcher.CustomMatcher<Object>(
    "beAbsent",
    new io.github.jvmspec.matcher.CustomMatcher.MatchPredicate<Object>() {
        @Override
        public boolean test(Object subject, Object... expected) {
            return subject == null;
        }
    }
));

match(null).shouldMatch("beAbsent");
```

`SpecDiscovery` recognizes the expanded chained matcher names on typed proxy calls for
method-discovery/default-return inference where applicable.

## Prophecy-style collaborators

Prophecy-style doubles are the recommended PHPSpec-like collaborator syntax when the subject depends
on another object. They are for **dependencies of the subject**, not for the subject itself.

Prefer generated typed `*Prophecy` wrappers in concrete specs:

```java
import static io.github.jvmspec.doubles.prophecy.Argument.*;

public class UserServiceSpec extends UserServiceSpecSupport {
    public void it_sends_a_welcome_email() {
        MailerProphecy mailer = prophesizeMailer();

        mailer.send(any(String.class), any(String.class), any(String.class))
            .willReturn(true)
            .shouldBeCalled();

        setMailer(mailer.reveal());
        sendWelcomeEmail("user@example.com");

        checkPredictions();
    }
}
```

On Java 10+, `var mailer = prophesizeMailer();` keeps the same typed method syntax while hiding the
wrapper type. This mirrors PHPSpec/Prophecy's intent: describe the collaborator promise and
prediction close to the behavior example, without stringly method names in the spec body.

Use the reflective form only as a bootstrap/fallback before the wrapper has been generated:

```java
ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);
mailer.method("send", any(String.class), any(String.class), any(String.class))
    .willReturn(true)
    .shouldBeCalled();
```

`javaspec prophesize com.example.Mailer` generates `MailerProphecy`. During
`javaspec run --generate`, javaspec can detect `prophesize(Mailer.class)` / `prophecy(Mailer.class)`
usage, generate the missing wrapper under `target/generated-sources/javaspec`, and update generated
`*SpecSupport` helpers such as `prophesizeMailer()` / `prophecyMailer()`.

Predictions are checked by `checkPredictions()` or automatically when `--auto-check-predictions` is
enabled.

## Interface doubles

The doubles API under `io.github.jvmspec.doubles` provides zero-runtime-dependency collaborator doubles.
Doubles are implemented with Java 8 JDK dynamic proxies, so the core runtime can double ordinary
interfaces without bytecode libraries.

Phase 28 strengthens the interface-double API with argument matchers, argument-constrained
stub precedence, throwing stubs, and answer callbacks. Core doubles remain interface-only and
zero-runtime-dependency; Phase 37 adds optional non-final concrete-class doubles through a
standalone bytecode adapter.

### Creating interface doubles

Use `Doubles` directly when working outside an `ObjectBehavior` subclass. The examples below assume
`Notifier` is an ordinary interface.

```java
import io.github.jvmspec.doubles.Doubles;
import io.github.jvmspec.doubles.InterfaceDouble;

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

`Doubles.isDouble(value)` returns whether a value is a javaspec double. `Doubles.control(proxy)`,
`Doubles.inspect(proxy)`, `doubleControl(proxy)`, and `inspectDouble(proxy)` return the control API
for an existing proxy.

### Argument matchers

Argument matchers live in `io.github.jvmspec.doubles` and match a single argument in existing vararg
APIs. Use the factories on `ArgumentMatchers` or the same convenience aliases on `Doubles`:

- **`any()` / `anyArgument()`**: Any argument, including `null`.
- **`any(Class<?>)` / `anyType(Class<?>)`**: `null` or a value assignable to the supplied type;
  primitive class tokens match boxed proxy arguments.
- **`isNull()`**: Only `null`.
- **`notNull()`**: Any non-null argument.
- **`eq(expected)` / `equalTo(expected)`**: javaspec array-aware equality with the expected value.

```java
import static io.github.jvmspec.doubles.Doubles.any;
import static io.github.jvmspec.doubles.Doubles.anyArgument;
import static io.github.jvmspec.doubles.Doubles.eq;
import static io.github.jvmspec.doubles.Doubles.notNull;

notifierDouble.when("send", eq("alerts"), any(String[].class)).thenReturn(Boolean.TRUE);
notifierDouble.verifyCalled("send", eq("alerts"), any(String[].class));

notifierDouble.verify("send", notNull(), anyArgument()).calledOnce();
```

Ordinary values in the same APIs remain exact expected values. Exact matching still supports `null`
and compares arrays by contents rather than identity. Matchers work in `when`, `verify`,
`verifyCalled`, `verifyNotCalled`, exact call counts, `calls`, and `Call.hasArguments`.

### Stubbing return values

Stubs can match by method name with any arguments or by method name with argument constraints:

```java
notifierDouble.when("channel").thenReturn("alerts");
notifierDouble.when("send", "alerts", new String[] {"ops", "oncall"}).thenReturn(Boolean.TRUE);
notifierDouble.when("send", eq("alerts"), any(String[].class)).thenReturn(Boolean.TRUE);

notifierDouble.returns("fallbackChannel", "general");
notifierDouble.returnsFor("send", Boolean.TRUE, "alerts", new String[] {"ops", "oncall"});
```

Argument-constrained stubs, including matcher patterns, take priority over method-wide stubs. Within
the same priority, the newest matching stub wins. Unstubbed methods return Java defaults: `false`
for `boolean`, zero values for numeric primitives, `'\0'` for `char`, `null` for reference types,
and no action for `void` methods.

### Throwing stubs and answer callbacks

Throwing stubs use `MethodStub.thenThrow(Throwable)` or the alias `throwsException(Throwable)`. The
double records the call before throwing the supplied throwable.

```java
notifierDouble.when("send", eq("alerts"), any(String[].class))
        .thenThrow(new IllegalStateException("offline"));
```

Answer stubs use `MethodStub.thenAnswer(StubAnswer)` or the alias `answers(StubAnswer)`. The answer
receives an immutable `DoubleInvocation` context with the reflective method, method name, immutable
argument snapshots, defensive argument-array copies, individual argument access, and argument count.

```java
import io.github.jvmspec.doubles.DoubleInvocation;
import io.github.jvmspec.doubles.StubAnswer;

notifierDouble.when("channelFor", any(String.class)).thenAnswer(new StubAnswer() {
    @Override
    public Object answer(DoubleInvocation invocation) {
        return "alerts-" + invocation.argument(0);
    }
});
```

Answer return values use the same return type validation rules as `thenReturn(Object)`. Throwables
raised by answer callbacks propagate from the proxy invocation.

### Call history and verification

Interface method calls are recorded as immutable `Call` snapshots before a stubbed return, thrown
exception, answer callback result, or default return is produced. The control APIs can inspect all
calls, calls by method name, or calls by method name and exact or matcher arguments:

```java
notifier.send("alerts", new String[] {"ops", "oncall"});

notifierDouble.calls();
notifierDouble.calls("send");
notifierDouble.calls("send", "alerts", new String[] {"ops", "oncall"});
notifierDouble.calls("send", eq("alerts"), any(String[].class));
notifierDouble.callCount("send");
notifierDouble.callCount("send", eq("alerts"), any(String[].class));
```

Verification supports called, not-called, called-once, and exact-count checks with ordinary exact
values or matchers:

```java
notifierDouble.verify("send").called();
notifierDouble.verify("send", "alerts", new String[] {"ops", "oncall"}).calledOnce();
notifierDouble.verify("send", eq("alerts"), any(String[].class)).times(1);
notifierDouble.verify("missing").notCalled();

notifierDouble.verifyCalled("send");
notifierDouble.verifyCalledWith("send", eq("alerts"), any(String[].class));
notifierDouble.verifyNotCalled("missing");
notifierDouble.verifyCallCount("send", 1);
```

`Call.hasArguments(...)` uses the same matcher-aware semantics:

```java
Call call = notifierDouble.calls("send").get(0);
call.hasArguments(eq("alerts"), any(String[].class));
```

`ObjectBehavior` adds spec-style convenience assertions and call queries. Their vararg argument APIs
accept the same ordinary exact values and matchers:

```java
shouldHaveBeenCalled(notifier, "send");
shouldHaveBeenCalledWith(notifier, "send", eq("alerts"), any(String[].class));
shouldNotHaveBeenCalled(notifier, "missing");
shouldHaveBeenCalledTimes(notifier, "send", 1);
shouldHaveBeenCalledTimes(notifier, "send", 1, eq("alerts"), any(String[].class));

doubleCalls(notifier);
doubleCalls(notifier, "send");
doubleCalls(notifier, "send", eq("alerts"), any(String[].class));
doubleCallCount(notifier, "send");
doubleCallCount(notifier, "send", eq("alerts"), any(String[].class));
```

### Object methods and resets

`toString()`, `equals(Object)`, and `hashCode()` are handled deterministically by the proxy
invocation handler. `toString()` identifies the doubled interface and internal id, `equals` uses
proxy identity, and `hashCode` uses the stable internal id. These object methods are not user stubs
and do not represent collaborator calls.

Call and stub state can be cleared separately or together:

```java
notifierDouble.clearCalls();
notifierDouble.clearStubs();
notifierDouble.reset();
```

### Supported targets and limitations

Core double factories support ordinary interfaces. They reject `null`, primitive types, arrays,
annotations, enums, concrete classes, and final classes with clear diagnostics. Optional non-final
concrete-class doubles require `javaspec-bytecode-doubles`; the adapter still rejects final classes,
enums, arrays, annotations, primitives, and interfaces, and it does not support static method or
constructor mocking. Unstubbed Java default interface methods are invoked by the proxy handler and still recorded for verification.

## Optional bytecode concrete-class doubles

Add the standalone adapter when a spec needs a non-final concrete collaborator double:

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec-bytecode-doubles</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

The core SPI (`ConcreteDoubleProvider`) is discovered through `ServiceLoader`. Without an adapter on
the runtime classpath, `Doubles.concreteDouble(Class<T>)` and alias `Doubles.classDouble(Class<T>)`
throw `IllegalStateException` with guidance. With the adapter present, ByteBuddy generates a
subclass for a non-final concrete class and delegates calls to the same `DoubleControl` stubbing and
verification semantics used by core doubles.

```java
import io.github.jvmspec.doubles.Doubles;
import io.github.jvmspec.doubles.InterfaceDouble;

InterfaceDouble<DataStore> storeDouble = Doubles.concreteDouble(DataStore.class);
storeDouble.control().returns("save", true);
beConstructedWith(storeDouble.instance());
match(subject().save("item")).shouldReturn(true);
```

The adapter is standalone, outside the root Maven reactor, and intentionally carries ByteBuddy
outside the zero-runtime-dependency core. It rejects final classes, enums, arrays, annotations,
primitives, and interfaces, and it does not mock static methods or constructors. See
`examples/bytecode-doubles-basic/`.

## Class-like type generation

javaspec supports these class-like production types. The javaspec binary remains Java 8-compatible;
post-Java-8 forms are generated as source text and represented as metadata/strings.

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

The command stays PHPSpec-like: `describe` does not take a type flag. To describe a non-class type,
edit the generated spec and add a marker example before running generation. During `run`, profile
enforcement allows Java 8-compatible kinds under `java8` and rejects `record`, `sealed class`, and
`sealed interface` unless the effective profile is at least `java17`.

When method descriptors are discovered before a missing class-like type is generated, Phase 10
enriches interface-style skeletons where valid: ordinary interfaces receive non-static method
declarations, annotations receive compatible no-argument elements, and sealed interfaces receive
root declarations plus generated nested permitted implementation bodies with Java default returns.
Existing class, final-class, sealed-class, enum, and record method-body generation remains
unchanged.

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

When related types are missing, `run` handles them before generating the owner type: it suggests or
creates their specs, then writes their production skeletons. For sealed classes, `shouldPermit(...)`
can create final permitted subtype specs that extend the sealed root. For sealed interfaces,
permitted implementations remain nested in the sealed interface source file; missing
sealed-interface skeletons generate those nested implementations with required default-return method
bodies, and existing sealed-interface sources can now be updated source-preservingly together with
their nested permitted implementations.

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

No production type skeleton is generated. If the spec describes constructors, static factories, or
supported missing methods, `run` may update the existing source according to the constructor policy
and method-generation confirmation rules. Existing ordinary interfaces can receive missing
declarations, existing annotations can receive compatible missing elements, and existing sealed
interfaces can receive root declarations plus nested permitted implementation method bodies
source-preservingly and idempotently.

If the class is available on the effective classloader or selected explicit classloader instead of
the source tree, javaspec reports:

```text
Classpath: present
```

After these existence and update checks, `run` executes the filtered examples when the compiled spec
class itself is available on the effective classloader, selected explicit classloader, or CLI
compile-output-first classloader. If only the source file is present and `--compile` was not
requested, the examples are listed as `SKIPPED` because default CLI runs do not compile them.

## No specs found

```sh
$javaspec run --spec-dir /tmp/empty-spec-root
```

Output:

```text
No specifications found in /tmp/empty-spec-root.
```

Exit code: `0`. Configured bootstrap hooks are not executed when no specs are discovered;
`--compile` / `--compile-output` requests also skip compilation, and requested no-spec JSON/JUnit
XML-compatible reports are still written as empty reports.

## Spec-to-class mapping

javaspec follows a PHPSpec-inspired namespace convention. The default convention uses spec package
prefix `spec` and an empty production package prefix; configured suites can replace both prefixes.

- **`src/test/java/spec/org/example/CalculatorSpec.java`**
  - Spec class: `spec.org.example.CalculatorSpec`
  - Support class: `spec.org.example.CalculatorSpecSupport`
  - Described production type: `org.example.Calculator`
- **`src/test/java/spec/com/acme/UserSpec.java`**
  - Spec class: `spec.com.acme.UserSpec`
  - Support class: `spec.com.acme.UserSpecSupport`
  - Described production type: `com.acme.User`
- **`src/test/java/spec/domain/CalculatorSpec.java` with `specPackagePrefix=spec.domain`,
  `packagePrefix=org.example`**
  - Spec class: `spec.domain.CalculatorSpec`
  - Support class: `spec.domain.CalculatorSpecSupport`
  - Described production type: `org.example.Calculator`

Rules:

1. The spec class name ends with `Spec`.
2. The generated support class name ends with `SpecSupport`.
3. The spec package starts with the active `specPackagePrefix`.
4. The described production package is the active `packagePrefix` plus the spec package suffix after
   `specPackagePrefix`. With the default empty production package prefix this is the spec package
   without the leading `spec.`.
5. The described production type name is the spec class name without the trailing `Spec`.
6. The described production kind defaults to class unless the spec contains a marker such as
   `shouldBeAFinalClass();`, `shouldBeAnInterface();`, `shouldBeAnEnum();`,
   `shouldBeAnAnnotation();`, `shouldBeARecord();`, `shouldBeASealedClass();`, or
   `shouldBeASealedInterface();`.
7. `shouldExtend(...)`, `shouldImplement(...)`, and `shouldPermit(...)` class literals are resolved
   through imports or the described production package.
8. Constructor and method descriptors are discovered heuristically from supported construction and
   typed proxy syntax: `beConstructedWith(...)` describes constructors; factory construction markers
   with string-literal Java-identifier names describe static factory methods; typed proxy calls
   using the expanded chained matcher names, throw-proxy calls, direct `subject().method(...)`, and
   simple setter calls describe instance methods where applicable. For interface/annotation
   described kinds, supported descriptors produce declarations or elements instead of method bodies;
   static descriptors are not inserted into ordinary interfaces, incompatible annotation descriptors
   are ignored, and existing sealed-interface source updates include nested permitted implementation
   bodies where supported.

Legacy same-package specs are also discovered by convention when the default production package
prefix is empty, but new specs generated by `describe` use the active suite naming convention.

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
$javaspec describe org.example.Calculator --compile
$javaspec describe org.example.Calculator --compile-output target/javaspec-classes
$javaspec describe org.example.Calculator --report target/javaspec-report.json
$javaspec describe org.example.Calculator --junit-xml target/javaspec-report.xml
```

Result examples:

```text
Error: The --generate option belongs to run; describe creates only a specification skeleton.
Error: The --dry-run option belongs to run; describe creates only a specification skeleton.
Error: The --formatter option belongs to run; describe does not execute examples.
Error: The --classpath option belongs to run; describe does not execute examples.
Error: The --compile option belongs to run; describe does not execute examples.
Error: The --compile-output option belongs to run; describe does not execute examples.
Error: The --report option belongs to run; describe does not execute examples.
Error: The --junit-xml option belongs to run; describe does not execute examples.
```

A config file used by `describe --config <file>` may contain report destinations; those values are
accepted as project defaults but ignored by `describe` because it does not execute examples or write
reports.

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

If external formatter providers are on the effective run classloader, their names appear in the
formatter valid-values list too.

### Profile compatibility violation

```sh
$javaspec run --profile java8 --generate
```

If discovery finds a spec that would generate a Java 17-only kind, or generated method signatures
using a resolvable later-JDK API owner for the selected profile, the run fails before writes:

```text
Profile compatibility error: sealed interface requires Java 17 but target profile is Java 8
Selected profile: java8 (Java 8)
Spec/type: spec.org.example.ShapeSpec -> sealed interface org.example.Shape
```

Exit code: `64`. No source files or reports are written.

### Bootstrap execution failure

```sh
$javaspec run --config javaspec.conf --report target/javaspec-report.json
```

If a configured hook cannot be loaded, does not implement `BootstrapHook`, lacks a public
no-argument constructor, or throws during execution, the run fails before examples and reports:

```text
Error: Bootstrap execution failed: Bootstrap hook 'org.example.SpecBootstrap' was not found: org.example.SpecBootstrap.
```

Exit code: `64`. No reports are written.

### Compiler unavailable

```sh
$javaspec run --compile
```

If the current runtime does not provide the JDK compiler API, the run fails before
bootstrap/examples/reports:

```text
Error: Java compiler is not available. Run javaspec with a JDK or omit --compile.
```

Exit code: `64`.

### Compilation failure

```sh
$javaspec run --compile --report target/javaspec-report.json --junit-xml target/javaspec-report.xml
```

If the current JDK compiler rejects any source under the effective source/spec roots, the run fails
before bootstrap/example execution and before report writing:

```text
Compilation failed:
  src/main/java/org/example/Calculator.java:5:16: error: illegal start of expression
```

Exit code: `1`. No JSON or JUnit XML-compatible reports are written.

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

- **`0`**: success, help, no specs found, existing/generated/updated targets, dry-run with no
  pending generation/update work, passed examples, or skipped/pending-only example runs
- **`1`**: missing production type or missing method update was not generated because the prompt was
  declined or input was unavailable; dry-run found pending generation/update work; opt-in CLI
  compilation failed; or executable examples failed/broke
- **`64`**: invalid command line usage, profile compatibility violation before generation/update
  writes, unavailable JDK compiler for `--compile`, or bootstrap execution failure before
  examples/reports
- **`70`**: I/O or security error while reading config, reading a classpath file, checking,
  writing/generated/compiled files, or writing a JSON/JUnit XML report

## Dependency policy

Runtime dependencies are not allowed for the core artifact. The repository test suite uses JUnit
only as a test-scope dependency; using javaspec specs, bootstrap hooks, the CLI runner, programmatic
invocation, the optional Maven plugin, the optional Gradle plugin, and JUnit XML-compatible reports
do not require JUnit in projects under test. The Phase 15 Maven plugin is a separate optional
artifact with Maven API/plugin annotations in `provided` scope, JUnit only in plugin `test` scope,
and a runtime tree containing the plugin plus compile-scope core `io.github.jvmspec:javaspec` only. The
Phase 16 Gradle plugin is a separate optional artifact with JUnit/TestKit only as plugin test
dependencies; its verified runtimeClasspath contains only core
`io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`. The Phase 17 JUnit Platform engine is a separate optional
artifact over the canonical javaspec runner; its runtime dependencies are isolated to the engine
artifact and do not enter the core runtime dependency tree. Projects that do not opt into the engine
keep the no-JUnit CLI/programmatic/Maven/Gradle execution paths. Bootstrap hooks are explicit
compiled Java classes plus ServiceLoader-discovered providers on the run classloader/classpath and
do not add scripts, package scanning, dependency resolution, or runtime dependencies. Opt-in
compilation uses the current JDK `javax.tools.JavaCompiler` API; it adds no runtime dependencies,
forks no `javac`, and remains explicit on CLI, programmatic, Maven, and Gradle paths. Phase 28
interface double matchers, throwing stubs, and answer callbacks remain JDK dynamic proxy features,
and Phase 37 bytecode doubles keep ByteBuddy isolated in the standalone adapter, not the core
runtime.

Check core runtime dependencies:

```sh
mvn dependency:tree -Dscope=runtime
```

Expected root output contains only the project artifact:

```text
io.github.jvmspec:javaspec:jar:0.1.0-SNAPSHOT
```

Check the standalone optional adapters through the aggregate script or separately when needed:

```sh
scripts/verify-all.sh
mvn -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
mvn -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime
```

Expected Maven plugin runtime scope contains the plugin plus compile-scope core
`io.github.jvmspec:javaspec` only. Expected Gradle plugin runtimeClasspath contains only core
`io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`. Expected JUnit Platform engine runtime scope contains core
`io.github.jvmspec:javaspec`, `org.junit.platform:junit-platform-engine`, `opentest4j`,
`junit-platform-commons`, and `apiguardian-api`, with no runtime `junit-jupiter`,
`junit-platform-launcher`, or `junit-platform-testkit`.

## Verification

Current verification after Phase 22:

- `LICENSE` is identical to `origin/main:LICENSE` with blob
  `b990d5492f3ef404ffc145890b83e51914351bb5`.
- `bash -n scripts/check-version-alignment.sh`, `bash -n scripts/verify-all.sh`, and `bash -n
  scripts/verify-examples.sh` passed; all three scripts are executable.
- `bash scripts/check-version-alignment.sh` passed with all checked versions aligned at
  `0.1.0-SNAPSHOT`.
- `git diff --check`, `git diff --cached --check`, and untracked whitespace checks passed.
- Effective POM generation passed for root, Maven plugin, and JUnit engine.
- Maven POM metadata checks for root, Maven plugin, and JUnit engine passed: MIT License, URL
  `https://opensource.org/licenses/MIT`, distribution `repo`, Mario Giustiniani email, and
  maintainer role.
- Gradle generated POMs `pluginMaven` and `javaspecPluginMarkerMaven` include MIT license and
  maintainer metadata.
- `mvn -q verify` passed with 386 tests, 0 failures, 0 errors, and 0 skipped.
- `mvn dependency:tree -Dscope=runtime` passed with root runtime containing no dependencies beyond
  `io.github.jvmspec:javaspec`.
- `mvn -q -Prelease-artifacts -DskipTests package` passed and found non-empty root main, sources,
  and javadoc jars.
- `mvn -q -DskipTests install` passed.
- Maven plugin and JUnit Platform engine `-Prelease-artifacts -DskipTests package` checks passed and
  found non-empty main, sources, and javadoc jars.
- Standalone Maven plugin `mvn -q verify` passed with 12 tests; standalone JUnit Platform engine
  `mvn -q verify` passed with 12 tests.
- Gradle plugin publication POM generation passed; Gradle plugin `clean test build` passed with 11
  tests and produced non-empty main/sources/javadoc jars; Gradle runtime dependencies contained only
  `io.github.jvmspec:javaspec:0.1.0-SNAPSHOT`.
- Full aggregate `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-all.sh` passed,
  covering version alignment, core verify, root audit, local install, Maven plugin verify/audit,
  JUnit engine verify/audit, Gradle plugin build/audit, and standalone examples verification.
- Phase 21 checks confirmed no core production/test Java changes, parsed the schema and golden
  reports, validated the golden JSON against `docs/schemas/run-report-v1.schema.json`, verified
  ignored generated example output directories, and passed
  `JAVASPEC_GRADLE_BIN=/tmp/gradle-8.8/bin/gradle scripts/verify-examples.sh`.
- Phase 22 checks passed targeted changed tests with 78 tests, root `mvn -q test` with 399 tests,
  root `mvn -q verify`, root runtime dependency audit, root install, standalone Maven plugin
  `verify` with 13 tests, standalone JUnit Platform engine `verify` with 13 tests, standalone Gradle
  plugin `clean test build` with 12 tests, adapter runtime dependency audits,
  `scripts/verify-examples.sh`, and `scripts/verify-all.sh`; Java 8 obsolete source/target warnings
  in Gradle were non-blocking.
- No tester files were modified and no publish/deploy/signing commands were run.
- Current release evidence should come from `./scripts/verify-all.sh`, targeted tests for changed
  areas, and GitHub Actions for the commit being released. Historical phase confirmations remain in
  `docs/test-report.md` but are not a substitute for current verification.
- Stable ids/source metadata, JSON reports, JUnit XML-compatible reports, CLI reports, Maven plugin
  reports, Gradle plugin reports, and the optional JUnit Platform adapter are covered by regression
  tests.
- Cross-JDK and adapter verification should be read from the latest CI/local verification output.
- Artifacts are published on Maven Central under `io.github.jvmspec`. The Gradle plugin is published
  on the Gradle Plugin Portal with plugin id `io.github.jvmspec`.

See [`../test-report.md`](../test-report.md) for the consolidated test and quality report.

## Future backlog

The active roadmap is tracked in `PLAN.md`. Recent completed work keeps the project PHPSpec-first while adding JUnit-level usefulness: optional build-tool/JUnit Platform adapters, explicit skip/pending semantics, richer doubles, opt-in compilation, report metadata, PHPSpec-style example data, row-aware reporting, record evolution, and parser/generator signature hardening.

Potential backlog items include deeper compiler-backed semantic attribution, diagnostics for ambiguous overload/null cases, plugin lookup beyond ServiceLoader, script/package-scanning bootstrap activation, richer failure-location diagnostics, automatic classpath repair, richer dependency resolution beyond the local-POM resolver, publishing/signing automation after signing/portal/final-approval decisions, and any future multi-module conversion decision. No-JUnit CLI/programmatic/Maven/Gradle paths remain first-class; the JUnit Platform engine remains an optional adapter.

## Current limitations

- Default CLI runs, programmatic invocation, optional Maven/Gradle plugin adapters, and the optional
  JUnit Platform engine remain classpath-based unless compilation is explicitly enabled. Source-only
  or otherwise unavailable spec classes are skipped/not executable until compiled classes are present
  on the effective classloader, selected explicit classloader, build-tool adapter classpath, CLI
  compile output, or JUnit Platform engine runtime classpath. Source-only CLI runs can opt into
  current-JDK compilation with `--compile` or `--compile-output <dir>`, can request `--release <N>`,
  and can add local Maven repository dependencies with `--resolve-pom <pom.xml>`. Compilation keeps
  an incremental cache for unchanged source inputs, but still does not fork `javac`, add compiler
  dependencies, change adapter behavior, or alter report schemas. `--classpath`, `--classpath-file`,
  resolved POM entries, and adapter-supplied entries still must point to already compiled classes or
  archives. The optional Maven plugin, Gradle plugin, and JUnit Platform engine supply integration
  paths but remain standalone artifacts that are not covered by repository-root `mvn verify`; verify
  them through `scripts/verify-all.sh` or separately after installing the current core. Phase 21
  examples are standalone consumer projects. Public artifacts are available on Maven Central under
  `io.github.jvmspec`.
- The runner lifecycle is intentionally small: configured bootstrap hooks run before examples, then
  each executable example uses a fresh spec instance plus optional public no-arg `let()` and
  `letGo()`. Explicit skipped/pending semantics are implemented.
- JSON reporting remains the Phase 11 `schemaVersion` 1 runner report with Phase 18 additive stable
  id/source fields, Phase 22 additive `pending` counts/statuses, and Phase 35 optional run-level
  metadata/properties from the default writer; older schemaVersion 1 JSON reports without metadata
  remain valid. Phase 14 JUnit XML-compatible reporting remains intentionally minimal with Phase 18
  additive testcase `file`/`line` attributes, Phase 22 skipped/pending mapping, and Phase 35
  testsuite metadata/properties. Phase 24 configuration-level report destinations configure output
  paths only; exit semantics and dry-run pending generation/update behavior remain unchanged.
- Configuration files currently drive selected suite paths, package-prefix naming,
  constructor-policy defaults, profile enforcement defaults, formatter defaults, extension
  activation, executable bootstrap hook class names, optional JSON/JUnit XML-compatible report
  destinations, and run class/example filters. CLI compilation, release selection, and local-POM
  dependency resolution are command-line run options. Bootstrap hooks must be compiled Java classes
  on the run classloader/classpath or ServiceLoader providers; javaspec does not add scripts,
  package scanning, automatic classpath repair, or dependency resolution beyond explicit
  `--resolve-pom` local-repository lookup.
- The extension API is minimal: extensions can receive `ExtensionContext` and register run
  formatters, and entry points can discover `RunFormatter`, `JavaspecExtension`, or alias
  `Extension` providers with JDK `ServiceLoader` where implemented. Discovery is classpath-based
  only; package scanning, plugin lookup, and automatic classpath repair remain unimplemented.
- Source parsing and generation use Java 8-compatible heuristics, not a full Java parser. Profile
  enforcement is conservative: it checks described kinds and resolvable cataloged API signature
  owners, but it ignores unknown project types and ambiguous/unresolvable simple names.
- Generated post-Java-8 source forms, such as records and sealed types, require an appropriate JDK
  to compile.
- Method generation covers the supported typed proxy, throw-proxy, direct subject/setter, and static
  factory construction marker syntax; it can emit method bodies for class-like body-bearing types,
  declarations for ordinary interfaces, compatible elements for annotations, and missing
  sealed-interface skeleton declarations plus nested permitted implementation bodies. It is not a
  general Java source synthesis engine.
- Doubles/collaborators are interface-only in the core runtime. Optional non-final concrete-class
  doubles require the standalone `javaspec-bytecode-doubles` subclass adapter. Optional final-class,
  static-method, and construction-aware doubles require the separate `javaspec-bytecode-agent`
  adapter and JVM instrumentation support. Primitive, array, annotation, enum, and inappropriate
  interface targets remain unsupported by concrete-class adapters.
- Double argument matching supports explicit `ArgumentMatcher` values in existing vararg APIs while
  ordinary exact values, `null`, and arrays remain supported. It is not a bytecode or concrete-class
  mocking facility.
- Stubbing supports return values, sequential returns, throwing stubs, and answer callbacks.
  Automatic side-effect orchestration is not implemented.
- Default interface methods are invoked for unstubbed interface-double calls; explicit stubs still take precedence.
