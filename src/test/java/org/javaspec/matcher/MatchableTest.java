package org.javaspec.matcher;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MatchableTest {
    private final MatcherRegistry registry = MatcherRegistry.createWithDefaults();

    @Test
    public void shouldBePassesForSameReference() {
        Object obj = new Object();
        Matchable<Object> matchable = new Matchable<Object>(obj, registry);

        matchable.shouldBe(obj); // same reference, passes
    }

    @Test(expected = AssertionError.class)
    public void shouldBeFailsForDifferentReferences() {
        Matchable<Object> matchable = new Matchable<Object>(new Object(), registry);

        matchable.shouldBe(new Object());
    }

    @Test
    public void shouldEqualPassesForEqualValues() {
        String value = "hello";
        Matchable<String> matchable = new Matchable<String>(value, registry);

        matchable.shouldEqual("hello");
    }

    @Test(expected = AssertionError.class)
    public void shouldEqualFailsForDifferentValues() {
        Matchable<String> matchable = new Matchable<String>("hello", registry);

        matchable.shouldEqual("world");
    }

    @Test
    public void shouldReturnPassesForEqualValuesAndFailsForDifferentValues() {
        new Matchable<String>("hello", registry).shouldReturn("hello");

        assertAssertionFails(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("hello", registry).shouldReturn("world");
            }
        });
    }

    @Test
    public void shouldNotReturnPassesForDifferentValuesAndFailsForEqualValues() {
        new Matchable<String>("hello", registry).shouldNotReturn("world");

        assertAssertionFails(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("hello", registry).shouldNotReturn("hello");
            }
        });
    }

    @Test
    public void shouldContainPassesAndFailsForSupportedSubjects() {
        new Matchable<String>("The Wizard of Oz", registry).shouldContain("Wizard");
        new Matchable<java.util.List<String>>(Arrays.asList("tin", "lion"), registry).shouldContain("lion");

        assertAssertionFails(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("The Wizard of Oz", registry).shouldContain("Dorothy");
            }
        });
    }

    @Test
    public void stringBoundaryAndPatternMatchersPassAndFailAsExpected() {
        new Matchable<String>("The Wizard of Oz", registry).shouldStartWith("The");
        new Matchable<String>("The Wizard of Oz", registry).shouldEndWith("Oz");
        new Matchable<String>("The Wizard of Oz", registry).shouldMatchPattern("Wizard\\s+of");

        assertAssertionFails(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("The Wizard of Oz", registry).shouldStartWith("A");
            }
        });
        assertAssertionFails(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("The Wizard of Oz", registry).shouldEndWith("Kansas");
            }
        });
        assertAssertionFails(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("The Wizard of Oz", registry).shouldMatchPattern("Kansas");
            }
        });
    }

    @Test
    public void shouldNotBePassesForDifferentReferences() {
        Matchable<String> matchable = new Matchable<String>("hello", registry);

        matchable.shouldNotBe("world");
    }

    @Test(expected = AssertionError.class)
    public void shouldNotBeFailsForSameReference() {
        String value = "hello";
        Matchable<String> matchable = new Matchable<String>(value, registry);

        matchable.shouldNotBe(value);
    }

    @Test
    public void shouldHaveTypePassesForExpectedType() {
        Matchable<String> matchable = new Matchable<String>("hello", registry);

        matchable.shouldHaveType(String.class);
    }

    @Test(expected = AssertionError.class)
    public void shouldHaveTypeFailsForWrongType() {
        Matchable<Integer> matchable = new Matchable<Integer>(42, registry);

        matchable.shouldHaveType(String.class);
    }

    @Test(expected = AssertionError.class)
    public void shouldHaveTypeFailsForNullSubject() {
        Matchable<String> matchable = new Matchable<String>(null, registry);

        matchable.shouldHaveType(String.class);
    }

    @Test
    public void shouldMatchUsesCustomMatcher() {
        registry.register("length", new CustomMatcher<Integer>("length", new CustomMatcher.MatchPredicate<Integer>() {
            @Override
            public boolean test(Integer subject, Object... expected) {
                if (expected.length == 0) return false;
                return subject.equals(expected[0]);
            }
        }));

        Matchable<Integer> matchable = new Matchable<Integer>(42, registry);
        matchable.shouldMatch("length", 42);
    }

    @Test(expected = AssertionError.class)
    public void shouldMatchFailsForUnmatchedCustomMatcher() {
        registry.register("length", new CustomMatcher<Integer>("length", new CustomMatcher.MatchPredicate<Integer>() {
            @Override
            public boolean test(Integer subject, Object... expected) {
                if (expected.length == 0) return false;
                return subject.equals(expected[0]);
            }
        }));

        Matchable<Integer> matchable = new Matchable<Integer>(42, registry);
        matchable.shouldMatch("length", 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldMatchFailsForUnknownMatcher() {
        Matchable<Integer> matchable = new Matchable<Integer>(42, registry);
        matchable.shouldMatch("nonexistent");
    }

    @Test
    public void returnsWrappedValue() {
        Matchable<Integer> matchable = new Matchable<Integer>(42, registry);

        assertEquals(42, (int) matchable.value());
    }

    private static void assertAssertionFails(Runnable runnable) {
        try {
            runnable.run();
        } catch (AssertionError expected) {
            return;
        }
        fail("Expected AssertionError");
    }
}
