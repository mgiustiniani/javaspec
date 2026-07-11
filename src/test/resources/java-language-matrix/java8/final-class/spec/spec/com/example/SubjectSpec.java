package spec.com.example;

public final class SubjectSpec {
    public void it_describes_a_final_subject() {
        shouldBeAFinalClass();
        addedBehavior().shouldReturn("value");
    }
}
