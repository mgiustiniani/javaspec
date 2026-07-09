# Generation and parser robustness matrix

This matrix tracks high-value edge cases for javaspec source discovery, skeleton generation, and
source-preserving updates. It complements `docs/test-report.md` and should be updated whenever new
language forms or generator behavior are added.

## Current hardening focus

| Area | Current coverage | Robustness target |
|---|---|---|
| Class/final class | Skeletons, constructors, method insertion, static factories, idempotence | More real-world formatting, annotations, nested types, generic methods |
| Enum | Skeletons, constants, implicit enum method filtering, semicolon insertion | More constructor/constant-argument combinations |
| Annotation | Skeletons, compatible elements, incompatible descriptor filtering | More array/class-valued element combinations |
| Record | Skeletons, Java 17 profile gating, method insertion | Component accessors treated as existing methods, compact constructors preserved, generic components |
| Sealed class | Skeletons, explicit `permits`, Java 17 profile gating, method-body insertion into root | Multiline permits, nested permitted classes, Java 17 compile checks |
| Sealed interface | Skeletons, explicit permits, nested permitted implementations, source-preserving existing updates | Additional nested generic/default-method cases |
| Parser | Comment/string masking, brace matching, ServiceLoader replacement | Generic method type parameters, annotations, varargs, multiline declarations |
| End-to-end CLI | Generate/compile/report paths for core flows; Java 17 record/sealed `run --generate --compile --release 17` fixture | Additional dry-run assertions for complex generated-source combinations |

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

## Recently added hardening

- Record component accessors are treated as existing no-arg methods during source-preserving record
  updates, so `record UserId(String value)` does not receive a generated `value()` stub.
- Record updates preserve compact constructors and tolerate generic/annotated component declarations.
- Existing sealed classes with multiline `permits` and nested permitted subclasses receive missing
  root method bodies idempotently and are checked with Java 17 compilation when available.
- The built-in comment-stripping parser recognizes generic method declarations with nested type
  parameter bounds and annotated varargs parameters.
- CLI `run --generate --compile --release 17` is covered for generated record and sealed-class
  skeletons, including execution of the resulting specs and class-file major-version checks.
