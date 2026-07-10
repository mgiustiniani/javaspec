package io.github.jvmspec.api;

/**
 * One-column PHPSpec-style example-data set.
 *
 * @param <A> first row value type
 */
public final class Examples1<A> {
    private final ExampleRow1<A>[] rows;

    Examples1(ExampleRow1<A>[] rows) {
        this.rows = rows;
    }

    public void verify(Example1<A> example) {
        for (int i = 0; i < rows.length; i++) {
            ExampleRow1<A> row = rows[i];
            try {
                example.run(row.first());
            } catch (AssertionError failure) {
                throw ExampleRows.assertionFailure(i + 1, row.describe(), failure);
            } catch (Throwable failure) {
                throw ExampleRows.unexpectedFailure(i + 1, row.describe(), failure);
            }
        }
    }
}
