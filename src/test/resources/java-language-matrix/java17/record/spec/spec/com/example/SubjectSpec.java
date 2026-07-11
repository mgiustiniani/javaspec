package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_record_components() {
        shouldBeARecord();
        addedBehavior().shouldReturn("value");
    }
}
