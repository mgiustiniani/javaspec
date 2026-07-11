# Generation and parser robustness matrix

This matrix tracks high-value edge cases for javaspec source discovery, skeleton generation, and
source-preserving updates. It complements `docs/test-report.md` and should be updated whenever new
language forms or generator behavior are added. The release-hardening inventory, per-LTS solutions,
and stable exit criteria are defined in [`docs/java-language-coverage-roadmap.md`](java-language-coverage-roadmap.md).

## Current hardening focus

| Area | Current coverage | Robustness target |
|---|---|---|
| Class/final class | Skeletons, constructors, method insertion, static factories, idempotence | More real-world formatting, annotations, nested types, generic methods |
| Enum | Skeletons, constants, implicit enum method filtering, semicolon insertion | More constructor/constant-argument combinations |
| Annotation | Skeletons, compatible elements, incompatible descriptor filtering | More array/class-valued element combinations |
| Record | Skeletons, Java 17 profile gating, method insertion, component accessors, constructor-driven component evolution | Compact constructors preserved, generic/annotated components, dry-run parity for header evolution |
| Sealed class | Skeletons, explicit `permits`, Java 17 profile gating, method-body insertion into root | Multiline permits, nested permitted classes, Java 17 compile checks |
| Sealed interface | Skeletons, explicit permits, nested permitted implementations, source-preserving existing updates | Additional nested generic/default-method cases |
| Parser | Comment/string/text-block masking, brace matching, direct-member scoping, ServiceLoader replacement | Generic method type parameters, annotations, varargs, multiline declarations, and fail-closed SAM target inference for lambda arguments in specs |
| End-to-end CLI | Generate/compile/report paths for core flows; Java 17 record/sealed `run --generate --compile --release 17` fixture | Additional dry-run assertions for complex generated-source combinations |

## PHPSpec compatibility focus

| PHPSpec area | Current coverage | Parity target |
|---|---|---|
| Examples | `it_*` / `its_*` discovery, filters, stable IDs, skip/pending states, inline `examples(row(...)).verify(...)` data rows | Row-level report/formatter representation without Jupiter parameterized syntax |
| Lifecycle | Fresh spec instance per example, `let()`, `letGo()`, construction helpers | Parameterized `let(...)` / examples for collaborator injection |
| Subject construction | `subject()`, `beConstructedWith`, named/static factory construction | Clearer diagnostics and snippet coverage for ambiguous constructors/factories |
| Matchers | Core `should*` / `shouldNot*` equality, type, approximate numeric, generated object-state, count, string, collection/map/iterable/iterator helpers | Inline and configured custom matchers |
| Prophecy | Interface/concrete/final wrappers, promises, predictions, reveal, auto-check support | Full argument-token parity and custom prediction callbacks |
| Generation | Spec/support/type/method/constructor/static factory generation and dry-run planning | Safer PHPSpec snippets, template overrides, and anti-batching guardrails |
| Formatters/reports | `progress`, `pretty`, JSON, JUnit XML, ServiceLoader formatters | Optional TAP/TeamCity/HTML/Open Test Reporting adapters |
| Cucumber boundary | No feature files or Given/When/Then DSL in core | Boundary guide and low-value Scenario Outline migration to example data |

## Acceptance rules for new generator work

1. Updating an existing source file must be idempotent.
2. Signatures in comments, string literals, and char/text blocks must not suppress generation.
3. Generated or updated Java 17-only forms should compile under `--release 17` when the host JDK
   supports it.
4. Java 8 compatibility of the javaspec binary must remain intact; post-Java-8 APIs stay out of the
   core runtime surface. Optional compiler-tree APIs must either be supplied explicitly on Java 8
   verification classpaths or fall back safely at runtime.
5. Dry-run behavior must be updated together with any new mutating generation/update path.
6. Profile enforcement must reject record/sealed generation below Java 17 before writes.
7. Every final construct relevant to a supported LTS profile must be classified as generated, updated,
   preserved, profile-gated, or intentionally unsupported; undocumented assumed support is not a
   release claim.
8. Modern-Java coverage must preserve the classic PHPSpec-inspired subject-centric workflow: one
   visible behavior, meaningful RED, mechanical scaffolding only, and no accidental GREEN.

## Recently added hardening

- JLC-0/JLC-1 add a deterministic Java 8/11/17/21/25 construct manifest, progressive strict coverage
  gate, per-JDK CI fixture step, and source-preservation fixtures. Text blocks are masked with stable
  offsets, and direct-member detection ignores matching methods inside local, anonymous, nested, or
  secondary top-level types.
- JLC-2 adds release-8 update/compile/idempotence fixtures for all supported class-like kinds,
  lambda/method-reference preservation, default/static interface methods, type-use/repeatable
  annotations, and complex generic/array/varargs source. Inferring a missing subject signature from
  a lambda remains planned and must fail closed unless the SAM target is explicit and unambiguous.
- JLC-3 covers Java 11 local/lambda `var`, private interface helpers, diamond anonymous classes,
  effectively-final try-with-resources, byte-preserved module descriptors, and representative
  collection/HTTP/file API consumer execution without changing subject-centric spec authoring.
- JLC-4 covers Java 17 record updates, multi-file sealed and top-level non-sealed hierarchies,
  abstract-subject preservation, text blocks, switch expressions, `instanceof` patterns, and
  representative stream/hex/time/random API execution. Abstract or non-sealed application design is
  preserved rather than invented by generation.
- JLC-5 covers Java 21 nested record patterns, type/record pattern switches, guarded `when` blocks,
  hash-stable multi-file sealed exhaustiveness, Sequenced Collections execution, and a joined virtual
  thread that terminates before the behavior returns.
- JLC-6 covers Java 25 unnamed variables/patterns, flexible constructor bodies, module imports,
  Markdown documentation comments, and Stream Gatherers. Compact source files compile as project
  files but are refused fail-closed as javaspec subjects before any support/source write, with a
  diagnostic recommending a named subject type.
- Record component accessors are treated as existing no-arg methods during source-preserving record
  updates, so `record UserId(String value)` does not receive a generated `value()` stub.
- Constructor-driven record specs can evolve record headers: `beConstructedWith(...)` plus a
  component accessor such as `value()` can update `record CertificateProfileId()` to
  `record CertificateProfileId(String value)` instead of failing with `No matching constructor`.
- Record updates preserve compact constructors and tolerate generic/annotated component declarations.
- Existing sealed classes with multiline `permits` and nested permitted subclasses receive missing
  root method bodies idempotently and are checked with Java 17 compilation when available.
- The built-in comment-stripping parser recognizes generic method declarations with nested type
  parameter bounds and annotated varargs parameters.
- CLI `run --generate --compile --release 17` is covered for generated record and sealed-class
  skeletons, including execution of the resulting specs and class-file major-version checks.
- Phase 47 example-data slice adds inline `row(...)` / `examples(...)` coverage for Java 8-compatible
  one- and two-column data rows with failing-row diagnostics.
