package org.javaspec.profile;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertFalse;

public class TargetProfileTest {
    @Test
    public void orderedProfilesFollowSupportedLtsSequence() {
        assertEquals(
                Arrays.asList(
                        TargetProfile.JAVA8,
                        TargetProfile.JAVA11,
                        TargetProfile.JAVA17,
                        TargetProfile.JAVA21,
                        TargetProfile.JAVA25
                ),
                TargetProfile.orderedProfiles()
        );
    }

    @Test
    public void parsesUsefulProfileNames() {
        assertEquals(TargetProfile.JAVA17, TargetProfile.parse("java17"));
        assertEquals(TargetProfile.JAVA17, TargetProfile.parse("Java 17"));
        assertEquals(TargetProfile.JAVA17, TargetProfile.parse("JDK 17"));
        assertEquals(TargetProfile.JAVA17, TargetProfile.parse("17"));
    }

    @Test
    public void fromKeyAcceptsCanonicalKeys() {
        assertEquals(TargetProfile.JAVA8, TargetProfile.fromKey("java8"));
        assertEquals(TargetProfile.JAVA11, TargetProfile.fromKey("java11"));
        assertEquals(TargetProfile.JAVA17, TargetProfile.fromKey("java17"));
        assertEquals(TargetProfile.JAVA21, TargetProfile.fromKey("java21"));
        assertEquals(TargetProfile.JAVA25, TargetProfile.fromKey("java25"));
    }

    @Test
    public void supportOrderingUsesMajorVersion() {
        assertTrue(TargetProfile.JAVA17.supports(TargetProfile.JAVA11));
        assertTrue(TargetProfile.JAVA17.supports(TargetProfile.JAVA17));
        assertFalse(TargetProfile.JAVA8.supports(TargetProfile.JAVA11));
        assertTrue(TargetProfile.JAVA8.isBefore(TargetProfile.JAVA11));
        assertTrue(TargetProfile.JAVA25.isAfter(TargetProfile.JAVA21));
    }

    @Test
    public void invalidProfileKeysThrowClearExceptions() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                TargetProfile.fromKey("java99");
            }
        });
        assertTrue(unknown.getMessage().contains("Unknown target profile key"));

        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                TargetProfile.fromKey("");
            }
        });
        assertTrue(empty.getMessage().contains("empty"));

        IllegalArgumentException whitespace = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                TargetProfile.fromKey(" java8");
            }
        });
        assertTrue(whitespace.getMessage().contains("whitespace"));

        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                TargetProfile.parse("   ");
            }
        });
        assertTrue(blank.getMessage().contains("empty"));
    }
}
