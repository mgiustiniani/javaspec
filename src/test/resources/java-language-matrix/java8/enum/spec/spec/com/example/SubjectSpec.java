package spec.com.example;

public final class SubjectSpec {
    public void it_describes_an_enum() {
        shouldBeAnEnum();
        addedBehavior().shouldReturn("value");
    }
}
