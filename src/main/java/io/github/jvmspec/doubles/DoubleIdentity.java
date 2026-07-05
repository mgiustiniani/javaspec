package io.github.jvmspec.doubles;

import java.util.concurrent.atomic.AtomicInteger;

final class DoubleIdentity {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private DoubleIdentity() {
    }

    static int nextId() {
        return NEXT_ID.getAndIncrement();
    }
}
