package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_effectively_final_resources() {
        addedBehavior().shouldReturn("value");
    }
}
