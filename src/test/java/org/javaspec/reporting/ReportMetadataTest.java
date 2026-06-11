package org.javaspec.reporting;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ReportMetadataTest {
    private static final String TIMESTAMP = "2026-06-11T12:34:56Z";
    private static final String HOSTNAME = "ci-host";

    @Test
    public void deterministicFactoriesRejectNullOrBlankTimestampAndHostname() {
        assertThrows(NullPointerException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of(null, HOSTNAME);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of("  ", HOSTNAME);
            }
        });
        assertThrows(NullPointerException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of(TIMESTAMP, null);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of(TIMESTAMP, "\t\n");
            }
        });
    }

    @Test
    public void deterministicFactoriesRejectInvalidElapsedMillisAndProperties() {
        assertThrows(IllegalArgumentException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of(TIMESTAMP, HOSTNAME, -1L);
            }
        });
        assertThrows(NullPointerException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of(TIMESTAMP, HOSTNAME, 0L, null);
            }
        });

        final Map<String, String> nullName = new LinkedHashMap<String, String>();
        nullName.put(null, "value");
        assertThrows(NullPointerException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of(TIMESTAMP, HOSTNAME, 0L, nullName);
            }
        });

        final Map<String, String> blankName = new LinkedHashMap<String, String>();
        blankName.put(" \t ", "value");
        assertThrows(IllegalArgumentException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of(TIMESTAMP, HOSTNAME, 0L, blankName);
            }
        });

        final Map<String, String> nullValue = new LinkedHashMap<String, String>();
        nullValue.put("name", null);
        assertThrows(NullPointerException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                ReportMetadata.of(TIMESTAMP, HOSTNAME, 0L, nullValue);
            }
        });
    }

    @Test
    public void deterministicFactoriesPreservePropertyOrderDefensivelyCopyAndReturnUnmodifiableProperties() {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("first", "one");
        properties.put("second", "two");

        ReportMetadata metadata = ReportMetadata.of(TIMESTAMP, HOSTNAME, 2500L, properties);
        properties.put("first", "changed");
        properties.put("third", "three");

        assertEquals(TIMESTAMP, metadata.timestamp());
        assertEquals(HOSTNAME, metadata.hostname());
        assertEquals("2.5", metadata.timeSeconds());
        assertEquals(Arrays.asList("first", "second"), new ArrayList<String>(metadata.properties().keySet()));
        assertEquals("one", metadata.properties().get("first"));
        assertEquals("two", metadata.properties().get("second"));
        assertEquals(2, metadata.properties().size());

        assertThrows(UnsupportedOperationException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                metadata.properties().put("third", "three");
            }
        });
    }

    @Test
    public void deterministicFactoryUsesDefaultPropertiesInDocumentedOrder() {
        ReportMetadata metadata = ReportMetadata.of(TIMESTAMP, HOSTNAME);

        assertEquals(TIMESTAMP, metadata.timestamp());
        assertEquals(HOSTNAME, metadata.hostname());
        assertEquals("0", metadata.timeSeconds());
        assertDefaultProperties(metadata.properties());
    }

    @Test
    public void currentReturnsNonBlankTimestampHostnameZeroTimeAndDefaultProperties() {
        ReportMetadata metadata = ReportMetadata.current();

        assertNonBlank(metadata.timestamp());
        assertNonBlank(metadata.hostname());
        assertEquals("0", metadata.timeSeconds());
        assertDefaultProperties(metadata.properties());
    }

    private static void assertDefaultProperties(Map<String, String> properties) {
        assertEquals(
                Arrays.asList(ReportMetadata.DEFAULT_SCHEMA_VERSION_PROPERTY, ReportMetadata.DEFAULT_TOOL_PROPERTY),
                new ArrayList<String>(properties.keySet())
        );
        assertEquals("1", properties.get(ReportMetadata.DEFAULT_SCHEMA_VERSION_PROPERTY));
        assertEquals("javaspec", properties.get(ReportMetadata.DEFAULT_TOOL_PROPERTY));
    }

    private static void assertNonBlank(String value) {
        assertTrue("Expected a non-blank value", value != null && value.trim().length() > 0);
    }
}
