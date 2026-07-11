package spec.com.example;

public final class SubjectSpec {
    public void it_describes_an_interface() {
        shouldBeAnInterface();
        addedBehavior().shouldReturn("value");
    }
}
