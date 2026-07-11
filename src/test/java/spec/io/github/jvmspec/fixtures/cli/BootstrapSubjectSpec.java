package spec.io.github.jvmspec.fixtures.cli;

import io.github.jvmspec.fixtures.cli.BootstrapEventLog;

public class BootstrapSubjectSpec {
    public void it_runs_after_bootstrap() {
        BootstrapEventLog.record("example");
    }
}
