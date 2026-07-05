package io.github.jvmspec.reporting;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable metadata emitted by dependency-free report writers.
 */
public final class ReportMetadata {
    public static final String DEFAULT_SCHEMA_VERSION_PROPERTY = "javaspec.report.schemaVersion";
    public static final String DEFAULT_TOOL_PROPERTY = "javaspec.report.tool";

    private static final String DEFAULT_SCHEMA_VERSION_VALUE = "1";
    private static final String DEFAULT_TOOL_VALUE = "javaspec";
    private static final String UNKNOWN_HOSTNAME = "unknown";

    private final String timestamp;
    private final String hostname;
    private final String timeSeconds;
    private final Map<String, String> properties;

    private ReportMetadata(String timestamp, String hostname, String timeSeconds, Map<String, String> properties) {
        this.timestamp = requireNonBlank(timestamp, "timestamp must not be null or blank");
        this.hostname = requireNonBlank(hostname, "hostname must not be null or blank");
        this.timeSeconds = requireNonNegativeTimeSeconds(timeSeconds);
        this.properties = immutableProperties(properties);
    }

    public static ReportMetadata current() {
        return of(Instant.now().toString(), resolveHostname());
    }

    public static ReportMetadata of(String timestamp, String hostname) {
        return of(timestamp, hostname, 0L);
    }

    public static ReportMetadata of(String timestamp, String hostname, long elapsedMillis) {
        return of(timestamp, hostname, elapsedMillis, defaultProperties());
    }

    public static ReportMetadata of(
            String timestamp,
            String hostname,
            long elapsedMillis,
            Map<String, String> properties
    ) {
        if (elapsedMillis < 0L) {
            throw new IllegalArgumentException("elapsedMillis must not be negative");
        }
        return new ReportMetadata(timestamp, hostname, timeSecondsFromMillis(elapsedMillis), properties);
    }

    public String timestamp() {
        return timestamp;
    }

    public String hostname() {
        return hostname;
    }

    public String timeSeconds() {
        return timeSeconds;
    }

    public Map<String, String> properties() {
        return properties;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReportMetadata)) {
            return false;
        }
        ReportMetadata that = (ReportMetadata) other;
        return timestamp.equals(that.timestamp)
                && hostname.equals(that.hostname)
                && timeSeconds.equals(that.timeSeconds)
                && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, hostname, timeSeconds, properties);
    }

    @Override
    public String toString() {
        return "ReportMetadata{" +
                "timestamp='" + timestamp + '\'' +
                ", hostname='" + hostname + '\'' +
                ", timeSeconds='" + timeSeconds + '\'' +
                ", properties=" + properties +
                '}';
    }

    private static Map<String, String> defaultProperties() {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put(DEFAULT_SCHEMA_VERSION_PROPERTY, DEFAULT_SCHEMA_VERSION_VALUE);
        properties.put(DEFAULT_TOOL_PROPERTY, DEFAULT_TOOL_VALUE);
        return properties;
    }

    private static Map<String, String> immutableProperties(Map<String, String> properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        Map<String, String> copy = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String name = requireNonBlank(entry.getKey(), "property name must not be null or blank");
            String value = Objects.requireNonNull(entry.getValue(), "property value for '" + name + "' must not be null");
            copy.put(name, value);
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireNonNegativeTimeSeconds(String value) {
        String timeSeconds = requireNonBlank(value, "timeSeconds must not be null or blank");
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(timeSeconds);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("timeSeconds must be a non-negative number", exception);
        }
        if (parsed.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("timeSeconds must not be negative");
        }
        return timeSeconds;
    }

    private static String timeSecondsFromMillis(long elapsedMillis) {
        BigDecimal seconds = BigDecimal.valueOf(elapsedMillis).movePointLeft(3).stripTrailingZeros();
        if (seconds.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return seconds.toPlainString();
    }

    private static String resolveHostname() {
        String hostname = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost != null) {
                hostname = localHost.getHostName();
            }
        } catch (Exception exception) {
            hostname = null;
        }
        if (isBlank(hostname)) {
            hostname = environmentValue("HOSTNAME");
        }
        if (isBlank(hostname)) {
            hostname = environmentValue("COMPUTERNAME");
        }
        if (isBlank(hostname)) {
            return UNKNOWN_HOSTNAME;
        }
        return hostname;
    }

    private static String environmentValue(String name) {
        try {
            return System.getenv(name);
        } catch (SecurityException exception) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
