package io.github.jvmspec.discovery;

import io.github.jvmspec.config.JavaspecSuiteConfiguration;
import io.github.jvmspec.model.DescribedClass;

import java.io.File;
import java.util.Objects;

/**
 * Maps production Java type names to PHPSpec-inspired specification and support type names.
 */
public class SpecNamingConvention {
    public static final String DEFAULT_SPEC_PACKAGE_PREFIX = "spec";
    public static final String DEFAULT_PRODUCTION_PACKAGE_PREFIX = "";
    public static final String SPEC_SUFFIX = "Spec";
    public static final String SUPPORT_SUFFIX = "SpecSupport";
    public static final String JAVA_SUFFIX = ".java";

    private static final String PACKAGE_VALIDATION_TYPE_NAME = "PackageValidationType";

    private final String specPackagePrefix;
    private final String productionPackagePrefix;
    private final String specPackagePrefixWithDot;
    private final String productionPackagePrefixWithDot;

    protected SpecNamingConvention(String specPackagePrefix, String productionPackagePrefix) {
        this.specPackagePrefix = validateRequiredPackageName("specPackagePrefix", specPackagePrefix);
        this.productionPackagePrefix = validateOptionalPackageName("productionPackagePrefix", productionPackagePrefix);
        this.specPackagePrefixWithDot = this.specPackagePrefix + ".";
        this.productionPackagePrefixWithDot = this.productionPackagePrefix.length() == 0
                ? ""
                : this.productionPackagePrefix + ".";
    }

    public static SpecNamingConvention defaults() {
        return new SpecNamingConvention(DEFAULT_SPEC_PACKAGE_PREFIX, DEFAULT_PRODUCTION_PACKAGE_PREFIX);
    }

    public static SpecNamingConvention defaultConvention() {
        return defaults();
    }

    public static SpecNamingConvention of(String specPackagePrefix, String productionPackagePrefix) {
        return new SpecNamingConvention(specPackagePrefix, productionPackagePrefix);
    }

    public static SpecNamingConvention from(JavaspecSuiteConfiguration suiteConfiguration) {
        Objects.requireNonNull(suiteConfiguration, "suiteConfiguration must not be null");
        return of(suiteConfiguration.specPackagePrefix(), suiteConfiguration.packagePrefix());
    }

    public String specPackagePrefix() {
        return specPackagePrefix;
    }

    public String getSpecPackagePrefix() {
        return specPackagePrefix;
    }

    public String productionPackagePrefix() {
        return productionPackagePrefix;
    }

    public String packagePrefix() {
        return productionPackagePrefix;
    }

    public String getProductionPackagePrefix() {
        return productionPackagePrefix;
    }

    public String getPackagePrefix() {
        return productionPackagePrefix;
    }

    public String specSimpleName(String describedQualifiedName) {
        return specSimpleName(DescribedClass.of(describedQualifiedName));
    }

