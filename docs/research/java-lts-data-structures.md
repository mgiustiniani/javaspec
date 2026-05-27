# Java LTS Data Structures Research

## Purpose

This document defines the initial metadata scope for Java data-structure-related APIs that javaspec must understand while remaining Java 8-compatible.

javaspec production code must not directly reference APIs introduced after Java 8. Later LTS APIs are cataloged as metadata and may be discovered through reflection only when running on a compatible JDK.

## Verification sources

Source verification date: 2026-05-27.

Official Oracle Java API documentation checked for this catalog:

- Java 8 API documentation: <https://docs.oracle.com/javase/8/docs/api/>
- Java 8 `java.util`: <https://docs.oracle.com/javase/8/docs/api/java/util/package-summary.html>
- Java 8 `java.util.concurrent`: <https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html>
- Java 8 `java.util.concurrent.atomic`: <https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html>
- Java 8 `java.lang.ref`: <https://docs.oracle.com/javase/8/docs/api/java/lang/ref/package-summary.html>
- Java 8 `java.util.stream`: <https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html>
- Java 11 API documentation: <https://docs.oracle.com/en/java/javase/11/docs/api/>
- Java 17 API documentation: <https://docs.oracle.com/en/java/javase/17/docs/api/>
- Java 21 API documentation: <https://docs.oracle.com/en/java/javase/21/docs/api/>
- Java 25 API documentation: <https://docs.oracle.com/en/java/javase/25/docs/api/>
- Java 25 `java.util.stream`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html>

## Definition of Data Structures for javaspec

For this project, "data structures" means public JDK collection, container, concurrent, reference, optional, array, stream-support, and collection factory/view abstractions relevant to modeling Java code in specifications.

This catalog prioritizes public JDK collection, container, concurrent coordination, reference, optional, atomic, array, and stream-support APIs. It is intentionally not a complete inventory of every public utility method in the JDK.

Included:

- Public `java.lang`, `java.util`, `java.util.concurrent`, `java.util.concurrent.atomic`, `java.lang.ref`, and stream-support abstractions that represent, expose, traverse, transform, or coordinate collections of values.
- Public collection interfaces, iterators, spliterators, maps, queues, deques, sets, lists, wrappers, factories, views, arrays, optional containers, atomic value containers, references, and stream pipelines.
- Concurrent result holders and coordination abstractions when they behave as value/result containers or affect data-flow modeling.
- Language-level primitive arrays and object arrays.

Excluded:

- Internal JDK implementation classes.
- Non-public immutable collection implementation classes returned by factories.
- Unrelated utilities that do not represent, traverse, expose, or coordinate data.
- General language features unless they directly affect data modeling; records and sealed types are noted as modeling tools but not classified as `java.util` collections.

## Java 8 Baseline

Java 8 is the only profile whose public APIs may be referenced directly by production source.

### Core collection and traversal abstractions

- `Iterable`
- `Collection`
- `List`
- `Set`
- `SortedSet`
- `NavigableSet`
- `Queue`
- `Deque`
- `BlockingQueue`
- `TransferQueue`
- `BlockingDeque`
- `Map`
- `Map.Entry`
- `SortedMap`
- `NavigableMap`
- `ConcurrentMap`
- `ConcurrentNavigableMap`
- `Iterator`
- `ListIterator`
- `Enumeration`
- `PrimitiveIterator`
- `PrimitiveIterator.OfInt`
- `PrimitiveIterator.OfLong`
- `PrimitiveIterator.OfDouble`
- `Spliterator`
- `Spliterator.OfPrimitive`
- `Spliterator.OfInt`
- `Spliterator.OfLong`
- `Spliterator.OfDouble`
- `Spliterators`
- `Spliterators.AbstractSpliterator`
- `Spliterators.AbstractIntSpliterator`
- `Spliterators.AbstractLongSpliterator`
- `Spliterators.AbstractDoubleSpliterator`
- `RandomAccess`

