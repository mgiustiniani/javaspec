package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_missing_final_newline() {
        addedBehavior().shouldReturn("value");
    }
}
