package spec.com.example;

public final class SubjectSpec {
    public void it_preserves_a_multifile_sealed_hierarchy() {
        shouldBeASealedClass();
        shouldPermit(com.example.Child.class);
        addedBehavior().shouldReturn("value");
    }
}
