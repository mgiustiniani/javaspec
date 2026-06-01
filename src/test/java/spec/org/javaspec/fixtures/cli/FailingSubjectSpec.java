package spec.org.javaspec.fixtures.cli;

public class FailingSubjectSpec {
    public void it_passes() {
    }

    public void it_fails() {
        throw new AssertionError("cli failure");
    }
}
