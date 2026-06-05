package org.javaspec.compatibility;

import org.javaspec.model.DescribedType;
import org.javaspec.model.MethodDescriptor;
import org.javaspec.profile.ApiSymbol;
import org.javaspec.profile.ApiSymbolKind;
import org.javaspec.profile.ProfileCatalog;
import org.javaspec.profile.TargetProfile;

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

    private void addMethodReturnTypeViolation(
            TargetProfile targetProfile,
            DescribedType describedType,
            MethodDescriptor method,
            List<ProfileViolation> violations
    ) {
        List<String> owners = inspectableCatalogOwners(method.returnType());
        for (int i = 0; i < owners.size(); i++) {
            CompatibilityResult result = compatibilityCheck.checkApiSymbol(targetProfile, owners.get(i), null);
            if (result.isDenied()) {
                violations.add(ProfileViolation.of(describedType, "method " + method.methodName() + " return type", result));
            }
        }
    }

    private void addMethodParameterTypeViolations(
            TargetProfile targetProfile,
            DescribedType describedType,
            MethodDescriptor method,
            List<ProfileViolation> violations
    ) {
        List<String> parameterTypes = method.parameterTypes();
        for (int i = 0; i < parameterTypes.size(); i++) {
            List<String> owners = inspectableCatalogOwners(parameterTypes.get(i));
            for (int oi = 0; oi < owners.size(); oi++) {
                CompatibilityResult result = compatibilityCheck.checkApiSymbol(targetProfile, owners.get(oi), null);
                if (result.isDenied()) {
                    violations.add(ProfileViolation.of(
                            describedType,
                            "method " + method.methodName() + " parameter " + (i + 1) + " type",
                            result
                    ));
                }
            }
        }
    }

    private List<String> inspectableCatalogOwners(String typeName) {
        List<String> owners = new ArrayList<String>();
        addInspectableCatalogOwners(typeName, owners);
        return owners;
    }

    private void addInspectableCatalogOwners(String typeName, List<String> owners) {
        String withoutWildcard = stripWildcardPrefix(typeName);
        String normalized = stripArrayAndVarargsSuffix(withoutWildcard);
        if (normalized == null || normalized.length() == 0 || isPrimitiveOrVoid(normalized)) {
            return;
        }
        TypeParts typeParts = TypeParts.parse(normalized);
        if (typeParts == null) {
            return;
        }
        addCatalogOwner(typeParts.rawType(), owners);
        List<String> arguments = typeParts.arguments();
        for (int i = 0; i < arguments.size(); i++) {
            addInspectableCatalogOwners(arguments.get(i), owners);
        }
    }

    private void addCatalogOwner(String rawType, List<String> owners) {
        if (rawType == null || rawType.length() == 0 || isPrimitiveOrVoid(rawType)) {
            return;
        }
        if (!isQualifiedOrSimpleTypeName(rawType)) {
            return;
        }
        String owner;
        if (rawType.indexOf('.') >= 0) {
            owner = hasTypeSymbol(rawType) ? rawType : null;
        } else {
            owner = uniqueCatalogOwnerForSimpleName(rawType);
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

    private static String stripWildcardPrefix(String typeName) {
        if (typeName == null) {
            return null;
        }
        String trimmed = typeName.trim();
        if ("?".equals(trimmed)) {
            return "";
        }
        if (trimmed.startsWith("? extends ")) {
            return trimmed.substring("? extends ".length()).trim();
        }
        if (trimmed.startsWith("? super ")) {
            return trimmed.substring("? super ".length()).trim();
        }
        return trimmed;
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
            List<String> arguments = splitTypeArguments(argumentsSource);
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

        private static List<String> splitTypeArguments(String source) {
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
                if (character == ',' && depth == 0) {
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
    }
}
