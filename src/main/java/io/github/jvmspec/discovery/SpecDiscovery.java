package io.github.jvmspec.discovery;

import io.github.jvmspec.internal.type.JavaTypeResolutionContext;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Discovers Java specification skeletons using the PHPSpec-inspired *Spec naming convention.
 */
public final class SpecDiscovery {
    private static final String JAVA_SUFFIX = SpecNamingConvention.JAVA_SUFFIX;

    private SpecDiscovery() {
    }

    public static List<DiscoveredSpec> discover(File specRoot) {
        return discover(SpecDiscoveryRequest.of(specRoot));
    }

    public static List<DiscoveredSpec> discover(File specRoot, SpecNamingConvention namingConvention) {
        return discover(SpecDiscoveryRequest.of(specRoot, namingConvention));
    }

    public static List<DiscoveredSpec> discover(SpecDiscoveryRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        if (!request.matchesSuite() || !request.specRoot().isDirectory()) {
            return specs;
        }

        collect(request.specRoot(), "", specs, request);
        return specs;
    }

    private static void collect(File directory, String packagePrefix, List<DiscoveredSpec> specs, SpecDiscoveryRequest request) {
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
                collect(child, childPackagePrefix(packagePrefix, child.getName()), specs, request);
            } else if (child.isFile() && request.namingConvention().isSpecSourceFileName(child.getName())) {
                addSpec(child, packagePrefix, specs, request);
            }
        }
    }

    private static String childPackagePrefix(String packagePrefix, String childName) {
        if (packagePrefix.length() == 0) {
            return childName;
        }
        return packagePrefix + "." + childName;
    }

    private static void addSpec(File specFile, String packageName, List<DiscoveredSpec> specs, SpecDiscoveryRequest request) {
        String fileName = specFile.getName();
        String specSimpleName = fileName.substring(0, fileName.length() - JAVA_SUFFIX.length());
        String specQualifiedName;
        if (packageName.length() == 0) {
            specQualifiedName = specSimpleName;
        } else {
            specQualifiedName = packageName + "." + specSimpleName;
        }

        try {
            DescribedClass describedClass = request.namingConvention().describedClassForSpec(specQualifiedName);
            if (!request.matchesClass(describedClass, specQualifiedName)) {
                return;
            }

            String describedPackageName = describedClass.packageName();
            String describedQualifiedName = describedClass.qualifiedName();
            String source = sourceOf(specFile);
            List<SpecExample> examples = request.filterExamples(ExampleDiscovery.discover(source));
            if (request.hasExampleFilters() && examples.isEmpty()) {
                return;
            }
            SpecCallScanner.ScanResult scan = scanSpecCalls(source);
            SubjectDeclarationDiscovery.Declaration declaration =
                    SubjectDeclarationDiscovery.discover(source, describedPackageName);
            List<ConstructorDescriptor> constructors = ConstructionDiscovery.discover(
                    source, describedPackageName, describedQualifiedName, scan,
                    JavaTypeKind.RECORD.equals(declaration.kind));
            List<MethodDescriptor> methods = MethodDiscovery.discover(
                    source, describedPackageName, describedQualifiedName, scan);
            if (JavaTypeKind.ENUM.equals(declaration.kind)
                    && !declaration.enumConstructors.isEmpty()) {
                constructors = ConstructionDiscovery.combine(
                        constructors, declaration.enumConstructors, describedQualifiedName,
                        JavaTypeResolutionContext.fromSource(source));
            }
            specs.add(DiscoveredSpec.of(
                    specFile,
                    specQualifiedName,
                    DescribedType.of(
                            DescribedClass.of(describedQualifiedName),
                            declaration.kind,
                            declaration.extendedTypes,
                            declaration.implementedTypes,
                            declaration.permittedTypes,
                            constructors,
                            methods,
                            declaration.enumConstants
                    ),
                    examples
            ));
        } catch (IllegalArgumentException ignored) {
            // Ignore files that match the suffix convention but cannot be mapped to a valid Java type name.
        }
    }

    private static SpecCallScanner.ScanResult scanSpecCalls(String source) {
        try {
            return SpecCallScanner.scan(source);
        } catch (LinkageError ex) {
            // On Java 8, com.sun.source.* lives in tools.jar and may be absent from the
            // runtime classpath. Fall back to legacy text-based extraction instead of failing.
            return null;
        }
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
}
