package com.example;

import java.util.concurrent.atomic.AtomicReference;

public class Subject {
    public String runOnVirtualThread() throws InterruptedException {
        AtomicReference<String> result = new AtomicReference<>();
        Thread worker = Thread.startVirtualThread(() -> result.set("done"));
        worker.join();
        return result.get() + ":" + worker.isVirtual() + ":" + worker.isAlive();
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
