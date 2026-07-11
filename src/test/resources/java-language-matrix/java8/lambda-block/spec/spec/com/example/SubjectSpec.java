package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_lambda_blocks() {
        addedBehavior().shouldReturn("value");
    }
}
