package io.github.jvmspec.generation;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plans and applies source-preserving Java record component header updates from constructor
 * descriptors discovered in specs.
 */
final class RecordComponentPlanner {
    private static final List<String> RESERVED_IDENTIFIERS = Collections.unmodifiableList(Arrays.asList(
            "_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short",
            "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "void", "volatile", "while"
    ));

    private RecordComponentPlanner() {
    }

    static List<Component> componentsFor(DescribedType describedType) {
        return componentsFor(describedType, new ArrayList<ParsedComponent>());
    }

    private static List<Component> componentsFor(
            DescribedType describedType,
            List<ParsedComponent> existingComponents
    ) {
        List<Component> components = new ArrayList<Component>();
        if (!JavaTypeKind.RECORD.equals(describedType.kind()) || !describedType.hasConstructors()) {
            return components;
        }
        ConstructorDescriptor constructor = canonicalConstructor(describedType);
        Set<String> usedNames = new LinkedHashSet<String>();
        Set<Integer> usedMethodIndexes = new LinkedHashSet<Integer>();
        for (int i = 0; i < constructor.parameterTypes().size(); i++) {
            String type = constructor.parameterTypes().get(i);
            String parameterName = constructor.parameterNames().get(i);
            String name;
            if (i < existingComponents.size() && sameSourceType(type, existingComponents.get(i).type())) {
                name = existingComponents.get(i).name();
                int existingAccessor = findMethodIndex(name, type, describedType.methods(), usedMethodIndexes);
                if (existingAccessor >= 0) {
                    usedMethodIndexes.add(Integer.valueOf(existingAccessor));
                }
            } else {
                name = componentName(type, parameterName, describedType.methods(), usedMethodIndexes);
            }
            validateComponentName(
                    describedType, i, type, parameterName, name, describedType.methods(), usedNames);
            usedNames.add(name);
            components.add(new Component(type, name));
        }
        return components;
    }

    static String updateRecordHeader(String source, DescribedType describedType) {
        if (!JavaTypeKind.RECORD.equals(describedType.kind()) || !describedType.hasConstructors()) {
            return source;
        }
        RecordHeader header = findHeader(source, describedType.simpleName());
        if (header == null) {
            return source;
        }
        List<ParsedComponent> existingComponents = parseExistingComponents(
                source.substring(header.openParen + 1, header.closeParen));
        List<Component> plannedComponents = componentsFor(describedType, existingComponents);
        if (plannedComponents.isEmpty()) {
            return source;
        }
        if (!existingComponents.isEmpty()) {
            if (componentTypesMatch(existingComponents, plannedComponents)) {
                return source;
            }
            if (!isPrefix(existingComponents, plannedComponents)) {
                return source;
            }
        }

        String componentList = renderComponentList(describedType, existingComponents, plannedComponents);
        String currentComponentList = source.substring(header.openParen + 1, header.closeParen);
        if (currentComponentList.equals(componentList)) {
            return source;
        }
        return source.substring(0, header.openParen + 1)
                + componentList
                + source.substring(header.closeParen);
    }

    static boolean isImplicitAccessor(Component component, MethodDescriptor method) {
        return component.name().equals(method.methodName())
                && !method.isStatic()
                && !method.hasParameters()
                && !method.isVoid()
                && sameSourceType(component.type(), method.returnType());
    }

