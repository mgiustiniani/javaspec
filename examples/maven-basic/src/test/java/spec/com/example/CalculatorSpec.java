package spec.com.example;

public class CalculatorSpec extends CalculatorSpecSupport {
    public void it_adds_two_numbers() {
        match(subject().add(2, 3)).shouldReturn(5);
    }
}
