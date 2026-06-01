package org.javaspec.doubles;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

final class Arguments {
    private Arguments() {
    }

    static Object[] copy(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return new Object[0];
        }
        IdentityHashMap<Object, Object> seenArrays = new IdentityHashMap<Object, Object>();
        Object[] copy = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            copy[i] = copyValue(arguments[i], seenArrays);
        }
        return copy;
    }

    static Object copyValue(Object value) {
        return copyValue(value, new IdentityHashMap<Object, Object>());
    }

    private static Object copyValue(Object value, IdentityHashMap<Object, Object> seenArrays) {
        if (value == null || !value.getClass().isArray()) {
            return value;
        }
        Object existing = seenArrays.get(value);
        if (existing != null) {
            return existing;
        }
        int length = Array.getLength(value);
        Class<?> componentType = value.getClass().getComponentType();
        Object copy = Array.newInstance(componentType, length);
        seenArrays.put(value, copy);
        for (int i = 0; i < length; i++) {
            Array.set(copy, i, copyValue(Array.get(value, i), seenArrays));
        }
        return copy;
    }

    static boolean equalValues(Object left, Object right) {
        return equalValues(left, right, new ArrayList<IdentityPair>());
    }

    private static boolean equalValues(Object left, Object right, List<IdentityPair> seenPairs) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        Class<?> leftType = left.getClass();
        Class<?> rightType = right.getClass();
        if (!leftType.isArray() || !rightType.isArray()) {
            return left.equals(right);
        }
        IdentityPair pair = new IdentityPair(left, right);
        if (seenPairs.contains(pair)) {
            return true;
        }
        seenPairs.add(pair);
        int length = Array.getLength(left);
        if (length != Array.getLength(right)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!equalValues(Array.get(left, i), Array.get(right, i), seenPairs)) {
                return false;
            }
        }
        return true;
    }

    static int hashValues(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return 1;
        }
        IdentityHashMap<Object, Boolean> seenArrays = new IdentityHashMap<Object, Boolean>();
        int result = 1;
        for (int i = 0; i < arguments.length; i++) {
            result = 31 * result + hashValue(arguments[i], seenArrays);
        }
        return result;
    }

    private static int hashValue(Object value, IdentityHashMap<Object, Boolean> seenArrays) {
        if (value == null) {
            return 0;
        }
        if (!value.getClass().isArray()) {
            return value.hashCode();
        }
        if (seenArrays.containsKey(value)) {
            return 0;
        }
        seenArrays.put(value, Boolean.TRUE);
        int result = 1;
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            result = 31 * result + hashValue(Array.get(value, i), seenArrays);
        }
        seenArrays.remove(value);
        return result;
    }

    static String describe(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return "()";
        }
        IdentityHashMap<Object, Boolean> seenArrays = new IdentityHashMap<Object, Boolean>();
        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            appendValue(builder, arguments[i], seenArrays);
        }
        builder.append(')');
        return builder.toString();
    }

    private static void appendValue(StringBuilder builder, Object value, IdentityHashMap<Object, Boolean> seenArrays) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (!value.getClass().isArray()) {
            builder.append(String.valueOf(value));
            return;
        }
        if (seenArrays.containsKey(value)) {
            builder.append("[...]");
            return;
        }
        seenArrays.put(value, Boolean.TRUE);
        builder.append('[');
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            appendValue(builder, Array.get(value, i), seenArrays);
        }
        builder.append(']');
        seenArrays.remove(value);
    }

    private static final class IdentityPair {
        private final Object left;
        private final Object right;

        IdentityPair(Object left, Object right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof IdentityPair)) {
                return false;
            }
            IdentityPair pair = (IdentityPair) other;
            return left == pair.left && right == pair.right;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(left) * 31 + System.identityHashCode(right);
        }
    }
}
