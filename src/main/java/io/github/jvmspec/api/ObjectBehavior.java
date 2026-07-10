package io.github.jvmspec.api;

import io.github.jvmspec.doubles.Call;
import io.github.jvmspec.doubles.DoubleControl;
import io.github.jvmspec.doubles.InterfaceDouble;
import io.github.jvmspec.doubles.prophecy.ObjectProphecy;
import io.github.jvmspec.doubles.prophecy.PredictionRegistry;
import io.github.jvmspec.doubles.prophecy.Prophecies;
import io.github.jvmspec.matcher.Matchable;
import io.github.jvmspec.matcher.MatcherRegistry;

import java.util.List;
import java.util.Objects;

/**
 * PHPSpec-inspired base class for generated javaspec specifications.
 * <p>
 * Provides lazy construction for the subject under test and matcher methods for specifying behaviour.
 * The pattern {@code match(subject().method()).shouldReturn(expected)} enables readable specifications,
 * while generated support classes can expose subject-specific typed proxy methods.
 * </p>
 *
 * @param <T> the subject type this spec describes
 */
public class ObjectBehavior<T> {
    private final SubjectLifecycle<T> lifecycle;
    private final AssertionDispatcher assertions;
    private final DoubleFacade doubles;
    private final SpecLifecycleSignals signals;
    private final SubjectTypeMarkers<T> subjectTypeMarkers;
    private MatcherRegistry matcherRegistry;

    /**
     * Creates a new ObjectBehavior with default matchers and no configured subject type.
     */
    public ObjectBehavior() {
        this(null);
    }

    /**
     * Creates a new ObjectBehavior with default matchers and a subject type for lazy construction.
     *
     * @param subjectType the concrete subject type to instantiate lazily
     */
    public ObjectBehavior(Class<? extends T> subjectType) {
        this.lifecycle = new SubjectLifecycle<T>(subjectType);
        this.matcherRegistry = MatcherRegistry.createWithDefaults();
        this.assertions = new AssertionDispatcher(this.matcherRegistry);
        this.doubles = new DoubleFacade();
        this.signals = new SpecLifecycleSignals();
        this.subjectTypeMarkers = new SubjectTypeMarkers<T>(this.lifecycle);
    }

    // --- Subject access ---

    /**
     * Returns the subject instance under test, constructing it lazily when a subject type is configured.
     */
    protected T subject() {
        return lifecycle.subject();
    }

    /**
     * Sets the subject instance. Called by the runner during lifecycle setup.
     */
    public void setSubject(T subject) {
        lifecycle.setSubject(subject);
    }

    // --- Matcher registry ---

    /**
     * Returns the matcher registry. Custom matchers can be added via
     * {@code matcherRegistry().register("name", matcher)}.
     */
    protected MatcherRegistry matcherRegistry() {
        return matcherRegistry;
    }

    /**
     * Replaces the default matcher registry with a custom one.
     * Useful for JUnit integration: pass a registry backed by JUnit assertions.
     */
    public void setMatcherRegistry(MatcherRegistry registry) {
        this.matcherRegistry = Objects.requireNonNull(registry, "registry must not be null");
        assertions.setMatcherRegistry(this.matcherRegistry);
    }

    /**
     * Returns the assertion dispatcher, allowing access to assertion methods directly.
     */
    public AssertionDispatcher assertions() {
        return assertions;
    }

    // --- Matchable wrapper ---

    /**
     * Wraps a value in a {@link Matchable} so that matcher methods can be chained.
     * <p>
     * Usage: {@code match(subject().getRating()).shouldReturn(5)}
     * </p>
     *
     * @param value the value to wrap
     * @param <R>   the type of the value
     * @return a Matchable that delegates to the registered matchers
     */
    protected <R> Matchable<R> match(R value) {
        return new Matchable<R>(value, matcherRegistry);
    }

    // --- PHPSpec-style example data ---

