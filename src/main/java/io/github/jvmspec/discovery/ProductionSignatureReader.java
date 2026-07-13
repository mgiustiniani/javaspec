package io.github.jvmspec.discovery;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Refines a discovered {@link DescribedType} with the real signatures declared by an already
 * existing production source file.
 *
 * <p>Spec-based inference can only approximate types from literals; the production class, when
 * present, is the truth. Every discovered method or constructor matching a production member by
 * name, arity and static-ness adopts the production return type, parameter types and parameter
 * names. Members absent from production are kept as inferred, so generation can still create
 * them.</p>
 */
public final class ProductionSignatureReader {
    private ProductionSignatureReader() {
    }

    /**
     * Returns a refined copy of {@code describedType}, or the same instance when the production
     * source is absent or cannot be parsed.
     */
    public static DescribedType refine(DescribedType describedType, File sourceRoot) {
        if (sourceRoot == null) {
            return describedType;
        }
        File sourceFile = new File(sourceRoot, describedType.sourceRelativePath());
        if (!sourceFile.isFile()) {
            return describedType;
        }
        String source;
        try {
            source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return describedType;
        }
        CompilationUnitTree unit;
        try {
            unit = SpecCallScanner.parseUnit(source);
        } catch (LinkageError ex) {
            // On Java 8, com.sun.source.* lives in tools.jar and may be absent from the
            // runtime classpath. Refinement is optional; keep the discovered type unchanged.
            return describedType;
        }
        if (unit == null) {
            return describedType;
        }
        ClassTree classTree = topLevelType(unit, describedType.describedClass().simpleName());
        if (classTree == null) {
            return describedType;
        }

        String packageName = packageNameOf(unit);
        Map<String, String> imports = importsBySimpleName(unit);
        List<ProductionMethod> productionMethods = new ArrayList<ProductionMethod>();
        List<ProductionMethod> productionConstructors = new ArrayList<ProductionMethod>();
        collectMembers(classTree, packageName, imports, productionMethods, productionConstructors);

        List<MethodDescriptor> refinedMethods = new ArrayList<MethodDescriptor>();
        for (int i = 0; i < describedType.methods().size(); i++) {
            refinedMethods.add(refineMethod(describedType.methods().get(i), productionMethods));
        }
        List<ConstructorDescriptor> refinedConstructors = new ArrayList<ConstructorDescriptor>();
        for (int i = 0; i < describedType.constructors().size(); i++) {
            refinedConstructors.add(refineConstructor(describedType.constructors().get(i), productionConstructors));
        }
        return describedType
                .withKind(sourceKindOf(classTree, describedType.kind()))
                .withConstructors(refinedConstructors)
                .withMethods(refinedMethods);
    }

    private static JavaTypeKind sourceKindOf(ClassTree classTree, JavaTypeKind fallback) {
        String kindName = classTree.getKind().name();
        if ("RECORD".equals(kindName)) {
            return JavaTypeKind.RECORD;
        }
        if (Tree.Kind.ENUM.name().equals(kindName)) {
            return JavaTypeKind.ENUM;
        }
        if (Tree.Kind.INTERFACE.name().equals(kindName)) {
            return JavaTypeKind.INTERFACE;
        }
        if (Tree.Kind.ANNOTATION_TYPE.name().equals(kindName)) {
            return JavaTypeKind.ANNOTATION;
        }
        return fallback;
    }

    private static MethodDescriptor refineMethod(MethodDescriptor descriptor, List<ProductionMethod> productionMethods) {
        ProductionMethod production = bestMethodMatch(descriptor, productionMethods);
        if (production == null) {
            return descriptor;
        }
        if ("void".equals(production.returnType)) {
            return MethodDescriptor.voidMethod(
                    production.name, production.parameterTypes, production.parameterNames);
        }
        if (production.isStatic) {
            return MethodDescriptor.staticMethod(
                    production.name, production.returnType,
                    production.parameterTypes, production.parameterNames);
        }
        return MethodDescriptor.of(
                production.name, production.returnType,
                production.parameterTypes, production.parameterNames);
    }

