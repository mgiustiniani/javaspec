package org.javaspec.discovery;

import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Checks classpath and source-root presence for class-like Java types without initializing loaded classes.
 */
public final class TypeExistenceChecker {
    private TypeExistenceChecker() {
    }

    public static TypeCheckResult check(DescribedType describedType, File sourceRoot, ClassLoader classLoader) {
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");

        File sourceFile = new File(sourceRoot, describedType.sourceRelativePath());
        JavaTypeKind classpathKind = classpathKind(describedType, classLoader);
        boolean classpathPresent = classpathKind != null;
        boolean sourceFilePresent = sourceFile.isFile();

        return TypeCheckResult.of(describedType, sourceRoot, sourceFile, classpathKind, classpathPresent, sourceFilePresent);
    }

    private static JavaTypeKind classpathKind(DescribedType describedType, ClassLoader classLoader) {
        try {
            Class<?> loadedClass = Class.forName(describedType.qualifiedName(), false, classLoader);
            return kindOf(loadedClass);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static JavaTypeKind kindOf(Class<?> loadedClass) {
        if (loadedClass.isAnnotation()) {
            return JavaTypeKind.ANNOTATION;
        }
        if (loadedClass.isEnum()) {
            return JavaTypeKind.ENUM;
        }
        if (reflectiveBoolean(loadedClass, "isRecord")) {
            return JavaTypeKind.RECORD;
        }

        boolean sealed = reflectiveBoolean(loadedClass, "isSealed");
        if (loadedClass.isInterface()) {
            if (sealed) {
                return JavaTypeKind.SEALED_INTERFACE;
            }
            return JavaTypeKind.INTERFACE;
        }
        if (sealed) {
            return JavaTypeKind.SEALED_CLASS;
        }
        return JavaTypeKind.CLASS;
    }

    private static boolean reflectiveBoolean(Class<?> target, String methodName) {
        try {
            Method method = Class.class.getMethod(methodName);
            Object result = method.invoke(target);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