    /**
     * Creates a one-column example-data row for a single behavior example.
     */
    protected static <A> ExampleRow1<A> row(A first) {
        return new ExampleRow1<A>(first);
    }

    /**
     * Creates a two-column example-data row for a single behavior example.
     */
    protected static <A, B> ExampleRow2<A, B> row(A first, B second) {
        return new ExampleRow2<A, B>(first, second);
    }

    /**
     * Creates a one-column PHPSpec-style example-data set.
     */
    @SafeVarargs
    protected static <A> Examples1<A> examples(ExampleRow1<A>... rows) {
        return new Examples1<A>(rows);
    }

    /**
     * Creates a two-column PHPSpec-style example-data set.
     */
    @SafeVarargs
    protected static <A, B> Examples2<A, B> examples(ExampleRow2<A, B>... rows) {
        return new Examples2<A, B>(rows);
    }

    // --- Interface doubles ---

    /**
     * Creates a zero-dependency proxy double for an interface type.
     */
    public <D> D doubleFor(Class<D> interfaceType) {
        return doubles.doubleFor(interfaceType);
    }

    /**
     * Creates a typed double handle containing both proxy and control API.
     */
    public <D> InterfaceDouble<D> interfaceDouble(Class<D> interfaceType) {
        return doubles.interfaceDouble(interfaceType);
    }

    /**
     * Returns the stubbing, verification, and inspection API for a javaspec double.
     */
    public DoubleControl doubleControl(Object doubleInstance) {
        return doubles.doubleControl(doubleInstance);
    }

    /**
     * Alias for {@link #doubleControl(Object)}.
     */
    public DoubleControl inspectDouble(Object doubleInstance) {
        return doubles.inspectDouble(doubleInstance);
    }

    /**
     * Returns all recorded calls for a javaspec double.
     */
    public List<Call> doubleCalls(Object doubleInstance) {
        return doubles.doubleCalls(doubleInstance);
    }

    /**
     * Returns recorded calls for a method on a javaspec double.
     */
    public List<Call> doubleCalls(Object doubleInstance, String methodName) {
        return doubles.doubleCalls(doubleInstance, methodName);
    }

    /**
     * Returns recorded calls for a method and exact arguments on a javaspec double.
     */
    public List<Call> doubleCalls(Object doubleInstance, String methodName, Object... exactArguments) {
        return doubles.doubleCalls(doubleInstance, methodName, exactArguments);
    }

    /**
     * Returns the number of calls for a method on a javaspec double.
     */
    public int doubleCallCount(Object doubleInstance, String methodName) {
        return doubles.doubleCallCount(doubleInstance, methodName);
    }

    /**
     * Returns the number of calls for a method and exact arguments on a javaspec double.
     */
    public int doubleCallCount(Object doubleInstance, String methodName, Object... exactArguments) {
        return doubles.doubleCallCount(doubleInstance, methodName, exactArguments);
    }

    /**
     * Returns the number of calls for a method and exact arguments on a javaspec double.
     */
    public int doubleCallCountFor(Object doubleInstance, String methodName, Object... exactArguments) {
        return doubles.doubleCallCountFor(doubleInstance, methodName, exactArguments);
    }

    /**
     * Asserts that a method on a javaspec double was called at least once.
     */
    public void shouldHaveBeenCalled(Object doubleInstance, String methodName) {
        doubles.shouldHaveBeenCalled(doubleInstance, methodName);
    }

    /**
     * Asserts that a method on a javaspec double was called at least once with exact arguments.
     */
    public void shouldHaveBeenCalled(Object doubleInstance, String methodName, Object... exactArguments) {
        doubles.shouldHaveBeenCalled(doubleInstance, methodName, exactArguments);
    }

    /**
     * Asserts that a method on a javaspec double was called at least once with exact arguments.
     */
    public void shouldHaveBeenCalledWith(Object doubleInstance, String methodName, Object... exactArguments) {
        doubles.shouldHaveBeenCalledWith(doubleInstance, methodName, exactArguments);
    }

