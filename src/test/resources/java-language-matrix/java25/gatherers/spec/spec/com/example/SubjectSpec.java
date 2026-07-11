package spec.com.example;

public final class SubjectSpec {
    public void it_uses_stream_gatherers_without_changing_spec_style() {
        addedBehavior().shouldReturn("value");
    }
}
