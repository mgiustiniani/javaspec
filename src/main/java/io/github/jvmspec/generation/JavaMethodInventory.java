package io.github.jvmspec.generation;

import static io.github.jvmspec.generation.JavaSourceEditor.*;

import io.github.jvmspec.generation.parser.JavaSourceParserLoader;
import io.github.jvmspec.generation.parser.ParsedSource;
import io.github.jvmspec.internal.type.JavaIdentifiers;
import io.github.jvmspec.internal.type.JavaSyntaxSplitter;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Inventories direct Java members and computes deterministic missing method contracts. */
final class JavaMethodInventory {
    private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile(
            "(?m)(?:^|[\\s;{}])" +
            "(?:(?:public|protected|private)\\s+)?" +
            "(?:(?:static|final|synchronized|abstract|native|strictfp)\\s+)*" +
            "(?:<[^\\n;{}()]+>\\s+)?" +
            "([A-Za-z_$][A-Za-z0-9_$.<>?\\[\\]]*(?:\\s*\\[\\])?)\\s+" +
            "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)"
    );

    private JavaMethodInventory() {
    }

    static List<MethodDescriptor> missingMethods(String source, DescribedType describedType) {
        String recordSimpleName = JavaTypeKind.RECORD.equals(describedType.kind())
                ? describedType.simpleName()
                : null;
        String sourceWithPlannedComponents = recordSimpleName == null
                ? source
                : RecordComponentPlanner.updateRecordHeader(source, describedType);
        return missingMethodsInScope(
                sourceWithPlannedComponents,
                JavaMethodEligibility.eligibleMethods(describedType),
                recordSimpleName,
                describedType.simpleName()
        );
    }

    static List<MethodDescriptor> missingMethodsInScope(String scopeSource, List<MethodDescriptor> methods) {
        return missingMethodsInScope(scopeSource, methods, null, null);
    }