    /**
     * Asserts that a method on a javaspec double was not called.
     */
    public void shouldNotHaveBeenCalled(Object doubleInstance, String methodName) {
        doubles.shouldNotHaveBeenCalled(doubleInstance, methodName);
    }

    /**
     * Asserts that a method on a javaspec double was not called with exact arguments.
     */
    public void shouldNotHaveBeenCalled(Object doubleInstance, String methodName, Object... exactArguments) {
        doubles.shouldNotHaveBeenCalled(doubleInstance, methodName, exactArguments);
    }

    /**
     * Asserts that a method on a javaspec double was not called with exact arguments.
     */
    public void shouldNotHaveBeenCalledWith(Object doubleInstance, String methodName, Object... exactArguments) {
        doubles.shouldNotHaveBeenCalledWith(doubleInstance, methodName, exactArguments);
    }

    /**
     * Asserts an exact call count for a method on a javaspec double.
     */
    public void shouldHaveBeenCalledTimes(Object doubleInstance, String methodName, int expectedCount) {
        doubles.shouldHaveBeenCalledTimes(doubleInstance, methodName, expectedCount);
    }

    /**
     * Asserts an exact call count for a method and exact arguments on a javaspec double.
     */
    public void shouldHaveBeenCalledTimes(
            Object doubleInstance,
            String methodName,
            int expectedCount,
            Object... exactArguments
    ) {
        doubles.shouldHaveBeenCalledTimes(doubleInstance, methodName, expectedCount, exactArguments);
    }

    // --- Prophecy-style doubles API ---

    private PredictionRegistry prophecyRegistry;
    private boolean autoCheckPredictions = true;

    /**
     * Convenience factory for a Prophecy-style collaborator double.
     *
     * <p>{@code ObjectBehavior} remains the base class for specs. The prophecy runtime lives in
     * {@link Prophecies}; this method only shares this spec's prediction registry and delegates to
     * {@link Prophecies#prophesize(Class, PredictionRegistry)}.</p>
     *
     * @param type the interface or concrete class to prophesize
     * @param <D>  the prophesized type
     * @return a new object prophecy
     */
    protected <D> ObjectProphecy<D> prophesize(Class<D> type) {
        return Prophecies.prophesize(type, prophecyRegistry());
    }

    /**
     * Alias for {@link #prophesize(Class)}.
     */
    protected <D> ObjectProphecy<D> prophecy(Class<D> type) {
        return prophesize(type);
    }

    /**
     * Returns the prediction registry, creating one lazily if needed.
     */
    protected PredictionRegistry prophecyRegistry() {
        return sharedProphecyRegistry();
    }

    /**
     * Public adapter hook for runners and generated support that need this spec's shared
     * prediction registry without reflective access to protected internals.
     */
    public PredictionRegistry sharedProphecyRegistry() {
        if (prophecyRegistry == null) {
            prophecyRegistry = new PredictionRegistry();
        }
        return prophecyRegistry;
    }

    /**
     * Checks all registered prophecies predictions.
     * <p>
     * Call this at the end of an example to verify that all
     * {@code shouldBeCalled()}, {@code shouldNotBeCalled()}, and
     * {@code shouldBeCalledTimes()} predictions were met.
     * </p>
     *
     * @throws AssertionError if any prediction is not met
     */
    protected void checkPredictions() {
        if (prophecyRegistry != null) {
            prophecyRegistry.checkAll();
        }
    }

    /**
     * Sets the prediction registry to a custom instance.
     * Useful for sharing a registry across multiple prophecies.
     */
    public void setProphecyRegistry(PredictionRegistry registry) {
        this.prophecyRegistry = registry;
    }

