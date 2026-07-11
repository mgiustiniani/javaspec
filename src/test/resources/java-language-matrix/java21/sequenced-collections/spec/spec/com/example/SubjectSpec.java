package spec.com.example;

public final class SubjectSpec {
    public void it_uses_sequenced_collections_without_changing_spec_style() {
        addedBehavior().shouldReturn("value");
    }
}
