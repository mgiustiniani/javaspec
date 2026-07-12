package io.github.jvmspec.cli.run;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Structured, prose-independent record of proposed and actually applied generation writes. */
public final class GenerationActivity {
    public enum Status { PROPOSED, APPLIED }

    private final List<Action> actions = new ArrayList<Action>();

    public void proposed(String kind, File file) {
        add(kind, file, Status.PROPOSED);
    }

    public void applied(String kind, File file) {
        add(kind, file, Status.APPLIED);
    }

    public List<Action> actions() {
        return Collections.unmodifiableList(new ArrayList<Action>(actions));
    }

    public int appliedWriteCount() {
        int count = 0;
        for (int i = 0; i < actions.size(); i++) {
            if (Status.APPLIED.equals(actions.get(i).status())) count++;
        }
        return count;
    }

    private void add(String kind, File file, Status status) {
        String path = file.getPath();
        for (int i = 0; i < actions.size(); i++) {
            Action existing = actions.get(i);
            if (existing.kind.equals(kind) && existing.path.equals(path)
                    && existing.status.equals(status)) return;
        }
        actions.add(new Action(kind, path, status));
    }

    public static final class Action {
        private final String kind;
        private final String path;
        private final Status status;

        private Action(String kind, String path, Status status) {
            this.kind = kind;
            this.path = path;
            this.status = status;
        }

        public String kind() { return kind; }
        public String path() { return path; }
        public Status status() { return status; }
    }
}
