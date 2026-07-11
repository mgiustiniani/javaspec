package com.example;

/**
 * A concrete data store — not an interface.
 * Demonstrates that javaspec-bytecode-doubles can double non-interface classes.
 */
public class DataStore {
    /**
     * Persists an item and returns true on success.
     */
    public boolean save(String item) {
        // real implementation would write to a database
        throw new UnsupportedOperationException("Real DataStore requires a database connection.");
    }

    /**
     * Finds an item by key, or returns null when not found.
     */
    public String find(String key) {
        throw new UnsupportedOperationException("Real DataStore requires a database connection.");
    }
}
