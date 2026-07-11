package io.github.jvmspec.api;

import io.github.jvmspec.doubles.InterfaceDouble;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ObjectBehaviorDoubleConvenienceTest {
    @Test
    public void createsInspectsAndVerifiesDoublesThroughConvenienceMethods() {
        ObjectBehavior<Object> behavior = new ObjectBehavior<Object>();
        Collaborator collaborator = behavior.doubleFor(Collaborator.class);

        behavior.doubleControl(collaborator).returnsFor("find", "found", "key-1");

        assertEquals("found", collaborator.find("key-1"));
        collaborator.accept("key-1", null);

        assertEquals(2, behavior.doubleCalls(collaborator).size());
        assertEquals("find", behavior.doubleCalls(collaborator).get(0).methodName());
        assertEquals(1, behavior.doubleCalls(collaborator, "find", "key-1").size());
        assertEquals(1, behavior.doubleCallCount(collaborator, "accept"));
        assertEquals(1, behavior.doubleCallCount(collaborator, "accept", "key-1", null));
        assertEquals(1, behavior.doubleCallCountFor(collaborator, "accept", "key-1", null));
        assertEquals(2, behavior.inspectDouble(collaborator).calls().size());

        behavior.shouldHaveBeenCalled(collaborator, "find");
        behavior.shouldHaveBeenCalled(collaborator, "find", "key-1");
        behavior.shouldHaveBeenCalledWith(collaborator, "accept", "key-1", null);
        behavior.shouldNotHaveBeenCalled(collaborator, "missing");
        behavior.shouldNotHaveBeenCalled(collaborator, "accept", "other", null);
        behavior.shouldNotHaveBeenCalledWith(collaborator, "find", "missing");
        behavior.shouldHaveBeenCalledTimes(collaborator, "find", 1);
        behavior.shouldHaveBeenCalledTimes(collaborator, "accept", 1, "key-1", null);
    }

    @Test
    public void createsTypedInterfaceDoubleThroughConvenienceHandle() {
        ObjectBehavior<Object> behavior = new ObjectBehavior<Object>();
        InterfaceDouble<Collaborator> doubleHandle = behavior.interfaceDouble(Collaborator.class);

        doubleHandle.when("find", "typed-key").thenReturn("typed-value");

        assertSame(Collaborator.class, doubleHandle.interfaceType());
        assertSame(doubleHandle.instance(), doubleHandle.proxy());
        assertEquals("typed-value", doubleHandle.proxy().find("typed-key"));
        doubleHandle.verifyCalledWith("find", "typed-key");
        behavior.shouldHaveBeenCalledWith(doubleHandle.instance(), "find", "typed-key");
    }

    public interface Collaborator {
        String find(String key);

        void accept(String key, Object value);
    }
}