    private static ProductionMethod bestMethodMatch(MethodDescriptor descriptor, List<ProductionMethod> productionMethods) {
        ProductionMethod best = null;
        int bestScore = -1;
        boolean ambiguousBest = false;
        for (int i = 0; i < productionMethods.size(); i++) {
            ProductionMethod production = productionMethods.get(i);
            if (!production.name.equals(descriptor.methodName())
                    || production.isStatic != descriptor.isStatic()) {
                continue;
            }
            int score = parameterCompatibilityScore(descriptor, production.parameterTypes);
            if (score > bestScore) {
                best = production;
                bestScore = score;
                ambiguousBest = false;
            } else if (score == bestScore && score >= 0 && best != null
                    && !best.parameterTypes.equals(production.parameterTypes)) {
                ambiguousBest = true;
            }
        }
        return ambiguousBest ? null : best;
    }

    private static ConstructorDescriptor refineConstructor(
            ConstructorDescriptor descriptor,
            List<ProductionMethod> productionConstructors
    ) {
        ProductionMethod production = bestConstructorMatch(descriptor, productionConstructors);
        if (production == null) {
            return descriptor;
        }
        return ConstructorDescriptor.of(
                production.parameterTypes, production.parameterNames, descriptor.bodyContent());
    }

    private static ProductionMethod bestConstructorMatch(
            ConstructorDescriptor descriptor,
            List<ProductionMethod> productionConstructors
    ) {
        ProductionMethod best = null;
        int bestScore = -1;
        boolean ambiguousBest = false;
        for (int i = 0; i < productionConstructors.size(); i++) {
            ProductionMethod production = productionConstructors.get(i);
            int score = parameterCompatibilityScore(descriptor.parameterTypes(), production.parameterTypes);
            if (score > bestScore) {
                best = production;
                bestScore = score;
                ambiguousBest = false;
            } else if (score == bestScore && score >= 0 && best != null
                    && !best.parameterTypes.equals(production.parameterTypes)) {
                ambiguousBest = true;
            }
        }
        return ambiguousBest ? null : best;
    }

    /**
     * Returns -1 when signatures are incompatible. Higher scores are better matches. Exact
     * normalized type matches beat simple-name matches, and both beat the {@code Object}
     * placeholder fallback. This preserves real {@code Object} overloads while still allowing
     * unknown spec expressions to resolve to the only compatible production overload.
     */
    private static int parameterCompatibilityScore(MethodDescriptor descriptor, List<String> productionTypes) {
        return parameterCompatibilityScore(descriptor.parameterTypes(), descriptor, productionTypes);
    }

    private static int parameterCompatibilityScore(List<String> inferredTypes, List<String> productionTypes) {
        return parameterCompatibilityScore(inferredTypes, null, productionTypes);
    }

    private static int parameterCompatibilityScore(
            List<String> inferredTypes,
            MethodDescriptor descriptor,
            List<String> productionTypes
    ) {
        if (inferredTypes.size() != productionTypes.size()) {
            return -1;
        }
        int score = 0;
        for (int i = 0; i < inferredTypes.size(); i++) {
            String inferred = MethodDescriptor.normalizedTypeName(inferredTypes.get(i));
            String production = MethodDescriptor.normalizedTypeName(productionTypes.get(i));
            if (inferred.equals(production)) {
                score += 4;
                continue;
            }
            if (simpleName(inferred).equals(simpleName(production))) {
                score += 3;
                continue;
            }
            if ("Object".equals(inferred) && isUnknownOrLegacyPlaceholder(descriptor, i)) {
                score += 1;
                continue;
            }
            return -1;
        }
        return score;
    }

    private static boolean isUnknownOrLegacyPlaceholder(MethodDescriptor descriptor, int index) {
        if (descriptor == null) {
            return true;
        }
        if (descriptor.isParameterTypeUnknown(index)) {
            return true;
        }
        return index < descriptor.parameterNames().size()
                && ("arg" + index).equals(descriptor.parameterNames().get(index));
    }