    /**
     * Enables or disables automatic prediction checking after each example.
     * <p>
     * When enabled, {@link #checkPredictions()} is called automatically by the
     * runner after each example method completes successfully.
     * </p>
     *
     * @param autoCheck true to enable auto-check, false to disable (default: true)
     */
    public void setAutoCheckPredictions(boolean autoCheck) {
        this.autoCheckPredictions = autoCheck;
    }

    /**
     * Returns whether automatic prediction checking is enabled.
     */
    public boolean isAutoCheckPredictions() {
        return autoCheckPredictions;
    }

    /**
     * Checks predictions if auto-check is enabled. Called by the runner.
     */
    public void checkPredictionsIfEnabled() {
        if (autoCheckPredictions) {
            checkPredictions();
        }
    }

    // --- Explicit skipped and pending example signals ---

    /**
     * Stops the current example and reports it as skipped with the default reason.
     *
     * @throws SkipExampleException always
     */
    protected void skip() {
        signals.skip();
    }

    /**
     * Stops the current example and reports it as skipped with the supplied reason.
     *
     * @param reason human-readable skip reason
     * @throws SkipExampleException always
     */
    protected void skip(String reason) {
        signals.skip(reason);
    }

    /**
     * Stops the current example and reports it as pending with the default reason.
     *
     * @throws PendingExampleException always
     */
    protected void pending() {
        signals.pending();
    }

    /**
     * Stops the current example and reports it as pending with the supplied reason.
     *
     * @param reason human-readable pending reason
     * @throws PendingExampleException always
     */
    protected void pending(String reason) {
        signals.pending(reason);
    }

    // --- Direct assertion methods (convenience) ---

    /**
     * Asserts that the subject value is identical to the expected value (by == semantics).
     */
    public void shouldBe(Object actual, Object expected) {
        assertions.shouldBe(actual, expected);
    }

    /**
     * Asserts that the subject value is equal to the expected value (by equals semantics).
     */
    public void shouldEqual(Object actual, Object expected) {
        assertions.shouldEqual(actual, expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)} using PHPSpec return terminology.
     */
    public void shouldReturn(Object actual, Object expected) {
        assertions.shouldReturn(actual, expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)}.
     */
    public void shouldBeLike(Object actual, Object expected) {
        assertions.shouldBeLike(actual, expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)}.
     */
    public void shouldBeEqualTo(Object actual, Object expected) {
        assertions.shouldBeEqualTo(actual, expected);
    }

    /**
     * Asserts that the subject value is NOT identical to the unexpected value.
     */
    public void shouldNotBe(Object actual, Object unexpected) {
        assertions.shouldNotBe(actual, unexpected);
    }

    /**
     * Asserts that the subject value is NOT equal to the unexpected value.
     */
    public void shouldNotEqual(Object actual, Object unexpected) {
        assertions.shouldNotEqual(actual, unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)} using PHPSpec return terminology.
     */
    public void shouldNotReturn(Object actual, Object unexpected) {
        assertions.shouldNotReturn(actual, unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)}.
     */
    public void shouldNotBeLike(Object actual, Object unexpected) {
        assertions.shouldNotBeLike(actual, unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)}.
     */
    public void shouldNotBeEqualTo(Object actual, Object unexpected) {
        assertions.shouldNotBeEqualTo(actual, unexpected);
    }

    /**
     * Asserts that the subject value has the given type.
     */
    public void shouldHaveType(Object actual, Class<?> expectedType) {
        assertions.shouldHaveType(actual, expectedType);
    }

    /**
     * Alias for {@link #shouldHaveType(Object, Class)}.
     */
    public void shouldBeAnInstanceOf(Object actual, Class<?> expectedType) {
        assertions.shouldBeAnInstanceOf(actual, expectedType);
    }

    /**
     * Alias for {@link #shouldHaveType(Object, Class)} using PHPSpec return terminology.
     */
    public void shouldReturnAnInstanceOf(Object actual, Class<?> expectedType) {
        assertions.shouldReturnAnInstanceOf(actual, expectedType);
    }

