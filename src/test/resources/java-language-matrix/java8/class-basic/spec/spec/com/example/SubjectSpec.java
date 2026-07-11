package spec.com.example;

public final class SubjectSpec {
    public void it_returns_a_greeting() {
        message().shouldReturn("hello");
    }
}
