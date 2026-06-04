package spec.com.example;

import com.example.Calculator;
import org.javaspec.api.ObjectBehavior;

public class CalculatorSpec extends ObjectBehavior<Calculator> {
    public CalculatorSpec() {
        super(Calculator.class);
    }

    public void it_adds_two_numbers() {
        match(subject().add(2, 3)).shouldReturn(5);
    }
}
