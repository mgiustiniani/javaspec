package org.javaspec.discovery;

import org.javaspec.model.DescribedClass;

import java.io.File;
import java.util.Objects;

/**
 * Checks classpath and source-root presence without initializing loaded classes.
 */
public final class ClassExistenceChecker {
    public static ClassCheckResult check(DescribedClass describedClass, File sourceRoot, ClassLoader classLoader) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");

        File sourceFile = new File(sourceRoot, describedClass.sourceRelativePath());
        boolean classpathPresent = isClasspathPresent(describedClass, classLoader);
        boolean sourceFilePresent = sourceFile.isFile();

        return ClassCheckResult.of(describedClass, sourceRoot, sourceFile, classpathPresent, sourceFilePresent);
    }

    public static ClassCheckResult checkClass(DescribedClass describedClass, File sourceRoot, ClassLoader classLoader) {
        return check(describedClass, sourceRoot, classLoader);
    }

    private static boolean isClasspathPresent(DescribedClass describedClass, ClassLoader classLoader) {
        try {
            Class.forName(describedClass.qualifiedName(), false, classLoader);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
