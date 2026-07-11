package io.github.jvmspec.compatibility;

import io.github.jvmspec.profile.ApiSymbol;
import io.github.jvmspec.profile.ApiSymbolKind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Safe reflection boundary for probing optional APIs by metadata name.
 */
public final class ApiAvailabilityProbe {
    private final ClassLoader classLoader;

    private ApiAvailabilityProbe(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
    }

    public static ApiAvailabilityProbe usingContextClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ApiAvailabilityProbe.class.getClassLoader();
        }
        return new ApiAvailabilityProbe(loader);
    }

    public static ApiAvailabilityProbe using(ClassLoader classLoader) {
        return new ApiAvailabilityProbe(classLoader);
    }

    public boolean isOwnerPresent(ApiSymbol symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        return isClassPresent(symbol.ownerQualifiedName());
    }

    public boolean isSymbolPresent(ApiSymbol symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (ApiSymbolKind.LANGUAGE_FEATURE.equals(symbol.kind())) {
            return isLanguageFeaturePresent(symbol.ownerQualifiedName());
        }
        if (!symbol.hasMemberName()) {
            return isOwnerPresent(symbol);
        }
        if (ApiSymbolKind.FIELD.equals(symbol.kind())) {
            return isFieldPresent(symbol.ownerQualifiedName(), symbol.memberName());
        }
        return isMethodPresent(symbol.ownerQualifiedName(), symbol.memberName());
    }

    public boolean isClassPresent(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        if (qualifiedName.endsWith("[]")) {
            return true;
        }
        return findClass(qualifiedName) != null;
    }

    public boolean isMethodPresent(String ownerQualifiedName, String methodName) {
        Class<?> owner = findClass(validateRequired(ownerQualifiedName, "ownerQualifiedName"));
        String member = validateRequired(methodName, "methodName");
        if (owner == null) {
            return false;
        }
        Method[] methods = owner.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (member.equals(methods[i].getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isFieldPresent(String ownerQualifiedName, String fieldName) {
        Class<?> owner = findClass(validateRequired(ownerQualifiedName, "ownerQualifiedName"));
        String member = validateRequired(fieldName, "fieldName");
        if (owner == null) {
            return false;
        }
        Field[] fields = owner.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (member.equals(fields[i].getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isLanguageFeaturePresent(String featureName) {
        if ("record".equals(featureName)) {
            return isClassPresent("java.lang.Record");
        }
        if ("sealed class".equals(featureName) || "sealed interface".equals(featureName)) {
            return isMethodPresent("java.lang.Class", "isSealed");
        }
        return false;
    }

    private Class<?> findClass(String qualifiedName) {
        Class<?> found = tryLoad(qualifiedName);
        if (found != null) {
            return found;
        }
        String candidate = qualifiedName;
        int dot = candidate.lastIndexOf('.');
        while (dot > 0) {
            candidate = candidate.substring(0, dot) + "$" + candidate.substring(dot + 1);
            found = tryLoad(candidate);
            if (found != null) {
                return found;
            }
            dot = candidate.lastIndexOf('.', dot - 1);
        }
        return null;
    }

    private Class<?> tryLoad(String binaryName) {
        try {
            return Class.forName(binaryName, false, classLoader);
        } catch (ClassNotFoundException ex) {
            return null;
        } catch (LinkageError ex) {
            return null;
        }
    }

    private static String validateRequired(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not contain leading or trailing whitespace: " + value);
        }
        return value;
    }
}
