package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_method_references() {
        addedBehavior().shouldReturn("value");
    }
}
