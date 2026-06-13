package org.javaspec.generation;

import org.javaspec.model.ConstructorDescriptor;
import org.javaspec.model.DescribedType;

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
 */
public final class ClassConstructorUpdater {
    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
            "\\s*(public|protected|private)\\s+" +
            "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(" +
            "([^)]*)" +
            "\\)\\s*\\{\\s*" +
            "((?:[^}]|\\{[^}]*\\})*)\\s*" +
            "\\s*\\}",
            Pattern.DOTALL
    );

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
        Files.write(classFile.toPath(), updatedSource.getBytes(StandardCharsets.UTF_8));
        return updatedSource;
    }

    private static List<ParsedConstructor> parseConstructors(String source, String className) {
        List<ParsedConstructor> constructors = new ArrayList<ParsedConstructor>();
        Matcher matcher = CONSTRUCTOR_PATTERN.matcher(source);
        while (matcher.find()) {
            String accessModifier = matcher.group(1);
            String methodName = matcher.group(2);
            String paramsGroup = matcher.group(3);
            String body = matcher.group(4);

            if (!methodName.equals(className)) {
                continue;
            }

            List<String> paramTypes = new ArrayList<String>();
            List<String> paramNames = new ArrayList<String>();

            if (paramsGroup.trim().length() > 0) {
                String[] params = paramsGroup.split(",");
                for (int i = 0; i < params.length; i++) {
                    String param = params[i].trim();
                    if (param.length() > 0) {
                        int lastSpace = param.lastIndexOf(' ');
                        if (lastSpace >= 0) {
                            paramTypes.add(param.substring(0, lastSpace).trim());
                            paramNames.add(param.substring(lastSpace + 1).trim());
                        }
                    }
                }
            }

            boolean isEmpty = body.trim().length() == 0;

            constructors.add(new ParsedConstructor(
                    accessModifier,
                    paramTypes,
                    paramNames,
                    body.trim(),
                    isEmpty,
                    matcher.start(),
                    matcher.end()
            ));
        }
        return constructors;
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

        // Find class declaration and body boundaries
        String classDeclPatternStr = "(class\\s+" + Pattern.quote(describedType.simpleName()) +
                "(?:\\s+extends\\s+[^{]+)?(?:\\s+implements\\s+[^{]+)?(?:\\s*permits\\s+[^{]+)?)\\s*\\{";
        Pattern classDeclPattern = Pattern.compile(classDeclPatternStr, Pattern.DOTALL);
        Matcher classMatcher = classDeclPattern.matcher(source);

        if (!classMatcher.find()) {
            return source;
        }

        int classDeclEnd = classMatcher.end();
        int braceDepth = 1;
        int classBodyEnd = classDeclEnd;
        while (braceDepth > 0 && classBodyEnd < source.length()) {
            char c = source.charAt(classBodyEnd);
            if (c == '{') braceDepth++;
            else if (c == '}') braceDepth--;
            if (braceDepth > 0) classBodyEnd++;
        }

        // Build the new class body
        StringBuilder newBody = new StringBuilder();

        // 1. Add spec constructors (with extended ones and body from matched existing)
        List<ConstructorDescriptor> extendedSpecs = new ArrayList<ConstructorDescriptor>();
        for (int si = 0; si < specConstructors.size(); si++) {
            ConstructorDescriptor spec = specConstructors.get(si);
            boolean wasExtended = false;

            // Check if this spec matches an existing constructor (same params)
            String matchedBody = null;
            for (int ei = 0; ei < specMatched.size(); ei++) {
                ParsedConstructor existing = specMatched.get(ei);
                if (constructorsMatch(existing, spec)) {
                    matchedBody = existing.body;
                    break;
                }
            }

            // Check if this spec can extend an existing constructor (existing has subset of params)
            if (matchedBody == null) {
                for (int ei = 0; ei < toExtend.size(); ei++) {
                    ParsedConstructor existing = toExtend.get(ei);
                    if (canExtend(existing, spec)) {
                        // Extend: use existing body, add new params from spec
                        List<String> newParams = new ArrayList<String>();
                        List<String> newNames = new ArrayList<String>();
                        for (int pi = 0; pi < spec.parameterTypes().size(); pi++) {
                            boolean found = false;
                            for (int epi = 0; epi < existing.paramTypes.size(); epi++) {
                                if (simpleName(existing.paramTypes.get(epi)).equals(simpleName(spec.parameterTypes().get(pi)))
                                        && existing.paramNames.get(epi).equals(spec.parameterNames().get(pi))) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                newParams.add(spec.parameterTypes().get(pi));
                                newNames.add(spec.parameterNames().get(pi));
                            }
                        }
                        // Build extended constructor: existing params + new params, existing body
                        newBody.append("    public ").append(describedType.simpleName()).append("(");
                        appendParameters(newBody, existing.paramTypes, existing.paramNames);
                        if (newParams.size() > 0) {
                            newBody.append(", ");
                            appendParameters(newBody, newParams, newNames);
                        }
                        newBody.append(") {\n");
                        if (existing.body.length() > 0) {
                            String[] bodyLines = existing.body.split("\n");
                            for (int li = 0; li < bodyLines.length; li++) {
                                newBody.append("        ").append(bodyLines[li]).append("\n");
                            }
                        }
                        newBody.append("    }\n\n");
                        wasExtended = true;
                        break;
                    }
                }
            }

            if (!wasExtended) {
                // Use matched body if available (existing constructor body preserved)
                String body = spec.hasBody() ? spec.bodyContent() : (matchedBody != null ? matchedBody : "");
                newBody.append("    public ").append(describedType.simpleName()).append("(");
                appendParameters(newBody, spec.parameterTypes(), spec.parameterNames());
                newBody.append(") {");
                if (body.length() > 0) {
                    newBody.append("\n");
                    String[] bodyLines = body.split("\n");
                    for (int li = 0; li < bodyLines.length; li++) {
                        newBody.append("        ").append(bodyLines[li]).append("\n");
                    }
                }
                newBody.append("    }\n\n");
            }
        }

        // 2. Add unmatched constructors that are kept (PRESERVE policy)
        for (int ki = 0; ki < toKeep.size(); ki++) {
            ParsedConstructor existing = toKeep.get(ki);
            newBody.append("    ").append(existing.accessModifier).append(" ")
                    .append(describedType.simpleName()).append("(");
            appendParameters(newBody, existing.paramTypes, existing.paramNames);
            newBody.append(") {\n");
            if (existing.body.length() > 0) {
                String[] bodyLines = existing.body.split("\n");
                for (int li = 0; li < bodyLines.length; li++) {
                    newBody.append("        ").append(bodyLines[li]).append("\n");
                }
            }
            newBody.append("    }\n\n");
        }

        // 3. Add unmatched constructors that are commented out (COMMENT policy)
        for (int ci = 0; ci < toComment.size(); ci++) {
            ParsedConstructor existing = toComment.get(ci);
            newBody.append("    /*\n");
            newBody.append("    ").append(existing.accessModifier).append(" ")
                    .append(describedType.simpleName()).append("(");
            appendParameters(newBody, existing.paramTypes, existing.paramNames);
            newBody.append(") {\n");
            if (existing.body.length() > 0) {
                String[] bodyLines = existing.body.split("\n");
                for (int li = 0; li < bodyLines.length; li++) {
                    newBody.append("        ").append(bodyLines[li]).append("\n");
                }
            }
            newBody.append("    }\n");
            newBody.append("    */\n\n");
        }

        // Build the final source
        StringBuilder builder = new StringBuilder();
        builder.append(source.substring(0, classDeclEnd));
        builder.append("\n");
        String bodyStr = newBody.toString().trim();
        if (bodyStr.length() > 0) {
            builder.append(bodyStr);
            builder.append("\n");
        }
        builder.append("}\n");

        return builder.toString();
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
        // Check that all existing params are present in the spec
        for (int ei = 0; ei < existing.paramTypes.size(); ei++) {
            String existingType = simpleName(existing.paramTypes.get(ei));
            String existingName = existing.paramNames.get(ei);
            boolean found = false;
            for (int si = 0; si < spec.parameterTypes().size(); si++) {
                String specType = simpleName(spec.parameterTypes().get(si));
                String specName = spec.parameterNames().get(si);
                if (existingType.equals(specType) && existingName.equals(specName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        // Check that spec has at least one param not in existing
        int extraCount = 0;
        for (int si = 0; si < spec.parameterTypes().size(); si++) {
            String specType = simpleName(spec.parameterTypes().get(si));
            String specName = spec.parameterNames().get(si);
            boolean found = false;
            for (int ei = 0; ei < existing.paramTypes.size(); ei++) {
                String existingType = simpleName(existing.paramTypes.get(ei));
                String existingName = existing.paramNames.get(ei);
                if (specType.equals(existingType) && specName.equals(existingName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                extraCount++;
            }
        }
        return extraCount > 0;
    }

    private static boolean constructorsMatch(ParsedConstructor existing, ConstructorDescriptor spec) {
        if (existing.paramTypes.size() != spec.parameterTypes().size()) {
            return false;
        }
        for (int i = 0; i < existing.paramTypes.size(); i++) {
            String existingType = simpleName(existing.paramTypes.get(i));
            String specType = simpleName(spec.parameterTypes().get(i));
            if (!existingType.equals(specType)) {
                return false;
            }
            String existingName = existing.paramNames.get(i);
            String specName = spec.parameterNames().get(i);
            if (!existingName.equals(specName)) {
                return false;
            }
        }
        return true;
    }

    private static void appendParameters(StringBuilder builder, List<String> types, List<String> names) {
        for (int pi = 0; pi < types.size(); pi++) {
            if (pi > 0) {
                builder.append(", ");
            }
            builder.append(types.get(pi)).append(" ").append(names.get(pi));
        }
    }

    private static String simpleName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot < 0) {
            return typeName;
        }
        return typeName.substring(lastDot + 1);
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
}
