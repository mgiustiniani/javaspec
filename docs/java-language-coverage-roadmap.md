# Java language coverage roadmap

This roadmap closes the gap between javaspec's Java 8/11/17/21/25 runtime/profile matrix and
end-to-end evidence that modern Java source is generated, updated, or preserved safely. It is a
release-hardening plan, not a mandate to generate every Java grammar production.

The product identity remains the subject-centric SpecBDD model inspired by classic PHPSpec:

- one described subject and one visible behavior at a time;
- a meaningful RED before minimal GREEN;
- mechanical generation only, never invented domain behavior;
- typed generated support with an always-available explicit `subject()` equivalent;
- fail-closed source updates when intent or source structure is ambiguous;
- no Gherkin, Jupiter, or AI authoring model added to the core to solve language compatibility.

## Coverage contract

Every final language construct relevant to a supported LTS profile must have an explicit disposition:

| Disposition | Required evidence |
|---|---|
| `GENERATED` | javaspec can create the construct, compile it with the matching `--release`, execute the behavior, and regenerate idempotently. |
| `UPDATED` | javaspec can make its documented mechanical change without damaging the construct; dry-run and mutation agree; a second update is empty. |
| `PRESERVED` | javaspec does not own the construct but parser/discovery/update passes leave it unchanged outside the requested edit. |
| `PROFILE_GATED` | an older profile refuses before writes and the first supporting profile accepts it. |
| `INTENTIONALLY_UNSUPPORTED` | javaspec refuses clearly or ignores the file safely; public documentation does not imply support. |

“Supports Java 25 source” therefore means all relevant final constructs through Java 25 have one of
these tested dispositions. Preview features are excluded unless a future explicit preview profile is
added.

## Common fixture protocol

Create `src/test/resources/java-language-matrix/<release>/<fixture>/` with initial source, expected
updated source, and minimal spec input. A shared JUnit 4 harness should perform, where applicable:

1. parse/discover the initial source;
2. produce a dry-run plan and assert no writes;
3. apply exactly one mechanical generation/update step;
4. compare the updated source and untouched regions;
5. compile with `javac --release <release>`;
6. run the selected subject-centric behavior;
7. rerun generation/update and assert byte-for-byte idempotence;
8. run the same request under the preceding profile and assert refusal before writes when gated.

Each fixture should exercise a normal typed proxy expression and retain an explicit equivalent such
as `match(subject().method()).shouldReturn(...)` in its diagnostic or companion evidence. Generated
`// javaspec:stub` markers must continue to prevent accidental GREEN.

## JLC-0 — Harness, inventory, and truthful claims

**Priority:** P0 before stable.
**Goal:** make missing coverage machine-visible rather than relying on prose or incidental tests.

Solutions:

- Add a deterministic fixture manifest containing release, construct, disposition, evidence status,
  scenario, fixture path, and description.
- Fail when a declared construct has no manifest row or disposition. `COVERED` rows require an
  executable fixture; `PLANNED` rows remain machine-visible while later JLC milestones are active,
  and strict stable mode fails if any `PLANNED` row remains.
- Keep runtime CI on Java 8/11/17/21/25 and add a language-fixture step on the matching JDK.
- Run only fixtures whose release is supported by the active compiler; the CI matrix guarantees that
  every profile is exercised by its own JDK.
- Separate API catalog evidence from language syntax evidence in release documentation.
- Generate a compact coverage report listing `PASS`, `REFUSED`, or `NOT_APPLICABLE`, without changing
  normal javaspec result semantics.

Acceptance:

- Every item below appears in the manifest.
- `scripts/verify-all.sh` runs the host-supported fixture suite as part of root `mvn verify`.
- CI proves all five LTS fixture partitions.
- Status: IMPLEMENTED — the test-only manifest/harness, first Java 8 update fixture, deterministic
  report, non-strict development mode, strict stable switch, and per-JDK CI step are present.

## JLC-1 — Parser and preservation hazards

**Priority:** P0 before stable.
**Goal:** prevent false methods, false braces, and destructive insertion.