    public String specSimpleName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        return describedClass.simpleName() + SPEC_SUFFIX;
    }

    public String supportSimpleName(String describedQualifiedName) {
        return supportSimpleName(DescribedClass.of(describedQualifiedName));
    }

    public String supportSimpleName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        return describedClass.simpleName() + SUPPORT_SUFFIX;
    }

    public String specPackageName(String describedQualifiedName) {
        return specPackageName(DescribedClass.of(describedQualifiedName));
    }

    public String specPackageName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        String relativePackageName = relativeProductionPackageName(describedClass);
        if (relativePackageName.length() == 0) {
            return specPackagePrefix;
        }
        return specPackagePrefix + "." + relativePackageName;
    }

    public String specQualifiedName(String describedQualifiedName) {
        return specQualifiedName(DescribedClass.of(describedQualifiedName));
    }

    public String toSpecQualifiedName(String describedQualifiedName) {
        return specQualifiedName(describedQualifiedName);
    }

    public String specQualifiedName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        return qualify(specPackageName(describedClass), specSimpleName(describedClass));
    }

    public String supportQualifiedName(String describedQualifiedName) {
        return supportQualifiedName(DescribedClass.of(describedQualifiedName));
    }

    public String toSupportQualifiedName(String describedQualifiedName) {
        return supportQualifiedName(describedQualifiedName);
    }

    public String supportQualifiedName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        return qualify(specPackageName(describedClass), supportSimpleName(describedClass));
    }

    public String specSourceRelativePath(String describedQualifiedName) {
        return specSourceRelativePath(DescribedClass.of(describedQualifiedName));
    }

    public String sourceRelativePath(String describedQualifiedName) {
        return specSourceRelativePath(describedQualifiedName);
    }

    public String specSourceRelativePath(DescribedClass describedClass) {
        return typeSourceRelativePath(specQualifiedName(describedClass));
    }

    public String sourceRelativePath(DescribedClass describedClass) {
        return specSourceRelativePath(describedClass);
    }

    public String supportSourceRelativePath(String describedQualifiedName) {
        return supportSourceRelativePath(DescribedClass.of(describedQualifiedName));
    }

    public String supportSourceRelativePath(DescribedClass describedClass) {
        return typeSourceRelativePath(supportQualifiedName(describedClass));
    }

    public String describedSourceRelativePath(String describedQualifiedName) {
        return describedSourceRelativePath(DescribedClass.of(describedQualifiedName));
    }

    public String productionSourceRelativePath(String describedQualifiedName) {
        return describedSourceRelativePath(describedQualifiedName);
    }

    public String describedSourceRelativePath(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        return describedClass.sourceRelativePath();
    }

    public String productionSourceRelativePath(DescribedClass describedClass) {
        return describedSourceRelativePath(describedClass);
    }

    public String specQualifiedNameForSourcePath(String specSourceRelativePath) {
        String specQualifiedName = qualifiedNameForSourcePath("specSourceRelativePath", specSourceRelativePath);
        validateSpecSimpleName(simpleName(specQualifiedName), specQualifiedName);
        return specQualifiedName;
    }

    public String supportQualifiedNameForSourcePath(String supportSourceRelativePath) {
        String supportQualifiedName = qualifiedNameForSourcePath("supportSourceRelativePath", supportSourceRelativePath);
        validateSupportSimpleName(simpleName(supportQualifiedName), supportQualifiedName);
        return supportQualifiedName;
    }

    public String describedQualifiedNameForSpec(String specQualifiedName) {
        return describedQualifiedNameForSpecQualifiedName(specQualifiedName);
    }

    public String describedQualifiedName(String specQualifiedName) {
        return describedQualifiedNameForSpecQualifiedName(specQualifiedName);
    }

    public String toDescribedQualifiedName(String specQualifiedName) {
        return describedQualifiedNameForSpecQualifiedName(specQualifiedName);
    }

    public String describedQualifiedNameForSpecQualifiedName(String specQualifiedName) {
        DescribedClass specClass = validateQualifiedTypeName("specQualifiedName", specQualifiedName);
        String specSimpleName = specClass.simpleName();
        validateSpecSimpleName(specSimpleName, specQualifiedName);
        String describedSimpleName = specSimpleName.substring(0, specSimpleName.length() - SPEC_SUFFIX.length());
        String describedPackageName = describedPackageNameForSpecPackageName(specClass.packageName());
        return DescribedClass.of(qualify(describedPackageName, describedSimpleName)).qualifiedName();
    }

    public DescribedClass describedClassForSpec(String specQualifiedName) {
        return DescribedClass.of(describedQualifiedNameForSpecQualifiedName(specQualifiedName));
    }

    public DescribedClass describedClassForSpecQualifiedName(String specQualifiedName) {
        return describedClassForSpec(specQualifiedName);
    }

    public String describedQualifiedNameForSupport(String supportQualifiedName) {
        return describedQualifiedNameForSupportQualifiedName(supportQualifiedName);
    }

    public String describedQualifiedNameForSupportQualifiedName(String supportQualifiedName) {
        DescribedClass supportClass = validateQualifiedTypeName("supportQualifiedName", supportQualifiedName);
        String supportSimpleName = supportClass.simpleName();
        validateSupportSimpleName(supportSimpleName, supportQualifiedName);
        String describedSimpleName = supportSimpleName.substring(0, supportSimpleName.length() - SUPPORT_SUFFIX.length());
        String describedPackageName = describedPackageNameForSpecPackageName(supportClass.packageName());
        return DescribedClass.of(qualify(describedPackageName, describedSimpleName)).qualifiedName();
    }

    public DescribedClass describedClassForSupport(String supportQualifiedName) {
        return DescribedClass.of(describedQualifiedNameForSupportQualifiedName(supportQualifiedName));
    }

    public DescribedClass describedClassForSupportQualifiedName(String supportQualifiedName) {
        return describedClassForSupport(supportQualifiedName);
    }

    public String describedQualifiedNameForSpecSourcePath(String specSourceRelativePath) {
        return describedQualifiedNameForSpecQualifiedName(specQualifiedNameForSourcePath(specSourceRelativePath));
    }

    public DescribedClass describedClassForSpecSourcePath(String specSourceRelativePath) {
        return DescribedClass.of(describedQualifiedNameForSpecSourcePath(specSourceRelativePath));
    }

    public String describedQualifiedNameForSupportSourcePath(String supportSourceRelativePath) {
        return describedQualifiedNameForSupportQualifiedName(supportQualifiedNameForSourcePath(supportSourceRelativePath));
    }

    public DescribedClass describedClassForSupportSourcePath(String supportSourceRelativePath) {
        return DescribedClass.of(describedQualifiedNameForSupportSourcePath(supportSourceRelativePath));
    }

    public boolean isSpecSimpleName(String simpleName) {
        return simpleName != null
                && simpleName.endsWith(SPEC_SUFFIX)
                && simpleName.length() > SPEC_SUFFIX.length();
    }

    public boolean isSupportSimpleName(String simpleName) {
        return simpleName != null
                && simpleName.endsWith(SUPPORT_SUFFIX)
                && simpleName.length() > SUPPORT_SUFFIX.length();
    }

    public boolean isSpecSourceFileName(String fileName) {
        return fileName != null
                && fileName.endsWith(SPEC_SUFFIX + JAVA_SUFFIX)
                && fileName.length() > (SPEC_SUFFIX + JAVA_SUFFIX).length();
    }

    public boolean isSupportSourceFileName(String fileName) {
        return fileName != null
                && fileName.endsWith(SUPPORT_SUFFIX + JAVA_SUFFIX)
                && fileName.length() > (SUPPORT_SUFFIX + JAVA_SUFFIX).length();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SpecNamingConvention)) {
            return false;
        }
        SpecNamingConvention that = (SpecNamingConvention) other;
        return specPackagePrefix.equals(that.specPackagePrefix)
                && productionPackagePrefix.equals(that.productionPackagePrefix);
    }

    @Override
    public int hashCode() {
        int result = specPackagePrefix.hashCode();
        result = 31 * result + productionPackagePrefix.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SpecNamingConvention{" +
                "specPackagePrefix='" + specPackagePrefix + '\'' +
                ", productionPackagePrefix='" + productionPackagePrefix + '\'' +
                '}';
    }

    private String relativeProductionPackageName(DescribedClass describedClass) {
        String describedPackageName = describedClass.packageName();
        if (productionPackagePrefix.length() == 0) {
            return describedPackageName;
        }
        if (describedPackageName.equals(productionPackagePrefix)) {
            return "";
        }
        if (describedPackageName.startsWith(productionPackagePrefixWithDot)) {
            return describedPackageName.substring(productionPackagePrefixWithDot.length());
        }
        throw new IllegalArgumentException("Described class " + describedClass.qualifiedName()
                + " is not under production package prefix " + productionPackagePrefix);
    }

    private String describedPackageNameForSpecPackageName(String specPackageName) {
        if (specPackageName.equals(specPackagePrefix)) {
            return productionPackagePrefix;
        }
        if (specPackageName.startsWith(specPackagePrefixWithDot)) {
            String relativePackageName = specPackageName.substring(specPackagePrefixWithDot.length());
            return qualify(productionPackagePrefix, relativePackageName);
        }
        if (productionPackagePrefix.length() == 0) {
            return specPackageName;
        }
        throw new IllegalArgumentException("Spec package " + specPackageName
                + " is not under spec package prefix " + specPackagePrefix);
    }

    private static String typeSourceRelativePath(String qualifiedName) {
        return qualifiedName.replace('.', File.separatorChar) + JAVA_SUFFIX;
    }

    private static String qualifiedNameForSourcePath(String fieldName, String sourceRelativePath) {
        Objects.requireNonNull(sourceRelativePath, fieldName + " must not be null");
        if (sourceRelativePath.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        if (sourceRelativePath.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!sourceRelativePath.equals(sourceRelativePath.trim())) {
            throw new IllegalArgumentException(fieldName + " must not contain leading or trailing whitespace: " + sourceRelativePath);
        }
        String normalized = sourceRelativePath.replace('\\', '/');
        if (normalized.charAt(0) == '/') {
            throw new IllegalArgumentException(fieldName + " must be a relative source path: " + sourceRelativePath);
        }
        if (!normalized.endsWith(JAVA_SUFFIX)) {
            throw new IllegalArgumentException(fieldName + " must end with " + JAVA_SUFFIX + ": " + sourceRelativePath);
        }
        String withoutSuffix = normalized.substring(0, normalized.length() - JAVA_SUFFIX.length());
        if (withoutSuffix.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must include a type name before " + JAVA_SUFFIX + ": " + sourceRelativePath);
        }
        String qualifiedName = withoutSuffix.replace('/', '.');
        return validateQualifiedTypeName(fieldName, qualifiedName).qualifiedName();
    }

    private static DescribedClass validateQualifiedTypeName(String fieldName, String qualifiedName) {
        Objects.requireNonNull(qualifiedName, fieldName + " must not be null");
        if (qualifiedName.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        if (qualifiedName.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!qualifiedName.equals(qualifiedName.trim())) {
            throw new IllegalArgumentException(fieldName + " must not contain leading or trailing whitespace: " + qualifiedName);
        }
        try {
            return DescribedClass.of(qualifiedName);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid Java type name: " + qualifiedName, exception);
        }
    }

    private static void validateSpecSimpleName(String specSimpleName, String specQualifiedName) {
        if (!specSimpleName.endsWith(SPEC_SUFFIX)) {
            throw new IllegalArgumentException("Spec qualified name must end with " + SPEC_SUFFIX + ": " + specQualifiedName);
        }
        if (specSimpleName.length() == SPEC_SUFFIX.length()) {
            throw new IllegalArgumentException("Spec qualified name must include a described type name before "
                    + SPEC_SUFFIX + ": " + specQualifiedName);
        }
    }

    private static void validateSupportSimpleName(String supportSimpleName, String supportQualifiedName) {
        if (!supportSimpleName.endsWith(SUPPORT_SUFFIX)) {
            throw new IllegalArgumentException("Support qualified name must end with " + SUPPORT_SUFFIX + ": " + supportQualifiedName);
        }
        if (supportSimpleName.length() == SUPPORT_SUFFIX.length()) {
            throw new IllegalArgumentException("Support qualified name must include a described type name before "
                    + SUPPORT_SUFFIX + ": " + supportQualifiedName);
        }
    }

    private static String validateRequiredPackageName(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        validatePackageName(fieldName, trimmed);
        return trimmed;
    }

    private static String validateOptionalPackageName(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return "";
        }
        validatePackageName(fieldName, trimmed);
        return trimmed;
    }

    private static void validatePackageName(String fieldName, String packageName) {
        try {
            DescribedClass.of(packageName + "." + PACKAGE_VALIDATION_TYPE_NAME);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid Java package name: " + packageName, exception);
        }
    }

    private static String qualify(String packageName, String simpleName) {
        if (packageName.length() == 0) {
            return simpleName;
        }
        if (simpleName.length() == 0) {
            return packageName;
        }
        return packageName + "." + simpleName;
    }

    private static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return qualifiedName;
        }
        return qualifiedName.substring(lastDot + 1);
    }
}
