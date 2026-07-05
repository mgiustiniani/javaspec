package io.github.jvmspec.matcher;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Phase 30 coverage: bounded count/emptiness checks on generic {@link Iterable} subjects
 * (iterables that are not collections or maps).
 * <p>
 * The bounded contract is: {@code shouldBeEmpty()} and {@code shouldNotBeEmpty()} probe
 * emptiness with a single {@code iterator().hasNext()} call, and {@code shouldHaveCount(n)}
 * iterates at most {@code n + 1} elements failing fast with a "more than n elements" message.
 * Infinite iterables therefore never hang these checks; every infinite-iterable test below is
 * guarded with a JUnit timeout so a regression fails instead of hanging the build.
 * </p>
 */
public class MatchableBoundedIterableTest {
    private final MatcherRegistry registry = MatcherRegistry.createWithDefaults();

    @Test(timeout = 5000)
    public void shouldBeEmptyFailsFastOnInfiniteIterable() {
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Iterable<String>>(infiniteIterable(), registry).shouldBeEmpty();
            }
        }, "to be empty", "at least one element");
    }

    @Test(timeout = 5000)
    public void shouldNotBeEmptyPassesOnInfiniteIterableWithoutHanging() {
        new Matchable<Iterable<String>>(infiniteIterable(), registry).shouldNotBeEmpty();
    }

    @Test(timeout = 5000)
    public void shouldHaveCountFailsFastOnInfiniteIterable() {
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Iterable<String>>(infiniteIterable(), registry).shouldHaveCount(3);
            }
        }, "count 3", "more than 3 elements");
    }

    @Test(timeout = 5000)
    public void shouldHaveCountConsumesAtMostExpectedPlusOneElements() {
        CountingIterable<String> counting = new CountingIterable<String>(infiniteIterable());

        assertAssertionMessageContains(matchHaveCount(counting, 3), "more than 3 elements");

        assertTrue("Expected shouldHaveCount(3) to consume at most 4 elements but consumed "
                + counting.nextCalls, counting.nextCalls <= 4);
    }

    @Test(timeout = 5000)
    public void emptinessChecksConsumeAtMostOneProbe() {
        CountingIterable<String> notEmptyProbe = new CountingIterable<String>(infiniteIterable());
        new Matchable<Iterable<String>>(notEmptyProbe, registry).shouldNotBeEmpty();
        assertBoundedEmptinessProbe("shouldNotBeEmpty", notEmptyProbe);

        CountingIterable<String> failingEmptyProbe = new CountingIterable<String>(infiniteIterable());
        assertAssertionFails(matchBeEmpty(failingEmptyProbe));
        assertBoundedEmptinessProbe("shouldBeEmpty", failingEmptyProbe);

        CountingIterable<String> emptyProbe = new CountingIterable<String>(iterable());
        new Matchable<Iterable<String>>(emptyProbe, registry).shouldBeEmpty();
        assertBoundedEmptinessProbe("shouldBeEmpty", emptyProbe);
    }

    @Test
    public void shouldHaveCountPassesForExactFiniteGenericIterable() {
        new Matchable<Iterable<String>>(iterable("north", "south"), registry).shouldHaveCount(2);
        new Matchable<Iterable<String>>(iterable(), registry).shouldHaveCount(0);
    }

    @Test
    public void shouldHaveCountReportsActualCountForUndercountedFiniteGenericIterable() {
        assertAssertionMessageContains(matchHaveCount(iterable("tin", "lion"), 3), "count 3", "got 2");
    }

    @Test
    public void shouldHaveCountFailsFastForOvercountedFiniteGenericIterable() {
        assertAssertionMessageContains(
                matchHaveCount(iterable("tin", "lion", "scarecrow"), 1),
                "count 1", "more than 1 elements");
    }

    @Test
    public void shouldBeEmptyFailureOnFiniteGenericIterableMentionsAtLeastOneElement() {
        assertAssertionMessageContains(matchBeEmpty(iterable("ruby")), "to be empty", "at least one element");
    }

    @Test
    public void nullSubjectStillFailsCountAndEmptinessChecksWithCountableMessage() {
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Object>(null, registry).shouldHaveCount(0);
            }
        }, "Expected a countable value but got null");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Object>(null, registry).shouldBeEmpty();
            }
        }, "Expected a countable value but got null");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Object>(null, registry).shouldNotBeEmpty();
            }
        }, "Expected a countable value but got null");
    }

    private Runnable matchHaveCount(final Iterable<String> subject, final int expectedCount) {
        return new Runnable() {
            @Override
            public void run() {
                new Matchable<Iterable<String>>(subject, registry).shouldHaveCount(expectedCount);
            }
        };
    }

    private Runnable matchBeEmpty(final Iterable<String> subject) {
        return new Runnable() {
            @Override
            public void run() {
                new Matchable<Iterable<String>>(subject, registry).shouldBeEmpty();
            }
        };
    }

    private static void assertBoundedEmptinessProbe(String checkName, CountingIterable<?> counting) {
        assertTrue("Expected " + checkName + " to call hasNext() at most once but called "
                + counting.hasNextCalls + " times", counting.hasNextCalls <= 1);
        assertTrue("Expected " + checkName + " to consume no elements but consumed "
                + counting.nextCalls, counting.nextCalls == 0);
    }

    private static void assertAssertionFails(Runnable runnable) {
        expectAssertion(runnable);
    }

    private static void assertAssertionMessageContains(Runnable runnable, String... fragments) {
        AssertionError error = expectAssertion(runnable);
        String message = String.valueOf(error.getMessage());
        for (int i = 0; i < fragments.length; i++) {
            assertTrue("Expected assertion message to contain '" + fragments[i] + "' but was: " + message,
                    message.contains(fragments[i]));
        }
    }

    private static AssertionError expectAssertion(Runnable runnable) {
        try {
            runnable.run();
        } catch (AssertionError expected) {
            return expected;
        }
        fail("Expected AssertionError");
        return null;
    }

    /**
     * An infinite iterable: its iterator always reports another element and returns a constant.
     * Any check that fully consumes such an iterable would never terminate.
     */
    private static Iterable<String> infiniteIterable() {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public String next() {
                        return "element";
                    }
                };
            }
        };
    }

    /** A finite generic iterable that is intentionally not a {@code Collection}. */
    private static Iterable<String> iterable(final String... values) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return Arrays.asList(values).iterator();
            }
        };
    }

    /** Wraps a delegate iterable and counts {@code hasNext()}/{@code next()} calls across all iterators. */
    private static final class CountingIterable<T> implements Iterable<T> {
        private final Iterable<T> delegate;
        private int hasNextCalls;
        private int nextCalls;

        CountingIterable(Iterable<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<T> iterator() {
            final Iterator<T> iterator = delegate.iterator();
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    hasNextCalls++;
                    return iterator.hasNext();
                }

                @Override
                public T next() {
                    nextCalls++;
                    return iterator.next();
                }
            };
        }
    }
}
