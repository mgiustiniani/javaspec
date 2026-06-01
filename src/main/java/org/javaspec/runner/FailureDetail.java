package org.javaspec.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable throwable details that can be rendered by a command-line frontend.
 */
public final class FailureDetail {
    private static final List<String> EMPTY_STACK_TRACE = Collections.unmodifiableList(new ArrayList<String>());

    private final String throwableClassName;
    private final String message;
    private final List<String> stackTrace;

    private FailureDetail(String throwableClassName, String message, List<String> stackTrace) {
        this.throwableClassName = throwableClassName;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public static FailureDetail of(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable must not be null");
        return new FailureDetail(
                throwable.getClass().getName(),
                safeMessage(throwable.getMessage()),
                immutableStackTrace(stackTraceOf(throwable))
        );
    }

    public static FailureDetail from(Throwable throwable) {
        return of(throwable);
    }

    public String throwableClassName() {
        return throwableClassName;
    }

    public String className() {
        return throwableClassName;
    }

    public String typeName() {
        return throwableClassName;
    }

    public String message() {
        return message;
    }

    public boolean hasMessage() {
        return message.length() > 0;
    }

    public List<String> stackTrace() {
        return stackTrace;
    }

    public List<String> stackTraceLines() {
        return stackTrace;
    }

    public String summary() {
        if (message.length() == 0) {
            return throwableClassName;
        }
        return throwableClassName + ": " + message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FailureDetail)) {
            return false;
        }
        FailureDetail that = (FailureDetail) other;
        return throwableClassName.equals(that.throwableClassName)
                && message.equals(that.message)
                && stackTrace.equals(that.stackTrace);
    }

    @Override
    public int hashCode() {
        int result = throwableClassName.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + stackTrace.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FailureDetail{" +
                "throwableClassName='" + throwableClassName + '\'' +
                ", message='" + message + '\'' +
                ", stackTrace=" + stackTrace +
                '}';
    }

    private static List<String> stackTraceOf(Throwable throwable) {
        StackTraceElement[] elements = throwable.getStackTrace();
        if (elements == null || elements.length == 0) {
            return EMPTY_STACK_TRACE;
        }
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < elements.length; i++) {
            lines.add(elements[i].toString());
        }
        return lines;
    }

    private static List<String> immutableStackTrace(List<String> stackTrace) {
        Objects.requireNonNull(stackTrace, "stackTrace must not be null");
        if (stackTrace.isEmpty()) {
            return EMPTY_STACK_TRACE;
        }
        List<String> copy = new ArrayList<String>();
        for (int i = 0; i < stackTrace.size(); i++) {
            copy.add(Objects.requireNonNull(stackTrace.get(i), "stackTrace[" + i + "] must not be null"));
        }
        return Collections.unmodifiableList(copy);
    }

    private static String safeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message;
    }
}