| Element | Current state | Proposed solution |
|---|---|---|
| Text blocks | `COVERED` | Offset-preserving masking has an explicit text-block state; fake `}`, comments, signatures, single quotes, and escaped quote sequences stay non-code. The matrix verifies update, release-17 compilation, and idempotence. |
| Lambda blocks | `COVERED` | Expression/block lambda source is preserved while the requested direct subject member is inserted and compiled at release 8. |
| Method references | `COVERED` | Constructor and unbound references are preserved by the matrix update/compile/idempotence protocol; remaining reference shapes stay in the Java 8 expansion milestone. |
| Local/anonymous classes | `COVERED` | Direct-member scoping prevents matching local or anonymous methods as subject members. |
| Multiple top-level types | `COVERED` | Target-body scoping ignores matching methods on a secondary top-level type and updates only the described type. |
| Comments/literals | `COVERED` | Line/block comments, strings, chars, and text blocks with fake signatures/braces are masked without changing structural offsets. |

Implementation rule: prefer the available javac-tree parser for structural identity; harden the
zero-dependency fallback for safe masking and fail closed when it cannot identify one unambiguous
scope.

Status: IMPLEMENTED — shared updater masking now handles text blocks, direct-member detection is
scope-safe, parser unit regressions cover text-block delimiters/offsets, and seven manifest rows are
`COVERED` across the JLC-0/JLC-1 fixture set.

## JLC-2 — Java 8 baseline structures

**Priority:** P0 because Java 8 is the core runtime/source floor.

| Element | Disposition | Proposed solution |
|---|---|---|
| Class/final class/interface/enum/annotation | `UPDATED` — `COVERED` | Shared fixtures discover each type, apply its legal member shape, compile at release 8, and assert second-pass byte idempotence. Existing skeleton-generation tests remain complementary evidence. |
| Lambdas in existing subject source | `PRESERVED` — `COVERED` | Fixtures cover inferred/explicit parameter types, expression/block bodies, captures, nested calls, update, compilation, and idempotence. |
| Lambda arguments in specs for missing subject methods | `GENERATED` — `COVERED` | The parse-only AST retains expression/block lambdas. Explicit generic/raw casts and explicitly typed locals/parameters provide deterministic standard or application-SAM types; one unambiguous existing production signature refines an inline lambda. Missing or overloaded-ambiguous targets refuse before source/support writes with assign-or-cast guidance. The fixture compiles the generated Java 8 subject, typed proxy, and classic spec; checks explicit `subject()` equivalence, visible stubs/meaningful RED, and byte-idempotence. |
| Method references | `PRESERVED` — `COVERED` | The fixture covers static, bound, unbound, constructor, and array-constructor references through update and release-8 compilation. |
| Default/static interface methods | `UPDATED`/`PRESERVED` — `COVERED` | A missing direct declaration is inserted without duplicating or rewriting existing default/static bodies. |
| Type-use and repeatable annotations | `PRESERVED` — `COVERED` | Type-use generic arguments and repeated type declarations survive update, release-8 compilation, and idempotence; focused signature tests cover additional shapes. |
| Nested/local/anonymous types | `PRESERVED` — `COVERED` | Scope-identity fixtures ensure nested matches cannot satisfy the described subject. |
| Generic bounds, wildcards, arrays, varargs | `PRESERVED` — `COVERED` | A combined source fixture survives update and release-8 compilation; focused generator/Prophecy tests retain complex generated-signature evidence. |

Status: IMPLEMENTED — all Java 8 preservation, type-shape, and spec-lambda target-inference rows
are `COVERED`. Functional targets are inferred only from explicit typing or one unambiguous
production signature; javaspec does not invent an application SAM.

## JLC-3 — Java 11 profile structures

**Priority:** P0 for a truthful Java 11 profile; most work is preservation rather than generation.

| Element | Disposition | Proposed solution |
|---|---|---|
| Local `var` | `PRESERVED` — `COVERED` | Discovery/update/idempotence fixture compiles with release 11. |
| `var` lambda parameters | `PRESERVED` — `COVERED` | Annotated and unannotated `var` parameters survive update and release-11 compilation. |
| Private interface methods | `PRESERVED` — `COVERED` | Direct declaration insertion leaves the private helper and default caller unchanged. |
| Diamond with anonymous classes | `PRESERVED` — `COVERED` | An anonymous diamond override matching the requested subject signature cannot suppress the direct subject update. |
| Effectively-final try-with-resources | `PRESERVED` — `COVERED` | Resource declaration and try body remain unchanged through update and release-11 compilation. |
| `module-info.java` | `INTENTIONALLY_UNSUPPORTED` as a subject; `PRESERVED` as project source — `COVERED` | Discovery ignores the descriptor, its hash remains unchanged, and javac compiles it with the updated named subject. |
| Java 11 APIs | `PROFILE_GATED` plus consumer evidence — `COVERED` | The fixture compiles and executes collection factories, `HttpClient` construction without network access, and `Files.readString`; existing enforcement tests reject older profiles. |