    /**
     * Asserts that the subject value implements or extends the expected type.
     */
    public void shouldImplement(Object actual, Class<?> expectedType) {
        assertions.shouldImplement(actual, expectedType);
    }

    /**
     * Asserts that the numeric subject value is within the inclusive tolerance of the expected value.
     */
    public void shouldBeApproximately(Object actual, Number expected, Number tolerance) {
        assertions.shouldBeApproximately(actual, expected, tolerance);
    }

    /**
     * Alias for {@link #shouldBeApproximately(Object, Number, Number)} using PHPSpec return terminology.
     */
    public void shouldReturnApproximately(Object actual, Number expected, Number tolerance) {
        assertions.shouldReturnApproximately(actual, expected, tolerance);
    }

    /**
     * Asserts that the numeric subject value is outside the inclusive tolerance of the unexpected value.
     */
    public void shouldNotBeApproximately(Object actual, Number unexpected, Number tolerance) {
        assertions.shouldNotBeApproximately(actual, unexpected, tolerance);
    }

    /**
     * Alias for {@link #shouldNotBeApproximately(Object, Number, Number)} using PHPSpec return terminology.
     */
    public void shouldNotReturnApproximately(Object actual, Number unexpected, Number tolerance) {
        assertions.shouldNotReturnApproximately(actual, unexpected, tolerance);
    }

    /**
     * Asserts that strings, collections, maps, arrays, or iterables contain the expected value.
     */
    public void shouldContain(Object actual, Object expected) {
        assertions.shouldContain(actual, expected);
    }

