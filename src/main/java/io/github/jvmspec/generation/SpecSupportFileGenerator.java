package io.github.jvmspec.generation;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.MethodDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes or updates generated subject-specific specification support classes.
 */
public final class SpecSupportFileGenerator {
    private SpecSupportFileGenerator() {
    }

    public static File writeOrUpdate(SpecGenerationPlan plan) throws IOException {
        return writeOrUpdateResult(plan).file();
    }

    /**
     * Writes the support class as a full regeneration from the plan.
     *
     * <p>The support class is a derived artifact living under the generated-sources root: it is a
     * pure function of the discovered spec, never hand-edited. It is therefore rewritten from
     * scratch on every run (never merged with the previous content), so proxy methods that the
     * spec no longer references disappear instead of lingering as stale members. Prophecy helper
     * blocks are re-added afterwards in the same run by the prophecy pipeline.</p>
     *
     * @return the target file together with a flag telling whether anything was written
     */
    public static SupportWriteResult writeOrUpdateResult(SpecGenerationPlan plan) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");

        File targetFile = plan.targetFile();
        File parent = targetFile.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        String renderedContent = plan.sourceContent();
        if (!targetFile.exists()) {
            AtomicFileWriter.writeUtf8(targetFile, renderedContent);
            return new SupportWriteResult(targetFile, true);
        }

