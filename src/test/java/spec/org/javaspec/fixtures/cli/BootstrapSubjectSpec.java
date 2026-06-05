package spec.org.javaspec.fixtures.cli;

import org.javaspec.fixtures.cli.BootstrapEventLog;

public class BootstrapSubjectSpec {
    public void it_runs_after_bootstrap() {
        BootstrapEventLog.record("example");
    }
}
