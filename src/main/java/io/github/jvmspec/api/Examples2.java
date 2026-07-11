package io.github.jvmspec.api;

/**
 * Two-column PHPSpec-style example-data set.
 *
 * @param <A> first row value type
 * @param <B> second row value type
 */
public final class Examples2<A, B> {
    private final ExampleRow2<A, B>[] rows;

    Examples2(ExampleRow2<A, B>[] rows) {
        this.rows = rows;
    }

    public void verify(Example2<A, B> example) {
        for (int i = 0; i < rows.length; i++) {
            ExampleRow2<A, B> row = rows[i];
            try {
                example.run(row.first(), row.second());
                ExampleDataRowRecorder.record(ExampleDataRowResult.passed(i + 1, row.describe()));
            } catch (AssertionError failure) {
                ExampleDataRowRecorder.record(ExampleDataRowResult.failed(i + 1, row.describe(), failure));
                throw ExampleRows.assertionFailure(i + 1, row.describe(), failure);
            } catch (Throwable failure) {
                ExampleDataRowRecorder.record(ExampleDataRowResult.broken(i + 1, row.describe(), failure));
                throw ExampleRows.unexpectedFailure(i + 1, row.describe(), failure);
            }
        }
    }
}
