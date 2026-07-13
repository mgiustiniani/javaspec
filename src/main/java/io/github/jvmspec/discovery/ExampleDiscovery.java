package io.github.jvmspec.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Package-private Java example-method discovery used by the public discovery facade. */
final class ExampleDiscovery {
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "public\\s+void\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)\\s*"
                    + "(?:throws\\s+[^\\{]+)?\\{",
            Pattern.DOTALL
    );

    private ExampleDiscovery() {
    }

    static List<SpecExample> discover(String source) {
        List<SpecExample> examples = new ArrayList<SpecExample>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(source);
        int orderIndex = 0;
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            if (SpecExample.isExampleMethodName(methodName)) {
                examples.add(SpecExample.of(
                        methodName,
                        orderIndex,
                        lineNumberAt(source, methodMatcher.start())
                ));
                orderIndex++;
            }
        }
        return examples;
    }

    private static int lineNumberAt(String source, int position) {
        int safePosition = Math.max(0, Math.min(position, source.length()));
        int lineNumber = 1;
        for (int i = 0; i < safePosition; i++) {
            char character = source.charAt(i);
            if (character == '\n') {
                lineNumber++;
            } else if (character == '\r') {
                lineNumber++;
                if (i + 1 < safePosition && source.charAt(i + 1) == '\n') {
                    i++;
                }
            }
        }
        return lineNumber;
    }
}