### Abstract base, legacy, and map-entry types

- `AbstractCollection`
- `AbstractList`
- `AbstractSequentialList`
- `AbstractSet`
- `AbstractQueue`
- `AbstractMap`
- `AbstractMap.SimpleEntry`
- `AbstractMap.SimpleImmutableEntry`
- `Dictionary`

These types are important for modeling inherited collection behavior and legacy APIs even when they are not preferred as new user-facing abstractions.

### Lists and list-like classes

- `ArrayList`
- `LinkedList`
- `Vector`
- `Stack`
- `CopyOnWriteArrayList`

### Sets

- `HashSet`
- `LinkedHashSet`
- `TreeSet`
- `EnumSet`
- `CopyOnWriteArraySet`
- `ConcurrentSkipListSet`

### Queues, deques, and concurrent queues

- `PriorityQueue`
- `ArrayDeque`
- `ConcurrentLinkedQueue`
- `ConcurrentLinkedDeque`
- `ArrayBlockingQueue`
- `LinkedBlockingQueue`
- `LinkedBlockingDeque`
- `PriorityBlockingQueue`
- `DelayQueue`
- `SynchronousQueue`
- `LinkedTransferQueue`

### Maps and map-like structures

- `HashMap`
- `LinkedHashMap`
- `TreeMap`
- `Hashtable`
- `WeakHashMap`
- `IdentityHashMap`
- `EnumMap`
- `ConcurrentHashMap`
- `ConcurrentHashMap.KeySetView`
- `ConcurrentSkipListMap`
- `Properties`

### Concurrent result and coordination containers

The main Java 8 data-structure emphasis remains on collections and containers above. The following public concurrent abstractions are also relevant as future/result holders or coordination structures:

- `Future`
- `RunnableFuture`
- `ScheduledFuture`
- `RunnableScheduledFuture`
- `FutureTask`
- `CompletableFuture`
- `CompletionStage`
- `CompletionService`
- `ExecutorCompletionService`

### Bit and utility containers

- `BitSet`

### Mutable reduction and string containers

- `IntSummaryStatistics`
- `LongSummaryStatistics`
- `DoubleSummaryStatistics`
- `StringJoiner`

These are mutable aggregation/value containers rather than general-purpose collections.

### Collection wrappers, factories, and views

`Collections` provides data-structure-relevant wrappers, factories, and views, including:

- Unmodifiable wrappers such as `Collections.unmodifiableCollection`, `unmodifiableList`, `unmodifiableSet`, `unmodifiableSortedSet`, `unmodifiableNavigableSet`, `unmodifiableMap`, `unmodifiableSortedMap`, and `unmodifiableNavigableMap`.
- Synchronized wrappers such as `Collections.synchronizedCollection`, `synchronizedList`, `synchronizedSet`, `synchronizedSortedSet`, `synchronizedNavigableSet`, `synchronizedMap`, `synchronizedSortedMap`, and `synchronizedNavigableMap`.
- Checked wrappers such as `Collections.checkedCollection`, `checkedList`, `checkedSet`, `checkedSortedSet`, `checkedNavigableSet`, `checkedQueue`, `checkedMap`, `checkedSortedMap`, and `checkedNavigableMap`.
- Empty factories/views such as `Collections.emptyList`, `emptySet`, `emptyMap`, `emptyIterator`, `emptyListIterator`, and `emptyEnumeration`.
- Singleton factories such as `Collections.singleton`, `singletonList`, and `singletonMap`.
- Repetition and adapter utilities such as `Collections.nCopies`, `asLifoQueue`, `newSetFromMap`, `enumeration`, and `list`.

### Arrays and array utilities

Language-level data structures:

- Primitive arrays: `boolean[]`, `byte[]`, `short[]`, `char[]`, `int[]`, `long[]`, `float[]`, `double[]`.
- Object arrays: `T[]`.

`Arrays` provides relevant utilities and views, including:

