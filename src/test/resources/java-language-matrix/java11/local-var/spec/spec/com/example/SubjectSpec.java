package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_local_var() {
        addedBehavior().shouldReturn("value");
    }
}
