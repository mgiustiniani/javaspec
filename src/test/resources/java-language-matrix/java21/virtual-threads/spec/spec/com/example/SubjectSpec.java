package spec.com.example;

public final class SubjectSpec {
    public void it_keeps_virtual_thread_usage_inside_subject_behavior() {
        addedBehavior().shouldReturn("value");
    }
}
