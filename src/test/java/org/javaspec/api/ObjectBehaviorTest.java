package org.javaspec.api;

import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObjectBehaviorTest {
    @Test
    public void lazilyConstructsNoArgSubjectAndShouldHaveTypeValidatesIt() {
        NoArgSubject.constructionCount = 0;
        NoArgBehavior behavior = new NoArgBehavior();

        assertEquals(0, NoArgSubject.constructionCount);

        behavior.shouldHaveType(NoArgSubject.class);

        NoArgSubject subject = behavior.currentSubject();
        assertEquals(1, NoArgSubject.constructionCount);
        assertSame(subject, behavior.currentSubject());
        assertEquals(1, NoArgSubject.constructionCount);
    }

    @Test
    public void shouldHaveTypeFailsWhenLazySubjectHasUnexpectedType() {
        final NoArgBehavior behavior = new NoArgBehavior();

        AssertionError error = (AssertionError) expect(AssertionError.class, new ThrowingCall() {
            @Override
            public void run() {
                behavior.shouldHaveType(String.class);
            }
        });

        assertTrue(error.getMessage().contains("Expected an instance of java.lang.String"));
        assertTrue(error.getMessage().contains(NoArgSubject.class.getName()));
    }

    @Test
    public void beConstructedWithPassesArgumentsAndCanBeOverriddenBeforeSubjectAccess() {
        ArgBehavior behavior = new ArgBehavior();

        behavior.beConstructedWith("first");
        behavior.beConstructedWith("second", 7);

        ArgSubject subject = behavior.currentSubject();
        assertEquals("second", subject.name);
        assertEquals(7, subject.count);
    }

    @Test
    public void beConstructedThroughAndNamedFactoriesUseStaticFactoryMethods() {
        FactoryBehavior throughBehavior = new FactoryBehavior();
        throughBehavior.beConstructedThrough("create", "Wizard");

        assertEquals("created:Wizard", throughBehavior.currentSubject().source);

        FactoryBehavior namedBehavior = new FactoryBehavior();
        namedBehavior.beConstructedNamed("named");

        assertEquals("named", namedBehavior.currentSubject().source);
    }

    @Test
    public void constructionChangesAfterSubjectInstantiationThrowIllegalStateException() {
        final ArgBehavior behavior = new ArgBehavior();
        behavior.beConstructedWith("ready", 1);
        behavior.currentSubject();

        IllegalStateException error = (IllegalStateException) expect(IllegalStateException.class, new ThrowingCall() {
            @Override
            public void run() {
                behavior.beConstructedWith("late", 2);
            }
        });

        assertTrue(error.getMessage().contains("Cannot change subject construction"));
    }

    @Test
    public void constructionFactoryChangesAfterSubjectInstantiationThrowIllegalStateException() {
        final FactoryBehavior behavior = new FactoryBehavior();
        behavior.beConstructedThrough("create", "first");
        behavior.currentSubject();

        expect(IllegalStateException.class, new ThrowingCall() {
            @Override
            public void run() {
                behavior.beConstructedThrough("create", "late");
            }
        });
    }

    @Test
    public void duringInstantiationDetectsConstructorExceptions() {
        ThrowingConstructorBehavior behavior = new ThrowingConstructorBehavior();

        behavior.shouldThrow(IllegalArgumentException.class).duringInstantiation();
    }

    @Test
    public void duringInstantiationDetectsFactoryExceptions() {
        FailingFactoryBehavior behavior = new FailingFactoryBehavior();
        behavior.beConstructedThrough("explode");

        behavior.shouldThrow(UnsupportedOperationException.class).duringInstantiation();
    }

    @Test
    public void directConvenienceAssertionsExposeExpandedMatchers() {
        ObjectBehavior<Object> behavior = new ObjectBehavior<Object>();
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("ruby", Integer.valueOf(1));
        map.put("emerald", Integer.valueOf(2));

        assertConvenienceMethodsAvailable(
                expected("shouldBeEqualTo", "ruby", "ruby"),
                expected("shouldNotBeLike", "ruby", "emerald"),
                expected("shouldNotBeEqualTo", "ruby", "emerald"),
                expected("shouldBeAnInstanceOf", "ruby", String.class),
                expected("shouldReturnAnInstanceOf", "ruby", CharSequence.class),
                expected("shouldImplement", new java.util.ArrayList<String>(), List.class),
                expected("shouldNotStartWith", "emerald", "ruby"),
                expected("shouldNotEndWith", "emerald", "ruby"),
                expected("shouldNotMatchPattern", "emerald", "ruby"),
                expected("shouldHaveCount", new String[] {"ruby", "emerald"}, Integer.valueOf(2)),
                expected("shouldHaveCount", Arrays.asList("ruby", "emerald"), Integer.valueOf(2)),
                expected("shouldHaveCount", map, Integer.valueOf(2)),
                expected("shouldHaveCount", "ruby", Integer.valueOf(4)),
                expected("shouldHaveCount", iterable("north", "south"), Integer.valueOf(2)),
                expected("shouldBeEmpty", new int[0]),
                expected("shouldBeEmpty", Collections.<String>emptyList()),
                expected("shouldBeEmpty", Collections.<String, Integer>emptyMap()),
                expected("shouldBeEmpty", ""),
                expected("shouldBeEmpty", iterable()),
                expected("shouldNotBeEmpty", new int[] {1}),
                expected("shouldNotBeEmpty", Arrays.asList("ruby")),
                expected("shouldNotBeEmpty", map),
                expected("shouldNotBeEmpty", "ruby"),
                expected("shouldNotBeEmpty", iterable("ruby")),
                expected("shouldHaveKey", map, "ruby"),
                expected("shouldNotHaveKey", map, "sapphire"),
                expected("shouldHaveValue", map, Integer.valueOf(1)),
                expected("shouldNotHaveValue", map, Integer.valueOf(3))
        );

        invokeConvenience(behavior, "shouldBeEqualTo", "ruby", "ruby");
        invokeConvenience(behavior, "shouldNotBeLike", "ruby", "emerald");
        invokeConvenience(behavior, "shouldNotBeEqualTo", "ruby", "emerald");
        invokeConvenience(behavior, "shouldBeAnInstanceOf", "ruby", String.class);
        invokeConvenience(behavior, "shouldReturnAnInstanceOf", "ruby", CharSequence.class);
        invokeConvenience(behavior, "shouldImplement", new java.util.ArrayList<String>(), List.class);
        invokeConvenience(behavior, "shouldNotStartWith", "emerald", "ruby");
        invokeConvenience(behavior, "shouldNotEndWith", "emerald", "ruby");
        invokeConvenience(behavior, "shouldNotMatchPattern", "emerald", "ruby");
        invokeConvenience(behavior, "shouldHaveCount", new String[] {"ruby", "emerald"}, Integer.valueOf(2));
        invokeConvenience(behavior, "shouldHaveCount", Arrays.asList("ruby", "emerald"), Integer.valueOf(2));
        invokeConvenience(behavior, "shouldHaveCount", map, Integer.valueOf(2));
        invokeConvenience(behavior, "shouldHaveCount", "ruby", Integer.valueOf(4));
        invokeConvenience(behavior, "shouldHaveCount", iterable("north", "south"), Integer.valueOf(2));
        invokeConvenience(behavior, "shouldBeEmpty", new int[0]);
        invokeConvenience(behavior, "shouldBeEmpty", Collections.<String>emptyList());
        invokeConvenience(behavior, "shouldBeEmpty", Collections.<String, Integer>emptyMap());
        invokeConvenience(behavior, "shouldBeEmpty", "");
        invokeConvenience(behavior, "shouldBeEmpty", iterable());
        invokeConvenience(behavior, "shouldNotBeEmpty", new int[] {1});
        invokeConvenience(behavior, "shouldNotBeEmpty", Arrays.asList("ruby"));
        invokeConvenience(behavior, "shouldNotBeEmpty", map);
        invokeConvenience(behavior, "shouldNotBeEmpty", "ruby");
        invokeConvenience(behavior, "shouldNotBeEmpty", iterable("ruby"));
        invokeConvenience(behavior, "shouldHaveKey", map, "ruby");
        invokeConvenience(behavior, "shouldNotHaveKey", map, "sapphire");
        invokeConvenience(behavior, "shouldHaveValue", map, Integer.valueOf(1));
        invokeConvenience(behavior, "shouldNotHaveValue", map, Integer.valueOf(3));
    }

    @Test
    public void directConvenienceAssertionFailuresPreserveUsefulMessages() {
        ObjectBehavior<Object> behavior = new ObjectBehavior<Object>();
        final Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("ruby", Integer.valueOf(1));

        assertConvenienceAssertionMessage(behavior, "shouldNotBeEqualTo",
                new Object[] {"ruby", "ruby"}, "not to equal", "ruby");
        assertConvenienceAssertionMessage(behavior, "shouldNotStartWith",
                new Object[] {"ruby", "ru"}, "not to start with", "ru");
        assertConvenienceAssertionMessage(behavior, "shouldHaveCount",
                new Object[] {Arrays.asList("ruby"), Integer.valueOf(2)}, "count 2", "got 1");
        assertConvenienceAssertionMessage(behavior, "shouldHaveKey",
                new Object[] {map, "emerald"}, "to have key", "emerald");
    }

    private static Throwable expect(Class<? extends Throwable> expectedType, ThrowingCall call) {
        try {
            call.run();
        } catch (Throwable thrown) {
            if (expectedType.isAssignableFrom(thrown.getClass())) {
                return thrown;
            }
            AssertionError error = new AssertionError("Expected " + expectedType.getName()
                    + " but got " + thrown.getClass().getName());
            error.initCause(thrown);
            throw error;
        }
        fail("Expected " + expectedType.getName() + " to be thrown");
        return null;
    }

    private interface ThrowingCall {
        void run() throws Throwable;
    }

    private static void invokeConvenience(ObjectBehavior<?> behavior, String methodName, Object... args) {
        Method method = directMethod(methodName, args);
        invokeMethod(behavior, method, args);
    }

    private static ExpectedConvenienceMethod expected(String methodName, Object... args) {
        return new ExpectedConvenienceMethod(methodName, args);
    }

    private static void assertConvenienceMethodsAvailable(ExpectedConvenienceMethod... expectedMethods) {
        List<String> missing = new ArrayList<String>();
        for (int i = 0; i < expectedMethods.length; i++) {
            ExpectedConvenienceMethod expectedMethod = expectedMethods[i];
            if (findDirectMethod(expectedMethod.methodName, expectedMethod.args) == null) {
                missing.add(expectedMethod.methodName + argumentTypes(expectedMethod.args));
            }
        }
        assertTrue("Expected ObjectBehavior to expose public direct convenience methods, missing: " + missing,
                missing.isEmpty());
    }

    private static void assertConvenienceAssertionMessage(
            ObjectBehavior<?> behavior,
            String methodName,
            Object[] args,
            String... fragments
    ) {
        Method method = directMethod(methodName, args);
        AssertionError error = expectAssertionFromMethod(behavior, method, args);
        String message = String.valueOf(error.getMessage());
        for (int i = 0; i < fragments.length; i++) {
            assertTrue("Expected assertion message to contain '" + fragments[i] + "' but was: " + message,
                    message.contains(fragments[i]));
        }
    }

    private static AssertionError expectAssertionFromMethod(ObjectBehavior<?> behavior, Method method, Object[] args) {
        try {
            invokeMethod(behavior, method, args);
        } catch (AssertionError expected) {
            return expected;
        }
        fail("Expected ObjectBehavior." + method.getName() + " to throw AssertionError");
        return null;
    }

    private static Method directMethod(String methodName, Object[] args) {
        Method method = findDirectMethod(methodName, args);
        if (method != null) {
            return method;
        }
        fail("Expected ObjectBehavior to expose public direct convenience method "
                + methodName + argumentTypes(args));
        return null;
    }

    private static Method findDirectMethod(String methodName, Object[] args) {
        Method[] methods = ObjectBehavior.class.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(methodName) && argumentsMatch(method, args)) {
                return method;
            }
        }
        return null;
    }

    private static boolean argumentsMatch(Method method, Object[] args) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (!method.isVarArgs()) {
            if (parameterTypes.length != args.length) {
                return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!compatible(parameterTypes[i], args[i])) {
                    return false;
                }
            }
            return true;
        }

        int fixedCount = parameterTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!compatible(parameterTypes[i], args[i])) {
                return false;
            }
        }
        Class<?> componentType = parameterTypes[parameterTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!compatible(componentType, args[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean compatible(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }
        Class<?> argumentType = arg.getClass();
        if (parameterType.isPrimitive()) {
            return wrapperType(parameterType).isAssignableFrom(argumentType);
        }
        return parameterType.isAssignableFrom(argumentType);
    }

    private static void invokeMethod(ObjectBehavior<?> behavior, Method method, Object[] args) {
        try {
            method.invoke(behavior, invocationArguments(method, args));
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof AssertionError) {
                throw (AssertionError) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            AssertionError error = new AssertionError("ObjectBehavior." + method.getName()
                    + " threw an unexpected checked exception");
            error.initCause(cause);
            throw error;
        } catch (Exception ex) {
            AssertionError error = new AssertionError("Could not invoke ObjectBehavior." + method.getName());
            error.initCause(ex);
            throw error;
        }
    }

    private static Object[] invocationArguments(Method method, Object[] args) {
        if (!method.isVarArgs()) {
            return args;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        int fixedCount = parameterTypes.length - 1;
        Object[] invocationArgs = new Object[parameterTypes.length];
        for (int i = 0; i < fixedCount; i++) {
            invocationArgs[i] = args[i];
        }
        Class<?> componentType = parameterTypes[parameterTypes.length - 1].getComponentType();
        Object varargsArray = Array.newInstance(componentType, args.length - fixedCount);
        for (int i = fixedCount; i < args.length; i++) {
            Array.set(varargsArray, i - fixedCount, args[i]);
        }
        invocationArgs[parameterTypes.length - 1] = varargsArray;
        return invocationArgs;
    }

    private static Class<?> wrapperType(Class<?> primitiveType) {
        if (boolean.class.equals(primitiveType)) return Boolean.class;
        if (byte.class.equals(primitiveType)) return Byte.class;
        if (short.class.equals(primitiveType)) return Short.class;
        if (int.class.equals(primitiveType)) return Integer.class;
        if (long.class.equals(primitiveType)) return Long.class;
        if (float.class.equals(primitiveType)) return Float.class;
        if (double.class.equals(primitiveType)) return Double.class;
        if (char.class.equals(primitiveType)) return Character.class;
        if (void.class.equals(primitiveType)) return Void.class;
        return primitiveType;
    }

    private static String argumentTypes(Object[] args) {
        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            Object arg = args[i];
            builder.append(arg == null ? "null" : arg.getClass().getName());
        }
        builder.append(')');
        return builder.toString();
    }

    private static Iterable<String> iterable(final String... values) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return Arrays.asList(values).iterator();
            }
        };
    }

    private static final class ExpectedConvenienceMethod {
        final String methodName;
        final Object[] args;

        ExpectedConvenienceMethod(String methodName, Object[] args) {
            this.methodName = methodName;
            this.args = args;
        }
    }

    public static final class NoArgSubject {
        static int constructionCount;

        public NoArgSubject() {
            constructionCount++;
        }
    }

    private static final class NoArgBehavior extends ObjectBehavior<NoArgSubject> {
        NoArgBehavior() {
            super(NoArgSubject.class);
        }

        NoArgSubject currentSubject() {
            return subject();
        }
    }

    public static final class ArgSubject {
        final String name;
        final int count;

        public ArgSubject(String name) {
            this(name, 0);
        }

        public ArgSubject(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    private static final class ArgBehavior extends ObjectBehavior<ArgSubject> {
        ArgBehavior() {
            super(ArgSubject.class);
        }

        ArgSubject currentSubject() {
            return subject();
        }
    }

    public static final class FactorySubject {
        final String source;

        private FactorySubject(String source) {
            this.source = source;
        }

        public static FactorySubject create(String value) {
            return new FactorySubject("created:" + value);
        }

        public static FactorySubject named() {
            return new FactorySubject("named");
        }
    }

    private static final class FactoryBehavior extends ObjectBehavior<FactorySubject> {
        FactoryBehavior() {
            super(FactorySubject.class);
        }

        FactorySubject currentSubject() {
            return subject();
        }
    }

    public static final class ThrowingConstructorSubject {
        public ThrowingConstructorSubject() {
            throw new IllegalArgumentException("constructor failed");
        }
    }

    private static final class ThrowingConstructorBehavior extends ObjectBehavior<ThrowingConstructorSubject> {
        ThrowingConstructorBehavior() {
            super(ThrowingConstructorSubject.class);
        }
    }

    public static final class FailingFactorySubject {
        private FailingFactorySubject() {
        }

        public static FailingFactorySubject explode() {
            throw new UnsupportedOperationException("factory failed");
        }
    }

    private static final class FailingFactoryBehavior extends ObjectBehavior<FailingFactorySubject> {
        FailingFactoryBehavior() {
            super(FailingFactorySubject.class);
        }
    }
}
