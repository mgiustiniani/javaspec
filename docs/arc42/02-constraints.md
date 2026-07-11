# 2. Constraints

## 2.1 Technical Constraints

- **Java 8 baseline**: Production code must compile with Java 8 source/target compatibility and run
  on a Java 8 runtime.
- **No runtime dependencies**: The core runtime artifact must depend only on the JDK; optional
  adapter artifacts must not leak third-party dependencies into core.
- **Core test-scope dependencies only**: External dependencies are allowed in the core build only
  for tests or build-time verification and must not leak into the core runtime artifact; standalone
  optional adapters may have their own isolated dependencies.
- **Maven**: The project uses Maven while preserving Java 8 bytecode compatibility; repository-root
  `mvn verify` is intentionally core-only, standalone optional Maven, Gradle, and JUnit Platform
  adapter artifacts plus Phase 21 example projects are intentionally not root Maven modules, and
  Maven `release-artifacts` profiles produce local sources/javadocs only.
- **Package base**: Production code uses package base `io.github.jvmspec`.
- **Post-Java 8 APIs**: APIs introduced after Java 8 must be represented as metadata, strings, or
  reflected conditionally; production source must not import them directly.
- **LTS profiles**: The system must model Java LTS profiles for 8, 11, 17, 21, and 25 and enforce
  selected profiles before generation/update writes without breaking the Java 8 binary baseline.
- **Restricted configuration parser**: Configuration files must be parsed by an internal line-based
  parser; no YAML/TOML/JSON parser may be added to the runtime artifact.
- **Java 25 stream metadata**: Java 25 stream gatherer metadata is implemented from verified
  API-documentation research, but runtime availability must remain metadata/reflection-only and be
  re-validated during quality-matrix work.
- **Doubles boundaries**: Core doubles must use Java 8 JDK dynamic proxies and therefore support
  ordinary interfaces only; argument matchers, throwing stubs, and answer callbacks must stay within
  that zero-dependency boundary. Optional non-final concrete-class doubles live in the standalone
  `javaspec-bytecode-doubles` adapter with isolated ByteBuddy dependency.
- **No-JUnit core execution**: Programmatic invocation, CLI execution, ServiceLoader
  formatter/extension discovery, skipped/pending semantics, JSON reports, JUnit XML-compatible
  reports, opt-in CLI source/spec compilation, and optional Maven/Gradle plugin execution must not
  add JUnit or third-party runtime dependencies to the core artifact or require JUnit in projects
  under test; the optional JUnit Platform engine must keep JUnit Platform dependencies isolated to
  its standalone artifact.
- **Opt-in compilation boundary**: Source/spec compilation is explicit opt-in on CLI, programmatic,
  Maven, and Gradle entry points where implemented, uses the current JDK `javax.tools.JavaCompiler`,
  and must not fork `javac`, add core runtime dependencies, resolve dependencies, manage
  source/release levels, maintain incremental caches, or change default classpath-based behavior.

## 2.2 Organizational Constraints

- Documentation phases and documenter-delegated tasks are documentation-only.
- Source code, build-file, scaffolding, and test changes must be delegated to implementation or
  testing agents.
- Architectural decisions must be documented as ADRs when made.
- Legal/product release metadata must not be invented. The MIT license and maintainer `Mario
  Giustiniani <mariogiustiniani@gmail.com>` are confirmed; artifacts are published on Maven Central
  under `io.github.jvmspec`
  until GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials, final
  release version/tag, and final publish approval are resolved.
- Generated documentation must be written in English.

## 2.3 Design Constraints

- The core runtime cannot depend on external assertion libraries, mocking libraries, YAML parsers,
  logging frameworks, dependency injection containers, or bytecode-generation libraries.
- Advanced features that normally require third-party dependencies must be implemented using JDK
  APIs, exposed as optional extensions/adapters outside the core runtime, or deferred.
- Core doubles are ordinary interface-only JDK dynamic proxies; Phase 28 adds explicit argument
  matchers, throwing stubs, and answer callbacks. Phase 37 permits non-final concrete-class doubles
  only through the standalone optional bytecode adapter. Final class doubles, static doubles,
  constructor doubles, and default-interface-method invocation remain unsupported.
- Reflection must be isolated behind compatibility boundaries to avoid accidental linkage to newer
  JDK APIs.
- Configuration bootstrap hooks are executable class names for `run`, not metadata-only values:
  hooks must implement `io.github.jvmspec.bootstrap.BootstrapHook`, have a public no-argument
  constructor, load from the effective run classloader/classpath, and execute immediately before
  examples; top-level hooks run before selected-suite hooks, preserving order and duplicates.
  Profile and formatter settings are active `run` selections with CLI overrides. Selected profiles
  are enforced before generation/update writes for described type kinds and resolvable cataloged
  Java API signature owners, but enforcement is not integrated compilation and ignores unknown
  project types plus ambiguous or unresolvable type names. Formatter names may be built-in or
  ServiceLoader-discovered from the effective run classloader. Suite package prefixes are active
  naming-convention inputs for `describe`, `run`, discovery, spec/support generation, bootstrap
  context, and MVP reflection execution. Bootstrap execution may use ServiceLoader hook discovery
  after explicit hooks, but must not add script engines, package scanning, dependency resolution,
  runtime dependencies, or Java 8 compatibility violations.
- Default runs remain classpath-based; executable examples require compiled spec classes on the
  effective, selected explicit, build-tool, programmatic, JUnit Platform launcher, or
  compile-output-first classloader, while source-only or unavailable spec classes are skipped with
  execution-availability diagnostics when applicable. CLI, programmatic, Maven, and Gradle entry
  points may explicitly opt into current-JDK compilation before bootstrap/examples; no-spec and
  dry-run paths skip compilation. The JUnit Platform engine remains host-classpath based.
- Dry-run mode must not write files, prompt, or compile, and run-only controls, including explicit
  classpath, compilation, and report options, must be rejected by `describe`/`desc`.
- User-facing diagnostics should explain zero-dependency and external-compilation limitations
  clearly.
- Aggregate release verification must use explicit scripts/CI workflow configuration rather than
  changing the root Maven reactor unless a future ADR decides otherwise.
- Release-readiness scaffolding must stay local and non-publishing until explicit owner decisions
  provide GPG signing, Central Portal publication, Gradle Plugin Portal publication/credentials,
  final release version/tag, and final publish approval.
- Standalone examples and report schema/golden docs are adoption assets only; they must not be
  treated as root build modules, public publication, deployment, signing, or remote CI proof.
- Explicit skipped/pending semantics must preserve the zero-runtime-dependency core and Java 8
  baseline; pending may be mapped to skipped only in JUnit-compatible formats that lack a distinct
  pending status.

## 2.4 Compatibility Constraints

- Java 8 is the binary compatibility floor.
- Java 11, 17, 21, and 25 are target profiles, not separate binaries unless a future ADR decides
  otherwise.
- The profile catalog and profile enforcement must avoid assuming the presence of Java 9+ classes
  while running on Java 8.
- The build and test matrix should include Java 8 plus each supported LTS runtime where available.
