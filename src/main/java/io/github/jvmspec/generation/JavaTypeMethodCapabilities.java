package io.github.jvmspec.generation;

import io.github.jvmspec.model.JavaTypeKind;

/** Java type-kind capabilities relevant to method synchronization and rendering. */
final class JavaTypeMethodCapabilities {
    private JavaTypeMethodCapabilities() {
    }

    static boolean supportsBodies(JavaTypeKind kind) {
        return JavaTypeKind.CLASS.equals(kind)
                || JavaTypeKind.FINAL_CLASS.equals(kind)
                || JavaTypeKind.SEALED_CLASS.equals(kind)
                || JavaTypeKind.ENUM.equals(kind)
                || JavaTypeKind.RECORD.equals(kind);
    }

    static boolean supportsInterfaceDeclarations(JavaTypeKind kind) {
        return JavaTypeKind.INTERFACE.equals(kind);
    }

    static boolean supportsAnnotationElements(JavaTypeKind kind) {
        return JavaTypeKind.ANNOTATION.equals(kind);
    }
}
