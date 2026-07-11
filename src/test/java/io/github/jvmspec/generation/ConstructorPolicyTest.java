package io.github.jvmspec.generation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConstructorPolicyTest {
    @Test
    public void defaultPolicyIsComment() {
        assertEquals(ConstructorPolicy.COMMENT, ConstructorPolicy.defaultPolicy());
    }
}
