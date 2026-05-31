package org.javaspec.profile;

/**
 * Data-structure and modeling category for an API symbol.
 */
public enum ApiSymbolCategory {
    CORE_COLLECTION("core collection"),
    COLLECTION_BASE("collection base"),
    LIST("list"),
    SET("set"),
    MAP("map"),
    QUEUE_DEQUE("queue/deque"),
    CONCURRENT_COLLECTION("concurrent collection"),
    CONCURRENT_RESULT("concurrent result"),
    COLLECTION_FACTORY("collection factory"),
    COLLECTION_WRAPPER("collection wrapper"),
    ARRAY("array"),
    OPTIONAL("optional"),
    ATOMIC_REFERENCE("atomic/reference"),
    REFERENCE("reference"),
    STREAM("stream"),
    COLLECTOR("collector"),
    DATA_FLOW("data-flow"),
    CLEANER("cleaner"),
    SEQUENCED_COLLECTION("sequenced collection"),
    STREAM_GATHERER("stream gatherer"),
    LANGUAGE_MODELING("language modeling"),
    RANDOM_GENERATOR("random generator"),
    HEX_FORMAT("hex format"),
    UTILITY_CONTAINER("utility container");

    private final String displayName;

    ApiSymbolCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
