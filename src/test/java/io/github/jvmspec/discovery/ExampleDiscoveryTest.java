package io.github.jvmspec.discovery;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExampleDiscoveryTest {
    @Test
    public void discoversOnlyBehaviorExamplesInSourceOrderWithMixedLineEndings() {
        String source = "public class SubjectSpec {\r\n"
                + "  public void let() { }\r\n"
                + "  public void it_handles_input() { }\n"
                + "  public void helper() { }\r"
                + "  public void its_result_is_stable() throws Exception { }\r\n"
                + "}\r\n";

        List<SpecExample> examples = ExampleDiscovery.discover(source);

        assertEquals(2, examples.size());
        assertEquals("it_handles_input", examples.get(0).methodName());
        assertEquals(0, examples.get(0).sourceOrderIndex());
        assertEquals(3, examples.get(0).sourceLine());
        assertEquals("its_result_is_stable", examples.get(1).methodName());
        assertEquals(1, examples.get(1).sourceOrderIndex());
        assertEquals(5, examples.get(1).sourceLine());
    }
}
