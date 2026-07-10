package io.github.jvmspec.discovery;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
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
        for (int i = 0; i < productionMethods.size(); i++) {
            ProductionMethod production = productionMethods.get(i);
            if (!production.name.equals(descriptor.methodName())
                    || production.isStatic != descriptor.isStatic()
                    || !parametersCompatible(descriptor.parameterTypes(), production.parameterTypes)) {
                continue;
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
        return descriptor;
    }

    private static ConstructorDescriptor refineConstructor(
            ConstructorDescriptor descriptor,
            List<ProductionMethod> productionConstructors
    ) {
        for (int i = 0; i < productionConstructors.size(); i++) {
            ProductionMethod production = productionConstructors.get(i);
            if (parametersCompatible(descriptor.parameterTypes(), production.parameterTypes)) {
                return ConstructorDescriptor.of(
                        production.parameterTypes, production.parameterNames, descriptor.bodyContent());
            }
        }
        return descriptor;
    }

    /**
     * Inferred parameters are compatible with production parameters when arity matches and each
     * inferred type either equals the production type (fully or by simple name) or is the
     * unknown placeholder {@code Object}. Conflicting concrete types mean the spec asks for a
     * different overload, which must stay as inferred so generation can create it.
     */
    private static boolean parametersCompatible(List<String> inferredTypes, List<String> productionTypes) {
        if (inferredTypes.size() != productionTypes.size()) {
            return false;
        }
        for (int i = 0; i < inferredTypes.size(); i++) {
            String inferred = inferredTypes.get(i);
            String production = productionTypes.get(i);
            if ("Object".equals(inferred) || inferred.equals(production)) {
                continue;
            }
            if (!simpleName(inferred).equals(simpleName(production))) {
                return false;
            }
        }
        return true;
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
        List<? extends Tree> members = classTree.getMembers();
        for (int i = 0; i < members.size(); i++) {
            Tree member = members.get(i);
            if (!(member instanceof MethodTree)) {
                continue;
            }
            MethodTree methodTree = (MethodTree) member;
            if (!methodTree.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                continue;
            }
            List<String> parameterTypes = new ArrayList<String>();
            List<String> parameterNames = new ArrayList<String>();
            List<? extends VariableTree> parameters = methodTree.getParameters();
            for (int j = 0; j < parameters.size(); j++) {
                VariableTree parameter = parameters.get(j);
                parameterTypes.add(SpecDiscovery.resolveTypeName(
                        parameter.getType().toString(), imports, packageName));
                parameterNames.add(parameter.getName().toString());
            }
            String methodName = methodTree.getName().toString();
            if ("<init>".equals(methodName)) {
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
