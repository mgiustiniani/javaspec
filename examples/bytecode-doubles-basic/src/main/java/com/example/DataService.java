package com.example;

/**
 * A service that delegates persistence to a {@link DataStore}.
 */
public class DataService {
    private final DataStore store;

    public DataService(DataStore store) {
        this.store = store;
    }

    /**
     * Saves the item via the store and returns the store's result.
     */
    public boolean save(String item) {
        return store.save(item);
    }

    /**
     * Looks up an item by key, prepending "found:" when present.
     * Returns "not found" when the store returns null.
     */
    public String lookup(String key) {
        String result = store.find(key);
        return result != null ? "found:" + result : "not found";
    }
}
