package io.github.jvmspec.discovery;

import io.github.jvmspec.internal.type.JavaSyntaxSplitter;
import io.github.jvmspec.internal.type.JavaTypeRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses Java imports and method-local type context used during spec discovery. */
final class JavaSourceContext {
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "(?m)^\\s*import\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "public\\s+void\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)\\s*"
                    + "(?:throws\\s+[^\\{]+)?\\{",
            Pattern.DOTALL
    );

    private JavaSourceContext() {
    }

    static Map<String, JavaExpressionTypeInference.MethodParameterInfo> parseMethods(
            String source,
            Map<String, String> imports,
            String describedPackageName
    ) {
        Map<String, JavaExpressionTypeInference.MethodParameterInfo> methods =
                new LinkedHashMap<String, JavaExpressionTypeInference.MethodParameterInfo>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(source);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            String parametersSource = methodMatcher.group(2).trim();
            List<String> types = new ArrayList<String>();
            List<String> names = new ArrayList<String>();
            if (parametersSource.length() > 0) {
                List<String> parameters = JavaSyntaxSplitter.splitTopLevel(parametersSource, ',');
                for (int i = 0; i < parameters.size(); i++) {
                    String parameter = parameters.get(i).trim();
                    int lastSpace = parameter.lastIndexOf(' ');
                    if (lastSpace >= 0) {
                        types.add(resolveTypeName(
                                parameter.substring(0, lastSpace).trim(),
                                imports,
                                describedPackageName
                        ));
                        names.add(parameter.substring(lastSpace + 1).trim());
                    }
                }
            }
            methods.put(methodName, new JavaExpressionTypeInference.MethodParameterInfo(
                    types, names, types.size()));
        }
        return methods;
    }

    static String findEnclosingMethod(String source, int position) {
        Matcher matcher = METHOD_PATTERN.matcher(source);
        String lastMethod = null;
        while (matcher.find() && matcher.start() < position) {
            lastMethod = matcher.group(1);
        }
        return lastMethod;
    }

    static Map<String, JavaExpressionTypeInference.MethodParameterInfo> methodInfoFromScan(
            SpecCallScanner.ScanResult scan,
            Map<String, String> imports,
            String describedPackageName
    ) {
        Map<String, JavaExpressionTypeInference.MethodParameterInfo> methods =
                new LinkedHashMap<String, JavaExpressionTypeInference.MethodParameterInfo>();
        for (Map.Entry<String, SpecCallScanner.SpecMethodParams> entry : scan.specMethods.entrySet()) {
            SpecCallScanner.SpecMethodParams parameters = entry.getValue();
            List<String> types = new ArrayList<String>();
            for (int i = 0; i < parameters.typeTexts.size(); i++) {
                String typeText = parameters.typeTexts.get(i);
                String initializerText = i < parameters.initializerTexts.size()
                        ? parameters.initializerTexts.get(i)
                        : null;
                if ("var".equals(typeText) && initializerText != null) {
                    types.add(JavaLiteralTypeInference.infer(
                            initializerText, imports, describedPackageName).typeName);
                } else {
                    types.add(resolveTypeName(typeText, imports, describedPackageName));
                }
            }
            methods.put(entry.getKey(), new JavaExpressionTypeInference.MethodParameterInfo(
                    types, parameters.names, parameters.formalParameterCount));
        }
        return methods;
    }

    static Map<String, String> importsBySimpleName(String source) {
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

    static String resolveTypeName(
            String typeName,
            Map<String, String> imports,
            String describedPackageName
    ) {
        return JavaTypeRef.resolve(typeName, imports, describedPackageName).canonicalName();
    }

    static boolean isJavaIdentifier(String value) {
        if (value == null || value.length() == 0) return false;
        int index = 0;
        int firstCodePoint = value.codePointAt(index);
        if (!Character.isJavaIdentifierStart(firstCodePoint)) return false;
        index += Character.charCount(firstCodePoint);
        while (index < value.length()) {
            int codePoint = value.codePointAt(index);
            if (!Character.isJavaIdentifierPart(codePoint)) return false;
            index += Character.charCount(codePoint);
        }
        return true;
    }

    static String decapitalize(String value) {
        if (value.length() == 0) return value;
        if (value.length() > 1
                && Character.isUpperCase(value.charAt(0))
                && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