    /**
     * Asserts that strings, collections, maps, arrays, or iterables do not contain the unexpected value.
     */
    public void shouldNotContain(Object actual, Object unexpected) {
        assertions.shouldNotContain(actual, unexpected);
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables have the expected count.
     */
    public void shouldHaveCount(Object actual, int expectedCount) {
        assertions.shouldHaveCount(actual, expectedCount);
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables are empty.
     */
    public void shouldBeEmpty(Object actual) {
        assertions.shouldBeEmpty(actual);
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables are not empty.
     */
    public void shouldNotBeEmpty(Object actual) {
        assertions.shouldNotBeEmpty(actual);
    }

    /**
     * Asserts that the map contains the expected key.
     */
    public void shouldHaveKey(Object actual, Object key) {
        assertions.shouldHaveKey(actual, key);
    }

    /**
     * Asserts that the map does not contain the unexpected key.
     */
    public void shouldNotHaveKey(Object actual, Object key) {
        assertions.shouldNotHaveKey(actual, key);
    }

    /**
     * Asserts that the map contains the expected value.
     */
    public void shouldHaveValue(Object actual, Object value) {
        assertions.shouldHaveValue(actual, value);
    }

    /**
     * Asserts that the map does not contain the unexpected value.
     */
    public void shouldNotHaveValue(Object actual, Object value) {
        assertions.shouldNotHaveValue(actual, value);
    }

    /**
     * Asserts that the character sequence does not start with the unexpected prefix.
     */
    public void shouldNotStartWith(Object actual, String prefix) {
        assertions.shouldNotStartWith(actual, prefix);
    }

    /**
     * Asserts that the character sequence does not end with the unexpected suffix.
     */
    public void shouldNotEndWith(Object actual, String suffix) {
        assertions.shouldNotEndWith(actual, suffix);
    }

    /**
     * Asserts that the character sequence does not match the supplied regular expression.
     */
    public void shouldNotMatchPattern(Object actual, String pattern) {
        assertions.shouldNotMatchPattern(actual, pattern);
    }

    // --- Discovery marker and runtime construction methods ---

    public void shouldHaveType(Class<?> expectedType) {
        subjectTypeMarkers.shouldHaveType(expectedType);
    }

    public void shouldBeAClass() {
        subjectTypeMarkers.shouldBeAClass();
    }

    public void shouldBeAFinalClass() {
        subjectTypeMarkers.shouldBeAFinalClass();
    }

    public void shouldBeAnInterface() {
        subjectTypeMarkers.shouldBeAnInterface();
    }

    public void shouldBeAnEnum() {
        subjectTypeMarkers.shouldBeAnEnum();
    }

    /**
     * Declares that the described enum should have a constant with the given name and optional constructor arguments.
     * Used in combination with shouldBeAnEnum() to verify enum constants exist.
     */
    public void shouldHaveConstant(String constantName, Object... args) {
        subjectTypeMarkers.shouldHaveConstant(constantName, args);
    }

    public void shouldBeAnAnnotation() {
        subjectTypeMarkers.shouldBeAnAnnotation();
    }

    public void shouldBeARecord() {
        subjectTypeMarkers.shouldBeARecord();
    }

    public void shouldBeASealedClass() {
        subjectTypeMarkers.shouldBeASealedClass();
    }

    public void shouldBeASealedInterface() {
        subjectTypeMarkers.shouldBeASealedInterface();
    }

    public void shouldExtend(Class<?>... extendedTypes) {
        subjectTypeMarkers.shouldExtend(extendedTypes);
    }

    public void shouldImplement(Class<?>... implementedTypes) {
        subjectTypeMarkers.shouldImplement(implementedTypes);
    }

    public void shouldPermit(Class<?>... permittedTypes) {
        subjectTypeMarkers.shouldPermit(permittedTypes);
    }

    /**
     * Configures lazy subject construction through a constructor with the given arguments.
     */
    public void beConstructedWith(Object... args) {
        lifecycle.beConstructedWith(args);
    }

    /**
     * Configures lazy subject construction through a static factory method.
     */
    public void beConstructedThrough(String methodName, Object... args) {
        lifecycle.beConstructedThrough(methodName, args);
    }

    /**
     * Configures lazy subject construction through a named static factory method.
     */
    public void beConstructedNamed(String name, Object... args) {
        lifecycle.beConstructedNamed(name, args);
    }

    /**
     * Configures lazy subject construction through a named static factory method.
     */
    public void beConstructedNamed(String name) {
        lifecycle.beConstructedNamed(name);
    }

    /**
     * Configures lazy subject construction through a named static factory method.
     */
    public void beConstructedThroughNamed(String name, Object... args) {
        lifecycle.beConstructedThroughNamed(name, args);
    }

    /**
     * Configures lazy subject construction through a named static factory method.
     */
    public void beConstructedThroughNamed(String name) {
        lifecycle.beConstructedThroughNamed(name);
    }

    /**
     * Creates a throw expectation for constructor/factory or method invocation checks.
     */
    public ThrowExpectation shouldThrow(Class<? extends Throwable> expectedType) {
        return new ThrowExpectation(this, expectedType);
    }

    /**
     * Runnable contract used by throw expectations without adding assertion dependencies.
     * This interface extends {@link SubjectLifecycle.ThrowingRunnable} for backward compatibility.
     */
    public interface ThrowingRunnable extends SubjectLifecycle.ThrowingRunnable {
    }

    /**
     * Zero-dependency throw expectation for method and instantiation checks.
     * This class delegates to {@link SubjectLifecycle.ThrowExpectation}.
     */
    public static class ThrowExpectation {
        private final SubjectLifecycle.ThrowExpectation delegate;

        protected ThrowExpectation(ObjectBehavior<?> behavior, Class<? extends Throwable> expectedType) {
            this.delegate = new SubjectLifecycle.ThrowExpectation(
                    Objects.requireNonNull(behavior, "behavior must not be null").lifecycle,
                    expectedType
            );
        }

        public void during(ThrowingRunnable runnable) {
            delegate.during(new SubjectLifecycle.ThrowingRunnable() {
                @Override
                public void run() throws Throwable {
                    runnable.run();
                }
            });
        }

        public void duringInstantiation() {
            delegate.duringInstantiation();
        }
    }
}