    static String renderComponentList(DescribedType owner, List<Component> components) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            Component component = components.get(i);
            builder.append(sourceTypeName(owner, component.type())).append(" ").append(component.name());
        }
        return builder.toString();
    }

    private static String renderComponentList(
            DescribedType owner,
            List<ParsedComponent> existingComponents,
            List<Component> plannedComponents
    ) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < plannedComponents.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            if (i < existingComponents.size()) {
                builder.append(existingComponents.get(i).sourceText().trim());
            } else {
                Component component = plannedComponents.get(i);
                builder.append(sourceTypeName(owner, component.type())).append(" ").append(component.name());
            }
        }
        return builder.toString();
    }

    private static ConstructorDescriptor canonicalConstructor(DescribedType describedType) {
        List<ConstructorDescriptor> constructors = describedType.constructors();
        ConstructorDescriptor selected = constructors.get(0);
        for (int i = 1; i < constructors.size(); i++) {
            ConstructorDescriptor candidate = constructors.get(i);
            if (candidate.parameterTypes().size() > selected.parameterTypes().size()) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static String componentName(
            String type,
            String parameterName,
            List<MethodDescriptor> methods,
            Set<Integer> usedMethodIndexes
    ) {
        if (isLegalJavaIdentifier(parameterName)) {
            int exactMethod = findMethodIndex(parameterName, type, methods, usedMethodIndexes);
            if (exactMethod >= 0) {
                usedMethodIndexes.add(Integer.valueOf(exactMethod));
                return parameterName;
            }
            if (!isPositionalName(parameterName) && !isTypeDerivedName(type, parameterName)) {
                return parameterName;
            }
        }
        int valueMethod = findMethodIndex("value", type, methods, usedMethodIndexes);
        if (valueMethod >= 0) {
            usedMethodIndexes.add(Integer.valueOf(valueMethod));
            return "value";
        }
        int uniqueCompatible = findUniqueCompatibleMethodIndex(type, methods, usedMethodIndexes);
        if (uniqueCompatible >= 0) {
            usedMethodIndexes.add(Integer.valueOf(uniqueCompatible));
            return methods.get(uniqueCompatible).methodName();
        }
        return null;
    }

    private static int findMethodIndex(
            String methodName,
            String type,
            List<MethodDescriptor> methods,
            Set<Integer> usedMethodIndexes
    ) {
        for (int i = 0; i < methods.size(); i++) {
            if (usedMethodIndexes.contains(Integer.valueOf(i))) {
                continue;
            }
            MethodDescriptor method = methods.get(i);
            if (methodName.equals(method.methodName()) && isComponentAccessorCandidate(type, method)) {
                return i;
            }
        }
        return -1;
    }

    private static int findUniqueCompatibleMethodIndex(
            String type,
            List<MethodDescriptor> methods,
            Set<Integer> usedMethodIndexes
    ) {
        int result = -1;
        for (int i = 0; i < methods.size(); i++) {
            if (usedMethodIndexes.contains(Integer.valueOf(i))) {
                continue;
            }
            if (!isComponentAccessorCandidate(type, methods.get(i))) {
                continue;
            }
            if (result >= 0) {
                return -1;
            }
            result = i;
        }
        return result;
    }

    private static boolean isComponentAccessorCandidate(String componentType, MethodDescriptor method) {
        return !method.isStatic()
                && !method.hasParameters()
                && !method.isVoid()
                && sameSourceType(componentType, method.returnType());
    }

    private static void validateComponentName(
            DescribedType describedType,
            int index,
            String type,
            String parameterName,
            String proposedName,
            List<MethodDescriptor> methods,
            Set<String> usedNames
    ) {
        String reason = null;
        if (proposedName == null) {
            reason = isLegalJavaIdentifier(parameterName)
                    ? "no matching accessor expectation or reliable constructor parameter"
                    : "illegal Java identifier and no matching accessor expectation";
        } else if (!isLegalJavaIdentifier(proposedName)) {
            reason = "illegal Java identifier";
        } else if (usedNames.contains(proposedName)) {
            reason = "component name is not unique or mapped to exactly one constructor position";
        }
        if (reason == null) {
            return;
        }
        throw new IllegalArgumentException(
                "AMBIGUOUS_RECORD_COMPONENT_NAME: subject " + describedType.qualifiedName()
                        + ", component index " + index
                        + ", inferred type " + type
                        + ", available naming evidence [constructor parameter '" + parameterName
                        + "', compatible accessors " + compatibleAccessorNames(type, methods) + "]"
                        + ", reason: " + reason + "."
        );
    }

    private static List<String> compatibleAccessorNames(String type, List<MethodDescriptor> methods) {
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (isComponentAccessorCandidate(type, method) && isLegalJavaIdentifier(method.methodName())) {
                names.add(method.methodName());
            }
        }
        return names;
    }

    private static boolean isLegalJavaIdentifier(String value) {
        return isJavaIdentifier(value) && !RESERVED_IDENTIFIERS.contains(value);
    }

    private static boolean isTypeDerivedName(String type, String name) {
        String rawType = type;
        int genericStart = rawType.indexOf('<');
        if (genericStart >= 0) {
            rawType = rawType.substring(0, genericStart);
        }
        int lastDot = rawType.lastIndexOf('.');
        String simpleType = lastDot < 0 ? rawType : rawType.substring(lastDot + 1);
        if (simpleType.length() == 0) {
            return false;
        }
        String derived = Character.toLowerCase(simpleType.charAt(0)) + simpleType.substring(1);
        return derived.equals(name);
    }

    private static boolean isPositionalName(String name) {
        if (name == null || !name.startsWith("arg") || name.length() == 3) {
            return false;
        }
        for (int i = 3; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean componentTypesMatch(List<ParsedComponent> existing, List<Component> planned) {
        if (existing.size() != planned.size()) {
            return false;
        }
        return isPrefix(existing, planned);
    }

    private static boolean isPrefix(List<ParsedComponent> existing, List<Component> planned) {
        if (existing.size() > planned.size()) {
            return false;
        }
        for (int i = 0; i < existing.size(); i++) {
            if (!sameSourceType(existing.get(i).type(), planned.get(i).type())) {
                return false;
            }
        }
        return true;
    }

    private static RecordHeader findHeader(String source, String simpleName) {
        String masked = maskNonCode(source);
        Pattern pattern = Pattern.compile(
                "\\brecord\\s+" + Pattern.quote(simpleName) + "\\b(?:\\s*<[^\\n;{}()]+>)?\\s*\\(",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(masked);
        if (!matcher.find()) {
            return null;
        }
        int openParen = matcher.end() - 1;
        int closeParen = findMatchingParenthesis(masked, openParen);
        if (closeParen < 0) {
            return null;
        }
        return new RecordHeader(openParen, closeParen);
    }

    private static List<ParsedComponent> parseExistingComponents(String componentSource) {
        List<ParsedComponent> result = new ArrayList<ParsedComponent>();
        List<String> parts = splitArguments(componentSource);
        for (int i = 0; i < parts.size(); i++) {
            String original = parts.get(i).trim();
            if (original.length() == 0) {
                continue;
            }
            String component = stripRecordComponentDecorators(original);
            int lastSpace = component.lastIndexOf(' ');
            if (lastSpace < 0) {
                continue;
            }
            String type = component.substring(0, lastSpace).trim();
            String name = component.substring(lastSpace + 1).trim();
            if (name.endsWith("[]")) {
                name = name.substring(0, name.length() - 2).trim();
                type = type + "[]";
            }
            if (type.length() > 0 && isJavaIdentifier(name)) {
                result.add(new ParsedComponent(original, type, name));
            }
        }
        return result;
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

    private static List<String> splitArguments(String text) {
        List<String> result = new ArrayList<String>();
        int start = 0;
        int angleDepth = 0;
        int parenDepth = 0;
        int bracketDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') {
                angleDepth++;
            } else if (c == '>' && angleDepth > 0) {
                angleDepth--;
            } else if (c == '(') {
                parenDepth++;
            } else if (c == ')' && parenDepth > 0) {
                parenDepth--;
            } else if (c == '[') {
                bracketDepth++;
            } else if (c == ']' && bracketDepth > 0) {
                bracketDepth--;
            } else if (c == ',' && angleDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
                result.add(text.substring(start, i));
                start = i + 1;
            }
        }
        String tail = text.substring(start);
        if (tail.trim().length() > 0 || text.trim().length() > 0) {
            result.add(tail);
        }
        return result;
    }

    private static int findMatchingParenthesis(String source, int openParen) {
        int depth = 0;
        for (int i = openParen; i < source.length(); i++) {
            char c = source.charAt(i);
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

    private static String maskNonCode(String source) {
        return NonCodeSourceMasker.mask(source);
    }

    private static boolean sameSourceType(String left, String right) {
        return normalizedSourceType(left).equals(normalizedSourceType(right));
    }

    private static String normalizedSourceType(String typeName) {
        String normalized = typeName.trim().replace("...", "[]").replace(" ", "");
        if (normalized.startsWith("java.lang.")) {
            normalized = normalized.substring("java.lang.".length());
        }
        int genericStart = normalized.indexOf('<');
        if (genericStart >= 0) {
            normalized = normalized.substring(0, genericStart);
        }
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0) {
            normalized = normalized.substring(lastDot + 1);
        }
        return normalized;
    }

    private static String sourceTypeName(DescribedType owner, String typeName) {
        String packageName = owner.packageName();
        if (packageName.length() > 0 && typeName.startsWith(packageName + ".")) {
            return typeName.substring(packageName.length() + 1);
        }
        if (typeName.startsWith("java.lang.")) {
            return typeName.substring("java.lang.".length());
        }
        return typeName;
    }

    private static boolean isJavaIdentifier(String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        int index = 0;
        int firstCodePoint = value.codePointAt(index);
        if (!Character.isJavaIdentifierStart(firstCodePoint)) {
            return false;
        }
        index += Character.charCount(firstCodePoint);
        while (index < value.length()) {
            int currentCodePoint = value.codePointAt(index);
            if (!Character.isJavaIdentifierPart(currentCodePoint)) {
                return false;
            }
            index += Character.charCount(currentCodePoint);
        }
        return true;
    }

    static final class Component {
        private final String type;
        private final String name;

        Component(String type, String name) {
            this.type = type;
            this.name = name;
        }

        String type() {
            return type;
        }

        String name() {
            return name;
        }
    }

    private static final class ParsedComponent {
        private final String sourceText;
        private final String type;
        private final String name;

        ParsedComponent(String sourceText, String type, String name) {
            this.sourceText = sourceText;
            this.type = type;
            this.name = name;
        }

        String sourceText() {
            return sourceText;
        }

        String type() {
            return type;
        }

        String name() {
            return name;
        }
    }

    private static final class RecordHeader {
        private final int openParen;
        private final int closeParen;

        RecordHeader(int openParen, int closeParen) {
            this.openParen = openParen;
            this.closeParen = closeParen;
        }
    }
}
