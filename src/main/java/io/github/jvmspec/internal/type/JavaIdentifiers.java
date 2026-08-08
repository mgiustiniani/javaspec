package io.github.jvmspec.internal.type;

/** Shared Unicode-aware Java identifier checks for internal source analysis. */
public final class JavaIdentifiers {
    private JavaIdentifiers() {
    }

    public static boolean isIdentifier(String value) {
        if (value == null || value.length() == 0) return false;
        int index = 0;
        int first = value.codePointAt(index);
        if (!Character.isJavaIdentifierStart(first)) return false;
        index += Character.charCount(first);
        while (index < value.length()) {
            int current = value.codePointAt(index);
            if (!Character.isJavaIdentifierPart(current)) return false;
            index += Character.charCount(current);
        }
        return true;
    }
}