- `Arrays.asList`
- `copyOf`
- `copyOfRange`
- `sort`
- `parallelSort`
- `binarySearch`
- `equals`
- `deepEquals`
- `fill`
- `stream`
- `spliterator`

### Optional value containers

- `Optional`
- `OptionalInt`
- `OptionalLong`
- `OptionalDouble`

### Atomic value containers and updaters

- `AtomicBoolean`
- `AtomicInteger`
- `AtomicLong`
- `AtomicReference`
- `AtomicIntegerArray`
- `AtomicLongArray`
- `AtomicReferenceArray`
- `AtomicIntegerFieldUpdater`
- `AtomicLongFieldUpdater`
- `AtomicReferenceFieldUpdater`
- `AtomicStampedReference`
- `AtomicMarkableReference`
- `LongAdder`
- `LongAccumulator`
- `DoubleAdder`
- `DoubleAccumulator`

### Reference queues and references

- `ReferenceQueue`
- `Reference`
- `SoftReference`
- `WeakReference`
- `PhantomReference`

### Streams as traversable pipelines

Streams are not collections, but they are public traversable pipeline abstractions and are relevant to data modeling and expectations:

- `BaseStream`
- `Stream`
- `IntStream`
- `LongStream`
- `DoubleStream`
- `StreamSupport`
- `Collector`
- `Collector.Characteristics`
- `Collectors` collection-producing operations such as `toList`, `toSet`, `toMap`, grouping, partitioning, joining, mapping, reducing, and summarizing collectors.

## Java 11 LTS Profile

The `java11` profile includes data-structure-relevant APIs introduced in Java 9, Java 10, and Java 11.

All APIs in this section are metadata/reflection-only for a Java 8 binary. Production source must not import or statically reference them.

### Collection factories and immutable/value-based collections

- `List.of`
- `Set.of`
- `Map.of`
- `Map.ofEntries`
- `Map.entry`
- `List.copyOf`
- `Set.copyOf`
- `Map.copyOf`
- `Collectors.toUnmodifiableList`
- `Collectors.toUnmodifiableSet`
- `Collectors.toUnmodifiableMap`

These APIs provide unmodifiable, value-based collection instances. javaspec should model the factory methods and semantics, but must not depend on the JDK's internal implementation classes.

### Optional streams

- `Optional.stream`
- `OptionalInt.stream`
- `OptionalLong.stream`
- `OptionalDouble.stream`

### Stream conveniences relevant to traversal

- `Stream.ofNullable`
- `Stream.takeWhile`
- `Stream.dropWhile`
- Additional `Stream.iterate` overloads introduced after Java 8.

### Arrays conveniences

Data-structure-relevant array comparison and mismatch helpers include:

- `Arrays.compare` overloads
- `Arrays.compareUnsigned` overloads
- `Arrays.mismatch` overloads
- Newer `Arrays.equals` range overloads where applicable

### Reference-cleanup support

- `Cleaner`
- `Cleaner.Cleanable`

These are reference-cleanup support APIs. They are not collections, but they are relevant to reference lifecycle modeling.

### Data-flow and concurrent stream support

- `Flow`
- `Flow.Publisher`
- `Flow.Subscriber`
- `Flow.Processor`
- `Flow.Subscription`
- `SubmissionPublisher`

These APIs describe reactive data-flow and concurrent stream coordination. They are not general-purpose collections and should be prioritized below concrete collection/container metadata.

### Java 11 conclusion

No new general-purpose public concrete collection classes were added in Java 11 compared with Java 8. The main additions are factory methods, unmodifiable/value-based collection semantics, optional-to-stream bridges, stream traversal conveniences, array comparison utilities, reference-cleanup support, and data-flow/concurrent stream support.

## Java 17 LTS Profile

The `java17` profile includes data-structure-relevant APIs and language modeling features introduced in Java 12 through Java 17.

