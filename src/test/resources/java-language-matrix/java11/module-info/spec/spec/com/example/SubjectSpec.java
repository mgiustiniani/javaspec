package spec.com.example;

public final class SubjectSpec {
    public void it_keeps_module_descriptors_outside_the_subject() {
        addedBehavior().shouldReturn("value");
    }
}
