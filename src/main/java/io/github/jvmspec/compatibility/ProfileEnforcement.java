package io.github.jvmspec.compatibility;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.MethodDescriptor;
import io.github.jvmspec.profile.ApiSymbol;
import io.github.jvmspec.profile.ApiSymbolKind;
import io.github.jvmspec.profile.ProfileCatalog;
import io.github.jvmspec.profile.TargetProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Enforces a selected target profile against source that javaspec may generate for a described type.
 */
public final class ProfileEnforcement {
    private static final ProfileEnforcement DEFAULT = new ProfileEnforcement(ProfileCompatibilityCheck.defaultCheck());

    private final ProfileCompatibilityCheck compatibilityCheck;

    private ProfileEnforcement(ProfileCompatibilityCheck compatibilityCheck) {
        this.compatibilityCheck = Objects.requireNonNull(compatibilityCheck, "compatibilityCheck must not be null");
    }

    public static ProfileEnforcement defaultEnforcement() {
        return DEFAULT;
    }

    public static ProfileEnforcement using(ProfileCatalog catalog) {
        return using(ProfileCompatibilityCheck.using(catalog));
    }

    public static ProfileEnforcement using(CompatibilityCheck compatibilityCheck) {
        Objects.requireNonNull(compatibilityCheck, "compatibilityCheck must not be null");
        return using(compatibilityCheck.catalog());
    }

    public static ProfileEnforcement using(ProfileCompatibilityCheck compatibilityCheck) {
        return new ProfileEnforcement(compatibilityCheck);
    }

    public ProfileCompatibilityCheck compatibilityCheck() {
        return compatibilityCheck;
    }

    public ProfileCompatibilityCheck check() {
        return compatibilityCheck;
    }

    public ProfileCatalog catalog() {
        return compatibilityCheck.catalog();
    }

    public ProfileEnforcementReport enforce(TargetProfile targetProfile, DescribedType describedType) {
        Objects.requireNonNull(targetProfile, "targetProfile must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");

        List<ProfileViolation> violations = new ArrayList<ProfileViolation>();
        CompatibilityResult kindResult = compatibilityCheck.checkTypeKind(targetProfile, describedType.kind());
        if (kindResult.isDenied()) {
            violations.add(ProfileViolation.of(describedType, "type kind", kindResult));
        }

        addExtendedTypeViolations(targetProfile, describedType, violations);
        addIndexedTypeReferenceViolations(
                targetProfile,
                describedType,
                describedType.implementedTypeNames(),
                "implemented type",
                violations
        );
        addIndexedTypeReferenceViolations(
                targetProfile,
                describedType,
                describedType.permittedTypeNames(),
                "permitted type",
                violations
        );

        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            addMethodReturnTypeViolation(targetProfile, describedType, method, violations);
            addMethodParameterTypeViolations(targetProfile, describedType, method, violations);
        }

