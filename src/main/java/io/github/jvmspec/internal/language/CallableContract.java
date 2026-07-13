package io.github.jvmspec.internal.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Portable required callable independent of Java source rendering. */
public final class CallableContract {
    public enum InvocationKind { INSTANCE, TYPE }

    private final String name;
    private final BehaviorTypeRef returnType;
    private final List<BehaviorParameter> parameters;
    private final InvocationKind invocationKind;

    private CallableContract(
            String name,
            BehaviorTypeRef returnType,
            List<BehaviorParameter> parameters,
            InvocationKind invocationKind
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.returnType = Objects.requireNonNull(returnType, "returnType must not be null");
        this.parameters = Collections.unmodifiableList(
                new ArrayList<BehaviorParameter>(Objects.requireNonNull(
                        parameters, "parameters must not be null")));
        this.invocationKind = Objects.requireNonNull(
                invocationKind, "invocationKind must not be null");
    }

    public static CallableContract of(
            String name,
            BehaviorTypeRef returnType,
            List<BehaviorParameter> parameters,
            InvocationKind invocationKind
    ) {
        return new CallableContract(name, returnType, parameters, invocationKind);
    }

    public String name() {
        return name;
    }

    public BehaviorTypeRef returnType() {
        return returnType;
    }

    public List<BehaviorParameter> parameters() {
        return parameters;
    }

    public InvocationKind invocationKind() {
        return invocationKind;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CallableContract)) return false;
        CallableContract that = (CallableContract) other;
        return name.equals(that.name)
                && returnType.equals(that.returnType)
                && parameters.equals(that.parameters)
                && invocationKind.equals(that.invocationKind);
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + returnType.hashCode();
        result = 31 * result + parameters.hashCode();
        result = 31 * result + invocationKind.hashCode();
        return result;
    }
}
