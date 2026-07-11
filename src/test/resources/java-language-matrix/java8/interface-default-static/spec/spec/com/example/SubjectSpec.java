package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_default_and_static_methods() {
        shouldBeAnInterface();
        addedBehavior().shouldReturn("value");
    }
}
