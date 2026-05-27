package org.javaspec.discovery;

import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers Java specification skeletons using the PHPSpec-inspired *Spec naming convention.
 */
public final class SpecDiscovery {
    private static final String JAVA_SUFFIX = ".java";
    private static final String SPEC_PACKAGE_PREFIX = "spec";
    private static final String SPEC_PACKAGE_PREFIX_WITH_DOT = SPEC_PACKAGE_PREFIX + ".";
    private static final String SPEC_SUFFIX = "Spec";
    private static final String SPEC_JAVA_SUFFIX = SPEC_SUFFIX + JAVA_SUFFIX;
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern SHOULD_EXTEND_PATTERN = Pattern.compile("shouldExtend\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_IMPLEMENT_PATTERN = Pattern.compile("shouldImplement\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_PERMIT_PATTERN = Pattern.compile("shouldPermit\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern CLASS_LITERAL_PATTERN = Pattern.compile("([A-Za-z_$][A-Za-z0-9_$.]*)\\s*\\.\\s*class");

    private SpecDiscovery() {
    }

    public static List<DiscoveredSpec> discover(File specRoot) {
        Objects.requireNonNull(specRoot, "specRoot must not be null");

        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        if (!specRoot.isDirectory()) {
            return specs;
        }

        collect(specRoot, "", specs);
        return specs;
    }

    private static void collect(File directory, String packagePrefix, List<DiscoveredSpec> specs) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }

        Arrays.sort(children, new Comparator<File>() {
            public int compare(File left, File right) {
                return left.getName().compareTo(right.getName());
            }
        });

        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                collect(child, childPackagePrefix(packagePrefix, child.getName()), specs);
            } else if (child.isFile() && child.getName().endsWith(SPEC_JAVA_SUFFIX)) {
                addSpec(child, packagePrefix, specs);
            }
        }
    }

    private static String childPackagePrefix(String packagePrefix, String childName) {
        if (packagePrefix.length() == 0) {
            return childName;
        }
        return packagePrefix + "." + childName;
    }

    private static void addSpec(File specFile, String packageName, List<DiscoveredSpec> specs) {
        String fileName = specFile.getName();
        String specSimpleName = fileName.substring(0, fileName.length() - JAVA_SUFFIX.length());
        String describedSimpleName = specSimpleName.substring(0, specSimpleName.length() - SPEC_SUFFIX.length());
        if (describedSimpleName.length() == 0) {
            return;
        }

        String describedPackageName = describedPackageName(packageName);
        String describedQualifiedName;
        String specQualifiedName;
        if (describedPackageName.length() == 0) {
            describedQualifiedName = describedSimpleName;
        } else {
            describedQualifiedName = describedPackageName + "." + describedSimpleName;
        }
        if (packageName.length() == 0) {
            specQualifiedName = specSimpleName;
        } else {
            specQualifiedName = packageName + "." + specSimpleName;
        }

        try {
            String source = sourceOf(specFile);
            specs.add(DiscoveredSpec.of(
                    specFile,
                    specQualifiedName,
                    DescribedType.of(
                            describedQualifiedName,
                            describedKind(source),
                            typeNames(source, describedPackageName, SHOULD_EXTEND_PATTERN),
                            typeNames(source, describedPackageName, SHOULD_IMPLEMENT_PATTERN),
                            typeNames(source, describedPackageName, SHOULD_PERMIT_PATTERN)
                    )
            ));
        } catch (IllegalArgumentException ignored) {
            // Ignore files that match the suffix convention but cannot be mapped to a valid Java type name.
        }
    }

    private static JavaTypeKind describedKind(String source) {
        if (source.contains("shouldBeAFinalClass(")) {
            return JavaTypeKind.FINAL_CLASS;
        }
        if (source.contains("shouldBeAnAnnotation(")) {
            return JavaTypeKind.ANNOTATION;
        }
        if (source.contains("shouldBeAnEnum(")) {
            return JavaTypeKind.ENUM;
        }
        if (source.contains("shouldBeARecord(")) {
            return JavaTypeKind.RECORD;
        }
        if (source.contains("shouldBeASealedInterface(")) {
            return JavaTypeKind.SEALED_INTERFACE;
        }
        if (source.contains("shouldBeASealedClass(")) {
            return JavaTypeKind.SEALED_CLASS;
        }
        if (source.contains("shouldBeAnInterface(")) {
            return JavaTypeKind.INTERFACE;
        }
        return JavaTypeKind.CLASS;
    }

    private static List<String> typeNames(String source, String describedPackageName, Pattern markerPattern) {
        List<String> typeNames = new ArrayList<String>();
        Map<String, String> imports = importsBySimpleName(source);
        Matcher markerMatcher = markerPattern.matcher(source);
        while (markerMatcher.find()) {
            String arguments = markerMatcher.group(1);
            Matcher classLiteralMatcher = CLASS_LITERAL_PATTERN.matcher(arguments);
            while (classLiteralMatcher.find()) {
                String typeName = resolveTypeName(classLiteralMatcher.group(1), imports, describedPackageName);
                if (!typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                }
            }
        }
        return typeNames;
    }

    private static Map<String, String> importsBySimpleName(String source) {
        Map<String, String> imports = new LinkedHashMap<String, String>();
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            String qualifiedName = matcher.group(1);
            int lastDot = qualifiedName.lastIndexOf('.');
            if (lastDot >= 0) {
                imports.put(qualifiedName.substring(lastDot + 1), qualifiedName);
            }
        }
        return imports;
    }

    private static String resolveTypeName(String typeName, Map<String, String> imports, String describedPackageName) {
        if (typeName.indexOf('.') >= 0) {
            return typeName;
        }
        String importedName = imports.get(typeName);
        if (importedName != null) {
            return importedName;
        }
        if (describedPackageName.length() == 0) {
            return typeName;
        }
        return describedPackageName + "." + typeName;
    }

    private static String sourceOf(File specFile) {
        try {
            return new String(Files.readAllBytes(specFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        } catch (SecurityException ignored) {
            return "";
        }
    }

    private static String describedPackageName(String specPackageName) {
        if (SPEC_PACKAGE_PREFIX.equals(specPackageName)) {
            return "";
        }
        if (specPackageName.startsWith(SPEC_PACKAGE_PREFIX_WITH_DOT)) {
            return specPackageName.substring(SPEC_PACKAGE_PREFIX_WITH_DOT.length());
        }
        return specPackageName;
    }
}
