package spec.com.example;

public class SubjectSpec extends SubjectSpecSupport {
    public void it_refuses_an_untyped_lambda() {
        transform(" value ", value -> value.toString()).shouldReturn("value");
    }
}
