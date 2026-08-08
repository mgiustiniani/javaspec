package io.github.jvmspec.discovery;

import java.util.ArrayList;
import java.util.List;

/** Splits Java expression argument lists without treating relational angle brackets as generics. */
final class JavaExpressionArguments {
    private JavaExpressionArguments() {
    }

    static List<String> split(String arguments) {
        List<String> result = new ArrayList<String>();
        if (arguments == null || arguments.trim().length() == 0) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        int nesting = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = 0; i < arguments.length(); i++) {
            char character = arguments.charAt(i);
            if (escaped) {
                current.append(character);
                escaped = false;
                continue;
            }
            if (character == '\\' && (inString || inChar)) {
                current.append(character);
                escaped = true;
                continue;
            }
            if (character == '"' && !inChar) {
                inString = !inString;
                current.append(character);
                continue;
            }
            if (character == '\'' && !inString) {
                inChar = !inChar;
                current.append(character);
                continue;
            }
            if (!inString && !inChar) {
                if (character == '(' || character == '[' || character == '{') {
                    nesting++;
                } else if (character == ')' || character == ']' || character == '}') {
                    if (nesting > 0) nesting--;
                } else if (character == ',' && nesting == 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(character);
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }
}