Status: IMPLEMENTED — all Java 11 manifest rows are `COVERED`. Per-JDK CI remains the authoritative
runtime proof on an actual Java 11 VM; newer hosts compile these fixtures with `--release 11`.

## JLC-4 — Java 17 profile structures

**Priority:** P0; records/sealed are central Java adaptations already promised by 1.0.

| Element | Disposition | Proposed solution |
|---|---|---|
| Records | `UPDATED` — `COVERED` | The shared fixture preserves components through direct-member update, release-17 compilation, and idempotence. Focused tests retain canonical/compact/auxiliary/generic/annotated/header-evolution/Javadoc evidence. |
| Sealed class/interface | `UPDATED` — `COVERED` | A multi-file sealed hierarchy is updated and compiled while its permitted companion remains hash-stable; focused tests retain sealed-interface and nested/multiline evidence. |
| Top-level `non-sealed` type | `PRESERVED` — `COVERED`; generation decision deferred | A non-sealed subject remains linked to its sealed base through update and release-17 compilation. No new generator kind is introduced before stable. |
| Abstract class | `PRESERVED` — `COVERED`; generation decision deferred | Abstract domain members remain unimplemented while an independent mechanical stub is added and compiled. |
| Text blocks | `PRESERVED` — `COVERED` | JLC-1 masks adversarial content and compiles the unchanged block at release 17. |
| Switch expressions/`yield` | `PRESERVED` — `COVERED` | Arrow/block arms, nested braces, `yield`, and fake signatures survive update and release-17 compilation. |
| Pattern matching for `instanceof` | `PRESERVED` — `COVERED` | Scoped pattern variables and compound conditions remain unchanged through update and compilation. |
| Java 17 APIs | `PROFILE_GATED` plus consumer evidence — `COVERED` | `Stream.toList`, `HexFormat`, `InstantSource`, and `RandomGenerator` compile and execute; existing enforcement tests cover older-profile rejection. |

Status: IMPLEMENTED — every Java 17 manifest row is `COVERED`; generation remains deliberately
narrower than preservation where abstract/non-sealed application design would otherwise be invented.

## JLC-5 — Java 21 profile structures

**Priority:** P0 before stable because real dogfooding uses Java 21.

| Element | Disposition | Proposed solution |
|---|---|---|
| Record patterns | `PRESERVED` — `COVERED` | Simple and nested deconstruction patterns survive update, release-21 compilation, and idempotence. |
| Pattern switch | `PRESERVED` — `COVERED` | Type/record patterns, `case null`, default arms, and arrow expressions remain unchanged through update and compilation. |
| Guarded `when` cases | `PRESERVED` — `COVERED` | Guards and block arms containing fake method text/braces remain parser-safe and compile on Java 21. |
| Record + sealed exhaustiveness | `PRESERVED`/`UPDATED` — `COVERED` | A multi-file sealed event hierarchy and exhaustive record switch recompile unchanged after one direct subject update; the companion stays hash-stable. |
| Sequenced collections | `PROFILE_GATED` plus end-to-end use — `COVERED` | `SequencedCollection` signatures and reversed views compile/run on 21; existing enforcement tests reject the API below 21. |
| Virtual threads | `PRESERVED` plus runtime consumer — `COVERED` | A deterministic joined virtual worker reports `isVirtual=true` and `isAlive=false`; generation remains outside scheduling policy. |

Status: IMPLEMENTED — every Java 21 manifest row is `COVERED`, including execution on the local
Java 21 runtime. The fixtures preserve subject-centric examples and do not move concurrency or
pattern design decisions into the framework.

## JLC-6 — Java 25 profile structures

**Priority:** P0 for source preservation and profile truth; generation of new Java 25 application
styles is not required for the classic PHPSpec subject model.