    static List<MethodDescriptor> missingMethodsInScope(
            String scopeSource,
            List<MethodDescriptor> methods,
            String recordSimpleName,
            String primaryTypeSimpleName
    ) {
        // Restrict signature checks to direct members. Methods inside local, anonymous, nested, or
        // secondary top-level types must not satisfy a behavior requested from the described type.
        String directMemberSource = directMemberSource(scopeSource, primaryTypeSimpleName);
        ParsedSource parsed = JavaSourceParserLoader.select(effectiveParserClassLoader()).parse(directMemberSource);
        Set<String> existingSignatures = existingMethodSignatures(directMemberSource);
        if (recordSimpleName != null) {
            existingSignatures.addAll(recordComponentAccessorSignatures(scopeSource, recordSimpleName));
        }
        List<MethodDescriptor> missing = new ArrayList<MethodDescriptor>();
        Set<String> plannedSignatures = new LinkedHashSet<String>();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            String key = signatureKey(method.methodName(), method.parameterTypes());
            boolean existsBySignatureKey = existingSignatures.contains(key);
            boolean existsByParser = parsed.hasMethod(method.methodName(), method.parameterTypes());
            if (existsBySignatureKey || existsByParser || plannedSignatures.contains(key)) {
                continue;
            }
            missing.add(method);
            plannedSignatures.add(key);
        }
        return missing;
    }


    private static String directMemberSource(String source, String primaryTypeSimpleName) {
        String masked = maskNonCode(source);
        int bodyStart = 0;
        int bodyEnd = masked.length();
        if (primaryTypeSimpleName != null) {
            int openBrace = findPrimaryTypeOpenBrace(masked, primaryTypeSimpleName);
            if (openBrace < 0) {
                return masked;
            }
            int closeBrace = findMatchingBraceInMasked(masked, openBrace);
            if (closeBrace < 0) {
                return masked;
            }
            bodyStart = openBrace + 1;
            bodyEnd = closeBrace;
        }

        char[] direct = masked.substring(bodyStart, bodyEnd).toCharArray();
        int depth = 0;
        for (int i = 0; i < direct.length; i++) {
            char current = direct[i];
            if (current == '{') {
                depth++;
                direct[i] = ' ';
            } else if (current == '}') {
                direct[i] = ' ';
                if (depth > 0) {
                    depth--;
                }
            } else if (depth > 0) {
                direct[i] = blankedChar(current);
            }
        }
        return new String(direct);
    }

    private static Set<String> existingMethodSignatures(String source) {
        Set<String> signatures = new LinkedHashSet<String>();
        String codeOnlySource = maskNonCode(source);
        Matcher matcher = METHOD_SIGNATURE_PATTERN.matcher(codeOnlySource);
        while (matcher.find()) {
            String returnType = matcher.group(1);
            if (isStatementKeyword(returnType)) {
                continue;
            }
            String methodName = matcher.group(2);
            List<String> parameterTypes = parseParameterTypes(matcher.group(3));
            signatures.add(signatureKey(methodName, parameterTypes));
        }
        return signatures;
    }

    private static Set<String> recordComponentAccessorSignatures(String source, String simpleName) {
        Set<String> signatures = new LinkedHashSet<String>();
        String codeOnlySource = maskNonCode(source);
        Pattern recordPattern = Pattern.compile(
                "\\brecord\\s+" + Pattern.quote(simpleName) + "\\b(?:\\s*<[^\\n;{}()]+>)?\\s*\\(",
                Pattern.DOTALL
        );
        Matcher matcher = recordPattern.matcher(codeOnlySource);
        if (!matcher.find()) {
            return signatures;
        }
        int openParen = matcher.end() - 1;
        int closeParen = findMatchingParenthesisInMasked(codeOnlySource, openParen);
        if (closeParen < 0) {
            return signatures;
        }
        String componentSource = codeOnlySource.substring(openParen + 1, closeParen);
        List<String> components = splitArguments(componentSource);
        for (int i = 0; i < components.size(); i++) {
            String component = stripRecordComponentDecorators(components.get(i).trim());
            int lastSpace = component.lastIndexOf(' ');
            if (lastSpace < 0) {
                continue;
            }
            String componentName = component.substring(lastSpace + 1).trim();
            if (componentName.endsWith("[]")) {
                componentName = componentName.substring(0, componentName.length() - 2).trim();
            }
            if (JavaIdentifiers.isIdentifier(componentName)) {
                signatures.add(signatureKey(componentName, new ArrayList<String>()));
            }
        }
        return signatures;
    }

    private static String stripRecordComponentDecorators(String component) {
        String result = component.trim();
        boolean changed = true;
        while (changed) {
            changed = false;
            while (result.startsWith("@")) {
                int index = 1;
                int nesting = 0;
                while (index < result.length()) {
                    char c = result.charAt(index);
                    if (c == '(') {
                        nesting++;
                    } else if (c == ')') {
                        if (nesting > 0) {
                            nesting--;
                        }
                    } else if (Character.isWhitespace(c) && nesting == 0) {
                        break;
                    }
                    index++;
                }
                result = index < result.length() ? result.substring(index + 1).trim() : "";
                changed = true;
            }
            if (result.startsWith("final ")) {
                result = result.substring("final ".length()).trim();
                changed = true;
            }
        }
        return result;
    }

    private static int findMatchingParenthesisInMasked(String masked, int openParen) {
        int depth = 0;
        for (int i = openParen; i < masked.length(); i++) {
            char c = masked.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static ClassLoader effectiveParserClassLoader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            return context;
        }
        return ClassMethodUpdater.class.getClassLoader();
    }

    private static boolean isStatementKeyword(String value) {
        return "return".equals(value)
                || "new".equals(value)
                || "if".equals(value)
                || "for".equals(value)
                || "while".equals(value)
                || "switch".equals(value)
                || "catch".equals(value);
    }

    private static List<String> parseParameterTypes(String parameterSource) {
        List<String> types = new ArrayList<String>();
        String trimmed = parameterSource.trim();
        if (trimmed.length() == 0) {
            return types;
        }
        List<String> parameters = splitArguments(trimmed);
        for (int i = 0; i < parameters.size(); i++) {
            String parameter = stripParameterDecorators(parameters.get(i).trim());
            int lastSpace = parameter.lastIndexOf(' ');
            if (lastSpace < 0) {
                continue;
            }
            String type = parameter.substring(0, lastSpace).trim();
            if (type.endsWith("...")) {
                type = type.substring(0, type.length() - 3) + "[]";
            }
            types.add(type);
        }
        return types;
    }

    private static String stripParameterDecorators(String parameter) {
        String result = parameter;
        while (result.startsWith("@")) {
            int space = result.indexOf(' ');
            if (space < 0) {
                return result;
            }
            result = result.substring(space + 1).trim();
        }
        while (result.startsWith("final ")) {
            result = result.substring("final ".length()).trim();
        }
        return result;
    }

    private static List<String> splitArguments(String arguments) {
        return JavaSyntaxSplitter.splitTopLevel(arguments, ',');
    }

    private static String signatureKey(String methodName, List<String> parameterTypes) {
        StringBuilder builder = new StringBuilder(methodName);
        builder.append('(');
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(normalizeType(parameterTypes.get(i)));
        }
        builder.append(')');
        return builder.toString();
    }

    private static String normalizeType(String typeName) {
        String normalized = typeName.trim().replace("...", "[]").replaceAll("\\s+", "");
        int arrayIndex = normalized.indexOf("[]");
        String suffix = "";
        if (arrayIndex >= 0) {
            suffix = normalized.substring(arrayIndex);
            normalized = normalized.substring(0, arrayIndex);
        }
        int genericIndex = normalized.indexOf('<');
        String generic = "";
        if (genericIndex >= 0) {
            generic = normalized.substring(genericIndex);
            normalized = normalized.substring(0, genericIndex);
        }
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0) {
            normalized = normalized.substring(lastDot + 1);
        }
        return normalized + generic + suffix;
    }
}
