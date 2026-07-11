package spec.com.example;

public final class SubjectSpec {
    public void it_ignores_fake_signatures_in_non_code_text() {
        addedBehavior().shouldReturn("value");
    }
}
