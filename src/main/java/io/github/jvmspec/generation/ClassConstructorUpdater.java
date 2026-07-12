package io.github.jvmspec.generation;

import io.github.jvmspec.internal.type.ConstructorSignature;
import io.github.jvmspec.internal.type.JavaSyntaxSplitter;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates an existing class source file to match constructor declarations from the spec.
 * <p>
 * Rules:
 * <ul>
 *   <li>Constructors present in the spec are always brought to the class with their content.</li>
 *   <li>Constructors in the class but NOT in the spec:
 *       <ul>
 *         <li>If empty (no body content): always deleted regardless of policy.</li>
 *         <li>If they have content: policy determines action (PRESERVE, DELETE, COMMENT).</li>
 *       </ul>
 *   </li>
 *   <li>If a spec constructor needs to be created and an existing constructor (not in spec) shares
 *       all its parameters except for the added ones in the new spec constructor, the existing
 *       constructor's signature is extended by adding the new parameters, and its body content
 *       is preserved. This avoids duplicating code.</li>
 * </ul>
 * <p>
 * This updater performs surgical edits scoped strictly to constructor source ranges. Fields,
 * methods, comments, and nested types are never rewritten, reformatted, or discarded: only the
 * exact text spans of existing constructors identified by {@link #parseConstructors} are ever
 * replaced, commented, or removed, and brand-new constructors are inserted immediately after the
 * class's opening brace without touching anything already present.
 */
public final class ClassConstructorUpdater {
    private static final Pattern CONSTRUCTOR_HEADER_PATTERN = Pattern.compile(
            "\\b(public|protected|private)\\s+" +
            "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(",
            Pattern.DOTALL
    );
    private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile(
            "([A-Za-z_$][A-Za-z0-9_$]*)\\s*((?:\\[\\s*\\]\\s*)*)$");

    private ClassConstructorUpdater() {
    }

    public static String updateSource(
            String existingSource,
            DescribedType describedType,
            ConstructorPolicy policy
    ) {
        Objects.requireNonNull(existingSource, "existingSource must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        if (JavaTypeKind.RECORD.equals(describedType.kind())) {
            return RecordComponentPlanner.updateRecordHeader(existingSource, describedType);
        }

        List<ParsedConstructor> existingConstructors = parseConstructors(existingSource, describedType.simpleName());
        List<ConstructorDescriptor> specConstructors = describedType.constructors();

        return applyConstructorChanges(existingSource, existingConstructors, specConstructors, describedType, policy);
    }

    public static String updateFile(
            File classFile,
            DescribedType describedType,
            ConstructorPolicy policy
    ) throws IOException {
        Objects.requireNonNull(classFile, "classFile must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        String existingSource = new String(Files.readAllBytes(classFile.toPath()), StandardCharsets.UTF_8);
        String updatedSource = updateSource(existingSource, describedType, policy);
        if (!existingSource.equals(updatedSource)) {
            AtomicFileWriter.writeUtf8(classFile, updatedSource);
        }
        return updatedSource;
    }

    /**
     * Applies constructor updates to the file only when the resulting source differs from the
     * existing one, and reports truthfully whether anything was written.
     *
     * @return {@code true} when the file content changed, {@code false} when it was already
     *         up to date and was left untouched
     */
    public static boolean updateFileIfChanged(
            File classFile,
            DescribedType describedType,
            ConstructorPolicy policy
    ) throws IOException {
        Objects.requireNonNull(classFile, "classFile must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        String existingSource = new String(Files.readAllBytes(classFile.toPath()), StandardCharsets.UTF_8);
        String updatedSource = updateSource(existingSource, describedType, policy);
        if (existingSource.equals(updatedSource)) {
            return false;
        }
        AtomicFileWriter.writeUtf8(classFile, updatedSource);
        return true;
    }

    private static List<ParsedConstructor> parseConstructors(String source, String className) {
        List<ParsedConstructor> constructors = new ArrayList<ParsedConstructor>();
        String masked = NonCodeSourceMasker.mask(source);
        Matcher matcher = CONSTRUCTOR_HEADER_PATTERN.matcher(masked);
        while (matcher.find()) {
            if (!className.equals(matcher.group(2))) continue;
            int openParen = matcher.end() - 1;
            int closeParen = findMatching(masked, openParen, '(', ')');
            if (closeParen < 0) continue;
            int bodyOpen = findConstructorBodyOpen(masked, closeParen + 1);
            if (bodyOpen < 0) continue;
            int bodyClose = findMatching(masked, bodyOpen, '{', '}');
            if (bodyClose < 0) continue;

            List<String> paramTypes = new ArrayList<String>();
            List<String> paramNames = new ArrayList<String>();
            String paramsGroup = source.substring(openParen + 1, closeParen);
            List<String> params = JavaSyntaxSplitter.splitTopLevel(paramsGroup, ',');
            for (int i = 0; i < params.size(); i++) {
                String parameter = params.get(i).trim();
                if (parameter.length() == 0) continue;
                Matcher parameterMatcher = PARAMETER_NAME_PATTERN.matcher(parameter);
                if (!parameterMatcher.find()) continue;
                String name = parameterMatcher.group(1);
                String trailingArrays = parameterMatcher.group(2).replaceAll("\\s+", "");
                String type = parameter.substring(0, parameterMatcher.start(1)).trim()
                        + trailingArrays;
                paramTypes.add(type.trim());
                paramNames.add(name);
            }

            String body = source.substring(bodyOpen + 1, bodyClose).trim();
            constructors.add(new ParsedConstructor(
                    matcher.group(1), paramTypes, paramNames, body, body.length() == 0,
                    matcher.start(), bodyClose + 1));
        }
        return constructors;
    }

    private static int findConstructorBodyOpen(String masked, int start) {
        for (int i = start; i < masked.length(); i++) {
            char c = masked.charAt(i);
            if (c == '{') return i;
            if (c == ';') return -1;
        }
        return -1;
    }

    private static int findMatching(String source, int open, char openChar, char closeChar) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == openChar) depth++;
            else if (c == closeChar && --depth == 0) return i;
        }
        return -1;
    }

    private static String applyConstructorChanges(
            String source,
            List<ParsedConstructor> existingConstructors,
            List<ConstructorDescriptor> specConstructors,
            DescribedType describedType,
            ConstructorPolicy policy
    ) {
        // Categorize existing constructors
        List<ParsedConstructor> specMatched = new ArrayList<ParsedConstructor>();
        List<ParsedConstructor> unmatched = new ArrayList<ParsedConstructor>();

        for (int ei = 0; ei < existingConstructors.size(); ei++) {
            ParsedConstructor existing = existingConstructors.get(ei);
            boolean matched = false;
            for (int si = 0; si < specConstructors.size(); si++) {
                if (constructorsMatch(existing, specConstructors.get(si))) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                specMatched.add(existing);
            } else {
                unmatched.add(existing);
            }
        }

        // Handle unmatched constructors: empty ones always deleted, non-empty ones by policy
        List<ParsedConstructor> toExtend = new ArrayList<ParsedConstructor>();
        List<ParsedConstructor> toKeep = new ArrayList<ParsedConstructor>();
        List<ParsedConstructor> toDelete = new ArrayList<ParsedConstructor>();
        List<ParsedConstructor> toComment = new ArrayList<ParsedConstructor>();

        for (int ui = 0; ui < unmatched.size(); ui++) {
            ParsedConstructor existing = unmatched.get(ui);
            if (existing.isEmpty) {
                // Empty constructors not in spec are always deleted
                toDelete.add(existing);
            } else {
                // Check if this constructor can be extended for a spec constructor
                boolean extended = false;
                for (int si = 0; si < specConstructors.size(); si++) {
                    ConstructorDescriptor spec = specConstructors.get(si);
                    if (canExtend(existing, spec)) {
                        toExtend.add(existing);
                        extended = true;
                        break;
                    }
                }
                if (!extended) {
                    // Apply policy
                    if (ConstructorPolicy.PRESERVE.equals(policy)) {
                        toKeep.add(existing);
                    } else if (ConstructorPolicy.COMMENT.equals(policy)) {
                        toComment.add(existing);
                    } else {
                        // DELETE policy
                        toDelete.add(existing);
                    }
                }
            }
        }

        // Find the class declaration's opening brace. Only constructor source ranges are ever
        // rewritten below; fields, methods, comments, and nested types are never touched.
        String classDeclPatternStr = "(class\\s+" + Pattern.quote(describedType.simpleName()) +
                "(?:\\s+extends\\s+[^{]+)?(?:\\s+implements\\s+[^{]+)?(?:\\s*permits\\s+[^{]+)?)\\s*\\{";
        Pattern classDeclPattern = Pattern.compile(classDeclPatternStr, Pattern.DOTALL);
        Matcher classMatcher = classDeclPattern.matcher(source);

        if (!classMatcher.find()) {
            return source;
        }

        int classDeclEnd = classMatcher.end();

        // Determine, for each spec constructor, whether it is already satisfied by an existing
        // constructor (exact match) or by an existing constructor being extended. Any spec
        // constructor left unsatisfied needs to be newly inserted.
        List<ConstructorDescriptor> unsatisfiedSpecs = new ArrayList<ConstructorDescriptor>();
        for (int si = 0; si < specConstructors.size(); si++) {
            ConstructorDescriptor spec = specConstructors.get(si);
            boolean satisfied = false;
            for (int ei = 0; ei < specMatched.size(); ei++) {
                if (constructorsMatch(specMatched.get(ei), spec)) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                for (int ei = 0; ei < toExtend.size(); ei++) {
                    if (canExtend(toExtend.get(ei), spec)) {
                        satisfied = true;
                        break;
                    }
                }
            }
            if (!satisfied) {
                unsatisfiedSpecs.add(spec);
            }
        }

        // Collect surgical edits: each edit rewrites only the exact source range of one existing
        // constructor (including its own leading blank/indentation, as captured by the parser).
        // specMatched (exact-match) and toKeep constructors get no edit at all and are therefore
        // left completely byte-identical, along with every field, method, and comment in the class.
        List<Edit> edits = new ArrayList<Edit>();

        for (int ei = 0; ei < toExtend.size(); ei++) {
            ParsedConstructor existing = toExtend.get(ei);
            ConstructorDescriptor matchedSpec = null;
            for (int si = 0; si < specConstructors.size(); si++) {
                if (canExtend(existing, specConstructors.get(si))) {
                    matchedSpec = specConstructors.get(si);
                    break;
                }
            }
            if (matchedSpec == null) {
                continue;
            }
            String leading = leadingWhitespace(source, existing);
            edits.add(new Edit(existing.start, existing.end, leading + renderExtendedConstructor(existing, matchedSpec, describedType)));
        }

        for (int ci = 0; ci < toComment.size(); ci++) {
            ParsedConstructor existing = toComment.get(ci);
            String leading = leadingWhitespace(source, existing);
            edits.add(new Edit(existing.start, existing.end, leading + renderCommentedConstructor(existing, describedType)));
        }

        for (int di = 0; di < toDelete.size(); di++) {
            ParsedConstructor existing = toDelete.get(di);
            edits.add(new Edit(existing.start, existing.end, ""));
        }

        // Apply edits from last to first so earlier offsets remain valid throughout.
        Collections.sort(edits, new Comparator<Edit>() {
            public int compare(Edit a, Edit b) {
                return Integer.compare(b.start, a.start);
            }
        });

        String result = source;
        for (int i = 0; i < edits.size(); i++) {
            result = applyEdit(result, edits.get(i));
        }

        // Insert brand-new constructors (spec constructors satisfied by neither an exact match
        // nor an extension) right after the class's opening brace, before any existing member.
        // classDeclEnd remains valid because every recorded edit starts strictly after it.
        if (!unsatisfiedSpecs.isEmpty()) {
            StringBuilder insertion = new StringBuilder();
            insertion.append("\n");
            for (int si = 0; si < unsatisfiedSpecs.size(); si++) {
                ConstructorDescriptor spec = unsatisfiedSpecs.get(si);
                insertion.append("    public ").append(describedType.simpleName()).append("(");
                appendParameters(insertion, spec.parameterTypes(), spec.parameterNames());
                insertion.append(") {");
                String body = spec.hasBody() ? spec.bodyContent() : "";
                if (body.length() > 0) {
                    insertion.append("\n");
                    String[] bodyLines = body.split("\n");
                    for (int li = 0; li < bodyLines.length; li++) {
                        insertion.append("        ").append(bodyLines[li]).append("\n");
                    }
                    insertion.append("    }\n");
                } else {
                    insertion.append("\n    }\n");
                }
            }
            result = result.substring(0, classDeclEnd) + insertion.toString() + result.substring(classDeclEnd);
        }

        return result;
    }

    /**
     * Returns the whitespace the constructor-matching regex captured immediately before the
     * access modifier (typically a newline plus indentation), so replacements can keep the
     * original blank-line/indentation formatting instead of collapsing it.
     */
    private static String leadingWhitespace(String source, ParsedConstructor existing) {
        int index = existing.start;
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return source.substring(existing.start, index);
    }

    private static String renderExtendedConstructor(ParsedConstructor existing, ConstructorDescriptor spec, DescribedType describedType) {
        List<String> newParams = new ArrayList<String>();
        List<String> newNames = new ArrayList<String>();
        for (int pi = existing.paramTypes.size(); pi < spec.parameterTypes().size(); pi++) {
            newParams.add(spec.parameterTypes().get(pi));
            newNames.add(spec.parameterNames().get(pi));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("public ").append(describedType.simpleName()).append("(");
        appendParameters(builder, existing.paramTypes, existing.paramNames);
        if (newParams.size() > 0) {
            builder.append(", ");
            appendParameters(builder, newParams, newNames);
        }
        builder.append(") {\n");
        if (existing.body.length() > 0) {
            String[] bodyLines = existing.body.split("\n");
            for (int li = 0; li < bodyLines.length; li++) {
                builder.append("        ").append(bodyLines[li]).append("\n");
            }
        }
        builder.append("    }");
        return builder.toString();
    }

    private static String renderCommentedConstructor(ParsedConstructor existing, DescribedType describedType) {
        StringBuilder builder = new StringBuilder();
        builder.append("/*\n");
        builder.append("    ").append(existing.accessModifier).append(" ")
                .append(describedType.simpleName()).append("(");
        appendParameters(builder, existing.paramTypes, existing.paramNames);
        builder.append(") {\n");
        if (existing.body.length() > 0) {
            String[] bodyLines = existing.body.split("\n");
            for (int li = 0; li < bodyLines.length; li++) {
                builder.append("        ").append(bodyLines[li]).append("\n");
            }
        }
        builder.append("    }\n");
        builder.append("    */");
        return builder.toString();
    }

    /**
     * Applies a single surgical constructor edit. Deletion additionally swallows one trailing
     * newline right after the removed range so a deleted constructor does not leave a blank line
     * behind; the removed range itself already starts at its own leading whitespace, per the
     * constructor-matching regex, so no backward trimming is needed or safe.
     */
    private static String applyEdit(String source, Edit edit) {
        int start = edit.start;
        int end = edit.end;
        if (edit.replacement.length() == 0) {
            int newEnd = end;
            while (newEnd < source.length() && (source.charAt(newEnd) == ' ' || source.charAt(newEnd) == '\t')) {
                newEnd++;
            }
            if (newEnd < source.length() && source.charAt(newEnd) == '\n') {
                newEnd++;
            }
            return source.substring(0, start) + source.substring(newEnd);
        }
        return source.substring(0, start) + edit.replacement + source.substring(end);
    }

    /**
     * Checks if an existing constructor can be extended to match a spec constructor.
     * The existing constructor must have all its parameters present in the spec constructor,
     * and the spec constructor must have additional parameters not present in the existing one.
     */
    private static boolean canExtend(ParsedConstructor existing, ConstructorDescriptor spec) {
        if (existing.paramTypes.size() >= spec.parameterTypes().size()) {
            return false;
        }
        // Constructor signatures are ordered and parameter names are not identity. An existing
        // implementation can be extended only when its erased parameter sequence is a prefix of
        // the requested signature; its body and meaningful existing parameter names are retained.
        for (int i = 0; i < existing.paramTypes.size(); i++) {
            if (!sameErasedType(existing.paramTypes.get(i), spec.parameterTypes().get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean constructorsMatch(ParsedConstructor existing, ConstructorDescriptor spec) {
        // Java constructor identity uses ordered erased parameter types. Parameter names,
        // annotations, source qualification, and generic arguments cannot define overloads.
        return ConstructorSignature.of("", existing.paramTypes).equals(
                ConstructorSignature.of("", spec.parameterTypes()));
    }

    private static boolean sameErasedType(String left, String right) {
        return ConstructorSignature.of("", Collections.singletonList(left)).equals(
                ConstructorSignature.of("", Collections.singletonList(right)));
    }

    private static void appendParameters(StringBuilder builder, List<String> types, List<String> names) {
        for (int pi = 0; pi < types.size(); pi++) {
            if (pi > 0) {
                builder.append(", ");
            }
            builder.append(types.get(pi)).append(" ").append(names.get(pi));
        }
    }

    private static final class ParsedConstructor {
        final String accessModifier;
        final List<String> paramTypes;
        final List<String> paramNames;
        final String body;
        final boolean isEmpty;
        final int start;
        final int end;

        ParsedConstructor(
                String accessModifier,
                List<String> paramTypes,
                List<String> paramNames,
                String body,
                boolean isEmpty,
                int start,
                int end
        ) {
            this.accessModifier = accessModifier;
            this.paramTypes = paramTypes;
            this.paramNames = paramNames;
            this.body = body;
            this.isEmpty = isEmpty;
            this.start = start;
            this.end = end;
        }
    }

    private static final class Edit {
        private final int start;
        private final int end;
        private final String replacement;

        private Edit(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
    }
}
