package spec.com.example;

import io.github.jvmspec.api.Pending;
import io.github.jvmspec.api.Skip;

public class CalculatorSpec extends CalculatorSpecSupport {
    public void let() {
        beConstructedWith(1);
    }

    public void letGo() {
        // Lifecycle cleanup hook exercised by the native runner.
    }

    public void it_adds_two_numbers() {
        add(2, 3).shouldReturn(6);
    }

    @Pending(reason = "Native preview follow-up behavior")
    public void it_records_pending_examples() {
        throw new AssertionError("pending example body must not execute");
    }

    @Skip(reason = "Native preview platform fixture")
    public void it_records_skipped_examples() {
        throw new AssertionError("skipped example body must not execute");
    }
}
