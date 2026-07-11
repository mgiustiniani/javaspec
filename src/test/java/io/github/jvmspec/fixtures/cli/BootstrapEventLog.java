package io.github.jvmspec.fixtures.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BootstrapEventLog {
    private static final List<String> EVENTS = new ArrayList<String>();

    private BootstrapEventLog() {
    }

    public static synchronized void reset() {
        EVENTS.clear();
    }

    public static synchronized void record(String event) {
        EVENTS.add(event);
    }

    public static synchronized List<String> events() {
        return Collections.unmodifiableList(new ArrayList<String>(EVENTS));
    }
}
