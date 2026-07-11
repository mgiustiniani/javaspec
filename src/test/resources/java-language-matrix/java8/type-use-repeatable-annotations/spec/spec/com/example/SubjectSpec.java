package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_java8_annotations() {
        addedBehavior().shouldReturn("value");
    }
}