    private static String simpleName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot < 0 ? typeName : typeName.substring(lastDot + 1);
    }

    private static void collectMembers(
            ClassTree classTree,
            String packageName,
            Map<String, String> imports,
            List<ProductionMethod> methods,
            List<ProductionMethod> constructors
    ) {
        Map<String, String> classTypeBounds = typeVariableBounds(
                classTree.getTypeParameters(), imports, packageName,
                new LinkedHashMap<String, String>());
        List<? extends Tree> members = classTree.getMembers();
        for (int i = 0; i < members.size(); i++) {
            Tree member = members.get(i);
            if (!(member instanceof MethodTree)) {
                continue;
            }
            MethodTree methodTree = (MethodTree) member;
            String methodName = methodTree.getName().toString();
            boolean constructor = "<init>".equals(methodName);
            if (!constructor
                    && !methodTree.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                continue;
            }
            Map<String, String> typeBounds = typeVariableBounds(
                    methodTree.getTypeParameters(), imports, packageName, classTypeBounds);
            List<String> parameterTypes = new ArrayList<String>();
            List<String> parameterNames = new ArrayList<String>();
            List<? extends VariableTree> parameters = methodTree.getParameters();
            for (int j = 0; j < parameters.size(); j++) {
                VariableTree parameter = parameters.get(j);
                parameterTypes.add(resolveProductionType(
                        parameter.getType().toString(), typeBounds, imports, packageName));
                parameterNames.add(parameter.getName().toString());
            }
            if (constructor) {
                constructors.add(new ProductionMethod(
                        methodName, "void", false, parameterTypes, parameterNames));
                continue;
            }
            Tree returnTypeTree = methodTree.getReturnType();
            if (returnTypeTree == null) {
                continue;
            }
            String returnType = SpecDiscovery.resolveTypeName(
                    returnTypeTree.toString(), imports, packageName);
            boolean isStatic = methodTree.getModifiers().getFlags().contains(Modifier.STATIC);
            methods.add(new ProductionMethod(
                    methodName, returnType, isStatic, parameterTypes, parameterNames));
        }
    }

    private static Map<String, String> typeVariableBounds(
            List<? extends TypeParameterTree> parameters,
            Map<String, String> imports,
            String packageName,
            Map<String, String> inherited
    ) {
        Map<String, String> result = new LinkedHashMap<String, String>(inherited);
        for (int i = 0; i < parameters.size(); i++) {
            TypeParameterTree parameter = parameters.get(i);
            String bound = "java.lang.Object";
            if (!parameter.getBounds().isEmpty()) {
                bound = SpecDiscovery.resolveTypeName(
                        parameter.getBounds().get(0).toString(), imports, packageName);
            }
            result.put(parameter.getName().toString(), bound);
        }
        return result;
    }

    private static String resolveProductionType(
            String sourceType,
            Map<String, String> typeBounds,
            Map<String, String> imports,
            String packageName
    ) {
        String normalized = sourceType.trim().replace("...", "[]");
        int arrayStart = normalized.indexOf('[');
        String raw = arrayStart < 0 ? normalized : normalized.substring(0, arrayStart).trim();
        String suffix = arrayStart < 0 ? "" : normalized.substring(arrayStart).replaceAll("\\s+", "");
        String bound = typeBounds.get(raw);
        if (bound != null) return bound + suffix;
        return SpecDiscovery.resolveTypeName(normalized, imports, packageName);
    }

    private static ClassTree topLevelType(CompilationUnitTree unit, String simpleName) {
        List<? extends Tree> declarations = unit.getTypeDecls();
        for (int i = 0; i < declarations.size(); i++) {
            Tree declaration = declarations.get(i);
            if (declaration instanceof ClassTree
                    && simpleName.equals(((ClassTree) declaration).getSimpleName().toString())) {
                return (ClassTree) declaration;
            }
        }
        return null;
    }

    private static String packageNameOf(CompilationUnitTree unit) {
        return unit.getPackageName() == null ? "" : unit.getPackageName().toString();
    }

    private static Map<String, String> importsBySimpleName(CompilationUnitTree unit) {
        Map<String, String> imports = new LinkedHashMap<String, String>();
        List<? extends ImportTree> importTrees = unit.getImports();
        for (int i = 0; i < importTrees.size(); i++) {
            ImportTree importTree = importTrees.get(i);
            if (importTree.isStatic()) {
                continue;
            }
            String qualified = importTree.getQualifiedIdentifier().toString();
            if (qualified.endsWith(".*")) {
                continue;
            }
            int lastDot = qualified.lastIndexOf('.');
            String simpleName = lastDot < 0 ? qualified : qualified.substring(lastDot + 1);
            imports.put(simpleName, qualified);
        }
        return imports;
    }

    private static final class ProductionMethod {
        final String name;
        final String returnType;
        final boolean isStatic;
        final List<String> parameterTypes;
        final List<String> parameterNames;

        ProductionMethod(
                String name,
                String returnType,
                boolean isStatic,
                List<String> parameterTypes,
                List<String> parameterNames
        ) {
            this.name = name;
            this.returnType = returnType;
            this.isStatic = isStatic;
            this.parameterTypes = parameterTypes;
            this.parameterNames = parameterNames;
        }
    }
}
