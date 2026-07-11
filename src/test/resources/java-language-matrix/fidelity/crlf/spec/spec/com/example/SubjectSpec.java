package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_crlf_line_endings() {
        addedBehavior().shouldReturn("value");
    }
}
