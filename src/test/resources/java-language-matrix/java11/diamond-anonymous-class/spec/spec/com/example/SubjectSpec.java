package spec.com.example;

public final class SubjectSpec {
    public void it_ignores_anonymous_diamond_members() {
        size().shouldReturn(1);
    }
}
