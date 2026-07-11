package io.github.jvmspec.generation;

/**
 * Internal offset-preserving masker for comments and Java literals used by source updaters.
 */
final class NonCodeSourceMasker {
    private NonCodeSourceMasker() {
    }

    static String mask(String source) {
        if (source == null || source.length() == 0) {
            return source;
        }
        char[] input = source.toCharArray();
        char[] output = input.clone();
        int index = 0;
        while (index < input.length) {
            char current = input[index];
            if (current == '/' && matches(input, index, "//")) {
                index = blankLineComment(input, output, index);
            } else if (current == '/' && matches(input, index, "/*")) {
                index = blankBlockComment(input, output, index);
            } else if (current == '"' && matches(input, index, "\"\"\"")) {
                index = blankTextBlock(input, output, index);
            } else if (current == '"') {
                index = blankQuotedLiteral(input, output, index, '"');
            } else if (current == '\'') {
                index = blankQuotedLiteral(input, output, index, '\'');
            } else {
                index++;
            }
        }
        return new String(output);
    }

    private static int blankLineComment(char[] input, char[] output, int start) {
        int index = start;
        while (index < input.length && input[index] != '\n' && input[index] != '\r') {
            output[index] = ' ';
            index++;
        }
        return index;
    }

    private static int blankBlockComment(char[] input, char[] output, int start) {
        int index = start;
        while (index < input.length) {
            if (matches(input, index, "*/")) {
                blank(output, index, 2);
                return index + 2;
            }
            output[index] = blanked(input[index]);
            index++;
        }
        return index;
    }

    private static int blankTextBlock(char[] input, char[] output, int start) {
        blank(output, start, 3);
        int index = start + 3;
        while (index < input.length) {
            if (input[index] == '\\') {
                output[index] = ' ';
                index++;
                if (index < input.length) {
                    output[index] = blanked(input[index]);
                    index++;
                }
                continue;
            }
            if (matches(input, index, "\"\"\"")) {
                blank(output, index, 3);
                return index + 3;
            }
            output[index] = blanked(input[index]);
            index++;
        }
        return index;
    }

    private static int blankQuotedLiteral(char[] input, char[] output, int start, char delimiter) {
        output[start] = ' ';
        int index = start + 1;
        while (index < input.length) {
            char current = input[index];
            output[index] = blanked(current);
            if (current == '\\') {
                index++;
                if (index < input.length) {
                    output[index] = blanked(input[index]);
                }
            } else if (current == delimiter) {
                return index + 1;
            }
            index++;
        }
        return index;
    }

    private static boolean matches(char[] input, int offset, String token) {
        if (offset + token.length() > input.length) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (input[offset + i] != token.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static void blank(char[] output, int offset, int length) {
        for (int i = 0; i < length && offset + i < output.length; i++) {
            output[offset + i] = ' ';
        }
    }

    private static char blanked(char value) {
        return value == '\n' || value == '\r' ? value : ' ';
    }
}
