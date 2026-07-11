package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_guarded_pattern_cases() {
        addedBehavior().shouldReturn("value");
    }
}
