package spec.com.example;

public final class SubjectSpec {
    public void it_uses_java11_apis_without_changing_spec_style() {
        addedBehavior().shouldReturn("value");
    }
}
