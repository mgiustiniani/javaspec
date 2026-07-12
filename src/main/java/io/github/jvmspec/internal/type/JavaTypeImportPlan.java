package io.github.jvmspec.internal.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic imports and rendering for structured Java source types. */
public final class JavaTypeImportPlan {
    private final String currentPackage;
    private final List<String> imports;
    private final Map<String, String> simpleNamesByQualifiedName;

    private JavaTypeImportPlan(
            String currentPackage,
            List<String> imports,
            Map<String, String> simpleNamesByQualifiedName
    ) {
        this.currentPackage = currentPackage;
        this.imports = Collections.unmodifiableList(imports);
        this.simpleNamesByQualifiedName = Collections.unmodifiableMap(simpleNamesByQualifiedName);
    }

    public static JavaTypeImportPlan forTypes(String currentPackage, List<String> canonicalTypes) {
        Set<String> qualified = new LinkedHashSet<String>();
        for (int i = 0; i < canonicalTypes.size(); i++) {
            qualified.addAll(JavaTypeRef.parseCanonical(canonicalTypes.get(i)).referencedQualifiedTypes());
        }
        Map<String, List<String>> bySimpleName = new LinkedHashMap<String, List<String>>();
        for (String name : qualified) {
            if (samePackage(name, currentPackage)) continue;
            String simple = simpleName(name);
            List<String> names = bySimpleName.get(simple);
            if (names == null) {
                names = new ArrayList<String>();
                bySimpleName.put(simple, names);
            }
            names.add(name);
        }
        List<String> imports = new ArrayList<String>();
        Map<String, String> renderNames = new LinkedHashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : bySimpleName.entrySet()) {
            if (entry.getValue().size() == 1) {
                String qualifiedName = entry.getValue().get(0);
                imports.add(qualifiedName);
                renderNames.put(qualifiedName, entry.getKey());
            }
        }
        Collections.sort(imports);
        return new JavaTypeImportPlan(currentPackage == null ? "" : currentPackage, imports, renderNames);
    }

    public List<String> imports() {
        return imports;
    }

    public String render(String canonicalType) {
        return JavaTypeRef.parseCanonical(canonicalType).render(simpleNamesByQualifiedName, currentPackage);
    }

    private static boolean samePackage(String qualifiedName, String packageName) {
        if (packageName == null || packageName.length() == 0 || !qualifiedName.startsWith(packageName + ".")) {
            return false;
        }
        return qualifiedName.substring(packageName.length() + 1).indexOf('.') < 0;
    }

    private static String simpleName(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        return dot < 0 ? qualifiedName : qualifiedName.substring(dot + 1);
    }
}
