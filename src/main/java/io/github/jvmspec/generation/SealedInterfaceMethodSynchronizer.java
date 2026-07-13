package io.github.jvmspec.generation;

import static io.github.jvmspec.generation.JavaSourceEditor.*;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.MethodDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-preserving method synchronization for sealed interfaces and nested permitted types. */
final class SealedInterfaceMethodSynchronizer {
    private static final Pattern NESTED_TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b");
    private static final Pattern PERMITS_KEYWORD_PATTERN = Pattern.compile("\\bpermits\\b");

    private SealedInterfaceMethodSynchronizer() {
    }

    static String updateSource(String existingSource, DescribedType describedType) {
        List<MethodDescriptor> methods = JavaMethodEligibility.interfaceMethods(describedType);
        if (methods.isEmpty()) {
            return existingSource;
        }
        String masked = maskNonCode(existingSource);
        int rootOpenBrace = findPrimaryTypeOpenBrace(masked, describedType.simpleName());
        if (rootOpenBrace < 0) {
            return existingSource;
        }
        int rootClosingBrace = findMatchingBraceInMasked(masked, rootOpenBrace);
        if (rootClosingBrace < 0) {
            return existingSource;
        }
        List<NestedTypeRegion> nestedRegions = nestedTypeRegions(masked, rootOpenBrace, rootClosingBrace);
        Set<String> permittedNames = permittedSimpleNames(masked, describedType);

        String result = existingSource;

        // The root closing brace is the highest insertion offset, so it is applied first; nested
        // regions are then applied from last to first so earlier offsets stay valid.
        List<MethodDescriptor> rootMissing = JavaMethodInventory.missingMethodsInScope(
                rootScopeText(masked, rootOpenBrace, rootClosingBrace, nestedRegions), methods);
        if (!rootMissing.isEmpty()) {
            String indent = memberIndentationBefore(existingSource, rootClosingBrace);
            String insertion = JavaMethodRenderer.renderInterfaceDeclarations(rootMissing, describedType, indent);
            result = insertBeforeClosingBraceKeepingIndent(result, rootClosingBrace, insertion);
        }
        for (int i = nestedRegions.size() - 1; i >= 0; i--) {
            NestedTypeRegion region = nestedRegions.get(i);
            if (!permittedNames.contains(region.simpleName)) {
                continue;
            }
            List<MethodDescriptor> nestedMissing = JavaMethodInventory.missingMethodsInScope(
                    masked.substring(region.openBrace + 1, region.closingBrace), methods);
            if (nestedMissing.isEmpty()) {
                continue;
            }
            String indent = memberIndentationBefore(existingSource, region.closingBrace);
            String insertion = "interface".equals(region.keyword)
                    ? JavaMethodRenderer.renderInterfaceDeclarations(nestedMissing, describedType, indent)
                    : JavaMethodRenderer.renderMethods(nestedMissing, describedType, indent);
            result = insertBeforeClosingBraceKeepingIndent(result, region.closingBrace, insertion);
        }
        return result;
    }

    /**
     * Finds nested type declarations placed directly inside the sealed root body.
     */
    private static List<NestedTypeRegion> nestedTypeRegions(String masked, int rootOpenBrace, int rootClosingBrace) {
        List<NestedTypeRegion> regions = new ArrayList<NestedTypeRegion>();
        Matcher matcher = NESTED_TYPE_PATTERN.matcher(masked);
        matcher.region(rootOpenBrace + 1, rootClosingBrace);
        while (matcher.find()) {
            if (braceDepthBetween(masked, rootOpenBrace + 1, matcher.start()) != 0) {
                continue;
            }
            int openBrace = masked.indexOf('{', matcher.end());
            if (openBrace < 0 || openBrace >= rootClosingBrace) {
                continue;
            }
            int closingBrace = findMatchingBraceInMasked(masked, openBrace);
            if (closingBrace < 0 || closingBrace >= rootClosingBrace) {
                continue;
            }
            regions.add(new NestedTypeRegion(matcher.group(1), matcher.group(2), matcher.start(), openBrace, closingBrace));
        }
        return regions;
    }

    private static int braceDepthBetween(String masked, int start, int end) {
        int depth = 0;
        for (int i = start; i < end; i++) {
            char c = masked.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return depth;
    }

    /**
     * Returns the masked sealed root body with nested type declarations blanked out, so that
     * signature de-duplication for the root scope ignores nested implementation members.
     */
    private static String rootScopeText(String masked, int rootOpenBrace, int rootClosingBrace, List<NestedTypeRegion> nestedRegions) {
        char[] scope = masked.substring(rootOpenBrace + 1, rootClosingBrace).toCharArray();
        for (int r = 0; r < nestedRegions.size(); r++) {
            NestedTypeRegion region = nestedRegions.get(r);
            int from = Math.max(0, region.declarationStart - rootOpenBrace - 1);
            int to = Math.min(scope.length, region.closingBrace + 1 - rootOpenBrace - 1);
            for (int i = from; i < to; i++) {
                scope[i] = blankedChar(scope[i]);
            }
        }
        return new String(scope);
    }

    /**
     * Collects the simple names of permitted implementations targeted for in-file updates: names
     * from the source {@code permits} clause, names from the described permitted types, and the
     * generated default nested {@code Permitted} implementation name.
     */
    private static Set<String> permittedSimpleNames(String masked, DescribedType describedType) {
        Set<String> names = new LinkedHashSet<String>();
        Matcher headerMatcher = primaryTypePattern(describedType.simpleName()).matcher(masked);
        if (headerMatcher.find()) {
            String header = headerMatcher.group();
            Matcher permitsMatcher = PERMITS_KEYWORD_PATTERN.matcher(header);
            if (permitsMatcher.find()) {
                String clause = header.substring(permitsMatcher.end(), header.length() - 1);
                String[] entries = clause.split(",");
                for (int i = 0; i < entries.length; i++) {
                    String entry = entries[i].trim();
                    if (entry.length() > 0) {
                        names.add(simpleNameOf(entry));
                    }
                }
            }
        }
        List<String> permittedTypeNames = describedType.permittedTypeNames();
        for (int i = 0; i < permittedTypeNames.size(); i++) {
            names.add(simpleNameOf(permittedTypeNames.get(i)));
        }
        names.add("Permitted");
        return names;
    }

    private static String simpleNameOf(String typeName) {
        String trimmed = typeName.trim();
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot < 0) {
            return trimmed;
        }
        return trimmed.substring(lastDot + 1);
    }

    private static final class NestedTypeRegion {
        private final String keyword;
        private final String simpleName;
        private final int declarationStart;
        private final int openBrace;
        private final int closingBrace;

        private NestedTypeRegion(String keyword, String simpleName, int declarationStart, int openBrace, int closingBrace) {
            this.keyword = keyword;
            this.simpleName = simpleName;
            this.declarationStart = declarationStart;
            this.openBrace = openBrace;
            this.closingBrace = closingBrace;
        }
    }
}
