package io.github.jvmspec.internal.type;

import java.util.ArrayList;
import java.util.List;

/** Balanced Java source-token splitter used for parameter, type, and argument lists. */
public final class JavaSyntaxSplitter {
    private JavaSyntaxSplitter() {
    }

    public static List<String> splitTopLevel(String source, char delimiter) {
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        int angle = 0;
        int paren = 0;
        int bracket = 0;
        int brace = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if ((inString || inChar) && c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }
            if (!inChar && c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }
            if (!inString && c == '\'') {
                inChar = !inChar;
                current.append(c);
                continue;
            }
            if (!inString && !inChar) {
                if (c == '<') angle++;
                else if (c == '>' && angle > 0) angle--;
                else if (c == '(') paren++;
                else if (c == ')' && paren > 0) paren--;
                else if (c == '[') bracket++;
                else if (c == ']' && bracket > 0) bracket--;
                else if (c == '{') brace++;
                else if (c == '}' && brace > 0) brace--;
                else if (c == delimiter && angle == 0 && paren == 0
                        && bracket == 0 && brace == 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0 || source.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }
}