        String existingSource = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
        if (existingSource.equals(renderedContent)) {
            return new SupportWriteResult(targetFile, false);
        }
        AtomicFileWriter.writeUtf8(targetFile, renderedContent);
        return new SupportWriteResult(targetFile, true);
    }

    /**
     * Result of a support write: the target file and whether its content actually changed.
     */
    public static final class SupportWriteResult {
        private final File file;
        private final boolean changed;

        private SupportWriteResult(File file, boolean changed) {
            this.file = file;
            this.changed = changed;
        }

        public File file() {
            return file;
        }

        public boolean changed() {
            return changed;
        }
    }

    public static String updateSource(String source, DescribedType describedType) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");

        String supportName = SpecSkeletonGenerator.supportSimpleName(describedType.describedClass());
        int classOpen = findClassOpenBrace(source, supportName);
        int classClose = findPrimaryClassClosingBrace(source, supportName);
        if (classOpen < 0 || classClose < 0) {
            return source;
        }

        String updated = source;
        if (!hasConstructor(updated, supportName)) {
            updated = insertAfterClassOpen(updated, supportName, renderConstructor(supportName, describedType.simpleName()));
        }

        if (SpecSkeletonGenerator.hasInstanceSubjectMethods(describedType)) {
            updated = insertMissingProxyMethods(updated, describedType);
            updated = insertOrUpdateThrowProxy(updated, describedType);
        }
        return updated;
    }

    public static String updateSourceWithProphecyHelpers(
            String source,
            DescribedType describedType,
            List<String> prophesizedTypeNames
    ) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(prophesizedTypeNames, "prophesizedTypeNames must not be null");
        String updated = updateSource(source, describedType);
        for (int i = 0; i < prophesizedTypeNames.size(); i++) {
            String typeName = prophesizedTypeNames.get(i);
            if (typeName == null || typeName.trim().length() == 0) {
                continue;
            }
            updated = insertMissingProphecyHelper(updated, describedType, typeName.trim());
        }
        return updated;
    }

    private static String insertMissingProphecyHelper(String source, DescribedType describedType, String typeName) {
        String simpleName = simpleName(typeName);
        String helperName = "prophesize" + simpleName;
        if (hasProphecyHelper(source, helperName)) {
            return source;
        }
        // Return type is the typed wrapper (e.g. MailerProphecy) so:
        //   Java 8:   MailerProphecy m = prophesizeMailer();   m.send(...).willReturn(true);
        //   Java 10+: var m = prophesizeMailer();              m.send(...).willReturn(true);
        // The wrapper class lives only in target/generated-sources/javaspec, never in src/.
        String wrapperFqcn = typeName + "Prophecy";
        StringBuilder block = new StringBuilder();
        block.append("    /**\n");
        block.append("     * Returns a typed prophecy for {@code ").append(simpleName).append("}\n");
        block.append("     * backed by this spec's shared {@link io.github.jvmspec.doubles.prophecy.PredictionRegistry}.\n");
        block.append("     *\n");
        block.append("     * <p>Java 8: {@code ").append(wrapperFqcn).append(" m = ").append(helperName).append("();}\n");
        block.append("     * Java 10+: {@code var m = ").append(helperName).append("();}</p>\n");
        block.append("     */\n");
        block.append("    protected ").append(wrapperFqcn).append(" ").append(helperName).append("() {\n");
        block.append("        io.github.jvmspec.doubles.InterfaceDouble<").append(typeName).append("> handle =\n");
        block.append("            ").append(typeName).append(".class.isInterface()\n");
        block.append("                ? io.github.jvmspec.doubles.Doubles.interfaceDouble(").append(typeName).append(".class)\n");
        block.append("                : io.github.jvmspec.doubles.Doubles.concreteDouble(").append(typeName).append(".class);\n");
        block.append("        return new ").append(wrapperFqcn).append("(handle, prophecyRegistry());\n");
        block.append("    }\n\n");
        block.append("    /** Alias for {@link #").append(helperName).append("()}. */\n");
        block.append("    protected ").append(wrapperFqcn).append(" prophecy").append(simpleName).append("() {\n");
        block.append("        return ").append(helperName).append("();\n");
        block.append("    }\n");
        return insertBeforeSupportClassClose(source, describedType, block.toString());
    }

    private static boolean hasProphecyHelper(String source, String helperName) {
        // Match only actual method declarations, not Javadoc @link references
        Pattern pattern = Pattern.compile(
                "(?:protected|public)\\s+\\S+\\s+" + Pattern.quote(helperName) + "\\s*\\(");
        return pattern.matcher(source).find();
    }

    private static String simpleName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot < 0 ? typeName : typeName.substring(lastDot + 1);
    }

    private static String insertMissingProxyMethods(String source, DescribedType describedType) {
        List<MethodDescriptor> missing = new ArrayList<MethodDescriptor>();
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (!SpecSkeletonGenerator.isSupportSubjectMethod(method)) {
                continue;
            }
            if (!hasDeclaredMethod(source, method.methodName(), method.parameterTypes())) {
                missing.add(method);
            }
        }
        if (missing.isEmpty()) {
            return source;
        }

        StringBuilder block = new StringBuilder();
        for (int i = 0; i < missing.size(); i++) {
            SpecSkeletonGenerator.appendSupportProxyMethod(block, missing.get(i));
            if (i < missing.size() - 1) {
                block.append("\n");
            }
        }
        return insertBeforeSupportClassClose(source, describedType, block.toString());
    }

    private static String insertOrUpdateThrowProxy(String source, DescribedType describedType) {
        String throwType = describedType.simpleName() + "ThrowExpectation";
        if (!source.contains("class " + throwType)) {
            StringBuilder block = new StringBuilder();
            SpecSkeletonGenerator.appendThrowProxy(block, describedType);
            return insertBeforeSupportClassClose(source, describedType, block.toString());
        }

        String updated = source;
        if (!hasShouldThrowOverride(updated, throwType)) {
            StringBuilder override = new StringBuilder();
            override.append("    @Override\n");
            override.append("    public ").append(throwType).append(" shouldThrow(Class<? extends Throwable> expectedType) {\n");
            override.append("        return new ").append(throwType).append("(expectedType);\n");
            override.append("    }\n");
            updated = insertBeforeSupportClassClose(updated, describedType, override.toString());
        }

        int throwClose = findNestedClassClosingBrace(updated, throwType);
        if (throwClose < 0) {
            return updated;
        }
        StringBuilder missingDuring = new StringBuilder();
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (!SpecSkeletonGenerator.isSupportSubjectMethod(method)) {
                continue;
            }
            String duringName = "during" + SpecSkeletonGenerator.capitalize(method.methodName());
            if (!hasDeclaredMethod(updated, duringName, method.parameterTypes())) {
                if (missingDuring.length() > 0) {
                    missingDuring.append("\n");
                }
                SpecSkeletonGenerator.appendDuringMethod(missingDuring, method);
            }
        }
        if (missingDuring.length() == 0) {
            return updated;
        }
        return insertBlockAt(updated, throwClose, missingDuring.toString());
    }

    private static String insertAfterClassOpen(String source, String supportName, String block) {
        int open = findClassOpenBrace(source, supportName);
        if (open < 0) {
            return source;
        }
        return insertBlockAt(source, open + 1, block);
    }

    private static String insertBeforeSupportClassClose(String source, DescribedType describedType, String block) {
        String supportName = SpecSkeletonGenerator.supportSimpleName(describedType.describedClass());
        int close = findPrimaryClassClosingBrace(source, supportName);
        if (close < 0) {
            return source;
        }
        return insertBlockAt(source, close, block);
    }

    private static String insertBlockAt(String source, int position, String block) {
        String prefix = source.substring(0, position);
        String suffix = source.substring(position);
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        if (!prefix.endsWith("\n")) {
            builder.append("\n");
        }
        if (!endsWithBlankLine(builder)) {
            builder.append("\n");
        }
        builder.append(block);
        if (!block.endsWith("\n")) {
            builder.append("\n");
        }
        builder.append(suffix);
        return builder.toString();
    }

    private static String renderConstructor(String supportName, String subjectSimpleName) {
        StringBuilder builder = new StringBuilder();
        builder.append("    public ").append(supportName).append("() {\n");
        builder.append("        super(").append(subjectSimpleName).append(".class);\n");
        builder.append("    }\n");
        return builder.toString();
    }

    private static boolean hasConstructor(String source, String supportName) {
        Pattern pattern = Pattern.compile("\\b(?:public|protected|private)?\\s*" + Pattern.quote(supportName) + "\\s*\\(");
        return pattern.matcher(source).find();
    }

    private static boolean hasShouldThrowOverride(String source, String throwType) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(throwType) + "\\s+shouldThrow\\s*\\(");
        return pattern.matcher(source).find();
    }

    private static boolean hasDeclaredMethod(String source, String methodName, List<String> parameterTypes) {
        Pattern pattern = Pattern.compile(
                "(?m)(?:public|protected|private)\\s+" +
                        "(?:[A-Za-z_$][A-Za-z0-9_$.<>?\\[\\]]*|void)\\s+" +
                        Pattern.quote(methodName) + "\\s*\\(([^)]*)\\)"
        );
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            if (sameParameterTypes(parseParameterTypes(matcher.group(1)), parameterTypes)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> parseParameterTypes(String parameterSource) {
        List<String> types = new ArrayList<String>();
        String trimmed = parameterSource.trim();
        if (trimmed.length() == 0) {
            return types;
        }
        String[] parts = trimmed.split(",");
        for (int i = 0; i < parts.length; i++) {
            String parameter = parts[i].trim();
            if (parameter.startsWith("final ")) {
                parameter = parameter.substring("final ".length()).trim();
            }
            int lastSpace = parameter.lastIndexOf(' ');
            if (lastSpace >= 0) {
                types.add(parameter.substring(0, lastSpace).trim());
            }
        }
        return types;
    }

    private static boolean sameParameterTypes(List<String> left, List<String> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (!normalizeType(left.get(i)).equals(normalizeType(right.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeType(String typeName) {
        String normalized = typeName.trim().replace("...", "[]").replaceAll("\\s+", "");
        int arrayIndex = normalized.indexOf("[]");
        String suffix = "";
        if (arrayIndex >= 0) {
            suffix = normalized.substring(arrayIndex);
            normalized = normalized.substring(0, arrayIndex);
        }
        int genericIndex = normalized.indexOf('<');
        String generic = "";
        if (genericIndex >= 0) {
            generic = normalized.substring(genericIndex);
            normalized = normalized.substring(0, genericIndex);
        }
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0) {
            normalized = normalized.substring(lastDot + 1);
        }
        return normalized + generic + suffix;
    }

    private static int findClassOpenBrace(String source, String className) {
        Pattern pattern = Pattern.compile("\\bclass\\s+" + Pattern.quote(className) + "\\b[^\\{]*\\{", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return -1;
        }
        return matcher.end() - 1;
    }

    private static int findPrimaryClassClosingBrace(String source, String className) {
        int open = findClassOpenBrace(source, className);
        if (open < 0) {
            return -1;
        }
        return findMatchingBrace(source, open);
    }

    private static int findNestedClassClosingBrace(String source, String className) {
        int open = findClassOpenBrace(source, className);
        if (open < 0) {
            return -1;
        }
        return findMatchingBrace(source, open);
    }

    private static int findMatchingBrace(String source, int openBrace) {
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;
        for (int i = openBrace; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (escaped) {
                escaped = false;
                continue;
            }
            if ((inString || inChar) && c == '\\') {
                escaped = true;
                continue;
            }
            if (!inString && !inChar && c == '/' && next == '/') {
                inLineComment = true;
                i++;
                continue;
            }
            if (!inString && !inChar && c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (!inChar && c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString && c == '\'') {
                inChar = !inChar;
                continue;
            }
            if (inString || inChar) {
                continue;
            }
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

    private static boolean endsWithBlankLine(StringBuilder builder) {
        int length = builder.length();
        if (length < 2 || builder.charAt(length - 1) != '\n') {
            return false;
        }
        int index = length - 2;
        while (index >= 0 && (builder.charAt(index) == ' ' || builder.charAt(index) == '\t' || builder.charAt(index) == '\r')) {
            index--;
        }
        return index >= 0 && builder.charAt(index) == '\n';
    }
}
