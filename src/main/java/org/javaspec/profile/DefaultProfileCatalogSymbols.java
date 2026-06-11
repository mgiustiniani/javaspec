package org.javaspec.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default LTS profile symbol metadata.
 */
final class DefaultProfileCatalogSymbols {
    private DefaultProfileCatalogSymbols() {
    }

    static List<ApiSymbol> create() {
        List<ApiSymbol> symbols = new ArrayList<ApiSymbol>();
        java8Symbols(symbols);
        java11Symbols(symbols);
        java17Symbols(symbols);
        java21Symbols(symbols);
        java25Symbols(symbols);
        return Collections.unmodifiableList(symbols);
    }

    private static void java8Symbols(List<ApiSymbol> symbols) {
        type(symbols, "java.lang.Iterable", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Root traversal abstraction.");
        type(symbols, "java.util.Collection", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Root collection abstraction.");
        type(symbols, "java.util.List", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "Ordered collection abstraction.");
        type(symbols, "java.util.Set", ApiSymbolCategory.SET, TargetProfile.JAVA8, "Unique element collection abstraction.");
        type(symbols, "java.util.SortedSet", ApiSymbolCategory.SET, TargetProfile.JAVA8, "Sorted set abstraction.");
        type(symbols, "java.util.NavigableSet", ApiSymbolCategory.SET, TargetProfile.JAVA8, "Navigable sorted set abstraction.");
        type(symbols, "java.util.Queue", ApiSymbolCategory.QUEUE_DEQUE, TargetProfile.JAVA8, "Queue abstraction.");
        type(symbols, "java.util.Deque", ApiSymbolCategory.QUEUE_DEQUE, TargetProfile.JAVA8, "Double-ended queue abstraction.");
        type(symbols, "java.util.concurrent.BlockingQueue", ApiSymbolCategory.QUEUE_DEQUE, TargetProfile.JAVA8, "Blocking queue abstraction.");
        type(symbols, "java.util.concurrent.TransferQueue", ApiSymbolCategory.QUEUE_DEQUE, TargetProfile.JAVA8, "Producer handoff queue abstraction.");
        type(symbols, "java.util.concurrent.BlockingDeque", ApiSymbolCategory.QUEUE_DEQUE, TargetProfile.JAVA8, "Blocking deque abstraction.");
        type(symbols, "java.util.Map", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Key/value map abstraction.");
        nestedType(symbols, "java.util.Map.Entry", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Map entry abstraction.");
        type(symbols, "java.util.SortedMap", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Sorted map abstraction.");
        type(symbols, "java.util.NavigableMap", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Navigable sorted map abstraction.");
        type(symbols, "java.util.concurrent.ConcurrentMap", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Concurrent map abstraction.");
        type(symbols, "java.util.concurrent.ConcurrentNavigableMap", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Concurrent navigable map abstraction.");
        type(symbols, "java.util.Iterator", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Iterator traversal abstraction.");
        type(symbols, "java.util.ListIterator", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "Bidirectional list iterator.");
        type(symbols, "java.util.Enumeration", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Legacy enumeration traversal abstraction.");
        type(symbols, "java.util.PrimitiveIterator", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Primitive iterator abstraction.");
        nestedType(symbols, "java.util.PrimitiveIterator.OfInt", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Primitive int iterator.");
        nestedType(symbols, "java.util.PrimitiveIterator.OfLong", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Primitive long iterator.");
        nestedType(symbols, "java.util.PrimitiveIterator.OfDouble", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Primitive double iterator.");
        type(symbols, "java.util.Spliterator", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Splittable traversal abstraction.");
        nestedType(symbols, "java.util.Spliterator.OfPrimitive", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Primitive spliterator abstraction.");
        nestedType(symbols, "java.util.Spliterator.OfInt", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Primitive int spliterator.");
        nestedType(symbols, "java.util.Spliterator.OfLong", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Primitive long spliterator.");
        nestedType(symbols, "java.util.Spliterator.OfDouble", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Primitive double spliterator.");
        type(symbols, "java.util.Spliterators", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Spliterator utility factory class.");
        nestedType(symbols, "java.util.Spliterators.AbstractSpliterator", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Base spliterator implementation.");
        nestedType(symbols, "java.util.Spliterators.AbstractIntSpliterator", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Base int spliterator implementation.");
        nestedType(symbols, "java.util.Spliterators.AbstractLongSpliterator", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Base long spliterator implementation.");
        nestedType(symbols, "java.util.Spliterators.AbstractDoubleSpliterator", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Base double spliterator implementation.");
        type(symbols, "java.util.RandomAccess", ApiSymbolCategory.CORE_COLLECTION, TargetProfile.JAVA8, "Marker for random-access lists.");

        type(symbols, "java.util.AbstractCollection", ApiSymbolCategory.COLLECTION_BASE, TargetProfile.JAVA8, "Base collection implementation.");
        type(symbols, "java.util.AbstractList", ApiSymbolCategory.COLLECTION_BASE, TargetProfile.JAVA8, "Base list implementation.");
        type(symbols, "java.util.AbstractSequentialList", ApiSymbolCategory.COLLECTION_BASE, TargetProfile.JAVA8, "Base sequential list implementation.");
        type(symbols, "java.util.AbstractSet", ApiSymbolCategory.COLLECTION_BASE, TargetProfile.JAVA8, "Base set implementation.");
        type(symbols, "java.util.AbstractQueue", ApiSymbolCategory.COLLECTION_BASE, TargetProfile.JAVA8, "Base queue implementation.");
        type(symbols, "java.util.AbstractMap", ApiSymbolCategory.COLLECTION_BASE, TargetProfile.JAVA8, "Base map implementation.");
        nestedType(symbols, "java.util.AbstractMap.SimpleEntry", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Mutable simple map entry.");
        nestedType(symbols, "java.util.AbstractMap.SimpleImmutableEntry", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Immutable simple map entry.");
        type(symbols, "java.util.Dictionary", ApiSymbolCategory.COLLECTION_BASE, TargetProfile.JAVA8, "Legacy key/value base type.");

        type(symbols, "java.util.ArrayList", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "Resizable array list.");
        type(symbols, "java.util.LinkedList", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "Linked list and deque implementation.");
        type(symbols, "java.util.Vector", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "Legacy synchronized list.");
        type(symbols, "java.util.Stack", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "Legacy stack container.");
        type(symbols, "java.util.concurrent.CopyOnWriteArrayList", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Copy-on-write concurrent list.");
        type(symbols, "java.util.HashSet", ApiSymbolCategory.SET, TargetProfile.JAVA8, "Hash set implementation.");
        type(symbols, "java.util.LinkedHashSet", ApiSymbolCategory.SET, TargetProfile.JAVA8, "Encounter-order hash set implementation.");
        type(symbols, "java.util.TreeSet", ApiSymbolCategory.SET, TargetProfile.JAVA8, "Tree-backed sorted set implementation.");
        type(symbols, "java.util.EnumSet", ApiSymbolCategory.SET, TargetProfile.JAVA8, "Enum-specialized set implementation.");
        type(symbols, "java.util.concurrent.CopyOnWriteArraySet", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Copy-on-write concurrent set.");
        type(symbols, "java.util.concurrent.ConcurrentSkipListSet", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Concurrent sorted set.");

        type(symbols, "java.util.PriorityQueue", ApiSymbolCategory.QUEUE_DEQUE, TargetProfile.JAVA8, "Priority queue implementation.");
        type(symbols, "java.util.ArrayDeque", ApiSymbolCategory.QUEUE_DEQUE, TargetProfile.JAVA8, "Array-backed deque implementation.");
        type(symbols, "java.util.concurrent.ConcurrentLinkedQueue", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Lock-free concurrent queue.");
        type(symbols, "java.util.concurrent.ConcurrentLinkedDeque", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Lock-free concurrent deque.");
        type(symbols, "java.util.concurrent.ArrayBlockingQueue", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Bounded blocking queue.");
        type(symbols, "java.util.concurrent.LinkedBlockingQueue", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Linked blocking queue.");
        type(symbols, "java.util.concurrent.LinkedBlockingDeque", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Linked blocking deque.");
        type(symbols, "java.util.concurrent.PriorityBlockingQueue", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Priority blocking queue.");
        type(symbols, "java.util.concurrent.DelayQueue", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Delayed element blocking queue.");
        type(symbols, "java.util.concurrent.SynchronousQueue", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Rendezvous blocking queue.");
        type(symbols, "java.util.concurrent.LinkedTransferQueue", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Linked transfer queue.");

        type(symbols, "java.util.HashMap", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Hash map implementation.");
        type(symbols, "java.util.LinkedHashMap", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Encounter-order hash map implementation.");
        type(symbols, "java.util.TreeMap", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Tree-backed sorted map implementation.");
        type(symbols, "java.util.Hashtable", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Legacy synchronized map.");
        type(symbols, "java.util.WeakHashMap", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Weak-key hash map.");
        type(symbols, "java.util.IdentityHashMap", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Identity-based hash map.");
        type(symbols, "java.util.EnumMap", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "Enum-specialized map.");
        type(symbols, "java.util.concurrent.ConcurrentHashMap", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Concurrent hash map.");
        nestedType(symbols, "java.util.concurrent.ConcurrentHashMap.KeySetView", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Concurrent hash map key set view.");
        type(symbols, "java.util.concurrent.ConcurrentSkipListMap", ApiSymbolCategory.CONCURRENT_COLLECTION, TargetProfile.JAVA8, "Concurrent sorted map.");
        type(symbols, "java.util.Properties", ApiSymbolCategory.MAP, TargetProfile.JAVA8, "String-oriented property map.");

        type(symbols, "java.util.concurrent.Future", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Asynchronous result holder.");
        type(symbols, "java.util.concurrent.RunnableFuture", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Runnable future result holder.");
        type(symbols, "java.util.concurrent.ScheduledFuture", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Scheduled future result holder.");
        type(symbols, "java.util.concurrent.RunnableScheduledFuture", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Runnable scheduled future result holder.");
        type(symbols, "java.util.concurrent.FutureTask", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Future implementation.");
        type(symbols, "java.util.concurrent.CompletableFuture", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Completable asynchronous result holder.");
        type(symbols, "java.util.concurrent.CompletionStage", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Completion pipeline abstraction.");
        type(symbols, "java.util.concurrent.CompletionService", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Completed task result service.");
        type(symbols, "java.util.concurrent.ExecutorCompletionService", ApiSymbolCategory.CONCURRENT_RESULT, TargetProfile.JAVA8, "Executor-backed completion service.");

        type(symbols, "java.util.BitSet", ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "Mutable bit container.");
        type(symbols, "java.util.IntSummaryStatistics", ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "Mutable int summary container.");
        type(symbols, "java.util.LongSummaryStatistics", ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "Mutable long summary container.");
        type(symbols, "java.util.DoubleSummaryStatistics", ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "Mutable double summary container.");
        type(symbols, "java.util.StringJoiner", ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "Mutable string joining container.");

        staticMethod(symbols, "java.util.Collections", "unmodifiableCollection", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Unmodifiable collection wrapper.");
        staticMethod(symbols, "java.util.Collections", "unmodifiableList", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Unmodifiable list wrapper.");
        staticMethod(symbols, "java.util.Collections", "unmodifiableSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Unmodifiable set wrapper.");
        staticMethod(symbols, "java.util.Collections", "unmodifiableSortedSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Unmodifiable sorted set wrapper.");
        staticMethod(symbols, "java.util.Collections", "unmodifiableNavigableSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Unmodifiable navigable set wrapper.");
        staticMethod(symbols, "java.util.Collections", "unmodifiableMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Unmodifiable map wrapper.");
        staticMethod(symbols, "java.util.Collections", "unmodifiableSortedMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Unmodifiable sorted map wrapper.");
        staticMethod(symbols, "java.util.Collections", "unmodifiableNavigableMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Unmodifiable navigable map wrapper.");
        staticMethod(symbols, "java.util.Collections", "synchronizedCollection", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Synchronized collection wrapper.");
        staticMethod(symbols, "java.util.Collections", "synchronizedList", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Synchronized list wrapper.");
        staticMethod(symbols, "java.util.Collections", "synchronizedSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Synchronized set wrapper.");
        staticMethod(symbols, "java.util.Collections", "synchronizedSortedSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Synchronized sorted set wrapper.");
        staticMethod(symbols, "java.util.Collections", "synchronizedNavigableSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Synchronized navigable set wrapper.");
        staticMethod(symbols, "java.util.Collections", "synchronizedMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Synchronized map wrapper.");
        staticMethod(symbols, "java.util.Collections", "synchronizedSortedMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Synchronized sorted map wrapper.");
        staticMethod(symbols, "java.util.Collections", "synchronizedNavigableMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Synchronized navigable map wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedCollection", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked collection wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedList", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked list wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked set wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedSortedSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked sorted set wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedNavigableSet", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked navigable set wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedQueue", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked queue wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked map wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedSortedMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked sorted map wrapper.");
        staticMethod(symbols, "java.util.Collections", "checkedNavigableMap", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Checked navigable map wrapper.");
        staticMethod(symbols, "java.util.Collections", "emptyList", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Empty list factory.");
        staticMethod(symbols, "java.util.Collections", "emptySet", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Empty set factory.");
        staticMethod(symbols, "java.util.Collections", "emptyMap", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Empty map factory.");
        staticMethod(symbols, "java.util.Collections", "emptyIterator", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Empty iterator factory.");
        staticMethod(symbols, "java.util.Collections", "emptyListIterator", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Empty list iterator factory.");
        staticMethod(symbols, "java.util.Collections", "emptyEnumeration", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Empty enumeration factory.");
        staticMethod(symbols, "java.util.Collections", "singleton", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Singleton set factory.");
        staticMethod(symbols, "java.util.Collections", "singletonList", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Singleton list factory.");
        staticMethod(symbols, "java.util.Collections", "singletonMap", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Singleton map factory.");
        staticMethod(symbols, "java.util.Collections", "nCopies", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Repeated immutable list factory.");
        staticMethod(symbols, "java.util.Collections", "asLifoQueue", ApiSymbolCategory.COLLECTION_WRAPPER, TargetProfile.JAVA8, "Deque-to-LIFO queue adapter.");
        staticMethod(symbols, "java.util.Collections", "newSetFromMap", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Set view backed by a map.");
        staticMethod(symbols, "java.util.Collections", "enumeration", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "Enumeration view over a collection.");
        staticMethod(symbols, "java.util.Collections", "list", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8, "List view over an enumeration.");

        arrayType(symbols, "boolean[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Primitive boolean array.");
        arrayType(symbols, "byte[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Primitive byte array.");
        arrayType(symbols, "short[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Primitive short array.");
        arrayType(symbols, "char[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Primitive char array.");
        arrayType(symbols, "int[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Primitive int array.");
        arrayType(symbols, "long[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Primitive long array.");
        arrayType(symbols, "float[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Primitive float array.");
        arrayType(symbols, "double[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Primitive double array.");
        arrayType(symbols, "T[]", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Object array.");
        staticMethod(symbols, "java.util.Arrays", "asList", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Fixed-size list view over an array.");
        staticMethod(symbols, "java.util.Arrays", "copyOf", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Array copy utility.");
        staticMethod(symbols, "java.util.Arrays", "copyOfRange", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Array range copy utility.");
        staticMethod(symbols, "java.util.Arrays", "sort", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Array sorting utility.");
        staticMethod(symbols, "java.util.Arrays", "parallelSort", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Parallel array sorting utility.");
        staticMethod(symbols, "java.util.Arrays", "binarySearch", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Array binary search utility.");
        staticMethod(symbols, "java.util.Arrays", "equals", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Whole-array equality utility.");
        staticMethod(symbols, "java.util.Arrays", "deepEquals", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Deep object-array equality utility.");
        staticMethod(symbols, "java.util.Arrays", "fill", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Array fill utility.");
        staticMethod(symbols, "java.util.Arrays", "stream", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Array-to-stream bridge.");
        staticMethod(symbols, "java.util.Arrays", "spliterator", ApiSymbolCategory.ARRAY, TargetProfile.JAVA8, "Array spliterator factory.");

        type(symbols, "java.util.Optional", ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA8, "Reference optional container.");
        type(symbols, "java.util.OptionalInt", ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA8, "Primitive int optional container.");
        type(symbols, "java.util.OptionalLong", ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA8, "Primitive long optional container.");
        type(symbols, "java.util.OptionalDouble", ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA8, "Primitive double optional container.");

        type(symbols, "java.util.concurrent.atomic.AtomicBoolean", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic boolean container.");
        type(symbols, "java.util.concurrent.atomic.AtomicInteger", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic integer container.");
        type(symbols, "java.util.concurrent.atomic.AtomicLong", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic long container.");
        type(symbols, "java.util.concurrent.atomic.AtomicReference", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic reference container.");
        type(symbols, "java.util.concurrent.atomic.AtomicIntegerArray", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic integer array container.");
        type(symbols, "java.util.concurrent.atomic.AtomicLongArray", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic long array container.");
        type(symbols, "java.util.concurrent.atomic.AtomicReferenceArray", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic reference array container.");
        type(symbols, "java.util.concurrent.atomic.AtomicIntegerFieldUpdater", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic integer field updater.");
        type(symbols, "java.util.concurrent.atomic.AtomicLongFieldUpdater", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic long field updater.");
        type(symbols, "java.util.concurrent.atomic.AtomicReferenceFieldUpdater", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic reference field updater.");
        type(symbols, "java.util.concurrent.atomic.AtomicStampedReference", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic stamped reference container.");
        type(symbols, "java.util.concurrent.atomic.AtomicMarkableReference", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "Atomic markable reference container.");
        type(symbols, "java.util.concurrent.atomic.LongAdder", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "High-throughput long adder container.");
        type(symbols, "java.util.concurrent.atomic.LongAccumulator", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "High-throughput long accumulator container.");
        type(symbols, "java.util.concurrent.atomic.DoubleAdder", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "High-throughput double adder container.");
        type(symbols, "java.util.concurrent.atomic.DoubleAccumulator", ApiSymbolCategory.ATOMIC_REFERENCE, TargetProfile.JAVA8, "High-throughput double accumulator container.");
        type(symbols, "java.lang.ref.ReferenceQueue", ApiSymbolCategory.REFERENCE, TargetProfile.JAVA8, "Reference lifecycle queue.");
        type(symbols, "java.lang.ref.Reference", ApiSymbolCategory.REFERENCE, TargetProfile.JAVA8, "Base reference type.");
        type(symbols, "java.lang.ref.SoftReference", ApiSymbolCategory.REFERENCE, TargetProfile.JAVA8, "Soft reference type.");
        type(symbols, "java.lang.ref.WeakReference", ApiSymbolCategory.REFERENCE, TargetProfile.JAVA8, "Weak reference type.");
        type(symbols, "java.lang.ref.PhantomReference", ApiSymbolCategory.REFERENCE, TargetProfile.JAVA8, "Phantom reference type.");

        type(symbols, "java.util.stream.BaseStream", ApiSymbolCategory.STREAM, TargetProfile.JAVA8, "Base stream abstraction.");
        type(symbols, "java.util.stream.Stream", ApiSymbolCategory.STREAM, TargetProfile.JAVA8, "Reference stream pipeline.");
        type(symbols, "java.util.stream.IntStream", ApiSymbolCategory.STREAM, TargetProfile.JAVA8, "Primitive int stream pipeline.");
        type(symbols, "java.util.stream.LongStream", ApiSymbolCategory.STREAM, TargetProfile.JAVA8, "Primitive long stream pipeline.");
        type(symbols, "java.util.stream.DoubleStream", ApiSymbolCategory.STREAM, TargetProfile.JAVA8, "Primitive double stream pipeline.");
        type(symbols, "java.util.stream.StreamSupport", ApiSymbolCategory.STREAM, TargetProfile.JAVA8, "Stream factory support class.");
        type(symbols, "java.util.stream.Collector", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Mutable reduction collector abstraction.");
        nestedType(symbols, "java.util.stream.Collector.Characteristics", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Collector characteristic flags.");
        type(symbols, "java.util.stream.Collectors", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Collector factory class.");
        staticMethod(symbols, "java.util.stream.Collectors", "toList", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Collector producing a list result with unspecified mutability.");
        staticMethod(symbols, "java.util.stream.Collectors", "toSet", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Collector producing a set result.");
        staticMethod(symbols, "java.util.stream.Collectors", "toMap", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Collector producing a map result.");
        staticMethod(symbols, "java.util.stream.Collectors", "groupingBy", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Grouping collector family.");
        staticMethod(symbols, "java.util.stream.Collectors", "groupingByConcurrent", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Concurrent grouping collector family.");
        staticMethod(symbols, "java.util.stream.Collectors", "partitioningBy", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Partitioning collector family.");
        staticMethod(symbols, "java.util.stream.Collectors", "joining", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "String joining collector family.");
        staticMethod(symbols, "java.util.stream.Collectors", "mapping", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Mapping downstream collector.");
        staticMethod(symbols, "java.util.stream.Collectors", "reducing", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Reducing collector family.");
        staticMethod(symbols, "java.util.stream.Collectors", "summarizingInt", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Int summary statistics collector.");
        staticMethod(symbols, "java.util.stream.Collectors", "summarizingLong", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Long summary statistics collector.");
        staticMethod(symbols, "java.util.stream.Collectors", "summarizingDouble", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Double summary statistics collector.");
        staticMethod(symbols, "java.util.stream.Collectors", "collectingAndThen", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA8, "Finisher-composing collector.");
    }

    private static void java11Symbols(List<ApiSymbol> symbols) {
        staticMethod(symbols, "java.util.List", "of", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "Unmodifiable value-based list factory family.");
        staticMethod(symbols, "java.util.Set", "of", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "Unmodifiable value-based set factory family.");
        staticMethod(symbols, "java.util.Map", "of", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "Unmodifiable value-based map factory family.");
        staticMethod(symbols, "java.util.Map", "ofEntries", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "Unmodifiable map factory from entries.");
        staticMethod(symbols, "java.util.Map", "entry", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "Unmodifiable map entry factory.");
        staticMethod(symbols, "java.util.List", "copyOf", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "Unmodifiable list copy factory.");
        staticMethod(symbols, "java.util.Set", "copyOf", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "Unmodifiable set copy factory.");
        staticMethod(symbols, "java.util.Map", "copyOf", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "Unmodifiable map copy factory.");
        staticMethod(symbols, "java.util.stream.Collectors", "toUnmodifiableList", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA11, "Collector producing an unmodifiable list result.");
        staticMethod(symbols, "java.util.stream.Collectors", "toUnmodifiableSet", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA11, "Collector producing an unmodifiable set result.");
        staticMethod(symbols, "java.util.stream.Collectors", "toUnmodifiableMap", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA11, "Collector producing an unmodifiable map result.");

        method(symbols, "java.util.Optional", "stream", ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA11, "Optional-to-stream bridge.");
        method(symbols, "java.util.OptionalInt", "stream", ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA11, "Primitive optional-to-stream bridge.");
        method(symbols, "java.util.OptionalLong", "stream", ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA11, "Primitive optional-to-stream bridge.");
        method(symbols, "java.util.OptionalDouble", "stream", ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA11, "Primitive optional-to-stream bridge.");
        staticMethod(symbols, "java.util.stream.Stream", "ofNullable", ApiSymbolCategory.STREAM, TargetProfile.JAVA11, "Nullable reference stream factory.");
        method(symbols, "java.util.stream.Stream", "takeWhile", ApiSymbolCategory.STREAM, TargetProfile.JAVA11, "Prefix-taking stream operation.");
        method(symbols, "java.util.stream.Stream", "dropWhile", ApiSymbolCategory.STREAM, TargetProfile.JAVA11, "Prefix-dropping stream operation.");
        staticMethod(symbols, "java.util.stream.Stream", "iterate", ApiSymbolCategory.STREAM, TargetProfile.JAVA11, "Predicate-bounded stream iteration overloads.");

        staticMethod(symbols, "java.util.Arrays", "compare", ApiSymbolCategory.ARRAY, TargetProfile.JAVA11, "Array lexicographic comparison overloads.");
        staticMethod(symbols, "java.util.Arrays", "compareUnsigned", ApiSymbolCategory.ARRAY, TargetProfile.JAVA11, "Unsigned array lexicographic comparison overloads.");
        staticMethod(symbols, "java.util.Arrays", "mismatch", ApiSymbolCategory.ARRAY, TargetProfile.JAVA11, "Array mismatch-location overloads.");
        staticMethod(symbols, "java.util.Arrays", "equals", ApiSymbolCategory.ARRAY, TargetProfile.JAVA11, "Range equality overloads.");

        type(symbols, "java.lang.ref.Cleaner", ApiSymbolCategory.CLEANER, TargetProfile.JAVA11, "Reference cleanup support type.");
        nestedType(symbols, "java.lang.ref.Cleaner.Cleanable", ApiSymbolCategory.CLEANER, TargetProfile.JAVA11, "Registered cleanup action handle.");
        staticMethod(symbols, "java.nio.file.Files", "readString", ApiSymbolCategory.FILE_IO, TargetProfile.JAVA11, "Read an entire file into a string.");
        staticMethod(symbols, "java.nio.file.Files", "writeString", ApiSymbolCategory.FILE_IO, TargetProfile.JAVA11, "Write a character sequence to a file.");
        type(symbols, "java.net.http.HttpClient", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP client type.");
        nestedType(symbols, "java.net.http.HttpClient.Builder", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP client builder.");
        nestedType(symbols, "java.net.http.HttpClient.Redirect", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP redirect policy enum.");
        nestedType(symbols, "java.net.http.HttpClient.Version", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP protocol version enum.");
        staticMethod(symbols, "java.net.http.HttpClient", "newHttpClient", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "Default HTTP client factory.");
        staticMethod(symbols, "java.net.http.HttpClient", "newBuilder", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP client builder factory.");
        type(symbols, "java.net.http.HttpRequest", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP request type.");
        nestedType(symbols, "java.net.http.HttpRequest.Builder", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP request builder.");
        nestedType(symbols, "java.net.http.HttpRequest.BodyPublisher", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP request body publisher.");
        nestedType(symbols, "java.net.http.HttpRequest.BodyPublishers", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP request body publisher factories.");
        staticMethod(symbols, "java.net.http.HttpRequest", "newBuilder", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP request builder factory.");
        type(symbols, "java.net.http.HttpResponse", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP response type.");
        nestedType(symbols, "java.net.http.HttpResponse.BodyHandler", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP response body handler.");
        nestedType(symbols, "java.net.http.HttpResponse.BodyHandlers", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP response body handler factories.");
        nestedType(symbols, "java.net.http.HttpResponse.BodySubscriber", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP response body subscriber.");
        nestedType(symbols, "java.net.http.HttpResponse.BodySubscribers", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP response body subscriber factories.");
        nestedType(symbols, "java.net.http.HttpResponse.PushPromiseHandler", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP/2 push promise handler.");
        nestedType(symbols, "java.net.http.HttpResponse.ResponseInfo", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP response metadata view.");
        type(symbols, "java.net.http.WebSocket", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "WebSocket client connection type.");
        nestedType(symbols, "java.net.http.WebSocket.Builder", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "WebSocket builder.");
        nestedType(symbols, "java.net.http.WebSocket.Listener", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "WebSocket listener.");
        type(symbols, "java.net.http.HttpHeaders", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP header view.");
        type(symbols, "java.net.http.HttpTimeoutException", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP timeout exception.");
        type(symbols, "java.net.http.HttpConnectTimeoutException", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "HTTP connection timeout exception.");
        type(symbols, "java.net.http.WebSocketHandshakeException", ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11, "WebSocket handshake exception.");
        type(symbols, "java.util.concurrent.Flow", ApiSymbolCategory.DATA_FLOW, TargetProfile.JAVA11, "Reactive data-flow container namespace.");
        nestedType(symbols, "java.util.concurrent.Flow.Publisher", ApiSymbolCategory.DATA_FLOW, TargetProfile.JAVA11, "Data-flow publisher abstraction.");
        nestedType(symbols, "java.util.concurrent.Flow.Subscriber", ApiSymbolCategory.DATA_FLOW, TargetProfile.JAVA11, "Data-flow subscriber abstraction.");
        nestedType(symbols, "java.util.concurrent.Flow.Processor", ApiSymbolCategory.DATA_FLOW, TargetProfile.JAVA11, "Data-flow processor abstraction.");
        nestedType(symbols, "java.util.concurrent.Flow.Subscription", ApiSymbolCategory.DATA_FLOW, TargetProfile.JAVA11, "Data-flow subscription abstraction.");
        type(symbols, "java.util.concurrent.SubmissionPublisher", ApiSymbolCategory.DATA_FLOW, TargetProfile.JAVA11, "Submission-based data-flow publisher.");
    }

    private static void java17Symbols(List<ApiSymbol> symbols) {
        staticMethod(symbols, "java.util.stream.Collectors", "teeing", ApiSymbolCategory.COLLECTOR, TargetProfile.JAVA17, "Collector composition with two downstream collectors.");
        method(symbols, "java.util.stream.Stream", "toList", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "Terminal operation producing an unmodifiable list result.");
        method(symbols, "java.util.stream.Stream", "mapMulti", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "One-to-many stream mapping operation.");
        method(symbols, "java.util.stream.Stream", "mapMultiToInt", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "One-to-many mapping to an int stream.");
        method(symbols, "java.util.stream.Stream", "mapMultiToLong", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "One-to-many mapping to a long stream.");
        method(symbols, "java.util.stream.Stream", "mapMultiToDouble", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "One-to-many mapping to a double stream.");
        method(symbols, "java.util.stream.IntStream", "mapMulti", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "Primitive int stream mapMulti operation.");
        method(symbols, "java.util.stream.LongStream", "mapMulti", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "Primitive long stream mapMulti operation.");
        method(symbols, "java.util.stream.DoubleStream", "mapMulti", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "Primitive double stream mapMulti operation.");
        nestedType(symbols, "java.util.stream.IntStream.IntMapMultiConsumer", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "Primitive int mapMulti consumer.");
        nestedType(symbols, "java.util.stream.LongStream.LongMapMultiConsumer", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "Primitive long mapMulti consumer.");
        nestedType(symbols, "java.util.stream.DoubleStream.DoubleMapMultiConsumer", ApiSymbolCategory.STREAM, TargetProfile.JAVA17, "Primitive double mapMulti consumer.");

        languageFeature(symbols, "record", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Record language feature for transparent data carriers.");
        type(symbols, "java.lang.Record", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Base runtime type for records.");
        type(symbols, "java.lang.reflect.RecordComponent", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Record component reflection metadata.");
        method(symbols, "java.lang.Class", "isRecord", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Reflection check for record classes.");
        method(symbols, "java.lang.Class", "getRecordComponents", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Reflection access to record components.");
        languageFeature(symbols, "sealed class", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Sealed class language feature for controlled hierarchies.");
        languageFeature(symbols, "sealed interface", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Sealed interface language feature for controlled hierarchies.");
        method(symbols, "java.lang.Class", "isSealed", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Reflection check for sealed classes and interfaces.");
        method(symbols, "java.lang.Class", "getPermittedSubclasses", ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "Reflection access to permitted subclasses.");
        type(symbols, "java.util.HexFormat", ApiSymbolCategory.HEX_FORMAT, TargetProfile.JAVA17, "Byte and hex formatting utility.");
        type(symbols, "java.time.InstantSource", ApiSymbolCategory.TIME_SOURCE, TargetProfile.JAVA17, "Source of instants for time-sensitive code.");
        staticMethod(symbols, "java.time.InstantSource", "system", ApiSymbolCategory.TIME_SOURCE, TargetProfile.JAVA17, "System clock instant source factory.");
        staticMethod(symbols, "java.time.InstantSource", "fixed", ApiSymbolCategory.TIME_SOURCE, TargetProfile.JAVA17, "Fixed instant source factory.");
        staticMethod(symbols, "java.time.InstantSource", "offset", ApiSymbolCategory.TIME_SOURCE, TargetProfile.JAVA17, "Offset instant source factory.");
        staticMethod(symbols, "java.time.InstantSource", "tick", ApiSymbolCategory.TIME_SOURCE, TargetProfile.JAVA17, "Ticking instant source factory.");
        type(symbols, "java.util.random.RandomGenerator", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Base random generator interface.");
        nestedType(symbols, "java.util.random.RandomGenerator.SplittableGenerator", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Splittable random generator interface.");
        nestedType(symbols, "java.util.random.RandomGenerator.StreamableGenerator", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Streamable random generator interface.");
        nestedType(symbols, "java.util.random.RandomGenerator.JumpableGenerator", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Jumpable random generator interface.");
        nestedType(symbols, "java.util.random.RandomGenerator.LeapableGenerator", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Leapable random generator interface.");
        nestedType(symbols, "java.util.random.RandomGenerator.ArbitrarilyJumpableGenerator", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Arbitrarily jumpable random generator interface.");
        type(symbols, "java.util.random.RandomGeneratorFactory", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Random generator factory.");
        staticMethod(symbols, "java.util.random.RandomGeneratorFactory", "all", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "All available random generator factories.");
        staticMethod(symbols, "java.util.random.RandomGeneratorFactory", "getDefault", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Default random generator factory.");
        staticMethod(symbols, "java.util.random.RandomGeneratorFactory", "of", ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17, "Named random generator factory lookup.");
    }

    private static void java21Symbols(List<ApiSymbol> symbols) {
        type(symbols, "java.util.SequencedCollection", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Encounter-order collection abstraction.");
        type(symbols, "java.util.SequencedSet", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Encounter-order set abstraction.");
        type(symbols, "java.util.SequencedMap", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Encounter-order map abstraction.");
        method(symbols, "java.util.SequencedCollection", "reversed", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Reverse encounter-order view.");
        method(symbols, "java.util.SequencedCollection", "getFirst", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "First element access.");
        method(symbols, "java.util.SequencedCollection", "getLast", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Last element access.");
        method(symbols, "java.util.SequencedCollection", "addFirst", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "First-position insertion.");
        method(symbols, "java.util.SequencedCollection", "addLast", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Last-position insertion.");
        method(symbols, "java.util.SequencedCollection", "removeFirst", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "First element removal.");
        method(symbols, "java.util.SequencedCollection", "removeLast", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Last element removal.");
        method(symbols, "java.util.SequencedSet", "reversed", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Reverse encounter-order set view.");
        method(symbols, "java.util.SequencedMap", "firstEntry", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "First map entry access.");
        method(symbols, "java.util.SequencedMap", "lastEntry", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Last map entry access.");
        method(symbols, "java.util.SequencedMap", "pollFirstEntry", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "First map entry removal.");
        method(symbols, "java.util.SequencedMap", "pollLastEntry", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Last map entry removal.");
        method(symbols, "java.util.SequencedMap", "putFirst", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "First-position map insertion.");
        method(symbols, "java.util.SequencedMap", "putLast", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Last-position map insertion.");
        method(symbols, "java.util.SequencedMap", "sequencedKeySet", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Sequenced key-set view.");
        method(symbols, "java.util.SequencedMap", "sequencedValues", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Sequenced values view.");
        method(symbols, "java.util.SequencedMap", "sequencedEntrySet", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Sequenced entry-set view.");
        method(symbols, "java.util.SequencedMap", "reversed", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Reverse encounter-order map view.");
        method(symbols, "java.util.List", "reversed", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Retrofitted reverse list view.");
        method(symbols, "java.util.Deque", "reversed", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Retrofitted reverse deque view.");
        method(symbols, "java.util.LinkedHashSet", "addFirst", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Retrofitted first-position insertion on linked sets.");
        method(symbols, "java.util.LinkedHashSet", "addLast", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Retrofitted last-position insertion on linked sets.");
        method(symbols, "java.util.LinkedHashMap", "putFirst", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Retrofitted first-position insertion on linked maps.");
        method(symbols, "java.util.LinkedHashMap", "putLast", ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21, "Retrofitted last-position insertion on linked maps.");
        nestedType(symbols, "java.lang.Thread.Builder", ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21, "Thread builder abstraction.");
        nestedType(symbols, "java.lang.Thread.Builder.OfPlatform", ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21, "Platform thread builder.");
        nestedType(symbols, "java.lang.Thread.Builder.OfVirtual", ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21, "Virtual thread builder.");
        staticMethod(symbols, "java.lang.Thread", "ofPlatform", ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21, "Platform thread builder factory.");
        staticMethod(symbols, "java.lang.Thread", "ofVirtual", ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21, "Virtual thread builder factory.");
        staticMethod(symbols, "java.lang.Thread", "startVirtualThread", ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21, "Start a virtual thread for a task.");
        method(symbols, "java.lang.Thread", "isVirtual", ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21, "Virtual-thread status query.");
        staticMethod(symbols, "java.util.concurrent.Executors", "newVirtualThreadPerTaskExecutor", ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21, "Executor factory that creates one virtual thread per task.");
    }

    private static void java25Symbols(List<ApiSymbol> symbols) {
        method(symbols, "java.util.stream.Stream", "gather", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Gatherer-based stream intermediate operation.");
        type(symbols, "java.util.stream.Gatherer", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Stream gatherer abstraction.");
        staticMethod(symbols, "java.util.stream.Gatherer", "of", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Gatherer factory family.");
        staticMethod(symbols, "java.util.stream.Gatherer", "ofSequential", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Sequential gatherer factory family.");
        method(symbols, "java.util.stream.Gatherer", "initializer", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Gatherer initializer function.");
        method(symbols, "java.util.stream.Gatherer", "integrator", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Gatherer integrator function.");
        method(symbols, "java.util.stream.Gatherer", "combiner", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Gatherer combiner function.");
        method(symbols, "java.util.stream.Gatherer", "finisher", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Gatherer finisher function.");
        nestedType(symbols, "java.util.stream.Gatherer.Downstream", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Gatherer downstream control abstraction.");
        nestedType(symbols, "java.util.stream.Gatherer.Integrator", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Gatherer integration abstraction.");
        nestedType(symbols, "java.util.stream.Gatherer.Integrator.Greedy", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Greedy gatherer integrator marker.");
        type(symbols, "java.util.stream.Gatherers", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Standard gatherer factory class.");
        staticMethod(symbols, "java.util.stream.Gatherers", "fold", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Stateful fold gatherer factory.");
        staticMethod(symbols, "java.util.stream.Gatherers", "mapConcurrent", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Concurrent mapping gatherer factory.");
        staticMethod(symbols, "java.util.stream.Gatherers", "scan", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Prefix scan gatherer factory.");
        staticMethod(symbols, "java.util.stream.Gatherers", "windowFixed", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Fixed-size window gatherer factory.");
        staticMethod(symbols, "java.util.stream.Gatherers", "windowSliding", ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25, "Sliding window gatherer factory.");
    }

    private static void type(List<ApiSymbol> symbols, String owner, ApiSymbolCategory category, TargetProfile profile, String notes) {
        symbols.add(ApiSymbol.type(owner, category, profile, notes));
    }

    private static void nestedType(List<ApiSymbol> symbols, String owner, ApiSymbolCategory category, TargetProfile profile, String notes) {
        symbols.add(ApiSymbol.nestedType(owner, category, profile, notes));
    }

    private static void arrayType(List<ApiSymbol> symbols, String owner, ApiSymbolCategory category, TargetProfile profile, String notes) {
        symbols.add(ApiSymbol.arrayType(owner, category, profile, notes));
    }

    private static void method(List<ApiSymbol> symbols, String owner, String member, ApiSymbolCategory category, TargetProfile profile, String notes) {
        symbols.add(ApiSymbol.method(owner, member, category, profile, notes));
    }

    private static void staticMethod(List<ApiSymbol> symbols, String owner, String member, ApiSymbolCategory category, TargetProfile profile, String notes) {
        symbols.add(ApiSymbol.staticMethod(owner, member, category, profile, notes));
    }

    private static void languageFeature(List<ApiSymbol> symbols, String owner, ApiSymbolCategory category, TargetProfile profile, String notes) {
        symbols.add(ApiSymbol.languageFeature(owner, category, profile, notes));
    }
}
