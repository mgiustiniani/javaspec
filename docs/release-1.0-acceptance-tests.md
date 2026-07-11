# javaspec 1.0 semantic acceptance test map

This file maps the mandatory PHPSpec-first 1.0 acceptance scenarios from [`docs/phpspec-compatibility-matrix.md`](phpspec-compatibility-matrix.md) to automated tests. It is a release gate: an unmapped scenario is not ready for RC.

## Scenario 1 — simple subject workflow

User behavior:

1. Write a subject-centric spec.
2. Subject or member is missing.
3. javaspec reports meaningful RED or generation work.
4. `run --generate` creates only mechanical skeleton.
5. Behavior stays non-final-GREEN until domain logic is implemented.
6. After implementation, behavior becomes GREEN.

Automated evidence:

- `MainTest.runWithGenerateWritesMethodSkeletonsAndSpecificationSupportForTypedProxySpec`
- `MainPhase29CompileCliTest.runCompileWithPendingGeneratedStubCannotAccidentallyGreen`
- `SpecSkeletonGeneratorTest.rendersPackagedSpecSkeleton`
- `StubMarkerTest.methodUpdaterMarksInsertedStubBodies`

1.0 decision: generated stubs carry `// javaspec:stub`; compiled runs with remaining markers add a synthetic `BROKEN` generation result.

## Scenario 2 — custom construction

User behavior:

1. A spec chooses a constructor or factory.
2. javaspec resolves construction deterministically.
3. Ambiguous overloads produce readable errors.
4. Classes and records preserve Java construction semantics.

Automated evidence:

- `ObjectBehaviorTest.beConstructedWithPassesArgumentsAndCanBeOverriddenBeforeSubjectAccess`
- `ObjectBehaviorTest.beConstructedThroughAndNamedFactoriesUseStaticFactoryMethods`
- `ObjectBehaviorTest.shouldThrowSupportsDuringInstantiationAndDuringFactory`
- `SubjectLifecycleRecordCompatibilityTest`
- `MainPhase29CompileCliTest.runGenerateCompilePadsExplicitRecordConstructionPrefixAfterRecordEvolution`

1.0 decision: records are a Java adaptation; prefix padding applies only to explicit record canonical-constructor prefixes and never to ordinary classes/factories.

## Scenario 3 — collaborator and prediction lifecycle

User behavior:

1. `let` receives/configures collaborator.
2. Subject receives the same collaborator.
3. Example executes behavior.
4. Predictions are verified before teardown.
5. `letGo` always runs and failure precedence is preserved.

Automated evidence:

- `SpecRunnerTest.injectsInterfaceCollaboratorParametersIntoLetExampleAndLetGo`
- `SpecRunnerTest.injectsGeneratedProphecyWrappersIntoLetExampleAndLetGo`
- `SpecRunnerTest.automaticPredictionCheckingRunsBeforeLetGoButStillRunsLetGo`
- `SpecRunnerTest.letGoFailureAfterAutomaticPredictionFailureBecomesBrokenWithSuppressedPrediction`
- `SpecRunnerTest.rejectsDuplicateCollaboratorParameterTypesAsAmbiguous`

1.0 decision: collaborator injection is PHPSpec collaborator support, not a general DI container.

## Scenario 4 — matcher and diagnostics

User behavior:

1. An expectation fails.
2. Expected/actual and source context are available where the adapter/report supports them.
3. Assertion RED is `FAILED`, not infrastructure `BROKEN`.
4. CLI and adapters preserve semantic cause.

Automated evidence:

- `MatchableTest`
- `MatchableBoundedIterableTest`
- `ObjectBehaviorTest.directConvenienceAssertionFailuresPreserveUsefulMessages`
- `MainRunnerIntegrationTest.runReturnsFailureExitCodeWhenSpecificationFails`
- `RunReportWriterTest.failedRunResultJsonIncludesFailureDetail`
- `JUnitXmlReportWriterTest.failedExamplesMapToFailuresAndBrokenExamplesMapToErrors`

1.0 decision: generated object-state expectations are Java-compiled helpers rather than runtime dynamic matcher dispatch.

## Scenario 5 — example data

User behavior:

1. Multiple rows describe one behavior.
2. Failing row is identifiable.
3. Lifecycle and selection semantics are coherent.
4. JSON, XML, and JUnit Platform translate the same result.

Automated evidence:

- `ObjectBehaviorTest.exampleDataRunsOneColumnRowsInsideCurrentExample`
- `ObjectBehaviorTest.exampleDataAssertionFailuresIncludeRowContext`
- `MainPhase47ExampleDataCliTest.exampleDataRowsAppearInJsonAndJUnitXmlReports`
- `MainPhase47ExampleDataCliTest.failingExampleDataRowIdentifiesRowInCliAndReports`
- `JavaspecTestEnginePhase17Test` row descriptor/filter tests

1.0 decision: rows execute inline inside the behavior example; adapters expose row descriptors/report entries without replacing the PHPSpec authoring model with Jupiter parameterized tests.

## Scenario 6 — pending and generation

User behavior:

1. Explicit pending examples are distinct from skipped examples.
2. Generated production stubs are distinct from user-declared pending examples.
3. Remaining stubs cannot produce final GREEN.
4. Formatter, reports, and exit code agree for compiled runs.

Automated evidence:

- `SpecRunnerTest.annotationBasedSkipAndPendingResultsUseReasonsAndSkipPrecedence`
- `SpecRunnerTest.runtimeSkipAndPendingExceptionsFromExamplesAndConvenienceMethodsMapToNonExecutedResults`
- `RunReportWriterTest.pendingRunResultJsonIncludesRunAndSpecPendingCountsAndPendingStatus`
- `JUnitXmlReportWriterTest.pendingWithDefaultReasonMapsToSkippedWithPendingMessage`
- `MainPhase29CompileCliTest.runCompileWithPendingGeneratedStubCannotAccidentallyGreen`
- `StubMarkerTest.scannerReportsPendingStubsWithFileAndLine`

1.0 decision: explicit pending examples remain successful non-executed examples; generated incomplete production stubs become a synthetic `BROKEN` generation result in compiled runs.

## Scenario 7 — adapters preserve javaspec semantics

User behavior:

1. Same spec can run through CLI, Maven, Gradle, and JUnit Platform.
2. No Jupiter annotations or lifecycle are required.
3. Result states are translated, not reinterpreted.

Automated evidence:

- `MainPhase11ReportCliTest`
- `JavaspecMojoTest` and `JavaspecRunMojoTest`
- `JavaspecGradlePluginTest`
- `JavaspecTestEnginePhase17Test`
- `scripts/verify-examples.sh`
- `scripts/verify-all.sh`

1.0 decision: JUnit Platform is an adapter for IDE/build interoperability; PHPSpec-style `it_*` authoring remains canonical.
