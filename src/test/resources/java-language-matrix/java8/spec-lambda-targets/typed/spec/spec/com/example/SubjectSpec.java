package spec.com.example;

import java.util.function.Function;

public class SubjectSpec extends SubjectSpecSupport {
    public void it_uses_explicit_lambda_targets() {
        Function<String, String> normalizer = value -> {
            return value.trim();
        };
        transform(" value ", normalizer).shouldReturn("value");
        transformCast(" value ", (com.example.Subject.TextTransform<String>) value -> value.trim())
                .shouldReturn("value");
    }

    public void it_uses_an_explicitly_typed_parameter(Function<String, String> normalizer) {
        transformProvided(" value ", normalizer).shouldReturn("value");
    }
}
