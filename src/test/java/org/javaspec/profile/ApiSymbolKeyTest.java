package org.javaspec.profile;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ApiSymbolKeyTest {
    @Test
    public void createsOwnerOnlyKeys() {
        ApiSymbolKey key = ApiSymbolKey.of("java.util.ArrayList");

        assertEquals("java.util.ArrayList", key.ownerQualifiedName());
        assertFalse(key.hasMemberName());
        assertEquals(null, key.memberName());
        assertEquals("java.util.ArrayList", key.toString());
    }

    @Test
    public void createsOwnerAndMemberKeys() {
        ApiSymbolKey key = ApiSymbolKey.of("java.util.Collections", "emptyList");

        assertEquals("java.util.Collections", key.ownerQualifiedName());
        assertTrue(key.hasMemberName());
        assertEquals("emptyList", key.memberName());
        assertEquals("java.util.Collections#emptyList", key.toString());
    }

    @Test
    public void equalityAndHashCodeUseOwnerAndMember() {
        ApiSymbolKey first = ApiSymbolKey.of("java.util.Collections", "emptyList");
        ApiSymbolKey same = ApiSymbolKey.of("java.util.Collections", "emptyList");
        ApiSymbolKey differentMember = ApiSymbolKey.of("java.util.Collections", "emptyMap");
        ApiSymbolKey ownerOnly = ApiSymbolKey.of("java.util.Collections");

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertFalse(first.equals(differentMember));
        assertFalse(first.equals(ownerOnly));
        assertFalse(first.equals("java.util.Collections#emptyList"));
    }

    @Test
    public void validatesRequiredOwnerName() {
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbolKey.of(null);
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbolKey.of("");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbolKey.of("   ");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbolKey.of(" java.util.List");
            }
        });
    }

    @Test
    public void validatesOptionalMemberNameWhenPresent() {
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbolKey.of("java.util.List", "");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbolKey.of("java.util.List", "   ");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbolKey.of("java.util.List", " size");
            }
        });
    }
}
