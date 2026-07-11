package spec.com.example;

public class SubjectSpec extends SubjectSpecSupport {
    public void it_uses_the_unique_production_target() {
        transform(" value ", value -> {
            return value.trim();
        }).shouldReturn("value");
    }
}
