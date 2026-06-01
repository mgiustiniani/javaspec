package org.javaspec.matcher;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public void negatedEqualityAliasesPassAndReportUsefulFailures() {
        new Matchable<String>("alpha", registry).shouldNotBeLike("beta");
        new Matchable<String>("alpha", registry).shouldNotBeEqualTo("gamma");

        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("alpha", registry).shouldNotBeLike("alpha");
            }
        }, "not to equal", "alpha");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("alpha", registry).shouldNotBeEqualTo("alpha");
            }
        }, "not to equal", "alpha");
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
    public void negatedStringBoundaryAndPatternMatchersPassAndReportUsefulFailures() {
        new Matchable<String>("The Wizard of Oz", registry).shouldNotStartWith("A");
        new Matchable<String>("The Wizard of Oz", registry).shouldNotEndWith("Kansas");
        new Matchable<String>("The Wizard of Oz", registry).shouldNotMatchPattern("Kansas");

        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("The Wizard of Oz", registry).shouldNotStartWith("The");
            }
        }, "not to start with", "The");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("The Wizard of Oz", registry).shouldNotEndWith("Oz");
            }
        }, "not to end with", "Oz");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("The Wizard of Oz", registry).shouldNotMatchPattern("Wizard\\s+of");
            }
        }, "not to match pattern", "Wizard\\s+of");
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

    @Test
    public void typeAndImplementationAliasesPassAndReportUsefulFailures() {
        new Matchable<String>("hello", registry).shouldBeAnInstanceOf(String.class);
        new Matchable<String>("hello", registry).shouldReturnAnInstanceOf(CharSequence.class);
        new Matchable<Object>(new java.util.ArrayList<String>(), registry).shouldImplement(List.class);
        new Matchable<Class<?>>(java.util.ArrayList.class, registry).shouldImplement(List.class);

        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Integer>(42, registry).shouldBeAnInstanceOf(String.class);
            }
        }, "Expected an instance of", String.class.getName(), Integer.class.getName());
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Object>("not a list", registry).shouldImplement(List.class);
            }
        }, "to implement", List.class.getName());
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
    public void countAndEmptinessMatchersSupportRepresentativeCountableTypes() {
        Map<String, Integer> populatedMap = new LinkedHashMap<String, Integer>();
        populatedMap.put("ruby", Integer.valueOf(1));
        populatedMap.put("emerald", Integer.valueOf(2));

        new Matchable<int[]>(new int[] {1, 2, 3}, registry).shouldHaveCount(3);
        new Matchable<List<String>>(Arrays.asList("tin", "lion"), registry).shouldHaveCount(2);
        new Matchable<Map<String, Integer>>(populatedMap, registry).shouldHaveCount(2);
        new Matchable<String>("Oz", registry).shouldHaveCount(2);
        new Matchable<Iterable<String>>(iterable("north", "south"), registry).shouldHaveCount(2);

        new Matchable<String[]>(new String[0], registry).shouldBeEmpty();
        new Matchable<List<String>>(Collections.<String>emptyList(), registry).shouldBeEmpty();
        new Matchable<Map<String, Integer>>(Collections.<String, Integer>emptyMap(), registry).shouldBeEmpty();
        new Matchable<String>("", registry).shouldBeEmpty();
        new Matchable<Iterable<String>>(iterable(), registry).shouldBeEmpty();

        new Matchable<String[]>(new String[] {"ruby"}, registry).shouldNotBeEmpty();
        new Matchable<List<String>>(Arrays.asList("ruby"), registry).shouldNotBeEmpty();
        new Matchable<Map<String, Integer>>(populatedMap, registry).shouldNotBeEmpty();
        new Matchable<String>("ruby", registry).shouldNotBeEmpty();
        new Matchable<Iterable<String>>(iterable("ruby"), registry).shouldNotBeEmpty();
    }

    @Test
    public void countAndEmptinessFailuresHaveUsefulMessages() {
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<List<String>>(Arrays.asList("tin", "lion"), registry).shouldHaveCount(3);
            }
        }, "count 3", "got 2");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("ruby", registry).shouldBeEmpty();
            }
        }, "to be empty", "count 4");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("", registry).shouldNotBeEmpty();
            }
        }, "not to be empty");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Object>(new Object(), registry).shouldHaveCount(1);
            }
        }, "countable value");
    }

    @Test
    public void mapKeyAndValueMatchersPassAndReportUsefulFailures() {
        final Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("ruby", Integer.valueOf(1));
        map.put("emerald", Integer.valueOf(2));

        new Matchable<Map<String, Integer>>(map, registry).shouldHaveKey("ruby");
        new Matchable<Map<String, Integer>>(map, registry).shouldNotHaveKey("sapphire");
        new Matchable<Map<String, Integer>>(map, registry).shouldHaveValue(Integer.valueOf(1));
        new Matchable<Map<String, Integer>>(map, registry).shouldNotHaveValue(Integer.valueOf(3));

        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Map<String, Integer>>(map, registry).shouldHaveKey("sapphire");
            }
        }, "to have key", "sapphire");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Map<String, Integer>>(map, registry).shouldNotHaveKey("ruby");
            }
        }, "not to have key", "ruby");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Map<String, Integer>>(map, registry).shouldHaveValue(Integer.valueOf(3));
            }
        }, "to have value", "3");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<Map<String, Integer>>(map, registry).shouldNotHaveValue(Integer.valueOf(1));
            }
        }, "not to have value", "1");
        assertAssertionMessageContains(new Runnable() {
            @Override
            public void run() {
                new Matchable<String>("not a map", registry).shouldHaveKey("ruby");
            }
        }, "Expected a map");
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

    private static Iterable<String> iterable(final String... values) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return Arrays.asList(values).iterator();
            }
        };
    }
}
