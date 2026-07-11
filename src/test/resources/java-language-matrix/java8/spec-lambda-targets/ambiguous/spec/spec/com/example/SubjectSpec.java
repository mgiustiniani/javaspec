package spec.com.example;

public class SubjectSpec extends SubjectSpecSupport {
    public void it_refuses_an_ambiguous_production_target() {
        transform(" value ", value -> value.trim()).shouldReturn("value");
    }
}
