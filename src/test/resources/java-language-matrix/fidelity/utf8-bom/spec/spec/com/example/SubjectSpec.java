package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_utf8_bom() {
        addedBehavior().shouldReturn("value");
    }
}
