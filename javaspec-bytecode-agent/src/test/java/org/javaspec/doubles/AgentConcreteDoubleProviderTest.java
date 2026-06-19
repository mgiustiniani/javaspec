package org.javaspec.doubles;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AgentConcreteDoubleProviderTest {

    @Test
    public void concreteDoubleSupportsFinalClassInstanceMethods() {
        InterfaceDouble<FinalGreeter> doubleHandle = Doubles.concreteDouble(FinalGreeter.class);
        doubleHandle.when("greet", "Ada").thenReturn("stubbed Ada");

        assertEquals("stubbed Ada", doubleHandle.instance().greet("Ada"));
        assertEquals("real Grace", new FinalGreeter().greet("Grace"));
        doubleHandle.verifyCalled("greet", "Ada");
    }

    @Test
    public void staticDoubleInterceptsStaticMethodsUntilClosed() {
        assertEquals("real before", StaticUtility.message("before"));
        StaticDouble<StaticUtility> statics = BytecodeAgentDoubles.staticDouble(StaticUtility.class);
        try {
            statics.when("message", "x").thenReturn("stubbed x");

            assertEquals("stubbed x", StaticUtility.message("x"));
            assertEquals(null, StaticUtility.message("y"));
            statics.control().verifyCalled("message", "x");
            statics.control().verifyCalled("message", "y");
        } finally {
            statics.close();
        }
        assertEquals("real after", StaticUtility.message("after"));
    }

    @Test
    public void constructionDoubleRegistersNewInstancesForMethodInterception() {
        ConstructionDouble<ConstructedGreeter> construction =
                BytecodeAgentDoubles.mockConstruction(ConstructedGreeter.class);
        try {
            construction.when("name").thenReturn("stubbed construction");

            ConstructedGreeter first = new ConstructedGreeter();
            ConstructedGreeter second = new ConstructedGreeter();

            assertEquals("stubbed construction", first.name());
            assertEquals("stubbed construction", second.name());
            construction.control().verifyCallCount("name", 2);
        } finally {
            construction.close();
        }
        assertEquals("real", new ConstructedGreeter().name());
    }

    @Test
    public void providerReportsFinalClassesAsSupportedWhenAgentAvailable() {
        AgentConcreteDoubleProvider provider = new AgentConcreteDoubleProvider();
        assertTrue(provider.supports(FinalGreeter.class));
    }

    public static final class FinalGreeter {
        public FinalGreeter() {
        }

        public final String greet(String name) {
            return "real " + name;
        }
    }

    public static final class StaticUtility {
        private StaticUtility() {
        }

        public static String message(String value) {
            return "real " + value;
        }
    }

    public static final class ConstructedGreeter {
        public ConstructedGreeter() {
        }

        public final String name() {
            return "real";
        }
    }
}
