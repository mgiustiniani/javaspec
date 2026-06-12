package org.javaspec.generation;

import java.io.File;
import java.util.Objects;

/**
 * Immutable plan for generating a Prophecy wrapper source file.
 */
public final class ProphecyGenerationPlan {
    private final Class<?> interfaceType;
    private final String packageName;
    private final String qualifiedName;
    private final String simpleName;
    private final File targetFile;
    private final String sourceCode;

    private ProphecyGenerationPlan(
            Class<?> interfaceType,
            String packageName,
            String qualifiedName,
            String simpleName,
            File targetFile,
            String sourceCode
    ) {
        this.interfaceType = interfaceType;
        this.packageName = packageName;
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.targetFile = targetFile;
        this.sourceCode = sourceCode;
    }

    /**
     * Creates a prophecy generation plan.
     *
     * @param interfaceType the interface type to wrap
     * @param packageName   the target package name
     * @param targetFile    the target source file
     * @param sourceCode    the generated source code
     * @return the plan
     */
    public static ProphecyGenerationPlan of(
            Class<?> interfaceType,
            String packageName,
            File targetFile,
            String sourceCode
    ) {
        Objects.requireNonNull(interfaceType, "interfaceType must not be null");
        Objects.requireNonNull(targetFile, "targetFile must not be null");
        Objects.requireNonNull(sourceCode, "sourceCode must not be null");
        String simpleName = interfaceType.getSimpleName() + "Prophecy";
        String qualifiedName = packageName.length() > 0
                ? packageName + "." + simpleName
                : simpleName;
        return new ProphecyGenerationPlan(
                interfaceType,
                packageName,
                qualifiedName,
                simpleName,
                targetFile,
                sourceCode
        );
    }

    /**
     * Returns the interface type.
     */
    public Class<?> interfaceType() {
        return interfaceType;
    }

    /**
     * Returns the target package name.
     */
    public String packageName() {
        return packageName;
    }

    /**
     * Returns the fully qualified class name.
     */
    public String qualifiedName() {
        return qualifiedName;
    }

    /**
     * Returns the simple class name (e.g. {@code MailerProphecy}).
     */
    public String simpleName() {
        return simpleName;
    }

    /**
     * Returns the target source file.
     */
    public File targetFile() {
        return targetFile;
    }

    /**
     * Returns the generated source code.
     */
    public String sourceCode() {
        return sourceCode;
    }
}
