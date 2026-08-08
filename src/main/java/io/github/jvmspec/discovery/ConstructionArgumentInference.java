package io.github.jvmspec.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.jvmspec.discovery.JavaExpressionTypeInference.findEnclosingMethod;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferLiteralType;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.parameterNameForArgument;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.typeDerivedParameterName;

/** Infers portable constructor parameter types and names from Java construction observations. */
final class ConstructionArgumentInference {
    private ConstructionArgumentInference() {
    }

    static Arguments infer(
            List<String> argumentValues,
            String source,
            int position,
            Map<String, JavaExpressionTypeInference.MethodParameterInfo> methods,
            Map<String, String> imports,
            String describedPackageName
    ) {
        JavaExpressionTypeInference.MethodParameterInfo info = null;
        String methodName = findEnclosingMethod(source, position, methods);
        if (methodName != null && methods.containsKey(methodName)) {
            info = methods.get(methodName);
        }
        return infer(argumentValues, info, imports, describedPackageName);
    }

    static Arguments infer(
            List<String> argumentValues,
            JavaExpressionTypeInference.MethodParameterInfo info,
            Map<String, String> imports,
            String describedPackageName
    ) {
        List<String> parameterTypes = new ArrayList<String>();
        List<String> parameterNames = new ArrayList<String>();
        for (int i = 0; i < argumentValues.size(); i++) {
            String argument = argumentValues.get(i).trim();
            if (info != null) {
                int parameterIndex = info.names.indexOf(argument);
                if (parameterIndex >= 0) {
                    parameterTypes.add(info.types.get(parameterIndex));
                    parameterNames.add(info.names.get(parameterIndex));
                    continue;
                }
                if (i < info.formalParameterCount) {
                    parameterTypes.add(info.types.get(i));
                    parameterNames.add(info.names.get(i));
                    continue;
                }
            }
            parameterTypes.add(inferLiteralType(argument, imports, describedPackageName).typeName);
            String parameterName = parameterNameForArgument(argument, i);
            if (parameterName.equals("arg" + i)) {
                String derivedName = typeDerivedParameterName(argument);
                if (derivedName != null && !parameterNames.contains(derivedName)) {
                    parameterName = derivedName;
                }
            }
            parameterNames.add(parameterName);
        }
        return new Arguments(parameterTypes, parameterNames);
    }

    static final class Arguments {
        final List<String> parameterTypes;
        final List<String> parameterNames;

        Arguments(List<String> parameterTypes, List<String> parameterNames) {
            this.parameterTypes = parameterTypes;
            this.parameterNames = parameterNames;
        }
    }
}
