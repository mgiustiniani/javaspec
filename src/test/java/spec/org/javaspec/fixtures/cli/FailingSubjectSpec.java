package spec.org.javaspec.fixtures.cli;

public class FailingSubjectSpec {
    public void it_passes() {
    }

    public void it_fails() {
        throw new AssertionError("cli failure");
    }

    @org.javaspec.api.Skip(reason = "cli skip")
    public void it_is_skipped() {
        throw new AssertionError("annotated skip should not run");
    }

    @org.javaspec.api.Pending(reason = "cli pending")
    public void it_is_pending() {
        throw new AssertionError("annotated pending should not run");
    }
}