        return ProfileEnforcementReport.of(targetProfile, describedType, violations);
    }

    public boolean isAllowed(TargetProfile targetProfile, DescribedType describedType) {
        return enforce(targetProfile, describedType).isAllowed();
    }

    public boolean isDenied(TargetProfile targetProfile, DescribedType describedType) {
        return enforce(targetProfile, describedType).isDenied();
    }

    private void addExtendedTypeViolations(
            TargetProfile targetProfile,
            DescribedType describedType,
            List<ProfileViolation> violations
    ) {
        List<String> extendedTypeNames = describedType.extendedTypeNames();
        for (int i = 0; i < extendedTypeNames.size(); i++) {
            String location = extendedTypeNames.size() == 1 ? "super type" : "super type " + (i + 1);
            addTypeReferenceViolations(targetProfile, describedType, extendedTypeNames.get(i), location, violations);
        }
    }

    private void addIndexedTypeReferenceViolations(
            TargetProfile targetProfile,
            DescribedType describedType,
            List<String> typeNames,
            String locationPrefix,
            List<ProfileViolation> violations
    ) {
        for (int i = 0; i < typeNames.size(); i++) {
            addTypeReferenceViolations(
                    targetProfile,
                    describedType,
                    typeNames.get(i),
                    locationPrefix + " " + (i + 1),
                    violations
            );
        }
    }

    private void addTypeReferenceViolations(
            TargetProfile targetProfile,
            DescribedType describedType,
            String typeName,
            String location,
            List<ProfileViolation> violations
    ) {
        List<String> owners = inspectableCatalogOwners(typeName);
        for (int i = 0; i < owners.size(); i++) {
            CompatibilityResult result = compatibilityCheck.checkApiSymbol(targetProfile, owners.get(i), null);
            if (result.isDenied()) {
                violations.add(ProfileViolation.of(describedType, location, result));
            }
        }
    }

    private void addMethodReturnTypeViolation(
            TargetProfile targetProfile,
            DescribedType describedType,
            MethodDescriptor method,
            List<ProfileViolation> violations
    ) {
        addTypeReferenceViolations(
                targetProfile,
                describedType,
                method.returnType(),
                "method " + method.methodName() + " return type",
                violations
        );
    }

    private void addMethodParameterTypeViolations(
            TargetProfile targetProfile,
            DescribedType describedType,
            MethodDescriptor method,
            List<ProfileViolation> violations
    ) {
        List<String> parameterTypes = method.parameterTypes();
        for (int i = 0; i < parameterTypes.size(); i++) {
            addTypeReferenceViolations(
                    targetProfile,
                    describedType,
                    parameterTypes.get(i),
                    "method " + method.methodName() + " parameter " + (i + 1) + " type",
                    violations
            );
        }
    }

    private List<String> inspectableCatalogOwners(String typeName) {
        List<String> owners = new ArrayList<String>();
        addInspectableCatalogOwners(typeName, owners);
        return owners;
    }

    private void addInspectableCatalogOwners(String typeName, List<String> owners) {
        String normalized = normalizeTypeReference(typeName);
        if (normalized == null || normalized.length() == 0) {
            return;
        }

        int methodTypeParametersEnd = methodTypeParametersEnd(normalized);
        if (methodTypeParametersEnd >= 0) {
            String typeParameterSource = normalized.substring(1, methodTypeParametersEnd);
            addTypeParameterBoundOwners(typeParameterSource, owners);
            String remainder = normalized.substring(methodTypeParametersEnd + 1).trim();
            if (remainder.length() > 0) {
                addInspectableCatalogOwners(remainder, owners);
            }
            return;
        }

        String wildcardBounds = wildcardBounds(normalized);
        if (wildcardBounds != null) {
            addBoundCatalogOwners(wildcardBounds, owners);
            return;
        }

        int typeVariableExtends = topLevelKeywordIndex(normalized, "extends");
        if (typeVariableExtends > 0 && isLikelyTypeVariableName(normalized.substring(0, typeVariableExtends))) {
            addBoundCatalogOwners(normalized.substring(typeVariableExtends + "extends".length()), owners);
            return;
        }

        List<String> intersectionParts = splitTopLevel(normalized, '&');
        if (intersectionParts == null) {
            return;
        }
        if (intersectionParts.size() > 1) {
            for (int i = 0; i < intersectionParts.size(); i++) {
                addInspectableCatalogOwners(intersectionParts.get(i), owners);
            }
            return;
        }

        String withoutArrayOrVarargs = stripArrayAndVarargsSuffix(normalized);
        if (withoutArrayOrVarargs == null || withoutArrayOrVarargs.length() == 0 || isPrimitiveOrVoid(withoutArrayOrVarargs)) {
            return;
        }
        TypeParts typeParts = TypeParts.parse(withoutArrayOrVarargs);
        if (typeParts == null) {
            return;
        }
        addCatalogOwner(typeParts.rawType(), owners);
        List<String> arguments = typeParts.arguments();
        for (int i = 0; i < arguments.size(); i++) {
            addInspectableCatalogOwners(arguments.get(i), owners);
        }
    }

    private void addTypeParameterBoundOwners(String typeParameterSource, List<String> owners) {
        List<String> declarations = splitTopLevel(typeParameterSource, ',');
        if (declarations == null) {
            return;
        }
        for (int i = 0; i < declarations.size(); i++) {
            String declaration = normalizeTypeReference(declarations.get(i));
            if (declaration == null || declaration.length() == 0) {
                continue;
            }
            int extendsIndex = topLevelKeywordIndex(declaration, "extends");
            if (extendsIndex < 0) {
                continue;
            }
            addBoundCatalogOwners(declaration.substring(extendsIndex + "extends".length()), owners);
        }
    }

    private void addBoundCatalogOwners(String boundsSource, List<String> owners) {
        String normalized = normalizeTypeReference(boundsSource);
        if (normalized == null || normalized.length() == 0) {
            return;
        }
        List<String> bounds = splitTopLevel(normalized, '&');
        if (bounds == null) {
            return;
        }
        for (int i = 0; i < bounds.size(); i++) {
            addInspectableCatalogOwners(bounds.get(i), owners);
        }
    }

    private void addCatalogOwner(String rawType, List<String> owners) {
        if (rawType == null || rawType.length() == 0 || isPrimitiveOrVoid(rawType)) {
            return;
        }
        String canonicalRawType = canonicalTypeName(rawType);
        if (!isQualifiedOrSimpleTypeName(canonicalRawType)) {
            return;
        }
        String owner;
        if (canonicalRawType.indexOf('.') >= 0) {
            owner = hasTypeSymbol(canonicalRawType) ? canonicalRawType : null;
        } else {
            owner = uniqueCatalogOwnerForSimpleName(canonicalRawType);
        }
        if (owner != null && !owners.contains(owner)) {
            owners.add(owner);
        }
    }

    private String uniqueCatalogOwnerForSimpleName(String simpleName) {
        String match = null;
        for (Map.Entry<String, List<ApiSymbol>> entry : catalog().symbolsByOwner().entrySet()) {
            String owner = entry.getKey();
            if (!hasTypeSymbol(owner) || !isQualifiedOrSimpleTypeName(owner)) {
                continue;
            }
            if (!simpleName.equals(simpleName(owner))) {
                continue;
            }
            if (match != null && !match.equals(owner)) {
                return null;
            }
            match = owner;
        }
        return match;
    }

    private boolean hasTypeSymbol(String owner) {
        List<ApiSymbol> symbols = catalog().lookup(owner, null);
        for (int i = 0; i < symbols.size(); i++) {
            ApiSymbolKind kind = symbols.get(i).kind();
            if (ApiSymbolKind.TYPE.equals(kind)
                    || ApiSymbolKind.NESTED_TYPE.equals(kind)
                    || ApiSymbolKind.ARRAY_TYPE.equals(kind)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeTypeReference(String typeName) {
        if (typeName == null) {
            return null;
        }
        return stripLeadingModifiers(stripAnnotations(typeName).trim()).trim();
    }

    private static String canonicalTypeName(String typeName) {
        return typeName.trim().replace('$', '.');
    }

    private static int methodTypeParametersEnd(String typeName) {
        if (!typeName.startsWith("<")) {
            return -1;
        }
        int depth = 0;
        for (int i = 0; i < typeName.length(); i++) {
            char character = typeName.charAt(i);
            if (character == '<') {
                depth++;
            } else if (character == '>') {
                depth--;
                if (depth == 0) {
                    return i;
                }
                if (depth < 0) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static String wildcardBounds(String typeName) {
        if (!typeName.startsWith("?")) {
            return null;
        }
        String rest = typeName.substring(1).trim();
        if (rest.length() == 0) {
            return "";
        }
        if (startsWithKeyword(rest, "extends")) {
            return rest.substring("extends".length()).trim();
        }
        if (startsWithKeyword(rest, "super")) {
            return rest.substring("super".length()).trim();
        }
        return null;
    }

    private static boolean startsWithKeyword(String value, String keyword) {
        if (!value.startsWith(keyword)) {
            return false;
        }
        if (value.length() == keyword.length()) {
            return true;
        }
        return !Character.isJavaIdentifierPart(value.charAt(keyword.length()));
    }

    private static int topLevelKeywordIndex(String value, String keyword) {
        int depth = 0;
        for (int i = 0; i <= value.length() - keyword.length(); i++) {
            char character = value.charAt(i);
            if (depth == 0 && value.startsWith(keyword, i) && hasKeywordBoundary(value, i, keyword.length())) {
                return i;
            }
            if (character == '<') {
                depth++;
            } else if (character == '>') {
                depth--;
                if (depth < 0) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static boolean hasKeywordBoundary(String value, int start, int length) {
        int before = start - 1;
        int after = start + length;
        if (before >= 0 && Character.isJavaIdentifierPart(value.charAt(before))) {
            return false;
        }
        return after >= value.length() || !Character.isJavaIdentifierPart(value.charAt(after));
    }

    private static boolean isLikelyTypeVariableName(String source) {
        String normalized = normalizeTypeReference(source);
        return normalized != null
                && normalized.indexOf('.') < 0
                && normalized.indexOf('<') < 0
                && isJavaIdentifier(normalized);
    }

    private static List<String> splitTopLevel(String source, char delimiter) {
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < source.length(); i++) {
            char character = source.charAt(i);
            if (character == '<') {
                depth++;
                current.append(character);
                continue;
            }
            if (character == '>') {
                depth--;
                if (depth < 0) {
                    return null;
                }
                current.append(character);
                continue;
            }
            if (character == delimiter && depth == 0) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        if (depth != 0) {
            return null;
        }
        String last = current.toString().trim();
        if (last.length() > 0 || source.length() > 0) {
            result.add(last);
        }
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).length() == 0) {
                return null;
            }
        }
        return result;
    }

    private static String stripAnnotations(String typeName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < typeName.length(); i++) {
            char character = typeName.charAt(i);
            if (character != '@') {
                result.append(character);
                continue;
            }

            int identifierStart = i + 1;
            int identifierEnd = readQualifiedIdentifier(typeName, identifierStart);
            if (identifierEnd < 0) {
                result.append(character);
                continue;
            }

            int afterAnnotation = skipWhitespace(typeName, identifierEnd);
            if (afterAnnotation < typeName.length() && typeName.charAt(afterAnnotation) == '(') {
                int afterArguments = skipBalancedParentheses(typeName, afterAnnotation);
                if (afterArguments < 0) {
                    return typeName;
                }
                afterAnnotation = afterArguments;
            }
            result.append(' ');
            i = afterAnnotation - 1;
        }
        return result.toString();
    }

    private static int readQualifiedIdentifier(String value, int start) {
        int index = readIdentifier(value, start);
        if (index < 0) {
            return -1;
        }
        while (index < value.length() && value.charAt(index) == '.') {
            int next = readIdentifier(value, index + 1);
            if (next < 0) {
                break;
            }
            index = next;
        }
        return index;
    }

    private static int readIdentifier(String value, int start) {
        if (start >= value.length()) {
            return -1;
        }
        int firstCodePoint = value.codePointAt(start);
        if (!Character.isJavaIdentifierStart(firstCodePoint)) {
            return -1;
        }
        int index = start + Character.charCount(firstCodePoint);
        while (index < value.length()) {
            int currentCodePoint = value.codePointAt(index);
            if (!Character.isJavaIdentifierPart(currentCodePoint)) {
                break;
            }
            index += Character.charCount(currentCodePoint);
        }
        return index;
    }

    private static int skipBalancedParentheses(String value, int openIndex) {
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = openIndex; i < value.length(); i++) {
            char character = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (character == '\\' && (inString || inChar)) {
                escaped = true;
                continue;
            }
            if (character == '"' && !inChar) {
                inString = !inString;
                continue;
            }
            if (character == '\'' && !inString) {
                inChar = !inChar;
                continue;
            }
            if (inString || inChar) {
                continue;
            }
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
                if (depth < 0) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static int skipWhitespace(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static String stripLeadingModifiers(String typeName) {
        String result = typeName.trim();
        boolean changed = true;
        while (changed) {
            changed = false;
            int tokenEnd = leadingTokenEnd(result);
            if (tokenEnd <= 0) {
                return result;
            }
            String token = result.substring(0, tokenEnd);
            if (isHarmlessModifier(token)) {
                result = result.substring(tokenEnd).trim();
                changed = true;
            }
        }
        return result;
    }

    private static int leadingTokenEnd(String value) {
        if (value.length() == 0) {
            return -1;
        }
        int index = 0;
        while (index < value.length()) {
            char character = value.charAt(index);
            if (!Character.isJavaIdentifierPart(character) && character != '-') {
                break;
            }
            index++;
        }
        return index;
    }

    private static boolean isHarmlessModifier(String token) {
        return "public".equals(token)
                || "protected".equals(token)
                || "private".equals(token)
                || "abstract".equals(token)
                || "static".equals(token)
                || "final".equals(token)
                || "strictfp".equals(token)
                || "transient".equals(token)
                || "volatile".equals(token)
                || "synchronized".equals(token)
                || "native".equals(token)
                || "default".equals(token)
                || "sealed".equals(token)
                || "non-sealed".equals(token);
    }

    private static String stripArrayAndVarargsSuffix(String typeName) {
        if (typeName == null) {
            return null;
        }
        String result = typeName.trim();
        boolean changed = true;
        while (changed) {
            changed = false;
            result = trimRight(result);
            if (result.endsWith("...")) {
                result = result.substring(0, result.length() - 3).trim();
                changed = true;
                continue;
            }
            int closeBracket = previousNonWhitespace(result, result.length() - 1);
            if (closeBracket >= 0 && result.charAt(closeBracket) == ']') {
                int openBracket = previousNonWhitespace(result, closeBracket - 1);
                if (openBracket >= 0 && result.charAt(openBracket) == '[') {
                    result = result.substring(0, openBracket).trim();
                    changed = true;
                }
            }
        }
        return result;
    }

    private static boolean isPrimitiveOrVoid(String typeName) {
        return "boolean".equals(typeName)
                || "byte".equals(typeName)
                || "short".equals(typeName)
                || "int".equals(typeName)
                || "long".equals(typeName)
                || "float".equals(typeName)
                || "double".equals(typeName)
                || "char".equals(typeName)
                || "void".equals(typeName);
    }

    private static boolean isQualifiedOrSimpleTypeName(String typeName) {
        if (typeName.length() == 0) {
            return false;
        }
        int start = 0;
        while (start <= typeName.length()) {
            int dot = typeName.indexOf('.', start);
            String part;
            if (dot < 0) {
                part = typeName.substring(start);
                start = typeName.length() + 1;
            } else {
                part = typeName.substring(start, dot);
                start = dot + 1;
            }
            if (!isJavaIdentifier(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJavaIdentifier(String value) {
        if (value.length() == 0) {
            return false;
        }
        int index = 0;
        int firstCodePoint = value.codePointAt(index);
        if (!Character.isJavaIdentifierStart(firstCodePoint)) {
            return false;
        }
        index += Character.charCount(firstCodePoint);
        while (index < value.length()) {
            int currentCodePoint = value.codePointAt(index);
            if (!Character.isJavaIdentifierPart(currentCodePoint)) {
                return false;
            }
            index += Character.charCount(currentCodePoint);
        }
        return true;
    }

    private static String simpleName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot < 0) {
            return typeName;
        }
        return typeName.substring(lastDot + 1);
    }

    private static String trimRight(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static int previousNonWhitespace(String value, int start) {
        for (int i = start; i >= 0; i--) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static final class TypeParts {
        private final String rawType;
        private final List<String> arguments;

        private TypeParts(String rawType, List<String> arguments) {
            this.rawType = rawType;
            this.arguments = arguments;
        }

        static TypeParts parse(String typeName) {
            String normalized = typeName.trim();
            int genericStart = normalized.indexOf('<');
            if (genericStart < 0) {
                if (normalized.indexOf('>') >= 0) {
                    return null;
                }
                return new TypeParts(normalized, new ArrayList<String>());
            }

            int genericEnd = previousNonWhitespace(normalized, normalized.length() - 1);
            if (genericEnd < 0 || normalized.charAt(genericEnd) != '>') {
                return null;
            }
            String raw = normalized.substring(0, genericStart).trim();
            if (raw.length() == 0) {
                return null;
            }
            String argumentsSource = normalized.substring(genericStart + 1, genericEnd);
            List<String> arguments = splitTopLevel(argumentsSource, ',');
            if (arguments == null) {
                return null;
            }
            return new TypeParts(raw, arguments);
        }

        String rawType() {
            return rawType;
        }

        List<String> arguments() {
            return arguments;
        }
    }
}
