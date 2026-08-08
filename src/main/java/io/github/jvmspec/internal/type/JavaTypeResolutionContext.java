package io.github.jvmspec.internal.type;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Java source-level type names to canonical erased identities within one source file.
 * This keeps import resolution separate from JVM constructor-signature comparison.
 */
public final class JavaTypeResolutionContext {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "(?m)^\\s*package\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "(?m)^\\s*import\\s+(?!static\\s+)([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern TYPE_PARAMETER_PATTERN = Pattern.compile(
            "(?:^|,)\\s*([A-Za-z_$][A-Za-z0-9_$]*)"
                    + "(?:\\s+extends\\s+(.+?))?\\s*(?=,|$)");

    private static final Map<String, String> JAVA_LANG_TYPES = javaLangTypes();

    private final String packageName;
    private final Map<String, String> imports;
    private final Map<String, String> typeVariableBounds;

    private JavaTypeResolutionContext(
            String packageName,
            Map<String, String> imports,
            Map<String, String> typeVariableBounds
    ) {
        this.packageName = packageName;
        this.imports = Collections.unmodifiableMap(new LinkedHashMap<String, String>(imports));
        this.typeVariableBounds = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(typeVariableBounds));
    }

    public static JavaTypeResolutionContext fromSource(String source) {
        String packageName = "";
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(source);
        if (packageMatcher.find()) packageName = packageMatcher.group(1);

        Map<String, String> imports = new LinkedHashMap<String, String>();
        Matcher importMatcher = IMPORT_PATTERN.matcher(source);
        while (importMatcher.find()) {
            String qualifiedName = importMatcher.group(1);
            int lastDot = qualifiedName.lastIndexOf('.');
            if (lastDot >= 0) {
                imports.put(qualifiedName.substring(lastDot + 1), qualifiedName);
            }
        }
        return new JavaTypeResolutionContext(
                packageName, imports, Collections.<String, String>emptyMap());
    }

    public JavaTypeResolutionContext withTypeParameters(String typeParameters) {
        if (typeParameters == null || typeParameters.trim().length() == 0) return this;
        String content = typeParameters.trim();
        if (content.startsWith("<") && content.endsWith(">")) {
            content = content.substring(1, content.length() - 1);
        }
        Map<String, String> bounds = new LinkedHashMap<String, String>(typeVariableBounds);
        Matcher matcher = TYPE_PARAMETER_PATTERN.matcher(content);
        while (matcher.find()) {
            String bound = matcher.group(2);
            if (bound == null || bound.trim().length() == 0) bound = "java.lang.Object";
            int additionalBound = bound.indexOf('&');
            if (additionalBound >= 0) bound = bound.substring(0, additionalBound);
            bounds.put(matcher.group(1), bound.trim());
        }
        return new JavaTypeResolutionContext(packageName, imports, bounds);
    }

    public String resolveErased(String sourceType) {
        String normalized = eraseGenericArguments(
                stripAnnotations(sourceType.trim()).replace("...", "[]"));
        int arrayStart = normalized.indexOf('[');
        String suffix = arrayStart < 0 ? "" : normalized.substring(arrayStart).replaceAll("\\s+", "");
        String raw = arrayStart < 0 ? normalized : normalized.substring(0, arrayStart);
        raw = raw.replaceAll("\\s+", "");
        return resolveRaw(raw) + suffix;
    }

    private String resolveRaw(String raw) {
        if (isPrimitive(raw)) return raw;
        String bound = typeVariableBounds.get(raw);
        if (bound != null) return resolveErased(bound);

        int firstDot = raw.indexOf('.');
        if (firstDot >= 0) {
            String first = raw.substring(0, firstDot);
            String imported = imports.get(first);
            if (imported != null) return imported + raw.substring(firstDot);
            if (Character.isLowerCase(raw.charAt(0))) return raw;
            return qualifyPackage(raw);
        }

        String imported = imports.get(raw);
        if (imported != null) return imported;
        String javaLang = JAVA_LANG_TYPES.get(raw);
        if (javaLang != null) return javaLang;
        return qualifyPackage(raw);
    }

    private String qualifyPackage(String raw) {
        return packageName.length() == 0 ? raw : packageName + "." + raw;
    }

    private static String eraseGenericArguments(String value) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') depth++;
            else if (c == '>' && depth > 0) depth--;
            else if (depth == 0) result.append(c);
        }
        return result.toString().trim();
    }

    private static String stripAnnotations(String source) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < source.length()) {
            char c = source.charAt(index);
            if (c != '@') {
                result.append(c);
                index++;
                continue;
            }
            index++;
            while (index < source.length()) {
                char name = source.charAt(index);
                if (Character.isJavaIdentifierPart(name) || name == '.') index++;
                else break;
            }
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++;
            if (index < source.length() && source.charAt(index) == '(') {
                int depth = 1;
                index++;
                while (index < source.length() && depth > 0) {
                    char nested = source.charAt(index++);
                    if (nested == '(') depth++;
                    else if (nested == ')') depth--;
                }
            }
            result.append(' ');
        }
        return result.toString().replaceAll("\\bfinal\\s+", "").trim();
    }

    private static boolean isPrimitive(String value) {
        return "boolean".equals(value) || "byte".equals(value) || "short".equals(value)
                || "int".equals(value) || "long".equals(value) || "float".equals(value)
                || "double".equals(value) || "char".equals(value) || "void".equals(value);
    }

    private static Map<String, String> javaLangTypes() {
        Map<String, String> result = new LinkedHashMap<String, String>();
        String[] names = {
                "Object", "String", "Boolean", "Byte", "Short", "Integer", "Long",
                "Float", "Double", "Character", "Number", "Class", "Enum", "Throwable",
                "Exception", "RuntimeException", "Error", "Iterable", "Comparable",
                "CharSequence", "Void"
        };
        for (int i = 0; i < names.length; i++) {
            result.put(names[i], "java.lang." + names[i]);
        }
        return Collections.unmodifiableMap(result);
    }
}
