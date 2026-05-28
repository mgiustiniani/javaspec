package org.javaspec.api;

import org.junit.Test;

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