All APIs in this section are metadata/reflection-only for a Java 8 binary. Production source must not import or statically reference them.

### Stream and collector additions

- `Collectors.teeing`
- `Stream.toList`
- `Stream.mapMulti`
- `Stream.mapMultiToInt`
- `Stream.mapMultiToLong`
- `Stream.mapMultiToDouble`
- Primitive stream `mapMulti` variants where available on the target JDK
- `IntStream.IntMapMultiConsumer`
- `LongStream.LongMapMultiConsumer`
- `DoubleStream.DoubleMapMultiConsumer`

`Stream.toList` produces an unmodifiable list result and should be modeled distinctly from Java 8 `Collectors.toList`, whose concrete mutability is not guaranteed by the collector contract.

### Modeling features, not collections

- Records are transparent data carriers and are useful for modeling immutable values in specifications. They are language/runtime features, not `java.util` collections.
- Sealed classes and interfaces support controlled hierarchy modeling. They are not collections.

### Generator APIs

- The `RandomGenerator` family introduced in Java 17 is relevant as a generator API. It should not be classified as a data structure, but may be useful for generated examples or property-style extensions later.

### Byte/hex formatting utility

- `HexFormat` is a byte/hex formatting utility introduced in this period. It can support readable byte-oriented examples, but it is not a collection data structure.

### Java 17 conclusion

No new general-purpose public concrete collection classes were added in Java 17 beyond Java 11. The main additions relevant to javaspec are stream terminal/intermediate operations, collector composition, stream nested support types, records as data carriers, sealed types as hierarchy modeling support, byte/hex formatting support, and the random generator API family.

## Java 21 LTS Profile

The `java21` profile includes the major sequenced collection additions introduced in Java 21. It also inherits Java 11 collection factories and Java 17 stream additions such as `Stream.toList`.

All APIs in this section are metadata/reflection-only for a Java 8 binary. Production source must not import or statically reference them.

### Sequenced collection abstractions

- `SequencedCollection`
- `SequencedSet`
- `SequencedMap`

### Sequenced operations and views

Java 21 introduces uniform encounter-order operations and reversed views across applicable collection types:

- `reversed` views where applicable.
- First/last element operations such as `getFirst`, `getLast`, `addFirst`, `addLast`, `removeFirst`, and `removeLast` on applicable sequenced collections.
- Sequenced map operations such as `firstEntry`, `lastEntry`, `pollFirstEntry`, `pollLastEntry`, `putFirst`, `putLast`, `sequencedKeySet`, `sequencedValues`, `sequencedEntrySet`, and `reversed`.
- Deque-compatible first/last operations such as `pollFirst` and `pollLast` remain relevant where the type supports deque semantics; sequenced maps use `pollFirstEntry` and `pollLastEntry`.
- Retrofitted support on established ordered types such as lists, deques, linked sets, sorted/navigable sets, linked maps, and sorted/navigable maps where the Java 21 API defines it.

### Concurrency lifecycle APIs outside the initial catalog

Java 21-era `java.util.concurrent` APIs such as `Future.State` and `StructuredTaskScope` are relevant to task lifecycle and structured concurrency. They are not collection data structures for the initial catalog. They should be considered only if a future concurrency profile explicitly includes task-lifecycle APIs.

### Modeling features, not collections

Pattern matching and records improve modeling and deconstruction of data, but they are not collection/data-structure abstractions for this catalog.

### Java 21 conclusion

Java 21 adds important public collection abstractions and views through sequenced collections. javaspec must treat these as Java 21 profile metadata and must not directly reference the interfaces or methods from Java 8 production source.

## Java 25 LTS Profile

The `java25` profile inherits the Java 21 public collection/data-structure APIs already cataloged above.

All APIs in this section are metadata/reflection-only for a Java 8 binary. Production source must not import or statically reference them.

### Verified Java 25 findings

