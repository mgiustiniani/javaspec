package spec.com.example;

public final class SubjectSpec {
    public void it_updates_only_the_described_type() {
        addedBehavior().shouldReturn("value");
    }
}
