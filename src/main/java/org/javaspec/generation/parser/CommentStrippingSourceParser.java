package org.javaspec.generation.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in {@link JavaSourceParser} that strips line comments, block comments,
 * and string/character literals before applying structural regex queries.
 *
 * <p>This removes the main sources of false positives and false negatives in
 * the heuristic-based {@code ClassMethodUpdater}: a method name in a comment
 * is no longer confused for an existing declaration, and a brace inside a
 * string literal no longer throws off the brace-counting algorithm.</p>
 *
 * <p>The pre-processed text replaces each removed token with the same number
 * of spaces (plus preserved newlines) so that character offsets in the
 * original source remain valid — insertions are applied to the <em>original</em>
 * source, not the stripped copy.</p>
 */
public final class CommentStrippingSourceParser implements JavaSourceParser {

    @Override
    public String name() {
        return "comment-stripping";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ParsedSource parse(String source) {
        String stripped = stripCommentsAndLiterals(source);
        return new StrippedParsedSource(source, stripped);
    }

    // -------------------------------------------------------------------------
    // Comment / literal stripping

    /**
     * Returns a copy of {@code source} where:
     * <ul>
     *   <li>Line comments ({@code //…\n}) are replaced by spaces up to the newline.</li>
     *   <li>Block comments ({@code /*…{@literal *}/}) are replaced by spaces, newlines preserved.</li>
     *   <li>String literals ({@code "…"}) are replaced by {@code ""} plus spaces for the content.</li>
     *   <li>Character literals ({@code '…'}) are replaced by {@code ''} plus spaces.</li>
     * </ul>
     */
    static String stripCommentsAndLiterals(String source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        char[] in = source.toCharArray();
        char[] out = in.clone();
        int i = 0;
        while (i < in.length) {
            char c = in[i];
            if (c == '/' && i + 1 < in.length) {
                if (in[i + 1] == '/') {
                    // Line comment: blank until \n
                    out[i] = ' ';
                    out[i + 1] = ' ';
                    i += 2;
                    while (i < in.length && in[i] != '\n') {
                        out[i] = ' ';
                        i++;
                    }
                    continue;
                } else if (in[i + 1] == '*') {
                    // Block comment: blank, preserve newlines
                    out[i] = ' ';
                    out[i + 1] = ' ';
                    i += 2;
                    while (i < in.length) {
                        if (in[i] == '*' && i + 1 < in.length && in[i + 1] == '/') {
                            out[i] = ' ';
                            out[i + 1] = ' ';
                            i += 2;
                            break;
                        }
                        if (in[i] != '\n') {
                            out[i] = ' ';
                        }
                        i++;
                    }
                    continue;
                }
            }
            if (c == '"') {
                // String literal
                i = blankStringLiteral(in, out, i);
                continue;
            }
            if (c == '\'') {
                // Character literal
                i = blankCharLiteral(in, out, i);
                continue;
            }
            i++;
        }
        return new String(out);
    }

    private static int blankStringLiteral(char[] in, char[] out, int start) {
        // Keep the opening quote; blank all content up to the closing quote.
        int i = start + 1;
        // Check for text block (triple-quote) — Java 13+
        if (i + 1 < in.length && in[i] == '"' && in[i + 1] == '"') {
            // Text block: scan for closing """
            out[i] = ' ';
            out[i + 1] = ' ';
            i += 2;
            while (i < in.length) {
                if (in[i] == '"' && i + 2 < in.length && in[i + 1] == '"' && in[i + 2] == '"') {
                    i += 3;
                    break;
                }
                if (in[i] != '\n') {
                    out[i] = ' ';
                }
                i++;
            }
            return i;
        }
        // Ordinary string literal
        while (i < in.length) {
            char c = in[i];
            if (c == '\\') {
                // Escape sequence: blank both characters
                if (in[i] != '\n') {
                    out[i] = ' ';
                }
                i++;
                if (i < in.length && in[i] != '\n') {
                    out[i] = ' ';
                }
                i++;
                continue;
            }
            if (c == '"') {
                // Closing quote
                i++;
                break;
            }
            if (c != '\n') {
                out[i] = ' ';
            }
            i++;
        }
        return i;
    }

    private static int blankCharLiteral(char[] in, char[] out, int start) {
        int i = start + 1;
        while (i < in.length) {
            char c = in[i];
            if (c == '\\') {
                if (in[i] != '\n') {
                    out[i] = ' ';
                }
                i++;
                if (i < in.length && in[i] != '\n') {
                    out[i] = ' ';
                }
                i++;
                continue;
            }
            if (c == '\'') {
                i++;
                break;
            }
            if (c != '\n') {
                out[i] = ' ';
            }
            i++;
        }
        return i;
    }

    // -------------------------------------------------------------------------
    // ParsedSource implementation

    private static final class StrippedParsedSource implements ParsedSource {

        private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile(
                "(?m)(?:^|[\\s;{}])" +
                "(?:(?:public|protected|private)\\s+)?" +
                "(?:(?:static|final|synchronized|abstract|native|strictfp|default)\\s+)*" +
                "(?:<[^>]*>\\s+)?" +                       // optional type-parameter prefix
                "([A-Za-z_$][A-Za-z0-9_$.<>?\\[\\], ]*?)\\s+" +  // return type
                "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)"    // name(params)
        );

        private final String original;
        private final String stripped;

        StrippedParsedSource(String original, String stripped) {
            this.original = original;
            this.stripped = stripped;
        }

        @Override
        public boolean hasMethod(String methodName, List<String> paramTypes) {
            Matcher m = METHOD_SIGNATURE_PATTERN.matcher(stripped);
            while (m.find()) {
                if (methodName.equals(m.group(2).trim())) {
                    if (paramTypesMatch(m.group(3), paramTypes)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean paramTypesMatch(String paramsText, List<String> expected) {
            if (paramsText == null) {
                return expected.isEmpty();
            }
            String trimmed = paramsText.trim();
            if (trimmed.isEmpty()) {
                return expected.isEmpty();
            }
            // Split on commas at depth 0 (handle generics like Map<K,V>)
            List<String> actual = splitParams(trimmed);
            if (actual.size() != expected.size()) {
                return false;
            }
            for (int i = 0; i < actual.size(); i++) {
                // Extract the type token (last word is the parameter name)
                String actualType = paramTypeToken(actual.get(i));
                String expectedType = expected.get(i).trim();
                // Simple match: compare simple names (last segment after .)
                if (!simpleNameOf(actualType).equals(simpleNameOf(expectedType))) {
                    return false;
                }
            }
            return true;
        }

        private static List<String> splitParams(String paramsText) {
            List<String> result = new java.util.ArrayList<String>();
            int depth = 0;
            int start = 0;
            for (int i = 0; i < paramsText.length(); i++) {
                char c = paramsText.charAt(i);
                if (c == '<' || c == '(') {
                    depth++;
                } else if (c == '>' || c == ')') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    result.add(paramsText.substring(start, i).trim());
                    start = i + 1;
                }
            }
            result.add(paramsText.substring(start).trim());
            return result;
        }

        private static String paramTypeToken(String param) {
            // "final int[] count" → "int[]"
            // "String name" → "String"
            // "List<String> items" → "List<String>"
            String t = param.trim();
            // Remove annotation prefixes
            while (t.startsWith("@") || t.startsWith("final ")) {
                if (t.startsWith("final ")) {
                    t = t.substring("final ".length()).trim();
                } else {
                    // skip annotation
                    int space = t.indexOf(' ');
                    if (space < 0) {
                        return t;
                    }
                    t = t.substring(space + 1).trim();
                }
            }
            // Last word is the parameter name; everything before is the type
            // Exception: if only one token, the whole thing is the type (no param name? rare)
            int lastSpace = lastSpaceOutsideAngles(t);
            if (lastSpace < 0) {
                return t;
            }
            return t.substring(0, lastSpace).trim();
        }

        private static int lastSpaceOutsideAngles(String s) {
            int depth = 0;
            int lastSpace = -1;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '<') {
                    depth++;
                } else if (c == '>') {
                    depth--;
                } else if ((c == ' ' || c == '\t') && depth == 0) {
                    lastSpace = i;
                }
            }
            return lastSpace;
        }

        private static String simpleNameOf(String typeName) {
            if (typeName == null || typeName.isEmpty()) {
                return typeName;
            }
            // Strip array brackets for comparison purposes
            String base = typeName.replace("[]", "").trim();
            int lastDot = base.lastIndexOf('.');
            return lastDot < 0 ? base : base.substring(lastDot + 1);
        }

        @Override
        public int typeClosingBraceOffset(String typeName) {
            return findClosingBrace(stripped, typeName, false, null);
        }

        @Override
        public int nestedTypeClosingBraceOffset(String outerTypeName, String nestedTypeName) {
            return findClosingBrace(stripped, outerTypeName, true, nestedTypeName);
        }

        private static int findClosingBrace(
                String stripped, String typeName, boolean findNested, String nestedName) {
            // Find the primary type declaration
            Pattern typePattern = Pattern.compile(
                    "\\b(?:class|interface|enum|record|@interface)\\s+" +
                    Pattern.quote(typeName) + "\\b");
            Matcher tm = typePattern.matcher(stripped);
            if (!tm.find()) {
                return -1;
            }
            // Find the opening brace of the primary type body
            int bodyStart = stripped.indexOf('{', tm.end());
            if (bodyStart < 0) {
                return -1;
            }

            if (!findNested) {
                return findMatchingBrace(stripped, bodyStart);
            }

            // Find the nested type inside the primary body
            int primaryClose = findMatchingBrace(stripped, bodyStart);
            if (primaryClose < 0) {
                return -1;
            }
            String primaryBody = stripped.substring(bodyStart + 1, primaryClose);
            Pattern nestedPattern = Pattern.compile(
                    "\\b(?:class|interface|enum|record)\\s+" +
                    Pattern.quote(nestedName) + "\\b");
            Matcher nm = nestedPattern.matcher(primaryBody);
            if (!nm.find()) {
                return -1;
            }
            int nestedBodyStart = primaryBody.indexOf('{', nm.end());
            if (nestedBodyStart < 0) {
                return -1;
            }
            // Offset relative to full source
            int absoluteStart = bodyStart + 1 + nestedBodyStart;
            return findMatchingBrace(stripped, absoluteStart);
        }

        /**
         * Returns the index of the {@code '}'} that matches the {@code '{'}
         * at {@code openBraceIdx} in the given source, using brace counting
         * (immune to string/comment content because {@code stripped} has those removed).
         */
        private static int findMatchingBrace(String source, int openBraceIdx) {
            int depth = 0;
            for (int i = openBraceIdx; i < source.length(); i++) {
                char c = source.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
}