- No additional general-purpose public collection classes or collection abstractions beyond Java 21 are cataloged for Java 25.
- Java 25 includes post-Java-21 stream-support APIs that are relevant to traversal/transformation modeling.
- `StructuredTaskScope` changed between Java 21 and Java 25, but it remains a concurrency task-lifecycle API rather than a collection data structure. It should be handled only if a future concurrency profile includes task-lifecycle APIs.

### Stream gatherer support

The Java 25 profile should include the following stream-support APIs as metadata:

- `Gatherer`
- `Gatherer.Downstream`
- `Gatherer.Integrator`
- `Gatherer.Integrator.Greedy`
- `Gatherers`

Gatherers extend stream processing capabilities. They are not general-purpose concrete collections, but they are relevant to stream traversal and transformation semantics.

### Java 25 conclusion

No additional general-purpose public collection classes or collection abstractions beyond Java 21 are cataloged for Java 25. Java 25-specific additions in this project are limited to verified stream-support metadata unless a later, separately scoped concurrency profile is created.

## LTS Profile Mapping Table

| LTS version | Metadata profile key | Introduced APIs modeled by this profile | Java 8 compatibility strategy |
|---|---|---|---|
| Java 8 | `java8` | Core collection interfaces, abstract bases, iterators, primitive iterators, spliterators, maps, queues, deques, concrete collections, concurrent collections, concurrent result holders, wrappers/factories, arrays, mutable reduction containers, optionals, atomics, references, streams, `StreamSupport`, and collector metadata | Direct use is allowed in Java 8-compatible production code. |
| Java 11 | `java11` | `List.of`, `Set.of`, `Map.of`, `Map.ofEntries`, `Map.entry`, `List.copyOf`, `Set.copyOf`, `Map.copyOf`, `Collectors.toUnmodifiableList/Set/Map`, optional streams, stream traversal conveniences, `Arrays.compare`/`mismatch`/range `equals` utilities, `Cleaner`, `Flow`, and `SubmissionPublisher` | Store API symbols as metadata strings; discover reflectively only when runtime JDK supports them. |
| Java 17 | `java17` | `Collectors.teeing`, `Stream.toList`, `mapMulti`/`mapMultiToX`, stream map-multi consumer nested types, records as data carriers, sealed types as hierarchy modeling, `HexFormat`, and the `RandomGenerator` family as generator API | Store class/method names as metadata; do not compile against records, sealed APIs, `HexFormat`, stream map-multi nested types, or `RandomGenerator` from Java 8 source. |
| Java 21 | `java21` | `SequencedCollection`, `SequencedSet`, `SequencedMap`, reversed views, first/last operations, `putFirst`, `putLast`, `pollFirstEntry`, `pollLastEntry`, sequenced map views; excludes task-lifecycle APIs such as `Future.State` and `StructuredTaskScope` from the initial data-structure catalog | Represent interfaces and methods by name; conditionally reflect when running on Java 21+. |
| Java 25 | `java25` | Inherits Java 21 collection profile; adds verified stream-support metadata for `Gatherer`, `Gatherer.Downstream`, `Gatherer.Integrator`, `Gatherer.Integrator.Greedy`, and `Gatherers`; no additional general-purpose public collection classes or collection abstractions beyond Java 21 are cataloged | Store stream-support API names as metadata strings; conditionally reflect when running on Java 25+; do not compile Java 8 production source against Java 25 APIs. |

## Implementation Notes for javaspec

- The profile catalog should distinguish public API symbols from JDK internal implementation classes.
- Metadata should capture API owner, member name, kind, introduced version, and notes about semantics such as mutability, value-based behavior, traversal behavior, or task/data-flow role.
- Java 9+ APIs must remain metadata strings or reflective probes only in Java 8-compatible production code.
- Reflection helpers should fail gracefully on older runtimes.
- Tests should validate that loading the profile catalog on Java 8 does not trigger `ClassNotFoundException`, `NoClassDefFoundError`, or linkage errors for newer APIs.
