package spec.io.github.jvmspec.fixtures.cli;

public class Phase9BrokenStopSubjectSpec {
    public void it_breaks_first() {
        throw new IllegalStateException("phase9 first broken");
    }

    public void it_passes_after_broken() {
    }
}
