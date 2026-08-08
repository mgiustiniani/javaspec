package io.github.jvmspec.internal.language;

import io.github.jvmspec.internal.type.JavaTypeRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Language-neutral structural type evidence carried by an internal behavior contract. */
public final class BehaviorTypeRef {
    public enum Kind { PRIMITIVE, DECLARED, TYPE_VARIABLE, ARRAY, WILDCARD, UNKNOWN }
    public enum Variance { EXACT, EXTENDS, SUPER, UNBOUNDED }

    private final Kind kind;
    private final String jvmName;
    private final List<BehaviorTypeRef> arguments;
    private final BehaviorTypeRef component;
    private final Variance variance;
    private final String sourceEvidence;

    private BehaviorTypeRef(
            Kind kind,
            String jvmName,
            List<BehaviorTypeRef> arguments,
            BehaviorTypeRef component,
            Variance variance,
            String sourceEvidence
    ) {
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.jvmName = jvmName;
        this.arguments = Collections.unmodifiableList(
                new ArrayList<BehaviorTypeRef>(arguments));
        this.component = component;
        this.variance = Objects.requireNonNull(variance, "variance must not be null");
        this.sourceEvidence = Objects.requireNonNull(
                sourceEvidence, "sourceEvidence must not be null");
    }

    public static BehaviorTypeRef fromJava(String sourceType) {
        return fromJava(sourceType, "");
    }

    public static BehaviorTypeRef fromJava(String sourceType, String defaultPackage) {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        try {
            return fromJavaType(JavaTypeRef.resolve(
                    sourceType, Collections.<String, String>emptyMap(),
                    defaultPackage == null ? "" : defaultPackage), sourceType);
        } catch (IllegalArgumentException ex) {
            return unknown(sourceType);
        }
    }

    public static BehaviorTypeRef unknown(String sourceEvidence) {
        return new BehaviorTypeRef(
                Kind.UNKNOWN, null, Collections.<BehaviorTypeRef>emptyList(),
                null, Variance.EXACT, sourceEvidence);
    }

    private static BehaviorTypeRef fromJavaType(JavaTypeRef type, String sourceEvidence) {
        if (JavaTypeRef.Kind.ARRAY.equals(type.kind())) {
            return new BehaviorTypeRef(
                    Kind.ARRAY, null, Collections.<BehaviorTypeRef>emptyList(),
                    fromJavaType(type.component(), type.component().canonicalName()),
                    Variance.EXACT, sourceEvidence);
        }
        if (JavaTypeRef.Kind.WILDCARD.equals(type.kind())) {
            Variance variance = Variance.UNBOUNDED;
            if ("extends".equals(type.wildcardBoundKind())) variance = Variance.EXTENDS;
            else if ("super".equals(type.wildcardBoundKind())) variance = Variance.SUPER;
            return new BehaviorTypeRef(
                    Kind.WILDCARD, null, Collections.<BehaviorTypeRef>emptyList(),
                    type.component() == null ? null
                            : fromJavaType(type.component(), type.component().canonicalName()),
                    variance, sourceEvidence);
        }
        if (JavaTypeRef.Kind.PRIMITIVE.equals(type.kind())) {
            return new BehaviorTypeRef(
                    Kind.PRIMITIVE, type.declaredName(),
                    Collections.<BehaviorTypeRef>emptyList(), null,
                    Variance.EXACT, sourceEvidence);
        }

        List<BehaviorTypeRef> arguments = new ArrayList<BehaviorTypeRef>();
        for (int i = 0; i < type.arguments().size(); i++) {
            JavaTypeRef argument = type.arguments().get(i);
            arguments.add(fromJavaType(argument, argument.canonicalName()));
        }
        String name = canonicalJvmName(type.declaredName());
        Kind kind = isTypeVariable(name) ? Kind.TYPE_VARIABLE : Kind.DECLARED;
        return new BehaviorTypeRef(
                kind, name, arguments, null, Variance.EXACT, sourceEvidence);
    }

    public Kind kind() {
        return kind;
    }

    public String jvmName() {
        return jvmName;
    }

    public List<BehaviorTypeRef> arguments() {
        return arguments;
    }

    public BehaviorTypeRef component() {
        return component;
    }

    public Variance variance() {
        return variance;
    }

    public String sourceEvidence() {
        return sourceEvidence;
    }

    public boolean isPortable() {
        if (Kind.UNKNOWN.equals(kind)) return false;
        if (component != null && !component.isPortable()) return false;
        for (int i = 0; i < arguments.size(); i++) {
            if (!arguments.get(i).isPortable()) return false;
        }
        return true;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BehaviorTypeRef)) return false;
        BehaviorTypeRef that = (BehaviorTypeRef) other;
        return kind.equals(that.kind)
                && Objects.equals(jvmName, that.jvmName)
                && arguments.equals(that.arguments)
                && Objects.equals(component, that.component)
                && variance.equals(that.variance)
                && (!Kind.UNKNOWN.equals(kind)
                    || sourceEvidence.equals(that.sourceEvidence));
    }

    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + (jvmName == null ? 0 : jvmName.hashCode());
        result = 31 * result + arguments.hashCode();
        result = 31 * result + (component == null ? 0 : component.hashCode());
        result = 31 * result + variance.hashCode();
        if (Kind.UNKNOWN.equals(kind)) result = 31 * result + sourceEvidence.hashCode();
        return result;
    }

    private static String canonicalJvmName(String name) {
        if (name.indexOf('.') >= 0) return name;
        if (isTypeVariable(name)) return name;
        if (javaLang(name)) return "java.lang." + name;
        return name;
    }

    private static boolean isTypeVariable(String name) {
        return name.length() == 1 && Character.isUpperCase(name.charAt(0));
    }

    private static boolean javaLang(String name) {
        return "String".equals(name) || "Object".equals(name) || "Boolean".equals(name)
                || "Byte".equals(name) || "Short".equals(name) || "Integer".equals(name)
                || "Long".equals(name) || "Float".equals(name) || "Double".equals(name)
                || "Character".equals(name) || "Class".equals(name) || "Number".equals(name)
                || "Throwable".equals(name) || "Exception".equals(name)
                || "RuntimeException".equals(name) || "Error".equals(name)
                || "Iterable".equals(name) || "Comparable".equals(name)
                || "CharSequence".equals(name) || "Void".equals(name);
    }
}
