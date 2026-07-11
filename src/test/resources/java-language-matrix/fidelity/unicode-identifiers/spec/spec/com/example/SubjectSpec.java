package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_unicode_identifiers() {
        addedBehavior().shouldReturn("value");
    }
}
