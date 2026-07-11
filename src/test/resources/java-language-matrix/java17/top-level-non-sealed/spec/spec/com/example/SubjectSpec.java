package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_a_non_sealed_subject() {
        addedBehavior().shouldReturn("value");
    }
}
