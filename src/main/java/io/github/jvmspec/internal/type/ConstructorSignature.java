package io.github.jvmspec.internal.type;

import io.github.jvmspec.model.ConstructorDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Java constructor identity: declaring type plus ordered erased parameter types. */
public final class ConstructorSignature {
    private final String declaringType;
    private final List<String> erasedParameterTypes;

    private ConstructorSignature(String declaringType, List<String> erasedParameterTypes) {
        this.declaringType = Objects.requireNonNull(declaringType, "declaringType must not be null");
        this.erasedParameterTypes = Collections.unmodifiableList(
                new ArrayList<String>(erasedParameterTypes));
    }

    public static ConstructorSignature of(String declaringType, ConstructorDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        return of(declaringType, descriptor.parameterTypes());
    }

    public static ConstructorSignature of(String declaringType, List<String> parameterTypes) {
        List<String> erased = new ArrayList<String>();
        for (int i = 0; i < parameterTypes.size(); i++) {
            erased.add(erase(parameterTypes.get(i)));
        }
        return new ConstructorSignature(declaringType, erased);
    }

    public List<String> erasedParameterTypes() {
        return erasedParameterTypes;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ConstructorSignature)) return false;
        ConstructorSignature that = (ConstructorSignature) other;
        return declaringType.equals(that.declaringType)
                && erasedParameterTypes.equals(that.erasedParameterTypes);
    }

    public int hashCode() {
        return 31 * declaringType.hashCode() + erasedParameterTypes.hashCode();
    }

    public String toString() {
        return declaringType + erasedParameterTypes;
    }

    private static String erase(String source) {
        String value = stripAnnotations(source.trim()).replace("...", "[]");
        StringBuilder erased = new StringBuilder();
        int genericDepth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') {
                genericDepth++;
            } else if (c == '>' && genericDepth > 0) {
                genericDepth--;
            } else if (genericDepth == 0 && !Character.isWhitespace(c)) {
                erased.append(c);
            }
        }
        String normalized = erased.toString();
        int array = normalized.indexOf('[');
        String suffix = array < 0 ? "" : normalized.substring(array);
        String raw = array < 0 ? normalized : normalized.substring(0, array);
        int lastDot = raw.lastIndexOf('.');
        if (lastDot >= 0) raw = raw.substring(lastDot + 1);
        return raw + suffix;
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
}
