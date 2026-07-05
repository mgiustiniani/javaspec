package spec.io.github.jvmspec.fixtures.cli;

public class Phase9FailureStopSubjectSpec {
    public void it_fails_first() {
        throw new AssertionError("phase9 first failure");
    }

    public void it_passes_after_failure() {
    }

    public void it_is_broken_after_failure() {
        throw new IllegalStateException("phase9 broken after failure");
    }
}
