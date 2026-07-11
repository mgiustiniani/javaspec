package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_private_interface_helpers() {
        shouldBeAnInterface();
        addedBehavior().shouldReturn("value");
    }
}