| Element | Disposition | Proposed solution |
|---|---|---|
| Unnamed variables and patterns | `PRESERVED` — `COVERED` | Unnamed catch/loop variables and nested unnamed record patterns survive update, release-25 compilation, and idempotence. |
| Flexible constructor bodies | `UPDATED`/`PRESERVED` — `COVERED` | Validation and normalization before `super(...)` retain order; matching constructor reconciliation is byte-stable; the base companion remains hash-stable; release-25 compilation passes. |
| Module import declarations | `PRESERVED` — `COVERED` | `import module java.base` and its unqualified API use survive update, compilation, and idempotence; javaspec does not synthesize module imports. |
| Compact source files and instance `main` | `INTENTIONALLY_UNSUPPORTED` as described subjects; `PRESERVED`/ignored as project files — `COVERED` | Compact source compiles, but generation refuses before support/source writes with an actionable named-subject diagnostic and leaves the file unchanged. |
| Markdown documentation comments | `PRESERVED` — `COVERED` | `///` comments containing braces, method-like text, links, and code spans remain byte-stable outside the requested insertion. |
| Stream Gatherers | `PROFILE_GATED` plus end-to-end use — `COVERED` | `Gatherers.fold` compiles and executes on Java 25; existing enforcement tests reject Gatherer signatures below 25. |

Preview features, including any Java 25 preview pattern extension, are `INTENTIONALLY_UNSUPPORTED`
for 1.0. They may be tested for safe refusal but must not be accepted through the stable `java25`
profile merely because the host compiler enables preview syntax.

Status: IMPLEMENTED — every Java 25 manifest row is `COVERED` and executed on the local Java 25
runtime. New application syntax is preserved or refused explicitly; it does not alter the classic
PHPSpec-inspired subject model.

## JLC-7 — Cross-cutting source fidelity

**Priority:** P0 before stable.

| Element | Proposed solution |
|---|---|
| CRLF | `COVERED` — generated insertion adopts CRLF uniformly and the second pass is hash-stable. |
| No final newline | `COVERED` — update/compile/idempotence preserve the absence of a trailing newline. |
| UTF-8 BOM | `COVERED` — BOM bytes survive update/idempotence; javac compilation is intentionally not claimed because javac rejects a leading BOM. |
| Unicode identifiers | `COVERED` — UTF-8 identifiers and values survive update and release-8 compilation. |
| Formatting/indentation | `COVERED` — direct-member indentation is inferred locally; tab-indented source receives a tab-indented mechanical member without unrelated reformatting. |
| Dry-run parity | `COVERED` — every covered update fixture compares its no-write planned source with the applied source. |
| Atomicity | `COVERED` — an injected move failure retains original bytes and deletes the same-directory temporary file. |
| Idempotence | `COVERED` — every covered update fixture requires an unchanged second-pass hash. |
| Diagnostics | `COVERED` — compact-source refusal names type/file/reason before writes; profile and other failure diagnostics retain focused contract tests. |

Status: IMPLEMENTED — every cross-cutting fidelity row is `COVERED`. With JLC-2 SAM target
inference now covered, strict manifest mode has no remaining `PLANNED` rows.

## JLC-8 — Classic PHPSpec dogfooding gate

**Priority:** final stable decision gate.

Use a real Java 21 project, preferably the existing `magrathea-pki` domain work, against the published
RC from remote repositories rather than a local javaspec checkout. Complete one coherent behavior
milestone that includes records, sealed hierarchy/pattern usage where natural, generated typed
support, explicit `subject()` fallback, generation, refactoring, reports, and clean-container CI.

Acceptance:

- framework-origin source corruption or ambiguous generation: release blocker and new RC;
- opaque proxy/construction failure without an actionable explicit equivalent: release blocker or
  documented fix with evidence;
- no new framework features introduced merely to make the sample pass;
- the project remains organized around subject behavior, not test-framework mechanics;
- all generated domain bodies begin incomplete and cannot produce accidental GREEN.

## Stable exit criteria

1. JLC-0 through JLC-7 P0 fixtures pass on Java 8/11/17/21/25 CI.
2. Every listed construct has a tested disposition; no undocumented “probably supported” state.
3. Existing core Java 8 bytecode, Animal Sniffer, zero-runtime-dependency, adapter, and release gates remain green.
4. The remote-RC dogfooding milestone passes from a clean environment.
5. Any source-corruption, accidental-GREEN, or semantic-regression fix is published through a new RC before `1.0.0`.
6. Documentation says “classic PHPSpec-inspired subject-centric SpecBDD” and does not imply automatic alignment with future PHPSpec roadmaps.
