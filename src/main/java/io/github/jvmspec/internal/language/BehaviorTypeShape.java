package io.github.jvmspec.internal.language;

/** Portable subject shape; language backends decide whether and how each shape is representable. */
public enum BehaviorTypeShape {
    REFERENCE_CLASS,
    FINAL_REFERENCE_CLASS,
    INTERFACE,
    ENUMERATION,
    METADATA_TYPE,
    PRODUCT_TYPE,
    SEALED_REFERENCE_CLASS,
    SEALED_INTERFACE
}
