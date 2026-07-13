package io.github.jvmspec.internal.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Internal structured representation of a Java source type. */
public final class JavaTypeRef {
    public enum Kind { PRIMITIVE, DECLARED, ARRAY, WILDCARD }

    private final Kind kind;
    private final String name;
    private final List<JavaTypeRef> arguments;
    private final JavaTypeRef component;
    private final String wildcardBoundKind;

    private JavaTypeRef(
            Kind kind,
            String name,
            List<JavaTypeRef> arguments,
            JavaTypeRef component,
            String wildcardBoundKind
    ) {
        this.kind = kind;
        this.name = name;
        this.arguments = Collections.unmodifiableList(new ArrayList<JavaTypeRef>(arguments));
        this.component = component;
        this.wildcardBoundKind = wildcardBoundKind;
    }

    public static JavaTypeRef resolve(
            String source,
            Map<String, String> imports,
            String defaultPackage
    ) {
        Objects.requireNonNull(imports, "imports must not be null");
        return new Parser(source, imports, defaultPackage == null ? "" : defaultPackage).parse();
    }

    public static JavaTypeRef parseCanonical(String source) {
        return new Parser(source, Collections.<String, String>emptyMap(), "").parse();
    }

    public Kind kind() {
        return kind;
    }

    public String declaredName() {
        return name;
    }

    public List<JavaTypeRef> arguments() {
        return arguments;
    }

    public JavaTypeRef component() {
        return component;
    }

    public String wildcardBoundKind() {
        return wildcardBoundKind;
    }

    public boolean structurallyEquivalent(JavaTypeRef other) {
        if (other == null || !kind.equals(other.kind)) {
            return false;
        }
        if (Kind.ARRAY.equals(kind)) {
            return component.structurallyEquivalent(other.component);
        }
        if (Kind.WILDCARD.equals(kind)) {
            return Objects.equals(wildcardBoundKind, other.wildcardBoundKind)
                    && (component == null ? other.component == null
                    : component.structurallyEquivalent(other.component));
        }
        if (!equivalentDeclaredName(name, other.name) || arguments.size() != other.arguments.size()) {
            return false;
        }
        for (int i = 0; i < arguments.size(); i++) {
            if (!arguments.get(i).structurallyEquivalent(other.arguments.get(i))) {
                return false;
            }
        }
        return true;
    }

