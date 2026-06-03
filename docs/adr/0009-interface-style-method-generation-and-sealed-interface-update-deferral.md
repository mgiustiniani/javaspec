# 0009 — Interface-Style Method Generation and Sealed-Interface Update Deferral

## Status

Accepted

## Context

ADR 0004 established that javaspec must generate missing methods inferred from typed proxy, throw-proxy, direct subject/setter, and construction-factory syntax. Phase 10 stabilized the next implementation increment for class-like type generation, especially ordinary interfaces, annotations, and sealed interfaces.

Java type kinds have different source rules. Classes, final classes, sealed classes, enums, and records can receive method bodies with Java default returns. Ordinary interfaces require declarations for instance methods. Annotations can contain only compatible no-argument elements. Sealed interfaces may require both root declarations and matching implementation bodies on nested permitted implementations to remain valid Java source.

The implementation must stay Java 8-compatible as a binary even when it emits post-Java-8 source forms, and source updates must remain deterministic and source-preserving where supported.

## Decision

Phase 10 method generation remains inside the existing discovery/generation boundary.

For missing ordinary interface skeletons and existing ordinary interface sources, javaspec emits discovered non-static instance method declarations ending in `;`. Static descriptors are skipped for ordinary interface-style generation.

For missing annotation skeletons and existing annotation sources, javaspec emits only compatible annotation elements: no-argument, non-static descriptors with annotation-compatible return types. Descriptors with parameters, static descriptors, `void`, `Object`, or otherwise incompatible return types are ignored for annotation sources.

For missing sealed-interface skeletons, javaspec emits root method declarations and generated nested permitted implementation bodies with Java default returns. This keeps generated Java 17 sealed-interface source forms syntactically complete.

Existing sealed-interface source updates are intentionally deferred. Updating an existing sealed interface safely requires source-preserving insertion into the sealed root and into each nested permitted implementation, which is more complex than updating ordinary interface declarations or annotation elements alone.

Existing class, final-class, sealed-class, enum, and record method-body generation remains unchanged and continues to use Java default returns for generated bodies.

## Consequences

Positive consequences:

- Method generation is aligned with Java source rules for ordinary interfaces and annotations.
- Missing sealed-interface skeleton generation can produce usable Java 17 source forms without compromising the Java 8 binary.
- Existing ordinary interfaces and annotations can be updated source-preservingly and idempotently.
- The documented sealed-interface limitation is explicit rather than hidden behind unsafe source edits.

Negative consequences and limitations:

- Existing sealed-interface sources do not receive missing generated methods yet.
- Annotation method generation intentionally ignores descriptors that are valid for classes but invalid for annotations.
- Source parsing remains heuristic and Java 8-compatible rather than a full Java parser.
- Generated post-Java-8 source forms still require an appropriate JDK to compile.

Related ARC42 sections: [5. Building Block View](../arc42/05-building-block-view.md), [6. Runtime View](../arc42/06-runtime-view.md), [8. Concepts](../arc42/08-concepts.md), and [11. Risks and Technical Debt](../arc42/11-risks-and-technical-debt.md).
