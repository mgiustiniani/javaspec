package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_nested_record_patterns() {
        addedBehavior().shouldReturn("value");
    }
}