    public String canonicalName() {
        if (Kind.ARRAY.equals(kind)) {
            return component.canonicalName() + "[]";
        }
        if (Kind.WILDCARD.equals(kind)) {
            return component == null ? "?" : "? " + wildcardBoundKind + " " + component.canonicalName();
        }
        StringBuilder text = new StringBuilder(name);
        if (!arguments.isEmpty()) {
            text.append('<');
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) text.append(", ");
                text.append(arguments.get(i).canonicalName());
            }
            text.append('>');
        }
        return text.toString();
    }

    public Set<String> referencedQualifiedTypes() {
        Set<String> result = new LinkedHashSet<String>();
        collectQualifiedTypes(result);
        return result;
    }

    public String render(Map<String, String> simpleNamesByQualifiedName, String currentPackage) {
        if (Kind.ARRAY.equals(kind)) {
            return component.render(simpleNamesByQualifiedName, currentPackage) + "[]";
        }
        if (Kind.WILDCARD.equals(kind)) {
            return component == null
                    ? "?"
                    : "? " + wildcardBoundKind + " " + component.render(simpleNamesByQualifiedName, currentPackage);
        }
        String renderedName = renderDeclaredName(name, simpleNamesByQualifiedName, currentPackage);
        if (arguments.isEmpty()) {
            return renderedName;
        }
        StringBuilder text = new StringBuilder(renderedName).append('<');
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) text.append(", ");
            text.append(arguments.get(i).render(simpleNamesByQualifiedName, currentPackage));
        }
        return text.append('>').toString();
    }

    private void collectQualifiedTypes(Set<String> result) {
        if (Kind.ARRAY.equals(kind) || Kind.WILDCARD.equals(kind)) {
            if (component != null) component.collectQualifiedTypes(result);
            return;
        }
        if (Kind.DECLARED.equals(kind) && name.indexOf('.') >= 0 && !name.startsWith("java.lang.")) {
            result.add(name);
        }
        for (int i = 0; i < arguments.size(); i++) {
            arguments.get(i).collectQualifiedTypes(result);
        }
    }

    private static boolean equivalentDeclaredName(String left, String right) {
        if (left.equals(right)) {
            return true;
        }
        boolean leftQualified = left.indexOf('.') >= 0;
        boolean rightQualified = right.indexOf('.') >= 0;
        return leftQualified != rightQualified && simpleName(left).equals(simpleName(right));
    }

    private static String renderDeclaredName(
            String qualifiedName,
            Map<String, String> simpleNamesByQualifiedName,
            String currentPackage
    ) {
        if (qualifiedName.indexOf('.') < 0) return qualifiedName;
        if (qualifiedName.startsWith("java.lang.")) return simpleName(qualifiedName);
        if (currentPackage != null && currentPackage.length() > 0
                && qualifiedName.startsWith(currentPackage + ".")
                && qualifiedName.substring(currentPackage.length() + 1).indexOf('.') < 0) {
            return qualifiedName.substring(currentPackage.length() + 1);
        }
        String imported = simpleNamesByQualifiedName.get(qualifiedName);
        return imported == null ? qualifiedName : imported;
    }

    private static String simpleName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    private static boolean primitive(String name) {
        return "boolean".equals(name) || "byte".equals(name) || "short".equals(name)
                || "int".equals(name) || "long".equals(name) || "float".equals(name)
                || "double".equals(name) || "char".equals(name) || "void".equals(name);
    }

    private static boolean javaLang(String name) {
        return "String".equals(name) || "Object".equals(name) || "Boolean".equals(name)
                || "Byte".equals(name) || "Short".equals(name) || "Integer".equals(name)
                || "Long".equals(name) || "Float".equals(name) || "Double".equals(name)
                || "Character".equals(name) || "Class".equals(name) || "Number".equals(name)
                || "Throwable".equals(name) || "Exception".equals(name);
    }

    private static final class Parser {
        private final String text;
        private final Map<String, String> imports;
        private final String defaultPackage;
        private int index;

        private Parser(String text, Map<String, String> imports, String defaultPackage) {
            this.text = Objects.requireNonNull(text, "type source must not be null").trim().replace("...", "[]");
            this.imports = imports;
            this.defaultPackage = defaultPackage;
        }

        private JavaTypeRef parse() {
            JavaTypeRef result = parseType();
            whitespace();
            if (index != text.length()) {
                throw new IllegalArgumentException("UNRESOLVED_GENERIC_TYPE: unexpected type syntax at '"
                        + text.substring(index) + "' in " + text);
            }
            return result;
        }

        private JavaTypeRef parseType() {
            whitespace();
            if (peek('?')) {
                index++;
                whitespace();
                if (word("extends")) return wildcard("extends");
                if (word("super")) return wildcard("super");
                return new JavaTypeRef(Kind.WILDCARD, null, empty(), null, null);
            }
            String raw = name();
            Kind kind = primitive(raw) ? Kind.PRIMITIVE : Kind.DECLARED;
            String resolved = Kind.PRIMITIVE.equals(kind) ? raw : resolveName(raw);
            List<JavaTypeRef> arguments = new ArrayList<JavaTypeRef>();
            whitespace();
            if (peek('<')) {
                index++;
                do {
                    arguments.add(parseType());
                    whitespace();
                    if (peek(',')) {
                        index++;
                    } else {
                        break;
                    }
                } while (true);
                whitespace();
                require('>');
            }
            JavaTypeRef result = new JavaTypeRef(kind, resolved, arguments, null, null);
            whitespace();
            while (peek('[')) {
                index++;
                require(']');
                result = new JavaTypeRef(Kind.ARRAY, null, empty(), result, null);
                whitespace();
            }
            return result;
        }

        private JavaTypeRef wildcard(String boundKind) {
            whitespace();
            return new JavaTypeRef(Kind.WILDCARD, null, empty(), parseType(), boundKind);
        }

        private String resolveName(String raw) {
            if (raw.indexOf('.') >= 0) {
                String first = raw.substring(0, raw.indexOf('.'));
                String importedOuter = imports.get(first);
                if (importedOuter != null) return importedOuter + raw.substring(first.length());
                if (Character.isLowerCase(raw.charAt(0))) return raw;
                return defaultPackage.length() == 0 ? raw : defaultPackage + "." + raw;
            }
            if (raw.length() == 1 && Character.isUpperCase(raw.charAt(0))) return raw;
            String imported = imports.get(raw);
            if (imported != null) return imported;
            if (javaLang(raw)) return raw;
            return defaultPackage.length() == 0 ? raw : defaultPackage + "." + raw;
        }

        private String name() {
            whitespace();
            int start = index;
            while (index < text.length()) {
                char c = text.charAt(index);
                if (Character.isJavaIdentifierPart(c) || c == '.' || c == '$') index++;
                else break;
            }
            if (start == index) throw new IllegalArgumentException("UNRESOLVED_GENERIC_TYPE: expected type in " + text);
            return text.substring(start, index);
        }

        private boolean word(String value) {
            if (!text.startsWith(value, index)) return false;
            int end = index + value.length();
            if (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) return false;
            index = end;
            return true;
        }

        private void whitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        }

        private boolean peek(char c) {
            return index < text.length() && text.charAt(index) == c;
        }

        private void require(char c) {
            whitespace();
            if (!peek(c)) throw new IllegalArgumentException("UNRESOLVED_GENERIC_TYPE: expected '" + c + "' in " + text);
            index++;
        }

        private static List<JavaTypeRef> empty() {
            return Collections.emptyList();
        }
    }
}
