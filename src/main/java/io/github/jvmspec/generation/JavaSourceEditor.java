package io.github.jvmspec.generation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offset-preserving Java source structure, indentation, and insertion operations. */
final class JavaSourceEditor {
    private JavaSourceEditor() {
    }

    static String insertBeforeClosingBraceKeepingIndent(String source, int closingBrace, String insertion) {
        int cutPosition = insertionCutPosition(source, closingBrace);
        String closingIndent = cutPosition == closingBrace ? indentationBefore(source, closingBrace) : "";
        return insertBeforeClosingBrace(source, cutPosition, insertion, closingIndent);
    }

    static int insertionCutPosition(String source, int closingBrace) {
        int index = closingBrace - 1;
        while (index >= 0 && (source.charAt(index) == ' ' || source.charAt(index) == '\t')) {
            index--;
        }
        if (index >= 0 && source.charAt(index) == '\n') {
            return index + 1;
        }
        return closingBrace;
    }

    static String insertBeforeClosingBrace(String source, int cutPosition, String insertion) {
        return insertBeforeClosingBrace(source, cutPosition, insertion, "");
    }

    static String insertBeforeClosingBrace(String source, int cutPosition, String insertion, String closingIndent) {
        String prefix = source.substring(0, cutPosition);
        String suffix = source.substring(cutPosition);

        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        String newline = lineSeparatorOf(source);
        String normalizedInsertion = normalizeLineSeparators(insertion, newline);
        if (!endsWithLineBreak(prefix)) {
            builder.append(newline);
        }
        if (!endsWithBlankLine(builder)) {
            builder.append(newline);
        }
        builder.append(normalizedInsertion);
        if (!endsWithLineBreak(normalizedInsertion)) {
            builder.append(newline);
        }
        builder.append(closingIndent);
        builder.append(suffix);
        return builder.toString();
    }

    static int findPrimaryTypeClosingBrace(String source, String simpleName) {
        int openBrace = findPrimaryTypeOpenBrace(source, simpleName);
        if (openBrace < 0) {
            return -1;
        }
        return findMatchingBrace(source, openBrace);
    }

    static String ensureEnumConstantsTerminated(String source, String simpleName) {
        int openBrace = findPrimaryTypeOpenBrace(source, simpleName);
        if (openBrace < 0) {
            return source;
        }
        String masked = maskNonCode(source);
        int depth = 0;
        int lastConstantChar = -1;
        for (int i = openBrace; i < masked.length(); i++) {
            char c = masked.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    break;
                }
            } else if (c == ';' && depth == 1) {
                return source; // constants already terminated
            } else if (depth == 1 && !Character.isWhitespace(c)) {
                lastConstantChar = i;
            }
        }
        if (lastConstantChar < 0) {
            // Empty enum body: insert a ';' just after the opening brace.
            return source.substring(0, openBrace + 1) + lineSeparatorOf(source) + "    ;"
                    + source.substring(openBrace + 1);
        }
        return source.substring(0, lastConstantChar + 1) + ";" + source.substring(lastConstantChar + 1);
    }

    static int findPrimaryTypeOpenBrace(String source, String simpleName) {
        Matcher matcher = primaryTypePattern(simpleName).matcher(source);
        if (!matcher.find()) {
            return -1;
        }
        return matcher.end() - 1;
    }

    static Pattern primaryTypePattern(String simpleName) {
        return Pattern.compile("\\b(?:class|interface|enum|record)\\s+" + Pattern.quote(simpleName) + "\\b[^\\{]*\\{", Pattern.DOTALL);
    }

    static int findMatchingBrace(String source, int openBrace) {
        return findMatchingBraceInMasked(maskNonCode(source), openBrace);
    }

    static int findMatchingBraceInMasked(String masked, int openBrace) {
        int depth = 0;
        for (int i = openBrace; i < masked.length(); i++) {
            char c = masked.charAt(i);
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

    static String maskNonCode(String source) {
        return NonCodeSourceMasker.mask(source);
    }

    static char blankedChar(char c) {
        return c == '\n' || c == '\r' ? c : ' ';
    }

    static String memberIndentationBefore(String source, int closingBrace) {
        String closingIndent = indentationBefore(source, closingBrace);
        int bodyOpen = matchingOpenBrace(source, closingBrace);
        int lowerBound = bodyOpen < 0 ? 0 : bodyOpen + 1;
        int lineEnd = closingBrace;
        while (lineEnd > lowerBound) {
            int previousBreak = previousLineBreak(source, lineEnd - 1);
            int lineStart = Math.max(previousBreak + 1, lowerBound);
            int content = lineStart;
            while (content < lineEnd && (source.charAt(content) == ' ' || source.charAt(content) == '\t')) {
                content++;
            }
            if (content < lineEnd && source.charAt(content) != '\r' && source.charAt(content) != '\n') {
                String candidate = source.substring(lineStart, content);
                if (candidate.length() > closingIndent.length()) {
                    return candidate;
                }
            }
            if (previousBreak < lowerBound) {
                break;
            }
            lineEnd = previousBreak;
            if (lineEnd > 0 && source.charAt(lineEnd - 1) == '\r') {
                lineEnd--;
            }
        }
        return closingIndent + "    ";
    }

    static int matchingOpenBrace(String source, int closingBrace) {
        String masked = maskNonCode(source);
        int depth = 0;
        for (int i = closingBrace; i >= 0; i--) {
            char current = masked.charAt(i);
            if (current == '}') {
                depth++;
            } else if (current == '{') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    static int previousLineBreak(String source, int from) {
        for (int i = from; i >= 0; i--) {
            char current = source.charAt(i);
            if (current == '\n' || current == '\r') {
                return i;
            }
        }
        return -1;
    }

    static String lineSeparatorOf(String source) {
        int newline = source.indexOf('\n');
        if (newline > 0 && source.charAt(newline - 1) == '\r') {
            return "\r\n";
        }
        if (newline >= 0) {
            return "\n";
        }
        return source.indexOf('\r') >= 0 ? "\r" : System.lineSeparator();
    }

    static String normalizeLineSeparators(String value, String newline) {
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        return "\n".equals(newline) ? normalized : normalized.replace("\n", newline);
    }

    static boolean endsWithLineBreak(String value) {
        return value.endsWith("\n") || value.endsWith("\r");
    }

    static String indentationBefore(String source, int position) {
        int index = position - 1;
        while (index >= 0 && source.charAt(index) != '\n' && source.charAt(index) != '\r') {
            index--;
        }
        int lineStart = index + 1;
        StringBuilder indentation = new StringBuilder();
        while (lineStart < position) {
            char c = source.charAt(lineStart);
            if (c == ' ' || c == '\t') {
                indentation.append(c);
                lineStart++;
                continue;
            }
            break;
        }
        return indentation.toString();
    }

    static boolean endsWithBlankLine(StringBuilder builder) {
        int length = builder.length();
        if (length < 2) {
            return false;
        }
        int last = length - 1;
        if (builder.charAt(last) != '\n') {
            return false;
        }
        int previous = last - 1;
        while (previous >= 0 && (builder.charAt(previous) == ' ' || builder.charAt(previous) == '\t' || builder.charAt(previous) == '\r')) {
            previous--;
        }
        return previous >= 0 && builder.charAt(previous) == '\n';
    }
}
